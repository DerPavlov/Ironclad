package at.pavlov.ironclad.dao;

import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.craft.Craft;
import at.pavlov.ironclad.craft.CraftManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.PreparedStatement;
import java.util.UUID;

public class SaveCraftTask extends BukkitRunnable {

    private UUID craftId = null;
    public SaveCraftTask(UUID craftId){
        this.craftId = craftId;
    }

    public SaveCraftTask(){
        this.craftId = null;
    }

    @Override
    public void run() {
        // check if there is a valid connection
        if (Ironclad.getPlugin().getConnection() == null)
            return;

        String insert = String.format("REPLACE INTO %s " +
                "(id, name, owner, world, craft_direction, loc_x, loc_y, loc_Z, yaw, pitch, velocity, design_id, travelled_distance, paid) VALUES" +
                "(?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
                , Ironclad.getPlugin().getCraftDatabase());
        try (PreparedStatement preparedStatement = Ironclad.getPlugin().getConnection().prepareStatement(insert)) {
            Ironclad.getPlugin().logDebug("[Ironclad] save Task start");
            for (Craft craft : CraftManager.getCraftList().values()) {
                // in case we want to save just one craft
                if (this.craftId != null && craft.getUID() != this.craftId) {
                    continue;
                }

                //dont save ironclad which are not valid anymore
                if (!craft.isValid())
                    continue;

                // is the entry different from the last stored entry? Then store it
                if (!craft.isUpdated())
                    continue;
                Ironclad.getPlugin().logDebug("Craft was updated");
                craft.setUpdated(false);


                if (craft.getOwner() == null) {
                    Ironclad.getPlugin().logDebug("Craft not saved. Owner of craft was null");
                    continue;
                }


                // fill the preparedStatement with values to store
                // since bukkit manages the preparedStatement, we do not need to set
                // the ID property
                preparedStatement.setString(1, craft.getUID().toString());
                preparedStatement.setString(2, craft.getCraftName());
                preparedStatement.setString(3, craft.getOwner().toString());
                preparedStatement.setString(4, craft.getWorld().toString());
                // craft direction
                preparedStatement.setString(5, craft.getCraftDirection().toString());
                // save offset
                preparedStatement.setInt(6, craft.getOffset().getBlockX());
                preparedStatement.setInt(7, craft.getOffset().getBlockY());
                preparedStatement.setInt(8, craft.getOffset().getBlockZ());
                //save pitch/yaw/velociy
                preparedStatement.setDouble(9, craft.getYaw());
                preparedStatement.setDouble(10, craft.getPitch());
                preparedStatement.setDouble(11, craft.getVelocity());
                // id
                preparedStatement.setString(12, craft.getDesignID());
                //travelled distance
                preparedStatement.setDouble(13, craft.getTravelledDistance());
                //save paid fee
                preparedStatement.setBoolean(14, craft.isPaid());

                preparedStatement.addBatch();
            }
            Ironclad.getPlugin().logDebug("[Ironclad] save Task execute");
            preparedStatement.executeBatch();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Ironclad.getPlugin().logDebug("[Ironclad] save Task finish");
    }
}
