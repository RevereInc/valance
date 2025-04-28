package dev.revere.valance.command.impl;

import dev.revere.valance.command.AbstractCommand;
import dev.revere.valance.command.ICommand;
import dev.revere.valance.command.exception.CommandException;
import dev.revere.valance.service.ICommandService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public class HelpCommand extends AbstractCommand {
    private final ICommandService commandService;

    public HelpCommand(ICommandService commandService) {
        super("help", List.of("?"), "Displays available commands.", "[command_name]");
        this.commandService = commandService;
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length == 0) {
            List<ICommand> commands = commandService.getCommands().stream()
                    .sorted(Comparator.comparing(ICommand::getName))
                    .collect(Collectors.toList());

            sendSuccess("--- Available Commands (" + commands.size() + ") ---");
            for (ICommand cmd : commands) {
                String aliasStr = cmd.getAliases().isEmpty() ? "" : " Aliases: " + String.join(", ", cmd.getAliases());
                sendSuccess(commandService.getPrefix() + cmd.getName() + " " + cmd.getUsage() + " - " + cmd.getDescription() + aliasStr);
            }
            sendSuccess("Type " + commandService.getPrefix() + "help <command_name> for more details.");

        } else if (args.length == 1) {
            String commandName = args[0];
            ICommand command = commandService.getCommand(commandName).orElseThrow(() -> new CommandException("Command '" + commandName + "' not found."));

            sendSuccess("--- Help: " + command.getName() + " ---");
            sendSuccess("Description: " + command.getDescription());
            sendSuccess("Usage: " + commandService.getPrefix() + command.getName() + " " + command.getUsage());
            if(!command.getAliases().isEmpty()){
                sendSuccess("Aliases: " + String.join(", ", command.getAliases()));
            }
        } else {
            sendUsage();
        }
    }
}