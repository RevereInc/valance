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
import dev.revere.valance.util.Logger;

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
    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":CommandManager]";

    private final IModuleManager moduleManager;

    private final Map<String, ICommand> commandMap = new ConcurrentHashMap<>();

    @Inject
    public CommandManagerService(IModuleManager moduleManager) {
        Logger.info(LOG_PREFIX, "Constructed.");
        this.moduleManager = Objects.requireNonNull(moduleManager);
    }

    @Override
    public void initialize(ClientContext context) throws ServiceException {
        Logger.info(LOG_PREFIX, "Initializing...");
        registerCommands();
        Logger.info(LOG_PREFIX, "Initialized. Registered " + commandMap.size() + " command triggers.");
    }

    private void registerCommands() {
        Logger.info(LOG_PREFIX, "Registering commands...");
        register(new HelpCommand(this));
        register(new ToggleCommand(moduleManager));
        register(new SettingCommand(moduleManager));
        Logger.info(LOG_PREFIX, "Command registration complete.");
    }

    @Override
    public void shutdown(ClientContext context) throws ServiceException {
        Logger.info(LOG_PREFIX, "Shutting down...");
        commandMap.clear();
        Logger.info(LOG_PREFIX, "Shutdown complete.");
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
            Logger.info(LOG_PREFIX, "Unregistered command trigger: " + nameLower);
            for (String alias : command.getAliases()) {
                String aliasLower = alias.toLowerCase();
                if (commandMap.get(aliasLower) == command) {
                    commandMap.remove(aliasLower);
                    Logger.info(LOG_PREFIX, "Unregistered command trigger: " + aliasLower);
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

        String[] parts = trimmedInput.split("\\s+", 2);
        String commandName = parts[0].toLowerCase();
        String[] args = (parts.length > 1 && parts[1] != null && !parts[1].trim().isEmpty())
                ? parts[1].trim().split("\\s+")
                : new String[0];

        Optional<ICommand> commandOpt = getCommand(commandName);

        if (commandOpt.isPresent()) {
            ICommand command = commandOpt.get();
            Logger.info(LOG_PREFIX, "Executing command: " + command.getName() + " Args: " + Arrays.toString(args));
            try {
                command.execute(args);
            } catch (CommandException e) {
                Logger.error(LOG_PREFIX, "Command error (" + command.getName() + "): " + e.getMessage());
                sendClientMessage("Error: " + e.getMessage(), true);
                sendClientMessage("Usage: " + getPrefix() + command.getName() + " " + command.getUsage(), true);
            } catch (Throwable t) {
                Logger.info(LOG_PREFIX, "[CRITICAL] Unexpected error executing command: " + command.getName());
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