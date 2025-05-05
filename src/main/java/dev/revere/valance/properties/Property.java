package dev.revere.valance.properties;

import dev.revere.valance.ClientLoader;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author Remi
 * @project valance
 * @date 5/2/2025
 */
@Getter
@Setter
public class Property<T> {
    protected static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":Property]";

    private final String name;
    private String description = "";

    private Supplier<Boolean> visibility = () -> true;

    private Property<?> parent = null;
    private List<Property<?>> children = new ArrayList<>();

    protected T value;
    private T defaultValue;

    private T minimum;
    private T maximum;
    private T incrementation;

    private int index = 0;

    /**
     * Main constructor.
     *
     * @param name  Name of the property.
     * @param value The initial and default value.
     */
    public Property(String name, T value) {
        this.name = Objects.requireNonNull(name, "Property name cannot be null");
        this.value = Objects.requireNonNull(value, "Property value cannot be null for: " + name);
        this.defaultValue = value;

        if (this.value instanceof Enum<?>) {
            index = ((Enum<?>) this.value).ordinal();
        }
    }

    /**
     * Cycles through Enum values and returns the next/previous value.
     *
     * @param previous If true, cycle backward; otherwise, cycle forward.
     * @return The next or previous Enum value. Returns current value if not an Enum property or constants are unavailable.
     */
    @SuppressWarnings("unchecked")
    public T getNextMode(boolean previous) {
        if (getValue() instanceof Enum<?>) {
            Enum<?> enumeration = (Enum<?>) getValue();
            Enum<?>[] constants = enumeration.getDeclaringClass().getEnumConstants();

            if (constants == null || constants.length == 0) {
                return getValue();
            }

            int currentIndex = enumeration.ordinal();

            int nextIndex;
            if (!previous) {
                nextIndex = (currentIndex + 1) % constants.length;
            } else {
                nextIndex = (currentIndex - 1 + constants.length) % constants.length;
            }

            this.index = nextIndex;

            return (T) constants[nextIndex];
        }
        return getValue();
    }

    /**
     * Sets the minimum value constraint.
     *
     * @param minimum The minimum value.
     * @return This Property instance for chaining.
     */
    public Property<T> minimum(T minimum) {
        this.minimum = minimum;
        return this;
    }

    /**
     * Sets the maximum value constraint.
     *
     * @param maximum The maximum value.
     * @return This Property instance for chaining.
     */
    public Property<T> maximum(T maximum) {
        this.maximum = maximum;
        return this;
    }

    /**
     * Sets the incrementation value constraint.
     *
     * @param incrementation The incrementation value.
     * @return This Property instance for chaining.
     */
    public Property<T> increment(T incrementation) {
        this.incrementation = incrementation;
        return this;
    }

    /**
     * Sets the description.
     *
     * @param description The description.
     * @return This Property instance for chaining.
     */
    public Property<T> describedBy(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the visibility condition.
     *
     * @param visibility The visibility supplier.
     * @return This Property instance for chaining.
     */
    public Property<T> visibleWhen(Supplier<Boolean> visibility) {
        this.visibility = Objects.requireNonNull(visibility);
        return this;
    }

    /**
     * Sets the parent property and adds this property to the parent's children.
     *
     * @param parent The parent Property.
     * @return This Property instance for chaining.
     */
    public Property<T> childOf(Property<?> parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
        if (this.parent.children == null) {
            this.parent.children = new ArrayList<>();
        }
        this.parent.children.add(this);
        return this;
    }

    /**
     * Generates a path string based on parent hierarchy (e.g., "ParentNameChildName").
     *
     * @return The hierarchical path string.
     */
    public String getPath() {
        return getParent() == null ? getName() : getParent().getPath() + getName();
    }

    /**
     * Recursively gets all child properties in the hierarchy below this one.
     *
     * @return A flat list of all descendant properties.
     */
    public List<Property<?>> getHierarchy() {
        List<Property<?>> hierarchy = new ArrayList<>();
        if (children != null) {
            for (Property<?> childProperty : children) {
                hierarchy.add(childProperty);
                hierarchy.addAll(childProperty.getHierarchy());
            }
        }
        return hierarchy;
    }

    /**
     * Checks if the property should be visible based on its supplier.
     * Includes basic error handling for the supplier.
     *
     * @return true if visible, false otherwise.
     */
    public boolean isVisible() {
        try {
            return visibility != null && visibility.get();
        } catch (Exception e) {
            System.err.println("[Property:" + name + "] Error evaluating visibility: " + e.getMessage());
            return true;
        }
    }
}