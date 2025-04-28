package dev.revere.valance.service;

import dev.revere.valance.command.ICommand;
import dev.revere.valance.core.lifecycle.IService;

import java.util.List;
import java.util.Optional;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public interface ICommandService extends IService {

    /**
     * Registers a command.
     *
     * @param command The command instance to register.
     * @throws IllegalArgumentException if a command with the same name or alias already exists.
     */
    void register(ICommand command);

    /**
     * Unregisters a command.
     *
     * @param command The command instance to unregister.
     */
    void unregister(ICommand command);

    /**
     * Gets a command by its name or alias (case-insensitive).
     *
     * @param name The name or alias of the command.
     * @return An Optional containing the command if found.
     */
    Optional<ICommand> getCommand(String name);

    /**
     * @return An unmodifiable list of all registered commands.
     */
    List<ICommand> getCommands();

    /**
     * Processes raw user input string. Checks for the command prefix,
     * parses the command name and arguments, finds the command, and executes it.
     *
     * @param rawInput The complete string entered by the user (e.g., in chat).
     * @return true if the input was handled as a command (even if execution failed), false otherwise.
     */
    boolean dispatch(String rawInput);

    /**
     * @return The command prefix used by the client (e.g., ".").
     */
    String getPrefix();
}
