package dev.revere.valance.command.exception;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public class CommandException extends Exception {
    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }
}