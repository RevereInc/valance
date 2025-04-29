package dev.revere.valance;

import dev.revere.valance.core.ClientContext;
import dev.revere.valance.core.exception.ClientInitializationException;
import dev.revere.valance.core.lifecycle.IService;

import java.util.Optional;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public final class ClientLoader {

    // --- Client Metadata ---
    public static final String CLIENT_NAME = "Valance";
    public static final String CLIENT_VERSION = "1.0.0";
    public static final String MINECRAFT_VERSION = "1.8.9";
    public static final String COMMAND_PREFIX = ".";

    private static ClientContext context = null;
    private static volatile boolean initialized = false;
    private static volatile boolean initializing = false;
    private static volatile boolean shuttingDown = false;

    private ClientLoader() {
    }

    /**
     * Initializes the client.
     * This method is synchronized to prevent concurrent initialization attempts.
     */
    public static synchronized void initialize() {
        if (initialized || initializing) {
            System.err.println("[" + CLIENT_NAME + ":Loader] [WARN] Initialization already completed or in progress.");
            return;
        }
        initializing = true;
        System.out.println("\n========================================");
        System.out.println(" Initializing " + CLIENT_NAME + " v" + CLIENT_VERSION + " for MC " + MINECRAFT_VERSION);
        System.out.println("========================================");

        try {
            ClientContext newContext = new ClientContext();
            newContext.initialize();
            context = newContext;
            initialized = true;
            System.out.println("========================================");
            System.out.println(" " + CLIENT_NAME + " Initialized Successfully!");
            System.out.println("========================================\n");
        } catch (ClientInitializationException e) {
            System.err.println("\n[" + CLIENT_NAME + ":Loader] [FATAL] CLIENT INITIALIZATION FAILED!");
        } catch (Throwable t) {
            System.err.println("\n[" + CLIENT_NAME + ":Loader] [FATAL] UNEXPECTED ERROR DURING INITIALIZATION!");
            t.printStackTrace();
        } finally {
            initializing = false;
        }
    }

    /**
     * Shuts down the client.
     * This method is synchronized.
     */
    public static synchronized void shutdown() {
        if (!initialized || shuttingDown) {
            if (!initialized)
                System.err.println("[" + CLIENT_NAME + ":Loader] [WARN] Shutdown called but client was not initialized.");
            if (shuttingDown) System.err.println("[" + CLIENT_NAME + ":Loader] [WARN] Shutdown already in progress.");
            return;
        }
        if (context == null) {
            System.err.println("[" + CLIENT_NAME + ":Loader] [ERROR] Shutdown called, client marked initialized but context is null!");
            initialized = false;
            return;
        }
        shuttingDown = true;
        System.out.println("\n========================================");
        System.out.println(" Shutting Down " + CLIENT_NAME);
        System.out.println("========================================");

        try {
            context.shutdown();
        } catch (Throwable t) {
            System.err.println("[" + CLIENT_NAME + ":Loader] [ERROR] Unexpected error during ClientContext shutdown:");
            t.printStackTrace();
        } finally {
            context = null;
            initialized = false;
            shuttingDown = false;
            System.out.println("========================================");
            System.out.println(" " + CLIENT_NAME + " Shutdown Complete.");
            System.out.println("========================================\n");
        }
    }

    /**
     * Gets the initialized ClientContext. Primarily for internal use or debugging.
     * Services and modules should receive dependencies via injection, not by fetching the context globally.
     *
     * @return An Optional containing the ClientContext if initialized, otherwise empty.
     */
    public static Optional<ClientContext> getContext() {
        if (initialized && context != null) {
            return Optional.of(context);
        } else {
            if (!initialized && !initializing && !shuttingDown) {
                System.err.println("[" + CLIENT_NAME + ":Loader] [WARN] Attempted to access ClientContext before initialization or after shutdown.");
            }
            return Optional.empty();
        }
    }

    /**
     * Utility method to safely access a specific service after initialization.
     * Usage should be minimized; prefer constructor injection within services/modules.
     *
     * @param serviceInterface The interface class of the desired service.
     * @param <T>              The type of the service.
     * @return An Optional containing the service if available, otherwise empty.
     */
    public static <T extends IService> Optional<T> getService(Class<T> serviceInterface) {
        return getContext().flatMap(ctx -> ctx.getService(serviceInterface));
    }
}