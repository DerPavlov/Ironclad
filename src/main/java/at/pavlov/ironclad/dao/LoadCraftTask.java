package at.pavlov.ironclad.dao;

import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.craft.Craft;
import at.pavlov.ironclad.craft.CraftDesign;
import at.pavlov.ironclad.scheduler.CreateCraft;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;

public class LoadCraftTask extends BukkitRunnable{
    public LoadCraftTask(){

    }

    @Override
    public void run() {
        ArrayList<UUID> invalid = new ArrayList<UUID>();
        int i = 0;

        try (Statement statement = Ironclad.getPlugin().getConnection().createStatement()) {
            // create a query that returns CannonBean

            ResultSet rs = statement.executeQuery(
                    String.format("SELECT * FROM %s", Ironclad.getPlugin().getCannonDatabase())
            );

            // found ironclad - load them
            while (rs.next()) {
                UUID cannon_id = UUID.fromString(rs.getString("id"));
                //check if craft design exists
                CraftDesign design = Ironclad.getPlugin().getCraftDesign(rs.getString("design_id"));
                if (design == null) {
                    Ironclad.getPlugin().logDebug("Design " + rs.getString("design_id") + " not found in plugin/designs");
                    invalid.add(cannon_id);
                    //deleteCannon(bean.getId());
                } else {
                    //load values for the craft
                    UUID world = UUID.fromString(rs.getString("world"));
                    //test if world is valid
                    World w = Bukkit.getWorld(world);

                    if (w == null) {
                        Ironclad.getPlugin().logDebug("World of craft " + cannon_id + " is not valid");
                        invalid.add(cannon_id);
                        continue;
                    }
                    String owner_str = rs.getString("owner");
                    if (owner_str == null) {
                        Ironclad.getPlugin().logDebug("Owner of craft " + cannon_id + " is null");
                        invalid.add(cannon_id);
                        continue;
                    }
                    UUID owner = UUID.fromString(owner_str);
                    boolean isBanned = false;
                    for (OfflinePlayer oplayer : Bukkit.getServer().getBannedPlayers()) {
                        if (oplayer.getUniqueId().equals(owner))
                            isBanned = true;
                    }
                    if (!Bukkit.getOfflinePlayer(owner).hasPlayedBefore() || isBanned) {
                        if (isBanned)
                            Ironclad.getPlugin().logDebug("Owner of craft " + cannon_id + " was banned");
                        else
                            Ironclad.getPlugin().logDebug("Owner of craft " + cannon_id + " does not exist");
                        invalid.add(cannon_id);
                        continue;
                    }

                    Vector offset = new Vector(rs.getInt("loc_x"), rs.getInt("loc_y"), rs.getInt("loc_z"));
                    BlockFace cannonDirection = BlockFace.valueOf(rs.getString("cannon_direction"));

                    //make a craft
                    Craft craft = new Craft(design, world, offset, cannonDirection, owner);
                    // craft created - load properties
                    craft.setUID(cannon_id);
                    craft.setCraftName(rs.getString("name"));

                    // craft fee
                    craft.setPaid(rs.getBoolean("paid"));

                    //add a craft to the craft list
                    BukkitTask task = new CreateCraft(Ironclad.getPlugin(), craft, false).runTask(Ironclad.getPlugin());
                    //plugin.createCannon(craft);
                    i++;
                }
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //delete invalid ironclad
        try (Statement statement = Ironclad.getPlugin().getConnection().createStatement()) {

            for (UUID inv : invalid) {
                statement.addBatch(String.format("DELETE FROM %s WHERE id='%s'", Ironclad.getPlugin().getCannonDatabase(), inv.toString()));
                Ironclad.getPlugin().logDebug("Delete craft " + inv);
            }
            statement.executeBatch();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Ironclad.getPlugin().logDebug(i + " ironclad loaded from the database");

    }
}
