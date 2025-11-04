package cnm.obsoverlay.commands.impl;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.commands.Command;
import cnm.obsoverlay.commands.CommandInfo;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventMotion;
import cnm.obsoverlay.utils.Wrapper;
import net.minecraft.client.gui.screens.LanguageSelectScreen;

@CommandInfo(
   name = "language",
   description = "Open language gui.",
   aliases = {"lang"}
)
public class CommandLanguage extends Command implements Wrapper {
   @Override
   public void onCommand(String[] args) {
      Naven.getInstance().getEventManager().register(new Object() {
         @EventTarget
         public void onMotion(EventMotion e) {
            if (e.getType() == EventType.PRE) {
               mc.setScreen(new LanguageSelectScreen(null, mc.options, mc.getLanguageManager()));
               Naven.getInstance().getEventManager().unregister(this);
            }
         }
      });
   }

   @Override
   public String[] onTab(String[] args) {
      return new String[0];
   }
}
