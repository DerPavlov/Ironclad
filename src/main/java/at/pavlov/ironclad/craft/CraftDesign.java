package at.pavlov.ironclad.craft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import at.pavlov.ironclad.container.SoundHolder;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;


import at.pavlov.ironclad.container.SimpleBlock;


public class CraftDesign
{
	//general
	private String designID;
	private String designName;
    private String messageName;
    private String description;
    private boolean lastUserBecomesOwner;

    private int craftMaxLength;
    private int craftMaxWidth;
    private int craftMaxHeight;
	
	//sign
	private boolean isSignRequired;

	//angles
	private BlockFace schematicDirection;
	private double maxHorizontalAngle;
	private double minHorizontalAngle;
	private double maxVerticalAngle;
	private double minVerticalAngle;
	private double angleStepSize;
	private double angleLargeStepSize;
	private int angleUpdateSpeed;
	private boolean angleUpdateMessage;

	//realistic behavior
	private double dismantlingDelay;

	//economy
	private double economyBuildingCost;
	private double economyDismantlingRefund;
	private double economyDestructionRefund;

	//permissions
	private String permissionBuild;
	private String permissionDismantle;
    private String permissionRename;
    private String permissionCruising;
	
	//accessRestriction
	private boolean accessForOwnerOnly;
	
	//allowedProjectile
	private List<String> allowedProjectiles;

    //sounds
    private SoundHolder soundCreate;
    private SoundHolder soundDestroy;
	private SoundHolder soundDismantle;
	private SoundHolder soundAngleChange;
	private SoundHolder soundMove;
	private SoundHolder soundSelected;
	private SoundHolder soundEnableCruisingMode;
	private SoundHolder soundDisableCruisingMode;

	
	//constructionblocks:
	private BlockState schematicBlockTypeIgnore;     				//this block this is ignored in the schematic file
    private BlockState schematicBlockTypeRotationCenter;			//location of the ship center
    private BlockState schematicBlockTypeEngine;					//blockdata of the engine
    private BlockState schematicBlockTypeChest;					//locations of the chest and sign
	private BlockState schematicBlockTypeSign;					//locations of the chest and sign
    private ArrayList<BlockState> schematicBlockTypeProtected;			//list of blocks that are protected from explosions (e.g. buttons)
    
    //craft design block lists for every direction (NORTH, EAST, SOUTH, WEST)
    private HashMap<BlockFace, CraftBlocks> cannonBlockMap = new HashMap<BlockFace, CraftBlocks>();

	/**
	 * returns real dimensions of the craft
	 * @return minimum block of the bounding box
	 */
	public BlockVector3 getCraftDimensions()
	{
		CraftBlocks cannonBlocks  = cannonBlockMap.get(BlockFace.NORTH);
		if (cannonBlocks != null) {
			return cannonBlocks.getMaxSize().add(BlockVector3.ONE).subtract(cannonBlocks.getMinSize());
		}
		System.out.println("[Ironclad] missing blocks for craft design");
		return null;
	}


	/**
	 * returns the minimum block location of the craft
	 * @param craft operated craft
	 * @return minimum block of the bounding box
	 */
	public BlockVector3 getMinBoundnigBoxLocation(Craft craft)
	{
		CraftBlocks cannonBlocks  = cannonBlockMap.get(craft.getCraftDirection());
		if (cannonBlocks != null)
		{
			return cannonBlocks.getMinSize().add(craft.getOffsetBlock());
		}

		System.out.println("[Ironclad] missing blocks for craft design " + craft.getCraftName());
		return null;
	}

	/**
	 * returns the maximum block location of the craft
	 * @param craft operated craft
	 * @return maximum block of the bounding box
	 */
	public BlockVector3 getMaxBoundnigBoxLocation(Craft craft)
	{
		CraftBlocks cannonBlocks  = cannonBlockMap.get(craft.getCraftDirection());
		if (cannonBlocks != null) {
			return cannonBlocks.getMaxSize().add(craft.getOffsetBlock());
		}

		System.out.println("[Ironclad] missing blocks for craft design " + craft.getCraftName());
		return null;
	}

	/**
	 * returns the center location of the craft
	 * @param craft operated craft
	 * @return center of the craft
	 */
	public Location getCraftCenter(Craft craft)
	{
		CraftBlocks cannonBlocks  = cannonBlockMap.get(craft.getCraftDirection());
		if (cannonBlocks != null) {
			System.out.println("CraftCenter" + cannonBlocks.getCraftCenter());
			return BukkitAdapter.adapt(craft.getWorldBukkit(), cannonBlocks.getCraftCenter().add(craft.getOffsetBlock()));
		}

		System.out.println("[Ironclad] missing location for craft design " + craft.getCraftName());
		return null;
	}


    /**
     * returns the rotation center location
     * @param craft operated craft
     * @return rotation center of the craft
     */
    public Location getRotationCenter(Craft craft)
    {
    	CraftBlocks cannonBlocks  = cannonBlockMap.get(craft.getCraftDirection());
    	if (cannonBlocks != null) {
    		return BukkitAdapter.adapt(craft.getWorldBukkit(), cannonBlocks.getRotationCenter().add(craft.getOffsetBlock()));
    	}

    	System.out.println("[Ironclad] missing location for craft design " + craft.getCraftName());
    	return null;
    }


    /**
     * returns a list of all cannonBlocks
     * @param cannonDirection - the direction the craft is facing
     * @return List of craft blocks
     */
    public List<SimpleBlock> getAllCraftBlocks(BlockFace cannonDirection)
    {
    	CraftBlocks cannonBlocks  = cannonBlockMap.get(cannonDirection);
    	if (cannonBlocks != null)
    	{
    		return cannonBlocks.getAllCraftBlocks();
    	}

    	return new ArrayList<SimpleBlock>();
    }


    /**
     * returns a list of all cannonBlocks
     * @param craft
     * @return
     */
    public List<SimpleBlock> getAllCraftBlocks(Craft craft)
    {
        CraftBlocks cannonBlocks  = cannonBlockMap.get(craft.getCraftDirection());
		ArrayList<SimpleBlock> blockList = new ArrayList<>();
        if (cannonBlocks != null) {
            for (SimpleBlock block : cannonBlocks.getAllCraftBlocks()) {
				blockList.add(block.clone().add(craft.getOffsetBlock()));
            }
        }
        return blockList;
    }

	/**
	 * returns a list of all cannonBlocks
	 * @param craft
	 * @return
	 */
	public List<Location> getAllCraftBlocksAfterMovement(Craft craft)
	{
		CraftBlocks cannonBlocks  = cannonBlockMap.get(craft.getFutureDirection());
		List<Location> locList = new ArrayList<>();
		if (cannonBlocks != null)
		{
			for (SimpleBlock block : cannonBlocks.getAllCraftBlocks())
			{
				BlockVector3 vect = block.toVector();
				locList.add(BukkitAdapter.adapt(craft.getWorldBukkit(), vect.add(craft.getOffsetBlock())));
			}
		}
		return locList;
	}

    /**
     * returns a list of all destructible blocks
     * @param craft
     * @return
     */
    public List<Location> getProtectedBlocks(Craft craft)
    {
     	CraftBlocks cannonBlocks  = cannonBlockMap.get(craft.getCraftDirection());
    	List<Location> locList = new ArrayList<Location>();
    	if (cannonBlocks != null)
    	{
    		for (BlockVector3 vect : cannonBlocks.getProtectedBlocks())
    		{
    			locList.add(BukkitAdapter.adapt(craft.getWorldBukkit(), vect.add(craft.getOffsetBlock())));
    		}
    	}
		return locList;
    }

    /**
     * returns a list of all hull blocks
     * @param craft
     * @return
     */
    public List<Location> getHullBlocks(Craft craft)
    {
        CraftBlocks cannonBlocks  = cannonBlockMap.get(craft.getCraftDirection());
        List<Location> locList = new ArrayList<Location>();
        if (cannonBlocks != null)
        {
            for (BlockVector3 vect : cannonBlocks.getHullBlocks())
            {
                locList.add(BukkitAdapter.adapt(craft.getWorldBukkit(), vect.add(craft.getOffsetBlock())));
            }
        }
        return locList;
    }

    /**
     * returns a list of all chest blocks
     * @param craft
     * @return
     */
    public List<Location> getChestLocations(Craft craft)
    {
    	CraftBlocks cannonBlocks  = cannonBlockMap.get(craft.getCraftDirection());
    	List<Location> locList = new ArrayList<Location>();
    	if (cannonBlocks != null) {
    		for (SimpleBlock block : cannonBlocks.getChest()) {
    			locList.add(block.toLocation(craft.getWorldBukkit(), craft.getOffsetBlock()));
    		}
    	}
		return locList;
    }


	/**
	 * returns a list of all sign blocks
	 * @param craft
	 * @return
	 */
	public List<Location> getSignLocations(Craft craft)
	{
		CraftBlocks cannonBlocks  = cannonBlockMap.get(craft.getCraftDirection());
		List<Location> locList = new ArrayList<Location>();
		if (cannonBlocks != null) {
			for (SimpleBlock block : cannonBlocks.getSign()) {
				locList.add(block.toLocation(craft.getWorldBukkit(), craft.getOffsetBlock()));
			}
		}
		return locList;
	}



	public String getDesignID()
	{
		return designID;
	}
	public void setDesignID(String designID)
	{
		this.designID = designID;
	}
	public String getDesignName()
	{
		return designName;
	}
	public void setDesignName(String designName)
	{
		this.designName = designName;
	}
	public boolean isSignRequired()
	{
		return isSignRequired;
	}
	public void setSignRequired(boolean isSignRequired)
	{
		this.isSignRequired = isSignRequired;
	}
	public boolean isAccessForOwnerOnly()
	{
		return accessForOwnerOnly;
	}
	public void setAccessForOwnerOnly(boolean accessForOwnerOnly)
	{
		this.accessForOwnerOnly = accessForOwnerOnly;
	}
	public List<String> getAllowedProjectiles()
	{
		return allowedProjectiles;
	}
	public void setAllowedProjectiles(List<String> allowedProjectiles)
	{
		this.allowedProjectiles = allowedProjectiles;
	}
	public BlockStateHolder getSchematicBlockTypeIgnore()
	{
		return schematicBlockTypeIgnore;
	}
	public void setSchematicBlockTypeIgnore(BlockState schematicBlockTypeIgnore)
	{
		this.schematicBlockTypeIgnore = schematicBlockTypeIgnore;
	}
	public BlockState getSchematicBlockTypeRotationCenter()
	{
		return schematicBlockTypeRotationCenter;
	}
	public void setSchematicBlockTypeRotationCenter(BlockState schematicBlockTypeRotationCenter)
	{
		this.schematicBlockTypeRotationCenter = schematicBlockTypeRotationCenter;
	}
	public BlockState getSchematicBlockTypeEngine()
	{
		return schematicBlockTypeEngine;
	}
	public void setSchematicBlockTypeEngine(BlockState schematicBlockTypeEngine)
	{
		this.schematicBlockTypeEngine = schematicBlockTypeEngine;
	}

	public HashMap<BlockFace, CraftBlocks> getCannonBlockMap()
	{
		return cannonBlockMap;
	}
	public void setCannonBlockMap(HashMap<BlockFace, CraftBlocks> cannonBlockMap)
	{
		this.cannonBlockMap = cannonBlockMap;
	}
	
	@Override
	public String toString()
	{
		return "designID:" + designID + " name:" + designName + " blocks:" + getAllCraftBlocks(BlockFace.NORTH).size();
	}


	public BlockState getSchematicBlockTypeChest()
	{
		return schematicBlockTypeChest;
	}


	public void setSchematicBlockTypeChest(BlockState schematicBlockTypeChest)
	{
		this.schematicBlockTypeChest = schematicBlockTypeChest;
	}

	public ArrayList<BlockState> getSchematicBlockTypeProtected()
	{
		return schematicBlockTypeProtected;
	}


	public void setSchematicBlockTypeProtected(ArrayList<BlockState> schematicBlockTypeProtected)
	{
		this.schematicBlockTypeProtected = schematicBlockTypeProtected;
	}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMessageName() {
        return messageName;
    }

    public void setMessageName(String messageName) {
        this.messageName = messageName;
    }

	public SoundHolder getSoundDismantle() {
		return soundDismantle;
	}

	public void setSoundDismantle(SoundHolder soundDismantle) {
		this.soundDismantle = soundDismantle;
	}

	public SoundHolder getSoundSelected() {
		return soundSelected;
	}

	public void setSoundSelected(SoundHolder soundSelected) {
		this.soundSelected = soundSelected;
	}

	public String getPermissionBuild() {
		return permissionBuild;
	}

	public void setPermissionBuild(String permissionBuild) {
		this.permissionBuild = permissionBuild;
	}

	public String getPermissionDismantle() {
		return permissionDismantle;
	}

	public void setPermissionDismantle(String permissionDismantle) {
		this.permissionDismantle = permissionDismantle;
	}

	public String getPermissionRename() {
		return permissionRename;
	}

	public void setPermissionRename(String permissionRename) {
		this.permissionRename = permissionRename;
	}

	public SoundHolder getSoundCreate() {
		return soundCreate;
	}

	public void setSoundCreate(SoundHolder soundCreate) {
		this.soundCreate = soundCreate;
	}

	public SoundHolder getSoundDestroy() {
		return soundDestroy;
	}

	public void setSoundDestroy(SoundHolder soundDestroy) {
		this.soundDestroy = soundDestroy;
	}

	public double getEconomyBuildingCost() {
		return economyBuildingCost;
	}

	public void setEconomyBuildingCost(double economyBuildingCost) {
		this.economyBuildingCost = economyBuildingCost;
	}

	public double getEconomyDismantlingRefund() {
		return economyDismantlingRefund;
	}

	public void setEconomyDismantlingRefund(double economyDismantlingRefund) {
		this.economyDismantlingRefund = economyDismantlingRefund;
	}

	public double getEconomyDestructionRefund() {
		return economyDestructionRefund;
	}

	public void setEconomyDestructionRefund(double economyDestructionRefund) {
		this.economyDestructionRefund = economyDestructionRefund;
	}

	public boolean isLastUserBecomesOwner() {
		return lastUserBecomesOwner;
	}

	public void setLastUserBecomesOwner(boolean lastUserBecomesOwner) {
		this.lastUserBecomesOwner = lastUserBecomesOwner;
	}

	public double getDismantlingDelay() {
		return dismantlingDelay;
	}

	public void setDismantlingDelay(double dismantlingDelay) {
		this.dismantlingDelay = dismantlingDelay;
	}

	public SoundHolder getSoundEnableCruisingMode() {
		return soundEnableCruisingMode;
	}

	public void setSoundEnableCruisingMode(SoundHolder soundEnableCruisingMode) {
		this.soundEnableCruisingMode = soundEnableCruisingMode;
	}

	public SoundHolder getSoundDisableCruisingMode() {
		return soundDisableCruisingMode;
	}

	public void setSoundDisableCruisingMode(SoundHolder soundDisableCruisingMode) {
		this.soundDisableCruisingMode = soundDisableCruisingMode;
	}

	public String getPermissionCruising() {
		return permissionCruising;
	}

	public void setPermissionCruising(String permissionCruising) {
		this.permissionCruising = permissionCruising;
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

	public BlockState getSchematicBlockTypeSign() {
		return schematicBlockTypeSign;
	}

	protected void setSchematicBlockTypeSign(BlockState schematicBlockTypeSign) {
		this.schematicBlockTypeSign = schematicBlockTypeSign;
	}

	public BlockFace getSchematicDirection() {
		return schematicDirection;
	}

	public void setSchematicDirection(BlockFace schematicDirection) {
		this.schematicDirection = schematicDirection;
	}

	public double getMaxHorizontalAngle() {
		return maxHorizontalAngle;
	}

	public void setMaxHorizontalAngle(double maxHorizontalAngle) {
		this.maxHorizontalAngle = maxHorizontalAngle;
	}

	public double getMinHorizontalAngle() {
		return minHorizontalAngle;
	}

	public void setMinHorizontalAngle(double minHorizontalAngle) {
		this.minHorizontalAngle = minHorizontalAngle;
	}

	public double getMaxVerticalAngle() {
		return maxVerticalAngle;
	}

	public void setMaxVerticalAngle(double maxVerticalAngle) {
		this.maxVerticalAngle = maxVerticalAngle;
	}

	public double getMinVerticalAngle() {
		return minVerticalAngle;
	}

	public void setMinVerticalAngle(double minVerticalAngle) {
		this.minVerticalAngle = minVerticalAngle;
	}

	public double getAngleStepSize() {
		return angleStepSize;
	}

	public void setAngleStepSize(double angleStepSize) {
		this.angleStepSize = angleStepSize;
	}

	public double getAngleLargeStepSize() {
		return angleLargeStepSize;
	}

	public void setAngleLargeStepSize(double angleLargeStepSize) {
		this.angleLargeStepSize = angleLargeStepSize;
	}

	public int getAngleUpdateSpeed() {
		return angleUpdateSpeed;
	}

	public void setAngleUpdateSpeed(int angleUpdateSpeed) {
		this.angleUpdateSpeed = angleUpdateSpeed;
	}

	public boolean isAngleUpdateMessage() {
		return angleUpdateMessage;
	}

	public void setAngleUpdateMessage(boolean angleUpdateMessage) {
		this.angleUpdateMessage = angleUpdateMessage;
	}

	public SoundHolder getSoundAngleChange() {
		return soundAngleChange;
	}

	public void setSoundAngleChange(SoundHolder soundAngleChange) {
		this.soundAngleChange = soundAngleChange;
	}

	public SoundHolder getSoundMove() {
		return soundMove;
	}

	public void setSoundMove(SoundHolder soundMove) {
		this.soundMove = soundMove;
	}
}
