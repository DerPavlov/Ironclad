package at.pavlov.ironclad.craft;

import at.pavlov.ironclad.Enum.FakeBlockType;
import at.pavlov.ironclad.Enum.InteractAction;
import at.pavlov.ironclad.Enum.MessageEnum;
import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.config.Config;
import at.pavlov.ironclad.config.UserMessages;
import at.pavlov.ironclad.container.SimpleBlock;
import at.pavlov.ironclad.container.SimpleEntity;
import at.pavlov.ironclad.event.CraftUseEvent;
import at.pavlov.ironclad.utils.IroncladUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.*;


public class CraftMovementManager {

    private class CraftAngles
    {
        private double horizontal;
        private double vertical;

        public CraftAngles(double deltaYaw, double deltaPitch)
        {
            this.setHorizontal(deltaYaw);
            this.setVertical(deltaPitch);
        }

        public double getHorizontal() {
            return horizontal;
        }

        public void setHorizontal(double horizontal) {
            this.horizontal = horizontal;
        }

        public double getVertical() {
            return vertical;
        }

        public void setVertical(double vertical) {
            this.vertical = vertical;
        }
    }
    
    private final Ironclad plugin;
    private final UserMessages userMessages;
    private final Config config;

    private BukkitTask asyncTask;

    //<Player,craft name>
    private HashMap<UUID, UUID> inCruisingMode = new HashMap<UUID, UUID>();
    //attached Blocks will be moved with the craft once the movement is calculated
    private ArrayList<Location> attachedBlocks = new ArrayList<>();
    //<Player>
    private HashSet<UUID> imitatedEffectsOff = new HashSet<UUID>();



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
        //changing angles for cruising mode
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
        {
            public void run()
            {
                //long startTime = System.nanoTime();
                updateCruisingMode();
                updateCraftMovement();
                //double time = (System.nanoTime() - startTime)/1000000.0;
                //if  (time > 1.)
                //    plugin.logDebug("Time update craft movement: " + new DecimalFormat("0.00").format(time) + "ms");
            }
        }, 1L, 40L);
    }

    private void updateCraftMovement(){
        if (asyncTask != null)
            return;

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
        //BlockVector3 dim = craft.getCraftDimensions();
        //plugin.logDebug("craft dimensions " + dim);

        craft.setLastMoved(System.currentTimeMillis());
        craft.setProcessing(true);


        //calculate the travel vector
        craft.updateCruisingVector();

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

        //#####################  Get blocks ###################
        startTime = System.nanoTime();
        HashMap<BlockVector3, BlockStateHolder> blockmap = new HashMap<>();
        for (SimpleBlock designBlock : craft.getCraftDesign().getAllCraftBlocks(craft)) {
            oldBlock = bworld.getBlockAt(designBlock.getLocX(), designBlock.getLocY(), designBlock.getLocZ());
            blockmap.put(BukkitAdapter.asBlockVector(oldBlock.getLocation()), BukkitAdapter.adapt(oldBlock.getBlockData()));
        }

        plugin.logDebug("Time update world get blocks: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");


        //plugin.logDebug("oldblock block: " + blockmap);


        long startTimeFull = System.nanoTime();
        startTime = System.nanoTime();

        //perform craft calculations
        for (SimpleBlock designBlock : craft.getCraftDesign().getAllCraftBlocks(craft)) {
//            oldBlock = bworld.getBlockAt(designBlock.getLocX(), designBlock.getLocY(), designBlock.getLocZ());
//            //Ironclad.getPlugin().logDebug("old block " + oldBlock);
//            if (oldBlock.isEmpty() || oldBlock.getBlockState() instanceof Levelled) {
//                Ironclad.getPlugin().logDebug("Found destroyed craft block " + oldBlock);
//            } else {
//                // move the craft to the new location. oldblock was updated to the new location
                targetLoc = craft.transformToFutureLocation(designBlock.toVector());
                targetBlock = bworld.getBlockAt(targetLoc.getBlockX(), targetLoc.getBlockX(), targetLoc.getBlockZ());
//                if (targetBlock == null) {
//                    //Ironclad.getPlugin().logDebug("target block " + targetLoc + " does not exist in snapshot");
//                    continue;
//                }
//
//                // target block should be Air or a liquid
//                if (!craft.isLocationPartOfCraft(targetLoc) && !(targetBlock.isEmpty() || targetBlock.getBlockState() instanceof Levelled)) {
//                    Ironclad.getPlugin().logDebug("Found blocking block at" + targetBlock);
//                    successful = false;
//                    break;
//                }
                //old blocks of the craft
                oldBlocks.add(designBlock.toVector());
                //just update blocks which are not the same
                if (!targetBlock.getBlockData().equals(designBlock.getBlockState())) {
                    //Ironclad.getPlugin().logDebug("block needs update " + targetBlock);
                    updateBlocks.add(new SimpleBlock(targetLoc, designBlock.getBlockState()));
                    //blocks that are overwritten by a new block
                    overwrittenBlocks.add(targetLoc);
                }
//            }
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
                editSession.smartSetBlock(uBlock.toVector(), uBlock.getBlockState());
            }

            plugin.logDebug("Time update write ship blocks: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
            startTime = System.nanoTime();

            // All changes will have been made once flushQueue is called
            editSession.flushSession();

            plugin.logDebug("Time update flush ship blocks: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
        }

        craft.movementPerformed();

        Bukkit.broadcastMessage("Time update " + craft.getCraftName() + ": " + new DecimalFormat("0.00").format((System.nanoTime() - startTimeFull)/1000000.0) + "ms");


        //plugin.logDebug("--- Final Craft Offset: " + craft.getOffset() + " Facing " + craft.getCraftDirection());

        //Async calculate craft movement
        //asyncTask = new MoveCalculateTask(craft.clone(),  blockSnapshot1, craft.getSimpleEntitiesOnShip()).runTaskAsynchronously(plugin);*/
    }

    private void chunksnapshots(Craft craft){
        World bworld = craft.getWorldBukkit();
        double startTime = System.nanoTime();

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
            //wBlock.setBlockData(rBlock.getBlockState());
        }

        //update blocks
        for (SimpleBlock cBlock : newBlocks) {
            plugin.logDebug("performCraft block update " + cBlock);
            Block wBlock = cBlock.toLocation(world).getBlock();
            //wBlock.setBlockData(cBlock.getBlockState());
        }
        //place the attachable blocks
        for (SimpleBlock aBlock : newAttachedBlocks){
            plugin.logDebug("performCraft attached block update " + aBlock);
            Block wBlock = aBlock.toLocation(world).getBlock();
            //wBlock.setBlockData(aBlock.getBlockState());
        }

        //remove blocks
        for (SimpleBlock rBlock : resetBlocks) {
            plugin.logDebug("performCraft block remove " + rBlock);
            Block wBlock = rBlock.toLocation(world).getBlock();
            //wBlock.setBlockData(rBlock.getBlockState());
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
     * @param player - player in cruising mode
     * @param craft - operated craft
     */
    public void cruisingMode(Player player, Craft craft)
    {
        if (player == null)
            return;

        boolean isPilotMode = inCruisingMode.containsKey(player.getUniqueId());
        if (isPilotMode) {
            if (craft == null)
                craft = getCraftInCruisingMode(player);
        }
        //enable cruising mode. Sentry crafts can't be operated by players
        else if(craft != null) {
            //check if player has permission to aim
            if (player.hasPermission(craft.getCraftDesign().getPermissionCruising()))
            {
                //check if pilot is on the craft
                if (craft.isEntityOnShip(player)) {
                    MessageEnum message = enableCruisingMode(player, craft);
                    userMessages.sendMessage(message, player, craft);
                }
                else {
                    userMessages.sendMessage(MessageEnum.CruisingModeTooFarAway, player, craft);
                }

            }
            else {
                //no Permission to pilot
                userMessages.sendMessage(MessageEnum.PermissionErrorPilot, player, craft);
            }
        }
    }

    /**
     * enable the cruising mode
     * @param player player how operates the craft
     * @param craft the craft in cruising mode
     * @return message for the user
     */
    public MessageEnum enableCruisingMode(Player player, Craft craft)
    {
        if (player == null)
            return null;

        if (!player.hasPermission(craft.getCraftDesign().getPermissionCruising()))
            return MessageEnum.PermissionErrorPilot;

        inCruisingMode.put(player.getUniqueId(), craft.getUID());

        IroncladUtil.playSound(player.getEyeLocation(), craft.getCraftDesign().getSoundEnableCruisingMode());

        return MessageEnum.CruisingModeEnabled;

    }


    /**
     * disables the cruising mode for this player
     * @param player - player in cruising mode
     * @return message for the player
     */
    public MessageEnum disableCruisingMode(Player player)
    {
        //player.playSound(player.getEyeLocation(), Sound.MINECART_BASE, 0.25f, 0.75f);
        Craft craft = getCraftInCruisingMode(player);
        if (craft!=null)
            IroncladUtil.playSound(player.getEyeLocation(), craft.getCraftDesign().getSoundDisableCruisingMode());
        return disableCruisingMode(player, craft);
    }

    /**
     * disables the cruising mode for this player
     * @param player player in cruising mode
     * @param craft operated craft
     * @return message for the player
     */
    public MessageEnum disableCruisingMode(Player player, Craft craft)
    {
        if (player == null)
            return null;

        if (inCruisingMode.containsKey(player.getUniqueId()))
        {
            //player in map -> remove
            inCruisingMode.remove(player.getUniqueId());

            return MessageEnum.CruisingModeDisabled;
        }
        return null;
    }


	public boolean isInCruisingMode(UUID player) {
		return player != null && inCruisingMode.containsKey(player);
	}

    /**
     * returns the craft of the player if he is in cruising mode
     * @param player the player who is in cruising mode
     * @return the craft which is in cruising mode by the given player
     */
    public Craft getCraftInCruisingMode(Player player)
    {
        if (player == null)
            return null;
        //return the craft of the player if he is in cruising mode
        return CraftManager.getCraft(inCruisingMode.get(player.getUniqueId()));
    }

    /**
     * player click interaction with craft
     * @param craft operated craft
     * @param action interaction of player with craft
     * @param clickedFace which side was clicked (up, down, left, right)
     * @param player operator of the craft
     * @return message for the player
     */
    public MessageEnum changeAngle(Craft craft, Action action, BlockFace clickedFace, Player player){
        //fire event
        CraftUseEvent useEvent = new CraftUseEvent(craft, player.getUniqueId(), InteractAction.moveFine);
        Bukkit.getServer().getPluginManager().callEvent(useEvent);

        if (useEvent.isCancelled())
            return null;


        if (action.equals(Action.RIGHT_CLICK_BLOCK )){
            if (config.getToolCruising().equalsFuzzy(player.getInventory().getItemInMainHand()))
            {
                //cruising mode
                pilotinMode(player, craft, false);
            }
        }
        return null;
    }

    /**
     * switches cruising mode for this craft
     * @param player - player cruising mode
     * @param craft - operated craft
     */
    public void pilotinMode(Player player, Craft craft, boolean fire)
    {
        if (player == null)
            return;

        boolean isCruisingMode = inCruisingMode.containsKey(player.getUniqueId());
        if (isCruisingMode)
        {
            if (craft == null)
                craft = getCraftInCruisingMode(player);

            //this player is already in cruising mode, he might fire the craft or turn the cruising mode off
            if (fire)
            {
                plugin.logDebug("fire Cannons");
                //MessageEnum message = plugin.getFireCrafts().playerFiring(craft, player, InteractAction.fireAutoaim);
                //userMessages.sendMessage(message, player, craft);
            }
            else
            {
                //turn off the cruising mode
                MessageEnum message = disableCruisingMode(player, craft);
                userMessages.sendMessage(message, player, craft);
            }
        }
        //enable cruising mode
        else if(craft != null)
        {
            //check if player has permission to aim
            if (player.hasPermission(craft.getCraftDesign().getPermissionCruising()))
            {
                //check distance before enabling the craft
                if (craft.isEntityOnShip(player))
                {
                    MessageEnum message = enableCruisingMode(player, craft);
                    userMessages.sendMessage(message, player, craft);
                }
                else
                {
                    userMessages.sendMessage(MessageEnum.CruisingModeTooFarAway, player, craft);
                }

            }
            else
            {
                //no Permission to aim
                userMessages.sendMessage(MessageEnum.PermissionErrorPilot, player, craft);
            }
        }
    }

    /**
     * updates the craft direction for player in cruising mode
     */
    private void updateCruisingMode()
    {
        //player in map change the angle to the angle the player is looking
        Iterator<Map.Entry<UUID, UUID>> iter = inCruisingMode.entrySet().iterator();
        while(iter.hasNext())
        {
            Map.Entry<UUID, UUID> entry = iter.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                iter.remove();
                continue;
            }


            //find the craft with this id
            Craft craft = CraftManager.getCraft(entry.getValue());
            if (craft == null ) {
                iter.remove();
                continue;
            }

            // only update if since the last update some ticks have past (updateSpeed is in ticks = 50ms)
            if (System.currentTimeMillis() >= craft.getLastAngleChange() + craft.getCraftDesign().getAngleUpdateSpeed())
            {
                // autoaming or fineadjusting
                if (craft.isEntityOnShip(player) && player.isOnline() && craft.isValid())
                {
                    MessageEnum message = updateAngle(player, craft, InteractAction.moveCruising);
                    userMessages.sendMessage(message, player, craft);
                }
                else
                {
                    //leave cruising Mode
                    MessageEnum message = disableCruisingMode(player, craft);
                    userMessages.sendMessage(message, player, craft);
                }
            }
        }
    }

    /**
     * evaluates the new craft angle and returns a message for the user
     * @param player operator of the craft
     * @param craft operated craft
     * @return message for the player
     */
    private MessageEnum updateAngle(Player player, Craft craft, InteractAction action) {
        if (craft == null)
            return null;

        if (player == null) {
            disableCruisingMode(player);
            return null;
        }

        CraftDesign design = craft.getCraftDesign();

        //angle changed
        boolean hasChanged = false;
        //message Enum
        MessageEnum message = null;

        //if the player is not the owner of this gun
        if (craft.getOwner()!=null && !craft.getOwner().equals(player.getUniqueId()) && design.isAccessForOwnerOnly())
            return MessageEnum.ErrorNotTheOwner;
        //if the player has the permission to adjust this gun
        if (!player.hasPermission(craft.getCraftDesign().getPermissionCruising()))
            return  MessageEnum.PermissionErrorPilot;

        CraftAngles angles;


        if (action == InteractAction.moveCruising && inCruisingMode.containsKey(player.getUniqueId()))
        {
            //aiming mode only if player is sneaking
            if (player.isSneaking())
            {
                angles = getCraftAngle(craft, player.getLocation().getYaw(), player.getLocation().getPitch());
            }
            else
                return null;
        }
        else
        {
            //todo implement stick move mode
            plugin.logDebug("Stick move mode");
            angles = getCraftAngle(craft, player.getLocation().getYaw(), player.getLocation().getPitch());
//            //barrel clicked to change angle
//            if (player!=null) {
//                angles = CheckBlockFace(clickedFace, craft.getCraftDirection(), player.isSneaking(), design.getAngleStepSize());
//                //register impact predictor
//                craft.addObserver(player, true);
//                combine = false;
//            }
//            else
//                return null;
        }

        boolean largeChange = false;
        //larger step
        if (Math.abs(angles.getHorizontal()) >= design.getAngleLargeStepSize())
        {
            if (setHorizontalAngle(craft, angles, design.getAngleLargeStepSize())){
                hasChanged = true;
                largeChange = true;
                message = MessageEnum.SettingCombinedAngle;
            }
        }
        //small step if no large step was possible
        if (!largeChange && Math.abs(angles.getHorizontal()) >= design.getAngleStepSize()/2.)
        {
            if (setHorizontalAngle(craft, angles, design.getAngleStepSize())){
                hasChanged = true;
                message = MessageEnum.SettingCombinedAngle;
            }
        }
        //larger step
        largeChange = false;
        if (Math.abs(angles.getVertical()) >= design.getAngleLargeStepSize())
        {
            if (setVerticalAngle(craft, angles, design.getAngleLargeStepSize())){
                hasChanged = true;
                largeChange = true;
                message = MessageEnum.SettingCombinedAngle;
            }
        }
        //small step if no large step was possible
        if (!largeChange && Math.abs(angles.getVertical()) >= design.getAngleStepSize()/2.)
        {
            if (setVerticalAngle(craft, angles, design.getAngleStepSize())){
                hasChanged = true;
                message = MessageEnum.SettingCombinedAngle;
            }
        }

        //update the time
        craft.setLastAngleChange(System.currentTimeMillis());
        //show aiming vector in front of the craft
        showAimingVector(craft, player);

        //display message only if the angle has changed
        if (hasChanged) {
            craft.angleHasChanged();
            //player.getWorld().playSound(craft.getRotationCenter(), Sound.IRON_GOLEM_WALK, 1f, 0.5f);
            IroncladUtil.playSound(craft.getRotationCenter(),design.getSoundAngleChange());
            if (craft.getCraftDesign().isAngleUpdateMessage())
                return message;
            else
                return null;
        }
        //no change in angle
        return null;
    }


    private boolean setHorizontalAngle(Craft craft, CraftAngles angles, double step){
        step = Math.abs(step);

        if (angles.getHorizontal() >= 0)
        {
            // right
            if (craft.getYaw() + step <= craft.getMaxYaw() + 0.001)
            {
                //if smaller than minimum -> set to minimum
                if (craft.getYaw() < craft.getMinYaw())
                    craft.setYaw(craft.getMinYaw());
                craft.setYaw(craft.getYaw() + step);
                return true;

            }
        }
        else
        {
            // left
            if (craft.getYaw() - step >= craft.getMinYaw() - 0.001)
            {
                //if smaller than maximum -> set to maximum
                if (craft.getYaw() > craft.getMaxYaw())
                    craft.setYaw(craft.getMaxYaw());
                craft.setYaw(craft.getYaw() - step);
                return true;
            }
        }
        return false;
    }

    private boolean setVerticalAngle(Craft craft, CraftAngles angles, double step) {
        step = Math.abs(step);

        if (angles.getVertical() >= 0.0)
        {
            // up
            if (craft.getPitch() + step <= craft.getMaxPitch() + 0.001)
            {
                //if smaller than minimum -> set to minimum
                if (craft.getPitch() < craft.getMinPitch())
                    craft.setPitch(craft.getMinPitch());
                craft.setPitch(craft.getPitch() + step);
                return true;

            }
        }
        else
        {
            // down
            if (craft.getPitch() - step >= craft.getMinPitch() - 0.001)
            {
                if (craft.getPitch() > craft.getMaxPitch())
                    craft.setPitch(craft.getMaxPitch());
                craft.setPitch(craft.getPitch() - step);
                return true;
            }
        }
        return false;
    }


    /**
     * evaluates the difference between actual craft direction and the given direction
     * @param craft operated craft
     * @param yaw yaw of the direction to aim
     * @param pitch pitch of the direction to aim
     * @return new craft aiming direction
     */
    private CraftAngles getCraftAngle(Craft craft, double yaw, double pitch)
    {
        double horizontal = yaw - craft.getYaw();
        horizontal = horizontal % 360;
        while(horizontal < -180)
            horizontal = horizontal + 360;

        return new CraftAngles(horizontal, -pitch - craft.getPitch());
    }

    /**
     * show a line where the cannon is aiming
     * @param craft - operated cannon
     * @param player - player operating the cannon
     */
    public void showAimingVector(Craft craft, Player player)
    {
        if (player == null || craft == null)
            return;

        // Imitation of angle
        if(config.isImitatedCruisingEnabled() && isImitatingEnabled(player.getUniqueId()))
        {
            plugin.getFakeBlockHandler().imitateLine(player, craft.getRotationCenter(), craft.getCruisingVector(), 0,
                    config.getImitatedCruisingLineLength(), config.getImitatedCruisingMaterial(), FakeBlockType.CRUISING, config.getImitatedCruisingTime());
        }
    }

    public void disableImitating(Player player){
        userMessages.sendMessage(MessageEnum.ImitatedEffectsDisabled, player);
        //it is enabled on default, adding to this list will stop the aiming effect
        imitatedEffectsOff.add(player.getUniqueId());
    }

    public boolean isImitatingEnabled(UUID playerUID){
        //it is enabled on default, adding to this list will stop the aiming effect
        return !imitatedEffectsOff.contains(playerUID);
    }

    public void enableImitating(Player player){
        userMessages.sendMessage(MessageEnum.ImitatedEffectsEnabled, player);
        //it is enabled on default, adding to this list will stop the aiming effect
        imitatedEffectsOff.remove(player.getUniqueId());
    }

}
