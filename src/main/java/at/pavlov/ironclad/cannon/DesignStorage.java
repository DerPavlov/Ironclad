package at.pavlov.ironclad.cannon;

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

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;




import at.pavlov.ironclad.container.SoundHolder;
import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.container.DesignFileName;
import at.pavlov.ironclad.container.SimpleBlock;
import at.pavlov.ironclad.utils.DesignComparator;

public class DesignStorage
{
	
	private final List<CraftDesign> craftsDesignList;
	private final Ironclad plugin;
	private final List<Material> craftBlockMaterials;

	public DesignStorage(Ironclad ironclad)
	{
		plugin = ironclad;
		craftsDesignList = new ArrayList<CraftDesign>();
		craftBlockMaterials = new ArrayList<>();
	}

	/**
	 * returns a list of all cannon design names
	 * @return list of all cannon design names
	 */
	public ArrayList<String> getDesignIds(){
		ArrayList<String> list = new ArrayList<String>();
		for (CraftDesign design : craftsDesignList){
			list.add(design.getDesignID());
		}
		return list;
	}

	/**
	 * loads all custom cannon desgins
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
			CraftDesign cannonDesign = new CraftDesign();
			//load .yml
			loadDesignYml(cannonDesign, designFile.getYmlString());
			//load .shematic and add to list if valid
			if (loadDesignSchematic(cannonDesign, designFile.getSchematicString()))
				craftsDesignList.add(cannonDesign);
		}
		
		//sort the list so the designs with more cannon blocks comes first
		//important if there is a design with one block less but else identically 
		Comparator<CraftDesign> comparator = new DesignComparator();
		craftsDesignList.sort(comparator);

		for (CraftDesign cannonDesign : getCraftsDesignList()) {
			for (SimpleBlock sBlock : cannonDesign.getAllCraftBlocks(BlockFace.NORTH)){
				Material material = sBlock.getBlockData().getMaterial();
				if (material != Material.AIR && !craftBlockMaterials.contains(material)) {
					craftBlockMaterials.add(sBlock.getBlockData().getMaterial());
				}
			}
		}


		for (CraftDesign design : craftsDesignList)
		{
			plugin.logDebug("design " + design.toString());
		}

	}

	/**
	 * returns a list with valid cannon designs (.yml + .schematic)
	 * 
	 * @return
	 */
	private ArrayList<DesignFileName> getDesignFiles()
	{
		ArrayList<DesignFileName> designList = new ArrayList<DesignFileName>();

		try
		{
			// check plugin/ironclad/designs for .yml and .schematic files
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
						if (new File(getPath() + schematicFile).isFile()) {
							// there is a shematic file and a .yml file
							designList.add(new DesignFileName(ymlFile, schematicFile));
						} else {
							plugin.logSevere(schematicFile + " is missing");
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
	 * loads the config for one cannon from the .yml file
     * @param craftDesign design of the cannon
	 * @param ymlFile of the cannon config file
	 */
	private void loadDesignYml(CraftDesign craftDesign, String ymlFile)
	{
		// load .yml file
		File cannonDesignFile = new File(getPath() + ymlFile);
		FileConfiguration craftDesignConfig = YamlConfiguration.loadConfiguration(cannonDesignFile);

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
		craftDesign.setDefaultHorizontalFacing(BlockFace.valueOf(craftDesignConfig.getString("angles.defaultHorizontalFacing", "NORTH").toUpperCase()));

		//realistic behavior
		craftDesign.setDismantlingDelay(craftDesignConfig.getDouble("realisticBehaviour.dismantlingDelay", 1.75));

        //economy
        craftDesign.setEconomyBuildingCost(craftDesignConfig.getDouble("economy.buildingCosts", 0.0));
        craftDesign.setEconomyDismantlingRefund(craftDesignConfig.getDouble("economy.dismantlingRefund", 0.0));
        craftDesign.setEconomyDestructionRefund(craftDesignConfig.getDouble("economy.destructionRefund", 0.0));


		// permissions
		craftDesign.setPermissionBuild(craftDesignConfig.getString("permissions.build", "ironclad.player.build"));
		craftDesign.setPermissionDismantle(craftDesignConfig.getString("permissions.dismantle", "ironclad.player.dismantle"));
		craftDesign.setPermissionRename(craftDesignConfig.getString("permissions.piloting", "ironclad.player.piloting"));
        craftDesign.setPermissionRename(craftDesignConfig.getString("permissions.rename", "ironclad.player.rename"));

		// accessRestriction
		craftDesign.setAccessForOwnerOnly(craftDesignConfig.getBoolean("accessRestriction.ownerOnly", false));

        // sounds
        craftDesign.setSoundCreate(new SoundHolder(craftDesignConfig.getString("sounds.create","BLOCK_ANVIL_LAND:1:0.5")));
        craftDesign.setSoundDestroy(new SoundHolder(craftDesignConfig.getString("sounds.destroy","ENTITY_ZOMBIE_ATTACK_IRON_DOOR:1:0.5")));
        craftDesign.setSoundDismantle(new SoundHolder(craftDesignConfig.getString("sounds.dismantle", "BLOCK_ANVIL_USE:1:0.5")));
		craftDesign.setSoundSelected(new SoundHolder(craftDesignConfig.getString("sounds.selected","BLOCK_ANVIL_LAND:1:2")));
		craftDesign.setSoundEnablePilotingMode(new SoundHolder(craftDesignConfig.getString("sounds.enablePilotingMode","NONE:1:1")));
		craftDesign.setSoundDisablePilotingMode(new SoundHolder(craftDesignConfig.getString("sounds.disablePilotingMode","NONE:1:1")));


		// constructionBlocks
		craftDesign.setSchematicBlockTypeIgnore(IroncladUtil.createBlockData(craftDesignConfig.getString("constructionBlocks.ignore", "minecraft:sand")));
		craftDesign.setSchematicBlockTypeRotationCenter(IroncladUtil.createBlockData(craftDesignConfig.getString("constructionBlocks.rotationCenter", "minecraft:snow_block")));
		craftDesign.setSchematicBlockTypeEngine(IroncladUtil.createBlockData(craftDesignConfig.getString("constructionBlocks.engine", "minecraft:furnace")));
		craftDesign.setSchematicBlockTypeChest(IroncladUtil.createBlockData(craftDesignConfig.getString("constructionBlocks.chests", "minecraft:chest")));
		// protected Blocks
		craftDesign.setSchematicBlockTypeProtected(IroncladUtil.toBlockDataList(craftDesignConfig.getStringList("constructionBlocks.protectedBlocks")));
	}

	/**
	 * loads the schematic of the config file
	 * @param cannonDesign design of the cannon
	 * @param schematicFile path of the schematic file
	 */
	private boolean loadDesignSchematic(CraftDesign cannonDesign, String schematicFile)
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

		BlockVector3 ve = cc.getMinimumPoint().multiply();

        AffineTransform transform = new AffineTransform().translate(cc.getMinimumPoint().multiply(-1)).rotateY(90);
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
		BlockData blockIgnore = cannonDesign.getSchematicBlockTypeIgnore();
		BlockData blockRotationCenter = cannonDesign.getSchematicBlockTypeRotationCenter();
		BlockData blockEngine = cannonDesign.getSchematicBlockTypeEngine();
		BlockData blockChest = cannonDesign.getSchematicBlockTypeChest();
        List<BlockData> blockProtectedList = new ArrayList<BlockData>(cannonDesign.getSchematicBlockTypeProtected());
		
		
		// get facing of the cannon
		BlockFace cannonDirection = cannonDesign.getDefaultHorizontalFacing();

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

					BlockData block = Bukkit.getServer().createBlockData(blockState.getAsString());


					// ignore if block is AIR, liquid or the IgnoreBlock type
					if (!block.getMaterial().equals(Material.AIR) && !block.getMaterial().isSolid() && !block.matches(blockIgnore)) {
						schematiclist.add(new SimpleBlock(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ(), block));
					}
				}
			}
		}

		for (int i = 0; i < 4; i++)
		{
			// create CraftBlocks entry
            CraftBlocks cannonBlocks = new CraftBlocks();

			// to set the muzzle location the maximum and mininum x, y, z values
			// of all muzzle blocks have to be found
			Vector minSize = new Vector(0, 0, 0);
			Vector maxSize = new Vector(0, 0, 0);
			boolean firstEntrySize = true;

			// to set the rotation Center maximum and mininum x, y, z values
			// of all rotation blocks have to be found
			// setting max to the size of the marked area is a good approximation
			// if no rotationblock is given
			Vector minRotation = new Vector(0, 0, 0);
			Vector maxRotation = new Vector(width, height, length);
			boolean firstEntryRotation = true;

            for (SimpleBlock sblock : schematiclist) {
                int x = sblock.getLocX();
                int y = sblock.getLocY();
                int z = sblock.getLocZ();

                // #############  find the min and max for the craft
				// reset for the first entry
				if (firstEntrySize)
				{
					firstEntrySize = false;
					minSize = new Vector(x, y, z);
					maxSize = new Vector(x, y, z);
				}
				else
				{
					minSize = findMinimum(x, y, z, minSize);
					maxSize = findMaximum(x, y, z, maxSize);
				}
				//muzzle blocks need to be air - else the projectile would spawn in a block
				cannonBlocks.getAllCannonBlocks().add(new SimpleBlock(x, y, z, Material.AIR));
                // #############  find the min and max for rotation blocks
                if (sblock.compareMaterial(blockRotationCenter))
                {
                    // reset for the first entry
                    if (firstEntryRotation)
                    {
                        firstEntryRotation = false;
                        minRotation = new Vector(x, y, z);
                        maxRotation= new Vector(x, y, z);
                    }
                    else
                    {
                        minRotation = findMinimum(x, y, z, minRotation);
                        maxRotation = findMaximum(x, y, z, maxRotation);
                    }
                }
				// #############  engines
				else if (sblock.compareMaterial(blockEngine))
				{
					// the id does not matter
					cannonBlocks.getEngines().add(new SimpleBlock(x, y, z, Material.FURNACE));
				}
                // #############  chests
                else if (sblock.compareMaterial(blockChest))
                {
                    // the id does not matter, but the data is important for signs
                    cannonBlocks.getChests().add(new SimpleBlock(x, y, z, Material.WALL_SIGN));
                }
                // #############  loading Interface is a cannonblock that is non of
                // the previous blocks
                else
                {
                    // all remaining blocks are loading interface or cannonBlocks
                    cannonBlocks.getHullBlocks().add(new Vector(x, y, z));
                    cannonBlocks.getAllCannonBlocks().add(new SimpleBlock(x, y, z, sblock.getBlockData()));
                    // this can be a destructible block
                    if (!isInList(blockProtectedList, sblock.getBlockData()))
                        cannonBlocks.getDestructibleBlocks().add(new Vector(x, y, z));
                }
            }

			// calculate the muzzle location
			//maxSize.add(new Vector(1, 1, 1));
			cannonBlocks.setMaxSize(minSize);
			cannonBlocks.setMaxSize(maxSize);

			// calculate the rotation Center
			maxRotation.add(new Vector(1, 1, 1));
			cannonBlocks.setRotationCenter(maxRotation.add(maxRotation).multiply(0.5));

            //set the muzzle location
            Vector compensation = new Vector(cannonBlocks.getRotationCenter().getBlockX(), cannonBlocks.getRotationCenter().getBlockY(), cannonBlocks.getRotationCenter().getBlockZ());

            for (SimpleBlock block : cannonBlocks.getAllCannonBlocks())
                block.subtract_noCopy(compensation);
            for (Vector block : cannonBlocks.getHullBlocks())
                block.subtract(compensation);
            for (SimpleBlock block : cannonBlocks.getChests())
                block.subtract_noCopy(compensation);
			for (SimpleBlock block : cannonBlocks.getEngines())
				block.subtract_noCopy(compensation);
            for (Vector block : cannonBlocks.getDestructibleBlocks())
                block.subtract(compensation);
            cannonBlocks.getMinSize().subtract(compensation);
			cannonBlocks.getMaxSize().subtract(compensation);
            cannonBlocks.getRotationCenter().subtract(compensation);

			// add blocks to the HashMap
			cannonDesign.getCannonBlockMap().put(cannonDirection, cannonBlocks);

			//rotate blocks for the next iteration
            IroncladUtil.roateBlockFacingClockwise(blockIgnore);
            IroncladUtil.roateBlockFacingClockwise(blockEngine);
            IroncladUtil.roateBlockFacingClockwise(blockEngine);
            IroncladUtil.roateBlockFacingClockwise(blockChest);
			for (BlockData aBlockProtectedList : blockProtectedList) {
                IroncladUtil.roateBlockFacingClockwise(aBlockProtectedList);
			}

			//rotate schematic blocks
			for (SimpleBlock simpleBlock : schematiclist){
				simpleBlock.rotate90();
			}

            //rotate cannonDirection
			cannonDirection = IroncladUtil.roatateFace(cannonDirection);


		}
        plugin.logDebug("Time to load designs: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");

        return true;
	}

	private Vector findMinimum(int x, int y, int z, Vector min)
	{
		if (x < min.getBlockX())
			min.setX(x);
		if (y < min.getBlockY())
			min.setY(y);
		if (z < min.getBlockZ())
			min.setZ(z);

		return min;
	}

	private Vector findMaximum(int x, int y, int z, Vector max)
	{
		if (x > max.getBlockX())
			max.setX(x);
		if (y > max.getBlockY())
			max.setY(y);
		if (z > max.getBlockZ())
			max.setZ(z);

		return max;
	}

	/**
	 * copy the default designs from the .jar to the disk
	 */
	private void copyDefaultDesigns()
	{
		copyFile("classic");
        copyFile("mortar");
        copyFile("ironCannon");
		copyFile("sentry");
	}

    /**
     * Copys the given .yml and .schematic from the .jar to the disk
     * @param fileName - name of the design file
     */
    private void copyFile(String fileName)
    {
        File YmlFile = new File(plugin.getDataFolder(), "designs/" + fileName + ".yml");
        File SchematicFile = new File(plugin.getDataFolder(), "designs/" + fileName + ".schematic");

        SchematicFile.getParentFile().mkdirs();
        if (!YmlFile.exists())
        {
            IroncladUtil.copyFile(plugin.getResource("designs/" + fileName + ".yml"), YmlFile);
        }
        if (!SchematicFile.exists())
        {
            IroncladUtil.copyFile(plugin.getResource("designs/" + fileName + ".schematic"), SchematicFile);
        }
    }
	
	private boolean isInList(List<BlockData> list, BlockData block)
	{
		if (block == null) return true;
		
		for (BlockData listBlock : list)
		{
			if (listBlock != null && listBlock.getMaterial().equals(block.getMaterial()))
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
	 * returns the cannon design by its id
	 * @param designId Name of the design
	 * @return cannon design
	 */
	public CraftDesign getDesign(String designId)
	{
		for (CraftDesign cannonDesign : craftsDesignList)
		{
			if (cannonDesign.getDesignID().equals(designId))
				return cannonDesign;
		}
		return null;
	}

	/**
	 * is there a cannon design with the give name
	 * @param name name of the design
	 * @return true if there is a cannon design with this name
     */
	public boolean hasDesign(String name){
		for (CraftDesign design : craftsDesignList){
			if (design.getDesignID().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	public List<Material> getCraftBlockMaterials() {
		return craftBlockMaterials;
	}

	public boolean isCannonBlockMaterial(Material material) {
		return material != Material.AIR && craftBlockMaterials.contains(material);
	}
}
