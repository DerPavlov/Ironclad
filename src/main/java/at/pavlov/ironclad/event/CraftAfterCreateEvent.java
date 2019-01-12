package at.pavlov.ironclad.event;

import at.pavlov.ironclad.cannon.Craft;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class CraftAfterCreateEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private final Craft craft;
	private final UUID player;
	
	public CraftAfterCreateEvent(Craft craft, UUID player) {
		
		this.craft = craft;
		this.player = player;
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

    public UUID getPlayer() {
        return player;
    }

}
