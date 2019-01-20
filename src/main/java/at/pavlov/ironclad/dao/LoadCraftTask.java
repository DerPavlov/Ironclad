package at.pavlov.ironclad.dao;

import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.craft.Craft;
import at.pavlov.ironclad.craft.CraftDesign;
import at.pavlov.ironclad.scheduler.CreateCraftTask;
import com.sk89q.worldedit.Vector;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

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
            // create a query that returns CraftBean

            ResultSet rs = statement.executeQuery(
                    String.format("SELECT * FROM %s", Ironclad.getPlugin().getCraftDatabase())
            );

            // found ironclad - load them
            while (rs.next()) {
                UUID craft_id = UUID.fromString(rs.getString("id"));
                //check if craft design exists
                CraftDesign design = Ironclad.getPlugin().getCraftDesign(rs.getString("design_id"));
                if (design == null) {
                    Ironclad.getPlugin().logDebug("Design " + rs.getString("design_id") + " not found in plugin/designs");
                    invalid.add(craft_id);
                } else {
                    //load values for the craft
                    UUID world = UUID.fromString(rs.getString("world"));
                    //test if world is valid
                    World w = Bukkit.getWorld(world);

                    if (w == null) {
                        Ironclad.getPlugin().logDebug("World of craft " + craft_id + " is not valid");
                        invalid.add(craft_id);
                        continue;
                    }
                    String owner_str = rs.getString("owner");
                    if (owner_str == null) {
                        Ironclad.getPlugin().logDebug("Owner of craft " + craft_id + " is null");
                        invalid.add(craft_id);
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
                            Ironclad.getPlugin().logDebug("Owner of craft " + craft_id + " was banned");
                        else
                            Ironclad.getPlugin().logDebug("Owner of craft " + craft_id + " does not exist");
                        invalid.add(craft_id);
                        continue;
                    }

                    Vector offset = new Vector(rs.getInt("loc_x"), rs.getInt("loc_y"), rs.getInt("loc_z"));
                    BlockFace craftDirection = BlockFace.valueOf(rs.getString("craft_direction"));

                    //make a craft
                    Craft craft = new Craft(design, world, offset, craftDirection, owner);
                    // craft created - load properties
                    craft.setUID(craft_id);
                    craft.setCraftName(rs.getString("name"));

                    craft.setYaw(rs.getDouble("yaw"));
                    craft.setPitch(rs.getDouble("pitch"));
                    craft.setVelocity(rs.getDouble("velocity"));
                    craft.setTravelledDistance(rs.getDouble("travelled_distance"));

                    // craft fee
                    craft.setPaid(rs.getBoolean("paid"));

                    //add a craft to the craft list
                    BukkitTask task = new CreateCraftTask(Ironclad.getPlugin(), craft, false).runTask(Ironclad.getPlugin());
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
                statement.addBatch(String.format("DELETE FROM %s WHERE id='%s'", Ironclad.getPlugin().getCraftDatabase(), inv.toString()));
                Ironclad.getPlugin().logDebug("Delete craft " + inv);
            }
            statement.executeBatch();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Ironclad.getPlugin().logDebug(i + " ironclad loaded from the database");

    }
}
