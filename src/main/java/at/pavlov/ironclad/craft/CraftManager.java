package at.pavlov.ironclad.craft;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.Enum.BreakCause;
import at.pavlov.ironclad.event.CraftDestroyedEvent;
import at.pavlov.ironclad.utils.IroncladUtil;
import at.pavlov.ironclad.utils.DelayedTask;
import at.pavlov.ironclad.utils.RemoveTaskWrapper;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import at.pavlov.ironclad.config.Config;
import at.pavlov.ironclad.Enum.MessageEnum;
import at.pavlov.ironclad.config.UserMessages;
import at.pavlov.ironclad.container.SimpleBlock;
import at.pavlov.ironclad.event.CraftAfterCreateEvent;
import at.pavlov.ironclad.event.CraftBeforeCreateEvent;


public class CraftManager
{
	private static final ConcurrentHashMap<UUID, Craft> craftList = new ConcurrentHashMap<UUID, Craft>();
    private static final ConcurrentHashMap<String, UUID> craftNameMap = new ConcurrentHashMap<String, UUID>();


    private final Ironclad plugin;
	private final UserMessages userMessages;
	private final Config config;



	public CraftManager(Ironclad ironclad, UserMessages userMessages, Config config)
	{
		this.userMessages = userMessages;
		this.config = config;
		this.plugin = ironclad;
	}

	/**
	 * removes a ironclad from the list that are not valid
     * @param cause the reason why the craft is removed
	 */
	private void removeInvalidCrafts(BreakCause cause)
	{
		Iterator<Craft> iter = craftList.values().iterator();

        while(iter.hasNext())
        {
            Craft next = iter.next();
			if (!next.isValid())
			{
				removeCraft(next, false, false, cause, false, false);
                iter.remove();
			}
		}
	}

    /**
     * deconstructs a craft without the risk of explosion
     * @param craft craft to remove
     */
    public void dismantleCraft(Craft craft, Player player)
    {
        if (craft == null)
            return;
        if (player==null){
            removeCraft(craft, true, false, BreakCause.Dismantling);
            return;
        }
        // admins can dismantle all ironclad
        if (player.hasPermission("ironclad.admin.dismantle"))
            removeCraft(craft, true, false, BreakCause.Dismantling);
        else if (player.hasPermission(craft.getCraftDesign().getPermissionDismantle())) {
            //only the owner of the craft can dismantle a craft
            if (craft.getOwner()!=null && craft.getOwner().equals(player.getUniqueId()))
                removeCraft(craft, true, false, BreakCause.Dismantling);
            else
            userMessages.sendMessage(MessageEnum.ErrorDismantlingNotOwner, player, craft);
        }
        else{
            userMessages.sendMessage(MessageEnum.PermissionErrorDismantle, player, craft);
        }

    }

	/**
	 * removes a craft from the list
	 * @param loc location of the craft
     * @param breakCraft all craft blocks will drop
     * @param canExplode if the craft can explode when loaded with gunpowder
     * @param cause the reason way the craft was broken
	 */
	public void removeCraft(Location loc, boolean breakCraft, boolean canExplode, BreakCause cause)
	{
		Craft craft = getCraft(loc, null);
		removeCraft(craft, breakCraft, canExplode, cause);
	}

	/**
	 * removes a craft from the list
	 * @param craft craft to remove
     * @param breakCraft all craft blocks will drop
     * @param canExplode if the craft can explode when loaded with gunpowder
     * @param cause the reason way the craft was broken
	 */
	public void removeCraft(Craft craft, boolean breakCraft, boolean canExplode, BreakCause cause)
	{
        removeCraft(craft, breakCraft, canExplode, cause, true, true);
	}

    /**
     * removes a craft from the list
     * @param uid UID of the craft
     * @param breakCraft all craft blocks will drop
     * @param canExplode if the craft can explode when loaded with gunpowder
     * @param cause the reason way the craft was broken
     */
    public void removeCraft(UUID uid, boolean breakCraft, boolean canExplode, BreakCause cause)
    {
        removeCraft(uid, breakCraft, canExplode, cause, true);
    }

    /**
     * removes a craft from the list
     * @param uid UID of the craft
     * @param breakCraft all craft blocks will drop
     * @param canExplode if the craft can explode when loaded with gunpowder
     * @param cause the reason way the craft was broken
     * @param removeEntry should the craft be removed from the list
     */
    public void removeCraft(UUID uid, boolean breakCraft, boolean canExplode, BreakCause cause, boolean removeEntry)
    {
        Craft craft = craftList.get(uid);
        removeCraft(craft, breakCraft, canExplode, cause, removeEntry, true);
    }


    /**
     * removes a craft from the list
     * @param craft craft to remove
     * @param breakCraft all craft blocks will drop
     * @param canExplode if the craft can explode when loaded with gunpowder
     * @param cause the reason way the craft was broken
     * @param ignoreInvalid if true invalid ironclad will be skipped and not removed
     */
    public void removeCraft(Craft craft, boolean breakCraft, boolean canExplode, BreakCause cause, boolean removeEntry, boolean ignoreInvalid)
    {
        //ignore invalid ironclad
        if (craft == null || (!craft.isValid() && ignoreInvalid))
            return;

        long delay = 0;
        if (cause == BreakCause.Dismantling || cause == BreakCause.Other) {
            plugin.logDebug("Dismantling," + craft.getCraftDesign().getSoundDismantle().toString());
            IroncladUtil.playSound(craft.getRandomBarrelBlock(), craft.getCraftDesign().getSoundDismantle());
            delay = (long) (craft.getCraftDesign().getDismantlingDelay()*20.0);
        }
        else
            IroncladUtil.playSound(craft.getRandomBarrelBlock(), craft.getCraftDesign().getSoundDestroy());

        //delay the remove task, so it fits to the sound
        RemoveTaskWrapper task = new RemoveTaskWrapper(craft, breakCraft, canExplode, cause, removeEntry, ignoreInvalid);
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new DelayedTask(task) {
            public void run(Object object) {
                RemoveTaskWrapper task = (RemoveTaskWrapper) object;
                Craft craft = task.getCraft();
                BreakCause cause = task.getCause();

                // send message to the owner
                Player player = null;
                if (craft.getOwner() != null) {
                    player = Bukkit.getPlayer(craft.getOwner());
                }


                //fire and an event that this craft is destroyed
                CraftDestroyedEvent destroyedEvent = new CraftDestroyedEvent(craft);
                Bukkit.getServer().getPluginManager().callEvent(destroyedEvent);

                if (craft.getOwner() != null) {
                    OfflinePlayer offplayer = Bukkit.getOfflinePlayer(craft.getOwner());
                    if (offplayer != null && offplayer.hasPlayedBefore() && plugin.getEconomy() != null) {
                        // return message
                        double funds;
                        switch (cause) {
                            case Other:
                                funds = craft.getCraftDesign().getEconomyDismantlingRefund();
                                break;
                            case Dismantling:
                                funds = craft.getCraftDesign().getEconomyDismantlingRefund();
                                break;
                            default:
                                funds = craft.getCraftDesign().getEconomyDestructionRefund();
                                break;
                        }
                        if (craft.isPaid())
                            plugin.getEconomy().depositPlayer(offplayer, funds);
                    }
                }

                MessageEnum message = craft.destroyCraft(task.breakCraft(), task.canExplode(), cause);
                if (player != null)
                    userMessages.sendMessage(message, player, craft);

                //remove from database
                plugin.getPersistenceDatabase().deleteCraft(craft.getUID());
                //remove craft name
                craftNameMap.remove(craft.getCraftName());

                //remove entry
                if (task.removeEntry())
                    craftList.remove(craft.getUID());

            }
        }, delay);
    }


	/**
	 * Checks if the name of a craft is unique
	 * @param name name of the craft
	 * @return true if the name is unique
	 */
	private static boolean isCraftNameUnique(String name)
	{
        if (name == null)
            return false;

		// try to find this in the map
        //there is no such craft name
        //there is such a craft name
        return craftNameMap.get(name) == null;
	}

	/**
	 * generates a new unique craft name
	 * @return name string for the new craft
	 */
	private String newCraftName(Craft craft)
	{		
		//check if this craft has a owner
		if (craft.getOwner() == null)
            return "missing Owner";
			
		String name;
		CraftDesign design = craft.getCraftDesign();
		if (design != null)
			name = design.getDesignName();
		else
			name = "craft";
	

		for (int i = 1; i < Integer.MAX_VALUE; i++)
		{
			String craftName = name + " " + i;

			if (isCraftNameUnique(craftName))
			{
				return craftName;
			}
		}
		return "no unique name";
	}

    public MessageEnum renameCraft(Player player, Craft craft, String newCraftName)
    {
        Validate.notNull(player, "player must not be null");
        Validate.notNull(craft, "craft must not be null");

        //check some permissions
        if (craft.getOwner() != null && !player.getUniqueId().equals(craft.getOwner()))
            return MessageEnum.ErrorNotTheOwner;
        if (!player.hasPermission(craft.getCraftDesign().getPermissionRename()))
            return MessageEnum.PermissionErrorRename;
        if (newCraftName == null || !isCraftNameUnique(newCraftName))
            return MessageEnum.CraftRenameFail;

        //put the new name
        craft.setCraftName(newCraftName);
        craft.updateCraftSigns();

        return MessageEnum.CraftRenameSuccess;

    }

	/**
	 * adds a new craft to the list of ironclad
	 * @param craft create this craft
     * @param saveToDatabase if the craft will be saved to the database after loading
	 */
	public void createCraft(Craft craft, boolean saveToDatabase)
	{
        //the owner can't be null
		if (craft.getOwner() == null)
		{
			plugin.logInfo("can't save a craft when the owner is null");
			return;
		}

        //ignore paid if there is no economy
        if (plugin.getEconomy() == null || craft.getCraftDesign().getEconomyBuildingCost() <= 0)
            craft.setPaid(true);

		//if the craftName is empty make a new one
		if (craft.getCraftName() ==  null || craft.getCraftName().equals(""))
			craft.setCraftName(newCraftName(craft));

		// add craft to the list
		craftList.put(craft.getUID(), craft);
        //add craft name to the list
        craftNameMap.put(craft.getCraftName(), craft.getUID());

        if (saveToDatabase) {
            plugin.getPersistenceDatabase().saveCraft(craft);
            craft.updateCraftSigns();
        }
        else {
            craft.setUpdated(false);
        }
        plugin.logDebug("added craft " + craft.getCraftName());
	}

    /**
     * returns all known ironclad in a sphere around the given location
     * @param center - center of the box
     * @param sphereRadius - radius of the sphere in blocks
     * @return - list of all ironclad in this sphere
     */
    public static HashSet<Craft> getCraftsInSphere(Location center, double sphereRadius)
    {
        HashSet<Craft> newCraftList = new HashSet<Craft>();

        for (Craft craft : getCraftList().values()) {
            if (craft.getWorld().equals(center.getWorld().getUID())) {
                Location newLoc = craft.getCraftDesign().getHullBlocks(craft).get(0);
                if (newLoc.distance(center) < sphereRadius)
                    newCraftList.add(craft);
            }
        }
        return newCraftList;
    }

    /**
     * returns all known ironclad in a box around the given location
     * @param center - center of the box
     * @param lengthX - box length in X
     * @param lengthY - box length in Y
     * @param lengthZ - box length in Z
     * @return - list of all ironclad in this sphere
     */
    public static HashSet<Craft> getCraftsInBox(Location center, double lengthX, double lengthY, double lengthZ)
    {
        HashSet<Craft> newCraftList = new HashSet<Craft>();

        for (Craft craft : getCraftList().values())
        {
            if (craft.getWorld().equals(center.getWorld().getUID())) {
                Location newLoc = craft.getCraftDesign().getHullBlocks(craft).get(0);
                Vector3 box = BukkitAdapter.asVector(newLoc.subtract(center));
                if (craft.getWorld().equals(center.getWorld().getUID()) && Math.abs(box.getX()) < lengthX / 2 && Math.abs(box.getY()) < lengthY / 2 && Math.abs(box.getZ()) < lengthZ / 2)
                    newCraftList.add(craft);
            }
        }
        return newCraftList;
    }

    public void claimCraftsInBox(Location center, UUID owner){
        int halflength = 60;
        for (int x = halflength; x >= -halflength; x--) {
            for (int y = halflength; y >= -halflength; y--) {
                for (int z = halflength; z >= -halflength; z--) {
                    getCraft(center.clone().add(x, y, z), owner);
                }
            }
        }
    }

    /**
     * returns all ironclad for a list of locations
     * @param locations - a list of location to search for ironclad
     * @return - list of all ironclad in this sphere
     */
    public static HashSet<Craft> getCraftsByLocations(List<Location> locations)
    {
        HashSet<Craft> newCraftList = new HashSet<Craft>();
        for (Craft craft : getCraftList().values())
        {
            for (Location loc : locations)
            {
                if (craft.isCraftBlock(loc.getBlock()))
                    newCraftList.add(craft);
            }

        }
        return newCraftList;
    }

    /**
     * returns all ironclad for a list of locations - this will update all locations
     * @param locations - a list of location to search for ironclad
     * @param player - player which operates the craft
     * @param silent - no messages will be displayed if silent is true
     * @return - list of all ironclad in this sphere
     */
    public HashSet<Craft> getCrafts(List<Location> locations, UUID player, boolean silent)
    {
        HashSet<Craft> newCraftList = new HashSet<Craft>();
        for (Location loc : locations)
        {
            Craft newCraft = getCraft(loc, player, silent);
            if (newCraft != null)
            {
                newCraftList.add(newCraft);
            }
        }

        return newCraftList;
    }

	/**
	 * get craft by craftName and Owner - used for Signs
	 * @param craftName name of the craft
	 * @return the craft with this name
	 */
	public static Craft getCraft(String craftName)
	{
		if (craftName == null) return null;

        UUID uid = craftNameMap.get(craftName);
        if (uid != null)
		    return craftList.get(uid);

        //craft not found
        return null;
	}

	/**
	 * Searches the storage if there is already a craftblock on this location
	 * and returns the craft
	 * @param loc location of one craft block
	 * @return the craft at this location
	 */
	private Craft getCraftFromStorage(Location loc)
	{
		for (Craft craft : craftList.values())
		{
			if (/*:*/loc.distance(BukkitAdapter.adapt(craft.getWorldBukkit(), craft.getOffset())) <= 64 /*To make code faster on servers with a lot of ironclad */ && craft.isCraftBlock(loc.getBlock()))
			{
				return craft;
			}
		}
		return null;
	}

	/**
	 * searches for a craft and creates a new entry if it does not exist
	 * @param craftBlock - one block of the craft
	 * @param owner - the owner of the craft (important for message notification). Can't be null if a new craft is created
	 * @return the craft at this location
	 */
	public Craft getCraft(Location craftBlock, UUID owner)
	{
		return getCraft(craftBlock, owner, false);
	}
	
	/**
	 * searches for a craft and creates a new entry if it does not exist
	 * 
	 * @param craftBlock - one block of the craft
	 * @param owner - the owner of the craft (important for message notification). Can't be null
	 * @return the craft at this location
	 */
	public Craft getCraft(Location craftBlock, UUID owner, boolean silent)
	{
        // is this block material used for a craft design
        if (craftBlock.getBlock() == null || !plugin.getDesignStorage().isCraftBlockMaterial(craftBlock.getBlock().getBlockData().getMaterial()))
            return null;

        long startTime = System.nanoTime();

        //check if there is a craft at this location
        Craft craft = checkCraft(craftBlock, owner);

        //if there is no craft, exit
        if (craft == null)
            return null;

        // search craft that is written on the sign
        Craft craftFromSign = getCraft(craft.getCraftNameFromSign());

        // if there is a different name on the craft sign we use that one
        if (craftFromSign != null)
        {
            plugin.logDebug("use entry from craft sign");
            //update the position of the craft
            craftFromSign.setCraftDirection(craft.getCraftDirection());
            craftFromSign.setOffset(craft.getOffset());
            //use the updated object from the storage
            craft = craftFromSign;
        }
        else
        {
            // this craft has no sign, so look in the database if there is something
            Craft storageCraft =  getCraftFromStorage(craftBlock);
            if (storageCraft != null)
            {
                //try to find something in the storage
                plugin.logDebug("craft found in storage");
                craft = storageCraft;
            }
            //nothing in the storage, so we make a new entry
            else
            {
                //search for a player, because owner == null is not valid
                if (owner == null)
                    return null;
                Player player = Bukkit.getPlayer(owner);

                //can this player can build one more craft
                MessageEnum	message = canBuildCraft(craft, owner);

                //if a sign is required to operate the craft, there must be at least one sign
                if (message == MessageEnum.CraftCreated && (craft.getCraftDesign().isSignRequired() && !craft.hasCraftSign()))
                    message = MessageEnum.ErrorMissingSign;

                CraftBeforeCreateEvent cbceEvent = new CraftBeforeCreateEvent(craft, message, player.getUniqueId());
                Bukkit.getServer().getPluginManager().callEvent(cbceEvent);

                //add craft to the list if everything was fine and return the craft
                if (!cbceEvent.isCancelled() && cbceEvent.getMessage() != null && cbceEvent.getMessage() == MessageEnum.CraftCreated)
                {
                    plugin.logDebug("a new craft was created by " + craft.getOwner());
                    createCraft(craft, true);

                    //send messages
                    if (!silent)
                    {
                        userMessages.sendMessage(message, owner, craft);
                        IroncladUtil.playSound(craft.getMuzzle(), craft.getCraftDesign().getSoundCreate());
                    }
                    CraftAfterCreateEvent caceEvent = new CraftAfterCreateEvent(craft, player.getUniqueId());
                	Bukkit.getServer().getPluginManager().callEvent(caceEvent);
                }
                else
                {
                    //send messages
                    if (!silent)
                    {
                        userMessages.sendMessage(message, player, craft);
                        IroncladUtil.playErrorSound(craft.getMuzzle());
                    }


                    plugin.logDebug("Creating a craft event was canceled: " + message);
                    return null;
                }
            }
        }

        plugin.logDebug("Time to find craft: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");

        return craft;
	}

    /**
     * returns the craft from the storage
     * @param uid UUID of the craft
     * @return the craft from the storage
     */
    public static Craft getCraft(UUID uid)
    {
        if (uid == null)
            return null;

        return craftList.get(uid);
    }

	/**
	 * searches if this block is part of a craft and create a new one
	 * @param craftBlock block of the craft
	 * @param owner the player who will be the owner of the craft if it is a new craft
	 * @return craft if found, else null
	 */
    private Craft checkCraft(Location craftBlock, UUID owner)
	{

	    // is this block material used for a craft design
        if (craftBlock.getBlock() == null || !plugin.getDesignStorage().isCraftBlockMaterial(craftBlock.getBlock().getBlockData().getMaterial()))
            return null;

		World world = craftBlock.getWorld();

		// check all craft design if this block is part of the design
		for (CraftDesign craftDesign : plugin.getDesignStorage().getCraftsDesignList()) {
			// check of all directions
			BlockFace craftDirection = BlockFace.NORTH;
			for (int i = 0; i < 4; i++) {
				// for all blocks for the design
				List<SimpleBlock> designBlockList = craftDesign.getAllCraftBlocks(craftDirection);
                //check for empty entries
                if (designBlockList.size() == 0) {
                    plugin.logSevere("There are empty craft design schematics in your design folder. Please check it.");
                    return null;
                }
				for (SimpleBlock designBlock : designBlockList) {
					// compare blocks
					if (designBlock.compareMaterialAndFacing(craftBlock.getBlock().getBlockData())) {
						// this block is same as in the design, get the offset
						BlockVector3 offset = designBlock.subtractInverted(craftBlock).toVector();

						// check all other blocks of the craft
						boolean isCraft = true;

						for (SimpleBlock checkBlocks : designBlockList) {
							if (!checkBlocks.compareMaterialAndFacing(world, offset)) {
								// if the block does not match this is not the
								// right one
								isCraft = false;
								break;
							}
						}

						// this is a craft
						if (isCraft) {
                           // craft
							return new Craft(craftDesign, world.getUID(), offset, craftDirection, owner);
						}
					}
				}
				// rotate craft direction
				craftDirection = IroncladUtil.roatateFace(craftDirection);
			}
		}
		return null;
	}

	/**
	 * returns the number of owned ironclad of a player
	 * @param player the owner of the ironclad
	 * @return number of ironclad
	 */
	public int getNumberOfCrafts(UUID player)
	{
		int i = 0;
		for (Craft craft : craftList.values())
		{
			if (craft.getOwner() == null)
			{
				plugin.logSevere("Craft has no owner. Contact the plugin developer");
			}
			else if (craft.getOwner().equals(player))
			{
				i++;
			}
		}
		return i;
	}

	/**
	 * 
	 * @return List of ironclad
	 */
	public static ConcurrentHashMap<UUID, Craft> getCraftList()
	{
		return craftList;
	}
	
	/**
	 * List of ironclad
	 */
	public void clearCraftList()
	{
		craftList.clear();
	}

	/**
	 * returns the number of ironclad manged by the plugin
	 * @return number of ironclad in all world
	 */
	public int getCraftListSize()
	{
		return craftList.size();
	}

	/**
	 * returns the amount of ironclad a player can build
	 * 
	 * @param player check the number of craft this player can build
	 * @return the maximum number of ironclad
	 */
    public int getCraftBuiltLimit(Player player)
	{
		// the player is not valid - no limit check
		if (player == null) return Integer.MAX_VALUE;

		// both limitA/B and ironclad.limit.5 work
		// if all notes are enabled, set limit to a high number. If no permission plugin is loaded, everything is enabled

        int newBuiltLimit = getNewBuildLimit(player);

		// config implementation
		if (config.isBuildLimitEnabled())
		{
            plugin.logDebug("BuildLimit: limitA and limitB are enabled");
			if (player.hasPermission("ironclad.limit.limitB") && (newBuiltLimit > config.getBuildLimitB()))
			{
				// return the
                plugin.logDebug("build limitB sets the number of ironclad to: " + config.getBuildLimitB());
				return config.getBuildLimitB();
			}
			// limit B is stronger
			else if (player.hasPermission("ironclad.limit.limitA") && (newBuiltLimit > config.getBuildLimitA()))
			{
                plugin.logDebug("build limitA sets the number of ironclad to: " + config.getBuildLimitA());
				return config.getBuildLimitA();
			}
		}
		// player implementation
		if (newBuiltLimit >= 0)
        {
            plugin.logDebug("permission build limit sets the maximum number of ironclad to: " + newBuiltLimit);
            return newBuiltLimit;
		}
        plugin.logDebug("no build limit found. Setting to max value.");
        return Integer.MAX_VALUE;
	}

    /**
     * returns the build limit for the player given by ironclad.limit.5
     * @param player the build limit for this player
     * @return how many cannosn this player can build
     */
    public int getNewBuildLimit(Player player)
    {
        if (player == null) return -1;

        if (player.hasPermission("ironclad.limit." + Integer.MAX_VALUE))
        {
            //all nodes are enabled
            plugin.logDebug("BuildLimit: all entries for ironclad.limit.x are TRUE. Returning max build limit.");
            return Integer.MAX_VALUE;
        }
        else
        {
            // else check all nodes for the player
            for (int i = 100; i >= 0; i--)
            {
                if (player.hasPermission("ironclad.limit." + i))
                {
                    plugin.logDebug("BuildLimit: entry for ironclad.limit."+i+" found");
                    return i;
                }
            }
        }
        //no build limit found
        plugin.logDebug("BuildLimit: no entry for ironclad.limit.x found");
        return Integer.MAX_VALUE;
    }
	/**
	 * checks if the player can build a craft (permission, builtLimit)
	 * @param craft
	 * @param owner
	 * @return
	 */
	private MessageEnum canBuildCraft(Craft craft, UUID owner)
	{
		CraftDesign design = craft.getCraftDesign();
		
		//get the player from the server
		if (owner == null) return null;
		Player player = Bukkit.getPlayer(owner);
		if (player == null) return null;
	
		// check if player has permission to build
		if (!player.hasPermission(design.getPermissionBuild()))
		{
			return MessageEnum.PermissionErrorBuild;
		}
		// player does not have too many guns
		if (getNumberOfCrafts(owner) >= getCraftBuiltLimit(player))
		{
			return MessageEnum.ErrorCraftBuiltLimit;
		}
		// player has sufficient permission to build a craft
		return MessageEnum.CraftCreated;
	}

    /**
     * removes all craft
     */
    public void deleteAllCrafts()
    {
        Iterator<Craft> iter = craftList.values().iterator();

        while (iter.hasNext())
        {
            Craft craft = iter.next();
            OfflinePlayer offplayer = Bukkit.getOfflinePlayer(craft.getOwner());
            // return money to the player if the craft was paid
            if (offplayer != null && offplayer.hasPlayedBefore() && plugin.getEconomy() != null) {
                if (craft.isPaid())
                    plugin.getEconomy().depositPlayer(offplayer, craft.getCraftDesign().getEconomyBuildingCost());
            }
            craft.destroyCraft(false, false, BreakCause.Other);
            iter.remove();
        }
    }


	/**
	 * Deletes all ironclad of this player in the database to reset the craft limit
	 * @param owner the owner of the craft
     * @return returns true if there was an entry of this player in the list
	 */
	public boolean deleteCrafts(UUID owner)
	{
		Iterator<Craft> iter = craftList.values().iterator();
        boolean inList = false;

		while (iter.hasNext())
		{
			Craft next = iter.next();
			if (next.getOwner() != null && next.getOwner().equals(owner))
			{
                inList = true;
				next.destroyCraft(false, false, BreakCause.Other);
				iter.remove();
			}
		}
        return inList;
	}

    /**
     * reloads designs from the design list and updates all entries in the craft
     */
    public void updateCrafts()
    {
        for (Craft craft : craftList.values())
        {
            craft.setCraftDesign(plugin.getCraftDesign(craft));
        }
    }




}