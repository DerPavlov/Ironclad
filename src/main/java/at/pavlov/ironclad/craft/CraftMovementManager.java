package at.pavlov.ironclad.craft;

import at.pavlov.ironclad.Enum.MessageEnum;
import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.config.Config;
import at.pavlov.ironclad.config.UserMessages;
import at.pavlov.ironclad.container.SimpleBlock;
import at.pavlov.ironclad.container.SimpleEntity;
import at.pavlov.ironclad.utils.IroncladUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

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
                if  (time > 1.)
                    plugin.logDebug("Time update craft movement: " + new DecimalFormat("0.00").format(time) + "ms");
            }
        }, 1L, 1L);
    }

    private void updateCraftMovement(){
        if (asyncTask != null)
            return;

        long startTimeFull = System.nanoTime();
        long startTime = System.nanoTime();

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

        //Worldedit
        BlockVector3 dim = craft.getCraftDimensions();
        plugin.logDebug("craft dimensions " + dim);

        craft.setLastMoved(System.currentTimeMillis());
        craft.setProcessing(true);


        //calculate the travel vector
        craft.updateTravelVector();

        plugin.logDebug("Time update pre calc movement: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
        startTime = System.nanoTime();


        World bworld = craft.getWorldBukkit();
        EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(bworld), -1);
        editSession.setFastMode(true);

        HashSet<BlockVector3> overwrittenBlocks = new HashSet<>();
        ArrayList<SimpleBlock> updateBlocks = new ArrayList<>();
        ArrayList<BlockVector3> oldBlocks = new ArrayList<>();
        boolean successful = true;
        BlockVector3 targetLoc;
        Block targetBlock;
        Block oldBlock = null;

        plugin.logDebug("Time update init movement: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
        startTime = System.nanoTime();

        craft.getCraftDesign().getAllCraftBlocks(craft);

        plugin.logDebug("Time update get all craft blocks: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
        startTime = System.nanoTime();

        BlockVector3 cmin = craft.getCraftMinBoundingBox();
        BlockVector3 cmax = craft.getCraftMaxBoundingBox();
        plugin.logDebug("cmin " + cmin + " cmax " + cmax);

        BlockVector3 cdim = craft.getCraftDimensions();
        int cxmin = cmin.getBlockX()>>4;
        int czmin = cmin.getBlockZ()>>4;
        int cxmax = cmax.getBlockX()>>4;
        int czmax = cmax.getBlockZ()>>4;
        int chunkX = cxmax - cxmin;
        int chunkZ = czmax - czmin;

        ChunkSnapshot[][] snapshots = new ChunkSnapshot[chunkX+1][chunkZ+1];
        plugin.logDebug("loading chunks: " + (chunkX+1) + "*" +(chunkZ+1) + " cxmin: " + cxmin + " czmin: " + czmin + " cxmax: " + cxmax + " czmax: " + czmax);
        for (int chX = 0; chX <= chunkX; chX ++){
            for (int chZ = 0; chZ <= chunkZ; chZ++){
                snapshots[chX][chZ] = bworld.getChunkAt((cxmin + chX)<<4, (czmin + chZ)<<4).getChunkSnapshot();
            }
        }

        plugin.logDebug("Time update capture snapshots: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
        startTime = System.nanoTime();

        BlockData blockData = null;
        int sx;
        int sz;
        for (SimpleBlock designBlock : craft.getCraftDesign().getAllCraftBlocks(craft)) {
            sx = designBlock.getLocX()-(cxmin<<4);
            sz = designBlock.getLocZ()-(czmin<<4);
            ChunkSnapshot snapshot = snapshots[sx>>4][sz>>4];
            blockData = snapshot.getBlockData(sx%16, designBlock.getLocY(), sz%16);
        }
        plugin.logDebug("blockdata chunk: " + blockData);

        plugin.logDebug("Time update snapshot get blocks: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
        startTime = System.nanoTime();

        for (SimpleBlock designBlock : craft.getCraftDesign().getAllCraftBlocks(craft)) {
            oldBlock = bworld.getBlockAt(designBlock.getLocX(), designBlock.getLocY(), designBlock.getLocZ());
        }

        plugin.logDebug("oldblock block: " + oldBlock);
        plugin.logDebug("Time update world get blocks: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
        startTime = System.nanoTime();

        //perform craft calculations
        for (SimpleBlock designBlock : craft.getCraftDesign().getAllCraftBlocks(craft)) {
            oldBlock = bworld.getBlockAt(designBlock.getLocX(), designBlock.getLocY(), designBlock.getLocZ());
            //Ironclad.getPlugin().logDebug("old block " + oldBlock);
            if (oldBlock.isEmpty() || oldBlock.getBlockData() instanceof Levelled) {
                Ironclad.getPlugin().logDebug("Found destroyed craft block " + oldBlock);
            } else {
                // move the craft to the new location. oldblock was updated to the new location
                targetLoc = craft.transformToFutureLocation(designBlock.toVector());

                targetBlock = bworld.getBlockAt(targetLoc.getBlockX(), targetLoc.getBlockX(), targetLoc.getBlockZ());
                if (targetBlock == null) {
                    //Ironclad.getPlugin().logDebug("target block " + targetLoc + " does not exist in snapshot");
                    continue;
                }

                // target block should be Air or a liquid
                if (!craft.isLocationPartOfCraft(targetLoc) && !(targetBlock.isEmpty() || targetBlock.getBlockData() instanceof Levelled)) {
                    Ironclad.getPlugin().logDebug("Found blocking block at" + targetBlock);
                    successful = false;
                    break;
                }
                //old blocks of the craft
                oldBlocks.add(designBlock.toVector());
                //just update blocks which are not the same
                if (!targetBlock.getBlockData().equals(oldBlock.getBlockData())) {
                    //Ironclad.getPlugin().logDebug("block needs update " + targetBlock);
                    updateBlocks.add(new SimpleBlock(targetLoc, oldBlock.getBlockData()));
                    //blocks that are overwritten by a new block
                    overwrittenBlocks.add(targetLoc);
                }
            }
        }

        plugin.logDebug("Time update search blocks: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");

        if (successful) {

            startTime = System.nanoTime();

            BlockState airState = BlockTypes.VOID_AIR.getDefaultState();
            for (BlockVector3 vector3 : oldBlocks)
                if(!overwrittenBlocks.contains(vector3))
                    editSession.smartSetBlock(vector3, airState);

            plugin.logDebug("Time update write air blocks: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
            startTime = System.nanoTime();

            for (SimpleBlock uBlock : updateBlocks) {
                editSession.smartSetBlock(uBlock.toVector(), BukkitAdapter.adapt(uBlock.getBlockData()));
            }

            plugin.logDebug("Time update write ship blocks: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
            startTime = System.nanoTime();

            // All changes will have been made once flushQueue is called
            editSession.flushSession();

            plugin.logDebug("Time update flush ship blocks: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
        }

        craft.movementPerformed();

        plugin.logDebug("Time update move blocks: " + new DecimalFormat("0.00").format((System.nanoTime() - startTimeFull)/1000000.0) + "ms");


        //plugin.logDebug("--- Final Craft Offset: " + craft.getOffset() + " Facing " + craft.getCraftDirection());

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
     * @param player - player in piloting mode
     * @param craft - operated craft
     */
    public void pilotingMode(Player player, Craft craft)
    {
        if (player == null)
            return;

        boolean isPilotMode = inPilotingMode.containsKey(player.getUniqueId());
        if (isPilotMode) {
            if (craft == null)
                craft = getCraftInPilotingMode(player);
        }
        //enable piloting mode. Sentry cannons can't be operated by players
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
        Craft cannon = getCraftInPilotingMode(player);
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
     * returns the craft of the player if he is in piloting mode
     * @param player the player who is in piloting mode
     * @return the craft which is in piloting mode by the given player
     */
    public Craft getCraftInPilotingMode(Player player)
    {
        if (player == null)
            return null;
        //return the craft of the player if he is in piloting mode
        return CraftManager.getCraft(inPilotingMode.get(player.getUniqueId()));
    }
}
