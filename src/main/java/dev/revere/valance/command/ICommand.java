package dev.revere.valance.command;

import dev.revere.valance.command.exception.CommandException;

import java.util.List;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public interface ICommand {
    /**
     * @return The primary name used to invoke the command (case-insensitive).
     */
    String getName();

    /**
     * @return A list of alternative names (aliases) for the command (case-insensitive). Can be empty.
     */
    List<String> getAliases();

    /**
     * @return A brief description of what the command does (for help commands).
     */
    String getDescription();

    /**
     * @return Example syntax showing how to use the command (e.g., "<module> <setting> <value>").
     */
    String getUsage();

    /**
     * Executes the command logic.
     *
     * @param args The arguments provided by the user after the command name.
     * @throws CommandException If execution fails due to invalid arguments, permissions, or other issues.
     */
    void execute(String[] args) throws CommandException;
}