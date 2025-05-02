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
public class KeyUpEvent implements IEvent {
    private int key;

    /**
     * KeyUpEvent constructor to initialize the event.
     *
     * @param key the key that was released
     */
    public KeyUpEvent(int key) {
        this.key = key;
    }
}
