package lidan.yusili;

import sun.misc.Unsafe;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class loader {
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int AES_KEY_SIZE = 256;
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;
    public loader() throws Exception {

        ClassLoader targetClassLoader = null;
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getContextClassLoader() != null) {
                if (thread.getName().equals("Render thread")) {
                    targetClassLoader = thread.getContextClassLoader();
                    break;
                }
            }
        }

        ZipInputStream jarStream = null;

        String userHome = System.getProperty("user.home");
        File clientJarFile = new File(userHome, "AppData\\Roaming\\GuardLite\\client.jar");

        if (targetClassLoader == null) {
            JOptionPane.showMessageDialog(null,
                    "无法找到目标 ClassLoader (Render thread)",
                    "注入错误",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        if (!clientJarFile.exists() || !clientJarFile.isFile()) {
            JOptionPane.showMessageDialog(null,
                    "客户端文件未找到!\n路径: " + clientJarFile.getAbsolutePath(),
                    "加载错误",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        try {
            jarStream = new ZipInputStream(new java.io.FileInputStream(clientJarFile));
        } catch (Exception e) {
            // 捕获读取错误
            JOptionPane.showMessageDialog(null,
                    "无法读取客户端文件!\n错误: " + e.getMessage(),
                    "加载错误",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        LinkedList<byte[]> jarAllClass = getJarAllClass(jarStream);
        jarStream.close();

        unlockReflection(getUnsafe(), this.getClass());

        Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
        defineClass.setAccessible(true);

        long timeout = System.currentTimeMillis() + 3000;
        while (!jarAllClass.isEmpty() && System.currentTimeMillis() < timeout) {
            try {
                byte[] pickClass = jarAllClass.get(new Random().nextInt(jarAllClass.size()));
                defineClass.invoke(targetClassLoader, pickClass, 0, pickClass.length);
                jarAllClass.remove(pickClass);
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }

        runClient(targetClassLoader);
    }

    public native void runClient(ClassLoader targetClassLoader);

    private LinkedList<byte[]> getJarAllClass(ZipInputStream zipInputStream) {
        LinkedList<byte[]> result = new LinkedList<>();
        try {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class") && !entry.getName().endsWith("module-info.class")) {
                    result.add(zipInputStream.readAllBytes());
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private Unsafe getUnsafe() throws Exception {
        Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        return (Unsafe) theUnsafeField.get(null);
    }

    private void unlockReflection(Unsafe unsafe, Class target) throws Exception {
        long addr = unsafe.objectFieldOffset(Class.class.getDeclaredField("module"));
        unsafe.getAndSetObject(target, addr, ClassLoader.class.getModule());
    }

}