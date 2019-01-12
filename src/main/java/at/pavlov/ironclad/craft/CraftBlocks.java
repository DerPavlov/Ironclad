package at.pavlov.ironclad.craft;

import java.util.ArrayList;

import org.bukkit.util.Vector;

import at.pavlov.ironclad.container.SimpleBlock;


class CraftBlocks
{
	private Vector rotationCenter;														//center off all rotation blocks
	private Vector minSize;
	private Vector maxSize;
    private ArrayList<SimpleBlock> allCannonBlocks = new ArrayList<>();
    private ArrayList<Vector> hullBlocks = new ArrayList<>();
	private ArrayList<SimpleBlock> engines = new ArrayList<>();
    private ArrayList<SimpleBlock> chest = new ArrayList<>();
	private ArrayList<SimpleBlock> sign = new ArrayList<>();
    private ArrayList<Vector> destructibleBlocks = new ArrayList<>();

	public Vector getRotationCenter()
	{
		return rotationCenter;
	}
	protected void setRotationCenter(Vector rotationCenter)
	{
		this.rotationCenter = rotationCenter;
	}
	public ArrayList<SimpleBlock> getAllCraftBlocks()
	{
		return allCannonBlocks;
	}
	protected void setAllCannonBlocks(ArrayList<SimpleBlock> allCannonBlocks)
	{
		this.allCannonBlocks = allCannonBlocks;
	}
	public ArrayList<Vector> getHullBlocks()
	{
		return hullBlocks;
	}
	protected void setBarrel (ArrayList<Vector> barrelBlocks)
	{
		this.hullBlocks = barrelBlocks;
	}

	public ArrayList<SimpleBlock> getChest()
	{
		return chest;
	}

	protected void setChest(ArrayList<SimpleBlock> chest)
	{
		this.chest = chest;
	}

	public ArrayList<Vector> getDestructibleBlocks()
	{
		return destructibleBlocks;
	}

	protected void setDestructibleBlocks(ArrayList<Vector> destructibleBlocks)
	{
		this.destructibleBlocks = destructibleBlocks;
	}

	public ArrayList<SimpleBlock> getEngines() {
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

	public ArrayList<SimpleBlock> getSign() {
		return sign;
	}

	protected void setSign(ArrayList<SimpleBlock> sign) {
		this.sign = sign;
	}
}