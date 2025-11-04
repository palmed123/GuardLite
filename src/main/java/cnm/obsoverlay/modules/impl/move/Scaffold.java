package cnm.obsoverlay.modules.impl.move;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.*;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.utils.*;
import cnm.obsoverlay.utils.math.MathHelper;
import cnm.obsoverlay.utils.rotation.RotationManager;
import cnm.obsoverlay.utils.rotation.RotationUtils;
import cnm.obsoverlay.utils.vector.Vector2f;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import cnm.obsoverlay.values.impl.FloatValue;
import cnm.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.RandomUtils;

import java.util.*;

@ModuleInfo(
        name = "Scaffold",
        description = "Automatically places blocks under you",
        category = Category.MOVEMENT
)
public class Scaffold extends Module {
    public static final List<Block> blacklistedBlocks = Arrays.asList(
            Blocks.AIR,
            Blocks.WATER,
            Blocks.LAVA,
            Blocks.ENCHANTING_TABLE,
            Blocks.GLASS_PANE,
            Blocks.GLASS_PANE,
            Blocks.IRON_BARS,
            Blocks.SNOW,
            Blocks.COAL_ORE,
            Blocks.DIAMOND_ORE,
            Blocks.EMERALD_ORE,
            Blocks.CHEST,
            Blocks.TRAPPED_CHEST,
            Blocks.TORCH,
            Blocks.ANVIL,
            Blocks.TRAPPED_CHEST,
            Blocks.NOTE_BLOCK,
            Blocks.JUKEBOX,
            Blocks.TNT,
            Blocks.GOLD_ORE,
            Blocks.IRON_ORE,
            Blocks.LAPIS_ORE,
            Blocks.STONE_PRESSURE_PLATE,
            Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
            Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Blocks.STONE_BUTTON,
            Blocks.LEVER,
            Blocks.TALL_GRASS,
            Blocks.TRIPWIRE,
            Blocks.TRIPWIRE_HOOK,
            Blocks.RAIL,
            Blocks.CORNFLOWER,
            Blocks.RED_MUSHROOM,
            Blocks.BROWN_MUSHROOM,
            Blocks.VINE,
            Blocks.SUNFLOWER,
            Blocks.LADDER,
            Blocks.FURNACE,
            Blocks.SAND,
            Blocks.CACTUS,
            Blocks.DISPENSER,
            Blocks.DROPPER,
            Blocks.CRAFTING_TABLE,
            Blocks.COBWEB,
            Blocks.PUMPKIN,
            Blocks.COBBLESTONE_WALL,
            Blocks.OAK_FENCE,
            Blocks.REDSTONE_TORCH,
            Blocks.FLOWER_POT
    );
    public Vector2f correctRotation = new Vector2f();
    public Vector2f rots = new Vector2f();
    public Vector2f lastRots = new Vector2f();
    public ModeValue mode = ValueBuilder.create(this, "Mode").setDefaultModeIndex(0).setModes("Normal", "Telly Bridge", "Keep Y").build().getModeValue();
    public BooleanValue eagle = ValueBuilder.create(this, "Eagle")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.mode.isCurrentMode("Normal"))
            .build()
            .getBooleanValue();
    public BooleanValue sneak = ValueBuilder.create(this, "Sneak").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue advancedBlockSearch = ValueBuilder.create(this, "Advanced Block Search").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue snap = ValueBuilder.create(this, "Snap")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.mode.isCurrentMode("Normal"))
            .build()
            .getBooleanValue();
    public BooleanValue hideSnap = ValueBuilder.create(this, "Hide Snap Rotation")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.mode.isCurrentMode("Normal") && this.snap.getCurrentValue())
            .build()
            .getBooleanValue();
    public BooleanValue renderItemSpoof = ValueBuilder.create(this, "Render Item Spoof").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue jumpSprint = ValueBuilder.create(this, "Jump Sprint")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.mode.isCurrentMode("Normal"))
            .build()
            .getBooleanValue();
    FloatValue rotSpeed = ValueBuilder.create(this, "Rotation Speed")
            .setDefaultFloatValue(1.5F)
            .setMaxFloatValue(10.0F)
            .setMinFloatValue(0.0F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();
    int oldSlot;
    private BlockPos pos;
    private int lastSneakTicks;
    private int airTicks = 0;

    public static boolean isValidStack(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof BlockItem) || stack.getCount() <= 1) {
            return false;
        } else if (!InventoryUtils.isItemValid(stack)) {
            return false;
        } else {
            String string = stack.getDisplayName().getString();
            if (string.contains("Click") || string.contains("点击")) {
                return false;
            } else if (stack.getItem() instanceof ItemNameBlockItem) {
                return false;
            } else {
                Block block = ((BlockItem) stack.getItem()).getBlock();
                if (block instanceof FlowerBlock) {
                    return false;
                } else if (block instanceof BushBlock) {
                    return false;
                } else if (block instanceof FungusBlock) {
                    return false;
                } else if (block instanceof CropBlock) {
                    return false;
                } else {
                    return !(block instanceof SlabBlock) && !blacklistedBlocks.contains(block);
                }
            }
        }
    }

    private static Vec3 getVec3(BlockPos checkPosition, BlockState block) {
        VoxelShape shape = block.getShape(mc.level, checkPosition);
        double ex = MathHelper.clamp(mc.player.getX(), checkPosition.getX(), checkPosition.getX() + shape.max(Direction.Axis.X));
        double ey = MathHelper.clamp(mc.player.getY(), checkPosition.getY(), checkPosition.getY() + shape.max(Direction.Axis.Y));
        double ez = MathHelper.clamp(mc.player.getZ(), checkPosition.getZ(), checkPosition.getZ() + shape.max(Direction.Axis.Z));
        return new Vec3(ex, ey, ez);
    }

    public static boolean isOnBlockEdge(float sensitivity) {
        return !mc.level
                .getCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate(-sensitivity, 0.0, -sensitivity))
                .iterator()
                .hasNext();
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            this.oldSlot = mc.player.getInventory().selected;
            this.rots.set(mc.player.getYRot() - 180.0F, mc.player.getXRot());
            this.lastRots.set(mc.player.yRotO - 180.0F, mc.player.xRotO);
            this.pos = null;
        }
    }

    @Override
    public void onDisable() {
        boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
        boolean isHoldingShift = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getKey().getValue());
        mc.options.keyJump.setDown(isHoldingJump);
        mc.options.keyShift.setDown(isHoldingShift);
        mc.options.keyUse.setDown(false);
        mc.player.getInventory().selected = this.oldSlot;
    }

    @EventTarget
    public void onUpdateHeldItem(EventUpdateHeldItem e) {
        if (this.renderItemSpoof.getCurrentValue() && e.getHand() == InteractionHand.MAIN_HAND) {
            e.setItem(mc.player.getInventory().getItem(this.oldSlot));
        }
    }

    @EventTarget(1)
    public void onEventEarlyTick(EventRunTicks e) {
        if (e.getType() == EventType.PRE && mc.screen == null && mc.player != null) {
            int slotID = -1;

            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.getItem() instanceof BlockItem && isValidStack(stack)) {
                    slotID = i;
                    break;
                }
            }

            if (slotID != -1 && mc.player.getInventory().selected != slotID) {
                mc.player.getInventory().selected = slotID;
            }

            this.pos = this.getBlockPos();
            if (this.pos != null) {
                this.correctRotation = this.getPlayerYawRotation();

                
                RotationManager.setRotations(this.correctRotation, rotSpeed.getCurrentValue());
                this.rots = RotationManager.rotations;
            }

            boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
            if (this.sneak.getCurrentValue()) {
                this.lastSneakTicks++;
                System.out.println(this.lastSneakTicks);
                if (this.lastSneakTicks == 18) {
                    if (mc.player.isSprinting()) {
                        mc.options.keySprint.setDown(false);
                        mc.player.setSprinting(false);
                    }

                    mc.options.keyShift.setDown(true);
                } else if (this.lastSneakTicks >= 21) {
                    mc.options.keyShift.setDown(false);
                    this.lastSneakTicks = 0;
                }
            }

            if (this.mode.isCurrentMode("Telly Bridge")) {
                mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
                if (mc.player.onGround() && PlayerUtils.movementInput()) {
                    this.rots.setX(RotationUtils.rotateToYaw(180, this.rots.getX(), mc.player.getYRot()));
                    this.lastRots.set(this.rots.getX(), this.rots.getY());
                    return;
                }
            } else if (this.mode.isCurrentMode("Keep Y")) {
                mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
            } else {
                if (this.eagle.getCurrentValue()) {
                    mc.options.keyShift.setDown(mc.player.onGround() && isOnBlockEdge(0.3F));
                }

                if (this.snap.getCurrentValue() && !isHoldingJump) {
                    this.doSnap();
                }
                if (mc.player.onGround() && PlayerUtils.movementInput() && isHoldingJump && jumpSprint.currentValue && !mc.options.keyShift.isDown()) {
                    this.rots.setX(RotationUtils.rotateToYaw(180.0F, this.rots.getX(), mc.player.getYRot()));
                    this.lastRots.set(this.rots.getX(), this.rots.getY());
                    return;
                }
            }

            this.lastRots.set(this.rots.getX(), this.rots.getY());
        }
    }

    private void doSnap() {
        boolean shouldPlaceBlock = false;
        HitResult objectPosition = RayTraceUtils.rayCast(1.0F, this.rots);
        if (objectPosition.getType() == Type.BLOCK) {
            BlockHitResult position = (BlockHitResult) objectPosition;
            if (position.getBlockPos().equals(this.pos) && position.getDirection() != Direction.UP) {
                shouldPlaceBlock = true;
            }
        }

        if (!shouldPlaceBlock) {
            this.rots.setX(mc.player.getYRot() + RandomUtils.nextFloat(0.0F, 0.5F) - 0.25F);
        }
    }

    @EventTarget
    public void onClick(EventClick e) {
        e.setCancelled(true);
        if (mc.screen == null && mc.player != null) {
            this.placeBlock();
        }
    }

    private void placeBlock() {
        if (this.pos != null && isValidStack(mc.player.getMainHandItem())) {
            HitResult objectPosition = RayTraceUtils.rayCast(1.0F, RotationManager.rotations);
            if (objectPosition.getType() == Type.BLOCK) {
                BlockHitResult position = (BlockHitResult) objectPosition;
                boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
                if (position.getBlockPos().equals(this.pos)
                        && (
                        position.getDirection() != Direction.UP
                                || mc.player.onGround()
                                || !PlayerUtils.movementInput()
                                || isHoldingJump
                                || this.mode.isCurrentMode("Normal")
                )) {
                    if (mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, position) == InteractionResult.SUCCESS) {
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                }
            }
        }
    }

    private Vector2f getPlayerYawRotation() {
        float rotationYaw = mc.player.getYRot() - 180.0F;
        if (this.isTower()) {
            HitResult objectPosition = mc.hitResult;
            if (objectPosition != null) {
                float pitch = 90.0F;
                return new Vector2f(rotationYaw, pitch);
            }
        }

        float pitch = 82.0F;
        Vector2f rotations = new Vector2f(rotationYaw, pitch);
        float realYaw = mc.player.getYRot();
        float magic = RandomUtils.nextFloat(0.0F, 0.5F) - 0.25F;
        if (this.advancedBlockSearch.getCurrentValue()) {
            if (mc.options.keyDown.isDown()) {
                realYaw += 180.0F;
                if (mc.options.keyLeft.isDown()) {
                    realYaw += 45.0F;
                } else if (mc.options.keyRight.isDown()) {
                    realYaw -= 45.0F;
                }
            } else if (mc.options.keyUp.isDown()) {
                if (mc.options.keyLeft.isDown()) {
                    realYaw -= 45.0F;
                } else if (mc.options.keyRight.isDown()) {
                    realYaw += 45.0F;
                }
            } else if (mc.options.keyRight.isDown()) {
                realYaw += 90.0F;
            } else if (mc.options.keyLeft.isDown()) {
                realYaw -= 90.0F;
            }
        }

        float yaw = realYaw - 180.0F + magic;
        rotations.setX(yaw);
        if (this.shouldBuild()) {
            HitResult initialHit = this.performRayCast(rotations);
            if (this.isHitValid(initialHit)) {
                return rotations;
            }

            ArrayList<Float> validPitches = this.findValidPitches(yaw);
            if (!validPitches.isEmpty()) {
                validPitches.sort(Comparator.comparingDouble(this::distanceToLastPitch));
                rotations.setY(validPitches.get(0));
                return rotations;
            }

            if (this.advancedBlockSearch.getCurrentValue()) {
                Vector2f optimalRotation = this.findOptimalRotation(yaw);
                if (optimalRotation != null) {
                    return optimalRotation;
                }
            }
        }

        return rotations;
    }

    private boolean shouldBuild() {
        BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5, mc.player.getZ());
        return mc.level.isEmptyBlock(playerPos) && isValidStack(mc.player.getMainHandItem());
    }

    private double distanceToLastPitch(float pitch) {
        return Math.abs(pitch - this.rots.y);
    }

    private ArrayList<Float> findValidPitches(float yaw) {
        ArrayList<Float> validPitches = new ArrayList<>();

        for (float i = Math.max(this.rots.y - 30.0F, -90.0F); i < Math.min(this.rots.y + 20.0F, 90.0F); i += 0.3F) {
            Vector2f fixed = RotationUtils.getFixedRotation(yaw, i, this.rots.x, this.rots.y);
            HitResult position = this.performRayCast(new Vector2f(yaw, fixed.y));
            if (this.isHitValid(position)) {
                validPitches.add(fixed.y);
            }
        }

        return validPitches;
    }

    private HitResult performRayCast(Vector2f rotations) {
        return RayTraceUtils.rayCast(1.0F, rotations);
    }

    private boolean isHitValid(HitResult hit) {
        Type block = Type.BLOCK;
        if (hit.getType() != block) {
            return false;
        } else {
            BlockHitResult blockHit = (BlockHitResult) hit;
            return this.isValidBlock(blockHit.getBlockPos())
                    && this.isNearbyBlockPos(blockHit.getBlockPos())
                    && blockHit.getDirection() != Direction.DOWN
                    && blockHit.getDirection() != Direction.UP;
        }
    }

    private Vector2f findOptimalRotation(float yaw) {
        for (float yawLoops = 0.0F; yawLoops < 180.0F; yawLoops++) {
            float currentPitch = this.rots.y;

            for (float pitchLoops = 0.0F; pitchLoops < 25.0F; pitchLoops++) {
                for (int i = 0; i < 2; i++) {
                    float pitch = currentPitch - pitchLoops * (i == 0 ? 1 : -1);
                    float[][] offsets = new float[][]{{yaw + yawLoops, pitch}, {yaw - yawLoops, pitch}};

                    for (float[] rotation : offsets) {
                        float rayCastPitch = MathHelper.clamp(rotation[1], -90.0F, 90.0F);
                        Vector2f fixedRotation = RotationUtils.getFixedRotation(rotation[0], rayCastPitch, this.rots.x, this.rots.y);
                        HitResult position = this.performRayCast(fixedRotation);
                        if (this.isHitValid(position)) {
                            return fixedRotation;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isNearbyBlockPos(BlockPos blockPos) {
        if (!mc.player.onGround()) {
            return blockPos.equals(this.pos);
        } else {
            for (int x = this.pos.getX() - 1; x <= this.pos.getX() + 1; x++) {
                for (int z = this.pos.getZ() - 1; z <= this.pos.getZ() + 1; z++) {
                    if (blockPos.equals(new BlockPos(x, this.pos.getY(), z))) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private BlockPos getBlockPos() {
        BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ());
        ArrayList<Vec3> positions = new ArrayList<>();
        HashMap<Vec3, BlockPos> hashMap = new HashMap<>();

        for (int x = playerPos.getX() - 5; x <= playerPos.getX() + 5; x++) {
            for (int y = playerPos.getY() - 1; y <= playerPos.getY(); y++) {
                for (int z = playerPos.getZ() - 5; z <= playerPos.getZ() + 5; z++) {
                    BlockPos checkPosition = new BlockPos(x, y, z);
                    if (this.isValidBlock(checkPosition)) {
                        BlockState block = mc.level.getBlockState(checkPosition);
                        Vec3 vec3 = getVec3(checkPosition, block);
                        positions.add(vec3);
                        hashMap.put(vec3, checkPosition);
                    }
                }
            }
        }

        if (!positions.isEmpty()) {
            positions.sort(Comparator.comparingDouble(this::getBlockDistance));
            return this.isTower() && hashMap.get(positions.get(0)).getY() != mc.player.getY() - 1.5
                    ? BlockPos.containing(mc.player.getX(), mc.player.getY() - 1.5, mc.player.getZ())
                    : hashMap.get(positions.get(0));
        } else {
            return null;
        }
    }

    public boolean isValidBlock(BlockPos blockPos) {
        Block block = mc.level.getBlockState(blockPos).getBlock();
        return !(block instanceof LiquidBlock)
                && !(block instanceof AirBlock)
                && !(block instanceof ChestBlock)
                && !(block instanceof FurnaceBlock)
                && !(block instanceof EnderChestBlock)
                && !(block instanceof TallGrassBlock)
                && !(block instanceof SnowLayerBlock)
                && !(block instanceof EnchantmentTableBlock)
                && !(block instanceof AnvilBlock)
                && !(block instanceof CraftingTableBlock);
    }

    private boolean isTower() {
        boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
        return isHoldingJump
                && !mc.options.keyUp.isDown()
                && !mc.options.keyDown.isDown()
                && !mc.options.keyLeft.isDown()
                && !mc.options.keyRight.isDown();
    }

    private double getBlockDistance(Vec3 vec3) {
        return mc.player.distanceToSqr(vec3.x, vec3.y, vec3.z);
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        this.setSuffix(mode.getCurrentMode());
        if (mc.player.onGround()) {
            airTicks = 0;
        } else {
            airTicks++;
        }
    }

    public record BlockPosWithFacing(BlockPos position, Direction facing) {
    }
}
