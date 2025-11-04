package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventMotion;
import cnm.obsoverlay.events.impl.EventSlowdown;
import cnm.obsoverlay.events.impl.EventUpdate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class LocalPlayerTransformer extends ASMTransformer {
    public LocalPlayerTransformer() {
        super(LocalPlayer.class);
    }

    // ===== Helpers =====
    public static void onEventUpdate() {
        Naven.getInstance().getEventManager().call(new EventUpdate());
    }

    public static boolean onSlowdown(LocalPlayer player) {
        EventSlowdown event = new EventSlowdown(player.isUsingItem());
        Naven.getInstance().getEventManager().call(event);
        return event.isSlowdown();
    }

    public static void sendPositionPatch(LocalPlayer self) {
        try {
            EventMotion eventPre = new EventMotion(EventType.PRE, self.getX(), self.getY(), self.getZ(), self.getYRot(), self.getXRot(), self.onGround());
            Naven.getInstance().getEventManager().call(eventPre);
            if (eventPre.isCancelled()) {
                Naven.getInstance().getEventManager().call(new EventMotion(EventType.POST, eventPre.getYaw(), eventPre.getPitch()));
                return;
            }

            // Reflect fields
            String fWasShift = Mapping.get(LocalPlayer.class, "wasShiftKeyDown", null);
            String fXLast = Mapping.get(LocalPlayer.class, "xLast", null);
            String fYLast = Mapping.get(LocalPlayer.class, "yLast1", null);
            String fZLast = Mapping.get(LocalPlayer.class, "zLast", null);
            String fYRotLast = Mapping.get(LocalPlayer.class, "yRotLast", null);
            String fXRotLast = Mapping.get(LocalPlayer.class, "xRotLast", null);
            String fPosRem = Mapping.get(LocalPlayer.class, "positionReminder", null);
            String fLastOnGround = Mapping.get(LocalPlayer.class, "lastOnGround", null);
            String fAutoJump = Mapping.get(LocalPlayer.class, "autoJumpEnabled", null);
            String fMinecraft = Mapping.get(LocalPlayer.class, "minecraft", null);
            String fConnection = Mapping.get(LocalPlayer.class, "connection", null);

            Field wasShift = LocalPlayer.class.getDeclaredField(fWasShift);
            Field xLast = LocalPlayer.class.getDeclaredField(fXLast);
            Field yLast1 = LocalPlayer.class.getDeclaredField(fYLast);
            Field zLast = LocalPlayer.class.getDeclaredField(fZLast);
            Field yRotLast = LocalPlayer.class.getDeclaredField(fYRotLast);
            Field xRotLast = LocalPlayer.class.getDeclaredField(fXRotLast);
            Field positionReminder = LocalPlayer.class.getDeclaredField(fPosRem);
            Field lastOnGround = LocalPlayer.class.getDeclaredField(fLastOnGround);
            Field autoJumpEnabled = LocalPlayer.class.getDeclaredField(fAutoJump);
            Field minecraftField = LocalPlayer.class.getDeclaredField(fMinecraft);
            Field connectionField = LocalPlayer.class.getDeclaredField(fConnection);
            wasShift.setAccessible(true);
            xLast.setAccessible(true);
            yLast1.setAccessible(true);
            zLast.setAccessible(true);
            yRotLast.setAccessible(true);
            xRotLast.setAccessible(true);
            positionReminder.setAccessible(true);
            lastOnGround.setAccessible(true);
            autoJumpEnabled.setAccessible(true);
            minecraftField.setAccessible(true);
            connectionField.setAccessible(true);

            Method isControlledCamera = resolveMethodInHierarchy(LocalPlayer.class, "isControlledCamera", "()Z");
            Method sendIsSprintingIfNeeded = resolveMethodInHierarchy(LocalPlayer.class, "sendIsSprintingIfNeeded", "()V");
            isControlledCamera.setAccessible(true);
            sendIsSprintingIfNeeded.setAccessible(true);

            sendIsSprintingIfNeeded.invoke(self);
            boolean flag3 = self.isShiftKeyDown();
            boolean wasShiftVal = wasShift.getBoolean(self);
            if (flag3 != wasShiftVal) {
                ServerboundPlayerCommandPacket.Action action = flag3 ? ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY : ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY;
                ((ClientPacketListener) connectionField.get(self)).send(new ServerboundPlayerCommandPacket(self, action));
                wasShift.setBoolean(self, flag3);
            }

            if ((boolean) isControlledCamera.invoke(self)) {
                double d4 = eventPre.getX() - xLast.getDouble(self);
                double d0 = eventPre.getY() - yLast1.getDouble(self);
                double d1 = eventPre.getZ() - zLast.getDouble(self);
                double d2 = eventPre.getYaw() - yRotLast.getFloat(self);
                double d3 = eventPre.getPitch() - xRotLast.getFloat(self);
                int posRem = positionReminder.getInt(self) + 1;
                positionReminder.setInt(self, posRem);
                boolean flag1 = (d4 * d4 + d0 * d0 + d1 * d1) > (2.0E-4 * 2.0E-4) || posRem >= 20;
                boolean flag2 = d2 != 0.0 || d3 != 0.0;

                ClientPacketListener connection = (ClientPacketListener) connectionField.get(self);
                if (self.isPassenger()) {
                    Vec3 vec3 = self.getDeltaMovement();
                    connection.send(new ServerboundMovePlayerPacket.PosRot(vec3.x, -999.0, vec3.z, eventPre.getYaw(), eventPre.getPitch(), eventPre.isOnGround()));
                    flag1 = false;
                } else if (flag1 && flag2) {
                    connection.send(new ServerboundMovePlayerPacket.PosRot(eventPre.getX(), eventPre.getY(), eventPre.getZ(), eventPre.getYaw(), eventPre.getPitch(), eventPre.isOnGround()));
                } else if (flag1) {
                    connection.send(new ServerboundMovePlayerPacket.Pos(eventPre.getX(), eventPre.getY(), eventPre.getZ(), eventPre.isOnGround()));
                } else if (flag2) {
                    connection.send(new ServerboundMovePlayerPacket.Rot(eventPre.getYaw(), eventPre.getPitch(), eventPre.isOnGround()));
                } else if (lastOnGround.getBoolean(self) != eventPre.isOnGround()) {
                    connection.send(new ServerboundMovePlayerPacket.StatusOnly(eventPre.isOnGround()));
                }

                if (flag1) {
                    xLast.setDouble(self, eventPre.getX());
                    yLast1.setDouble(self, eventPre.getY());
                    zLast.setDouble(self, eventPre.getZ());
                    positionReminder.setInt(self, 0);
                }
                if (flag2) {
                    yRotLast.setFloat(self, eventPre.getYaw());
                    xRotLast.setFloat(self, eventPre.getPitch());
                }
                lastOnGround.setBoolean(self, eventPre.isOnGround());
                Minecraft mc = (Minecraft) minecraftField.get(self);
                autoJumpEnabled.setBoolean(self, mc.options.autoJump().get());
            }

            Naven.getInstance().getEventManager().call(new EventMotion(EventType.POST, eventPre.getYaw(), eventPre.getPitch()));
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][LocalPlayerTransformer] sendPositionPatch failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private static Method resolveMethodInHierarchy(Class<?> startOwner, String deobfName, String desc, Class<?>... paramTypes) throws NoSuchMethodException {
        Class<?> cls = startOwner;
        NoSuchMethodException last = null;
        while (cls != null) {
            String mapped = Mapping.get(cls, deobfName, desc);
            try {
                Method m = cls.getDeclaredMethod(mapped, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ex) {
                last = ex;
                cls = cls.getSuperclass();
            }
        }
        throw last != null ? last : new NoSuchMethodException(startOwner.getName() + "." + deobfName + "()");
    }

    // ===== Injections =====
    @Inject(method = "tick", desc = "()V")
    private void injectTick(MethodNode node) {
        String mappedTick = Mapping.get(AbstractClientPlayer.class, "tick", "()V");
        boolean inserted = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && (m.getOpcode() == Opcodes.INVOKESPECIAL || m.getOpcode() == Opcodes.INVOKEVIRTUAL)
                    && m.owner.equals(Type.getInternalName(AbstractClientPlayer.class))
                    && m.name.equals(mappedTick)
                    && m.desc.equals("()V")) {
                InsnList list = new InsnList();
                list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(LocalPlayerTransformer.class),
                        "onEventUpdate",
                        "()V",
                        false
                ));
                node.instructions.insertBefore(m, list);
                inserted = true;
                break;
            }
        }
        if (!inserted && MixinLoader.debugging) {
            System.out.println("[ASM][LocalPlayerTransformer] Failed to insert EventUpdate before AbstractClientPlayer.tick in LocalPlayer.tick");
        }
    }

    @Inject(method = "sendPosition", desc = "()V")
    private void injectSendPosition(MethodNode node) {
        // Overwrite by delegating to static patch and return
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(LocalPlayerTransformer.class),
                "sendPositionPatch",
                "(Lnet/minecraft/client/player/LocalPlayer;)V",
                false
        ));
        list.add(new InsnNode(Opcodes.RETURN));
        node.instructions.clear();
        node.instructions.add(list);
    }

    @Inject(method = "aiStep", desc = "()V")
    private void injectAiStep(MethodNode node) {
        String mappedIsUsing = Mapping.get(LocalPlayer.class, "isUsingItem", "()Z");
        boolean replaced = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.owner.equals(Type.getInternalName(LocalPlayer.class))
                    && m.name.equals(mappedIsUsing)
                    && m.desc.equals("()Z")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(LocalPlayerTransformer.class),
                        "onSlowdown",
                        "(Lnet/minecraft/client/player/LocalPlayer;)Z",
                        false
                );
                node.instructions.set(m, redirect);
                replaced = true;
                break;
            }
        }
        if (!replaced && MixinLoader.debugging) {
            System.out.println("[ASM][LocalPlayerTransformer] Failed to redirect LocalPlayer.isUsingItem in aiStep");
        }
    }
}


