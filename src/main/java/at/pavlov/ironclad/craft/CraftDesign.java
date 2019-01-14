package at.pavlov.ironclad.craft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import at.pavlov.ironclad.container.SoundHolder;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

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
    private String permissionPiloting;
	
	//accessRestriction
	private boolean accessForOwnerOnly;
	
	//allowedProjectile
	private List<String> allowedProjectiles;

    //sounds
    private SoundHolder soundCreate;
    private SoundHolder soundDestroy;
	private SoundHolder soundDismantle;
	private SoundHolder soundSelected;
	private SoundHolder soundEnablePilotingMode;
	private SoundHolder soundDisablePilotingMode;

	
	//constructionblocks:
	private BlockData schematicBlockTypeIgnore;     				//this block this is ignored in the schematic file
    private BlockData schematicBlockTypeRotationCenter;			//location of the ship center
    private BlockData schematicBlockTypeEngine;					//blockdata of the engine
    private BlockData schematicBlockTypeChest;					//locations of the chest and sign
	private BlockData schematicBlockTypeSign;					//locations of the chest and sign
    private List<BlockData> schematicBlockTypeProtected;			//list of blocks that are protected from explosions (e.g. buttons)
    
    //craft design block lists for every direction (NORTH, EAST, SOUTH, WEST)
    private HashMap<BlockFace, CraftBlocks> cannonBlockMap = new HashMap<BlockFace, CraftBlocks>();

	/**
	 * returns real dimensions of the craft
	 * @return minimum block of the bounding box
	 */
	public Vector getCraftDimensions()
	{
		CraftBlocks cannonBlocks  = cannonBlockMap.get(BlockFace.NORTH);
		if (cannonBlocks != null) {
			return cannonBlocks.getMaxSize().clone().add(new Vector(1,1,1)).subtract(cannonBlocks.getMinSize().clone());
		}
		System.out.println("[Ironclad] missing blocks for craft design");
		return null;
	}


	/**
	 * returns the minimum block location of the craft
	 * @param craft operated craft
	 * @return minimum block of the bounding box
	 */
	public Location getMinBoundnigBoxLocation(Craft craft)
	{
		CraftBlocks cannonBlocks  = cannonBlockMap.get(craft.getCraftDirection());
		if (cannonBlocks != null)
		{
			return cannonBlocks.getMinSize().clone().add(craft.getOffset()).toLocation(craft.getWorldBukkit());
		}

		System.out.println("[Ironclad] missing blocks for craft design " + craft.getCraftName());
		return craft.getOffset().toLocation(craft.getWorldBukkit());
	}

	/**
	 * returns the maximum block location of the craft
	 * @param craft operated craft
	 * @return maximum block of the bounding box
	 */
	public Location getMaxBoundnigBoxLocation(Craft craft)
	{
		CraftBlocks cannonBlocks  = cannonBlockMap.get(craft.getCraftDirection());
		if (cannonBlocks != null)
		{
			return cannonBlocks.getMaxSize().clone().add(craft.getOffset()).toLocation(craft.getWorldBukkit());
		}

		System.out.println("[Ironclad] missing blocks for craft design " + craft.getCraftName());
		return craft.getOffset().toLocation(craft.getWorldBukkit());
	}


	/**
	 * returns the center location of the craft
	 * @param craft operated craft
	 * @return center of the craft
	 */
	public Location getCraftCenter(Craft craft)
	{
		CraftBlocks cannonBlocks  = cannonBlockMap.get(craft.getCraftDirection());
		if (cannonBlocks != null)
		{
			return cannonBlocks.getCraftCenter().clone().add(craft.getOffset()).toLocation(craft.getWorldBukkit());
		}

		System.out.println("[Ironclad] missing location for craft design " + craft.getCraftName());
		return craft.getOffset().toLocation(craft.getWorldBukkit());
	}


    /**
     * returns the rotation center location
     * @param craft operated craft
     * @return rotation center of the craft
     */
    public Location getRotationCenter(Craft craft)
    {
    	CraftBlocks cannonBlocks  = cannonBlockMap.get(craft.getCraftDirection());
    	if (cannonBlocks != null)
    	{
    		return cannonBlocks.getRotationCenter().clone().add(craft.getOffset()).toLocation(craft.getWorldBukkit());
    	}

    	System.out.println("[Ironclad] missing location for craft design " + craft.getCraftName());
    	return craft.getOffset().toLocation(craft.getWorldBukkit());
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
				blockList.add(block.clone().add(craft.getOffset()));
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
		List<Location> locList = new ArrayList<Location>();
		if (cannonBlocks != null)
		{
			for (SimpleBlock block : cannonBlocks.getAllCraftBlocks())
			{
				Vector vect = block.toVector();
				locList.add(vect.clone().add(craft.getOffset()).toLocation(craft.getWorldBukkit()));
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
    		for (Vector vect : cannonBlocks.getProtectedBlocks())
    		{
    			locList.add(vect.clone().add(craft.getOffset()).toLocation(craft.getWorldBukkit()));
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
            for (Vector vect : cannonBlocks.getHullBlocks())
            {
                locList.add(vect.clone().add(craft.getOffset()).toLocation(craft.getWorldBukkit()));
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
    			locList.add(block.toLocation(craft.getWorldBukkit(), craft.getOffset()));
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
				locList.add(block.toLocation(craft.getWorldBukkit(), craft.getOffset()));
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
	public BlockData getSchematicBlockTypeIgnore()
	{
		return schematicBlockTypeIgnore;
	}
	public void setSchematicBlockTypeIgnore(BlockData schematicBlockTypeIgnore)
	{
		this.schematicBlockTypeIgnore = schematicBlockTypeIgnore;
	}
	public BlockData getSchematicBlockTypeRotationCenter()
	{
		return schematicBlockTypeRotationCenter;
	}
	public void setSchematicBlockTypeRotationCenter(BlockData schematicBlockTypeRotationCenter)
	{
		this.schematicBlockTypeRotationCenter = schematicBlockTypeRotationCenter;
	}
	public BlockData getSchematicBlockTypeEngine()
	{
		return schematicBlockTypeEngine;
	}
	public void setSchematicBlockTypeEngine(BlockData schematicBlockTypeEngine)
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


	public BlockData getSchematicBlockTypeChest()
	{
		return schematicBlockTypeChest;
	}


	public void setSchematicBlockTypeChest(BlockData schematicBlockTypeChest)
	{
		this.schematicBlockTypeChest = schematicBlockTypeChest;
	}

	public List<BlockData> getSchematicBlockTypeProtected()
	{
		return schematicBlockTypeProtected;
	}


	public void setSchematicBlockTypeProtected(List<BlockData> schematicBlockTypeProtected)
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

	public SoundHolder getSoundEnablePilotingMode() {
		return soundEnablePilotingMode;
	}

	public void setSoundEnablePilotingMode(SoundHolder soundEnablePilotingMode) {
		this.soundEnablePilotingMode = soundEnablePilotingMode;
	}

	public SoundHolder getSoundDisablePilotingMode() {
		return soundDisablePilotingMode;
	}

	public void setSoundDisablePilotingMode(SoundHolder soundDisablePilotingMode) {
		this.soundDisablePilotingMode = soundDisablePilotingMode;
	}

	public String getPermissionPiloting() {
		return permissionPiloting;
	}

	public void setPermissionPiloting(String permissionPiloting) {
		this.permissionPiloting = permissionPiloting;
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

	public BlockData getSchematicBlockTypeSign() {
		return schematicBlockTypeSign;
	}

	protected void setSchematicBlockTypeSign(BlockData schematicBlockTypeSign) {
		this.schematicBlockTypeSign = schematicBlockTypeSign;
	}

	public BlockFace getSchematicDirection() {
		return schematicDirection;
	}

	public void setSchematicDirection(BlockFace schematicDirection) {
		this.schematicDirection = schematicDirection;
	}
}
