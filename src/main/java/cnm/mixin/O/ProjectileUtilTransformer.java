package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.utils.BlinkingPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.function.Predicate;

public class ProjectileUtilTransformer extends ASMTransformer {
    public ProjectileUtilTransformer() {
        super(ProjectileUtil.class);
    }

    public static List<Entity> hook(Level instance, Entity pEntity, AABB pBoundingBox, Predicate<? super Entity> pPredicate) {
        List<Entity> entities = instance.getEntities(pEntity, pBoundingBox, pPredicate);
        entities.removeIf(entity -> entity instanceof BlinkingPlayer);
        return entities;
    }

    @Inject(method = "getEntityHitResult", desc = "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;")
    private static void injectGetEntityHitResult(MethodNode node) {
        String mappedGetEntities = Mapping.get(Level.class, "getEntities", "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;");
        boolean replaced = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.owner.equals(Type.getInternalName(Level.class))
                    && m.name.equals(mappedGetEntities)
                    && m.desc.equals("(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(ProjectileUtilTransformer.class),
                        "hook",
                        "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
                        false
                );
                node.instructions.set(m, redirect);
                replaced = true;
                // 不 break，理论上仅一次
            }
        }
        if (!replaced && MixinLoader.debugging) {
            System.out.println("[ASM][ProjectileUtilTransformer] Failed to redirect Level.getEntities in getEntityHitResult");
        }
    }
}


