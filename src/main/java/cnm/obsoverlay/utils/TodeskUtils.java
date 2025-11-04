package cnm.obsoverlay.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;

public class TodeskUtils {
   /**
    * 读取 ToDesk 配置文件中的 LoginPhone 值。
    * 优先从 C:\\Program Files\\ToDesk\\config.ini 读取，若不存在则尝试 C:\\Program Files (x86)\\ToDesk\\config.ini。
    * 非 Windows 或未找到/未包含该键时返回 null。
    */
   public static String getLoginPhone() throws IOException {
      if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
         return null;
      }

      File primary = new File("C:\\Program Files\\ToDesk\\config.ini");
      File secondary = new File("C:\\Program Files (x86)\\ToDesk\\config.ini");
      File ini = primary.exists() ? primary : (secondary.exists() ? secondary : null);
      if (ini == null || !ini.isFile()) {
         return null;
      }

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(ini.toPath())))) {
         String line;
         while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) {
               continue;
            }

            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
               continue;
            }

            String key = trimmed.substring(0, eq).trim();
            if ("LoginPhone".equalsIgnoreCase(key)) {
               String value = trimmed.substring(eq + 1).trim();
               if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                  value = value.substring(1, value.length() - 1);
               }
               return value.isEmpty() ? null : value;
            }
         }
      }

      return null;
   }
}


