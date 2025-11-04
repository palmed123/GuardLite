package cnm.obsoverlay.events.api.events.callables;

import cnm.obsoverlay.events.api.events.Cancellable;
import cnm.obsoverlay.events.api.events.Event;

public abstract class EventCancellable implements Event, Cancellable {
   public boolean cancelled;

   protected EventCancellable() {
   }

   @Override
   public boolean isCancelled() {
      return this.cancelled;
   }

   @Override
   public void setCancelled(boolean state) {
      this.cancelled = state;
   }
}
