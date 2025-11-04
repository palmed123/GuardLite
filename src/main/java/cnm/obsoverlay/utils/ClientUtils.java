package cnm.obsoverlay.utils;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.files.FileManager;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ClientUtils implements Wrapper {
    public static void mc_debugMessage_no_prefix(String debugMessage) {
        if(mc.level == null || mc.player == null ) return;

        // 在1.20.1中，TextComponent被替换为Component.literal
        mc.gui.getChat().addMessage(Component.literal(debugMessage));
    }

    public static void log(String debugMessage) {
        mc_debugMessage_no_prefix("§8[§c§l" + Naven.CLIENT_DISPLAY_NAME + "§8]§c§d" + " " + debugMessage);
    }

    public static void deepseek(String debugMessage) {
        mc_debugMessage_no_prefix("§8[§c§l" + "DeepSeek R1" + "§8]§c§d" + debugMessage);
    }

    public static void displayIRC(String text) {
        mc_debugMessage_no_prefix("§r§l[§bLoratadine§r§l] §r" + text);
    }

    public static void throwableBug(String targetName, Throwable e) {
        try {
            // 确保文件路径存在
            File errorFile = new File(FileManager.clientFolder, String.valueOf(System.currentTimeMillis()));
            Files.write(errorFile.toPath(), (e.getMessage() != null ? e.getMessage() : "无错误消息|" + targetName + "|" + e).getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {}
    }
}