package cnm.obsoverlay.commands.impl;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.commands.Command;
import cnm.obsoverlay.commands.CommandInfo;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.impl.EventKey;
import cnm.obsoverlay.exceptions.NoSuchModuleException;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.utils.ChatUtils;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;

@CommandInfo(
   name = "bind",
   description = "Bind a command to a key",
   aliases = {"b"}
)
public class CommandBind extends Command {
   @Override
   public void onCommand(String[] args) {
      if (args.length == 1) {
         final String moduleName = args[0];

         try {
            final Module module = Naven.getInstance().getModuleManager().getModule(moduleName);
            if (module != null) {
               ChatUtils.addChatMessage("Press a key to bind " + moduleName + " to.");
               final Object keyListener = new Object() {
                  @EventTarget
                  public void onKey(EventKey e) {
                     if (e.isState()) {
                        try {
                           module.setKey(e.getKey());
                           Key key = InputConstants.getKey(e.getKey(), 0);
                           if (key != null && key != InputConstants.UNKNOWN) {
                              String keyName = key.getDisplayName().getString().toUpperCase();
                              ChatUtils.addChatMessage("Bound " + moduleName + " to " + keyName + ".");
                           } else {
                              ChatUtils.addChatMessage("Bound " + moduleName + " to key code " + e.getKey() + ".");
                           }
                           Naven.getInstance().getEventManager().unregister(this);
                           Naven.getInstance().getFileManager().save();
                        } catch (Exception ex) {
                           ChatUtils.addChatMessage("Error binding key: " + ex.getMessage());
                           Naven.getInstance().getEventManager().unregister(this);
                        }
                     }
                  }
               };
               Naven.getInstance().getEventManager().register(keyListener);
            } else {
               ChatUtils.addChatMessage("Invalid module.");
            }
         } catch (NoSuchModuleException var7) {
            ChatUtils.addChatMessage("Invalid module.");
         }
      } else if (args.length == 2) {
         String moduleName = args[0];
         String keyName = args[1];

         try {
            Module module = Naven.getInstance().getModuleManager().getModule(moduleName);
            if (module != null) {
               if (keyName.equalsIgnoreCase("none")) {
                  module.setKey(InputConstants.UNKNOWN.getValue());
                  ChatUtils.addChatMessage("Unbound " + moduleName + ".");
                  Naven.getInstance().getFileManager().save();
               } else {
                  Key key = InputConstants.getKey("key.keyboard." + keyName.toLowerCase());
                  if (key != InputConstants.UNKNOWN) {
                     module.setKey(key.getValue());
                     ChatUtils.addChatMessage("Bound " + moduleName + " to " + keyName.toUpperCase() + ".");
                     Naven.getInstance().getFileManager().save();
                  } else {
                     ChatUtils.addChatMessage("Invalid key.");
                  }
               }
            } else {
               ChatUtils.addChatMessage("Invalid module.");
            }
         } catch (NoSuchModuleException var6) {
            ChatUtils.addChatMessage("Invalid module.");
         }
      } else {
         ChatUtils.addChatMessage("Usage: .bind <module> [key]");
      }
   }

   @Override
   public String[] onTab(String[] args) {
      return Naven.getInstance()
         .getModuleManager()
         .getModules()
         .stream()
         .map(Module::getName)
         .filter(name -> name.toLowerCase().startsWith(args.length == 0 ? "" : args[0].toLowerCase()))
         .toArray(String[]::new);
   }
}
