package at.pavlov.ironclad.event;

import at.pavlov.ironclad.Enum.MessageEnum;
import at.pavlov.ironclad.craft.Craft;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class CraftBeforeCreateEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private final Craft craft;
    private MessageEnum message;
	private final UUID player;
	private boolean cancelled;
	
	public CraftBeforeCreateEvent(Craft craft, MessageEnum message, UUID player) {

        this.craft = craft;
        this.message = message;
        this.player = player;
        this.cancelled = false;
    }
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
 
    public void setCancelled(boolean a) {
        this.cancelled = a;
    }

    public Craft getCraft() {
        return craft;
    }

    public UUID getPlayer() {
        return player;
    }


    public MessageEnum getMessage() {
        return message;
    }

    public void setMessage(MessageEnum message) {
        this.message = message;
    }
}
