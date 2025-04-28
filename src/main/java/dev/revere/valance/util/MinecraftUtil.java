package dev.revere.valance.util;

import dev.revere.valance.ClientLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.ChatComponentText;

import java.util.Optional;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public final class MinecraftUtil {

    private MinecraftUtil() {}

    /**
     * Gets the Minecraft instance.
     *
     * @return The Minecraft instance.
     * @throws IllegalStateException if called before Minecraft is initialized.
     */
    public static Minecraft mc() {
        Minecraft instance = Minecraft.getMinecraft();
        if (instance == null) {
            throw new IllegalStateException("Minecraft.getMinecraft() returned null! Client is likely not fully initialized.");
        }
        return instance;
    }

    /**
     * Gets the player entity if available.
     *
     * @return An Optional containing the player entity, or empty if not available.
     */
    public static Optional<EntityPlayerSP> getPlayer() {
        try {
            return Optional.ofNullable(mc().thePlayer);
        } catch (IllegalStateException e) {
            return Optional.empty(); // MC not ready
        }
    }

    /**
     * Gets the world client if available.
     *
     * @return An Optional containing the world client, or empty if not available.
     */
    public static Optional<WorldClient> getWorld() {
        try {
            return Optional.ofNullable(mc().theWorld);
        } catch (IllegalStateException e) {
            return Optional.empty(); // MC not ready
        }
    }

    /**
     * Checks if the player is in a valid game state (logged in, world loaded).
     *
     * @return true if the player and world exist, false otherwise.
     */
    public static boolean isInGame() {
        try {
            return mc().thePlayer != null && mc().theWorld != null;
        } catch (IllegalStateException e) {
            return false; // MC not ready
        }
    }

    /**
     * Sends a chat message to the player with a client prefix.
     * Ensures the message is sent only when the player is available.
     *
     * @param message The message content (without prefix).
     * @param error   Whether to format the message as an error (e.g., red color).
     */
    public static void sendClientMessage(String message, boolean error) {
        getPlayer().ifPresent(player -> {
            String prefix = "\u00A77[\u00A7c" + ClientLoader.CLIENT_NAME + "\u00A77] ";
            String color = error ? "\u00A7c" : "\u00A7f";
            player.addChatMessage(new ChatComponentText(prefix + color + message));
        });
    }

    /**
     * Sends a normal chat message to the player with a client prefix.
     *
     * @param message The message content (without prefix).
     */
    public static void sendClientMessage(String message) {
        sendClientMessage(message, false);
    }
}