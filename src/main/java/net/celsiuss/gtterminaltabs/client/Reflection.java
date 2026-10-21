package net.celsiuss.gtterminaltabs.client;

import java.lang.reflect.Field;

import net.celsiuss.gtterminaltabs.GtTerminalTabs;

/** Reaches the AE2 and NEI fields that have no accessor. Each one missing only turns off the feature that needs it. */
final class Reflection {

    private Reflection() {}

    /** The declared field, made accessible, or null after logging what won't work without it. */
    static Field findField(Class<?> owner, String name, String withoutIt) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            GtTerminalTabs.LOG.warn("{} has no field {}; {}", owner.getName(), name, withoutIt, e);
            return null;
        }
    }

    /** The field's value, or null when the field is missing or holds something other than a {@code type}. */
    static <T> T get(Field field, Object target, Class<T> type) {
        if (field == null) return null;
        try {
            Object value = field.get(target);
            return type.isInstance(value) ? type.cast(value) : null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /** Sets the field, if it was found. */
    static void set(Field field, Object target, Object value) {
        if (field == null) return;
        try {
            field.set(target, value);
        } catch (IllegalAccessException ignored) {}
    }
}
