package at.pavlov.ironclad.craft;

import java.util.*;

import at.pavlov.ironclad.Enum.BreakCause;
import at.pavlov.ironclad.utils.IroncladUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.material.Attachable;
import org.bukkit.material.Directional;
import org.bukkit.util.Vector;

import at.pavlov.ironclad.Enum.MessageEnum;
import at.pavlov.ironclad.container.SimpleBlock;
import at.pavlov.ironclad.utils.InventoryManagement;
import at.pavlov.ironclad.sign.CraftSign;

public class Craft
{
    // Database id - is -1 until stored in the database. Then it is the id in the
    // database
    private UUID databaseId;

    private String designID;
    private String craftName;

    // direction the craft is facing
    private BlockFace craftDirection;
    // the angle and velocity the craft is currently moving
    private double yaw;
    private double pitch;
    private double velocity;
    // the location is describe by the offset of the craft and the design
    private Vector offset;
    // world of the craft
    private UUID world;

    //actual dimensions of the craft
    private int craftMaxLength;
    private int craftMaxWidth;
    private int craftMaxHeight;

    // was the craft fee paid
    private boolean paid;

    // player who has build this craft
    private UUID owner;
    // designID of the craft, for different types of ironclad - not in use
    private boolean isValid;
    // time point of the last start of the firing sequence (used in combination with isFiring)
    private long lastUsed;
    // the player which has used the craft last
    private UUID lastUser;

    // amount of fired cannonballs with this craft
    private double travelledDistance;

    // has the craft entry changed since it was last saved in the database
    private boolean updated;

    private CraftDesign design;


    public Craft(CraftDesign design, UUID world, Vector cannonOffset, BlockFace craftDirection, UUID owner)
    {

        this.design = design;
        this.designID = design.getDesignID();
        this.world = world;
        this.offset = cannonOffset;
        this.craftDirection = craftDirection;
        this.owner = owner;
        this.isValid = true;
        this.craftName = null;
        // ignore if there is no fee
        this.paid = design.getEconomyBuildingCost() <= 0;

        //the craft is not moving
        this.yaw = IroncladUtil.directionToYaw(craftDirection);
        this.pitch = 0.0;
        this.velocity = 0.0;

        this.databaseId = UUID.randomUUID();
        this.updated = true;
    }


    /**
     * returns the location of the location of the craft
     * @return location of the craft
     */
    public Location getLocation()
    {
        return design.getAllCraftBlocks(this).get(0);
    }

    /**
     * returns the location of the muzzle
     * @return location of the muzzle
     */
    public Location getMuzzle()
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
        List<Location> all = design.getAllCraftBlocks(this);
        return all.get(r.nextInt(all.size()));
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
        updateCannonSigns();

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
            System.out.println("cBlock: " + cBlock.getBlockData());
            //check if the block is attached to something, then do it later
            if (cBlock.getBlockData() instanceof org.bukkit.block.data.Directional){
                System.out.println("to attach Block: " + cBlock.getBlockData());
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
            System.out.println("attached block: " + aBlock.getBlockData());
            System.out.println("attached loc: " +       aBlock.toLocation(getWorldBukkit(), offset));
            System.out.println("--------------------");
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
        List<Location> locList = design.getAllCraftBlocks(this);
        for (Location loc : locList)
        {
            loc.getBlock().breakNaturally();
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
                if (designBlock.compareMaterialAndLoc(block, offset))
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
                if (designBlock.compareLocation(loc, offset))
                    return true;
            }
        }
        return false;
    }

    /**
     * return true if this block can be destroyed, false if it is protected
     * @param block - location of the block
     * @return - true if the block can be destroyed
     */
    public boolean isDestructibleBlock(Location block)
    {
        for (Location loc : design.getDestructibleBlocks(this))
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
    public void move(Vector moved)
    {
        offset.add(moved);
        this.hasUpdated();
    }

    /**
     * updates the rotation of the craft
     * @param center - center of the rotation
     * @param angle - how far the craft is rotated in degree (90, 180, 270, -90)
     */
    public void rotate(Vector center, int angle)
    {
        if (angle == 0)
            return;

        double dAngle =  angle*Math.PI/180;

        center = new Vector (center.getBlockX(), center.getBlockY(), center.getBlockZ());

        Vector diffToCenter = offset.clone().subtract(center);

        double newX = diffToCenter.getX()*Math.cos(dAngle) - diffToCenter.getZ()*Math.sin(dAngle);
        double newZ = diffToCenter.getX()*Math.sin(dAngle) + diffToCenter.getZ()*Math.cos(dAngle);

        offset = new Vector(Math.round(center.getX()+newX), offset.getBlockY(), Math.round(center.getZ()+newZ));

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
    public void rotateRight(Vector center)
    {
        this.rotate(center, 90);
    }

    /**
     * updates the rotation of the craft by rotating it 90 to the left
     * @param center - center of the rotation
     */
    public void rotateLeft(Vector center)
    {
        this.rotate(center, -90);
    }

    /**
     * updates the rotation of the craft by rotating it 180
     * @param center - center of the rotation
     */
    public void rotateFlip(Vector center)
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
    public boolean hasCannonSign()
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
    public void updateCannonSigns()
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
    public String getCannonNameFromSign()
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

    public void setCraftDirection(BlockFace craftDirection)
    {
        this.craftDirection = craftDirection;
        this.hasUpdated();
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

    public Vector getOffset()
    {
        return offset;
    }

    public void setOffset(Vector offset)
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

    public Vector getTravelVector(){
        yaw = yaw % 360;
        while(yaw < -180)
            yaw = yaw + 360;
        while(yaw > 180)
            yaw = yaw - 360;

        double ryaw = Math.toRadians(yaw);

        return (new Vector(Math.sin(ryaw), 0, Math.cos(ryaw)).multiply(velocity) );
    }

    public boolean isChunkLoaded(){
        Chunk chunk = getLocation().getChunk();
        return chunk != null && chunk.isLoaded();
    }

    public double getVelocity() {
        return velocity;
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
        this.yaw = yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public int getCraftMaxLength() {
        return craftMaxLength;
    }

    protected void setCraftMaxLength(int craftMaxLength) {
        this.craftMaxLength = craftMaxLength;
    }

    public int getCraftMaxWidth() {
        return craftMaxWidth;
    }

    protected void setCraftMaxWidth(int craftMaxWidth) {
        this.craftMaxWidth = craftMaxWidth;
    }

    public int getCraftMaxHeight() {
        return craftMaxHeight;
    }

    protected void setCraftMaxHeight(int craftMaxHeight) {
        this.craftMaxHeight = craftMaxHeight;
    }

    public boolean isEntityOnShip(Entity entity){
        if (entity == null || entity.getLocation() == null || entity.getLocation().getWorld() == null)
            return false;

        Location loc = entity.getLocation();
        //make the bounding box a little bit large if someone is peeking over the edge
        Location minBB = this.getCraftDesign().getMinBoundnigBoxLocation(this).subtract(1, 1, 1);
        Location maxBB = this.getCraftDesign().getMaxBoundnigBoxLocation(this).add(1, 1, 1);
        if (loc.getWorld().getUID().equals(this.world)){
            return loc.getX() >= minBB.getX() && loc.getY() >= minBB.getY() && loc.getZ() >= minBB.getZ() && loc.getX() <= maxBB.getX() && loc.getY() <= maxBB.getY() && loc.getZ() <= maxBB.getZ();
        }
        return false;
    }

    public boolean isBlockOnShip(Location loc){
        if (loc == null || loc.getWorld() == null)
            return false;

        //make the bounding box a little bit large if someone is peeking over the edge
        Location minBB = this.getCraftDesign().getMinBoundnigBoxLocation(this).subtract(1, 1, 1);
        Location maxBB = this.getCraftDesign().getMaxBoundnigBoxLocation(this).add(1, 1, 1);
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
}
