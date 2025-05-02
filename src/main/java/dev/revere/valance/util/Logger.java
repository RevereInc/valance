package dev.revere.valance.util;

import dev.revere.valance.ClientLoader;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Remi
 * @project valance
 * @date 5/2/2025
 */
public class Logger {
    private static final boolean INCLUDE_TIMESTAMP = true;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final String DEFAULT_CLIENT_PREFIX = "[" + ClientLoader.CLIENT_NAME + "]";

    private static final boolean ENABLE_DEBUG_LOGGING = false;

    private Logger() {}

    public static void info(String prefix, String message) {
        log(System.out, "INFO", prefix, message, null);
    }

    public static void info(Class<?> clazz, String message) {
        info(formatPrefix(clazz), message);
    }

    public static void warn(String prefix, String message) {
        log(System.err, "WARN", prefix, message, null);
    }

    public static void warn(Class<?> clazz, String message) {
        warn(formatPrefix(clazz), message);
    }

    public static void error(String prefix, String message, Throwable throwable) {
        log(System.err, "ERROR", prefix, message, throwable);
    }

    public static void error(String prefix, String message) {
        error(prefix, message, null);
    }

    public static void error(Class<?> clazz, String message, Throwable throwable) {
        error(formatPrefix(clazz), message, throwable);
    }

    public static void error(Class<?> clazz, String message) {
        error(formatPrefix(clazz), message, null);
    }

    public static void debug(String prefix, String message) {
        if (ENABLE_DEBUG_LOGGING) {
            log(System.out, "DEBUG", prefix, message, null);
        }
    }

    public static void debug(Class<?> clazz, String message) {
        debug(formatPrefix(clazz), message);
    }

    private static void log(PrintStream stream, String level, String prefix, String message, Throwable throwable) {
        StringBuilder logBuilder = new StringBuilder();

        if (INCLUDE_TIMESTAMP) {
            logBuilder.append("[").append(LocalDateTime.now().format(TIME_FORMATTER)).append("] ");
        }

        logBuilder.append("[").append(Thread.currentThread().getName()).append("/").append(level).append("] ");

        if (prefix != null && !prefix.trim().isEmpty()) {
            if (prefix.startsWith("[") && prefix.endsWith("]")) {
                logBuilder.append(prefix).append(" ");
            } else {
                logBuilder.append("[").append(prefix).append("] ");
            }
        } else {
            logBuilder.append(DEFAULT_CLIENT_PREFIX).append(" ");
        }


        logBuilder.append(message);

        synchronized (stream) {
            stream.println(logBuilder);
            if (throwable != null) {
                throwable.printStackTrace(stream);
            }
        }
    }

    /**
     * Helper to create a standard prefix from a class name.
     */
    private static String formatPrefix(Class<?> clazz) {
        return "[" + clazz.getSimpleName() + "]";
    }
}
