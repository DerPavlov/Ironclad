package at.pavlov.ironclad.craft;

import java.util.ArrayList;

import org.bukkit.util.Vector;

import at.pavlov.ironclad.container.SimpleBlock;


class CraftBlocks
{
	private Vector craftCenter;
	private Vector rotationCenter;														//center off all rotation blocks
	private Vector minSize;
	private Vector maxSize;
    private ArrayList<SimpleBlock> allCraftBlocks = new ArrayList<>();
    private ArrayList<Vector> hullBlocks = new ArrayList<>();
	private ArrayList<SimpleBlock> engines = new ArrayList<>();
    private ArrayList<SimpleBlock> chest = new ArrayList<>();
	private ArrayList<SimpleBlock> sign = new ArrayList<>();
    private ArrayList<Vector> protectedBlocks = new ArrayList<>();

	public Vector getRotationCenter()
	{
		return rotationCenter;
	}
	protected void setRotationCenter(Vector rotationCenter)
	{
		this.rotationCenter = rotationCenter;
	}
	protected ArrayList<SimpleBlock> getAllCraftBlocks()
	{
		return allCraftBlocks;
	}
	protected void setAllCraftBlocks(ArrayList<SimpleBlock> allCraftBlocks)
	{
		this.allCraftBlocks = allCraftBlocks;
	}
	protected ArrayList<Vector> getHullBlocks()
	{
		return hullBlocks;
	}
	protected void setBarrel (ArrayList<Vector> barrelBlocks)
	{
		this.hullBlocks = barrelBlocks;
	}

	protected ArrayList<SimpleBlock> getChest()
	{
		return chest;
	}

	protected void setChest(ArrayList<SimpleBlock> chest)
	{
		this.chest = chest;
	}

	protected ArrayList<Vector> getProtectedBlocks()
	{
		return protectedBlocks;
	}

	protected void setProtectedBlocks(ArrayList<Vector> protectedBlocks)
	{
		this.protectedBlocks = protectedBlocks;
	}

	protected ArrayList<SimpleBlock> getEngines() {
		return engines;
	}

	protected void setEngines(ArrayList<SimpleBlock> engines) {
		this.engines = engines;
	}

	public Vector getMinSize() {
		return minSize;
	}

	protected void setMinSize(Vector minSize) {
		this.minSize = minSize;
	}

	public Vector getMaxSize() {
		return maxSize;
	}

	protected void setMaxSize(Vector maxSize) {
		this.maxSize = maxSize;
	}

	protected ArrayList<SimpleBlock> getSign() {
		return sign;
	}

	protected void setSign(ArrayList<SimpleBlock> sign) {
		this.sign = sign;
	}

	protected Vector getCraftCenter() {
		return craftCenter;
	}

	protected void setCraftCenter(Vector craftCenter) {
		this.craftCenter = craftCenter;
	}
}