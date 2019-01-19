package at.pavlov.ironclad.container;


import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.UUID;


public class SimpleEntity implements Cloneable
{
	private UUID uuid;
	private Location location;
	//parameter to make delayed teleporation more smooth
	private float yawUpdate;
	private Vector locationUpate;

	public SimpleEntity(UUID uuid, Location location) {
		this.uuid = uuid;
		this.location = location;
	}

	public SimpleEntity(Entity entity) {
		this.uuid = entity.getUniqueId();
		this.location = entity.getLocation();
	}

	public String toString()
	{
		return "Simple Entity " + uuid + " " + location;
	}


	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	/**
	 * Get a new vector.
	 *
	 * @return vector
	 */
	@Override
	public SimpleEntity clone() {
		try {
			return (SimpleEntity) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new Error(e);
		}
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

	public float getYawUpdate() {
		return yawUpdate;
	}

	public void setYawUpdate(float yawUpdate) {
		this.yawUpdate = yawUpdate;
	}

	public Vector getLocationUpate() {
		return locationUpate;
	}

	public void setLocationUpate(Vector locationUpate) {
		this.locationUpate = locationUpate;
	}
}
