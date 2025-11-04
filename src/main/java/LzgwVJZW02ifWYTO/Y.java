package LzgwVJZW02ifWYTO;

import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;

import java.util.HashMap;

public class Y {

    public static HashMap<Class, byte[]> cachedClassBytes = new HashMap<>();


    public static byte[] getClassesBytes(Class<?> clazz) {
        try {
            return cachedClassBytes.get(clazz);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
    }

    public static byte[] nativeCallJava(Class capturedClass, ClassLoader itsClassLoader, String className, byte[] classBytesIn) {
        try {
//            System.out.println("[nativeCallJava]\n" + capturedClass.getName());
            for (ASMTransformer asmTransformer : MixinLoader.transformer.transformers) {
                if (asmTransformer.getTarget() == capturedClass) {
                    cachedClassBytes.putIfAbsent(capturedClass, classBytesIn);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
        return classBytesIn;
    }

    public static native void javaCallNative(Class cls);

    public static native void redefineClasses(Class<?> targetClass, byte[] newClassBytes);
}