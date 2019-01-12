package at.pavlov.ironclad.scheduler;


import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.cannon.Craft;
import org.bukkit.scheduler.BukkitRunnable;

public class CreateCraft extends BukkitRunnable {

    private final Ironclad plugin;
    private Craft craft;
    private Boolean saveToDatabase;

    public CreateCraft(Ironclad plugin, Craft craft, boolean saveToDatabase){
        this.plugin = plugin;
        this.craft = craft;
        this.saveToDatabase = saveToDatabase;
    }

    @Override
    public void run() {
        plugin.getCraftManager().createCannon(craft, saveToDatabase);
    }
}
