package okreplay

import com.google.common.annotations.VisibleForTesting
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.Rule
import org.junit.runners.model.Statement
import java.util.logging.Level
import java.util.logging.Logger
import okreplay.CaseFormat.*

/**
 * This is an extension of [Recorder] that can be used as a
 * JUnit [Rule] allowing tests annotated with [OkReplay] to automatically
 * activate OkReplay recording.
 */
@VisibleForTesting
public class RecorderRule(configuration: OkReplayConfig) : Recorder(configuration), TestRule {
  override fun apply(statement: Statement, description: Description): Statement {
    val annotation = description.getAnnotation(OkReplay::class.java)
    if (annotation != null) {
      LOG.info(String.format("found @OkReplay annotation on '%s'", description.displayName))
      return object : Statement() {
        override fun evaluate() {
          try {
            val tapeName = if (annotation.tape.isEmpty()) {
              description.defaultTapeName()
            } else {
              annotation.tape
            }
            val tapeMode = annotation.mode
            val matchRules = annotation.match
            val matchRule = if (matchRules.isNotEmpty()) {
              ComposedMatchRule.of(*matchRules)
            } else {
              null
            }
            start(tapeName, tapeMode.toNullable(), matchRule)
            statement.evaluate()
          } catch (e: Exception) {
            LOG.log(Level.SEVERE, "Caught exception starting OkReplay", e)
            throw e
          } finally {
            try {
              stop()
            } catch (e: IllegalStateException) {
              // Recorder has not started yet.
            }
          }
        }
      }
    } else {
      LOG.info(String.format("no @OkReplay annotation on '%s'", description.displayName))
      return statement
    }
  }

  private fun Description.defaultTapeName(): String {
    val name = if (methodName != null) {
      LOWER_CAMEL.to(LOWER_UNDERSCORE, methodName)
    } else {
      UPPER_CAMEL.to(LOWER_UNDERSCORE, testClass.simpleName)
    }
    return name.replace('_', ' ')
  }

  companion object {
    private val LOG = Logger.getLogger(RecorderRule::class.java.simpleName)
  }
}
