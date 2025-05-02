package dev.revere.valance.service;

import dev.revere.valance.core.lifecycle.IService;
import dev.revere.valance.module.Category;
import dev.revere.valance.module.api.IModule;

import java.util.List;
import java.util.Optional;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public interface IModuleManager extends IService {
    /**
     * Retrieves a module instance by its case-insensitive name.
     *
     * @param name The name of the module.
     * @return An Optional containing the module if found, otherwise empty.
     */
    Optional<IModule> getModule(String name);

    /**
     * Retrieves a module instance by its class type.
     *
     * @param moduleClass The class of the module.
     * @param <T>         The type of the module.
     * @return An Optional containing the module if found, otherwise empty.
     */
    <T extends IModule> Optional<T> getModule(Class<T> moduleClass);

    /**
     * @return An unmodifiable list of all registered modules.
     */
    List<IModule> getModules();

    /**
     * Retrieves all modules belonging to a specific category.
     *
     * @param category The category to filter by.
     * @return An unmodifiable list of modules in the specified category.
     */
    List<IModule> getModulesInCategory(Category category);

    /**
     * Retrieves all modules of a specific type.
     *
     * @param type The class type of the module.
     * @param <T>  The type of the module.
     * @return An unmodifiable list of modules of the specified type.
     */
    <T extends IModule> List<T> getModulesOfType(Class<T> type);
}