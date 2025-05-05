package dev.revere.valance.service;

import dev.revere.valance.alt.Alt;
import dev.revere.valance.core.lifecycle.IService;

import java.util.List;

/**
 * Service interface for managing Minecraft alternate accounts (Alts).
 * Provides methods for adding, removing, retrieving, and validating alts,
 * as well as managing status messages related to alt operations.
 *
 * @author Remi
 * @project valance
 * @date 5/5/2025 (Updated 2025-05-06)
 */
public interface IAltService extends IService {

    /**
     * Adds a new alternate account to the manager.
     * Implementations should handle persistence (saving the alt).
     *
     * @param alt The Alt object representing the account to add. Must not be null.
     */
    void addAlt(Alt alt);

    /**
     * Removes the specified alternate account from the manager.
     * Implementations should handle persistence (updating the saved list).
     * Removal might be based on object reference, UUID, or username comparison.
     *
     * @param alt The Alt object representing the account to remove. Must not be null.
     * @return {@code true} if the alt was found and removed, {@code false} otherwise.
     */
    boolean removeAlt(Alt alt);

    /**
     * Checks if a given username conforms to the basic syntax rules
     * often used for cracked (offline mode) Minecraft accounts.
     * Note: This does not guarantee the username is actually usable, only that it fits common patterns.
     *
     * @param username The username string to validate.
     * @return {@code true} if the username seems valid for cracked usage, {@code false} otherwise.
     */
    boolean isValidCrackedAlt(String username);

    /**
     * Retrieves the current status message related to alt operations
     * (e.g., "Logging in...", "Login failed.", "Alt added.").
     *
     * @return The current status message, potentially empty or null.
     */
    String getStatus();

    /**
     * Sets the status message to be displayed, often related to the result
     * of the last alt operation.
     *
     * @param status The new status message. Null may be treated as an empty string.
     */
    void setStatus(String status);

    /**
     * Retrieves the list of currently managed alternate accounts.
     * The returned list itself should never be null, but it might be empty.
     *
     * @return A {@link List} containing the managed {@link Alt} objects. Never null.
     */
    List<Alt> getAlts();
}