package cnm.mixin.O.mixin.utils;

public class ClassUtils {
    public static Class<?> getClass(String name) {
        Class<?> clazz = null;
        name = name.replace('/', '.');
        try {
            clazz = Class.forName(name);
        } catch (Throwable ignored) {
        }
        return clazz;
    }
}
