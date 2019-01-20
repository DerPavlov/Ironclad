package at.pavlov.ironclad.scheduler;

import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.container.SimpleBlock;
import at.pavlov.ironclad.container.SimpleEntity;
import at.pavlov.ironclad.craft.Craft;
import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.boydti.fawe.object.collection.BlockVectorSet;
import com.sk89q.worldedit.Vector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.util.*;

public class MoveCalculateTask extends BukkitRunnable{
    private final Craft craftClone;
    private final AsyncWorld asyncWorld;


    public MoveCalculateTask(Craft craftClone, AsyncWorld asyncWorld){
        this.craftClone = craftClone;
        this.asyncWorld = asyncWorld;
    }

    @Override
    public void run() {
        long startTime = System.nanoTime();

        ArrayList<AsyncBlock> newBlocks = new ArrayList<>();
        ArrayList<AsyncBlock> newAttachedBlocks = new ArrayList<>();
        ArrayList<AsyncBlock> resetBlocks = new ArrayList<>();
        ArrayList<AsyncBlock> resetAttachedBlocks = new ArrayList<>();
        BlockVectorSet overwrittenBlocks = new BlockVectorSet();
        boolean successful = true;

        Vector targetLoc;
        AsyncBlock targetBlock;

        //perform craft calculations
        for (SimpleBlock designBlock : craftClone.getCraftDesign().getAllCraftBlocks(craftClone)){
            targetLoc = designBlock.toVector();
            AsyncBlock oldBlock = asyncWorld.getBlockAt(targetLoc.getBlockX(), targetLoc.getBlockY(), targetLoc.getBlockZ());
            Ironclad.getPlugin().logDebug("old block " + oldBlock);
            if (oldBlock.getType() == Material.AIR || oldBlock.getBlockData() instanceof Levelled){
                Ironclad.getPlugin().logDebug("Found destroyed craft block " + oldBlock);
            }
            else{
                // move the craft to the new location. oldblock was updated to the new location
                targetLoc = craftClone.transformToFutureLocation(designBlock.toVector());
                //remove decimal places to the the correct block location
//                targetLoc.setX(Math.floor(targetLoc.getX()));
//                targetLoc.setY(Math.floor(targetLoc.getY()));
//                targetLoc.setZ(Math.floor(targetLoc.getX()));

                targetBlock = asyncWorld.getBlockAt(targetLoc.getBlockX(), targetLoc.getBlockY(), targetLoc.getBlockZ());
                if (targetBlock == null){
                    Ironclad.getPlugin().logDebug("target block " + targetLoc + " does not exist in snapshot");
                    continue;
                }

                Ironclad.getPlugin().logDebug("target block " + targetBlock + " is part of craft " + craftClone.isLocationPartOfCraft(targetLoc));
                // target block should be Air or a liquid
                if (!craftClone.isLocationPartOfCraft(targetLoc) && !(targetBlock.getType() == Material.AIR || targetBlock.getBlockData() instanceof Levelled)){
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
        for (SimpleBlock designBlock : craftClone.getCraftDesign().getAllCraftBlocks(craftClone)){
            targetLoc = designBlock.toVector();
            Ironclad.getPlugin().logDebug("left over ship block " + targetLoc + " overwritten contains " + overwrittenBlocks.contains(targetLoc));
            if (!overwrittenBlocks.contains(targetLoc)){
                Ironclad.getPlugin().logDebug("Found left over block");
                targetBlock = asyncWorld.getBlockAt(targetLoc.getBlockX(), targetLoc.getBlockY(), targetLoc.getBlockZ());
                if (targetBlock.getBlockData() instanceof Directional) {
                    targetBlock.setBlockData(Bukkit.createBlockData(Material.AIR));
                    resetAttachedBlocks.add(targetBlock);
                }
                else{
                    targetBlock.setBlockData(Bukkit.createBlockData(Material.AIR));
                    resetBlocks.add(targetBlock);
                }
            }
        }

        if (successful)
            asyncWorld.commit();
        else
            asyncWorld.clear();

//        //calculate teleport location of entities
//        for (SimpleEntity entity : entities) {
//            Location futureLoc = craftClone.getFutureLocation(entity.getLocation());
//            entity.setLocationUpate(futureLoc.clone().subtract(entity.getLocation()).toVector());
//            entity.setYawUpdate(futureLoc.getYaw() - entity.getLocation().getYaw());
//        }


        //run a sync thread to update the calculated data
//        BukkitTask task = new MoveCraftTask(craftClone, newBlocks, newAttachedBlocks, resetBlocks, resetAttachedBlocks, entities, successful).runTask(Ironclad.getPlugin());

        Ironclad.getPlugin().logDebug("Time for async calculation of craft movement: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
    }
}
