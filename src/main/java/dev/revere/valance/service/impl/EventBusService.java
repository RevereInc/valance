package dev.revere.valance.service.impl;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.core.ClientContext;
import dev.revere.valance.core.annotation.Service;
import dev.revere.valance.core.exception.ServiceException;
import dev.revere.valance.event.EventBus;
import dev.revere.valance.event.IEvent;
import dev.revere.valance.event.Listener;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.util.Logger;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@Service(provides = IEventBusService.class, priority = -100)
public class EventBusService implements IEventBusService {
    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":EventBusService]";

    private EventBus eventBus;

    public EventBusService() {
        Logger.info(LOG_PREFIX, "Constructed.");
    }

    @Override
    public void setup(ClientContext context) throws ServiceException {
        Logger.info(LOG_PREFIX, "Setting up...");
        if (this.eventBus == null) {
            this.eventBus = new EventBus();
            Logger.info(LOG_PREFIX, "EventBus instance created.");
        } else {
            Logger.warn(LOG_PREFIX, "EventBus instance already exists during setup.");
        }
        Logger.info(LOG_PREFIX, "Setup complete.");
    }


    @Override
    public void initialize(ClientContext context) throws ServiceException {
        Logger.info(LOG_PREFIX, "Initializing...");
        if (this.eventBus == null) {
            throw new ServiceException("EventBus instance was null during initialization!");
        }
        Logger.info(LOG_PREFIX, "Initialized.");
    }

    @Override
    public void shutdown(ClientContext context) throws ServiceException {
        Logger.info(LOG_PREFIX, "Shutting down...");
        this.eventBus = null;
        Logger.info(LOG_PREFIX, "Shutdown complete.");
    }

    @Override
    public void register(Listener listener) {
        if (eventBus == null) {
            Logger.error(LOG_PREFIX, "Cannot register listener, EventBus is not available!");
            return;
        }
        this.eventBus.register(listener);
    }

    @Override
    public void unregister(Listener listener) {
        if (eventBus == null) {
            Logger.warn(LOG_PREFIX, "Cannot unregister listener, EventBus is not available (likely during shutdown).");
            return;
        }
        this.eventBus.unregister(listener);
    }

    @Override
    public <T extends IEvent> T post(T event) {
        if (eventBus == null) {
            Logger.error(LOG_PREFIX, "Cannot post event, EventBus is not available!");
            return event;
        }
        return this.eventBus.post(event);
    }
}