package at.pavlov.ironclad.dao;

import java.util.UUID;

import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.cannon.Craft;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class PersistenceDatabase
{
	private Ironclad plugin;
	private BukkitTask saveTask = null;

	public PersistenceDatabase(Ironclad _plugin)
	{
		plugin = _plugin;
	}

	public void createTables(){
		CreateTableTask createTableTask = new CreateTableTask();
		createTableTask.run();
	}

    /**
     * loads all ironclad from the database
     *
     * @return true if loading was successful
     */
	public void loadCrafts()
	{
	    if (!plugin.hasConnection()) {
            plugin.logSevere("No connection to database");
            return;
        }
		plugin.getCraftManager().clearCannonList();

	    LoadCraftTask task = new LoadCraftTask();
	    task.runTaskAsynchronously(plugin);
	}

	public void saveAllCrafts(boolean async){
		if (!plugin.hasConnection()) {
			plugin.logSevere("No connection to database");
			return;
		}
		SaveCraftTask saveCannonTask = new SaveCraftTask();
		if (async)
			saveTask = saveCannonTask.runTaskAsynchronously(plugin);
		else
			saveCannonTask.run();
    }

    public void saveCannon(Craft craft){
		if (!plugin.hasConnection()) {
			plugin.logSevere("No connection to database");
			return;
		}
		SaveCraftTask saveCannonTask = new SaveCraftTask(craft.getUID());
		saveTask = saveCannonTask.runTaskAsynchronously(plugin);
	}

	public boolean isSaveTaskRunning() {
		return saveTask != null && Bukkit.getScheduler().isCurrentlyRunning(saveTask.getTaskId());
	}

    public void deleteCannon(UUID cannon_id){
		if (!plugin.hasConnection()) {
			plugin.logSevere("No connection to database");
			return;
		}
		DeleteCraftTask deleteCraftTask = new DeleteCraftTask(cannon_id);
		deleteCraftTask.runTaskAsynchronously(plugin);
	}

	public void deleteAllCrafts(){
		if (!plugin.hasConnection()) {
			plugin.logSevere("No connection to database");
			return;
		}
		DeleteCraftTask deleteCraftTask = new DeleteCraftTask();
		deleteCraftTask.runTaskAsynchronously(plugin);
	}

	public void deleteCrafts(UUID player_id){
		if (!plugin.hasConnection()) {
			plugin.logSevere("No connection to database");
			return;
		}
		DeleteCraftTask deleteCraftTask = new DeleteCraftTask(player_id, true);
		deleteCraftTask.runTaskAsynchronously(plugin);
	}
}
