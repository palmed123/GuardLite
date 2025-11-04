package cnm.obsoverlay.modules.impl.render;

import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.ui.ClickGUI;

@ModuleInfo(
   name = "ClickGUI",
   category = Category.RENDER,
   description = "The ClickGUI"
)
public class ClickGUIModule extends Module {
   ClickGUI clickGUI = null;

   @Override
   protected void initModule() {
      super.initModule();
      this.setKey(344);
   }

   @Override
   public void onEnable() {
      if (this.clickGUI == null) {
         this.clickGUI = new ClickGUI();
      }

      mc.setScreen(this.clickGUI);
      this.toggle();
   }
}
