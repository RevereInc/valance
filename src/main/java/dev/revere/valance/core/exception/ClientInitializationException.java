package dev.revere.valance.core.exception;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public class ClientInitializationException extends RuntimeException {
    public ClientInitializationException(String message) {
        super(message);
    }

    public ClientInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}