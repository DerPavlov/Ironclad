package at.pavlov.ironclad.scheduler;

import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.container.SimpleBlock;
import at.pavlov.ironclad.craft.Craft;
import org.bukkit.ChunkSnapshot;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MoveCalculateTask extends BukkitRunnable{
    private final Craft craftClone;
    private final Map<Vector, SimpleBlock> blockSnapshot;
    private final Set<Entity> entities;


    public MoveCalculateTask(Craft craftClone, Map<Vector, SimpleBlock> blockSnapshot, Set<Entity> entities){
        this.craftClone = craftClone;
        this.blockSnapshot = blockSnapshot;
        this.entities = entities;
    }

    @Override
    public void run() {
        long startTime = System.nanoTime();

        ArrayList<SimpleBlock> newBlocks = new ArrayList<>();
        ArrayList<SimpleBlock> newAttachedBlocks = new ArrayList<>();

        //perform craft calculations
        for (SimpleBlock block : craftClone.getCraftDesign().getAllCraftBlocks(craftClone)){

        }

        //run a sync thread to update the calculated data
        BukkitTask task = new MoveCraftTask(craftClone, newBlocks, newAttachedBlocks, entities).runTask(Ironclad.getPlugin());

        Ironclad.getPlugin().logDebug("Time for async calculation of craft movement: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
    }
}
