package dev.revere.valance.core.lifecycle;

import dev.revere.valance.core.ClientContext;
import dev.revere.valance.core.exception.ServiceException;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public interface IService {
    /**
     * Called by the ClientContext *after* the service instance has been created
     * and its *core* dependencies are available (but not necessarily fully initialized yet).
     * Use for essential setup that doesn't rely on the full initialization of other services.
     *
     * @param context The client context, providing access to other services.
     * @throws ServiceException If setup fails.
     */
    default void setup(ClientContext context) throws ServiceException {
        // Default implementation: no-op
    }

    /**
     * Called by the ClientContext *after* all service instances have been created and setup.
     * Use this for initialization logic that might require other services to be operational.
     *
     * @param context The client context.
     * @throws ServiceException If initialization fails.
     */
    void initialize(ClientContext context) throws ServiceException;

    /**
     * Called by the ClientContext during client shutdown.
     * Should release resources, unregister listeners, save state, etc.
     * Called in reverse order of initialization priority.
     *
     * @param context The client context.
     * @throws ServiceException If shutdown fails (errors should be logged but not prevent other shutdowns).
     */
    void shutdown(ClientContext context) throws ServiceException;
}