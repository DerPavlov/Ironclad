package at.pavlov.ironclad.listener;

import at.pavlov.ironclad.cannon.Craft;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import at.pavlov.ironclad.cannon.CraftManager;
import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.cannon.CraftDesign;
import at.pavlov.ironclad.config.Config;
import at.pavlov.ironclad.config.UserMessages;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;

public class PlayerListener implements Listener
{
    private final Config config;
    private final UserMessages userMessages;
    private final Ironclad plugin;
    private final CraftManager cannonManager;

    public PlayerListener(Ironclad plugin)
    {
        this.plugin = plugin;
        this.config = this.plugin.getMyConfig();
        this.userMessages = this.plugin.getMyConfig().getUserMessages();
        this.cannonManager = this.plugin.getCraftManager();
    }

    @EventHandler
    public void PlayerDeath(PlayerDeathEvent event)
    {

    }

    @EventHandler
    public void PlayerMove(PlayerMoveEvent event)
    {
        // only active if the player is in moving mode
//        Craft craft =  aiming.getCraftInAimingMode(event.getPlayer());
//        if (!aiming.distanceCheck(event.getPlayer(), craft)) {
//            userMessages.sendMessage(MessageEnum.AimingModeTooFarAway, event.getPlayer());
//            MessageEnum message = aiming.disableAimingMode(event.getPlayer(), craft);
//            userMessages.sendMessage(message, event.getPlayer());
//        }
    }
    /*
    * remove Player from auto aiming list
    * @param event - PlayerQuitEvent
    */
    @EventHandler
    public void LogoutEvent(PlayerQuitEvent event)
    {
        //aiming.removePlayer(event.getPlayer());
    }

    /**
     * cancels the event if the player click a cannon with water
     * @param event - PlayerBucketEmptyEvent
     */
    @EventHandler
    public void PlayerBucketEmpty(PlayerBucketEmptyEvent event)
    {
//        // if player loads a lava/water bucket in the craft
//        Location blockLoc = event.getBlockClicked().getLocation();
//
//        Craft craft = cannonManager.getCraft(blockLoc, event.getPlayer().getUniqueId());
//
//        // check if it is a craft
//        if (craft != null)
//        {
//            // data =-1 means no data check, all buckets are allowed
//            Projectile projectile = plugin.getProjectile(craft, event.getItemStack());
//            if (projectile != null) event.setCancelled(true);
//        }
    }

    /**
     * Create a cannon if the building process is finished Deletes a projectile
     * if loaded Checks for redstone torches if built
     * @param event BlockPlaceEvent
     */
    @EventHandler
    public void BlockPlace(BlockPlaceEvent event)
    {

        Block block = event.getBlockPlaced();
        Location blockLoc = block.getLocation();

        // setup a new cannon
        cannonManager.getCraft(blockLoc, event.getPlayer().getUniqueId());

        // cancel igniting of the craft
        if (event.getBlock().getType() == Material.FIRE)
        {
            // check cannon
            if (event.getBlockAgainst() != null) {
                Location loc = event.getBlockAgainst().getLocation();
                if (cannonManager.getCraft(loc, event.getPlayer().getUniqueId(), true) != null) {
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * handles redstone events (torch, wire, repeater, button
     * @param event - BlockRedstoneEvent
     */
    @EventHandler
    public void RedstoneEvent(BlockRedstoneEvent event)
    {

    }



    /**
     * Handles event if player interacts with the cannon
     * @param event
     */
	@EventHandler
    public void PlayerInteract(PlayerInteractEvent event)
    {
        Action action = event.getAction();

        Block clickedBlock = null;
        if(event.getClickedBlock() == null)
        {
            // no clicked block - get block player is looking at
            Location location = event.getPlayer().getEyeLocation();
            BlockIterator blocksToAdd = new BlockIterator(location, 0, 5);
            Block block = null;
            while(blocksToAdd.hasNext()) {
                block = blocksToAdd.next();
                if (block.getType() != Material.AIR){
                    clickedBlock = block;
                }
            }
            if (clickedBlock == null) {
                clickedBlock = block;
            }
        }
        else
        {
            clickedBlock = event.getClickedBlock();
        }

        if (clickedBlock == null){
            return;
        }

        final Player player = event.getPlayer();
        final Location barrel = clickedBlock.getLocation();

        //if try if the player has really nothing in his hands, or minecraft is blocking it
        final ItemStack eventitem;
        if (event.getItem() == null) {
            eventitem = player.getInventory().getItemInMainHand();
        }
        else{
            eventitem = event.getItem();
        }

        // find craft or add it to the list
        final Craft craft = cannonManager.getCraft(barrel, player.getUniqueId(), false);

        // ############ select a craft ####################
        if(plugin.getCommandListener().isSelectingMode(player))
        {
            if (plugin.getCommandListener().isBlockSelectingMode(player)){
                plugin.getCommandListener().setSelectedBlock(player, clickedBlock);
                event.setCancelled(true);
                return;
            }
            else if (craft != null){
                plugin.getCommandListener().setSelectedCannon(player, craft);
                event.setCancelled(true);
                return;
            }
        }

    	if((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.PHYSICAL) && event.getHand() == EquipmentSlot.HAND && craft != null)
        {
            // get craft design
            final CraftDesign design = craft.getCraftDesign();

            // prevent eggs and snowball from firing when loaded into the gun
            if(config.isCancelItem(eventitem))
                event.setCancelled(true);


//            // ############ set cruising angle ################################
//            if((config.getToolAdjust().equalsFuzzy(eventitem) || config.getToolAutoaim().equalsFuzzy(eventitem)))
//            {
//                plugin.logDebug("change craft angle");
//                event.setCancelled(true);
//
//                if (plugin.getEconomy() != null && !craft.isPaid()){
//                    // craft fee is not paid
//                    userMessages.sendMessage(MessageEnum.ErrorNotPaid, player, craft);
//                    IroncladUtil.playErrorSound(craft.getMuzzle());
//                    return;
//                }
//
//                MessageEnum message = aiming.changeAngle(craft, event.getAction(), event.getBlockFace(), player);
//                userMessages.sendMessage(message, player, craft);
//
//                // update Signs
//                craft.updateCannonSigns();
//
//                if(message!=null)
//                    return;
//            }
        }
        //no craft found - maybe the player has click into the air to stop aiming
        else if(craft == null && action == Action.RIGHT_CLICK_AIR && event.getHand() == EquipmentSlot.HAND){
                // stop aiming mode when right clicking in the air
//                if (config.getToolAutoaim().equalsFuzzy(eventitem))
//                    aiming.pilotingMode(player, null, false);
                plugin.getCommandListener().removeCraftSelector(player);
        }
    }
}
