package at.pavlov.ironclad.listener;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import at.pavlov.ironclad.Enum.BreakCause;
import at.pavlov.ironclad.cannon.Craft;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;

import at.pavlov.ironclad.Ironclad;

public class EntityListener implements Listener
{
	private final Ironclad plugin;
	
	public EntityListener(Ironclad plugin)
	{
		this.plugin = plugin;
	}

    /**
     * The projectile explosion has damaged an entity
     * @param event
     */
    @EventHandler
    public void onEntityDamageByBlockEvent(EntityDamageByBlockEvent event)
    {
        //if (plugin.getProjectileManager().isFlyingProjectile(event.getDamager()))
        {
            //event.setCancelled(true);
            //plugin.logDebug("Explosion damage was canceled. Damage done: " + event.getDamage());
        }
    }

	/**
	 * handles the explosion event. Protects the buttons and torches of a cannon, because they break easily
	 * @param event
	 */
	@EventHandler
	public void EntityExplode(EntityExplodeEvent event)
	{
		plugin.logDebug("Explode event listener called");

		//do nothing if it is cancelled
		if (event.isCancelled())
			return;
		
		ExplosionEventHandler(event.blockList());
	}

    /**
     * searches for destroyed ironclad in the explosion event and removes ironclad parts which can't be destroyed in an explosion.
     * @param blocklist list of blocks involved in the event
     */
    public void ExplosionEventHandler(List<Block> blocklist){
        HashSet<UUID> remove = new HashSet<UUID>();

        // first search if a barrel block was destroyed.
        for (Block block : blocklist) {
            Craft craft = plugin.getCraftManager().getCraft(block.getLocation(), null);

            // if it is a craft block
            if (craft != null) {
                if (craft.isDestructibleBlock(block.getLocation())) {
                    //this craft is destroyed
                    remove.add(craft.getUID());
                }
            }
        }

        //iterate again and remove all block of intact ironclad
        for (int i = 0; i < blocklist.size(); i++)
        {
            Block block = blocklist.get(i);
            Craft craft = plugin.getCraftManager().getCraft(block.getLocation(), null);

            // if it is a craft block and the craft is not destroyed (see above)
            if (craft != null && !remove.contains(craft.getUID()))
            {
                if (craft.isCraftBlock(block))
                {
                    blocklist.remove(i--);
                }
            }
        }

        //now remove all invalid ironclad
        for (UUID id : remove)
            plugin.getCraftManager().removeCannon(id, false, true, BreakCause.Explosion);
    }
}
