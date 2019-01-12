package at.pavlov.ironclad.API;

import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.Enum.BreakCause;
import at.pavlov.ironclad.Enum.InteractAction;
import at.pavlov.ironclad.cannon.Craft;
import at.pavlov.ironclad.Enum.MessageEnum;
import at.pavlov.ironclad.cannon.CraftManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

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
     * returns the cannon on the given location
     * @param location - location of a cannon block
     * @param playerUID - player UID searching for the cannon. If there is no cannon he will be the owner. If null no new Craft can be created.
     * @return - null if there is no cannon, else the cannon
     */
    public Craft getCannon(Location location, UUID playerUID)
    {
        return plugin.getCraftManager().getCraft(location, playerUID);
    }

    /**
     * returns all known cannon in a sphere around the given location
     * @param center - center of the box
     * @param sphereRadius - radius of the sphere in blocks
     * @return - list of all ironclad in this sphere
     */
    public static HashSet<Craft> getCannonsInSphere(Location center, double sphereRadius)
    {
        return CraftManager.getCannonsInSphere(center, sphereRadius);
    }

    /**
     * returns all known cannon in a box around the given location
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
     * returns all ironclad for a list of locations - this will update all cannon locations
     * @param locations - a list of location to search for ironclad
     * @param playerUID - player UID which operates the cannon
     * @param silent - no messages will be displayed if silent is true
     * @return - list of all ironclad in this sphere
     */
    public HashSet<Craft> getCannons(List<Location> locations, UUID playerUID, boolean silent)
    {
        return plugin.getCraftManager().getCannons(locations, playerUID, silent);
    }

    /**
     * returns all ironclad for a list of locations - this will update all cannon locations
     * @param locations - a list of location to search for ironclad
     * @param playerUID - player UID which operates the cannon
     * @return - list of all ironclad in this sphere
     */
    public HashSet<Craft> getCannons(List<Location> locations, UUID playerUID)
    {
        return plugin.getCraftManager().getCannons(locations, playerUID, true);
    }

    /**
     * returns the cannon from the storage
     * @param uid UUID of the cannon
     * @return the cannon from the storage
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
     * removes a cannon from the list
     * @param uid UID of the cannon
     * @param breakCannon the cannon will explode and all cannon blocks will drop
     * @param canExplode if the cannon can explode when loaded with gunpowder
     * @param cause the reason way the cannon was broken
     */
    public void removeCannon(UUID uid, boolean breakCannon, boolean canExplode, BreakCause cause)
    {
        plugin.getCraftManager().removeCannon(uid, breakCannon, canExplode, cause, true);
    }



}
