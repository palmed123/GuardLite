package cnm.obsoverlay.modules.impl.render;

import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.FloatValue;

@ModuleInfo(
   name = "MotionCamera",
   description = "Smooth camera motion effect in third person view",
   category = Category.RENDER
)
public class MotionCamera extends Module {
   public FloatValue interpolation = ValueBuilder.create(this, "Interpolation")
      .setMinFloatValue(0.01F)
      .setMaxFloatValue(0.5F)
      .setDefaultFloatValue(0.05F)
      .setFloatStep(0.01F)
      .build()
      .getFloatValue();
}

