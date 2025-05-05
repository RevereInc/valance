package dev.revere.valance.command.impl;

import dev.revere.valance.command.AbstractCommand;
import dev.revere.valance.command.exception.CommandException;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.properties.Property;
import dev.revere.valance.service.IModuleManager;
import dev.revere.valance.util.LoggerUtil;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Remi
 * @project valance
 * @date 5/5/2025
 */
public class SettingCommand extends AbstractCommand {
    private final IModuleManager moduleManager;

    public SettingCommand(IModuleManager moduleManager) {
        super("setting", List.of("set", "var"), "Views or modifies module settings.", "<module> [setting_path] [new_value]");
        this.moduleManager = moduleManager;
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 1) {
            sendUsage();
            return;
        }

        String moduleName = args[0];
        AbstractModule module = (AbstractModule) moduleManager.getModule(moduleName)
                .orElseThrow(() -> new CommandException("Module '" + moduleName + "' not found or is not manageable."));

        if (args.length == 1) {
            listModuleSettings(module);
            return;
        }

        String settingIdentifier = args[1];
        Property<?> property = findPropertyByIdentifier(module, settingIdentifier);

        if (args.length == 2) {
            sendSuccess(module.getName() + " -> " + getPropertyDisplayName(property) + " = " + formatPropertyValue(property));
            displayPropertyInfo(property);
            return;
        }

        String valueStr = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        try {
            parseAndSetPropertyValue(module, property, valueStr);
        } catch (IllegalArgumentException | CommandException e) {
            throw new CommandException(e.getMessage());
        } catch (Exception e) {
            LoggerUtil.error("[" + getName() + "]", "Internal error setting property " + module.getName() + "/" + getPropertyDisplayName(property), e);
            throw new CommandException("An internal error occurred. Check logs.");
        }
    }

    private void listModuleSettings(AbstractModule module) {
        List<Property<?>> settings = module.getProperties();
        if (settings.isEmpty()) {
            sendSuccess("'" + module.getName() + "' has no configurable settings.");
            return;
        }
        sendSuccess("--- Settings for " + module.getName() + " ---");
        settings.stream()
                .filter(Property::isVisible)
                .sorted(Comparator.comparing(this::getPropertyDisplayName))
                .forEach(prop -> sendSuccess(" " + getPropertyDisplayName(prop) + " = " + formatPropertyValue(prop)));
        sendSuccess("Use: " + getUsage());
    }

    private Property<?> findPropertyByIdentifier(AbstractModule module, String identifier) throws CommandException {
        Optional<Property<?>> propertyOpt = module.getProperties().stream()
                .filter(p -> getPropertyDisplayName(p).equalsIgnoreCase(identifier) && p.isVisible())
                .findFirst();
        if (propertyOpt.isEmpty()) {
            propertyOpt = module.getProperties().stream()
                    .filter(p -> getPropertyDisplayName(p).equalsIgnoreCase(identifier))
                    .findFirst();
        }
        return propertyOpt.orElseThrow(() -> new CommandException("Setting '" + identifier + "' not found in module '" + module.getName() + "'."));
    }

    private String getPropertyDisplayName(Property<?> property) {
        return property.getName();
    }

    private void displayPropertyInfo(Property<?> property) {
        if (!property.getDescription().isEmpty()) {
            sendSuccess("  Desc: " + property.getDescription());
        }
        Object defaultValue = property.getDefaultValue();
        String typeInfo = "Type: " + defaultValue.getClass().getSimpleName();

        if (defaultValue instanceof Boolean) {
            typeInfo += " (true/false)";
        } else if (defaultValue instanceof Color) {
            typeInfo = "Type: Color (#HexRRGGBB or #HexAARRGGBB)";
        } else if (defaultValue instanceof Number) {
            String constraints = "";
            if (property.getMinimum() != null) constraints += ", Min: " + property.getMinimum();
            if (property.getMaximum() != null) constraints += ", Max: " + property.getMaximum();
            if (property.getIncrementation() != null) constraints += ", Inc: " + property.getIncrementation();
            if (!constraints.isEmpty()) typeInfo += " (" + constraints.substring(2) + ")";
        } else if (defaultValue instanceof Enum) {
            try {
                Class<? extends Enum<?>> enumClass = ((Enum<?>) defaultValue).getDeclaringClass();
                String constants = Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
                typeInfo += " (Values: " + constants + ")";
            } catch (Exception e) {
                typeInfo += " (Error getting enum values)";
            }
        } else if (defaultValue instanceof String) {
            typeInfo += " (Text)";
        }

        sendSuccess("  (" + typeInfo + ")");
    }

    private String formatPropertyValue(Property<?> property) {
        Object value = property.getValue();
        if (value == null) return "null";

        if (value instanceof Color) {
            return formatColorToString((Color) value);
        } else if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }
        return String.valueOf(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void parseAndSetPropertyValue(AbstractModule module, Property<?> property, String valueStr) throws CommandException {
        Object defaultValue = property.getDefaultValue();
        Object parsedValue;

        try {
            if (defaultValue instanceof Boolean) {
                parsedValue = parseBooleanRobust(valueStr);
                ((Property<Boolean>) property).setValue((Boolean) parsedValue);
            } else if (defaultValue instanceof Color) {
                parsedValue = parseColorString(valueStr);
                if (parsedValue == null) {
                    throw new IllegalArgumentException("Invalid color format '" + valueStr + "'. Use #RRGGBB or #AARRGGBB.");
                }
                ((Property<Color>) property).setValue((Color) parsedValue);
            } else if (defaultValue instanceof Integer) {
                parsedValue = Integer.parseInt(valueStr.trim());
                ((Property<Integer>) property).setValue((Integer) parsedValue);
            } else if (defaultValue instanceof Double) {
                parsedValue = Double.parseDouble(valueStr.trim());
                ((Property<Double>) property).setValue((Double) parsedValue);
            } else if (defaultValue instanceof Float) {
                parsedValue = Float.parseFloat(valueStr.trim());
                ((Property<Float>) property).setValue((Float) parsedValue);
            } else if (defaultValue instanceof Long) {
                parsedValue = Long.parseLong(valueStr.trim());
                ((Property<Long>) property).setValue((Long) parsedValue);
            } else if (defaultValue instanceof Short) {
                parsedValue = Short.parseShort(valueStr.trim());
                ((Property<Short>) property).setValue((Short) parsedValue);
            } else if (defaultValue instanceof Byte) {
                parsedValue = Byte.parseByte(valueStr.trim());
                ((Property<Byte>) property).setValue((Byte) parsedValue);
            } else if (defaultValue instanceof Enum) {
                Enum<?> currentEnum = (Enum<?>) defaultValue;
                Class<? extends Enum> enumClass = currentEnum.getDeclaringClass();
                Enum<?> match = Arrays.stream(enumClass.getEnumConstants())
                        .filter(e -> e.name().equalsIgnoreCase(valueStr.trim()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Invalid enum value '" + valueStr + "'."));
                parsedValue = match;
                ((Property<Enum>) property).setValue((Enum) parsedValue);
            } else if (defaultValue instanceof String) {
                parsedValue = valueStr;
                ((Property<String>) property).setValue((String) parsedValue);
            } else {
                throw new CommandException("Unsupported property type: " + defaultValue.getClass().getSimpleName());
            }

            sendSuccess(module.getName() + " -> " + getPropertyDisplayName(property) + " set to " + formatPropertyValue(property));

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: '" + valueStr + "'.");
        } catch (IllegalArgumentException e) {
            if (defaultValue instanceof Enum) {
                Class<? extends Enum> enumClass = ((Enum<?>) defaultValue).getDeclaringClass();
                String validValues = Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
                throw new IllegalArgumentException("Invalid value '" + valueStr + "'. Valid options: " + validValues);
            } else if (defaultValue instanceof Boolean) {
                throw new IllegalArgumentException("Invalid boolean value '" + valueStr + "'. Use true/false, on/off, etc.");
            } else if (defaultValue instanceof Color) {
                throw new IllegalArgumentException("Invalid Color value: '" + valueStr + "'. Use hex format #RRGGBB or #AARRGGBB.");
            }
            throw e;
        }
    }

    private boolean parseBooleanRobust(String value) throws IllegalArgumentException {
        String lower = value.trim().toLowerCase(Locale.ROOT);
        if (Set.of("true", "on", "1", "yes", "enable", "allow").contains(lower)) return true;
        if (Set.of("false", "off", "0", "no", "disable", "deny").contains(lower)) return false;
        throw new IllegalArgumentException("Not a valid boolean value");
    }

    private Color parseColorString(String value) {
        if (value == null || value.isEmpty()) return null;
        value = value.trim();
        try {
            if (value.startsWith("#")) {
                if (value.length() == 7) {
                    return new Color(Integer.parseInt(value.substring(1, 3), 16), Integer.parseInt(value.substring(3, 5), 16), Integer.parseInt(value.substring(5, 7), 16));
                } else if (value.length() == 9) {
                    return new Color(Integer.parseInt(value.substring(3, 5), 16), Integer.parseInt(value.substring(5, 7), 16), Integer.parseInt(value.substring(7, 9), 16), Integer.parseInt(value.substring(1, 3), 16));
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
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException |
                 StringIndexOutOfBoundsException e) { /* Ignore */ }
        return null;
    }

    private String formatColorToString(Color color) {
        if (color == null) return null;
        return String.format("#%02X%02X%02X%02X", color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
    }
}