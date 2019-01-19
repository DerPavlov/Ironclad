package at.pavlov.ironclad.scheduler;


import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.container.SimpleBlock;
import at.pavlov.ironclad.craft.Craft;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

public class MoveCraftTask extends BukkitRunnable {

    final private Craft craftClone;
    final private List<SimpleBlock> newBlocks;
    final private List<SimpleBlock> newAttachedBlocks;
    private final List<SimpleBlock> resetBlocks;
    private final List<SimpleBlock> resetAttachedBlocks;
    final private Set<Entity> entities;
    private final boolean successful;


    MoveCraftTask(Craft craftClone, List<SimpleBlock> newBlocks, List<SimpleBlock> newAttachedBlocks, List<SimpleBlock> resetBlocks, List<SimpleBlock> resetAttachedBlocks, Set<Entity> entities, boolean successful){
        this.craftClone = craftClone;
        this.newBlocks = newBlocks;
        this.newAttachedBlocks = newAttachedBlocks;
        this.resetBlocks = resetBlocks;
        this.resetAttachedBlocks = resetAttachedBlocks;
        this.entities = entities;
        this.successful = successful;
    }

    @Override
    public void run() {
        Ironclad.getPlugin().getCraftMovementManager().performCraftMovement(craftClone, newBlocks, newAttachedBlocks, resetBlocks, resetAttachedBlocks, entities, successful);
    }
}
