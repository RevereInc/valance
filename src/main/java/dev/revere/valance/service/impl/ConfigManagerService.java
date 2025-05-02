package dev.revere.valance.service.impl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import dev.revere.valance.ClientLoader;
import dev.revere.valance.core.ClientContext;
import dev.revere.valance.core.annotation.Inject;
import dev.revere.valance.core.annotation.Service;
import dev.revere.valance.core.exception.ServiceException;
import dev.revere.valance.module.api.IModule;
import dev.revere.valance.service.IConfigService;
import dev.revere.valance.service.IModuleManager;
import dev.revere.valance.settings.Setting;
import dev.revere.valance.util.Logger;
import dev.revere.valance.util.MinecraftUtil;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@Service(provides = IConfigService.class, priority = 10)
public class ConfigManagerService implements IConfigService {
    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":ConfigManager]";
    private static final String CONFIG_DIR_NAME = ClientLoader.CLIENT_NAME + "/configs";
    private static final String CONFIG_FILE_EXTENSION = ".json";
    private static final String DEFAULT_PROFILE_NAME = "default";

    private final IModuleManager moduleManager;
    private final Gson gson;
    private Path configDirectory;
    private String currentProfile = DEFAULT_PROFILE_NAME;

    @Inject
    public ConfigManagerService(IModuleManager moduleManager) {
        Logger.info(LOG_PREFIX, "Constructed.");
        this.moduleManager = Objects.requireNonNull(moduleManager, "ModuleManager cannot be null");
        this.gson = createGsonInstance();
    }

    private Gson createGsonInstance() {
        return new GsonBuilder()
                .registerTypeAdapterFactory(new EnumTypeAdapterFactory())
                .setPrettyPrinting()
                .serializeNulls()
                .disableHtmlEscaping()
                .create();
    }

    @Override
    public void setup(ClientContext context) throws ServiceException {
        Logger.info(LOG_PREFIX, "Setting up...");
        try {
            Path mcDir = MinecraftUtil.mc().mcDataDir.toPath();
            configDirectory = mcDir.resolve(CONFIG_DIR_NAME);
            if (!Files.exists(configDirectory)) {
                Files.createDirectories(configDirectory);
                Logger.info(LOG_PREFIX, "Created config directory: " + configDirectory);
            } else {
                Logger.info(LOG_PREFIX, "Using config directory: " + configDirectory);
            }
        } catch (Exception e) {
            Logger.error(LOG_PREFIX, "Failed to create or access config directory: " + configDirectory);
            throw new ServiceException("Failed to initialize config directory", e);
        }
        Logger.info(LOG_PREFIX, "Setup complete.");
    }

    @Override
    public void initialize(ClientContext context) throws ServiceException {
        Logger.info(LOG_PREFIX, "Initializing...");
        if (!loadConfig(DEFAULT_PROFILE_NAME)) {
            Logger.warn(LOG_PREFIX, "Failed to load default config profile on startup. Defaults will be used.");
            Logger.info(LOG_PREFIX, "Attempting to save initial default configuration...");
            saveConfig(DEFAULT_PROFILE_NAME);
        } else {
            Logger.info(LOG_PREFIX, "Successfully loaded profile: " + DEFAULT_PROFILE_NAME);
        }
        Logger.info(LOG_PREFIX, "Initialized.");
    }

    @Override
    public void shutdown(ClientContext context) throws ServiceException {
        Logger.info(LOG_PREFIX, "Shutting down...");
        if (!saveCurrentConfig()) {
            Logger.error(LOG_PREFIX, "Failed to save current config profile '" + currentProfile + "' during shutdown!");
        } else {
            Logger.info(LOG_PREFIX, "Saved current profile '" + currentProfile + "' successfully.");
        }
        Logger.info(LOG_PREFIX, "Shutdown complete.");
    }

    @Override
    public boolean loadConfig(String profileName) {
        Path configFile = getConfigFile(profileName);
        if (!Files.exists(configFile)) {
            Logger.info(LOG_PREFIX, "Config file not found: " + configFile.getFileName() + ". Using default module states and settings.");
            moduleManager.getModules().forEach(m -> m.getSettings().forEach(Setting::resetToDefault));
            return true;
        }

        Logger.info(LOG_PREFIX, "Loading config from: " + configFile.getFileName());
        Type configType = new TypeToken<Map<String, ModuleConfigData>>() {
        }.getType();

        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            Map<String, ModuleConfigData> loadedData = gson.fromJson(reader, configType);
            if (loadedData == null || loadedData.isEmpty()) {
                Logger.warn(LOG_PREFIX, "Loaded config file was empty or invalid: " + configFile.getFileName());
                return false;
            }

            applyLoadedConfig(loadedData);
            this.currentProfile = profileName;
            return true;

        } catch (IOException e) {
            Logger.error(LOG_PREFIX, "IOException while reading config file: " + configFile.getFileName());
            e.printStackTrace();
            return false;
        } catch (JsonSyntaxException e) {
            Logger.error(LOG_PREFIX, "JSON Syntax error in config file: " + configFile.getFileName());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            Logger.error(LOG_PREFIX, "Unexpected error applying loaded config: " + configFile.getFileName());
            e.printStackTrace();
            return false;
        }
    }

    private void applyLoadedConfig(Map<String, ModuleConfigData> loadedData) {
        Logger.info(LOG_PREFIX, "Applying loaded configuration data...");
        int modulesApplied = 0;
        int settingsApplied = 0;

        Set<String> modulesInConfig = new HashSet<>(loadedData.keySet());

        for (IModule module : moduleManager.getModules()) {
            String moduleKey = module.getName();
            ModuleConfigData moduleData = loadedData.get(moduleKey);

            if (moduleData != null) {
                if (module.isEnabled() != moduleData.enabled) {
                    try {
                        module.setEnabled(moduleData.enabled);
                    } catch (Exception e) {
                        Logger.error(LOG_PREFIX, "Failed to apply enabled state (" + moduleData.enabled + ") for module: " + moduleKey);
                        e.printStackTrace();
                    }
                }

                if (moduleData.settings != null) {
                    for (Setting<?> setting : module.getSettings()) {
                        Object loadedValue = moduleData.settings.get(setting.getName());
                        if (loadedValue != null) {
                            if (setting.setValueFromObject(loadedValue)) {
                                settingsApplied++;
                            } else {
                                Logger.warn(LOG_PREFIX, "Failed to apply loaded value for setting '" + setting.getName() + "' in module '" + moduleKey + "'. Using default.");
                                setting.resetToDefault();
                            }
                        } else {
                            Logger.info(LOG_PREFIX, "[DEBUG] Setting '" + setting.getName() + "' not found in config for module '" + moduleKey + "'. Resetting to default.");
                            setting.resetToDefault();
                        }
                    }
                } else {
                    module.getSettings().forEach(Setting::resetToDefault);
                }
                modulesApplied++;
                modulesInConfig.remove(moduleKey);

            } else {
                Logger.info(LOG_PREFIX, "[DEBUG] Module '" + moduleKey + "' not found in config. Disabling and resetting settings.");
                if (module.isEnabled()) {
                    try {
                        module.setEnabled(false);
                    } catch (Exception ignored) {
                    }
                }
                module.getSettings().forEach(Setting::resetToDefault);
            }
        }

        if (!modulesInConfig.isEmpty()) {
            Logger.warn(LOG_PREFIX, "Config contained data for modules not currently loaded: " + modulesInConfig);
        }
        Logger.info(LOG_PREFIX, "Applied configuration for " + modulesApplied + " modules and " + settingsApplied + " settings.");
    }

    @Override
    public boolean saveConfig(String profileName) {
        Path configFile = getConfigFile(profileName);
        Logger.info(LOG_PREFIX, "Saving config to: " + configFile.getFileName());

        Map<String, ModuleConfigData> configData = buildConfigData();

        try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
            gson.toJson(configData, writer);
            return true;
        } catch (IOException e) {
            Logger.error(LOG_PREFIX, "IOException while writing config file: " + configFile.getFileName());
            e.printStackTrace();
            return false;
        } catch (JsonIOException e) {
            Logger.error(LOG_PREFIX, "Gson IO error while writing config file: " + configFile.getFileName());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            Logger.error(LOG_PREFIX, "Unexpected error saving config: " + configFile.getFileName());
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, ModuleConfigData> buildConfigData() {
        Map<String, ModuleConfigData> data = new LinkedHashMap<>();
        List<IModule> modules = moduleManager.getModules();

        for (IModule module : modules) {
            ModuleConfigData moduleData = new ModuleConfigData();
            moduleData.enabled = module.isEnabled();
            moduleData.settings = new LinkedHashMap<>();

            List<Setting<?>> settings = module.getSettings();
            // todo: Sort settings alphabetically?
            // settings.sort(Comparator.comparing(Setting::getName));

            for (Setting<?> setting : settings) {
                Object valueToSave = setting.getValue();
                if (valueToSave instanceof Enum) {
                    valueToSave = ((Enum<?>) valueToSave).name();
                }
                // TODO: Add special handling for color if needed when writing cgui (e.g., save as hex string or int)
                moduleData.settings.put(setting.getName(), valueToSave);
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
        if (profileName == null || profileName.trim().isEmpty()) {
            Logger.warn(LOG_PREFIX, "Attempted to set null or empty profile name. Using default.");
            profileName = DEFAULT_PROFILE_NAME;
        }
        if (!profileName.matches("^[a-zA-Z0-9_-]+$")) {
            Logger.error(LOG_PREFIX, "Invalid profile name: '" + profileName + "'. Use alphanumeric, underscore, or hyphen.");
            return;
        }

        if (!this.currentProfile.equals(profileName)) {
            Logger.info(LOG_PREFIX, "Switching active profile to: " + profileName);
            // saveCurrentConfig();
            if (loadConfig(profileName)) {
                Logger.info(LOG_PREFIX, "Successfully loaded profile: " + profileName);
            } else {
                Logger.error(LOG_PREFIX, "Failed to load profile '" + profileName + "' during switch. Reverting to '" + this.currentProfile + "'.");
                loadConfig(this.currentProfile);
            }
        }
    }

    @Override
    public List<String> listProfiles() {
        if (configDirectory == null || !Files.isDirectory(configDirectory)) {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.list(configDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(CONFIG_FILE_EXTENSION))
                    .map(name -> name.substring(0, name.length() - CONFIG_FILE_EXTENSION.length()))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            Logger.error(LOG_PREFIX, "Failed to list config profiles in: " + configDirectory);
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public boolean deleteProfile(String profileName) {
        if (DEFAULT_PROFILE_NAME.equalsIgnoreCase(profileName)) {
            Logger.error(LOG_PREFIX, "Cannot delete the default profile.");
            return false;
        }
        if (currentProfile.equalsIgnoreCase(profileName)) {
            Logger.error(LOG_PREFIX, "Cannot delete the currently active profile. Switch profiles first.");
            return false;
        }

        Path configFile = getConfigFile(profileName);
        try {
            boolean deleted = Files.deleteIfExists(configFile);
            if (deleted) {
                Logger.info(LOG_PREFIX, "Deleted profile: " + profileName);
                return true;
            } else {
                Logger.warn(LOG_PREFIX, "Profile not found for deletion: " + profileName);
                return false;
            }
        } catch (IOException | SecurityException e) {
            Logger.error(LOG_PREFIX, "Failed to delete profile file: " + configFile.getFileName());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean saveCurrentConfig() {
        return saveConfig(this.currentProfile);
    }

    private Path getConfigFile(String profileName) {
        return configDirectory.resolve(profileName + CONFIG_FILE_EXTENSION);
    }

    private static class ModuleConfigData {
        boolean enabled;
        Map<String, Object> settings;
    }

    // --- Gson Type Adapter for Enums (Case-Insensitive Loading) ---
    private static class EnumTypeAdapterFactory implements TypeAdapterFactory {
        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            Class<? super T> rawType = typeToken.getRawType();
            if (!rawType.isEnum()) {
                return null;
            }

            final Map<String, T> lowercaseToConstant = new HashMap<>();
            for (Object constant : rawType.getEnumConstants()) {
                lowercaseToConstant.put(((Enum) constant).name().toLowerCase(Locale.ROOT), (T) constant);
            }

            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(((Enum) value).name());
                    }
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    if (in.peek() == JsonToken.NULL) {
                        in.nextNull();
                        return null;
                    } else {
                        String enumName = in.nextString();
                        T constant = lowercaseToConstant.get(enumName.toLowerCase(Locale.ROOT));
                        if (constant == null) {
                            System.err.println("[GsonEnum] Unknown enum constant '" + enumName + "' for type " + rawType.getSimpleName());
                            return null;
                        }
                        return constant;
                    }
                }
            };
        }
    }
}