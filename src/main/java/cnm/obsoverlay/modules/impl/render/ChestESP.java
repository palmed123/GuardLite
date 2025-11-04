package cnm.obsoverlay.modules.impl.render;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventMotion;
import cnm.obsoverlay.events.impl.EventPacket;
import cnm.obsoverlay.events.impl.EventRender;
import cnm.obsoverlay.events.impl.EventRespawn;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.utils.BlockUtils;
import cnm.obsoverlay.utils.ChunkUtils;
import cnm.obsoverlay.utils.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;

@ModuleInfo(
   name = "ChestESP",
   description = "Highlights chests",
   category = Category.RENDER
)
public class ChestESP extends Module {
   private static final float[] chestColor = new float[]{0.0F, 1.0F, 0.0F};
   private static final float[] openedChestColor = new float[]{1.0F, 0.0F, 0.0F};
   private final List<BlockPos> openedChests = new CopyOnWriteArrayList<>();
   private final List<AABB> renderBoundingBoxes = new CopyOnWriteArrayList<>();

   @Override
   public void onDisable() {
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      this.openedChests.clear();
   }

   @EventTarget
   public void onPacket(EventPacket e) {
      if (e.getType() == EventType.RECEIVE && e.getPacket() instanceof ClientboundBlockEventPacket) {
         ClientboundBlockEventPacket packet = (ClientboundBlockEventPacket)e.getPacket();
         if ((packet.getBlock() == Blocks.CHEST || packet.getBlock() == Blocks.TRAPPED_CHEST) && packet.getB0() == 1 && packet.getB1() == 1) {
            this.openedChests.add(packet.getPos());
         }
      }
   }

   @EventTarget
   public void onTick(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         ArrayList<BlockEntity> blockEntities = ChunkUtils.getLoadedBlockEntities().collect(Collectors.toCollection(ArrayList::new));
         this.renderBoundingBoxes.clear();

         for (BlockEntity blockEntity : blockEntities) {
            if (blockEntity instanceof ChestBlockEntity) {
               ChestBlockEntity chestBE = (ChestBlockEntity)blockEntity;
               AABB box = this.getChestBox(chestBE);
               if (box != null) {
                  this.renderBoundingBoxes.add(box);
               }
            }
         }
      }
   }

   private AABB getChestBox(ChestBlockEntity chestBE) {
      BlockState state = chestBE.getBlockState();
      if (!state.hasProperty(ChestBlock.TYPE)) {
         return null;
      } else {
         ChestType chestType = (ChestType)state.getValue(ChestBlock.TYPE);
         if (chestType == ChestType.LEFT) {
            return null;
         } else {
            BlockPos pos = chestBE.getBlockPos();
            AABB box = BlockUtils.getBoundingBox(pos);
            if (chestType != ChestType.SINGLE) {
               BlockPos pos2 = pos.relative(ChestBlock.getConnectedDirection(state));
               if (BlockUtils.canBeClicked(pos2)) {
                  AABB box2 = BlockUtils.getBoundingBox(pos2);
                  box = box.minmax(box2);
               }
            }

            return box;
         }
      }
   }

   @EventTarget
   public void onRender(EventRender e) {
      PoseStack stack = e.getPMatrixStack();
      stack.pushPose();
      RenderSystem.disableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      Tesselator tessellator = RenderSystem.renderThreadTesselator();
      BufferBuilder bufferBuilder = tessellator.getBuilder();

      for (AABB box : this.renderBoundingBoxes) {
         BlockPos pos = BlockPos.containing(box.minX, box.minY, box.minZ);
         float[] color = this.openedChests.contains(pos) ? openedChestColor : chestColor;
         RenderSystem.setShaderColor(color[0], color[1], color[2], 0.25F);
         RenderUtils.装女人(bufferBuilder, stack.last().pose(), box);
      }

      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      stack.popPose();
   }
}
