package at.pavlov.ironclad.dao;

import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.cannon.Craft;
import at.pavlov.ironclad.cannon.CraftManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.PreparedStatement;
import java.util.UUID;

public class SaveCraftTask extends BukkitRunnable {

    private UUID cannonId = null;
    public SaveCraftTask(UUID cannonId){
        this.cannonId = cannonId;
    }

    public SaveCraftTask(){
        this.cannonId = null;
    }

    @Override
    public void run() {
        // check if there is a valid connection
        if (Ironclad.getPlugin().getConnection() == null)
            return;

        String insert = String.format("REPLACE INTO %s " +
                "(id, name, owner, world, cannon_direction, loc_x, loc_y, loc_Z, soot, gunpowder, projectile_id, projectile_pushed, cannon_temperature, cannon_temperature_timestamp, horizontal_angle, vertical_angle, design_id, fired_cannonballs, target_mob, target_player, target_cannon, target_other, paid) VALUES" +
                "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
                , Ironclad.getPlugin().getCannonDatabase());
        try (PreparedStatement preparedStatement = Ironclad.getPlugin().getConnection().prepareStatement(insert)) {
            Ironclad.getPlugin().logDebug("[Ironclad] save Task start");
            for (Craft craft : CraftManager.getCraftList().values()) {
                // in case we want to save just one craft
                if (this.cannonId != null && craft.getUID() != this.cannonId) {
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

                //save paid fee
                preparedStatement.setBoolean(23, craft.isPaid());

                preparedStatement.addBatch();
            }
            Ironclad.getPlugin().logDebug("[Ironclad] save Task execute");
            preparedStatement.executeBatch();
        } catch (Exception e) {
            e.printStackTrace();
        }


        //Whitelist
        insert = String.format("REPLACE INTO %s " +
                        "(cannon_bean_id, player) VALUES" +
                        "(?,?)"
                , Ironclad.getPlugin().getWhitelistDatabase());
        try (PreparedStatement preparedStatement = Ironclad.getPlugin().getConnection().prepareStatement(insert)) {
            preparedStatement.executeBatch();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Ironclad.getPlugin().logDebug("[Ironclad] save Task finish");
    }
}
