package dev.revere.valance.service;

import dev.revere.valance.core.lifecycle.IService;

import java.util.List;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public interface IConfigService extends IService {
    /**
     * Loads configuration data from the specified profile name.
     * Applies module enabled states and setting values.
     * If the profile doesn't exist, it might load defaults or do nothing.
     *
     * @param profileName The name of the configuration profile (e.g., "default", "pvp").
     * @return true if loading was successful (or file didn't exist but defaults applied), false on error.
     */
    boolean loadConfig(String profileName);

    /**
     * Saves the current client state (module enabled/disabled, setting values)
     * to the specified profile name. Creates the file/directory if needed.
     *
     * @param profileName The name of the configuration profile.
     * @return true if saving was successful, false on error.
     */
    boolean saveConfig(String profileName);

    /**
     * @return The name of the currently active configuration profile.
     */
    String getCurrentProfile();

    /**
     * Sets the active configuration profile. Might trigger a load.
     *
     * @param profileName The name of the profile to activate.
     */
    void setCurrentProfile(String profileName);

    /**
     * @return A list of available configuration profile names found on disk.
     */
    List<String> listProfiles();

    /**
     * Deletes a specific configuration profile. Use with caution!
     */
    boolean deleteProfile(String profileName);

    /**
     * Saves the currently active profile. Convenience method.
     */
    boolean saveCurrentConfig();
}