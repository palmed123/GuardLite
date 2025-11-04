package cnm.obsoverlay.modules.impl.misc;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventMotion;
import cnm.obsoverlay.events.impl.EventPacket;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.modules.impl.move.Scaffold;
import cnm.obsoverlay.ui.ClickGUI;
import cnm.obsoverlay.ui.notification.Notification;
import cnm.obsoverlay.ui.notification.NotificationLevel;
import cnm.obsoverlay.utils.InventoryUtils;
import cnm.obsoverlay.utils.math.MathUtils;
import cnm.obsoverlay.utils.MoveUtils;
import cnm.obsoverlay.utils.TickTimeHelper;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import cnm.obsoverlay.values.impl.FloatValue;
import cnm.obsoverlay.values.impl.ModeValue;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ModuleInfo(
        name = "InvManager",
        description = "Automatically manage your inventory",
        category = Category.MISC
)
public class InventoryCleaner extends Module {
    private static final TickTimeHelper timer = new TickTimeHelper();
    private static final TickTimeHelper startTimer = new TickTimeHelper();
    private final FloatValue startDelay = ValueBuilder.create(this, "Start Delay")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    private final FloatValue minDelay = ValueBuilder.create(this, "Min Delay")
            .setDefaultFloatValue(2.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    private final FloatValue maxDelay = ValueBuilder.create(this, "Max Delay")
            .setDefaultFloatValue(6.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    ModeValue offhandItems = ValueBuilder.create(this, "Offhand Items")
            .setModes("None", "Golden Apple", "Projectile", "Fishing Rod", "Block")
            .build()
            .getModeValue();
    BooleanValue autoArmor = ValueBuilder.create(this, "Auto Armor").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue inventoryOnly = ValueBuilder.create(this, "Inventory Only").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue switchSword = ValueBuilder.create(this, "Switch Sword").setDefaultBooleanValue(true).build().getBooleanValue();
    FloatValue swordSlot = ValueBuilder.create(this, "Sword Slot")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(9.0F)
            .setVisibility(() -> this.switchSword.getCurrentValue())
            .build()
            .getFloatValue();
    BooleanValue switchBlock = ValueBuilder.create(this, "Switch Block")
            .setVisibility(() -> !this.offhandItems.isCurrentMode("Block"))
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    FloatValue blockSlot = ValueBuilder.create(this, "Block Slot")
            .setDefaultFloatValue(2.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(9.0F)
            .setVisibility(() -> this.switchBlock.getCurrentValue() && !this.offhandItems.isCurrentMode("Block"))
            .build()
            .getFloatValue();
    FloatValue maxBlockSize = ValueBuilder.create(this, "Max Block Size")
            .setDefaultFloatValue(256.0F)
            .setFloatStep(64.0F)
            .setMinFloatValue(64.0F)
            .setMaxFloatValue(512.0F)
            .setVisibility(() -> this.switchBlock.getCurrentValue())
            .build()
            .getFloatValue();
    BooleanValue switchPickaxe = ValueBuilder.create(this, "Switch Pickaxe").setDefaultBooleanValue(true).build().getBooleanValue();
    FloatValue pickaxeSlot = ValueBuilder.create(this, "Pickaxe Slot")
            .setDefaultFloatValue(3.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(9.0F)
            .setVisibility(() -> this.switchPickaxe.getCurrentValue())
            .build()
            .getFloatValue();
    BooleanValue switchAxe = ValueBuilder.create(this, "Switch Axe").setDefaultBooleanValue(true).build().getBooleanValue();
    FloatValue axeSlot = ValueBuilder.create(this, "Axe Slot")
            .setDefaultFloatValue(4.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(9.0F)
            .setVisibility(() -> this.switchAxe.getCurrentValue())
            .build()
            .getFloatValue();
    BooleanValue switchBow = ValueBuilder.create(this, "Switch Bow or Crossbow").setDefaultBooleanValue(true).build().getBooleanValue();
    FloatValue bowSlot = ValueBuilder.create(this, "Bow Slot")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(9.0F)
            .setVisibility(() -> this.switchBow.getCurrentValue())
            .build()
            .getFloatValue();
    ModeValue preferBow = ValueBuilder.create(this, "Bow Priority")
            .setModes("Crossbow", "Power Bow", "Punch Bow")
            .setVisibility(() -> this.switchBow.getCurrentValue())
            .build()
            .getModeValue();
    FloatValue maxArrowSize = ValueBuilder.create(this, "Max Arrow Size")
            .setDefaultFloatValue(256.0F)
            .setFloatStep(64.0F)
            .setMinFloatValue(64.0F)
            .setMaxFloatValue(512.0F)
            .setVisibility(() -> this.switchBow.getCurrentValue())
            .build()
            .getFloatValue();
    BooleanValue switchWaterBucket = ValueBuilder.create(this, "Switch Water Bucket").setDefaultBooleanValue(true).build().getBooleanValue();
    FloatValue waterBucketSlot = ValueBuilder.create(this, "Water Bucket Slot")
            .setDefaultFloatValue(6.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(9.0F)
            .setVisibility(() -> this.switchWaterBucket.getCurrentValue())
            .build()
            .getFloatValue();
    BooleanValue switchEnderPearl = ValueBuilder.create(this, "Switch Ender Pearl").setDefaultBooleanValue(true).build().getBooleanValue();
    FloatValue enderPearlSlot = ValueBuilder.create(this, "Ender Pearl Slot")
            .setDefaultFloatValue(7.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(9.0F)
            .setVisibility(() -> this.switchEnderPearl.getCurrentValue())
            .build()
            .getFloatValue();
    BooleanValue switchFireball = ValueBuilder.create(this, "Switch Fireball").setDefaultBooleanValue(true).build().getBooleanValue();
    FloatValue fireballSlot = ValueBuilder.create(this, "Fireball Slot")
            .setDefaultFloatValue(8.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(9.0F)
            .setVisibility(() -> this.switchFireball.getCurrentValue())
            .build()
            .getFloatValue();
    BooleanValue switchGoldenApple = ValueBuilder.create(this, "Switch Golden Apple")
            .setVisibility(() -> !this.offhandItems.isCurrentMode("Golden Apple"))
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    FloatValue goldenAppleSlot = ValueBuilder.create(this, "Golden Apple Slot")
            .setDefaultFloatValue(9.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(9.0F)
            .setVisibility(() -> this.switchGoldenApple.getCurrentValue() && !this.offhandItems.isCurrentMode("Golden Apple"))
            .build()
            .getFloatValue();
    BooleanValue throwItems = ValueBuilder.create(this, "Throw Items").setDefaultBooleanValue(true).build().getBooleanValue();
    FloatValue waterBucketCount = ValueBuilder.create(this, "Keep Water Buckets")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(5.0F)
            .setVisibility(() -> this.throwItems.getCurrentValue())
            .build()
            .getFloatValue();
    FloatValue lavaBucketCount = ValueBuilder.create(this, "Keep Lava Buckets")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(5.0F)
            .setVisibility(() -> this.throwItems.getCurrentValue())
            .build()
            .getFloatValue();
    BooleanValue keepProjectile = ValueBuilder.create(this, "Keep Eggs & Snowballs").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue switchProjectile = ValueBuilder.create(this, "Switch Eggs & Snowballs")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> this.keepProjectile.getCurrentValue() && !this.offhandItems.isCurrentMode("Projectile"))
            .build()
            .getBooleanValue();
    FloatValue projectileSlot = ValueBuilder.create(this, "Eggs & Snowballs Slot")
            .setDefaultFloatValue(9.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(9.0F)
            .setVisibility(() -> this.switchProjectile.getCurrentValue() && this.keepProjectile.getCurrentValue() && !this.offhandItems.isCurrentMode("Projectile"))
            .build()
            .getFloatValue();
    FloatValue maxProjectileSize = ValueBuilder.create(this, "Max Eggs & Snowballs Size")
            .setDefaultFloatValue(64.0F)
            .setFloatStep(16.0F)
            .setMinFloatValue(16.0F)
            .setMaxFloatValue(256.0F)
            .setVisibility(() -> this.keepProjectile.getCurrentValue())
            .build()
            .getFloatValue();
    BooleanValue switchRod = ValueBuilder.create(this, "Switch Rod")
            .setVisibility(() -> !this.offhandItems.isCurrentMode("Fishing Rod"))
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    FloatValue rodSlot = ValueBuilder.create(this, "Rod Slot")
            .setDefaultFloatValue(9.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(9.0F)
            .setVisibility(() -> this.switchRod.getCurrentValue() && !this.offhandItems.isCurrentMode("Fishing Rod"))
            .build()
            .getFloatValue();
    int noMoveTicks = 0;
    private boolean clickOffHand = false;
    private boolean inventoryOpen = false;
    private boolean inventoryScreenDelayActive = false;
    private boolean wasInventoryScreen = false;

    public static int getMaxBlockSize() {
        return (int) ((InventoryCleaner) Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).maxBlockSize.getCurrentValue();
    }

    public static boolean shouldKeepProjectile() {
        return ((InventoryCleaner) Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).keepProjectile.getCurrentValue();
    }

    public static int getMaxProjectileSize() {
        return (int) ((InventoryCleaner) Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).maxProjectileSize.getCurrentValue();
    }

    public static int getMaxArrowSize() {
        return (int) ((InventoryCleaner) Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).maxArrowSize.getCurrentValue();
    }

    public static int getWaterBucketCount() {
        return (int) ((InventoryCleaner) Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).waterBucketCount.getCurrentValue();
    }

    public static int getLavaBucketCount() {
        return (int) ((InventoryCleaner) Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).lavaBucketCount.getCurrentValue();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        startTimer.reset();
    }

    public boolean isItemUseful(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else if (InventoryUtils.isGodItem(stack)) {
            return true;
        } else if (stack.getDisplayName().getString().contains("点击使用")) {
            return true;
        } else if (stack.getItem() instanceof ArmorItem item) {
            float protection = InventoryUtils.getProtection(stack);
            if (InventoryUtils.getCurrentArmorScore(item.getEquipmentSlot()) >= protection) {
                return false;
            } else {
                float bestArmor = InventoryUtils.getBestArmorScore(item.getEquipmentSlot());
                return !(protection < bestArmor);
            }
        } else if (stack.getItem() instanceof SwordItem) {
            return InventoryUtils.getBestSword() == stack;
        } else if (stack.getItem() instanceof PickaxeItem) {
            return InventoryUtils.getBestPickaxe() == stack;
        } else if (stack.getItem() instanceof AxeItem && !InventoryUtils.isSharpnessAxe(stack)) {
            return InventoryUtils.getBestAxe() == stack;
        } else if (stack.getItem() instanceof ShovelItem) {
            return InventoryUtils.getBestShovel() == stack;
        } else if (stack.getItem() instanceof CrossbowItem) {
            return InventoryUtils.getBestCrossbow() == stack;
        } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPunchBow(stack)) {
            return InventoryUtils.getBestPunchBow() == stack;
        } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPowerBow(stack)) {
            return InventoryUtils.getBestPowerBow() == stack;
        } else if (stack.getItem() instanceof BowItem && InventoryUtils.getItemCount(Items.BOW) > 1) {
            return false;
        } else if (stack.getItem() == Items.WATER_BUCKET && InventoryUtils.getItemCount(Items.WATER_BUCKET) > getWaterBucketCount()) {
            return false;
        } else if (stack.getItem() == Items.LAVA_BUCKET && InventoryUtils.getItemCount(Items.LAVA_BUCKET) > getLavaBucketCount()) {
            return false;
        } else if (stack.getItem() instanceof FishingRodItem && InventoryUtils.getItemCount(Items.FISHING_ROD) > 1) {
            return false;
        } else if ((stack.getItem() == Items.SNOWBALL || stack.getItem() == Items.EGG) && !shouldKeepProjectile()) {
            return false;
        } else {
            return !(stack.getItem() instanceof ItemNameBlockItem) && InventoryUtils.isCommonItemUseful(stack);
        }
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (e.getType() == EventType.SEND) {
            if (e.getPacket() instanceof ServerboundContainerClosePacket) {
                this.inventoryOpen = false;
            }

            if (this.inventoryOpen && !this.inventoryOnly.getCurrentValue()) {
                if (e.getPacket() instanceof ServerboundMovePlayerPacket) {
                    if (MoveUtils.isMoving()) {
                        mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
                    }
                } else if (e.getPacket() instanceof ServerboundUseItemOnPacket
                        || e.getPacket() instanceof ServerboundUseItemPacket
                        || e.getPacket() instanceof ServerboundInteractPacket
                        || e.getPacket() instanceof ServerboundPlayerActionPacket) {
                    mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
                }
            }
        }
    }

    private boolean checkConfig() {
        List<Pair<BooleanValue, FloatValue>> pairs = new ArrayList<>();
        if (!this.keepProjectile.getCurrentValue()) {
            this.switchProjectile.setCurrentValue(false);
        }

        pairs.add(Pair.of(this.switchSword, this.swordSlot));
        pairs.add(Pair.of(this.switchPickaxe, this.pickaxeSlot));
        pairs.add(Pair.of(this.switchAxe, this.axeSlot));
        pairs.add(Pair.of(this.switchBow, this.bowSlot));
        pairs.add(Pair.of(this.switchWaterBucket, this.waterBucketSlot));
        pairs.add(Pair.of(this.switchEnderPearl, this.enderPearlSlot));
        pairs.add(Pair.of(this.switchFireball, this.fireballSlot));
        if (!this.offhandItems.isCurrentMode("Golden Apple")) {
            pairs.add(Pair.of(this.switchGoldenApple, this.goldenAppleSlot));
        }

        if (!this.offhandItems.isCurrentMode("Projectile")) {
            pairs.add(Pair.of(this.switchProjectile, this.projectileSlot));
        }

        if (!this.offhandItems.isCurrentMode("Fishing Rod")) {
            pairs.add(Pair.of(this.switchRod, this.rodSlot));
        }

        if (!this.offhandItems.isCurrentMode("Block")) {
            pairs.add(Pair.of(this.switchBlock, this.blockSlot));
        }

        Set<Integer> usedSlot = new HashSet<>();

        for (Pair<BooleanValue, FloatValue> pair : pairs) {
            if (pair.getKey().getCurrentValue()) {
                int targetSlot = (int) (pair.getValue().getCurrentValue() - 1.0F);
                if (usedSlot.contains(targetSlot)) {
                    return false;
                }

                usedSlot.add(targetSlot);
            }
        }

        return true;
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            if (!offhandItems.getCurrentMode().equals("None")) this.setSuffix(offhandItems.getCurrentMode());
            if (!(mc.screen instanceof ClickGUI) && !this.checkConfig()) {
                Notification notification = new Notification(
                        NotificationLevel.ERROR, "Duplicate slot config in Inventory Manager! Please check your config!", 8000L
                );
                Naven.getInstance().getNotificationManager().addNotification(notification);
                this.toggle();
                return;
            }

            if (InventoryUtils.shouldDisableFeatures()) {
                return;
            }

            if (MoveUtils.isMoving()) {
                this.noMoveTicks = 0;
            } else {
                this.noMoveTicks++;
            }

            if (ChestStealer.isWorking()
                    || Naven.getInstance().getModuleManager().getModule(Scaffold.class).isEnabled()
                    || (this.inventoryOnly.getCurrentValue() ? !(mc.screen instanceof InventoryScreen) : this.noMoveTicks <= 1)) {
                this.clickOffHand = false;
                return;
            }

            if (mc.screen instanceof AbstractContainerScreen container && container.getMenu().containerId != mc.player.inventoryMenu.containerId) {
                return;
            }

            boolean isInventoryScreen = mc.screen instanceof InventoryScreen;
            if (isInventoryScreen && !this.wasInventoryScreen) {
                startTimer.reset();
                this.inventoryScreenDelayActive = true;
            }
            this.wasInventoryScreen = isInventoryScreen;
            if (isInventoryScreen && this.inventoryScreenDelayActive && !startTimer.delay(this.startDelay.getCurrentValue())) {
                return;
            }
            if (!isInventoryScreen) {
                this.inventoryScreenDelayActive = false;
            }

            if (this.autoArmor.getCurrentValue()) {
                for (int i = 0; i < mc.player.getInventory().armor.size(); i++) {
                    ItemStack stack = mc.player.getInventory().armor.get(i);
                    if (stack.getItem() instanceof ArmorItem item) {
                        if (!stack.isEmpty()
                                && timer.delay(getDelay())
                                && InventoryUtils.getBestArmorScore(item.getEquipmentSlot()) > InventoryUtils.getProtection(stack)) {
                            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, 4 + (4 - i), 1, ClickType.THROW, mc.player);
                            this.inventoryOpen = true;
                            timer.reset();
                        }
                    }
                }

                for (int ix = 0; ix < mc.player.getInventory().items.size(); ix++) {
                    ItemStack stack = mc.player.getInventory().items.get(ix);
                    if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem item) {
                        float currentItemScore = InventoryUtils.getProtection(stack);
                        boolean isBestItem = InventoryUtils.getBestArmorScore(item.getEquipmentSlot()) == currentItemScore;
                        boolean isBetterItem = InventoryUtils.getCurrentArmorScore(item.getEquipmentSlot()) < currentItemScore;
                        if (isBestItem && isBetterItem && timer.delay(getDelay())) {
                            if (ix < 9) {
                                mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, ix + 36, 0, ClickType.QUICK_MOVE, mc.player);
                            } else {
                                mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, ix, 0, ClickType.QUICK_MOVE, mc.player);
                            }

                            this.inventoryOpen = true;
                            timer.reset();
                        }
                    }
                }
            }

            if (this.clickOffHand && timer.delay(getDelay())) {
                mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, 45, 0, ClickType.PICKUP, mc.player);
                this.inventoryOpen = true;
                this.clickOffHand = false;
                timer.reset();
            }

            if (this.offhandItems.isCurrentMode("Golden Apple")) {
                ItemStack offHand = mc.player.getInventory().offhand.get(0);
                int slot = InventoryUtils.getItemSlot(Items.GOLDEN_APPLE);
                if (slot != -1 && timer.delay(getDelay())) {
                    if (offHand.getItem() == Items.GOLDEN_APPLE) {
                        ItemStack goldenAppleStack = mc.player.getInventory().items.get(slot);
                        if (offHand.getCount() + goldenAppleStack.getCount() <= 64) {
                            if (slot < 9) {
                                mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot + 36, 0, ClickType.PICKUP, mc.player);
                            } else {
                                mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, 0, ClickType.PICKUP, mc.player);
                            }

                            this.inventoryOpen = true;
                            this.clickOffHand = true;
                            timer.reset();
                        }
                    } else {
                        this.swapOffHand(slot);
                    }
                }
            } else if (this.offhandItems.isCurrentMode("Projectile")) {
                ItemStack offHand = mc.player.getInventory().offhand.get(0);
                ItemStack bestProjectile = InventoryUtils.getBestProjectile();
                if (bestProjectile != null) {
                    int slot = InventoryUtils.getItemStackSlot(bestProjectile);
                    boolean shouldSwap = false;
                    if (offHand.getItem() != Items.EGG && offHand.getItem() != Items.SNOWBALL) {
                        shouldSwap = true;
                    } else if (offHand.getCount() < bestProjectile.getCount()) {
                        shouldSwap = true;
                    }

                    if (shouldSwap && slot != -1 && timer.delay(getDelay())) {
                        this.swapOffHand(slot);
                    }
                }
            } else if (this.offhandItems.isCurrentMode("Fishing Rod")) {
                ItemStack offHand = mc.player.getInventory().offhand.get(0);
                int slotx = InventoryUtils.getItemSlot(Items.FISHING_ROD);
                if (slotx != -1 && timer.delay(getDelay()) && offHand.getItem() != Items.FISHING_ROD) {
                    this.swapOffHand(slotx);
                }
            } else if (this.offhandItems.isCurrentMode("Block")) {
                ItemStack offHand = mc.player.getInventory().offhand.get(0);
                ItemStack bestBlock = InventoryUtils.getBestBlock();
                if (bestBlock != null) {
                    int slotx = InventoryUtils.getItemStackSlot(bestBlock);
                    boolean shouldSwapx = false;
                    if (Scaffold.isValidStack(offHand)) {
                        if (offHand.getCount() < bestBlock.getCount()) {
                            shouldSwapx = true;
                        }
                    } else {
                        shouldSwapx = true;
                    }

                    if (shouldSwapx && slotx != -1 && timer.delay(getDelay())) {
                        this.swapOffHand(slotx);
                    }
                }
            }

            if (this.switchGoldenApple.getCurrentValue() && !this.offhandItems.isCurrentMode("Golden Apple")) {
                this.swapItem((int) (this.goldenAppleSlot.getCurrentValue() - 1.0F), Items.GOLDEN_APPLE);
            }

            if (this.switchBlock.getCurrentValue()) {
                int blockSlot = (int) (this.blockSlot.getCurrentValue() - 1.0F);
                ItemStack currentBlock = mc.player.getInventory().items.get(blockSlot);
                ItemStack bestBlock = InventoryUtils.getBestBlock();
                if (bestBlock != null
                        && (bestBlock.getCount() > currentBlock.getCount() || !Scaffold.isValidStack(currentBlock))
                        && !this.offhandItems.isCurrentMode("Block")) {
                    this.swapItem(blockSlot, bestBlock);
                }

                if ((float) InventoryUtils.getBlockCountInInventory() > this.maxBlockSize.getCurrentValue()) {
                    ItemStack worstBlock = InventoryUtils.getWorstBlock();
                    this.throwItem(worstBlock);
                }
            }

            if (this.switchSword.getCurrentValue()) {
                int slotxx = (int) (this.swordSlot.getCurrentValue() - 1.0F);
                ItemStack currentSword = mc.player.getInventory().items.get(slotxx);
                ItemStack bestSword = InventoryUtils.getBestSword();
                ItemStack bestShapeAxe = InventoryUtils.getBestShapeAxe();
                if (InventoryUtils.getAxeDamage(bestShapeAxe) > InventoryUtils.getSwordDamage(bestSword)) {
                    bestSword = bestShapeAxe;
                }

                if (bestSword != null) {
                    float currentDamage = currentSword.getItem() instanceof SwordItem
                            ? InventoryUtils.getSwordDamage(currentSword)
                            : InventoryUtils.getAxeDamage(currentSword);
                    float bestWeaponDamage = bestSword.getItem() instanceof SwordItem
                            ? InventoryUtils.getSwordDamage(bestSword)
                            : InventoryUtils.getAxeDamage(bestSword);
                    if (bestWeaponDamage > currentDamage) {
                        this.swapItem(slotxx, bestSword);
                    }
                }
            }

            if (this.switchPickaxe.getCurrentValue()) {
                int slotxxx = (int) (this.pickaxeSlot.getCurrentValue() - 1.0F);
                ItemStack bestPickaxe = InventoryUtils.getBestPickaxe();
                ItemStack currentPickaxe = mc.player.getInventory().items.get(slotxxx);
                if (bestPickaxe != null
                        && bestPickaxe.getItem() instanceof PickaxeItem
                        && (InventoryUtils.getToolScore(bestPickaxe) > InventoryUtils.getToolScore(currentPickaxe) || !(currentPickaxe.getItem() instanceof PickaxeItem))
                ) {
                    this.swapItem(slotxxx, bestPickaxe);
                }
            }

            if (this.switchAxe.getCurrentValue()) {
                int slotxxx = (int) (this.axeSlot.getCurrentValue() - 1.0F);
                ItemStack bestAxe = InventoryUtils.getBestAxe();
                ItemStack currentAxe = mc.player.getInventory().items.get(slotxxx);
                if (bestAxe != null
                        && bestAxe.getItem() instanceof AxeItem
                        && (InventoryUtils.getToolScore(bestAxe) > InventoryUtils.getToolScore(currentAxe) || !(currentAxe.getItem() instanceof AxeItem))) {
                    this.swapItem(slotxxx, bestAxe);
                }
            }

            if (this.switchRod.getCurrentValue() && !this.offhandItems.isCurrentMode("Fishing Rod")) {
                int slotxxx = (int) (this.rodSlot.getCurrentValue() - 1.0F);
                ItemStack bestRod = InventoryUtils.getFishingRod();
                ItemStack currentRod = mc.player.getInventory().items.get(slotxxx);
                if (!(currentRod.getItem() instanceof FishingRodItem)) {
                    this.swapItem(slotxxx, bestRod);
                }
            }

            if (this.switchBow.getCurrentValue()) {
                int slotxxx = (int) (this.bowSlot.getCurrentValue() - 1.0F);
                ItemStack currentBow = mc.player.getInventory().items.get(slotxxx);
                ItemStack bestBow;
                float bestBowScore;
                float currentBowScore;
                if (this.preferBow.isCurrentMode("Crossbow")) {
                    bestBow = InventoryUtils.getBestCrossbow();
                    bestBowScore = InventoryUtils.getCrossbowScore(bestBow);
                    currentBowScore = InventoryUtils.getCrossbowScore(currentBow);
                } else if (this.preferBow.isCurrentMode("Power Bow")) {
                    bestBow = InventoryUtils.getBestPowerBow();
                    bestBowScore = InventoryUtils.getPowerBowScore(bestBow);
                    currentBowScore = InventoryUtils.getPowerBowScore(currentBow);
                } else {
                    bestBow = InventoryUtils.getBestPunchBow();
                    bestBowScore = InventoryUtils.getPunchBowScore(bestBow);
                    currentBowScore = InventoryUtils.getPunchBowScore(currentBow);
                }

                if (bestBow == null) {
                    bestBow = InventoryUtils.getBestCrossbow();
                    bestBowScore = InventoryUtils.getCrossbowScore(bestBow);
                    currentBowScore = InventoryUtils.getCrossbowScore(currentBow);
                }

                if (bestBow == null) {
                    bestBow = InventoryUtils.getBestPowerBow();
                    bestBowScore = InventoryUtils.getPowerBowScore(bestBow);
                    currentBowScore = InventoryUtils.getPowerBowScore(currentBow);
                }

                if (bestBow == null) {
                    bestBow = InventoryUtils.getBestPunchBow();
                    bestBowScore = InventoryUtils.getPunchBowScore(bestBow);
                    currentBowScore = InventoryUtils.getPunchBowScore(currentBow);
                }

                if (bestBow != null && bestBowScore > currentBowScore) {
                    this.swapItem(slotxxx, bestBow);
                }

                if ((float) InventoryUtils.getItemCount(Items.ARROW) > this.maxArrowSize.getCurrentValue()) {
                    ItemStack worstArrow = InventoryUtils.getWorstArrow();
                    this.throwItem(worstArrow);
                }
            }

            if (this.switchEnderPearl.getCurrentValue()) {
                this.swapItem((int) (this.enderPearlSlot.getCurrentValue() - 1.0F), Items.ENDER_PEARL);
            }

            if (this.switchWaterBucket.getCurrentValue()) {
                this.swapItem((int) (this.waterBucketSlot.getCurrentValue() - 1.0F), Items.WATER_BUCKET);
            }

            if (this.switchFireball.getCurrentValue()) {
                this.swapItem((int) (this.fireballSlot.getCurrentValue() - 1.0F), Items.FIRE_CHARGE);
            }

            if (this.keepProjectile.getCurrentValue()) {
                if ((float) (InventoryUtils.getItemCount(Items.EGG) + InventoryUtils.getItemCount(Items.SNOWBALL)) > this.maxProjectileSize.getCurrentValue()) {
                    ItemStack worstProjectile = InventoryUtils.getWorstProjectile();
                    this.throwItem(worstProjectile);
                }

                if (this.switchProjectile.getCurrentValue() && !this.offhandItems.isCurrentMode("Projectile")) {
                    int projectileSlot = (int) (this.projectileSlot.getCurrentValue() - 1.0F);
                    if (InventoryUtils.getItemCount(Items.EGG) > 0) {
                        this.swapItem(projectileSlot, Items.EGG);
                    } else if (InventoryUtils.getItemCount(Items.SNOWBALL) > 0) {
                        this.swapItem(projectileSlot, Items.SNOWBALL);
                    }
                }
            }

            if (this.throwItems.getCurrentValue()) {
                List<Integer> slots = IntStream.range(0, mc.player.getInventory().items.size()).boxed().collect(Collectors.toList());
                Collections.shuffle(slots);

                for (Integer slotxxxx : slots) {
                    ItemStack stack = mc.player.getInventory().items.get(slotxxxx);
                    if (!stack.isEmpty() && !this.isItemUseful(stack)) {
                        this.throwItem(stack);
                    }
                }
            }
        }
    }

    private void swapOffHand(int slot) {
        if (slot < 9) {
            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot + 36, 40, ClickType.SWAP, mc.player);
        } else {
            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, 40, ClickType.SWAP, mc.player);
        }

        this.inventoryOpen = true;
        timer.reset();
    }

    private void throwItem(ItemStack item) {
        if (InventoryUtils.isItemValid(item) && timer.delay(getDelay())) {
            int itemSlot = InventoryUtils.getItemStackSlot(item);
            if (itemSlot != -1) {
                if (itemSlot < 9) {
                    mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, itemSlot + 36, 1, ClickType.THROW, mc.player);
                } else {
                    mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, itemSlot, 1, ClickType.THROW, mc.player);
                }

                this.inventoryOpen = true;
                timer.reset();
            }
        }
    }

    private void swapItem(int targetSlot, ItemStack bestItem) {
        ItemStack currentSlot = mc.player.getInventory().items.get(targetSlot);
        if (InventoryUtils.isItemValid(currentSlot) && bestItem != currentSlot && timer.delay(getDelay())) {
            int bestItemSlot = InventoryUtils.getItemStackSlot(bestItem);
            if (bestItemSlot != -1) {
                if (bestItemSlot < 9) {
                    mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, bestItemSlot + 36, targetSlot, ClickType.SWAP, mc.player);
                } else {
                    mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, bestItemSlot, targetSlot, ClickType.SWAP, mc.player);
                }

                this.inventoryOpen = true;
                timer.reset();
            }
        }
    }

    private void swapItem(int targetSlot, Item item) {
        ItemStack currentSlot = mc.player.getInventory().items.get(targetSlot);
        if (InventoryUtils.isItemValid(currentSlot) && timer.delay(getDelay())) {
            int bestItemSlot = InventoryUtils.getItemSlot(item);
            if (bestItemSlot != -1) {
                ItemStack bestItemStack = mc.player.getInventory().items.get(bestItemSlot);
                if (currentSlot.getItem() != item || currentSlot.getItem() == item && currentSlot.getCount() < bestItemStack.getCount()) {
                    if (bestItemSlot < 9) {
                        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, bestItemSlot + 36, targetSlot, ClickType.SWAP, mc.player);
                    } else {
                        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, bestItemSlot, targetSlot, ClickType.SWAP, mc.player);
                    }

                    this.inventoryOpen = true;
                    timer.reset();
                }
            }
        }
    }
    
    private int getDelay() {
        return MathUtils.getRandomIntInRange((int) minDelay.getCurrentValue(), (int) maxDelay.getCurrentValue() + 1);
    }
}
