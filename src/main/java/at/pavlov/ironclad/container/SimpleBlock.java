package at.pavlov.ironclad.container;


import at.pavlov.ironclad.utils.IroncladUtil;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.material.Directional;

public class SimpleBlock implements Cloneable
{
	private int locX;
	private int locY;
	private int locZ;
	
	private BlockState blockData;

	public SimpleBlock(int x, int y, int z, BlockState blockData)
	{
		locX = x;
		locY = y;
		locZ = z;

		this.blockData = blockData;
	}

	public SimpleBlock(BlockVector3 vect, BlockState blockData) {
		this(vect.getBlockX(), vect.getBlockY(), vect.getBlockZ(), blockData);
	}

	public SimpleBlock(Block block) {
		this(block.getX(), block.getY(), block.getZ(), BukkitAdapter.adapt(block.getBlockData().clone()));
	}

	private SimpleBlock(BlockVector3 vect, Material material)
	{
		this(vect, BukkitAdapter.adapt(material.createBlockData()));
	}
	
	public SimpleBlock(Location loc, Material material)
	{
		locX = loc.getBlockX();
		locY = loc.getBlockY();
		locZ = loc.getBlockZ();
		
		this.blockData = BukkitAdapter.adapt(material.createBlockData());
	}

	/**
	 * to location with offset
	 * @param world bukkit world
	 * @return location of the block
	 */
	public Location toLocation(World world)
	{
		return new Location(world, locX, locY, locZ);
	}
	
	/**
	 * to location with offset
	 * @param world bukkit world
	 * @return location of the block
	 */
	public Location toLocation(World world, BlockVector3 offset)
	{
		return new Location(world, locX + offset.getX(), locY + offset.getY(), locZ + offset.getZ());
	}

	/**
	 * to location with offset
	 * @param world bukkit world
	 * @return location of the block
	 */
	public Location toLocation(World world, Vector3 offset)
	{
		return new Location(world, locX + offset.getX(), locY + offset.getY(), locZ + offset.getZ());
	}

	/**
	 * compare the location
	 * @param loc location to compare to
	 * @param offset the offset of the craft
	 * @return true if both block match
	 */
	public boolean compareLocation(Location loc, BlockVector3 offset)
	{
        org.bukkit.util.Vector v = loc.toVector();
		return compareLocation(BlockVector3.at(v.getX(), v.getY(), v.getZ()), offset);
	}

	/**
	 * compare the location
	 * @param loc location to compare to
	 * @param offset the offset of the craft
	 * @return true if both block match
	 */
	public boolean compareLocation(BlockVector3 loc, BlockVector3 offset)
	{
		return toVector().add(offset).equals(loc);
	}

	/**
	 * compare the location of the block and the id and data or data = -1
	 * @param block block to compare to
	 * @param offset the offset of the craft
	 * @return true if both block match
	 */
	public boolean compareMaterialAndLoc(Block block, BlockVector3 offset)
	{
		if (toVector().add(offset).equals(BukkitAdapter.asBlockVector(block.getLocation())))
		{
			return compareMaterial(BukkitAdapter.adapt(block.getBlockData()));
		}
		return false;
	}

	/**
	 * return true if Materials match
	 * @param block block to compare to
	 * @return true if both block match
	 */
	public boolean compareMaterial(BlockStateHolder block)
	{
		return block.getBlockType().getMaterial().equals(this.blockData.getBlockType().getMaterial());
	}

	/**
	 * compares material and facing
	 * @param blockData block to compare to
	 * @return true if both block match
	 */
	public boolean compareMaterialAndFacing(BlockStateHolder blockData) {
		// different materials
		if (!compareMaterial(blockData)) {
			return false;
		}
		// compare facing and face
		if (blockData instanceof Directional && this instanceof Directional){
			return ((Directional) this).getFacing().equals(((Directional) blockData).getFacing());
		}
		return true;
	}

	/**
	 * compares the real world block by material and facing
	 * @param world the world of the block
	 * @param offset the locations in x,y,z
	 * @return true if both block are equal in data and facing
	 */
	public boolean compareMaterialAndFacing(World world, BlockVector3 offset)
	{
		Block block = toLocation(world, offset).getBlock();
		return compareMaterialAndFacing(BukkitAdapter.adapt(block.getBlockData()));
	}

	/**
	 * shifts the location of the block without comparing the id
	 * @param loc location to add
	 * @return new Simpleblock
	 */
	public SimpleBlock add(Location loc)
	{
		return new SimpleBlock(locX + loc.getBlockX(), locY + loc.getBlockY(), locZ + loc.getBlockZ(), this.blockData);
	}
	
	/**
	 * shifts the location of the block without comparing the id
	 * @param vect offset vector
	 * @return a new block with a shifted location
	 */
	public SimpleBlock add(BlockVector3 vect)
	{
		return new SimpleBlock(toVector().add(vect), this.blockData);
	}
	
	/** 
	 * shifts the location of the block without comparing the id
	 * @param vect vector to subtract
	 * @return new block with new subtracted location
	 */
	public SimpleBlock subtract(BlockVector3 vect)
	{
		return new SimpleBlock(vect.getBlockX() - locX, vect.getBlockY() - locY, vect.getBlockZ() - locZ, this.blockData);
	}

    /**
     * shifts the location of the block without comparing the id
     * @param vect vector to subtract
     */
    public void subtract_noCopy(BlockVector3 vect)
    {
        locX -= vect.getBlockX();
        locY -= vect.getBlockY();
        locZ -= vect.getBlockZ();
    }

	/** 
	 * shifts the location of the block without comparing the id
	 * @param loc
	 */
	public SimpleBlock subtractInverted(Location loc)
	{
		return new SimpleBlock(loc.getBlockX() - locX, loc.getBlockY() - locY, loc.getBlockZ() - locZ, this.blockData);
	}
	

	
	/** 
	 * shifts the location of the block without comparing the id
	 * @param loc
	 */
	public SimpleBlock subtract(Location loc)
	{
		return new SimpleBlock(locX - loc.getBlockX() , locY - loc.getBlockY(), locZ - loc.getBlockZ(), this.blockData);
	}

	/**
	 * Get a new vector.
	 *
	 * @return vector
	 */
	@Override
	public SimpleBlock clone() {
		try {
			return (SimpleBlock) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new Error(e);
		}
	}

	/**
	 * rotate the block 90° degree clockwise)
	 * @return
	 */
	public void rotate90(){
		this.blockData = IroncladUtil.roateBlockFacingClockwise(this.blockData);
		int newx = this.locZ;
		this.locZ = -this.locX;
		this.locX = newx;
	}
	
	/**
	 * SimpleBlock to Vector
	 */
	public BlockVector3 toVector()
	{
		return BlockVector3.at(locX, locY, locZ);
	}

	/**
	 * SimpleBlock set Vector
	 */
	public void setVector(BlockVector3 vector)
	{
		this.locX = vector.getBlockX();
		this.locY = vector.getBlockY();
		this.locZ = vector.getBlockZ();
	}

	public int getLocX()
	{
		return locX;
	}

	public void setLocX(int locX)
	{
		this.locX = locX;
	}

	public int getLocY()
	{
		return locY;
	}

	public void setLocY(int locY)
	{
		this.locY = locY;
	}

	public int getLocZ()
	{
		return locZ;
	}

	public void setLocZ(int locZ)
	{
		this.locZ = locZ;
	}

	public void setBlockData(BlockState blockData)
	{
		this.blockData = blockData;
	}

	public BlockState getBlockState()
	{
		return this.blockData;
	}

	public BlockData getBlockData()
	{
		return BukkitAdapter.adapt(this.blockData);
	}

	public Material getMaterial()
	{
		return BukkitAdapter.adapt(this.blockData).getMaterial();
	}

	public String toString()
	{
		return "x:" + locX + " y:" + locY + " z:" + locZ +" blockdata:" + this.getBlockState().toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SimpleBlock)) {
			return false;
		}

		SimpleBlock other = (SimpleBlock) obj;
		return other.locX == this.locX && other.locY == this.locY && other.locZ == this.locZ && other.blockData.equals(this.blockData);
	}

	@Override
	public int hashCode() {
		int hash = 17;
		hash = 31 * hash + Integer.hashCode(locX);
		hash = 31 * hash + Integer.hashCode(locY);
		hash = 31 * hash + Integer.hashCode(locZ);
		return hash;
	}


}
