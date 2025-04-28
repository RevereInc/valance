package dev.revere.valance.command.impl;

import dev.revere.valance.command.AbstractCommand;
import dev.revere.valance.command.exception.CommandException;
import dev.revere.valance.module.api.IModule;
import dev.revere.valance.service.IModuleManager;
import dev.revere.valance.settings.Setting;
import dev.revere.valance.settings.type.BooleanSetting;
import dev.revere.valance.settings.type.EnumSetting;
import dev.revere.valance.settings.type.NumberSetting;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public class SettingCommand extends AbstractCommand {
    private final IModuleManager moduleManager;

    public SettingCommand(IModuleManager moduleManager) {
        super("setting", List.of("set", "var"), "Views or modifies module settings.", "<module> [setting_name] [new_value]");
        this.moduleManager = moduleManager;
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 1) {
            sendUsage();
            return;
        }

        String moduleName = args[0];
        IModule module = moduleManager.getModule(moduleName)
                .orElseThrow(() -> new CommandException("Module '" + moduleName + "' not found."));

        if (args.length == 1) {
            listModuleSettings(module);
            return;
        }

        String settingName = args[1];
        Setting<?> setting = findSettingByName(module, settingName);

        if (args.length == 2) {
            sendSuccess(module.getName() + " -> " + setting.getName() + " = " + setting.getValue());
            if(setting instanceof NumberSetting<?>) {
                NumberSetting<?> ns = (NumberSetting<?>) setting;
                sendSuccess("  (Type: Number, Min: " + ns.getMinimum() + ", Max: " + ns.getMaximum() + ")");
            } else if (setting instanceof EnumSetting<?>){
                EnumSetting<?> es = (EnumSetting<?>) setting;
                String constants = Arrays.stream(es.getConstants()).map(Enum::name).collect(Collectors.joining(", "));
                sendSuccess("  (Type: Enum, Values: " + constants + ")");
            } else if (setting instanceof BooleanSetting) {
                sendSuccess("  (Type: Boolean)");
            } else {
                sendSuccess("  (Type: " + setting.getDefaultValue().getClass().getSimpleName() + ")");
            }
            return;
        }


        String valueStr = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        setSettingValue(module, setting, valueStr);
    }

    private void listModuleSettings(IModule module) {
        List<Setting<?>> settings = module.getSettings();
        if (settings.isEmpty()) {
            sendSuccess(module.getName() + " has no configurable settings.");
            return;
        }
        sendSuccess("--- Settings for " + module.getName() + " (" + settings.size() + ") ---");
        settings.stream()
                .sorted(Comparator.comparing(Setting::getName))
                .forEach(s -> sendSuccess(" " + s.getName() + " = " + s.getValue() + (s.isVisible() ? "" : " (Hidden)")));
        sendSuccess("Use " + getName() + "setting " + module.getName() + " <setting_name> [new_value] to view/change.");
    }

    private Setting<?> findSettingByName(IModule module, String settingName) throws CommandException {
        return module.getSettings().stream()
                .filter(s -> s.getName().equalsIgnoreCase(settingName))
                .findFirst()
                .orElseThrow(() -> new CommandException("Setting '" + settingName + "' not found in module '" + module.getName() + "'."));
    }

    private void setSettingValue(IModule module, Setting<?> setting, String valueStr) throws CommandException {
        Object parsedValue;
        String typeName = setting.getDefaultValue().getClass().getSimpleName();

        try {
            // --- Type Parsing ---
            if (setting instanceof BooleanSetting) {
                parsedValue = parseBoolean(valueStr);
                typeName = "Boolean";
            } else if (setting instanceof NumberSetting) {
                NumberSetting<?> ns = (NumberSetting<?>) setting;
                typeName = ns.getDefaultValue().getClass().getSimpleName();
                if (ns.getDefaultValue() instanceof Integer) {
                    parsedValue = Integer.parseInt(valueStr);
                } else if (ns.getDefaultValue() instanceof Double) {
                    parsedValue = Double.parseDouble(valueStr);
                } else if (ns.getDefaultValue() instanceof Float) {
                    parsedValue = Float.parseFloat(valueStr);
                }
                else {
                    throw new CommandException("Unsupported NumberSetting type: " + typeName);
                }
            } else if (setting instanceof EnumSetting) {
                EnumSetting<?> es = (EnumSetting<?>) setting;
                typeName = es.getDefaultValue().getClass().getSimpleName();
                // Case-insensitive enum lookup
                Object match = Arrays.stream(es.getConstants())
                        .filter(e -> e.name().equalsIgnoreCase(valueStr))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Invalid value"));
                parsedValue = match;
            }
            // todo: Add ColorSetting parsing when implemented
            else {
                throw new CommandException("Cannot set value for unsupported setting type: " + typeName);
            }

            if (setting.setValueFromObject(parsedValue)) {
                sendSuccess(module.getName() + " -> " + setting.getName() + " set to " + setting.getValue());
            } else {
                throw new CommandException("Failed to apply parsed value '" + valueStr + "' to setting '" + setting.getName() + "'.");
            }

        } catch (NumberFormatException e) {
            throw new CommandException("Invalid " + typeName + " value: '" + valueStr + "'.");
        } catch (IllegalArgumentException e) {
            if(setting instanceof EnumSetting){
                EnumSetting<?> es = (EnumSetting<?>) setting;
                String validValues = Arrays.stream(es.getConstants()).map(Enum::name).collect(Collectors.joining(", "));
                throw new CommandException("Invalid value '" + valueStr + "' for " + typeName + " setting '" + setting.getName() + "'. Valid values: " + validValues);
            } else {
                throw new CommandException("Invalid value: '" + valueStr + "' for " + typeName + " setting '" + setting.getName() + "'.");
            }
        } catch (Exception e) {
            System.err.println(LOG_PREFIX_CMD + "[ERROR] Error setting value for " + module.getName() + "/" + setting.getName());
            e.printStackTrace();
            throw new CommandException("An internal error occurred while setting value.", e);
        }
    }

    private boolean parseBoolean(String value) throws IllegalArgumentException {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.equals("true") || lower.equals("on") || lower.equals("1") || lower.equals("yes") || lower.equals("enable") || lower.equals("allow")) return true;
        if (lower.equals("false") || lower.equals("off") || lower.equals("0") || lower.equals("no") || lower.equals("disable") || lower.equals("deny")) return false;
        throw new IllegalArgumentException("Not a valid boolean value");
    }
}