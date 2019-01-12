package at.pavlov.ironclad.dao;

import at.pavlov.ironclad.Ironclad;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Statement;
import java.util.UUID;

public class DeleteCannonTask extends BukkitRunnable{
    private UUID cannonId = null;
    private UUID playerId = null;
    public DeleteCannonTask(){
        this.cannonId = null;
        this.playerId = null;
    }

    public DeleteCannonTask(UUID cannonId){
        this.cannonId = cannonId;
        this.playerId = null;
    }

    public DeleteCannonTask(UUID playerId, boolean player){
        this.cannonId = null;
        this.playerId = playerId;
    }


    @Override
    public void run() {
        try (Statement statement = Ironclad.getPlugin().getConnection().createStatement()) {
            if (cannonId == null && playerId == null){
                statement.executeUpdate(String.format("DELETE FROM %s", Ironclad.getPlugin().getCannonDatabase()));
            }
            else if (cannonId != null) {
                statement.executeUpdate(String.format("DELETE FROM %s WHERE id='%s'", Ironclad.getPlugin().getCannonDatabase(), cannonId));
            }
            else{
                statement.executeUpdate(String.format("DELETE FROM %s WHERE owner='%s'", Ironclad.getPlugin().getCannonDatabase(), playerId));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
