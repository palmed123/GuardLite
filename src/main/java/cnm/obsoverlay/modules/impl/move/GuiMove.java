package cnm.obsoverlay.modules.impl.move;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.impl.EventKey;
import cnm.obsoverlay.events.impl.EventMoveInput;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import cnm.obsoverlay.values.impl.ModeValue;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.HashMap;
import java.util.Map;

@ModuleInfo(
    name = "GuiMove",
    description = "Allows you to walk while a GUI screen is opened",
    category = Category.MOVEMENT
)
public class GuiMove extends Module {

    private final ModeValue behavior = ValueBuilder.create(this, "Behavior")
        .setDefaultModeIndex(0)
        .setModes("Normal", "Undetectable")
        .build()
        .getModeValue();

    private final BooleanValue passthroughSneak = ValueBuilder.create(this, "PassthroughSneak")
        .setDefaultBooleanValue(false)
        .build()
        .getBooleanValue();

    private final BooleanValue passthroughJump = ValueBuilder.create(this, "PassthroughJump")
        .setDefaultBooleanValue(true)
        .build()
        .getBooleanValue();

    private final BooleanValue noSprint = ValueBuilder.create(this, "NoSprint")
        .setDefaultBooleanValue(false)
        .build()
        .getBooleanValue();

    private final Map<KeyMapping, Boolean> movementKeys = new HashMap<>();
    private KeyMapping[] movementKeyArray;

    public GuiMove() {}

    private void initializeMovementKeys() {
        if (mc.options == null) return;
        
        movementKeys.put(mc.options.keyUp, false);
        movementKeys.put(mc.options.keyDown, false);
        movementKeys.put(mc.options.keyLeft, false);
        movementKeys.put(mc.options.keyRight, false);
        movementKeys.put(mc.options.keyJump, false);
        movementKeys.put(mc.options.keyShift, false);
        movementKeys.put(mc.options.keySprint, false);

        movementKeyArray = new KeyMapping[]{
            mc.options.keyUp, mc.options.keyDown, 
            mc.options.keyLeft, mc.options.keyRight,
            mc.options.keyJump, mc.options.keyShift,
            mc.options.keySprint
        };
    }

    @EventTarget
    private void onKey(EventKey event) {
        if (mc.player == null || mc.level == null) return;

        KeyMapping key = getMovementKeyByCode(event.getKey());
        if (key == null) return;

        if (shouldHandleInputs(key)) {
            boolean pressed = event.isState();
            movementKeys.put(key, pressed);

            if (isEnabled()) {
                updateKeyBindingState(key, pressed);
            }
        }
    }

    @EventTarget
    private void onMoveInput(EventMoveInput event) {
        if (mc.player == null) return;

        Screen currentScreen = mc.screen;

        if (currentScreen == null || !shouldHandleMoveInput()) {
            return;
        }

        Boolean upPressed = movementKeys.get(mc.options.keyUp);
        Boolean downPressed = movementKeys.get(mc.options.keyDown);
        Boolean leftPressed = movementKeys.get(mc.options.keyLeft);
        Boolean rightPressed = movementKeys.get(mc.options.keyRight);
        Boolean jumpPressed = movementKeys.get(mc.options.keyJump);
        Boolean sneakPressed = movementKeys.get(mc.options.keyShift);
        Boolean sprintPressed = movementKeys.get(mc.options.keySprint);

        if (Boolean.TRUE.equals(upPressed)) {
            event.setForward(1.0f);
        } else if (Boolean.TRUE.equals(downPressed)) {
            event.setForward(-1.0f);
        }

        if (Boolean.TRUE.equals(leftPressed)) {
            event.setStrafe(1.0f);
        } else if (Boolean.TRUE.equals(rightPressed)) {
            event.setStrafe(-1.0f);
        }

        if (shouldHandleJumpInput()) {
            event.setJump(Boolean.TRUE.equals(jumpPressed));
        }

        if (shouldHandleSneakInput()) {
            event.setSneak(Boolean.TRUE.equals(sneakPressed));
        }

        if (noSprint.getCurrentValue() && currentScreen != null) {
            event.setSprinting(false);
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }
        } else if (Boolean.TRUE.equals(sprintPressed) && currentScreen != null) {
            event.setSprinting(true);
        }
    }

    private boolean shouldHandleMoveInput() {
        Screen screen = mc.screen;
        if (screen == null) {
            return false;
        }

        if (screen instanceof ChatScreen) {
            return false;
        }

        if (isInCreativeSearchField(screen)) {
            return false;
        }

        String mode = behavior.getCurrentMode();
        switch (mode) {
            case "Normal":
                return true;
            case "Undetectable":
                return !(screen instanceof AbstractContainerScreen);
            default:
                return false;
        }
    }

    private boolean shouldHandleInputs(KeyMapping keyBinding) {
        Screen screen = mc.screen;
        if (screen == null) {
            return true;
        }

        if (screen instanceof ChatScreen) {
            return false;
        }

        if (isInCreativeSearchField(screen)) {
            return false;
        }

        if (keyBinding == mc.options.keyShift && !passthroughSneak.getCurrentValue()) {
            return false;
        }

        if (keyBinding == mc.options.keyJump && !passthroughJump.getCurrentValue()) {
            return false;
        }

        String mode = behavior.getCurrentMode();
        switch (mode) {
            case "Normal":
                return true;
            case "Undetectable":
                return !(screen instanceof AbstractContainerScreen);
            default:
                return false;
        }
    }

    private boolean shouldHandleSneakInput() {
        return passthroughSneak.getCurrentValue();
    }

    private boolean shouldHandleJumpInput() {
        return passthroughJump.getCurrentValue();
    }

    private KeyMapping getMovementKeyByCode(int keyCode) {
        if (movementKeyArray == null) return null;

        for (KeyMapping key : movementKeyArray) {
            if (key != null && key.matches(keyCode, 0)) {
                return key;
            }
        }
        return null;
    }

    private boolean isInCreativeSearchField(Screen screen) {
        if (!(screen instanceof CreativeModeInventoryScreen)) {
            return false;
        }

        try {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateKeyBindingState(KeyMapping key, boolean pressed) {
        if (key != null) {
            key.setDown(pressed);
        }
    }

    @Override
    public void onEnable() {
        initializeMovementKeys();

        Map<KeyMapping, Boolean> keysCopy = new HashMap<>(movementKeys);
        for (KeyMapping key : keysCopy.keySet()) {
            movementKeys.put(key, false);
        }

        if (noSprint.getCurrentValue() && mc.screen != null && mc.player != null) {
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }
        }
    }

    @Override
    public void onDisable() {
        Map<KeyMapping, Boolean> keysCopy = new HashMap<>(movementKeys);
        for (KeyMapping key : keysCopy.keySet()) {
            movementKeys.put(key, false);

            if (key != null) {
                key.setDown(false);
            }
        }
    }
    
    @Override
    public String getDescription() {
        return "Allows you to walk while a GUI screen is opened with enhanced features";
    }
}
