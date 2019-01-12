package at.pavlov.ironclad.listener;


import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.Enum.BreakCause;
import at.pavlov.ironclad.cannon.Craft;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;
import java.util.List;

public class BlockListener implements Listener
{
	private final Ironclad plugin;

	public BlockListener(Ironclad plugin)
	{
		this.plugin = plugin;
	}


    @EventHandler
    public void blockExplodeEvent(BlockExplodeEvent event) {
        if (plugin.getMyConfig().isRelayExplosionEvent()) {
            EntityExplodeEvent explodeEvent = new EntityExplodeEvent(null, event.getBlock().getLocation(), event.blockList(), event.getYield());
            Bukkit.getServer().getPluginManager().callEvent(explodeEvent);
            event.setCancelled(explodeEvent.isCancelled());
        }

        //ironclad event - remove unbreakable blocks like bedrock
        //this will also affect other plugins which spawn bukkit explosions
        List<Block> blocks = event.blockList();
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            for (BlockData unbreakableBlock : plugin.getMyConfig().getUnbreakableBlocks()) {
                if (unbreakableBlock.matches(block.getBlockData())) {
                    blocks.remove(i--);
                }
            }
        }

        //search for destroyed ironclad
        plugin.getEntityListener().ExplosionEventHandler(event.blockList());
    }

    /**
     * Water will not destroy button and torches
     * @param event
     */
    @EventHandler
    public void BlockFromTo(BlockFromToEvent event)
    {
        Block block = event.getToBlock();
        Craft craft = plugin.getCraftManager().getCraft(block.getLocation(), null);
        if (craft !=  null)//block.getType() == Material.STONE_BUTTON || block.getType() == Material.WOOD_BUTTON || block.getType() == Material.   || block.getType() == Material.TORCH)
        {
            if (craft.isCraftBlock(block))
            {
                event.setCancelled(true);
            }
        }
    }

    /**
     * prevent fire on ironclad
     * @param event
     */
    @EventHandler
    public void BlockSpread(BlockSpreadEvent  event)
    {
        Block block = event.getBlock().getRelative(BlockFace.DOWN);
        Craft craft = plugin.getCraftManager().getCraft(block.getLocation(), null);

        if (craft !=  null)
        {
            if (craft.isCraftBlock(block))
            {
                event.setCancelled(true);
            }
        }
    }


    /**
     * retraction pistons will trigger this event. If the pulled block is part of a cannon, it is canceled
     * @param event - BlockPistonRetractEvent
     */
    @EventHandler
    public void BlockPistonRetract(BlockPistonRetractEvent event)
    {
        // when piston is sticky and has a cannon block attached delete the
        // cannon
        if (event.isSticky())
        {
            Location loc = event.getBlock().getRelative(event.getDirection(), 2).getLocation();
            Craft craft = plugin.getCraftManager().getCraft(loc, null);
            if (craft != null)
            {
                event.setCancelled(true);
            }
        }
    }

    /**
     * pushing pistons will trigger this event. If the pused block is part of a cannon, it is canceled
     * @param event - BlockPistonExtendEvent
     */
    @EventHandler
    public void BlockPistonExtend(BlockPistonExtendEvent event)
    {
        // when the moved block is a cannonblock
        for (Iterator<Block> iter = event.getBlocks().iterator(); iter.hasNext();)
        {
            // if moved block is cannonBlock delete craft
            Craft craft = plugin.getCraftManager().getCraft(iter.next().getLocation(), null);
            if (craft != null)
            {
                event.setCancelled(true);
            }
        }
    }

    /**
     * if the block catches fire this event is triggered. Ironclad can't burn.
     * @param event - BlockBurnEvent
     */
    @EventHandler
    public void BlockBurn(BlockBurnEvent event)
    {
        // the cannon will not burn down
        if (plugin.getCraftManager().getCraft(event.getBlock().getLocation(), null) != null)
        {
            event.setCancelled(true);
        }
    }

    /**
     * if one block of the cannon is destroyed, it is removed from the list of ironclad
     * @param event - BlockBreakEvent
     */
    @EventHandler
    public void BlockBreak(BlockBreakEvent event)
    {

        Craft craft = plugin.getCraftManager().getCraft(event.getBlock().getLocation(), null);
        if (craft != null)
        {
            //breaking is only allowed when the barrel is broken - minor stuff as buttons are canceled
            //you can't break your own craft in aiming mode
            //breaking craft while player is in selection (command) mode is not allowed
            Craft aimingCraft = null;
            if (plugin.getCraftMovement().isInPilotingMode(event.getPlayer().getUniqueId()))
                 aimingCraft = plugin.getCraftMovement().getCraftInAimingMode(event.getPlayer());

            if (craft.isDestructibleBlock(event.getBlock().getLocation()) && (aimingCraft ==null||!craft.equals(aimingCraft)) && !plugin.getCommandListener().isSelectingMode(event.getPlayer())) {
                plugin.getCraftManager().removeCannon(craft, false, true, BreakCause.PlayerBreak);
                plugin.logDebug("craft broken:  " + craft.isDestructibleBlock(event.getBlock().getLocation()));
            }
            else {
                event.setCancelled(true);
                plugin.logDebug("cancelled craft destruction: " + craft.isDestructibleBlock(event.getBlock().getLocation()));
            }
        }
    }
}