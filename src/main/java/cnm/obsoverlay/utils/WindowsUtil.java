package cnm.obsoverlay.utils;

import java.awt.*;

public class WindowsUtil {
    public static void info(String text) {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage("");

            TrayIcon trayIcon = new TrayIcon(image, "Java Tray");
            trayIcon.setImageAutoSize(true);

            try {
                tray.add(trayIcon);

                trayIcon.displayMessage("PacketDebug", text, TrayIcon.MessageType.INFO);

            } catch (AWTException ignored) {}
        } else {
            System.out.println("Success -> " + text);
        }
    }

    public static void success(String text) {
        info(text);
    }

    public static void warn(String text) {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage("");

            TrayIcon trayIcon = new TrayIcon(image, "Java Tray");
            trayIcon.setImageAutoSize(true);

            try {
                tray.add(trayIcon);

                trayIcon.displayMessage("Loratadine", text, TrayIcon.MessageType.WARNING);

            } catch (AWTException ignored) {}
        } else {
            System.out.println("Success -> " + text);
        }
    }

    public static void error(String text) {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage("");

            TrayIcon trayIcon = new TrayIcon(image, "Java Tray");
            trayIcon.setImageAutoSize(true);

            try {
                tray.add(trayIcon);

                trayIcon.displayMessage("Loratadine", text, TrayIcon.MessageType.ERROR);

            } catch (AWTException ignored) {}
        } else {
            System.out.println("Success -> " + text);
        }
    }

    public static void clearTheConsole() {
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (Throwable ignored) {}
    }
}
