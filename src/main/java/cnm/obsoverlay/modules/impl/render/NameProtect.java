package cnm.obsoverlay.modules.impl.render;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.impl.EventRenderTabOverlay;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

@ModuleInfo(
   name = "NameProtect",
   description = "Protect your name",
   category = Category.RENDER
)
public class NameProtect extends Module {
   private static final String DEFAULT_NAME = "§7[§c服主§7]§f张梦媛";
   private String customName = DEFAULT_NAME;

   public String getDisplayName(String string) {
      if (!this.isEnabled() || mc.player == null) {
         return string;
      } else {
         return string.contains(mc.player.getName().getString()) ? StringUtils.replace(string, mc.player.getName().getString(), customName) : string;
      }
   }

   public void setCustomName(String name) {
      this.customName = name;
   }

   public void resetName() {
      this.customName = DEFAULT_NAME;
   }

   public String getCustomName() {
      return this.customName;
   }

   @EventTarget
   public void onRenderTab(EventRenderTabOverlay e) {
      e.setComponent(Component.literal(this.getDisplayName(e.getComponent().getString())));
   }
}
