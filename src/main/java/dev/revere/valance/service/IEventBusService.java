package dev.revere.valance.service;

import dev.revere.valance.core.lifecycle.IService;
import dev.revere.valance.event.IEvent;
import dev.revere.valance.event.Listener;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public interface IEventBusService extends IService {
    /**
     * Registers a listener object with the event bus.
     *
     * @param listener The listener to register.
     */
    void register(Listener listener);

    /**
     * Unregisters a listener object from the event bus.
     *
     * @param listener The listener to unregister.
     */
    void unregister(Listener listener);

    /**
     * Posts an event to the event bus.
     *
     * @param event The event to post.
     * @param <T>   The type of the event.
     * @return The posted event.
     */
    <T extends IEvent> T post(T event);
}