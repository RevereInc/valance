package dev.revere.valance.command;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.command.exception.CommandException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static dev.revere.valance.util.MinecraftUtil.sendClientMessage;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public abstract class AbstractCommand implements ICommand {
    protected static final String LOG_PREFIX_CMD = "[" + ClientLoader.CLIENT_NAME + ":Command] ";

    private final String name;
    private final List<String> aliases;
    private final String description;
    private final String usage;

    public AbstractCommand(String name, List<String> aliases, String description, String usage) {
        this.name = Objects.requireNonNull(name, "Command name cannot be null").toLowerCase();
        this.aliases = aliases == null ? Collections.emptyList() :
                aliases.stream().map(String::toLowerCase).collect(Collectors.toList());
        this.description = description == null ? "" : description;
        this.usage = usage == null ? "" : usage;

        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Command name cannot be empty.");
        }
        if (name.contains(" ")) {
            throw new IllegalArgumentException("Command name cannot contain spaces: " + name);
        }
        this.aliases.forEach(alias -> {
            if (alias.contains(" "))
                throw new IllegalArgumentException("Command alias cannot contain spaces: " + alias);
        });
    }

    public AbstractCommand(String name, String description, String usage) {
        this(name, Collections.emptyList(), description, usage);
    }


    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final List<String> getAliases() {
        return Collections.unmodifiableList(aliases);
    }

    @Override
    public final String getDescription() {
        return description;
    }

    @Override
    public final String getUsage() {
        return usage;
    }

    @Override
    public abstract void execute(String[] args) throws CommandException;

    /**
     * Helper method to send usage instructions to the player
     */
    protected void sendUsage() {
        sendClientMessage("Usage: " + ClientLoader.COMMAND_PREFIX + getName() + " " + getUsage(), true);
    }

    /**
     * Helper method to send error messages
     */
    protected void sendError(String message) {
        sendClientMessage(message, true);
    }

    /**
     * Helper method to send success/info messages
     */
    protected void sendSuccess(String message) {
        sendClientMessage(message, false);
    }
}
