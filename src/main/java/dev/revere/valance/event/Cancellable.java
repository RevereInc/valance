package dev.revere.valance.event;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public interface Cancellable {
    /**
     * Checks if this event has been cancelled.
     *
     * @return true if the event is cancelled, false otherwise.
     */
    boolean isCancelled();

    /**
     * Sets the cancellation state of this event.
     *
     * @param cancel true to cancel the event, false otherwise.
     */
    void setCancelled(boolean cancel);
}
