package at.pavlov.ironclad.API;

import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.Enum.BreakCause;
import at.pavlov.ironclad.craft.Craft;
import at.pavlov.ironclad.craft.CraftManager;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class IroncladAPI {

    private final Ironclad plugin;

    public IroncladAPI(Ironclad ironclad)
    {
        this.plugin = ironclad;
    }

    /**
     * returns the craft on the given location
     * @param location - location of a craft block
     * @param playerUID - player UID searching for the craft. If there is no craft he will be the owner. If null no new Craft can be created.
     * @return - null if there is no craft, else the craft
     */
    public Craft getCannon(Location location, UUID playerUID)
    {
        return plugin.getCraftManager().getCraft(location, playerUID);
    }

    /**
     * returns all known craft in a sphere around the given location
     * @param center - center of the box
     * @param sphereRadius - radius of the sphere in blocks
     * @return - list of all ironclad in this sphere
     */
    public static HashSet<Craft> getCannonsInSphere(Location center, double sphereRadius)
    {
        return CraftManager.getCannonsInSphere(center, sphereRadius);
    }

    /**
     * returns all known craft in a box around the given location
     * @param center - center of the box
     * @param lengthX - box length in X
     * @param lengthY - box length in Y
     * @param lengthZ - box length in Z
     * @return - list of all ironclad in this sphere
     */
    public static HashSet<Craft> getCannonsInBox(Location center, double lengthX, double lengthY, double lengthZ)
    {
        return CraftManager.getCannonsInBox(center, lengthX, lengthY, lengthZ);
    }

    /**
     * returns all ironclad for a list of locations - this will update all craft locations
     * @param locations - a list of location to search for ironclad
     * @param playerUID - player UID which operates the craft
     * @param silent - no messages will be displayed if silent is true
     * @return - list of all ironclad in this sphere
     */
    public HashSet<Craft> getCannons(List<Location> locations, UUID playerUID, boolean silent)
    {
        return plugin.getCraftManager().getCannons(locations, playerUID, silent);
    }

    /**
     * returns all ironclad for a list of locations - this will update all craft locations
     * @param locations - a list of location to search for ironclad
     * @param playerUID - player UID which operates the craft
     * @return - list of all ironclad in this sphere
     */
    public HashSet<Craft> getCannons(List<Location> locations, UUID playerUID)
    {
        return plugin.getCraftManager().getCannons(locations, playerUID, true);
    }

    /**
     * returns the craft from the storage
     * @param uid UUID of the craft
     * @return the craft from the storage
     */
    public static Craft getCannon(UUID uid)
    {
        return CraftManager.getCraft(uid);
    }


    public void setCannonAngle(Craft craft, double horizontal, double vertical)
    {
        //plugin.getAiming().
    }

    /**
     * removes a craft from the list
     * @param uid UID of the craft
     * @param breakCannon the craft will explode and all craft blocks will drop
     * @param canExplode if the craft can explode when loaded with gunpowder
     * @param cause the reason way the craft was broken
     */
    public void removeCannon(UUID uid, boolean breakCannon, boolean canExplode, BreakCause cause)
    {
        plugin.getCraftManager().removeCannon(uid, breakCannon, canExplode, cause, true);
    }



}
