package at.pavlov.ironclad.craft;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import at.pavlov.ironclad.utils.IroncladUtil;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.util.io.Closer;
import com.sk89q.worldedit.world.block.BlockState;

import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;




import at.pavlov.ironclad.container.SoundHolder;
import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.container.DesignFileName;
import at.pavlov.ironclad.container.SimpleBlock;
import at.pavlov.ironclad.utils.DesignComparator;

public class DesignStorage
{
	
	private final List<CraftDesign> craftsDesignList;
	private final Ironclad plugin;
	private final List<BlockMaterial> craftBlockMaterials;

	public DesignStorage(Ironclad ironclad)
	{
		plugin = ironclad;
		craftsDesignList = new ArrayList<CraftDesign>();
		craftBlockMaterials = new ArrayList<>();
	}

	/**
	 * returns a list of all craft design names
	 * @return list of all craft design names
	 */
	public ArrayList<String> getDesignIds(){
		ArrayList<String> list = new ArrayList<String>();
		for (CraftDesign design : craftsDesignList){
			list.add(design.getDesignID());
		}
		return list;
	}

	/**
	 * loads all custom craft desgins
	 */
	public void loadCraftDesigns()
	{
		plugin.logInfo("Loading craft designs");

		//clear designList before loading
		craftsDesignList.clear();
		
		// check if design folder is empty or does not exist
		if (IroncladUtil.isFolderEmpty(getPath()))
		{
			// the folder is empty, copy defaults
			plugin.logInfo("No craft designs loaded - loading default designs");
			copyDefaultDesigns();
		}

		ArrayList<DesignFileName> designFileList = getDesignFiles();

		// stop if there are no files found
		if (designFileList == null || designFileList.size() == 0)
			return;

		for (DesignFileName designFile : designFileList)
		{
			plugin.logDebug("loading craft " + designFile.getYmlString());
			CraftDesign craftDesign = new CraftDesign();
			//load .yml
			loadDesignYml(craftDesign, designFile.getYmlString());
			//load .shematic and add to list if valid
			if (loadDesignSchematic(craftDesign, designFile.getSchematicString()))
				craftsDesignList.add(craftDesign);
		}
		
		//sort the list so the designs with more craft blocks comes first
		//important if there is a design with one block less but else identically 
		Comparator<CraftDesign> comparator = new DesignComparator();
		craftsDesignList.sort(comparator);

		for (CraftDesign craftDesign : getCraftsDesignList()) {
			for (SimpleBlock sBlock : craftDesign.getAllCraftBlocks(BlockFace.NORTH)){
				BlockMaterial material = sBlock.getBlockState().getBlockType().getMaterial();
				if (!material.isAir() && !craftBlockMaterials.contains(material)) {
					craftBlockMaterials.add(sBlock.getBlockState().getBlockType().getMaterial());
				}
			}
		}


		for (CraftDesign design : craftsDesignList)
		{
			plugin.logDebug("design " + design.toString());
		}

	}

	/**
	 * returns a list with valid craft designs (.yml + .schem)
	 * 
	 * @return
	 */
	private ArrayList<DesignFileName> getDesignFiles()
	{
		ArrayList<DesignFileName> designList = new ArrayList<DesignFileName>();

		try
		{
			// check plugin/ironclad/designs for .yml and .schem files
			String ymlFile;
			File folder = new File(getPath());

			File[] listOfFiles = folder.listFiles();
            if (listOfFiles == null)
            {
                plugin.logSevere("Design folder empty");
                return designList;
            }


			for (File listOfFile : listOfFiles) {
				if (listOfFile.isFile()) {
					ymlFile = listOfFile.getName();
					if (ymlFile.endsWith(".yml") || ymlFile.endsWith(".yaml")) {
						String schematicFile = IroncladUtil.changeExtension(ymlFile, ".schematic");
						String schemFile = IroncladUtil.changeExtension(ymlFile, ".schem");
						if (new File(getPath() + schematicFile).isFile()) {
							// there is a shematic file and a .yml file
							designList.add(new DesignFileName(ymlFile, schematicFile));
						} else if (new File(getPath() + schemFile).isFile()) {
							// there is a shematic file and a .yml file
							designList.add(new DesignFileName(ymlFile, schemFile));
						} else {
							plugin.logSevere(schemFile + " is missing");
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			plugin.logSevere("Error while checking yml and schematic " + e);
		}
		return designList;
	}

	/**
	 * loads the config for one craft from the .yml file
     * @param craftDesign design of the craft
	 * @param ymlFile of the craft config file
	 */
	private void loadDesignYml(CraftDesign craftDesign, String ymlFile)
	{
		// load .yml file
		File craftDesignFile = new File(getPath() + ymlFile);
		FileConfiguration craftDesignConfig = YamlConfiguration.loadConfiguration(craftDesignFile);

		// load all entries of the config file

		// general
		craftDesign.setDesignID(IroncladUtil.removeExtension(ymlFile));
        craftDesign.setDesignName(craftDesignConfig.getString("general.designName", "no craftName"));
        craftDesign.setMessageName(craftDesignConfig.getString("general.messageName", "no messageName"));
        craftDesign.setDescription(craftDesignConfig.getString("general.description", "no description for this craft"));
		craftDesign.setLastUserBecomesOwner(craftDesignConfig.getBoolean("general.lastUserBecomesOwner", false));

		//size
		craftDesign.setCraftMaxLength(craftDesignConfig.getInt("size.length", 20));
		craftDesign.setCraftMaxWidth(craftDesignConfig.getInt("size.width", 10));
		craftDesign.setCraftMaxHeight(craftDesignConfig.getInt("size.height", 20));

		// sign
		craftDesign.setSignRequired(craftDesignConfig.getBoolean("signs.isSignRequired", false));

		// angles
		craftDesign.setSchematicDirection(BlockFace.valueOf(craftDesignConfig.getString("angles.schematicDirection", "NORTH").toUpperCase()));
		craftDesign.setMaxHorizontalAngle(craftDesignConfig.getDouble("angles.maxHorizontalAngle", 20.0));
		craftDesign.setMinHorizontalAngle(craftDesignConfig.getDouble("angles.minHorizontalAngle", -20.0));
		craftDesign.setMaxVerticalAngle(craftDesignConfig.getDouble("angles.maxVerticalAngle", 10.0));
		craftDesign.setMinVerticalAngle(craftDesignConfig.getDouble("angles.minVerticalAngle", -10.0));
		craftDesign.setAngleStepSize(craftDesignConfig.getDouble("angles.angleStepSize", 0.1));
		craftDesign.setAngleLargeStepSize(craftDesignConfig.getDouble("angles.largeStepSize", 1.0));
		craftDesign.setAngleUpdateSpeed((int) (craftDesignConfig.getDouble("angles.angleUpdateSpeed", 1.0) * 1000.0));
		craftDesign.setAngleUpdateMessage(craftDesignConfig.getBoolean("angles.angleUpdateMessage", false));


		//realistic behavior
		craftDesign.setDismantlingDelay(craftDesignConfig.getDouble("realisticBehaviour.dismantlingDelay", 1.75));

        //economy
        craftDesign.setEconomyBuildingCost(craftDesignConfig.getDouble("economy.buildingCosts", 0.0));
        craftDesign.setEconomyDismantlingRefund(craftDesignConfig.getDouble("economy.dismantlingRefund", 0.0));
        craftDesign.setEconomyDestructionRefund(craftDesignConfig.getDouble("economy.destructionRefund", 0.0));


		// permissions
		craftDesign.setPermissionBuild(craftDesignConfig.getString("permissions.build", "ironclad.player.build"));
		craftDesign.setPermissionDismantle(craftDesignConfig.getString("permissions.dismantle", "ironclad.player.dismantle"));
		craftDesign.setPermissionRename(craftDesignConfig.getString("permissions.cruising", "ironclad.player.cruising"));
        craftDesign.setPermissionRename(craftDesignConfig.getString("permissions.rename", "ironclad.player.rename"));

		// accessRestriction
		craftDesign.setAccessForOwnerOnly(craftDesignConfig.getBoolean("accessRestriction.ownerOnly", false));

        // sounds
        craftDesign.setSoundCreate(new SoundHolder(craftDesignConfig.getString("sounds.create","BLOCK_ANVIL_LAND:1:0.5")));
        craftDesign.setSoundDestroy(new SoundHolder(craftDesignConfig.getString("sounds.destroy","ENTITY_ZOMBIE_ATTACK_IRON_DOOR:1:0.5")));
        craftDesign.setSoundDismantle(new SoundHolder(craftDesignConfig.getString("sounds.dismantle", "BLOCK_ANVIL_USE:1:0.5")));
		craftDesign.setSoundAngleChange(new SoundHolder(craftDesignConfig.getString("sounds.angleChange","ENTITY_IRON_GOLEM_STEP:1:0.5")));
		craftDesign.setSoundMove(new SoundHolder(craftDesignConfig.getString("sounds.move","BLOCK_ANVIL_LAND:5:1")));
		craftDesign.setSoundSelected(new SoundHolder(craftDesignConfig.getString("sounds.selected","BLOCK_ANVIL_LAND:1:2")));
		craftDesign.setSoundEnableCruisingMode(new SoundHolder(craftDesignConfig.getString("sounds.enableCruisingMode","NONE:1:1")));
		craftDesign.setSoundDisableCruisingMode(new SoundHolder(craftDesignConfig.getString("sounds.disableCruisingMode","NONE:1:1")));


		// constructionBlocks
		craftDesign.setSchematicBlockTypeIgnore(IroncladUtil.createBlockData(craftDesignConfig.getString("constructionBlocks.ignore", "minecraft:sand")));
		craftDesign.setSchematicBlockTypeRotationCenter(IroncladUtil.createBlockData(craftDesignConfig.getString("constructionBlocks.rotationCenter", "minecraft:snow_block")));
		craftDesign.setSchematicBlockTypeEngine(IroncladUtil.createBlockData(craftDesignConfig.getString("constructionBlocks.engine", "minecraft:furnace")));
		craftDesign.setSchematicBlockTypeChest(IroncladUtil.createBlockData(craftDesignConfig.getString("constructionBlocks.chest", "minecraft:chest")));
		craftDesign.setSchematicBlockTypeSign(IroncladUtil.createBlockData(craftDesignConfig.getString("constructionBlocks.sign", "minecraft:wall_sign")));
		// protected Blocks
		craftDesign.setSchematicBlockTypeProtected(IroncladUtil.toBlockDataList(craftDesignConfig.getStringList("constructionBlocks.protectedBlocks")));
	}

	/**
	 * loads the schematic of the config file
	 * @param craftDesign design of the craft
	 * @param schematicFile path of the schematic file
	 */
	private boolean loadDesignSchematic(CraftDesign craftDesign, String schematicFile)
	{
        long startTime = System.nanoTime();
		
		// load schematic with worldedit
        Clipboard cc;
        File f = new File(getPath() + schematicFile);
		ClipboardFormat format = ClipboardFormats.findByFile(f);
		try (Closer closer = Closer.create()) {
			FileInputStream fis = closer.register(new FileInputStream(f));
			BufferedInputStream bis = closer.register(new BufferedInputStream(fis));
			ClipboardReader reader = closer.register(format.getReader(bis));

			cc = reader.read();
		} catch (IOException e) {
			plugin.logSevere("Error while loading schematic " + getPath() + schematicFile + " :" + e  + "; does file exist: " + f.exists());
			return false;
		}
		//failed to load schematic
		if (cc == null) 
		{
			plugin.logSevere("Failed to loading schematic");
			return false;
		}

		AffineTransform transform = new AffineTransform().translate(cc.getMinimumPoint().multiply(-1));
		BlockTransformExtent extent = new BlockTransformExtent(cc, transform);
		ForwardExtentCopy copy = new ForwardExtentCopy(extent, cc.getRegion(), cc.getOrigin(), cc, BlockVector3.ZERO);
		copy.setTransform(transform);
        try {
            Operations.complete(copy);
        } catch (WorldEditException e) {
            e.printStackTrace();
        }

		// convert all schematic blocks from the config to BaseBlocks so they
		// can be rotated
		BlockStateHolder blockIgnore = craftDesign.getSchematicBlockTypeIgnore();
		BlockState blockRotationCenter = craftDesign.getSchematicBlockTypeRotationCenter();
		BlockState blockEngine = craftDesign.getSchematicBlockTypeEngine();
		BlockState blockChest = craftDesign.getSchematicBlockTypeChest();
		BlockState blockSign = craftDesign.getSchematicBlockTypeSign();
        List<BlockState> blockProtectedList = new ArrayList<BlockState>(craftDesign.getSchematicBlockTypeProtected());
		
		
		// get facing of the craft
		BlockFace craftDirection = craftDesign.getSchematicDirection();

		// read out blocks
		int width = cc.getDimensions().getBlockX();
		int height = cc.getDimensions().getBlockY();
		int length = cc.getDimensions().getBlockZ();

		cc.setOrigin(BlockVector3.ZERO);

        ArrayList<SimpleBlock> schematiclist = new ArrayList<>();
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				for (int z = 0; z < length; ++z) {

					BlockVector3 pt = BlockVector3.at(x, y, z);
					BlockState blockState = cc.getBlock(pt.add(cc.getMinimumPoint()));
					//plugin.logDebug("blockstate: " + blockState.getAsString());

					// ignore if block is AIR, liquid or the IgnoreBlock type
					if (!blockState.getBlockType().getMaterial().isAir() && !blockState.getBlockType().getMaterial().isLiquid() && !blockState.getBlockType().getMaterial().equals(blockIgnore)) {
						schematiclist.add(new SimpleBlock(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ(), blockState));
					}
				}
			}
		}

		for (int i = 0; i < 4; i++)
		{
			// create CraftBlocks entry
            CraftBlocks craftBlocks = new CraftBlocks();

			// to set the muzzle location the maximum and mininum x, y, z values
			// of all size blocks have to be found
            BlockVector3 minSize = BlockVector3.ZERO;
            BlockVector3 maxSize = BlockVector3.ZERO;
			boolean firstEntrySize = true;

			// to set the rotation Center maximum and mininum x, y, z values
			// of all rotation blocks have to be found
			// setting max to the size of the marked area is a good approximation
			// if no rotationblock is given
            BlockVector3 minRotation = null;
            BlockVector3 maxRotation = null;
			boolean firstEntryRotation = true;

            for (SimpleBlock sblock : schematiclist) {
				int x = sblock.getLocX();
				int y = sblock.getLocY();
				int z = sblock.getLocZ();

				// #############  find the min and max for rotation blocks
				if (sblock.compareMaterial(blockRotationCenter))
				{
					// reset for the first entry
					if (firstEntryRotation)
					{
						firstEntryRotation = false;
						minRotation = BlockVector3.at(x, y, z);
						maxRotation= BlockVector3.at(x, y, z);
					}
					else
					{
                        minRotation = findMinimum(x, y, z, minRotation);
                        maxRotation = findMaximum(x, y, z, maxRotation);
					}
				}
				else {
					//all craft blocks. Ignore the rotation center
					craftBlocks.getAllCraftBlocks().add(new SimpleBlock(x, y, z, sblock.getBlockState()));
					// this can be a destructible block
					if (isInList(blockProtectedList, sblock.getBlockState()))
						craftBlocks.getProtectedBlocks().add(BlockVector3.at(x, y, z));

					// #############  find the min and max for the craft
					// reset for the first entry
					if (firstEntrySize) {
						firstEntrySize = false;
						minSize = BlockVector3.at(width, height, length);
						maxSize = BlockVector3.at(width, height, length);
					} else {
                        minSize = findMinimum(x, y, z, minSize);
                        maxSize= findMaximum(x, y, z, maxSize);
					}
					// #############  engines ########################
					if (sblock.compareMaterial(blockEngine)) {
						// the id does not matter
						craftBlocks.getEngines().add(new SimpleBlock(x, y, z, sblock.getBlockState()));
					}
					// #############  chests ########################
					else if (sblock.compareMaterial(blockChest)) {
						// the id does not matter
						craftBlocks.getChest().add(new SimpleBlock(x, y, z, sblock.getBlockState()));
					}
					// #############  sign ########################
					else if (sblock.compareMaterial(blockSign)) {
						// the id does not matter, but the data is important for signs
						craftBlocks.getSign().add(new SimpleBlock(x, y, z, sblock.getBlockState()));
					}
					// #############  hull of the ship
					else {
						// all remaining blocks are loading interface or craftBlocks
						craftBlocks.getHullBlocks().add(BlockVector3.at(x, y, z));
					}
				}
            }

			// calculate the muzzle location
			//maxSize.add(new Vector(1, 1, 1));
			craftBlocks.setMinSize(minSize);
			craftBlocks.setMaxSize(maxSize);

			//craft center
			BlockVector3 center = maxSize.add(BlockVector3.ONE);
			craftBlocks.setCraftCenter(center.add(minSize).divide(2));

			// calculate the rotation Center if a rotation center block was used, otherwise use the center of the craft
			if (maxRotation != null){
				craftBlocks.setRotationCenter(maxRotation.add(BlockVector3.ONE).add(minRotation).divide(2));

			}
			else {
				craftBlocks.setRotationCenter(craftBlocks.getCraftCenter());
			}
			//set rotation center above Craft for visual effects
			BlockVector3 rotation = craftBlocks.getRotationCenter();
			craftBlocks.setRotationCenter(BlockVector3.at(rotation.getX(), maxSize.getY()+1,rotation.getZ()));

            //set the center location to Zero
            BlockVector3 compensation = BlockVector3.at(craftBlocks.getRotationCenter().getX(), craftBlocks.getRotationCenter().getY(), craftBlocks.getRotationCenter().getZ());

            for (SimpleBlock block : craftBlocks.getAllCraftBlocks())
                block.subtract_noCopy(compensation);
            craftBlocks.setHullBlocks(IroncladUtil.subtractBlockVectorList(craftBlocks.getHullBlocks(), compensation));
            for (SimpleBlock block : craftBlocks.getChest())
                block.subtract_noCopy(compensation);
			for (SimpleBlock block : craftBlocks.getSign())
				block.subtract_noCopy(compensation);
			for (SimpleBlock block : craftBlocks.getEngines())
				block.subtract_noCopy(compensation);
			craftBlocks.setProtectedBlocks(IroncladUtil.subtractBlockVectorList(craftBlocks.getProtectedBlocks(), compensation));
            craftBlocks.setMinSize(craftBlocks.getMinSize().subtract(compensation));
            craftBlocks.setMaxSize(craftBlocks.getMaxSize().subtract(compensation));
            craftBlocks.setRotationCenter(craftBlocks.getRotationCenter().subtract(compensation));
            craftBlocks.setCraftCenter(craftBlocks.getCraftCenter().subtract(compensation));

			// add blocks to the HashMap
			craftDesign.getCannonBlockMap().put(craftDirection, craftBlocks);

			//rotate blocks for the next iteration
			IroncladUtil.roateBlockFacingClockwise(blockRotationCenter);
			IroncladUtil.roateBlockFacingClockwise(blockEngine);
            IroncladUtil.roateBlockFacingClockwise(blockChest);
            IroncladUtil.roateBlockFacingClockwise(blockSign);
			for (BlockState aBlockProtectedList : blockProtectedList) {
                IroncladUtil.roateBlockFacingClockwise(aBlockProtectedList);
			}

			//rotate schematic blocks
			for (SimpleBlock simpleBlock : schematiclist){
				simpleBlock.rotate90();
			}

            //rotate craftDirection
			craftDirection = IroncladUtil.roatateFace(craftDirection);



		}
        plugin.logDebug("Time to load designs: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");

        return true;

	}

	private BlockVector3 findMinimum(int x, int y, int z, BlockVector3 min)
	{
		if (x < min.getBlockX())
			min = min.withX(x);
		if (y < min.getBlockY())
            min = min.withY(y);
		if (z < min.getBlockZ())
            min = min.withZ(z);

		return min;
	}

	private BlockVector3 findMaximum(int x, int y, int z, BlockVector3 max)
	{
		if (x > max.getBlockX())
            max = max.withX(x);
		if (y > max.getBlockY())
            max = max.withY(y);
		if (z > max.getBlockZ())
            max = max.withZ(z);

		return max;
	}

	/**
	 * copy the default designs from the .jar to the disk
	 */
	private void copyDefaultDesigns()
	{
		copyFile("frigate");
		copyFile("galleon");
		copyFile("manowar");
		copyFile("indiaman");
	}

    /**
     * Copys the given .yml and .schematic from the .jar to the disk
     * @param fileName - name of the design file
     */
    private void copyFile(String fileName)
    {
        File YmlFile = new File(plugin.getDataFolder(), "designs/" + fileName + ".yml");
        File SchematicFile = new File(plugin.getDataFolder(), "designs/" + fileName + ".schem");

        SchematicFile.getParentFile().mkdirs();
        if (!YmlFile.exists())
        {
            IroncladUtil.copyFile(plugin.getResource("designs/" + fileName + ".yml"), YmlFile);
        }
        if (!SchematicFile.exists())
        {
            IroncladUtil.copyFile(plugin.getResource("designs/" + fileName + ".schem"), SchematicFile);
        }
    }
	
	private boolean isInList(List<BlockState> list, BlockStateHolder block)
	{
		if (block == null) return true;
		
		for (BlockStateHolder listBlock : list)
		{
			if (listBlock != null && listBlock.getBlockType().getMaterial().equals(block.getBlockType().getMaterial()))
				return true;
		}
		return false;
	}
	
	private String getPath()
	{
		// Directory path here
		return "plugins/Ironclad/designs/";
	}
	
	public List<CraftDesign> getCraftsDesignList()
	{
		return craftsDesignList;
	}
	
	/**
	 * returns the craft design of the craft
	 * @param craft the craft
	 * @return design of craft
	 */
	public CraftDesign getDesign(Craft craft)
	{
		return getDesign(craft.getDesignID());
	}
	
	/**
	 * returns the craft design by its id
	 * @param designId Name of the design
	 * @return craft design
	 */
	public CraftDesign getDesign(String designId)
	{
		for (CraftDesign craftDesign : craftsDesignList)
		{
			if (craftDesign.getDesignID().equals(designId))
				return craftDesign;
		}
		return null;
	}

	/**
	 * is there a craft design with the give name
	 * @param name name of the design
	 * @return true if there is a craft design with this name
     */
	public boolean hasDesign(String name){
		for (CraftDesign design : craftsDesignList){
			if (design.getDesignID().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	public List<BlockMaterial> getCraftBlockMaterials() {
		return craftBlockMaterials;
	}

	public boolean isCraftBlockMaterial(BlockMaterial material) {
		return !material.isAir() && craftBlockMaterials.contains(material);
	}
}
