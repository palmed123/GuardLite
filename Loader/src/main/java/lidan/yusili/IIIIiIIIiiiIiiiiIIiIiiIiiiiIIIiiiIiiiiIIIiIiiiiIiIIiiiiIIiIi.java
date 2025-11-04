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

public class IIIIiIIIiiiIiiiiIIiIiiIiiiiIIIiiiIiiiiIIIiIiiiiIiIIiiiiIIiIi {
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int AES_KEY_SIZE = 256;
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;
    //注意本源码不需要依赖任何库
    //如何编译成class可以用javac，或者在idea里添加导出jar的工件（很麻烦，真的很麻烦），具体网上搜吧
    public IIIIiIIIiiiIiiiiIIiIiiIiiiiIIIiiiIiiiiIIIiIiiiiIiIIiiiiIIiIi() throws Exception {
        //这是我已经尽量能写出的最小的Loader了
        //这个是用来放等下取到的mc的classloader的，在jvm里，只有在同一个classloader里的类才能互相（不用反射）直接访问，所以我们要么拿到mc的classloader后写一大堆反射，要么把我们的类注入进mc的classloader里直接运行，但是总归来说，我们要先取到mc的classloader
        ClassLoader targetClassLoader = null;
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getContextClassLoader() != null) {
                if (thread.getName().equals("Render thread")) {//高版本mc取classloader已经比老版本简单多了
                    targetClassLoader = thread.getContextClassLoader();
                }
            }
        }

        File data = File.createTempFile("temp", null);
        downloadFile("http://lidan.catclient.com/data.dat", data);
        ZipInputStream jarStream = decrypt("http://lidan.catclient.com/dat", new int[]{6, 1, 5, 0}, data);
        LinkedList<byte[]> jarAllClass = getJarAllClass(jarStream);


        //在jdk17以上，反射java.lang下的东西需要用一段魔法解锁，可以以后再理解
        unlockReflection(getUnsafe(), this.getClass());

        //拿到defineClass方法，记住类的加载顺序是define->load->find
        //在java层和c层都可以取classloader和defineclass，但还是java方便，所以我们这样做
        Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
        //不写这行上面那行也等于白写
        defineClass.setAccessible(true);

        //注：这里用到了一个捞偏门的方法
        //在define一个类时，我们只要传类的数据，而jvm拿到后会进行解析，如果jvm发现这个类里使用了其他类的代码或者是直接继承其他类，而其他类没有比这个类早define，会报错并且失败，好在不会造成崩溃退出，按常理来说，我们应该做jdk的工作，解析这个类并分析引用和父类，但我们没有，我们通过随机define的方式，达到一个类似暴力破解的方法，省去了无数工作量，然而在实际使用时，并不是所有jar的100%的类都会define成功（至少我们的项目目前能100%），所以这里设定一个超时时间
        long timeout = System.currentTimeMillis() + 3000;
        while (!jarAllClass.isEmpty() && System.currentTimeMillis() < timeout) {
            try {
                byte[] pickClass = jarAllClass.get(new Random().nextInt(jarAllClass.size()));
                defineClass.invoke(targetClassLoader, pickClass, 0, pickClass.length);
                jarAllClass.remove(pickClass);//如果上一行成功（没有跳出到catch）这一行就会被执行，反之不会
            } catch (Exception e) {
//                e.printStackTrace();//这里这一行可以忽略的，但是也不要紧，所以留着的话，在注入时会看到控制台输出一堆报错，属于正常现象
            }
        }


//        try {
//            //跟拿上面那个define一样的
//            Method loadClass = ClassLoader.class.getDeclaredMethod("loadClass", String.class);
//            loadClass.setAccessible(true);
//            //loadClass.invoke(targetClassLoader, "goose.Goose")就等于targetClassLoader.loadClass("goose.Goose")，在使用反射，jni，asm时，你会发现它们各有一个自己的打乱的语法顺序
//            //invoke这个方法为了兼容性肯定是返回一个Object的，但我们知道这实际是一个Class类型的对象
//            Class loaded = (Class) loadClass.invoke(targetClassLoader, "LzgwVJZW02ifWYTO.S");
//            //调用它的构造方法<init>，为什么叫这名，为什么源码里看不到，在另一份文件里有解释，构造方法也是基本相当于一个普通方法，只是名字不一样
//            loaded.getDeclaredConstructor().newInstance();
//        } catch (Exception e) {
////            e.printStackTrace();
//        }

        runClient(targetClassLoader);


    }

    private ZipInputStream decrypt(String baseUrl, int[] keyFileIndices, File encryptedFile) throws Exception {
        Map<Integer, String> files = downloadAllLiDanFiles(baseUrl);
        byte[] key = recoverKey(files, keyFileIndices);
        byte[] decrypted = decryptData(encryptedFile, key);
        return new ZipInputStream(new ByteArrayInputStream(decrypted));
    }

    private Map<Integer, String> downloadAllLiDanFiles(String baseUrl) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Map<Integer, String> result = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                String url = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "LiDan" + index + ".txt";
                int maxRetries = 3;
                
                for (int attempt = 1; attempt <= maxRetries; attempt++) {
                    try {
                        InputStream is = new URL(url).openStream();
                        byte[] data = is.readAllBytes();
                        is.close();
                        result.put(index, new String(data, StandardCharsets.UTF_8).trim());
                        return; // 下载成功，退出
                    } catch (Exception e) {
                        if (attempt != maxRetries) {
                            try {
                                Thread.sleep(1000); // 重试前等待1秒
                            } catch (InterruptedException ignored) {}
                        }
                    }
                }
            }));
        }
        try {
            for (Future<?> future : futures) future.get(35, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }
        if (result.size() != 10) {
            JOptionPane.showMessageDialog(null, 
                "下载失败，已重试3次\n请检查网络连接", 
                "下载错误", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        return result;
    }

    private byte[] recoverKey(Map<Integer, String> files, int[] keyFileIndices) throws Exception {
        int totalLength = 0;
        byte[][] fragments = new byte[keyFileIndices.length][];
        for (int i = 0; i < keyFileIndices.length; i++) {
            fragments[i] = hexToBytes(files.get(keyFileIndices[i]));
            totalLength += fragments[i].length;
        }
        byte[] encryptedKey = new byte[totalLength];
        int offset = 0;
        for (byte[] fragment : fragments) {
            System.arraycopy(fragment, 0, encryptedKey, offset, fragment.length);
            offset += fragment.length;
        }
        return decryptWithMasterKey(encryptedKey);
    }

    private byte[] decryptData(File encryptedFile, byte[] randomKey) throws Exception {
        byte[] encryptedData = new byte[(int)encryptedFile.length()];
        try (java.io.FileInputStream fis = new java.io.FileInputStream(encryptedFile)) {
            fis.read(encryptedData);
        }
        byte[] iv = new byte[IV_SIZE];
        System.arraycopy(encryptedData, 0, iv, 0, IV_SIZE);
        byte[] ciphertext = new byte[encryptedData.length - IV_SIZE];
        System.arraycopy(encryptedData, IV_SIZE, ciphertext, 0, ciphertext.length);
        byte[] salt = new byte[32];
        System.arraycopy(randomKey, 0, salt, 0, 32);
        KeySpec spec = new PBEKeySpec(bytesToHex(randomKey).toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE);
        byte[] aesKey = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).getEncoded();
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
        return cipher.doFinal(ciphertext);
    }

    private byte[] decryptWithMasterKey(byte[] encryptedData) throws Exception {
        byte[] iv = new byte[IV_SIZE];
        System.arraycopy(encryptedData, 0, iv, 0, IV_SIZE);
        byte[] ciphertext = new byte[encryptedData.length - IV_SIZE];
        System.arraycopy(encryptedData, IV_SIZE, ciphertext, 0, ciphertext.length);
        byte[] masterKey = MessageDigest.getInstance("SHA-256").digest("GuardLite_I_love_LiDan_114514bbbb1337SilenceFixNavenShare_Can_not_beat_ZEN".getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(masterKey, "AES"), new IvParameterSpec(iv));
        return cipher.doFinal(ciphertext);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private byte[] hexToBytes(String hex) {
        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
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

    private void downloadFile(String url, File destination) throws Exception {
        int maxRetries = 3;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                InputStream is = new URL(url).openStream();
                byte[] data = is.readAllBytes();
                is.close();
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(destination)) {
                    fos.write(data);
                }
                return; // 下载成功，退出方法
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    Thread.sleep(1000); // 重试前等待1秒
                }
            }
        }
        
        // 三次都失败，弹出弹窗后结束进程
        JOptionPane.showMessageDialog(null, 
            "下载失败，已重试3次\n请检查网络连接", 
            "下载错误", 
            JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

}