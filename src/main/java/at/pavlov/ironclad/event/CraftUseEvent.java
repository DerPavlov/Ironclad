package at.pavlov.ironclad.event;

import at.pavlov.ironclad.Enum.InteractAction;
import at.pavlov.ironclad.craft.Craft;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class CraftUseEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();
    private final Craft craft;
    private final UUID player;
    private final InteractAction action;
    private boolean cancelled;

    public CraftUseEvent(Craft craft, UUID player, InteractAction action)
    {
        this.craft = craft;
        this.player = player;
        this.action = action;
        this.cancelled = false;
    }

    public Craft getCraft() {
        return craft;
    }

    public UUID getPlayer() {
        return player;
    }

    public InteractAction getAction() {
        return action;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
