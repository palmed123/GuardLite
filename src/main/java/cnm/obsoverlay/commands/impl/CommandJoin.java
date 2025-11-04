package cnm.obsoverlay.commands.impl;

import cnm.obsoverlay.commands.Command;
import cnm.obsoverlay.commands.CommandInfo;
import cnm.obsoverlay.utils.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

@CommandInfo(
   name = "join",
   description = "Connect to the server",
   aliases = {"j", "connect"}
)
public class CommandJoin extends Command {
   
   @Override
   public void onCommand(String[] args) {
      if (args.length == 1) {
         String serverAddress = args[0];

         String[] parts = serverAddress.split(":");
         if (parts.length != 2) {
            ChatUtils.addChatMessage("Invalid server address format");
            return;
         }
         
         String host = parts[0];
         int port;
         
         try {
            port = Integer.parseInt(parts[1]);
            if (port < 1 || port > 65535) {
               ChatUtils.addChatMessage("Port number must be between 1 and 65535");
               return;
            }
         } catch (NumberFormatException e) {
            ChatUtils.addChatMessage("Port number must be a valid number");
            return;
         }

         ServerData serverData = new ServerData("Temporary server", host + ":" + port, false);

         Minecraft mc = Minecraft.getInstance();

         ServerAddress address = ServerAddress.parseString(host + ":" + port);

         try {
            ConnectScreen.startConnecting(mc.screen, mc, address, serverData, false);
            ChatUtils.addChatMessage("Connecting to server: " + host + ":" + port);
         } catch (Exception e) {
            ChatUtils.addChatMessage("Error connecting to server: " + e.getMessage());
         }
         
      } else {
         ChatUtils.addChatMessage("Usage: .join <address:port>");
         ChatUtils.addChatMessage("Example: .join play.bjd-mc.com:25565");
      }
   }

   @Override
   public String[] onTab(String[] args) {
      return new String[0];
   }
}