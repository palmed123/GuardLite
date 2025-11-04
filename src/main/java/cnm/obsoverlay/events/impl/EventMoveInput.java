package cnm.obsoverlay.events.impl;

import cnm.obsoverlay.events.api.events.Event;

public class EventMoveInput implements Event {
   public static Object r;
   public static Object s;
   private float forward;
   private float strafe;
   private boolean jump;
   private boolean sneak;
   private boolean sprinting;
   private double sneakSlowDownMultiplier;

   public float getForward() {
      return this.forward;
   }

   public float getStrafe() {
      return this.strafe;
   }

   public boolean isJump() {
      return this.jump;
   }

   public boolean isSneak() {
      return this.sneak;
   }

   public boolean isSprinting() {
      return this.sprinting;
   }

   public double getSneakSlowDownMultiplier() {
      return this.sneakSlowDownMultiplier;
   }

   public void setForward(float forward) {
      this.forward = forward;
   }

   public void setStrafe(float strafe) {
      this.strafe = strafe;
   }

   public void setJump(boolean jump) {
      this.jump = jump;
   }

   public void setSneak(boolean sneak) {
      this.sneak = sneak;
   }

   public void setSprinting(boolean sprinting) {
      this.sprinting = sprinting;
   }

   public void setSneakSlowDownMultiplier(double sneakSlowDownMultiplier) {
      this.sneakSlowDownMultiplier = sneakSlowDownMultiplier;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventMoveInput other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else if (Float.compare(this.getForward(), other.getForward()) != 0) {
         return false;
      } else if (Float.compare(this.getStrafe(), other.getStrafe()) != 0) {
         return false;
      } else if (this.isJump() != other.isJump()) {
         return false;
      } else if (this.isSneak() != other.isSneak()) {
         return false;
      } else {
         return this.isSprinting() != other.isSprinting() ? false : Double.compare(this.getSneakSlowDownMultiplier(), other.getSneakSlowDownMultiplier()) == 0;
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof EventMoveInput;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + Float.floatToIntBits(this.getForward());
      result = result * 59 + Float.floatToIntBits(this.getStrafe());
      result = result * 59 + (this.isJump() ? 79 : 97);
      result = result * 59 + (this.isSneak() ? 79 : 97);
      result = result * 59 + (this.isSprinting() ? 79 : 97);
      long $sneakSlowDownMultiplier = Double.doubleToLongBits(this.getSneakSlowDownMultiplier());
      return result * 59 + (int)($sneakSlowDownMultiplier >>> 32 ^ $sneakSlowDownMultiplier);
   }

   @Override
   public String toString() {
      return "EventMoveInput(forward="
         + this.getForward()
         + ", strafe="
         + this.getStrafe()
         + ", jump="
         + this.isJump()
         + ", sneak="
         + this.isSneak()
         + ", sprinting="
         + this.isSprinting()
         + ", sneakSlowDownMultiplier="
         + this.getSneakSlowDownMultiplier()
         + ")";
   }

   public EventMoveInput(float forward, float strafe, boolean jump, boolean sneak, boolean sprinting, double sneakSlowDownMultiplier) {
      this.forward = forward;
      this.strafe = strafe;
      this.jump = jump;
      this.sneak = sneak;
      this.sprinting = sprinting;
      this.sneakSlowDownMultiplier = sneakSlowDownMultiplier;
   }

   public EventMoveInput(float forward, float strafe, boolean jump, boolean sneak, double sneakSlowDownMultiplier) {
      this(forward, strafe, jump, sneak, false, sneakSlowDownMultiplier);
   }
}
