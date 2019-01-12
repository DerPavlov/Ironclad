package at.pavlov.ironclad.dao;

import at.pavlov.ironclad.Ironclad;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Statement;
import java.util.UUID;

public class DeleteCraftTask extends BukkitRunnable{
    private UUID craftId = null;
    private UUID playerId = null;
    public DeleteCraftTask(){
        this.craftId = null;
        this.playerId = null;
    }

    public DeleteCraftTask(UUID craftId){
        this.craftId = craftId;
        this.playerId = null;
    }

    public DeleteCraftTask(UUID playerId, boolean player){
        this.craftId = null;
        this.playerId = playerId;
    }


    @Override
    public void run() {
        try (Statement statement = Ironclad.getPlugin().getConnection().createStatement()) {
            if (craftId == null && playerId == null){
                statement.executeUpdate(String.format("DELETE FROM %s", Ironclad.getPlugin().getCraftDatabase()));
            }
            else if (craftId != null) {
                statement.executeUpdate(String.format("DELETE FROM %s WHERE id='%s'", Ironclad.getPlugin().getCraftDatabase(), craftId));
            }
            else{
                statement.executeUpdate(String.format("DELETE FROM %s WHERE owner='%s'", Ironclad.getPlugin().getCraftDatabase(), playerId));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
