package dev.revere.valance.service.impl;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.command.ICommand;
import dev.revere.valance.command.exception.CommandException;
import dev.revere.valance.command.impl.HelpCommand;
import dev.revere.valance.command.impl.SettingCommand;
import dev.revere.valance.command.impl.ToggleCommand;
import dev.revere.valance.core.ClientContext;
import dev.revere.valance.core.annotation.Inject;
import dev.revere.valance.core.annotation.Service;
import dev.revere.valance.core.exception.ServiceException;
import dev.revere.valance.service.ICommandService;
import dev.revere.valance.service.IModuleManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static dev.revere.valance.util.MinecraftUtil.sendClientMessage;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@Service(provides = ICommandService.class, priority = 50)
public class CommandManagerService implements ICommandService {
    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":CommandManager] ";

    private final IModuleManager moduleManager; // Needed by commands like .toggle, .setting

    // Use ConcurrentHashMap for thread safety if commands could be registered dynamically
    private final Map<String, ICommand> commandMap = new ConcurrentHashMap<>();

    @Inject
    public CommandManagerService(IModuleManager moduleManager) {
        System.out.println(LOG_PREFIX + "Constructed.");
        this.moduleManager = Objects.requireNonNull(moduleManager);
    }

    @Override
    public void initialize(ClientContext context) throws ServiceException {
        System.out.println(LOG_PREFIX + "Initializing...");
        registerCommands();
        System.out.println(LOG_PREFIX + "Initialized. Registered " + commandMap.size() + " command triggers.");
    }

    private void registerCommands() {
        System.out.println(LOG_PREFIX + "Registering commands...");
        register(new HelpCommand(this));
        register(new ToggleCommand(moduleManager));
        register(new SettingCommand(moduleManager));
        System.out.println(LOG_PREFIX + "Command registration complete.");
    }

    @Override
    public void shutdown(ClientContext context) throws ServiceException {
        System.out.println(LOG_PREFIX + "Shutting down...");
        commandMap.clear(); // Clear registry
        System.out.println(LOG_PREFIX + "Shutdown complete.");
    }


    @Override
    public void register(ICommand command) {
        Objects.requireNonNull(command, "Cannot register null command.");
        String nameLower = command.getName().toLowerCase();

        if (commandMap.containsKey(nameLower)) {
            throw new IllegalArgumentException("Command name conflict: '" + nameLower + "' is already registered.");
        }
        commandMap.put(nameLower, command);

        for (String alias : command.getAliases()) {
            String aliasLower = alias.toLowerCase();
            if (commandMap.containsKey(aliasLower)) {
                commandMap.remove(nameLower);
                throw new IllegalArgumentException("Command alias conflict: '" + aliasLower + "' (from " + nameLower + ") is already registered.");
            }
            commandMap.put(aliasLower, command);
        }
    }

    @Override
    public void unregister(ICommand command) {
        Objects.requireNonNull(command, "Cannot unregister null command.");
        String nameLower = command.getName().toLowerCase();
        ICommand removed = commandMap.remove(nameLower);
        if (removed != null) {
            System.out.println(LOG_PREFIX + "Unregistered command trigger: " + nameLower);
            // Remove aliases too
            for (String alias : command.getAliases()) {
                String aliasLower = alias.toLowerCase();
                if (commandMap.get(aliasLower) == command) { // Ensure we only remove if it points to the same command instance
                    commandMap.remove(aliasLower);
                    System.out.println(LOG_PREFIX + "Unregistered command trigger: " + aliasLower);
                }
            }
        }
    }

    @Override
    public Optional<ICommand> getCommand(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(commandMap.get(name.toLowerCase()));
    }

    @Override
    public List<ICommand> getCommands() {
        return Collections.unmodifiableList(new ArrayList<>(new HashSet<>(commandMap.values())));
    }

    @Override
    public boolean dispatch(String rawInput) {
        if (rawInput == null || !rawInput.startsWith(getPrefix())) {
            return false;
        }

        String trimmedInput = rawInput.substring(getPrefix().length()).trim();
        if (trimmedInput.isEmpty()) {
            return true;
        }

        String[] parts = trimmedInput.split("\\s+", 2); // Split into command name and the rest (args)
        String commandName = parts[0].toLowerCase();
        String[] args = (parts.length > 1 && parts[1] != null && !parts[1].trim().isEmpty())
                ? parts[1].trim().split("\\s+") // Split remaining into args
                : new String[0]; // No arguments


        Optional<ICommand> commandOpt = getCommand(commandName);

        if (commandOpt.isPresent()) {
            ICommand command = commandOpt.get();
            System.out.println(LOG_PREFIX + "Executing command: " + command.getName() + " Args: " + Arrays.toString(args));
            try {
                command.execute(args);
            } catch (CommandException e) {
                System.err.println(LOG_PREFIX + "[ERROR] Command error (" + command.getName() + "): " + e.getMessage());
                sendClientMessage("Error: " + e.getMessage(), true);
                // Optionally send usage info on error
                sendClientMessage("Usage: " + getPrefix() + command.getName() + " " + command.getUsage(), true);
            } catch (Throwable t) { // Catch unexpected runtime errors during execution
                System.err.println(LOG_PREFIX + "[CRITICAL] Unexpected error executing command: " + command.getName());
                t.printStackTrace();
                sendClientMessage("An internal error occurred while executing '" + command.getName() + "'. Check logs.", true);
            }
        } else {
            sendClientMessage("Unknown command: '" + commandName + "'. Type " + getPrefix() + "help for a list.", true);
        }
        return true;
    }

    @Override
    public String getPrefix() {
        return ClientLoader.COMMAND_PREFIX;
    }
}