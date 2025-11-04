package cnm.obsoverlay.utils;

import cnm.obsoverlay.Naven;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

public class ChatUtils implements Wrapper {
   private static final String PREFIX = "§7[§b" + Naven.CLIENT_DISPLAY_NAME.charAt(0) + "§7] ";

   public static void component(Component component) {
      ChatComponent chat = mc.gui.getChat();
      chat.addMessage(component);
   }

   public static void addChatMessage(String message) {
      addChatMessage(true, message);
   }

   public static void addChatMessage(boolean prefix, String message) {
      component(Component.nullToEmpty((prefix ? PREFIX : "") + message));
   }
}
