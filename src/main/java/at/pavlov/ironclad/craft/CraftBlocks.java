package at.pavlov.ironclad.craft;

import java.util.ArrayList;

import at.pavlov.ironclad.container.SimpleBlock;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;


class CraftBlocks
{
	private BlockVector3 craftCenter;
	private BlockVector3 rotationCenter;														//center off all rotation blocks
	private BlockVector3 minSize;
	private BlockVector3 maxSize;
    private ArrayList<SimpleBlock> allCraftBlocks = new ArrayList<>();
    private ArrayList<BlockVector3> hullBlocks = new ArrayList<>();
	private ArrayList<SimpleBlock> engines = new ArrayList<>();
    private ArrayList<SimpleBlock> chest = new ArrayList<>();
	private ArrayList<SimpleBlock> sign = new ArrayList<>();
    private ArrayList<BlockVector3> protectedBlocks = new ArrayList<>();

	public BlockVector3 getRotationCenter()
	{
		return rotationCenter;
	}
	void setRotationCenter(BlockVector3 rotationCenter)
	{
		this.rotationCenter = rotationCenter;
	}
	ArrayList<SimpleBlock> getAllCraftBlocks()
	{
		return allCraftBlocks;
	}
	void setAllCraftBlocks(ArrayList<SimpleBlock> allCraftBlocks)
	{
		this.allCraftBlocks = allCraftBlocks;
	}
	ArrayList<BlockVector3> getHullBlocks()
	{
		return hullBlocks;
	}
	void setHullBlocks (ArrayList<BlockVector3> hullBlocks)
	{
		this.hullBlocks = hullBlocks;
	}

	protected ArrayList<SimpleBlock> getChest()
	{
		return chest;
	}

	protected void setChest(ArrayList<SimpleBlock> chest)
	{
		this.chest = chest;
	}

	protected ArrayList<BlockVector3> getProtectedBlocks()
	{
		return protectedBlocks;
	}

	protected void setProtectedBlocks(ArrayList<BlockVector3> protectedBlocks)
	{
		this.protectedBlocks = protectedBlocks;
	}

	protected ArrayList<SimpleBlock> getEngines() {
		return engines;
	}

	protected void setEngines(ArrayList<SimpleBlock> engines) {
		this.engines = engines;
	}

	public BlockVector3 getMinSize() {
		return minSize;
	}

	void setMinSize(BlockVector3 minSize) {
		this.minSize = minSize;
	}

	public BlockVector3 getMaxSize() {
		return maxSize;
	}

	protected void setMaxSize(BlockVector3 maxSize) {
		this.maxSize = maxSize;
	}

	protected ArrayList<SimpleBlock> getSign() {
		return sign;
	}

	protected void setSign(ArrayList<SimpleBlock> sign) {
		this.sign = sign;
	}

	protected BlockVector3 getCraftCenter() {
		return craftCenter;
	}

	protected void setCraftCenter(BlockVector3 craftCenter) {
		this.craftCenter = craftCenter;
	}
}