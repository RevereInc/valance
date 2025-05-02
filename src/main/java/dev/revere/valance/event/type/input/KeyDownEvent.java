package dev.revere.valance.event.type.input;

import dev.revere.valance.event.IEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Remi
 * @project valance
 * @date 5/1/2025
 */
@Getter
@Setter
public class KeyDownEvent implements IEvent {
    private int key;

    /**
     * KeyDownEvent constructor to initialize the event.
     *
     * @param key the key that was pressed
     */
    public KeyDownEvent(int key) {
        this.key = key;
    }
}
