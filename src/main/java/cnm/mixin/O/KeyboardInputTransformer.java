package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.impl.EventMoveInput;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;

public class KeyboardInputTransformer extends ASMTransformer {
    public KeyboardInputTransformer() {
        super(KeyboardInput.class);
    }

    public static void handleTickTail(Object self, boolean isMovingSlowly) {
        try {
            Class<?> inputCls = Input.class;

            String fUp = Mapping.get(Input.class, "up", null);
            String fDown = Mapping.get(Input.class, "down", null);
            String fLeft = Mapping.get(Input.class, "left", null);
            String fRight = Mapping.get(Input.class, "right", null);
            String fJump = Mapping.get(Input.class, "jumping", null);
            String fSneak = Mapping.get(Input.class, "shiftKeyDown", null);
            String fForward = Mapping.get(Input.class, "forwardImpulse", null);
            String fStrafe = Mapping.get(Input.class, "leftImpulse", null);

            Field up = inputCls.getDeclaredField(fUp);
            Field down = inputCls.getDeclaredField(fDown);
            Field left = inputCls.getDeclaredField(fLeft);
            Field right = inputCls.getDeclaredField(fRight);
            Field jumping = inputCls.getDeclaredField(fJump);
            Field sneak = inputCls.getDeclaredField(fSneak);
            Field forward = inputCls.getDeclaredField(fForward);
            Field strafe = inputCls.getDeclaredField(fStrafe);
            up.setAccessible(true);
            down.setAccessible(true);
            left.setAccessible(true);
            right.setAccessible(true);
            jumping.setAccessible(true);
            sneak.setAccessible(true);
            forward.setAccessible(true);
            strafe.setAccessible(true);

            boolean upVal = up.getBoolean(self);
            boolean downVal = down.getBoolean(self);
            boolean leftVal = left.getBoolean(self);
            boolean rightVal = right.getBoolean(self);
            boolean jumpingVal = jumping.getBoolean(self);
            boolean sneakVal = sneak.getBoolean(self);

            float forwardImpulse = (upVal == downVal) ? 0.0F : (upVal ? 1.0F : -1.0F);
            float leftImpulse = (leftVal == rightVal) ? 0.0F : (leftVal ? 1.0F : -1.0F);

            EventMoveInput event = new EventMoveInput(forwardImpulse, leftImpulse, jumpingVal, sneakVal, 0.3);
            Naven.getInstance().getEventManager().call(event);

            double sneakMultiplier = event.getSneakSlowDownMultiplier();
            float outForward = event.getForward();
            float outStrafe = event.getStrafe();
            boolean outJump = event.isJump();
            boolean outSneak = event.isSneak();

            if (isMovingSlowly) {
                outStrafe = (float) (outStrafe * sneakMultiplier);
                outForward = (float) (outForward * sneakMultiplier);
            }

            forward.setFloat(self, outForward);
            strafe.setFloat(self, outStrafe);
            jumping.setBoolean(self, outJump);
            sneak.setBoolean(self, outSneak);
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][KeyboardInputTransformer] handleTickTail failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @Inject(method = "tick", desc = "(ZF)V")
    private void injectTickTail(MethodNode node) {
        if (node == null) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][KeyboardInputTransformer] Target method not found: tick TAIL (node is null)");
            }
            return;
        }
        boolean inserted = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                InsnList list = new InsnList();
                list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                list.add(new VarInsnNode(Opcodes.ILOAD, 1));
                list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(KeyboardInputTransformer.class),
                        "handleTickTail",
                        "(Ljava/lang/Object;Z)V",
                        false
                ));
                try {
                    node.instructions.insertBefore(insn, list);
                    inserted = true;
                } catch (Throwable ex) {
                    if (MixinLoader.debugging) {
                        System.out.println("[ASM][KeyboardInputTransformer] Failed to insert handleTickTail at tick TAIL: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
                break;
            }
        }
        if (!inserted && MixinLoader.debugging) {
            System.out.println("[ASM][KeyboardInputTransformer] Did not find RETURN in tick for TAIL insertion");
        }
    }
}


