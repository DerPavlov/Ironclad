package at.pavlov.ironclad.event;

import at.pavlov.ironclad.cannon.Craft;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CraftDestroyedEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private final Craft craft;

	public CraftDestroyedEvent(Craft craft) {

        this.craft = craft;
    }
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Craft getCraft() {
        return craft;
    }
}
