package at.pavlov.ironclad.craft;

import java.util.*;
import java.util.stream.Collectors;

import at.pavlov.ironclad.Enum.BreakCause;
import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.container.SimpleEntity;
import at.pavlov.ironclad.utils.IroncladUtil;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.material.Attachable;

import at.pavlov.ironclad.Enum.MessageEnum;
import at.pavlov.ironclad.container.SimpleBlock;
import at.pavlov.ironclad.utils.InventoryManagement;
import at.pavlov.ironclad.sign.CraftSign;

public class Craft implements Cloneable {
    // Database id - is -1 until stored in the database. Then it is the id in the database
    private UUID databaseId;

    private String designID;
    private String craftName;

    // direction the craft is facing
    private BlockFace craftDirection;
    private BlockFace futureCraftDirection;
    // the angle and velocity the craft is currently moving
    private double yaw;
    private double pitch;
    private double velocity;
    //direction the craft is moving (updated from yaw, pitch, velocity)
    private Vector3 cruisingVector;
    // the location is describe by the offset of the craft and the design
    private Vector3 offset;
    // world of the craft
    private UUID world;

    //actual dimensions of the craft
    private int craftLength;
    private int craftWidth;
    private int craftHeight;

    // was the craft fee paid
    private boolean paid;

    // player who has build this craft
    private UUID owner;
    // designID of the craft, for different types of ironclad - not in use
    private boolean isValid;
    // time point of the last movement of the craft
    private long lastMoved = 0;
    // last time the angle of the ship has been updated by the cruising mode
    private long lastAngleChange = 0;
    // currently changed by async thread
    private boolean isProcessing;
    // the player which has used the craft last
    private UUID lastUser;

    // distance travelled by this craft
    private double travelledDistance;

    // has the craft entry changed since it was last saved in the database
    private boolean updated;

    private CraftDesign design;

    public Craft(CraftDesign design, UUID world, BlockVector3 craftOffset, BlockFace craftDirection, UUID owner) {
        this(design, world, Vector3.at(craftOffset.getX(), craftOffset.getY(), craftOffset.getZ()), craftDirection, owner);
    }

    public Craft(CraftDesign design, UUID world, Vector3 craftOffset, BlockFace craftDirection, UUID owner)
    {
        this.design = design;
        this.designID = design.getDesignID();
        this.world = world;
        this.offset = craftOffset;
        this.craftDirection = craftDirection;
        this.futureCraftDirection = craftDirection;
        this.owner = owner;
        this.isValid = true;
        this.craftName = null;
        // ignore if there is no fee
        this.paid = design.getEconomyBuildingCost() <= 0;

        //the craft is not moving
        this.yaw = IroncladUtil.directionToYaw(craftDirection);
        this.pitch = 0.0;
        this.velocity = 1.0;

        BlockVector3 dim = design.getCraftDimensions();
        if (dim.getX() >= dim.getY()) {
            craftLength = dim.getBlockX();
            craftWidth = dim.getBlockZ();
        }
        else{
            craftLength = dim.getBlockZ();
            craftWidth = dim.getBlockX();
        }
        craftHeight = dim.getBlockY();

        this.lastMoved = System.currentTimeMillis();
        this.lastAngleChange = System.currentTimeMillis();
        this.isProcessing = false;

        this.databaseId = UUID.randomUUID();
        this.updated = true;
    }

    /**
     * Get a new vector.
     *
     * @return vector
     */
    @Override
    public Craft clone() {
        try {
            return (Craft) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }


    /**
     * returns the location of the location of the craft
     * @return location of the craft
     */
    public Location getLocation()
    {
        return design.getAllCraftBlocks(this).get(0).toLocation(getWorldBukkit());
    }

    /**
     * returns the location of the muzzle
     * @return location of the muzzle
     */
    public Location getRotationCenter()
    {
          return design.getRotationCenter(this);
    }

    /**
     * returns a random block of the barrel or the craft if there is no barrel
     * @return location of the barrel block
     */
    public Location getRandomBarrelBlock()
    {
        Random r = new Random();
        List<Location> barrel = design.getHullBlocks(this);
        if (barrel.size() > 0)
            return barrel.get(r.nextInt(barrel.size()));
        List<SimpleBlock> all = design.getAllCraftBlocks(this);
        return all.get(r.nextInt(all.size())).toLocation(getWorldBukkit());
    }

    /**
     * returns the inventories of all attached chests
     * @return - list of inventory
     */
    List<Inventory> getInventoryList()
    {
        //get the inventories of all attached chests
        List<Inventory> invlist = new ArrayList<Inventory>();
        for (Location loc : getCraftDesign().getChestLocations(this))
        {
            // check if block is a chest
            invlist = InventoryManagement.getInventories(loc.getBlock(), invlist);
        }
        return invlist;
    }

    /**
     * removes the sign text and charge of the craft after destruction
     * @param breakBlocks break all craft block naturally
     * @param canExplode if the craft can explode when loaded with gunpoweder
     * @param cause cause of the craft destruction
     */
    public MessageEnum destroyCraft(boolean breakBlocks, boolean canExplode, BreakCause cause)
    {
        // update craft signs the last time
        isValid = false;
        updateCraftSigns();

        if (breakBlocks)
            breakAllBlocks();

        // return message
        switch (cause)
        {
            case Other:
                return null;
            case Dismantling:
                return MessageEnum.CraftDismantled;
            default:
                return MessageEnum.CraftDestroyed;
        }
    }

    /**
     * this will force the craft to create up at this location - all blocks will be overwritten
     */
    public void create()
    {
        ArrayList<SimpleBlock> attatchedBlocks = new ArrayList<>();
        for (SimpleBlock cBlock : design.getAllCraftBlocks(this.getCraftDirection()))
        {
            //check if the block is attached to something, then do it later
            if (cBlock.getBlockState() instanceof org.bukkit.block.data.Directional){
                attatchedBlocks.add(cBlock);
            }
            else {
                Block wBlock = cBlock.toLocation(getWorldBukkit(), offset).getBlock();
                wBlock.setBlockData(cBlock.getBlockData());
            }
        }
        //place the attachable blocks
        for (SimpleBlock aBlock : attatchedBlocks){
            Block wBlock = aBlock.toLocation(getWorldBukkit(), offset).getBlock();
            wBlock.setBlockData(aBlock.getBlockData());
        }
    }

    /**
     * this will force the craft blocks to become AIR
     */
    public void hide()
    {
        //remove only attachable block
        for (SimpleBlock cBlock : design.getAllCraftBlocks(this.getCraftDirection()))
        {
            Block wBlock = cBlock.toLocation(getWorldBukkit(), offset).getBlock();
            //if that block is not loaded
            if (wBlock == null) return;

            if (wBlock.getState() instanceof Attachable)
            {
                //System.out.println("hide " + wBlock.getType());
                wBlock.setType(Material.AIR);
                //wBlock.setData((byte) 0, false);
            }
        }

        //remove all
        for (SimpleBlock cBlock : design.getAllCraftBlocks(this.getCraftDirection()))
        {
            Block wBlock = cBlock.toLocation(getWorldBukkit(), offset).getBlock();

            if (wBlock.getType() != Material.AIR)
            {
                wBlock.setType(Material.AIR);
               // wBlock.setData((byte) 0, false);
            }
        }
    }


    /**
     * breaks all craft blocks of the craft
     */
    private void breakAllBlocks()
    {
        List<SimpleBlock> locList = design.getAllCraftBlocks(this);
        World world = getWorldBukkit();
        for (SimpleBlock loc : locList)
        {
            loc.toLocation(world).getBlock().breakNaturally();
        }
    }


    /**
     * returns true if this block is a block of the craft
     * @param block - block to check
     * @return - true if it is part of this craft
     */
    public boolean isCraftBlock(Block block)
    {
        if (block == null || block.getLocation() == null || block.getWorld() == null)
            return false;

        if (getWorld().equals(block.getWorld().getUID())){
            for (SimpleBlock designBlock : design.getAllCraftBlocks(craftDirection))
            {
                if (designBlock.compareMaterialAndLoc(block, getOffsetBlock()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * returns true if this block is a block of the craft
     * @param loc - location to check
     * @return - true if it is part of this craft
     */
    public boolean isLocationPartOfCraft(Location loc)
    {
        if (loc == null || loc.getWorld() == null)
            return false;

        if (getWorld().equals(loc.getWorld().getUID())){
            for (SimpleBlock designBlock : design.getAllCraftBlocks(craftDirection)) {
                if (designBlock.compareLocation(loc, getOffsetBlock()))
                    return true;
            }
        }
        return false;
    }

    /**
     * returns true if this block is a block of the craft
     * @param loc - location to check
     * @return - true if it is part of this craft
     */
    public boolean isLocationPartOfCraft(BlockVector3 loc)
    {
        if (loc == null)
            return false;

        for (SimpleBlock designBlock : design.getAllCraftBlocks(craftDirection)) {
            if (designBlock.compareLocation(loc, getOffsetBlock()))
                return true;
        }
        return false;
    }

    /**
     * return true if this block can be destroyed, false if it is protected
     * @param block - location of the block
     * @return - true if the block can be destroyed
     */
    public boolean isProtectedBlock(Location block)
    {
        for (Location loc : design.getProtectedBlocks(this))
        {
            if (loc.equals(block))
            {
                return true;
            }
        }
        return false;
    }


    /**
     * return true if this location where the torch interacts with the craft
     *
     * @param block
     * @return
     */
    public boolean isChestInterface(Location block)
    {
        for (Location loc : design.getChestLocations(this))
        {
            if (loc.equals(block))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * return true if this location where the torch interacts with the craft
     * does not check the ID
     *
     * @param loc
     * @return
     */
    public boolean isCannonSign(Location loc)
    {
        if (loc.getBlock().getType() != Material.WALL_SIGN) return false;

        CraftBlocks cannonBlocks  = this.getCraftDesign().getCannonBlockMap().get(this.getCraftDirection());
        if (cannonBlocks != null)
        {
            for (SimpleBlock cannonblock : cannonBlocks.getChest())
            {
                // compare location
                if (cannonblock.toLocation(this.getWorldBukkit(),this.offset).equals(loc))
                {
                    Block block = loc.getBlock();
                    //compare and data
                    //only the two lower bits of the bytes are important for the direction (delays are not interessting here)
                    //todo check facing
                    //if (cannonblock.getData() == block.getData() || block.getData() == -1 || cannonblock.getData() == -1 )
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * updates the location of the craft
     * @param moved - how far the craft has been moved
     */
    public void move(Vector3 moved)
    {
        offset = offset.add(moved);
        setCraftDirection(this.futureCraftDirection);
        setLastMoved(System.currentTimeMillis());
        this.hasUpdated();
    }

    /**
     * finished update of async craft movment
     */
    public void movementPerformed(){
        setProcessing(false);
        move(getCruisingVector());
    }

    /**
     * updates the rotation of the craft
     * @param center - center of the rotation
     * @param angle - how far the craft is rotated in degree (90, 180, 270, -90)
     */
    public void rotate(BlockVector3 center, int angle)
    {
        if (angle == 0)
            return;

        double dAngle =  angle*Math.PI/180;

        BlockVector3 diffToCenter = getOffsetBlock().subtract(center);

        double newX = diffToCenter.getX()*Math.cos(dAngle) - diffToCenter.getZ()*Math.sin(dAngle);
        double newZ = diffToCenter.getX()*Math.sin(dAngle) + diffToCenter.getZ()*Math.cos(dAngle);

        offset = Vector3.at(Math.round(center.getX()+newX), offset.getY(), Math.round(center.getZ()+newZ));

        //rotate blockface
        if (angle > 0)
        {
            for (int i = 0; i<=angle%90; i++)
                craftDirection = IroncladUtil.roatateFace(craftDirection);
        }
        else
        {
            for (int i = 0; i<=(-angle)%90; i++)
                craftDirection = IroncladUtil.roatateFaceOpposite(craftDirection);
        }
        this.hasUpdated();

    }

    /**
     * updates the rotation of the craft by rotating it 90 to the right
     * @param center - center of the rotation
     */
    public void rotateRight(BlockVector3 center)
    {
        this.rotate(center, 90);
    }

    /**
     * updates the rotation of the craft by rotating it 90 to the left
     * @param center - center of the rotation
     */
    public void rotateLeft(BlockVector3 center)
    {
        this.rotate(center, -90);
    }

    /**
     * updates the rotation of the craft by rotating it 180
     * @param center - center of the rotation
     */
    public void rotateFlip(BlockVector3 center)
    {
        this.rotate(center, 180);
    }


    /**
     * plays the given effect on random locations of the barrel
     * @param amount - number of effects
     */
    void playSmokeEffect(int amount)
    {
        if (amount <= 0)
            return;

        Random r = new Random();
        List<Location> HullBlockList = design.getHullBlocks(this);

        //if the barrel list is 0 something is completely odd
        int max = HullBlockList.size();
        if (max < 0)
            return;

        Location effectLoc;
        BlockFace face;

        for (int i=0; i<amount; i++)
        {
            //grab a random face and find a block for them the adjacent block is AIR
            face = IroncladUtil.randomBlockFaceNoDown();
            int j = 0;
            do
            {
                i++;
                effectLoc = HullBlockList.get(r.nextInt(max)).getBlock().getRelative(face).getLocation();
            } while (i<4 && effectLoc.getBlock().getType() != Material.AIR);

            effectLoc.getWorld().playEffect(effectLoc, Effect.SMOKE, face);
            //IroncladUtil.playSound(effectLoc, design.getSoundHot());
        }
    }


    /**
     * @return true if the ironclad has a sign
     */
    public boolean hasCraftSign()
    {
        // search all possible sign locations
        for (Location signLoc : design.getSignLocations(this))
        {
            if (signLoc.getBlock().getType().equals(Material.WALL_SIGN))
                return true;
        }
        return false;
    }

    /**
     * updates all signs that are attached to a craft
     */
    public void updateCraftSigns()
    {
        // update all possible sign locations
        for (Location signLoc : design.getSignLocations(this))
        {
            //check blocktype and orientation before updating sign.
            if (isCannonSign(signLoc))
                updateSign(signLoc.getBlock());
        }
    }

    /**
     * updates the selected sign
     * @param block sign block
     */
    private void updateSign(Block block)
    {
        Sign sign = (Sign) block.getState();

        if (isValid)
        {
            // Craft name in the first line
            sign.setLine(0, getSignString(0));
            // Craft owner in the second
            sign.setLine(1, getSignString(1));
            // loaded Gunpowder/Projectile
            sign.setLine(2, getSignString(2));
            // angles
            sign.setLine(3, getSignString(3));
        }
        else
        {
            // Craft name in the first line
            sign.setLine(0, "this craft is");
            // Craft owner in the second
            sign.setLine(1, "damaged");
            // loaded Gunpowder/Projectile
            sign.setLine(2, "");
            // angles
            sign.setLine(3, "");
        }

        sign.update(true);
    }

    /**
     * returns the strings for the sign
     * @param index line on sign
     * @return line on the sign
     */
    public String getSignString(int index)
    {

        switch (index)
        {

            case 0 :
                // Craft name in the first line
                if (craftName == null) craftName = "missing Name";
                return craftName;
            case 1 :
                // Craft owner in the second
                if (owner == null)
                    return "missing Owner";
                OfflinePlayer bPlayer = Bukkit.getOfflinePlayer(owner);
                if (bPlayer == null || !bPlayer.hasPlayedBefore())
                    return "not found";
                return bPlayer.getName();
            case 2 :
                return "";
            case 3 :
                return "";
        }
        return "missing";
    }




    /**
     * returns the name of the craft written on the sign
     *
     * @return
     */
    private String getLineOfCannonSigns(int line)
    {
        String lineStr = "";
        // goto the first craft sign
        for (Location signLoc : design.getSignLocations(this))
        {
            lineStr = CraftSign.getLineOfThisSign(signLoc.getBlock(), line);
            // if something is found return it
            if (lineStr != null && !lineStr.equals(""))
            {
                return lineStr;
            }
        }

        return lineStr;
    }

    /**
     * returns the craft name that is written on a craft sign
     *
     * @return
     */
    public String getCraftNameFromSign()
    {
        return getLineOfCannonSigns(0);
    }

    /**
     * returns the craft owner that is written on a craft sign
     *
     * @return
     */
    public String getOwnerFromSign()
    {
        return getLineOfCannonSigns(1);
    }

    /**
     * returns true if craft design for this craft is found
     *
     * @param cannonDesign
     * @return
     */
    public boolean equals(CraftDesign cannonDesign)
    {
        if (designID.equals(cannonDesign.getDesignID())) return true;
        return false;
    }

    /**
     *
     * @param obj - object to compare
     * @return true if both ironclad are equal
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Craft){
            Craft obj2 = (Craft) obj;
            return this.getUID().equals(obj2.getUID());
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return databaseId.hashCode();
    }

    /**
     * get bukkit world
     *
     * @return
     */
    public World getWorldBukkit()
    {
        if (this.world != null)
        {
            World bukkitWorld = Bukkit.getWorld(this.world);
            if (bukkitWorld == null)
                System.out.println("[Ironclad] Can't find world: " + world);
            return Bukkit.getWorld(this.world);
            // return new Location(bukkitWorld, )
        }
        return null;
    }

    public UUID getUID()
    {
        return databaseId;
    }

    public void setUID(UUID ID)
    {
        this.databaseId = ID;
        this.hasUpdated();
    }

    public String getDesignID()
    {
        return designID;
    }

    public void setDesignID(String designID)
    {
        this.designID = designID;
        this.hasUpdated();
    }

    public String getCraftName()
    {
        return craftName;
    }

    public void setCraftName(String name)
    {
        this.craftName = name;
        this.hasUpdated();
    }

    public BlockFace getCraftDirection()
    {
        return craftDirection;
    }

    public BlockFace getFutureDirection()
    {
        //todo implement
        return futureCraftDirection;
    }

    public void setFutureCraftDirection(BlockFace futureCraftDirection) {
        this.futureCraftDirection = futureCraftDirection;
    }

    public BlockVector3 getFutureCraftOffset(){
        //todo add rotation
        return this.getOffset().add(getCruisingVector()).toBlockPoint();
    }

    public Location getFutureLocation(Location start){
        Location loc = start.add(BukkitAdapter.adapt(this.getWorldBukkit(), getCruisingVector()));
        //todo rotation
        return loc;
    }

    public void setCraftDirection(BlockFace craftDirection)
    {
        this.craftDirection = craftDirection;
        this.hasUpdated();
    }

    public Location transformToFutureLocation(Location loc){
        Ironclad.getPlugin().logDebug("CraftLoc " + loc);
        return BukkitAdapter.adapt(loc.getWorld(), transformToFutureLocation(BukkitAdapter.asVector(loc)));
    }

    public Vector3 transformToFutureLocation(Vector3 vec){
        return IroncladUtil.rotateDirection(getCraftDirection(), getFutureDirection(), vec.subtract(getOffset())).add(offset).add(getCruisingVector());
    }

    public BlockVector3 transformToFutureLocation(BlockVector3 vec){
        return IroncladUtil.rotateDirection(getCraftDirection(), getFutureDirection(), vec.subtract(getOffsetBlock())).add(getFutureCraftOffset());
    }

    public UUID getWorld()
    {
        return world;
    }

    public void setWorld(UUID world)
    {
        this.world = world;
        this.hasUpdated();
    }

    public UUID getOwner()
    {
        return owner;
    }

    public void setOwner(UUID owner)
    {
        this.owner = owner;
        this.hasUpdated();
    }

    public boolean isValid()
    {
        return isValid;
    }

    public void setValid(boolean isValid)
    {
        this.isValid = isValid;
        this.hasUpdated();
    }

    /**
     * returns the exact Vector (double) of the craft
     * @return Vector(double, double, double) of the craft position
     */
    public Vector3 getOffset()
    {
        return offset;
    }

    /**
     * returns the block Vector (int) of the craft
     * @return Block Vector(int, int, int) of the craft position
     */
    public BlockVector3 getOffsetBlock()
    {
        return offset.toBlockPoint();
    }

    public void setOffset(Vector3 offset)
    {
        this.offset = offset;
        this.hasUpdated();
    }

    public void setCraftDesign(CraftDesign design)
    {
        this.design = design;
        this.hasUpdated();
    }

    public CraftDesign getCraftDesign()
    {
        return this.design;
    }

    public UUID getLastUser()
    {
        return lastUser;
    }

    public void setLastUser(UUID lastUser)
    {
        this.lastUser = lastUser;
        if(design.isLastUserBecomesOwner())
            this.setOwner(lastUser);
    }

    public void updateCruisingVector(){
        this.cruisingVector = IroncladUtil.directionToVector(this.yaw, this.pitch, this.velocity);
        //System.out.println("getCruisingVector: " + cruisingVector + " Yaw " + this.yaw);
    }

    public Vector3 getCruisingVector(){
        return this.cruisingVector;
    }

    public boolean isChunkLoaded(){
        Chunk chunk = getLocation().getChunk();
        return chunk != null && chunk.isLoaded();
    }

    public double getVelocity() {
        return velocity;
    }

    public boolean isMoving(){
        return Math.abs(velocity) > 0.1;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
        this.hasUpdated();
    }

    public void boughtByPlayer(UUID playerID){
        setPaid(true);
        setOwner(playerID);
    }

    public boolean isUpdated() {
        return updated;
    }

    public void hasUpdated() {
        this.updated = true;
    }

    public void setUpdated(boolean updated){
        this.updated = updated;
    }


    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        yaw = yaw % 360;
        while(yaw < -180)
            yaw = yaw + 360;
        while(yaw > 180)
            yaw = yaw - 360;

        this.yaw = yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public int getCraftLength() {
        return craftLength;
    }

    protected void setCraftLength(int craftLength) {
        this.craftLength = craftLength;
    }

    public int getCraftWidth() {
        return craftWidth;
    }

    protected void setCraftWidth(int craftWidth) {
        this.craftWidth = craftWidth;
    }

    public int getCraftHeight() {
        return craftHeight;
    }

    protected void setCraftHeight(int craftHeight) {
        this.craftHeight = craftHeight;
    }

    /**
     * returns the dimensions of the craft depending of the directions the craft is facing
     * @return Vector(x,y,z) of the dimensions
     */
    public BlockVector3 getCraftDimensions(){
        switch (this.craftDirection){
            case NORTH:
            case SOUTH:
                return BlockVector3.at(getCraftWidth(), getCraftHeight(), getCraftLength());
            case EAST:
            case WEST:
                return BlockVector3.at(getCraftLength(), getCraftHeight(), getCraftWidth());
            default:
                return null;
        }
    }

    /**
     * returns the minimum bounding box of the craft
     * @return the minimum bounding box of the craft
     */
    public BlockVector3 getCraftMinBoundingBox(){
        return this.design.getMinBoundnigBoxLocation(this);
    }

    /**
     * returns the maximum bounding box of the craft
     * @return the maximum bounding box of the craft
     */
    public BlockVector3 getCraftMaxBoundingBox(){
        return this.design.getMaxBoundnigBoxLocation(this);
    }

    /**
     * get all Entities on a ship
     * @return Set of Entities on ship
     */
    public Set<Entity> getEntitiesOnShip(){
        BlockVector3 d = getCraftDimensions();
        Set<Entity> entities = IroncladUtil.getNearbyEntitiesInBox(design.getCraftCenter(this), d.getX(), d.getY(), d.getZ());
        entities.removeIf(entity -> !isEntityOnShip(entity));
        return  entities;
    }

    /**
     * get all Entities on a ship
     * @return Set of Entities on ship
     */
    public Set<SimpleEntity> getSimpleEntitiesOnShip(){
        return getEntitiesOnShip().stream().map(SimpleEntity::new).collect(Collectors.toSet());
    }

    public boolean isEntityOnShip(Entity entity){
//        if (entity == null || entity.getLocation() == null || entity.getLocation().getWorld() == null)
//            return false;
        Location loc = entity.getLocation();
        //make the bounding box a little bit large if someone is peeking over the edge
        BlockVector3 minBB = this.getCraftDesign().getMinBoundnigBoxLocation(this).subtract(1, 1, 1);
        BlockVector3 maxBB = this.getCraftDesign().getMaxBoundnigBoxLocation(this).add(1, 1, 1);
        if (loc.getWorld().getUID().equals(this.world)){
            return loc.getX() >= minBB.getX() && loc.getY() >= minBB.getY() && loc.getZ() >= minBB.getZ() && loc.getX() <= maxBB.getX() && loc.getY() <= maxBB.getY() && loc.getZ() <= maxBB.getZ();
        }
        return false;
    }

    public boolean isBlockOnShip(Location loc){
        if (loc == null || loc.getWorld() == null)
            return false;

        //make the bounding box a little bit large if someone is peeking over the edge
        BlockVector3 minBB = this.getCraftDesign().getMinBoundnigBoxLocation(this).subtract(1, 1, 1);
        BlockVector3 maxBB = this.getCraftDesign().getMaxBoundnigBoxLocation(this).add(1, 1, 1);
        if (loc.getWorld().getUID().equals(this.world)){
            if (loc.getX() >= minBB.getX() && loc.getY() >= minBB.getY() && loc.getZ() >= minBB.getZ() && loc.getX() <= maxBB.getX() && loc.getY() <= maxBB.getY() && loc.getZ() <= maxBB.getZ()){
                return this.isLocationPartOfCraft(loc.clone().subtract(1, 0, 0)) &&
                        this.isLocationPartOfCraft(loc.clone().add(1, 0, 0)) &&
                        this.isLocationPartOfCraft(loc.clone().subtract(0, 1, 0)) &&
                        this.isLocationPartOfCraft(loc.clone().add(0, 1, 0)) &&
                        this.isLocationPartOfCraft(loc.clone().subtract(0, 0, 1)) &&
                        this.isLocationPartOfCraft(loc.clone().add(0, 0, 1));
            }
        }
        return false;
    }

    public double getTravelledDistance() {
        return travelledDistance;
    }

    public void setTravelledDistance(double travelledDistance) {
        this.travelledDistance = travelledDistance;
    }

    public long getLastMoved() {
            return lastMoved;
    }

    public void setLastMoved(long lastMoved) {
        this.lastMoved = lastMoved;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public void setProcessing(boolean processing) {
        isProcessing = processing;
    }

    public long getLastAngleChange() {
        return lastAngleChange;
    }

    public void setLastAngleChange(long lastAngleChange) {
        this.lastAngleChange = lastAngleChange;
    }

    public void angleHasChanged(){
        lastAngleChange = System.currentTimeMillis();
    }

    public double getMaxYaw(){
        return this.getYaw() + getCraftDesign().getMaxHorizontalAngle();
    }

    public double getMinYaw(){
        return this.getYaw() + getCraftDesign().getMinHorizontalAngle();
    }

    public double getMaxPitch(){
        return getCraftDesign().getMaxVerticalAngle();
    }

    public double getMinPitch(){
        return getCraftDesign().getMinVerticalAngle();
    }
}
