package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.impl.EventRender;
import cnm.obsoverlay.events.impl.EventRender2D;
import cnm.obsoverlay.events.impl.EventRenderAfterWorld;
import cnm.obsoverlay.modules.impl.render.FullBright;
import cnm.obsoverlay.modules.impl.render.MotionCamera;
import cnm.obsoverlay.modules.impl.render.NoHurtCam;
import cnm.obsoverlay.utils.math.MathUtils;
import cnm.obsoverlay.utils.RenderUtils;
import cnm.obsoverlay.utils.Wrapper;
import cnm.obsoverlay.utils.auth.AuthUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector4f;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GameRendererTransformer extends ASMTransformer implements Wrapper {
    private static double prevRenderX = 0.0;
    private static double prevRenderY = 0.0;
    private static double prevRenderZ = 0.0;
    private static CameraType lastCameraType = null;
    static List<Vector4f> crash = new ArrayList<>();

    public GameRendererTransformer() {
        super(GameRenderer.class);
    }

    // ===== Helpers =====
    public static void onRenderLevel(float partialTicks, PoseStack pose) {
        Naven.getInstance().getEventManager().call(new EventRender(partialTicks, pose));
    }

    public static void onRenderWorldTail() {
        Naven.getInstance().getEventManager().call(new EventRenderAfterWorld());
    }

    public static boolean shouldOverrideNightVision() {
        FullBright module = (FullBright) Naven.getInstance().getModuleManager().getModule(FullBright.class);
        return module != null && module.isEnabled();
    }

    public static float computeNightVisionScale(LivingEntity entity, float nanoTime) {
        FullBright module = (FullBright) Naven.getInstance().getModuleManager().getModule(FullBright.class);
        return module != null ? module.brightness.getCurrentValue() : 1.0F;
    }

//    public static void onRenderTailBlur(Minecraft minecraft, float tickDelta) {
//        MotionBlur motionblur = MotionBlur.instance;
//        if (motionblur != null && motionblur.isEnabled() && minecraft != null && minecraft.player != null && motionblur.shader != null) {
//            motionblur.shader.process(tickDelta);
//        }
//    }

    public static void onRender2D(Minecraft minecraft, RenderBuffers renderBuffers) {
        GuiGraphics e = new GuiGraphics(minecraft, renderBuffers.bufferSource());

        if (AuthUtils.errorCount > 3) {
            e.pose().pushPose();
            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();
            crash.add(new Vector4f(MathUtils.getRandomIntInRange(0, width),  MathUtils.getRandomIntInRange(0, height), MathUtils.getRandomIntInRange(0, width),  MathUtils.getRandomIntInRange(0, height)));
            for (Vector4f vec4f : crash) {
                RenderUtils.fill(e.pose(), vec4f.x(), vec4f.y(), vec4f.z(), vec4f.w(), new Color(MathUtils.getRandomIntInRange(0, 255), MathUtils.getRandomIntInRange(0, 255), MathUtils.getRandomIntInRange(0, 255), MathUtils.getRandomIntInRange(0, 255)).getRGB());
            }
            e.pose().popPose();
        }
        EventRender2D event = new EventRender2D(e.pose(), e);
        Naven.getInstance().getEventManager().call(event);
    }

    public static boolean shouldCancelHurtCam() {
        NoHurtCam module = (NoHurtCam) Naven.getInstance().getModuleManager().getModule(NoHurtCam.class);
        return module != null && module.isEnabled();
    }

    public static boolean shouldApplyMotionCamera() {
        MotionCamera module = (MotionCamera) Naven.getInstance().getModuleManager().getModule(MotionCamera.class);
        Minecraft mc = Minecraft.getInstance();
        return module != null && module.isEnabled() 
            && !mc.options.getCameraType().isFirstPerson() 
            && mc.player != null 
            && mc.level != null;
    }

    public static void applyMotionCamera(float partialTicks, PoseStack poseStack) {
        MotionCamera module = (MotionCamera) Naven.getInstance().getModuleManager().getModule(MotionCamera.class);
        Minecraft mc = Minecraft.getInstance();
        
        if (module == null || !module.isEnabled()) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Entity entity = camera.getEntity();

        if (entity != null) {
            float eyeHeight = entity.getEyeHeight();
            float interpolation = module.interpolation.getCurrentValue();
            double renderX = entity.xo + (entity.getX() - entity.xo) * (double) partialTicks;
            double renderY = entity.yo + (entity.getY() - entity.yo) * (double) partialTicks + (double) eyeHeight;
            double renderZ = entity.zo + (entity.getZ() - entity.zo) * (double) partialTicks;

            CameraType currentCameraType = mc.options.getCameraType();
            
            // 检测视角切换（从第一人称切换到第三人称）
            if (lastCameraType != currentCameraType) {
                // 重置相机位置到玩家眼部位置
                prevRenderX = renderX;
                prevRenderY = renderY;
                prevRenderZ = renderZ;
                lastCameraType = currentCameraType;
            }

            prevRenderX = prevRenderX + (renderX - prevRenderX) * interpolation;
            prevRenderY = prevRenderY + (renderY - prevRenderY) * interpolation;
            prevRenderZ = prevRenderZ + (renderZ - prevRenderZ) * interpolation;

            if (currentCameraType == CameraType.THIRD_PERSON_BACK) {
                poseStack.translate(renderX - prevRenderX, renderY - prevRenderY, renderZ - prevRenderZ);
            } else {
                poseStack.translate(prevRenderX - renderX, renderY - prevRenderY, prevRenderZ - renderZ);
            }
        }
    }

    // ===== Injections =====
    @Inject(method = "renderLevel", desc = "(FJLcom/mojang/blaze3d/vertex/PoseStack;)V")
    private void injectRenderLevel(MethodNode node) {
        String mappedRenderHand = Mapping.get(GameRenderer.class, "renderHand", null);
        boolean injected = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.GETFIELD) {
                FieldInsnNode f = (FieldInsnNode) insn;
                if (f.owner.equals(Type.getInternalName(GameRenderer.class)) && f.name.equals(mappedRenderHand) && f.desc.equals("Z")) {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(Opcodes.FLOAD, 1));
                    list.add(new VarInsnNode(Opcodes.ALOAD, 4));
                    list.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(GameRendererTransformer.class),
                            "onRenderLevel",
                            "(FLcom/mojang/blaze3d/vertex/PoseStack;)V",
                            false
                    ));
                    node.instructions.insertBefore(insn, list);
                    injected = true;
                    break;
                }
            }
        }
        if (!injected && MixinLoader.debugging) {
            System.out.println("[ASM][GameRendererTransformer] Failed to inject EventRender at renderLevel (renderHand field not found)");
        }
    }

    @Inject(method = "renderLevel", desc = "(FJLcom/mojang/blaze3d/vertex/PoseStack;)V")
    private void injectMotionCamera(MethodNode node) {
        // 查找 RenderSystem.setInverseViewRotationMatrix 调用
        String renderSystemOwner = "com/mojang/blaze3d/systems/RenderSystem";
        boolean motionCameraInjected = false;
        
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.INVOKESTATIC && insn instanceof MethodInsnNode methodInsnNode) {
                // 查找 RenderSystem.setInverseViewRotationMatrix 调用
                if (methodInsnNode.owner.equals(renderSystemOwner) && 
                    methodInsnNode.name.contains("setInverseViewRotationMatrix")) {
                    
                    // 创建注入指令列表
                    InsnList motionCameraInstructions = new InsnList();
                    LabelNode continueLabel = new LabelNode();

                    // 调用 shouldApplyMotionCamera() 检查
                    motionCameraInstructions.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(GameRendererTransformer.class),
                            "shouldApplyMotionCamera",
                            "()Z",
                            false
                    ));

                    // 如果返回 false，跳过 motion camera
                    motionCameraInstructions.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));

                    // 加载参数
                    motionCameraInstructions.add(new VarInsnNode(Opcodes.FLOAD, 1)); // p_109090_ (float partialTicks)
                    motionCameraInstructions.add(new VarInsnNode(Opcodes.ALOAD, 4)); // p_109092_ (PoseStack)

                    // 调用 applyMotionCamera
                    motionCameraInstructions.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(GameRendererTransformer.class),
                            "applyMotionCamera",
                            "(FLcom/mojang/blaze3d/vertex/PoseStack;)V",
                            false
                    ));

                    // 添加 continueLabel
                    motionCameraInstructions.add(continueLabel);

                    // 在 setInverseViewRotationMatrix 调用之前插入
                    node.instructions.insertBefore(insn, motionCameraInstructions);
                    motionCameraInjected = true;
                    break;
                }
            }
        }

        if (!motionCameraInjected && MixinLoader.debugging) {
            System.out.println("[ASM][GameRendererTransformer] Failed to inject MotionCamera: setInverseViewRotationMatrix not found");
        }
    }

    @Inject(method = "renderLevel", desc = "(FJLcom/mojang/blaze3d/vertex/PoseStack;)V")
    private void injectRenderWorldTail(MethodNode node) {
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                InsnList list = new InsnList();
                list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(GameRendererTransformer.class),
                        "onRenderWorldTail",
                        "()V",
                        false
                ));
                node.instructions.insertBefore(insn, list);
            }
        }
    }

    @Inject(method = "getNightVisionScale", desc = "(Lnet/minecraft/world/entity/LivingEntity;F)F")
    private void injectNightVision(MethodNode node) {
        InsnList list = new InsnList();
        LabelNode cont = new LabelNode();
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(GameRendererTransformer.class),
                "shouldOverrideNightVision",
                "()Z",
                false
        ));
        list.add(new JumpInsnNode(Opcodes.IFEQ, cont));
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new VarInsnNode(Opcodes.FLOAD, 1));
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(GameRendererTransformer.class),
                "computeNightVisionScale",
                "(Lnet/minecraft/world/entity/LivingEntity;F)F",
                false
        ));
        list.add(new InsnNode(Opcodes.FRETURN));
        list.add(cont);
        node.instructions.insert(list);
    }

    @Inject(method = "render", desc = "(FJZ)V")
    private void injectRenderTailAnd2D(MethodNode node) {
        String mappedRender = Mapping.get(net.minecraft.client.gui.Gui.class, "render", "(Lnet/minecraft/client/gui/GuiGraphics;F)V");
        String mappedMinecraftField = Mapping.get(GameRenderer.class, "minecraft", null);
        String mappedRenderBuffersField = Mapping.get(GameRenderer.class, "renderBuffers", null);
        boolean inserted2D = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.owner.equals(Type.getInternalName(net.minecraft.client.gui.Gui.class))
                    && m.name.equals(mappedRender)
                    && m.desc.equals("(Lnet/minecraft/client/gui/GuiGraphics;F)V")) {
                InsnList list = new InsnList();
                list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                list.add(new FieldInsnNode(
                        Opcodes.GETFIELD,
                        Type.getInternalName(GameRenderer.class),
                        mappedMinecraftField,
                        Type.getDescriptor(Minecraft.class)
                ));
                list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                list.add(new FieldInsnNode(
                        Opcodes.GETFIELD,
                        Type.getInternalName(GameRenderer.class),
                        mappedRenderBuffersField,
                        Type.getDescriptor(RenderBuffers.class)
                ));
                list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(GameRendererTransformer.class),
                        "onRender2D",
                        "(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/renderer/RenderBuffers;)V",
                        false
                ));
                node.instructions.insertBefore(m, list);
                inserted2D = true;
                break;
            }
        }
        if (!inserted2D && MixinLoader.debugging) {
            System.out.println("[ASM][GameRendererTransformer] Failed to insert onRender2D before Gui.render");
        }

//        // Tail motion blur
//        for (AbstractInsnNode insn : node.instructions.toArray()) {
//            if (insn.getOpcode() == Opcodes.RETURN) {
//                InsnList list = new InsnList();
//                list.add(new VarInsnNode(Opcodes.ALOAD, 0));
//                list.add(new FieldInsnNode(
//                        Opcodes.GETFIELD,
//                        Type.getInternalName(GameRenderer.class),
//                        mappedMinecraftField,
//                        Type.getDescriptor(Minecraft.class)
//                ));
//                list.add(new VarInsnNode(Opcodes.FLOAD, 1));
//                list.add(new MethodInsnNode(
//                        Opcodes.INVOKESTATIC,
//                        Type.getInternalName(GameRendererTransformer.class),
//                        "onRenderTailBlur",
//                        "(Lnet/minecraft/client/Minecraft;F)V",
//                        false
//                ));
//                node.instructions.insertBefore(insn, list);
//            }
//        }
    }

    @Inject(method = "bobHurt", desc = "(Lcom/mojang/blaze3d/vertex/PoseStack;F)V")
    private void injectBobHurt(MethodNode node) {
        InsnList list = new InsnList();
        LabelNode cont = new LabelNode();
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(GameRendererTransformer.class),
                "shouldCancelHurtCam",
                "()Z",
                false
        ));
        list.add(new JumpInsnNode(Opcodes.IFEQ, cont));
        list.add(new InsnNode(Opcodes.RETURN));
        list.add(cont);
        node.instructions.insert(list);
    }
}


