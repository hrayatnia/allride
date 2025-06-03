package utils

import mu.KotlinLogging

object Logger {
    private val logger = KotlinLogging.logger {}

    fun info(message: String, vararg args: Any?) {
        logger.info { formatMessage(message, args) }
    }

    fun debug(message: String, vararg args: Any?) {
        logger.debug { formatMessage(message, args) }
    }

    fun warn(message: String, vararg args: Any?) {
        logger.warn { formatMessage(message, args) }
    }

    fun error(message: String, throwable: Throwable? = null, vararg args: Any?) {
        if (throwable != null) {
            logger.error(throwable) { formatMessage(message, args) }
        } else {
            logger.error { formatMessage(message, args) }
        }
    }

    private fun formatMessage(message: String, args: Array<out Any?>): String {
        return if (args.isEmpty()) {
            message
        } else {
            try {
                String.format(message, *args)
            } catch (e: Exception) {
                "$message (args: ${args.joinToString()})"
            }
        }
    }

    fun withContext(context: Map<String, Any?>) = ContextualLogger(context)
}

class ContextualLogger(private val context: Map<String, Any?>) {
    private val logger = KotlinLogging.logger {}
    private val contextString = context.entries.joinToString(", ") { "${it.key}=${it.value}" }

    fun info(message: String, vararg args: Any?) {
        logger.info { "[Context: $contextString] ${formatMessage(message, args)}" }
    }

    fun debug(message: String, vararg args: Any?) {
        logger.debug { "[Context: $contextString] ${formatMessage(message, args)}" }
    }

    fun warn(message: String, vararg args: Any?) {
        logger.warn { "[Context: $contextString] ${formatMessage(message, args)}" }
    }

    fun error(message: String, throwable: Throwable? = null, vararg args: Any?) {
        if (throwable != null) {
            logger.error(throwable) { "[Context: $contextString] ${formatMessage(message, args)}" }
        } else {
            logger.error { "[Context: $contextString] ${formatMessage(message, args)}" }
        }
    }

    private fun formatMessage(message: String, args: Array<out Any?>): String {
        return if (args.isEmpty()) {
            message
        } else {
            try {
                String.format(message, *args)
            } catch (e: Exception) {
                "$message (args: ${args.joinToString()})"
            }
        }
    }
} 