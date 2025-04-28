package dev.revere.valance.command.impl;

import dev.revere.valance.command.AbstractCommand;
import dev.revere.valance.command.exception.CommandException;
import dev.revere.valance.module.api.IModule;
import dev.revere.valance.service.IModuleManager;

import java.util.List;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public class ToggleCommand extends AbstractCommand {

    private final IModuleManager moduleManager;

    public ToggleCommand(IModuleManager moduleManager) {
        super("toggle", List.of("t"), "Toggles a module on or off.", "<module_name>");
        this.moduleManager = moduleManager;
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length != 1) {
            sendUsage();
            return;
        }

        String moduleName = args[0];
        IModule module = moduleManager.getModule(moduleName).orElseThrow(() -> new CommandException("Module '" + moduleName + "' not found."));

        module.toggle();

        sendSuccess(module.getName() + " has been " + (module.isEnabled() ? "enabled" : "disabled") + ".");
    }
}