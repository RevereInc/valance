package dev.revere.valance.event.type.game;

import dev.revere.valance.event.IEvent;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public class ClientTickEvent implements IEvent {
    public static ClientTickEvent INSTANCE = new ClientTickEvent();
}