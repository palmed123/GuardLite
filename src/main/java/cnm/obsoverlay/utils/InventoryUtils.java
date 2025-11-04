package cnm.obsoverlay.utils;

import cnm.obsoverlay.modules.impl.move.Scaffold;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BookItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;

public class InventoryUtils implements Wrapper {

   public static boolean shouldDisableFeatures() {
      return getAllItems().stream().anyMatch(item -> {
         if (item.isEmpty()) {
            return false;
         } else {
            String string = item.getDisplayName().getString();
            return string.contains("长按点击") || string.contains("点击使用") || string.contains("离开游戏") || string.contains("选择一个队伍") || string.contains("再来一局");
         }
      });
   }

   public static boolean isGoldenHead(ItemStack e) {
      if (e.isEmpty()) {
         return false;
      } else {
         if (e.getItem() instanceof BlockItem) {
            BlockItem item = (BlockItem)e.getItem();
            if (item.getBlock() instanceof SkullBlock) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean isSharpnessAxe(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (!(stack.getItem() instanceof AxeItem)) {
         return false;
      } else {
         int itemEnchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, stack);
         return itemEnchantmentLevel >= 8 && itemEnchantmentLevel < 50;
      }
   }

   public static boolean isGodAxe(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         return stack.getItem() != Items.GOLDEN_AXE ? false : EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, stack) > 100;
      }
   }

   public static boolean isEnchantedGApple(ItemStack stack) {
      return stack.isEmpty() ? false : stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
   }

   public static boolean isEndCrystal(ItemStack stack) {
      return stack.isEmpty() ? false : stack.getItem() == Items.END_CRYSTAL;
   }

   public static boolean isKBBall(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         return stack.getItem() != Items.SLIME_BALL ? false : EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, stack) > 1;
      }
   }

   public static boolean isKBStick(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         return stack.getItem() != Items.STICK ? false : EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, stack) > 1;
      }
   }

   public static int findEmptyInventory() {
      for (int i = 9; i < mc.player.getInventory().items.size(); i++) {
         if (((ItemStack)mc.player.getInventory().items.get(i)).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   public static int findEmptySlot() {
      for (int i = 0; i < 9; i++) {
         if (((ItemStack)mc.player.getInventory().items.get(i)).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   public static int getPunchLevel(ItemStack stack) {
      return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, stack);
   }

   public static int getPowerLevel(ItemStack stack) {
      return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack);
   }

   public static List<ItemStack> getAllItems() {
      ArrayList<ItemStack> list = new ArrayList<>(40);
      list.addAll(mc.player.getInventory().items);
      list.addAll(mc.player.getInventory().armor);
      return list;
   }

   public static float getBestArmorScore(EquipmentSlot slot) {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof ArmorItem && ((ArmorItem)item.getItem()).getEquipmentSlot() == slot)
         .map(InventoryUtils::getProtection)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static float getCurrentArmorScore(EquipmentSlot slot) {
      if (slot == EquipmentSlot.HEAD) {
         return getProtection((ItemStack)mc.player.getInventory().armor.get(3));
      } else if (slot == EquipmentSlot.CHEST) {
         return getProtection((ItemStack)mc.player.getInventory().armor.get(2));
      } else if (slot == EquipmentSlot.LEGS) {
         return getProtection((ItemStack)mc.player.getInventory().armor.get(1));
      } else {
         return slot == EquipmentSlot.FEET ? getProtection((ItemStack)mc.player.getInventory().armor.get(0)) : 0.0F;
      }
   }

   public static float getBestSwordDamage() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof SwordItem)
         .map(InventoryUtils::getSwordDamage)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static ItemStack getBestSword() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof SwordItem)
         .max(Comparator.comparingInt(s -> (int)(getSwordDamage(s) * 100.0F)))
         .orElse(null);
   }

   public static int getItemStackSlot(ItemStack stack) {
      if (stack == null) {
         return -1;
      } else {
         for (int i = 0; i < mc.player.getInventory().items.size(); i++) {
            if (mc.player.getInventory().items.get(i) == stack) {
               return i;
            }
         }

         return -1;
      }
   }

   public static boolean isItemValid(ItemStack s) {
      if (!s.isEmpty()) {
         if (s.getItem() instanceof PlayerHeadItem) {
            return false;
         }

         String string = s.getDisplayName().getString();
         if (string.contains("Click")) {
            return false;
         }

         if (string.contains("Right")) {
            return false;
         }

         if (string.contains("点击")) {
            return false;
         }

         if (string.contains("Teleport")) {
            return false;
         }

         if (string.contains("使用")) {
            return false;
         }

         if (string.contains("传送")) {
            return false;
         }

         if (string.contains("再来")) {
            return false;
         }
      }

      return true;
   }

   public static int getItemSlot(Item item) {
      for (int i = 0; i < mc.player.getInventory().items.size(); i++) {
         ItemStack itemStack = (ItemStack)mc.player.getInventory().items.get(i);
         if (itemStack.getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   public static ItemStack getBestProjectile() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && (item.getItem() == Items.EGG || item.getItem() == Items.SNOWBALL) && isItemValid(item))
         .max(Comparator.comparingInt(ItemStack::getCount))
         .orElse(null);
   }

   public static ItemStack getFishingRod() {
      return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof FishingRodItem && isItemValid(item)).findAny().orElse(null);
   }

   public static int getBlockCountInInventory() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && Scaffold.isValidStack(item) && isItemValid(item))
         .mapToInt(ItemStack::getCount)
         .sum();
   }

   /**
    * 只获取快捷栏（Hotbar）9 个槽位中 Scaffold 可用的方块数量
    * 索引范围：0-8
    */
   public static int getBlockCountInHotbar() {
      int count = 0;
      for (int i = 0; i < 9; i++) {
         ItemStack item = mc.player.getInventory().getItem(i);
         if (!item.isEmpty() && item.getItem() instanceof BlockItem && Scaffold.isValidStack(item) && isItemValid(item)) {
            count += item.getCount();
         }
      }
      return count;
   }

   public static ItemStack getWorstProjectile() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && (item.getItem() == Items.EGG || item.getItem() == Items.SNOWBALL))
         .min(Comparator.comparingInt(ItemStack::getCount))
         .orElse(null);
   }

   public static ItemStack getWorstArrow() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof ArrowItem && isItemValid(item))
         .min(Comparator.comparingInt(ItemStack::getCount))
         .orElse(null);
   }

   public static ItemStack getWorstBlock() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && Scaffold.isValidStack(item) && isItemValid(item))
         .min(Comparator.comparingInt(ItemStack::getCount))
         .orElse(null);
   }

   public static ItemStack getBestBlock() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && Scaffold.isValidStack(item) && isItemValid(item))
         .max(Comparator.comparingInt(ItemStack::getCount))
         .orElse(null);
   }

   public static float getBestPickaxeScore() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof PickaxeItem && isItemValid(item))
         .map(InventoryUtils::getToolScore)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static ItemStack getBestPickaxe() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof PickaxeItem && isItemValid(item))
         .max(Comparator.comparingInt(s -> (int)(getToolScore(s) * 100.0F)))
         .orElse(null);
   }

   public static float getBestAxeScore() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof AxeItem && !isSharpnessAxe(item) && isItemValid(item))
         .map(InventoryUtils::getToolScore)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static ItemStack getBestAxe() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof AxeItem && !isSharpnessAxe(item) && isItemValid(item))
         .max(Comparator.comparingInt(s -> (int)(getToolScore(s) * 100.0F)))
         .orElse(null);
   }

   public static ItemStack getBestShapeAxe() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof AxeItem && isSharpnessAxe(item) && isItemValid(item) && !isGodAxe(item))
         .max(Comparator.comparingInt(s -> (int)(getAxeDamage(s) * 100.0F)))
         .orElse(null);
   }

   public static float getBestShovelScore() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof ShovelItem && isItemValid(item))
         .map(InventoryUtils::getToolScore)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static ItemStack getBestShovel() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof ShovelItem && isItemValid(item))
         .max(Comparator.comparingInt(s -> (int)(getToolScore(s) * 100.0F)))
         .orElse(null);
   }

   public static float getBestCrossbowScore() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof CrossbowItem && isItemValid(item))
         .map(InventoryUtils::getCrossbowScore)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static ItemStack getBestCrossbow() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof CrossbowItem && isItemValid(item))
         .max(Comparator.comparingInt(s -> (int)(getCrossbowScore(s) * 100.0F)))
         .orElse(null);
   }

   public static float getBestPunchBowScore() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
         .map(InventoryUtils::getPunchBowScore)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static ItemStack getBestPunchBow() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
         .max(Comparator.comparingInt(s -> (int)(getPunchBowScore(s) * 100.0F)))
         .orElse(null);
   }

   public static float getBestPowerBowScore() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
         .map(InventoryUtils::getPowerBowScore)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static ItemStack getBestPowerBow() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
         .max(Comparator.comparingInt(s -> (int)(getPowerBowScore(s) * 100.0F)))
         .orElse(null);
   }

   public static boolean isPunchBow(ItemStack stack) {
      return getPunchBowScore(stack) > 10.0F && isItemValid(stack);
   }

   public static boolean isPowerBow(ItemStack stack) {
      return getPowerBowScore(stack) > 10.0F && isItemValid(stack);
   }

   public static boolean hasItem(Item checkItem) {
      return getAllItems().stream().anyMatch(item -> !item.isEmpty() && item.getItem() == checkItem);
   }

   public static int getItemCount(Item checkItem) {
      return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() == checkItem).mapToInt(ItemStack::getCount).sum();
   }

   public static float getPunchBowScore(ItemStack stack) {
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else if (stack.getItem() instanceof BowItem) {
         float valence = 10.0F;
         valence += (float)EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, stack);
         valence += (float)EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack);
         valence += (float)EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, stack);
         valence += (float)EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack) / 10.0F;
         return valence + (float)stack.getDamageValue() / (float)stack.getMaxDamage();
      } else {
         return 0.0F;
      }
   }

   public static float getPowerBowScore(ItemStack stack) {
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else if (stack.getItem() instanceof BowItem) {
         float valence = 10.0F;
         valence += (float)EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, stack) / 10.0F;
         valence += (float)EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack);
         valence += (float)EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, stack);
         valence += (float)EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack);
         return valence + (float)stack.getDamageValue() / (float)stack.getMaxDamage();
      } else {
         return 0.0F;
      }
   }

   public static float getToolScore(ItemStack stack) {
      float valence = 0.0F;
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else if (isGodItem(stack)) {
         return 0.0F;
      } else if (isSharpnessAxe(stack)) {
         return 0.0F;
      } else {
         if (stack.getItem() instanceof PickaxeItem) {
            valence += stack.getDestroySpeed(Blocks.STONE.defaultBlockState());
         } else if (stack.getItem() instanceof AxeItem) {
            valence += stack.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState());
         } else {
            if (!(stack.getItem() instanceof ShovelItem)) {
               return 0.0F;
            }

            valence += stack.getDestroySpeed(Blocks.DIRT.defaultBlockState());
         }

         int efficiency = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, stack);
         if (efficiency > 0) {
            valence += (float)efficiency * 0.0075F;
         }

         return valence;
      }
   }

   public static float getAxeDamage(ItemStack stack) {
      float valence = 0.0F;
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else {
         if (stack.getItem() instanceof AxeItem && isSharpnessAxe(stack)) {
            AxeItem axe = (AxeItem)stack.getItem();
            if (axe == Items.WOODEN_AXE) {
               valence += 4.0F;
            } else if (axe == Items.STONE_AXE) {
               valence += 5.0F;
            } else if (axe == Items.IRON_AXE) {
               valence += 6.0F;
            } else if (axe == Items.GOLDEN_AXE) {
               valence += 4.0F;
            } else if (axe == Items.DIAMOND_AXE) {
               valence += 7.0F;
            }
         }

         int itemEnchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, stack);
         if (itemEnchantmentLevel > 0) {
            float damageBonus = Enchantments.SHARPNESS.getDamageBonus(itemEnchantmentLevel, MobType.UNDEFINED);
            valence += damageBonus;
         }

         return valence;
      }
   }

   public static float getSwordDamage(ItemStack stack) {
      float valence = 0.0F;
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else {
         if (stack.getItem() instanceof SwordItem) {
            SwordItem sword = (SwordItem)stack.getItem();
            valence += sword.getDamage() + 1.0F;
         }

         int itemEnchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, stack);
         if (itemEnchantmentLevel > 0) {
            float damageBonus = Enchantments.SHARPNESS.getDamageBonus(itemEnchantmentLevel, MobType.UNDEFINED);
            valence += damageBonus;
         }

         return valence;
      }
   }

   public static float getProtection(ItemStack itemStack) {
      int valence = 0;
      if (itemStack == null) {
         return 0.0F;
      } else if (itemStack.isEmpty()) {
         return 0.0F;
      } else {
         if (itemStack.getItem() instanceof ArmorItem) {
            ArmorItem armor = (ArmorItem)itemStack.getItem();
            ArmorMaterial material = armor.getMaterial();
            if (material == ArmorMaterials.LEATHER) {
               valence += 100;
            } else if (material == ArmorMaterials.CHAIN) {
               valence += 200;
            } else if (material == ArmorMaterials.IRON) {
               valence += 400;
            } else if (material == ArmorMaterials.GOLD) {
               valence += 300;
            } else if (material == ArmorMaterials.DIAMOND) {
               valence += 500;
            } else if (material == ArmorMaterials.NETHERITE) {
               valence += 600;
            }
         }

         valence += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, itemStack);
         return (float)valence;
      }
   }

   public static float getCrossbowScore(ItemStack stack) {
      int valence = 0;
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else {
         if (stack.getItem() instanceof CrossbowItem) {
            valence += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
            valence += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, stack);
            valence += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, stack);
         }

         return (float)valence;
      }
   }

   public static boolean isGodItem(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (stack.getItem() instanceof AxeItem
         && stack.getItem() == Items.GOLDEN_AXE
         && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, stack) > 100) {
         return true;
      } else if (stack.getItem() == Items.SLIME_BALL && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, stack) > 1) {
         return true;
      } else {
         return stack.getItem() == Items.TOTEM_OF_UNDYING ? true : stack.getItem() == Items.END_CRYSTAL;
      }
   }

   public static boolean isCommonItemUseful(ItemStack stack) {
      if (stack.isEmpty()) {
         return true;
      } else {
         Item item = stack.getItem();
         if (item instanceof BlockItem block) {
            if (block.getBlock() == Blocks.ENCHANTING_TABLE) {
               return false;
            }

            if (block.getBlock() == Blocks.COBWEB) {
               return false;
            }
         } else {
            if (item instanceof BookItem) {
               return false;
            }

            if (item instanceof ExperienceBottleItem) {
               return false;
            }

            if (item instanceof FireworkRocketItem) {
               return false;
            }

            if (item == Items.WHEAT_SEEDS || item == Items.BEETROOT_SEEDS || item == Items.MELON_SEEDS || item == Items.PUMPKIN_SEEDS) {
               return false;
            }

            if (item == Items.FLINT_AND_STEEL) {
               return false;
            }
         }

         return true;
      }
   }
}
