package dev.revere.valance.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import dev.revere.valance.ClientLoader;
import dev.revere.valance.core.ClientContext;
import dev.revere.valance.core.annotation.Inject;
import dev.revere.valance.core.annotation.Service;
import dev.revere.valance.core.exception.ServiceException;
import dev.revere.valance.input.BindType;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.properties.Property;
import dev.revere.valance.service.IConfigService;
import dev.revere.valance.service.IModuleManager;
import dev.revere.valance.util.LoggerUtil;
import dev.revere.valance.util.MinecraftUtil;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages loading and saving of configuration profiles using the new Property system.
 * Inspired by Virago's ConfigService logic for handling property hierarchies and types.
 *
 * @author Remi (Original), Gemini (Refactored)
 * @project valance
 * @date 5/5/2025
 */
@Service(provides = IConfigService.class, priority = 10)
public class ConfigManagerService implements IConfigService {
    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":ConfigManager]";
    private static final String CONFIG_DIR_NAME = ClientLoader.CLIENT_NAME + "/configs";
    private static final String CONFIG_FILE_EXTENSION = ".json";
    private final IModuleManager moduleManager;
    private final Gson gson;
    private Path configDirectory;
    private String currentProfile = null;

    private static class ModuleConfigData {
        boolean enabled;
        Integer keybind = Keyboard.KEY_NONE;
        String bindType = BindType.TOGGLE.name();
        Map<String, Object> properties = new LinkedHashMap<>();
    }

    private static final Type CONFIG_TYPE = new TypeToken<Map<String, ModuleConfigData>>() {
    }.getType();

    @Inject
    public ConfigManagerService(IModuleManager moduleManager) {
        LoggerUtil.info(LOG_PREFIX, "Constructed.");
        this.moduleManager = Objects.requireNonNull(moduleManager, "ModuleManager cannot be null");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    @Override
    public void setup(ClientContext context) throws ServiceException {
        LoggerUtil.info(LOG_PREFIX, "Setting up...");
        try {
            Path mcDir = MinecraftUtil.mc().mcDataDir.toPath();
            configDirectory = mcDir.resolve(CONFIG_DIR_NAME);
            Files.createDirectories(configDirectory);
            LoggerUtil.info(LOG_PREFIX, "Using config directory: " + configDirectory);
        } catch (Exception e) {
            LoggerUtil.error(LOG_PREFIX, "Failed to create or access config directory: " + configDirectory, e);
            throw new ServiceException("Failed to initialize config directory", e);
        }
        LoggerUtil.info(LOG_PREFIX, "Setup complete.");
    }

    @Override
    public void initialize(ClientContext context) throws ServiceException {
        LoggerUtil.info(LOG_PREFIX, "Initialized.");
    }

    @Override
    public void shutdown(ClientContext context) throws ServiceException {
        LoggerUtil.info(LOG_PREFIX, "Shutting down...");
    }

    @Override
    public boolean loadConfig(String profileName) {
        String sanitizedName = sanitizeProfileName(profileName);
        if (sanitizedName == null) return false;

        Path configFile = getConfigFile(sanitizedName);

        if (!Files.exists(configFile)) {
            LoggerUtil.error(LOG_PREFIX, "Profile not found: " + configFile.getFileName());
            return false;
        }

        LoggerUtil.info(LOG_PREFIX, "Loading config: " + configFile.getFileName());
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            Map<String, ModuleConfigData> loadedData = gson.fromJson(reader, CONFIG_TYPE);
            if (loadedData == null) {
                LoggerUtil.error(LOG_PREFIX, "Failed to parse config file (null content): " + configFile.getFileName());
                return false;
            }
            applyConfigData(loadedData);
            this.currentProfile = sanitizedName;
            LoggerUtil.info(LOG_PREFIX, "Successfully loaded profile: " + this.currentProfile);
            return true;
        } catch (IOException | JsonSyntaxException e) {
            LoggerUtil.error(LOG_PREFIX, "Failed to load/parse config file: " + configFile.getFileName(), e);
            return false;
        } catch (Exception e) {
            LoggerUtil.error(LOG_PREFIX, "Unexpected error applying config: " + configFile.getFileName(), e);
            return false;
        }
    }

    /**
     * Applies loaded configuration data to the modules.
     * Resets modules not found in the config to their default state.
     *
     * @param loadedData The loaded configuration data.
     */
    private void applyConfigData(Map<String, ModuleConfigData> loadedData) {
        Set<String> modulesInConfig = new HashSet<>(loadedData.keySet());
        List<AbstractModule> currentModules = moduleManager.getModulesOfType(AbstractModule.class);

        for (AbstractModule module : currentModules) {
            String moduleName = module.getName();
            ModuleConfigData data = loadedData.get(moduleName);

            if (data != null) {
                module.setEnabled(data.enabled);
                module.setKey(data.keybind != null ? data.keybind : Keyboard.KEY_NONE);
                try {
                    module.setBindType(data.bindType != null ? BindType.valueOf(data.bindType.toUpperCase(Locale.ROOT)) : BindType.TOGGLE);
                } catch (IllegalArgumentException e) {
                    LoggerUtil.warn(LOG_PREFIX, "Invalid bind type '" + data.bindType + "' for module '" + moduleName + "'. Using TOGGLE.");
                    module.setBindType(BindType.TOGGLE);
                }

                applyModuleProperties(module, data.properties != null ? data.properties : Collections.emptyMap());

                modulesInConfig.remove(moduleName);
            } else {
                LoggerUtil.debug(LOG_PREFIX, "Module '" + moduleName + "' not in config..");
            }
        }

        if (!modulesInConfig.isEmpty()) {
            LoggerUtil.warn(LOG_PREFIX, "Config contained data for unloaded modules: " + String.join(", ", modulesInConfig));
        }
    }

    /**
     * Applies loaded properties to the module, resetting to default if not found or mismatched.
     *
     * @param module           The module to apply properties to.
     * @param loadedProperties The loaded properties from the config.
     */
    private void applyModuleProperties(AbstractModule module, Map<String, Object> loadedProperties) {
        for (Property<?> property : module.getProperties()) {
            String path = property.getPath();
            Object loadedValue = loadedProperties.get(path);

            if (loadedValue != null) {
                if (!trySetPropertyValue(property, loadedValue)) {
                    LoggerUtil.warn(LOG_PREFIX, "Failed to set property '" + path + "' for module '" + module.getName() + "'.");
                }
            }
        }
    }

    /**
     * Attempts to set the property value based on the loaded value.
     * Handles type conversions and logging for mismatches.
     *
     * @param property    The property to set.
     * @param loadedValue The loaded value from the config.
     * @return true if successful, false otherwise.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean trySetPropertyValue(Property<?> property, Object loadedValue) {
        Object currentValue = property.getValue();
        String path = property.getPath();

        try {
            if (currentValue instanceof Boolean && loadedValue instanceof Boolean) {
                ((Property<Boolean>) property).setValue((Boolean) loadedValue);
            } else if (currentValue instanceof Integer && loadedValue instanceof Number) {
                ((Property<Integer>) property).setValue(((Number) loadedValue).intValue());
            } else if (currentValue instanceof Double && loadedValue instanceof Number) {
                ((Property<Double>) property).setValue(((Number) loadedValue).doubleValue());
            } else if (currentValue instanceof Float && loadedValue instanceof Number) {
                ((Property<Float>) property).setValue(((Number) loadedValue).floatValue());
            } else if (currentValue instanceof Long && loadedValue instanceof Number) {
                ((Property<Long>) property).setValue(((Number) loadedValue).longValue());
            } else if (currentValue instanceof String) {
                ((Property<String>) property).setValue(String.valueOf(loadedValue));
            } else if (currentValue instanceof Enum && loadedValue instanceof String) {
                Enum currentEnum = (Enum) currentValue;
                Class enumClass = currentEnum.getDeclaringClass();
                try {
                    Enum enumValue = Enum.valueOf(enumClass, ((String) loadedValue).toUpperCase(Locale.ROOT));
                    ((Property<Enum>) property).setValue(enumValue);
                } catch (IllegalArgumentException e) {
                    LoggerUtil.warn(LOG_PREFIX, "Unknown enum constant '" + loadedValue + "' for property '" + path + "' of type " + enumClass.getSimpleName());
                    return false;
                }
            } else if (currentValue instanceof Color && loadedValue instanceof String) {
                Color color = parseColorString((String) loadedValue);
                if (color != null) {
                    ((Property<Color>) property).setValue(color);
                } else {
                    LoggerUtil.warn(LOG_PREFIX, "Could not parse color string '" + loadedValue + "' for property '" + path + "'");
                    return false;
                }
            } else {
                LoggerUtil.warn(LOG_PREFIX, "Type mismatch for property '" + path + "'. Expected compatible type for " + currentValue.getClass().getSimpleName() + ", but got " + loadedValue.getClass().getSimpleName() + " from config.");
                return false;
            }
            return true;
        } catch (ClassCastException e) {
            LoggerUtil.error(LOG_PREFIX, "Failed to cast value for property '" + path + "'. Loaded type: " + loadedValue.getClass().getSimpleName() + ", Expected type: " + currentValue.getClass().getSimpleName(), e);
            return false;
        } catch (Exception e) {
            LoggerUtil.error(LOG_PREFIX, "Error setting property '" + path + "' with value '" + loadedValue + "'", e);
            return false;
        }
    }

    @Override
    public boolean saveConfig(String profileName) {
        String sanitizedName = sanitizeProfileName(profileName);
        if (sanitizedName == null) return false;

        Path configFile = getConfigFile(sanitizedName);
        LoggerUtil.info(LOG_PREFIX, "Saving config: " + configFile.getFileName());

        Map<String, ModuleConfigData> configData = buildConfigDataMap();

        try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
            gson.toJson(configData, writer);
            return true;
        } catch (IOException | JsonIOException e) {
            LoggerUtil.error(LOG_PREFIX, "Failed to write config file: " + configFile.getFileName(), e);
            return false;
        } catch (Exception e) {
            LoggerUtil.error(LOG_PREFIX, "Unexpected error saving config: " + configFile.getFileName(), e);
            return false;
        }
    }

    /**
     * Builds a map of module configuration data for saving.
     * Sorts modules and properties for consistent output.
     *
     * @return A map of module names to their configuration data.
     */
    private Map<String, ModuleConfigData> buildConfigDataMap() {
        Map<String, ModuleConfigData> data = new LinkedHashMap<>();
        List<AbstractModule> modules = moduleManager.getModulesOfType(AbstractModule.class);
        modules.sort(Comparator.comparing(AbstractModule::getName));

        for (AbstractModule module : modules) {
            ModuleConfigData moduleData = new ModuleConfigData();
            moduleData.enabled = module.isEnabled();
            moduleData.keybind = module.getKey();
            moduleData.bindType = module.getBindType().name();

            List<Property<?>> properties = new ArrayList<>(module.getProperties());
            properties.sort(Comparator.comparing(Property::getPath));

            for (Property<?> property : properties) {
                Object valueToSave = property.getValue();
                String path = property.getPath();

                if (valueToSave instanceof Enum) {
                    valueToSave = ((Enum<?>) valueToSave).name();
                } else if (valueToSave instanceof Color) {
                    valueToSave = formatColorToString((Color) valueToSave);
                }

                moduleData.properties.put(path, valueToSave);
            }
            data.put(module.getName(), moduleData);
        }
        return data;
    }

    @Override
    public String getCurrentProfile() {
        return currentProfile;
    }

    @Override
    public void setCurrentProfile(String profileName) {
        String sanitizedName = sanitizeProfileName(profileName);
        if (sanitizedName == null) {
            LoggerUtil.error(LOG_PREFIX, "Invalid profile name provided: '" + profileName + "'.");
            return;
        }

        if (!Objects.equals(this.currentProfile, sanitizedName)) {
            LoggerUtil.info(LOG_PREFIX, "Attempting to switch profile from '" + this.currentProfile + "' to '" + sanitizedName + "'...");
            boolean loadSuccess = loadConfig(sanitizedName);

            if (!loadSuccess) {
                LoggerUtil.error(LOG_PREFIX, "Failed to load profile '" + sanitizedName + "'. Keeping current profile: '" + this.currentProfile + "'");
            }
        } else {
            LoggerUtil.debug(LOG_PREFIX, "Profile '" + sanitizedName + "' is already the current profile (or both are null).");
        }
    }

    @Override
    public List<String> listProfiles() {
        if (configDirectory == null) return Collections.emptyList();
        try (Stream<Path> stream = Files.list(configDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.toLowerCase().endsWith(CONFIG_FILE_EXTENSION))
                    .map(name -> name.substring(0, name.length() - CONFIG_FILE_EXTENSION.length()))
                    .filter(name -> sanitizeProfileName(name) != null)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LoggerUtil.error(LOG_PREFIX, "Failed to list config profiles in: " + configDirectory, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean deleteProfile(String profileName) {
        String sanitizedName = sanitizeProfileName(profileName);
        if (sanitizedName == null) {
            LoggerUtil.error(LOG_PREFIX, "Cannot delete profile with invalid name: '" + profileName + "'");
            return false;
        }

        if (Objects.equals(currentProfile, sanitizedName)) {
            LoggerUtil.error(LOG_PREFIX, "Cannot delete the active profile ('" + currentProfile + "'). Switch first or use null profile.");
            return false;
        }

        Path configFile = getConfigFile(sanitizedName);
        try {
            if (Files.deleteIfExists(configFile)) {
                LoggerUtil.info(LOG_PREFIX, "Deleted profile: " + sanitizedName);
                return true;
            } else {
                LoggerUtil.warn(LOG_PREFIX, "Profile not found for deletion: " + sanitizedName);
                return false;
            }
        } catch (IOException | SecurityException e) {
            LoggerUtil.error(LOG_PREFIX, "Failed to delete profile: " + configFile.getFileName(), e);
            return false;
        }
    }

    @Override
    public boolean saveCurrentConfig() {
        if (this.currentProfile == null) {
            LoggerUtil.info(LOG_PREFIX, "No active profile set. Use '.c save <name>' to save the current configuration.");
            return false;
        }
        boolean success = saveConfig(this.currentProfile);
        if (success) {
            LoggerUtil.info(LOG_PREFIX, "Saved current state to active profile: " + this.currentProfile);
        }
        return success;
    }

    private String sanitizeProfileName(String profileName) {
        if (profileName == null || profileName.trim().isEmpty()) {
            LoggerUtil.warn(LOG_PREFIX, "Profile name is null or empty");
            return null;
        }
        String trimmed = profileName.trim();
        if (!trimmed.matches("^[a-zA-Z0-9_-]+$")) {
            LoggerUtil.error(LOG_PREFIX, "Invalid profile name: '" + profileName + "'. Use alphanumeric, underscore, or hyphen.");
            return null;
        }
        return trimmed;
    }

    private Path getConfigFile(String sanitizedProfileName) {
        return configDirectory.resolve(sanitizedProfileName + CONFIG_FILE_EXTENSION);
    }

    private Color parseColorString(String value) {
        if (value == null || value.isEmpty()) return null;
        value = value.trim();
        try {
            if (value.startsWith("#")) {
                if (value.length() == 7) {
                    return new Color(Integer.parseInt(value.substring(1, 3), 16), Integer.parseInt(value.substring(3, 5), 16), Integer.parseInt(value.substring(5, 7), 16));
                } else if (value.length() == 9) {
                    return new Color(Integer.parseInt(value.substring(1, 3), 16), Integer.parseInt(value.substring(3, 5), 16), Integer.parseInt(value.substring(5, 7), 16), Integer.parseInt(value.substring(7, 9), 16));
                }
            } else if (value.contains(":")) {
                String[] parts = value.split(":");
                int r = Integer.parseInt(parts[0]);
                int g = Integer.parseInt(parts[1]);
                int b = Integer.parseInt(parts[2]);
                int a = (parts.length == 4) ? Integer.parseInt(parts[3]) : 255;
                if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255 || a < 0 || a > 255) return null;
                return new Color(r, g, b, a);
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) { /* Ignore */ }
        return null;
    }

    private String formatColorToString(Color color) {
        if (color == null) return null;
        return String.format("#%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }
}