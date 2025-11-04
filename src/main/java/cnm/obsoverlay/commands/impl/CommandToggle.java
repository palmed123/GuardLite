package cnm.obsoverlay.commands.impl;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.commands.Command;
import cnm.obsoverlay.commands.CommandInfo;
import cnm.obsoverlay.exceptions.NoSuchModuleException;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.utils.ChatUtils;

@CommandInfo(
   name = "toggle",
   description = "Toggle a module",
   aliases = {"t"}
)
public class CommandToggle extends Command {
   @Override
   public void onCommand(String[] args) {
      if (args.length == 1) {
         String moduleName = args[0];

         try {
            Module module = Naven.getInstance().getModuleManager().getModule(moduleName);
            if (module != null) {
               module.toggle();
            } else {
               ChatUtils.addChatMessage("Invalid module.");
            }
         } catch (NoSuchModuleException var4) {
            ChatUtils.addChatMessage("Invalid module.");
         }
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
