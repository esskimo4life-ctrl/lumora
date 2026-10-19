package dev.astral.boost;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Apply known client options; skip renamed or unavailable options instead of crashing. */
public final class PerformanceTuner {
    private static volatile String lastSummary = "Профиль ещё не применён";
    private static volatile List<String> lastApplied = List.of();
    private PerformanceTuner() {}

    public static synchronized ApplyResult apply(Object minecraftClient, Map<String, Object> requested) {
        List<String> applied = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        try {
            Object options = readFieldOrMethod(minecraftClient, "options", "options");
            if (options == null) {
                lastSummary = "Не удалось найти параметры клиента";
                lastApplied = List.of();
                return new ApplyResult(0, requested.size(), lastSummary, List.of());
            }
            for (Map.Entry<String, Object> entry : requested.entrySet()) {
                String name = entry.getKey();
                try {
                    Field field = findField(options.getClass(), aliases(name));
                    if (field == null) { skipped.add(name); continue; }
                    field.setAccessible(true);
                    Object optionInstance = field.get(options);
                    if (optionInstance == null) { skipped.add(name); continue; }
                    Method getter = findMethod(optionInstance.getClass(), "get", 0);
                    Method setter = findMethod(optionInstance.getClass(), "set", 1);
                    if (getter == null || setter == null) { skipped.add(name); continue; }
                    getter.setAccessible(true); setter.setAccessible(true);
                    Object current = getter.invoke(optionInstance);
                    Object converted = convert(entry.getValue(), current == null ? null : current.getClass());
                    if (converted == NOT_CONVERTIBLE) { skipped.add(name); continue; }
                    setter.invoke(optionInstance, converted);
                    applied.add(name);
                } catch (ReflectiveOperationException | RuntimeException ex) { skipped.add(name); }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            lastSummary = "Не удалось прочитать настройки: " + e.getClass().getSimpleName();
            lastApplied = List.copyOf(applied);
            return new ApplyResult(applied.size(), requested.size() - applied.size(), lastSummary, List.copyOf(applied));
        }
        lastApplied = List.copyOf(applied);
        lastSummary = "Применено параметров: " + applied.size() + "/" + requested.size()
                + (skipped.isEmpty() ? "" : " (недоступно: " + skipped.size() + ")");
        return new ApplyResult(applied.size(), skipped.size(), lastSummary, List.copyOf(applied));
    }

    public static String lastSummary() { return lastSummary; }
    public static List<String> lastApplied() { return lastApplied; }

    private static String[] aliases(String name) {
        return switch (name) {
            case "particles" -> new String[]{"particles", "particleStatus"};
            case "graphicsMode" -> new String[]{"graphicsMode", "graphics"};
            case "clouds" -> new String[]{"clouds", "cloudStatus"};
            case "bobView" -> new String[]{"bobView", "viewBobbing"};
            case "enableVsync" -> new String[]{"enableVsync", "vsync"};
            case "entityDistanceScaling" -> new String[]{"entityDistanceScaling", "entityDistance"};
            default -> new String[]{name};
        };
    }
    private static final Object NOT_CONVERTIBLE = new Object();

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(Object wanted, Class<?> targetType) {
        if (targetType == null) return NOT_CONVERTIBLE;
        try {
            if (targetType.isEnum() && wanted instanceof String text)
                return Enum.valueOf((Class<? extends Enum>) targetType.asSubclass(Enum.class), text);
            if (targetType == Boolean.class || targetType == boolean.class)
                return wanted instanceof Boolean ? wanted : Boolean.parseBoolean(String.valueOf(wanted));
            if (targetType == Integer.class || targetType == int.class)
                return wanted instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(wanted));
            if (targetType == Double.class || targetType == double.class)
                return wanted instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(wanted));
            if (targetType == Float.class || targetType == float.class)
                return wanted instanceof Number n ? n.floatValue() : Float.parseFloat(String.valueOf(wanted));
            if (targetType.isInstance(wanted)) return wanted;
        } catch (RuntimeException ignored) { }
        return NOT_CONVERTIBLE;
    }

    private static Object readFieldOrMethod(Object target, String fieldName, String methodName) throws ReflectiveOperationException {
        if (target == null) return null;
        Field field = findField(target.getClass(), fieldName);
        if (field != null) { field.setAccessible(true); return field.get(target); }
        Method method = findMethod(target.getClass(), methodName, 0);
        if (method != null) { method.setAccessible(true); return method.invoke(target); }
        return null;
    }

    private static Field findField(Class<?> type, String... names) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) for (String name : names) {
            try { return c.getDeclaredField(name); } catch (NoSuchFieldException ignored) { }
        }
        return null;
    }
    private static Method findMethod(Class<?> type, String name, int parameterCount) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) for (Method m : c.getDeclaredMethods())
            if (m.getName().equals(name) && m.getParameterCount() == parameterCount) return m;
        return null;
    }
    public record ApplyResult(int applied, int skipped, String summary, List<String> appliedOptions) { }
}
