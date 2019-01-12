package at.pavlov.ironclad.dao;

import at.pavlov.ironclad.Ironclad;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Statement;

public class CreateTableTask extends BukkitRunnable {
    @Override
    public void run() {
        String sql1 = String.format("CREATE TABLE IF NOT EXISTS %s (" +
                        "id VARCHAR(40) PRIMARY KEY, " +
                        "name VARCHAR(20) NOT NULL," +
                        "owner VARCHAR(40) NOT NULL," +
                        "world VARCHAR(40) NOT NULL," +
                        "craft_direction VARCHAR(20)," +
                        "loc_x INTEGER," +
                        "loc_y INTEGER," +
                        "loc_z INTEGER," +
                        "yaw DOUBLE," +
                        "pitch DOUBLE," +
                        "velocity DOUBLE," +
                        "design_id VARCHAR(20)," +
                        "travelled_distance DOUBLE," +
                        "paid BOOLEAN)"
                , Ironclad.getPlugin().getCraftDatabase());
        try (Statement statement = Ironclad.getPlugin().getConnection().createStatement()) {
            statement.execute(sql1);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
