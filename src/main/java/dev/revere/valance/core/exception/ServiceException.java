package dev.revere.valance.core.exception;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public class ServiceException extends RuntimeException {
    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}