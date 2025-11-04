package cnm.mixin.O;

import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.modules.impl.render.NameTags;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class EntityRendererTransformer extends ASMTransformer {
    public EntityRendererTransformer() {
        super(EntityRenderer.class);
    }

    public static boolean shouldCancelNameTag(Entity entity) {
        return entity instanceof Player && Naven.getInstance().getModuleManager().getModule(NameTags.class).isEnabled();
    }

    @Inject(method = "renderNameTag", desc = "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void injectRenderNameTag(MethodNode node) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 1)); // pEntity
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(EntityRendererTransformer.class),
                "shouldCancelNameTag",
                "(Lnet/minecraft/world/entity/Entity;)Z",
                false
        ));
        LabelNode continueLabel = new LabelNode();
        list.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
        list.add(new InsnNode(Opcodes.RETURN));
        list.add(continueLabel);
        try {
            node.instructions.insert(list);
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][EntityRendererTransformer] Failed to insert head cancel: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}


