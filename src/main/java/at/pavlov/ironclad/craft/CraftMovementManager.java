package at.pavlov.ironclad.craft;

import at.pavlov.ironclad.Enum.MessageEnum;
import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.config.Config;
import at.pavlov.ironclad.config.UserMessages;
import at.pavlov.ironclad.container.SimpleBlock;
import at.pavlov.ironclad.container.SimpleEntity;
import at.pavlov.ironclad.scheduler.MoveCalculateTask;
import at.pavlov.ironclad.scheduler.MoveCraftTask;
import at.pavlov.ironclad.utils.IroncladUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import sun.java2d.pipe.SpanShapeRenderer;

import java.text.DecimalFormat;
import java.util.*;


public class CraftMovementManager {
    private final Ironclad plugin;
    private final UserMessages userMessages;
    private final Config config;

    private BukkitTask asyncTask;

    //<Player,craft name>
    private HashMap<UUID, UUID> inPilotingMode = new HashMap<UUID, UUID>();
    //<craft uid, timespamp>
    private HashMap<UUID, Long> lastInteraction = new HashMap<UUID, Long>();
    //attached Blocks will be moved with the craft once the movement is calculated
    private ArrayList<Location> attachedBlocks = new ArrayList<>();



    /**
     * Constructor
     * @param plugin Ironclad main class
     */
	public CraftMovementManager(Ironclad plugin) {
        this.plugin = plugin;
        this.config = plugin.getMyConfig();
        this.userMessages = plugin.getMyConfig().getUserMessages();

        asyncTask = null;
    }

    /**
     * starts the scheduler which moves the crafts in minecraft
     */
    public void initMovementExecute()
    {
        //changing angles for aiming mode
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
        {
            public void run()
            {
                long startTime = System.nanoTime();
                updateCraftMovement();
                double time = (System.nanoTime() - startTime)/1000000.0;
                if  (time > 5.)
                    plugin.logDebug("Time update craft movement: " + new DecimalFormat("0.00").format(time) + "ms");
            }
        }, 1L, 1L);
    }

    private void updateCraftMovement(){
        if (asyncTask != null)
            return;
        //search for the craft which is waiting the longest
        long waiting = System.currentTimeMillis();
        Craft craft = null;
        for (Craft fcraft : CraftManager.getCraftList().values()) {
            if (fcraft.isMoving() && System.currentTimeMillis() > fcraft.getLastMoved() + 10000 && fcraft.getLastMoved() < waiting )  {
                waiting = fcraft.getLastMoved();
                craft = fcraft;
            }
        }

        //no suitable craft found
        if (craft == null)
            return;

        craft.setLastMoved(System.currentTimeMillis());
        craft.setProcessing(true);

        //Worldedit
        BlockVector3 dim = craft.getCraftDimensions();
        BlockVector3 travel = craft.getFutureCraftOffset();
        int x = dim.getX() + Math.abs(travel.getX()) + 1;
        int y = dim.getY() + Math.abs(travel.getY()) + 1;
        int z = dim.getZ() + Math.abs(travel.getZ()) + 1;
        plugin.logDebug("array dimensions " + x + "," + y + "," + z);

        // If no player is doing the changes
        EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(WorldEdit.getInstance().get, -1);
        // For a specific player
        editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(worldEditWorld, -1, actor);

        // Do stuff with the EditSession
        editSession.setBlock(new Vector(x, y, z), new BaseBlock(id, data));
// All changes will have been made once flushQueue is called
        editSession.flushQueue();


        /*
        Craft craftClone = craft.clone();

        HashSet<Vector> overwrittenBlocks = new HashSet<>();

        boolean successful = true;
        Vector targetLoc;
        SimpleBlock targetBlock;

        //perform craft calculations
        for (SimpleBlock designBlock : craftClone.getCraftDesign().getAllCraftBlocks(craftClone)){
            SimpleBlock oldBlock = blockSnapshot.get(designBlock.toVector());
            Ironclad.getPlugin().logDebug("old block " + oldBlock);
            if (oldBlock.getMaterial() == Material.AIR || oldBlock.getBlockData() instanceof Levelled){
                Ironclad.getPlugin().logDebug("Found destroyed craft block " + oldBlock);
            }
            else{
                // move the craft to the new location. oldblock was updated to the new location
                targetLoc = craftClone.transformToFutureLocation(designBlock.toVector());
                //remove decimal places to the the correct block location
                targetLoc.setX(Math.floor(targetLoc.getX()));
                targetLoc.setY(Math.floor(targetLoc.getY()));
                targetLoc.setZ(Math.floor(targetLoc.getX()));
                targetBlock = blockSnapshot.get(targetLoc);
                if (targetBlock == null){
                    Ironclad.getPlugin().logDebug("target block " + targetLoc + " does not exist in snapshot");
                    continue;
                }

                Ironclad.getPlugin().logDebug("target block " + targetBlock + !craftClone.isLocationPartOfCraft(targetLoc));
                // target block should be Air or a liquid
                if (!craftClone.isLocationPartOfCraft(targetLoc) && !(targetBlock.getMaterial() == Material.AIR || targetBlock.getBlockData() instanceof Levelled)){
                    Ironclad.getPlugin().logDebug("Found blocking block at" + targetBlock);
                    successful = false;
                    break;
                }
                overwrittenBlocks.add(targetLoc);
                //just update blocks which are not the same
                if (!targetBlock.getBlockData().equals(oldBlock.getBlockData())) {
                    Ironclad.getPlugin().logDebug("block needs update " + targetBlock);
                    if (targetBlock.getBlockData() instanceof Directional)
                        newAttachedBlocks.add(targetBlock);
                    else
                        newBlocks.add(targetBlock);
                }
            }*/

        //found a craft, move it

//        SimpleBlock[][][] blockSnapshot = new SimpleBlock[x][y][z];
//
//        //collect the block which should be moved
//        World world = craft.getWorldBukkit();
//        IntVector offbox = craft.getCraftDesign().getArrayOffset(craft);
//        plugin.logDebug("offbox " + offbox);
//        for (SimpleBlock sblock : craft.getCraftDesign().getAllCraftBlocks(craft)){
//            SimpleBlock snapblock = new SimpleBlock(sblock.toLocation(world).getBlock());
//            plugin.logDebug("start block " + snapblock);
//            snapblock.subtract_noCopy(offbox);
//            plugin.logDebug("snapblock " + snapblock);
//            blockSnapshot[snapblock.getLocX()][snapblock.getLocY()][snapblock.getLocZ()] = snapblock;
//        }
//
//        //add the blocks the craft should be moved
//        for (Location loc : craft.getCraftDesign().getAllCraftBlocksAfterMovement(craft)){
//            SimpleBlock snapblock = new SimpleBlock(loc.getBlock());
//            plugin.logDebug("future block " + snapblock);
//            plugin.logDebug("future blo " + snapblock.toVector());
//            plugin.logDebug("future loc " + loc.toVector());
//            // add if block not exist already
//            snapblock.subtract_noCopy(offbox);
//            plugin.logDebug("snapblock " + snapblock);
//            blockSnapshot[snapblock.getLocX()][snapblock.getLocY()][snapblock.getLocZ()] = snapblock;
//        }


        Map<Vector, SimpleBlock> blockSnapshot1 = new HashMap<>();
        //Async calculate craft movement
        //asyncTask = new MoveCalculateTask(craft.clone(),  blockSnapshot1, craft.getSimpleEntitiesOnShip()).runTaskAsynchronously(plugin);*/
    }


    public void performCraftMovement(Craft craftClone, List<SimpleBlock> newBlocks, List<SimpleBlock> newAttachedBlocks, List<SimpleBlock> resetBlocks, List<SimpleBlock> resetAttachedBlocks, Set<SimpleEntity> entities, boolean successful){
        asyncTask = null;
        Craft craft = CraftManager.getCraft(craftClone.getUID());
        // not successful since target desitination was blocked
        if (!successful) {
            plugin.logDebug("Could not move craft " + craftClone.getCraftName() + " since target location was blocked");
            craft.setVelocity(0);
            return;
        }

        plugin.logDebug("performCraftMovement " + newBlocks.size());

        long startTime = System.nanoTime();
        World world = craft.getWorldBukkit();

        //remove attached blocks
        for (SimpleBlock rBlock : resetAttachedBlocks) {
            plugin.logDebug("performCraft attached block remove " + rBlock);
            Block wBlock = rBlock.toLocation(world).getBlock();
            wBlock.setBlockData(rBlock.getBlockData());
        }

        //update blocks
        for (SimpleBlock cBlock : newBlocks) {
            plugin.logDebug("performCraft block update " + cBlock);
            Block wBlock = cBlock.toLocation(world).getBlock();
            wBlock.setBlockData(cBlock.getBlockData());
        }
        //place the attachable blocks
        for (SimpleBlock aBlock : newAttachedBlocks){
            plugin.logDebug("performCraft attached block update " + aBlock);
            Block wBlock = aBlock.toLocation(world).getBlock();
            wBlock.setBlockData(aBlock.getBlockData());
        }

        //remove blocks
        for (SimpleBlock rBlock : resetBlocks) {
            plugin.logDebug("performCraft block remove " + rBlock);
            Block wBlock = rBlock.toLocation(world).getBlock();
            wBlock.setBlockData(rBlock.getBlockData());
        }

        plugin.logDebug("Time move craft: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
        startTime = System.nanoTime();

        //teleport the entities
        for (SimpleEntity entity : entities){
            Entity bEntity = Bukkit.getEntity(entity.getUuid());
            bEntity.getLocation().add(entity.getLocationUpate());
            bEntity.getLocation().setYaw(entity.getYawUpdate());
        }

        //movement finished
        craft.movementPerformed();
        plugin.logDebug("Time teleportation: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
    }

    public void moveCraft(BlockFace blockFace){

    }

    /**
     *
     */
    public void moveCraft(){

    }

    /**
     * moves the craft in the direction of the given vector
     * @param movement vector of movement
     */
    public void moveCraft(Vector movement){


    }

    /**
     * switches aming mode for this craft
     * @param player - player in aiming mode
     * @param craft - operated craft
     */
    public void pilotingMode(Player player, Craft craft)
    {
        if (player == null)
            return;

        boolean isPilotMode = inPilotingMode.containsKey(player.getUniqueId());
        if (isPilotMode) {
            if (craft == null)
                craft = getCraftInAimingMode(player);
        }
        //enable aiming mode. Sentry cannons can't be operated by players
        else if(craft != null) {
            //check if player has permission to aim
            if (player.hasPermission(craft.getCraftDesign().getPermissionPiloting()))
            {
                //check if pilot is on the craft
                if (craft.isEntityOnShip(player)) {
                    MessageEnum message = enablePilotingMode(player, craft);
                    userMessages.sendMessage(message, player, craft);
                }
                else {
                    userMessages.sendMessage(MessageEnum.PilotingModeTooFarAway, player, craft);
                }

            }
            else {
                //no Permission to pilot
                userMessages.sendMessage(MessageEnum.PermissionErrorPilot, player, craft);
            }
        }
    }

    /**
     * enable the aiming mode
     * @param player player how operates the craft
     * @param craft the craft in piloting mode
     * @return message for the user
     */
    public MessageEnum enablePilotingMode(Player player, Craft craft)
    {
        if (player == null)
            return null;

        if (!player.hasPermission(craft.getCraftDesign().getPermissionPiloting()))
            return MessageEnum.PermissionErrorPilot;

        inPilotingMode.put(player.getUniqueId(), craft.getUID());

        IroncladUtil.playSound(player.getEyeLocation(), craft.getCraftDesign().getSoundEnablePilotingMode());

        return MessageEnum.PilotingModeEnabled;

    }


    /**
     * disables the aiming mode for this player
     * @param player - player in aiming mode
     * @return message for the player
     */
    public MessageEnum disableAimingMode(Player player)
    {
        //player.playSound(player.getEyeLocation(), Sound.MINECART_BASE, 0.25f, 0.75f);
        Craft cannon = getCraftInAimingMode(player);
        if (cannon!=null)
            IroncladUtil.playSound(player.getEyeLocation(), cannon.getCraftDesign().getSoundDisablePilotingMode());
        return disableAimingMode(player, cannon);
    }

    /**
     * disables the aiming mode for this player
     * @param player player in aiming mode
     * @param craft operated craft
     * @return message for the player
     */
    public MessageEnum disableAimingMode(Player player, Craft craft)
    {
        if (player == null)
            return null;

        if (inPilotingMode.containsKey(player.getUniqueId()))
        {
            //player in map -> remove
            inPilotingMode.remove(player.getUniqueId());

            return MessageEnum.PilotingModeDisabled;
        }
        return null;
    }


	public boolean isInPilotingMode(UUID player) {
		return player != null && inPilotingMode.containsKey(player);
	}

    /**
     * returns the craft of the player if he is in aiming mode
     * @param player the player who is in aiming mode
     * @return the craft which is in aiming mode by the given player
     */
    public Craft getCraftInAimingMode(Player player)
    {
        if (player == null)
            return null;
        //return the craft of the player if he is in aiming mode
        return CraftManager.getCraft(inPilotingMode.get(player.getUniqueId()));
    }
}
