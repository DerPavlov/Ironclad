package at.pavlov.ironclad.scheduler;

import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.container.SimpleBlock;
import at.pavlov.ironclad.container.SimpleEntity;
import at.pavlov.ironclad.craft.Craft;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
    private final Set<SimpleEntity> entities;


    public MoveCalculateTask(Craft craftClone, Map<Vector, SimpleBlock> blockSnapshot, Set<SimpleEntity> entities){
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
        Vector targetLoc;
        SimpleBlock targetBlock;

        //perform craft calculations
        for (SimpleBlock designBlock : craftClone.getCraftDesign().getAllCraftBlocks(craftClone)){
            SimpleBlock oldBlock = blockSnapshot.get(designBlock.toVector());
            Ironclad.getPlugin().logDebug("old block " + oldBlock);
            if (oldBlock.getMaterial() == Material.AIR || oldBlock.getBlockData() instanceof Levelled){
                Ironclad.getPlugin().logDebug("Found destroyed craft block " + oldBlock);
            }
            else{
                // move the craft to the new location. oldblock was updated to the new location
                targetLoc = craftClone.transformToFutureLocation(designBlock.toVector());
                //remove decimal places to the the correct block location
                targetLoc.setX(Math.floor(targetLoc.getX()));
                targetLoc.setY(Math.floor(targetLoc.getY()));
                targetLoc.setZ(Math.floor(targetLoc.getX()));
                targetBlock = blockSnapshot.get(targetLoc);
                if (targetBlock == null){
                    Ironclad.getPlugin().logDebug("target block " + targetLoc + " does not exist in snapshot");
                    continue;
                }

                Ironclad.getPlugin().logDebug("target block " + targetBlock + !craftClone.isLocationPartOfCraft(targetLoc));
                // target block should be Air or a liquid
                if (!craftClone.isLocationPartOfCraft(targetLoc) && !(targetBlock.getMaterial() == Material.AIR || targetBlock.getBlockData() instanceof Levelled)){
                    Ironclad.getPlugin().logDebug("Found blocking block at" + targetBlock);
                    successful = false;
                    break;
                }
                overwrittenBlocks.add(targetLoc);
                //just update blocks which are not the same
                if (!targetBlock.getBlockData().equals(oldBlock.getBlockData())) {
                    Ironclad.getPlugin().logDebug("block needs update " + targetBlock);
                    if (targetBlock.getBlockData() instanceof Directional)
                        newAttachedBlocks.add(targetBlock);
                    else
                        newBlocks.add(targetBlock);
                }
            }
        }

        //remove left over ship blocks
        for (SimpleBlock block : craftClone.getCraftDesign().getAllCraftBlocks(craftClone)){
            Ironclad.getPlugin().logDebug("left over ship block " + block + " overwritten contains " + overwrittenBlocks.contains(block.toVector()));
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

        //calculate teleport location of entities
        for (SimpleEntity entity : entities) {
            Location futureLoc = craftClone.getFutureLocation(entity.getLocation());
            entity.setLocationUpate(futureLoc.clone().subtract(entity.getLocation()).toVector());
            entity.setYawUpdate(futureLoc.getYaw() - entity.getLocation().getYaw());
        }


        //run a sync thread to update the calculated data
        BukkitTask task = new MoveCraftTask(craftClone, newBlocks, newAttachedBlocks, resetBlocks, resetAttachedBlocks, entities, successful).runTask(Ironclad.getPlugin());

        Ironclad.getPlugin().logDebug("Time for async calculation of craft movement: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
    }
}
