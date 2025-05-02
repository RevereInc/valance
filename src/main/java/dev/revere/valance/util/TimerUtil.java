package dev.revere.valance.util;

/**
 * @author Remi
 * @project valance
 * @date 5/2/2025
 */
public class TimerUtil {

    private long lastResetTimeMillis;

    /**
     * Creates a new Stopwatch and initializes its last reset time to the current time.
     */
    public TimerUtil() {
        reset();
    }

    /**
     * Checks if a specified delay (in milliseconds) has passed since the last reset.
     *
     * @param delayMillis The delay duration in milliseconds.
     * @return true if the elapsed time is greater than or equal to the delay, false otherwise.
     */
    public boolean hasElapsed(long delayMillis) {
        return System.currentTimeMillis() - lastResetTimeMillis >= delayMillis;
    }

    /**
     * Resets the stopwatch's timer to the current system time.
     */
    public void reset() {
        this.lastResetTimeMillis = System.currentTimeMillis();
    }

    /**
     * Gets the total time elapsed in milliseconds since the last reset.
     *
     * @return The elapsed time in milliseconds.
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - this.lastResetTimeMillis;
    }

    /**
     * Gets the timestamp (in milliseconds since epoch) when the stopwatch was last reset.
     * @return The last reset time in milliseconds.
     */
    public long getLastResetTimeMillis() {
        return lastResetTimeMillis;
    }

    /**
     * Manually sets the last reset time. Useful for synchronizing or specific timing scenarios.
     *
     * @param timeMillis The time in milliseconds to set as the last reset time.
     */
    public void setLastResetTimeMillis(long timeMillis) {
        this.lastResetTimeMillis = timeMillis;
    }
}
