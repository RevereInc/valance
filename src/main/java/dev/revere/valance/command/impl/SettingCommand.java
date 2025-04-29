package dev.revere.valance.command.impl;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.command.AbstractCommand;
import dev.revere.valance.command.exception.CommandException;
import dev.revere.valance.module.api.IModule;
import dev.revere.valance.service.IModuleManager;
import dev.revere.valance.settings.Setting;
import dev.revere.valance.settings.type.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
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
            sendSuccess(module.getName() + " -> " + setting.getName() + " = " + formatSettingValue(setting));
            displaySettingTypeInfo(setting);
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
        Optional<Setting<?>> settingOpt = module.getSettings().stream()
                .filter(s -> s.getName().equalsIgnoreCase(settingName) && s.isVisible())
                .findFirst();
        if (settingOpt.isEmpty()) {
            settingOpt = module.getSettings().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(settingName))
                    .findFirst();
        }
        return settingOpt.orElseThrow(() -> new CommandException("Setting '" + settingName + "' not found in module '" + module.getName() + "'."));
    }

    private void displaySettingTypeInfo(Setting<?> setting) {
        if (!setting.getDescription().isEmpty()) {
            sendSuccess("  Description: " + setting.getDescription());
        }
        if (setting instanceof NumberSetting) {
            NumberSetting<?> ns = (NumberSetting<?>) setting;
            sendSuccess("  (Type: Number, Min: " + ns.getMinimum() + ", Max: " + ns.getMaximum() + (ns.getIncrement() != null ? ", Inc: " + ns.getIncrement() : "") + ")");
        } else if (setting instanceof EnumSetting) {
            EnumSetting<?> es = (EnumSetting<?>) setting;
            String constants = Arrays.stream(es.getConstants()).map(Enum::name).collect(Collectors.joining(", "));
            sendSuccess("  (Type: Enum, Values: " + constants + ")");
        } else if (setting instanceof BooleanSetting) {
            sendSuccess("  (Type: Boolean, Values: true, false)");
        } else if (setting instanceof ColorSetting) {
            ColorSetting cs = (ColorSetting) setting;
            sendSuccess("  (Type: Color" + (cs.hasAlpha() ? " w/ Alpha" : "") + ", Format: #Hex" + (cs.hasAlpha() ? "AARRGGBB" : "RRGGBB") + ")");
        } else if (setting instanceof StringSetting) {
            sendSuccess("  (Type: String)");
        } else {
            sendSuccess("  (Type: " + setting.getDefaultValue().getClass().getSimpleName() + ")");
        }
    }

    /**
     * Formats a setting's value for display, handling specific types like Color and String.
     */
    private String formatSettingValue(Setting<?> setting) {
        Object value = setting.getValue();
        if (setting instanceof ColorSetting) {
            int intVal = (Integer) value;
            if (((ColorSetting) setting).hasAlpha()) {
                return String.format("#%08X", intVal);
            } else {
                return String.format("#%06X", intVal & 0xFFFFFF);
            }
        } else if (setting instanceof StringSetting) {
            return "\"" + value + "\"";
        }
        return String.valueOf(value);
    }

    private void setSettingValue(IModule module, Setting<?> setting, String valueStr) throws CommandException {
        Object parsedValue;
        String typeName = setting.getDefaultValue().getClass().getSimpleName();
        boolean valueSetSuccessfully = false;

        try {
            // --- Type Parsing ---
            if (setting instanceof BooleanSetting) {
                parsedValue = parseBoolean(valueStr);
                typeName = "Boolean";
                valueSetSuccessfully = setting.setValueFromObject(parsedValue);

            } else if (setting instanceof NumberSetting) {
                NumberSetting<?> ns = (NumberSetting<?>) setting;
                typeName = ns.getDefaultValue().getClass().getSimpleName();
                Number parsedNum = getNumber(valueStr, ns, typeName);
                valueSetSuccessfully = ns.setValueFromObject(parsedNum);

            } else if (setting instanceof EnumSetting) {
                EnumSetting<?> es = (EnumSetting<?>) setting;
                typeName = es.getDefaultValue().getClass().getSimpleName();
                Enum<?> match = Arrays.stream(es.getConstants())
                        .filter(e -> e.name().equalsIgnoreCase(valueStr))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Invalid value"));
                valueSetSuccessfully = es.setValueFromObject(match.name());

            } else if (setting instanceof ColorSetting) {
                typeName = "Color";
                String hex = valueStr.trim();
                if (hex.startsWith("#")) {
                    hex = hex.substring(1);
                }
                if (!hex.matches("^[a-fA-F0-9]{6,8}$")) {
                    throw new IllegalArgumentException("Invalid hex format");
                }
                try {
                    long longVal = Long.parseLong(hex, 16);
                    int intVal;
                    if (hex.length() == 8) {
                        intVal = (int) longVal;
                    } else {
                        intVal = 0xFF000000 | (int) longVal;
                    }
                    parsedValue = intVal;
                    valueSetSuccessfully = setting.setValueFromObject(parsedValue);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Invalid hex number");
                }

            } else if (setting instanceof StringSetting) {
                typeName = "String";
                parsedValue = valueStr;
                valueSetSuccessfully = setting.setValueFromObject(parsedValue);

            }

            if (valueSetSuccessfully) {
                sendSuccess(module.getName() + " -> " + setting.getName() + " set to " + formatSettingValue(setting));
            } else {
                throw new CommandException(String.format("Failed to apply value '%s' to setting '%s'. Check type or constraints.", valueStr, setting.getName()));
            }

        } catch (NumberFormatException e) {
            throw new CommandException(String.format("Invalid %s value: '%s'. Expected a number.", typeName, valueStr));
        } catch (IllegalArgumentException e) {
            if (setting instanceof EnumSetting) {
                EnumSetting<?> es = (EnumSetting<?>) setting;
                String validValues = Arrays.stream(es.getConstants()).map(Enum::name).collect(Collectors.joining(", "));
                throw new CommandException("Invalid value '" + valueStr + "' for " + typeName + " setting '" + setting.getName() + "'. Valid values: " + validValues);
            } else if (setting instanceof ColorSetting) {
                throw new CommandException("Invalid Color value: '" + valueStr + "'. Use hex format #RRGGBB or #AARRGGBB.");
            } else {
                throw new CommandException("Invalid value: '" + valueStr + "' for " + typeName + " setting '" + setting.getName() + "'.");
            }
        } catch (Exception e) {
            System.err.println("[" + ClientLoader.CLIENT_NAME + ":Command] [ERROR] Error setting value for " + module.getName() + "/" + setting.getName());
            e.printStackTrace();
            throw new CommandException("An internal error occurred while setting value. Check logs.", e);
        }
    }

    private @NotNull Number getNumber(String valueStr, NumberSetting<?> ns, String typeName) throws CommandException {
        Number parsedNum;
        if (ns.getDefaultValue() instanceof Integer) {
            parsedNum = Integer.parseInt(valueStr);
        } else if (ns.getDefaultValue() instanceof Double) {
            parsedNum = Double.parseDouble(valueStr);
        } else if (ns.getDefaultValue() instanceof Float) {
            parsedNum = Float.parseFloat(valueStr);
        } else if (ns.getDefaultValue() instanceof Long) {
            parsedNum = Long.parseLong(valueStr);
        } else if (ns.getDefaultValue() instanceof Short) {
            parsedNum = Short.parseShort(valueStr);
        } else if (ns.getDefaultValue() instanceof Byte) {
            parsedNum = Byte.parseByte(valueStr);
        } else {
            throw new CommandException("Unsupported NumberSetting internal type: " + typeName);
        }
        return parsedNum;
    }

    /**
     * Helper for robust boolean parsing.
     */
    private boolean parseBoolean(String value) throws IllegalArgumentException {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.equals("true") || lower.equals("on") || lower.equals("1") || lower.equals("yes") || lower.equals("enable") || lower.equals("allow"))
            return true;
        if (lower.equals("false") || lower.equals("off") || lower.equals("0") || lower.equals("no") || lower.equals("disable") || lower.equals("deny"))
            return false;
        throw new IllegalArgumentException("Not a valid boolean value");
    }
}