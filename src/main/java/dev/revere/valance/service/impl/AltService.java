package dev.revere.valance.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import dev.revere.valance.ClientLoader;
import dev.revere.valance.alt.Alt;
import dev.revere.valance.core.ClientContext;
import dev.revere.valance.core.annotation.Inject;
import dev.revere.valance.core.annotation.Service;
import dev.revere.valance.core.exception.ServiceException;
import dev.revere.valance.service.IAltService;
import dev.revere.valance.util.LoggerUtil;
import dev.revere.valance.util.MinecraftUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service(provides = IAltService.class, priority = 400)
public class AltService implements IAltService {
    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":AltService]";
    private static final String DIR_NAME = ClientLoader.CLIENT_NAME;
    private static final String ALTS_FILENAME = "alts.json";

    private final ArrayList<Alt> alts = new ArrayList<>();
    private String status = "";
    private final Gson gson;
    private Path dataFile;

    private static final Type ALT_LIST_TYPE = new TypeToken<ArrayList<Alt>>() {
    }.getType();

    @Inject
    public AltService() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

        try {
            Path mcDir = MinecraftUtil.mc().mcDataDir.toPath();
            Path clientDir = mcDir.resolve(DIR_NAME);
            Files.createDirectories(clientDir);
            this.dataFile = clientDir.resolve(ALTS_FILENAME);
            LoggerUtil.info(LOG_PREFIX, "Using alts file: " + this.dataFile);
        } catch (Exception e) {
            LoggerUtil.error(LOG_PREFIX, "CRITICAL: Failed to initialize alts file path!", e);
            this.dataFile = null;
        }
    }

    @Override
    public void setup(ClientContext context) throws ServiceException {
        loadAltsFromFile();
    }


    @Override
    public void initialize(ClientContext context) throws ServiceException {
        LoggerUtil.info(LOG_PREFIX, "Initialized. Loaded " + alts.size() + " alts.");
    }

    @Override
    public void shutdown(ClientContext context) throws ServiceException {
        saveAltsToFile();
        LoggerUtil.info(LOG_PREFIX, "Shutdown complete. Saved " + alts.size() + " alts.");
    }

    @Override
    public synchronized void addAlt(Alt alt) {
        Objects.requireNonNull(alt, "Cannot add a null alt");
        alts.add(alt);
        LoggerUtil.info(LOG_PREFIX, "Added alt: " + alt.getAlias() + " (" + alt.getUsername() + ")");
    }

    @Override
    public synchronized List<Alt> getAlts() {
        return this.alts;
    }

    @Override
    public synchronized boolean removeAlt(Alt altToRemove) {
        Objects.requireNonNull(altToRemove, "Cannot remove a null alt");
        boolean removed = alts.removeIf(alt -> alt.getUuid() != null && alt.getUuid().equals(altToRemove.getUuid()) || alt.getUsername().equalsIgnoreCase(altToRemove.getUsername()));
        if (removed) {
            LoggerUtil.info(LOG_PREFIX, "Removed alt: " + altToRemove.getAlias());
        }
        return removed;
    }

    @Override
    public boolean isValidCrackedAlt(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9_]{3,16}$");
    }

    @Override
    public synchronized void setStatus(String status) {
        this.status = status == null ? "" : status;
    }

    @Override
    public synchronized String getStatus() {
        return status;
    }

    private synchronized void loadAltsFromFile() {
        if (dataFile == null) {
            LoggerUtil.error(LOG_PREFIX, "Cannot load alts, dataFile path is not initialized.");
            return;
        }
        if (!Files.exists(dataFile)) {
            LoggerUtil.info(LOG_PREFIX, "Alts file not found (" + ALTS_FILENAME + "), starting with empty list.");
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            ArrayList<Alt> loadedAlts = gson.fromJson(reader, ALT_LIST_TYPE);
            if (loadedAlts != null) {
                this.alts.clear();
                this.alts.addAll(loadedAlts);
                LoggerUtil.info(LOG_PREFIX, "Successfully loaded " + this.alts.size() + " alts from " + ALTS_FILENAME);
            } else {
                LoggerUtil.warn(LOG_PREFIX, "Alts file was empty or contained invalid JSON. Starting with empty list.");
                this.alts.clear();
            }
        } catch (IOException e) {
            LoggerUtil.error(LOG_PREFIX, "IOException occurred while reading alts file: " + dataFile, e);
            this.alts.clear();
        } catch (JsonSyntaxException | JsonIOException e) {
            LoggerUtil.error(LOG_PREFIX, "JSON parsing error while reading alts file: " + dataFile + ". File might be corrupted.", e);
            this.alts.clear();
        } catch (Exception e) {
            LoggerUtil.error(LOG_PREFIX, "Unexpected error loading alts file: " + dataFile, e);
            this.alts.clear();
        }
    }

    private synchronized void saveAltsToFile() {
        if (dataFile == null) {
            LoggerUtil.error(LOG_PREFIX, "Cannot save alts, dataFile path is not initialized.");
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(dataFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(this.alts, writer);
            LoggerUtil.info(LOG_PREFIX, "Successfully saved " + this.alts.size() + " alts to " + ALTS_FILENAME);
        } catch (IOException | JsonIOException e) {
            LoggerUtil.error(LOG_PREFIX, "Failed to write alts to file: " + dataFile, e);
        } catch (Exception e) {
            LoggerUtil.error(LOG_PREFIX, "Unexpected error saving alts file: " + dataFile, e);
        }
    }
}