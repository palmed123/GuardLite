package cnm.obsoverlay.events.impl;

import cnm.obsoverlay.events.api.events.Event;
import com.mojang.blaze3d.vertex.PoseStack;

public class EventRender implements Event {
   private final float renderPartialTicks;
   private final PoseStack pMatrixStack;

   public float getRenderPartialTicks() {
      return this.renderPartialTicks;
   }

   public PoseStack getPMatrixStack() {
      return this.pMatrixStack;
   }

   public EventRender(float renderPartialTicks, PoseStack pMatrixStack) {
      this.renderPartialTicks = renderPartialTicks;
      this.pMatrixStack = pMatrixStack;
   }
}
