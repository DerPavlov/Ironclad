package at.pavlov.ironclad.container;


import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.UUID;


public class IntVector implements Cloneable
{
	private int x;
	private int y;
	private int z;


	public IntVector(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public IntVector(Vector vector) {
		this.x = (int) Math.floor(vector.getX());
		this.x = (int) Math.floor(vector.getY());
		this.x = (int) Math.floor(vector.getZ());
	}

	/**
	 * Get a new vector.
	 *
	 * @return vector
	 */
	@Override
	public IntVector clone() {
		try {
			return (IntVector) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new Error(e);
		}
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	/**
	 * shifts the location of the block without comparing the id
	 * @param vector vector to add
	 * @return new Simpleblock
	 */
	public IntVector add(Vector vector) {
		this.x += vector.getBlockX();
		this.y += vector.getBlockY();
		this.z += vector.getBlockZ();
		return this;
	}

	/**
	 * shifts the location of the block without comparing the id
	 * @param vector vector to add
	 * @return new Simpleblock
	 */
	public IntVector add(IntVector vector) {
		this.x += vector.getX();
		this.y += vector.getY();
		this.z += vector.getZ();
		return this;
	}

	/**
	 * shifts the location of the block without comparing the id
	 * @param vector vector to subtract
	 * @return new Simpleblock
	 */
	public IntVector subtract(Vector vector) {
		this.x -= vector.getBlockX();
		this.y -= vector.getBlockY();
		this.z -= vector.getBlockZ();
		return this;
	}

	/**
	 * shifts the location of the block without comparing the id
	 * @param vector vector to subtract
	 * @return new Simpleblock
	 */
	public IntVector subtract(IntVector vector) {
		this.x -= vector.getX();
		this.y -= vector.getY();
		this.z -= vector.getZ();
		return this;
	}


	public Vector toVector(){
		return new Vector(this.x, this.y, this.z);
	}

}
