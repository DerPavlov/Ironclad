package at.pavlov.ironclad.scheduler;

import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.container.SimpleBlock;
import at.pavlov.ironclad.craft.Craft;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.*;

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
        ArrayList<SimpleBlock> resetBlocks = new ArrayList<>();
        ArrayList<SimpleBlock> resetAttachedBlocks = new ArrayList<>();
        HashSet<Vector> overwrittenBlocks = new HashSet<>();

        boolean successful = true;

        //perform craft calculations
        for (SimpleBlock block : craftClone.getCraftDesign().getAllCraftBlocks(craftClone)){
            SimpleBlock oldBlock = blockSnapshot.get(block.toVector());
            if (oldBlock.getMaterial() == Material.AIR || oldBlock.getBlockData() instanceof Levelled){
                Ironclad.getPlugin().logDebug("Found destroyed craft block" + oldBlock);
            }
            else{
                // move the craft to the new location
                Ironclad.getPlugin().logDebug("old CraftVec " + oldBlock.toVector());
                //oldblock was updated to the new location
                oldBlock.setVector(craftClone.transformToFutureLocation(oldBlock.toVector()));
                Ironclad.getPlugin().logDebug("new CraftVec " + oldBlock.toVector());
                SimpleBlock targetBlock = blockSnapshot.get(block.toVector());
                if (targetBlock.getMaterial() == Material.AIR || targetBlock.getBlockData() instanceof Levelled){
                    Ironclad.getPlugin().logDebug("Found blocking block at" + oldBlock);
                    successful = false;
                    break;
                }
                if (oldBlock.getBlockData() instanceof Directional)
                    newAttachedBlocks.add(oldBlock);
                else
                    newBlocks.add(oldBlock);
                overwrittenBlocks.add(oldBlock.toVector().clone());
            }
        }

        //remove left over ship blocks
        for (SimpleBlock block : craftClone.getCraftDesign().getAllCraftBlocks(craftClone)){
            Ironclad.getPlugin().logDebug("left over ship block" + block + " overwritten contains " + overwrittenBlocks.contains(block.toVector()));
            if (!overwrittenBlocks.contains(block.toVector())){
                Ironclad.getPlugin().logDebug("Found left over block");
                if (block.getBlockData() instanceof Directional) {
                    block.setBlockData(Bukkit.createBlockData(Material.AIR));
                    resetAttachedBlocks.add(block);
                }
                else{
                    block.setBlockData(Bukkit.createBlockData(Material.AIR));
                    resetBlocks.add(block);
                }
            }

        }


        //run a sync thread to update the calculated data
        BukkitTask task = new MoveCraftTask(craftClone, newBlocks, newAttachedBlocks, resetBlocks, resetAttachedBlocks, entities, successful).runTask(Ironclad.getPlugin());

        Ironclad.getPlugin().logDebug("Time for async calculation of craft movement: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
    }
}
