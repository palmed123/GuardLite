package cnm.obsoverlay.utils;

import cnm.mixin.O.mapping.Mapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectUtil implements Wrapper {
    public static Method getMethod(Class<?> clazz, String notObfuscatedName, String docs, Class<?>... parameterTypes) {
        String mappedName = Mapping.get(clazz, notObfuscatedName, docs);
        if (mappedName != null) {
            try {
                Method method = clazz.getDeclaredMethod(mappedName, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static Object invokeMethod(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Field getField(Class<?> clazz, String notObfuscatedName) {
        String mappedName = Mapping.get(clazz, notObfuscatedName, null);
        if (mappedName != null) {
            try {
                Field field = clazz.getDeclaredField(mappedName);
                field.setAccessible(true);
                return field;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static Object getFieldValue(Class<?> clazz, String notObfuscatedName, Object obj) {
        String mappedName = Mapping.get(clazz, notObfuscatedName, null);
        if (mappedName != null) {
            try {
                Field field = clazz.getDeclaredField(mappedName);
                field.setAccessible(true);
                Object value = field.get(obj);
                return value;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static Boolean setFieldValue(Class<?> clazz, String notObfuscatedName, Object obj, Object value) {
        String mappedName = Mapping.get(clazz, notObfuscatedName, null);
        if (mappedName != null) {
            try {
                Field field = clazz.getDeclaredField(mappedName);
                field.setAccessible(true);
                field.set(obj, value);
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }
}

