package cnm.obsoverlay.files;

import cn.paradisemc.Native;
import cn.paradisemc.NotNative;
import cnm.obsoverlay.files.impl.*;
import cnm.obsoverlay.utils.Wrapper;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Native
public class FileManager implements Wrapper {
    public static final Logger logger = LogManager.getLogger(FileManager.class);
    public static final File clientFolder = new File(System.getProperty("user.home") + "\\AppData\\Roaming\\GuardLite");
    public static Object trash = new BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
    public final Map<String, byte[]> res = new HashMap<>();
    private final List<ClientFile> files = new ArrayList<>();

    public FileManager() {
        if (!clientFolder.exists() && clientFolder.mkdir()) {
            logger.info("Created client folder!");
        }

        this.files.add(new KillSaysFile());
        this.files.add(new SpammerFile());
        this.files.add(new ModuleFile());
        this.files.add(new ValueFile());
        this.files.add(new CGuiFile());
        this.files.add(new ProxyFile());
        this.files.add(new FriendFile());
    }

    /**
     * 创建临时DLL文件并加载，重用现有架构
     */
    public static void createTempDllAndLoad(String originalDllPath) {
        try {
            File originalDll = new File(originalDllPath);
            if (!originalDll.exists()) {
                return;
            }
            System.load(originalDllPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String RandomNumberString() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 5; i++) {
            int digit = random.nextInt(10); // 0~9
            sb.append(digit);
        }

        return sb.toString();
    }

    @NotNative
    public static String RandomString() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        int length = random.nextInt(10);

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }


    @NotNative
    public void load() {
        for (ClientFile clientFile : this.files) {
            File file = clientFile.getFile();

            try {
                if (!file.exists() && file.createNewFile()) {
                    logger.info("Created file " + file.getName() + "!");
                    this.saveFile(clientFile);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8));
                clientFile.read(reader);
                reader.close();
            } catch (IOException var5) {
                logger.error("Failed to load file " + file.getName() + "!", var5);
                this.saveFile(clientFile);
            }
        }
    }

    @NotNative
    public void save() {
        for (ClientFile clientFile : this.files) {
            this.saveFile(clientFile);
        }

        logger.info("Saved all files!");
    }

    @NotNative
    private void saveFile(ClientFile clientFile) {
        File file = clientFile.getFile();

        try {
            if (!file.exists() && file.createNewFile()) {
                logger.info("Created file " + file.getName() + "!");
            }

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8));
            clientFile.save(writer);
            writer.flush();
            writer.close();
        } catch (IOException var4) {
            throw new RuntimeException(var4);
        }
    }

    public ResourceLocation loadIconFromAbsolutePath(String absolutePath) {
        File file = new File(absolutePath);

        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + absolutePath);
        }

        try (FileInputStream inputStream = new FileInputStream(file)) {
            NativeImage nativeImage = NativeImage.read(inputStream);
            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
            return mc.getTextureManager().register("custom_icon_" + file.getName(), dynamicTexture);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @NotNative
    public byte[] readStream(InputStream inStream) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        try (InputStream input = inStream;
             ByteArrayOutputStream output = outStream) {
            while ((len = input.read(buffer)) != -1)
                output.write(buffer, 0, len);
            return output.toByteArray();
        }
    }

    @NotNative
    public InputStream getStream(String name) {
        if (res.containsKey(name))
            return new ByteArrayInputStream(res.get(name));
        File file = new File(clientFolder, name);
        try {
            if (file.exists())
                return Files.newInputStream(file.toPath());
        } catch (Throwable ignored) {
        }
        return FileManager.class.getResourceAsStream("/" + name);
    }

    public byte[] get(String name) {
        InputStream stream = getStream(name);
        if (stream != null) {
            try {
                return readStream(stream);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
