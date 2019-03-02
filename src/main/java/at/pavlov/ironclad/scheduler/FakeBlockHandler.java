package at.pavlov.ironclad.scheduler;

import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.Enum.FakeBlockType;
import at.pavlov.ironclad.container.FakeBlockEntry;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;


public class FakeBlockHandler {
    private final Ironclad plugin;

    private ArrayList<FakeBlockEntry> list = new ArrayList<FakeBlockEntry>();

    private long lastCruising;
    private long lastImpactPredictor;


    /**
     * Constructor
     * @param plugin - Ironclad instance
     */
    public FakeBlockHandler(Ironclad plugin)
    {
        this.plugin = plugin;
    }

    /**
     * starts the scheduler of the teleporter
     */
    public void setupScheduler()
    {
        //changing angles for cruising mode
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
        {
            public void run() {
                removeOldBlocks();
                removeOldBlockType();
            }

        }, 1L, 1L);
    }


    /**
     * removes old blocks form the players vision
     */
    private void removeOldBlocks()
    {
        Iterator<FakeBlockEntry> iter = list.iterator();
        while(iter.hasNext())
        {
            FakeBlockEntry next = iter.next();
            Player player = next.getPlayerBukkit();

            //if player is offline remove this one
            if (player == null) {
                iter.remove();
                continue;
            }

            if (next.isExpired())
            {
                //send real block to player
                Location loc = next.getLocation();
                if (loc != null)
                {
                    player.sendBlockChange(loc, loc.getBlock().getBlockData());
                    // plugin.logDebug("expired fake block: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ", " + next.getType().toString());
                }
                //remove this entry
                iter.remove();
            }
        }
    }

    /**
     * removes previous entries for this type of fake blocks
     */
    private void removeOldBlockType()
    {
        Iterator<FakeBlockEntry> iter = list.iterator();
        while(iter.hasNext())
        {
            FakeBlockEntry next = iter.next();
            //if older and if the type matches
            if (next.getStartTime() < (lastCruising - 50) && (next.getType() == FakeBlockType.CRUISING))
            {
                //send real block to player
                Player player = next.getPlayerBukkit();
                Location loc = next.getLocation();
                if (player != null && loc != null)
                {
                    player.sendBlockChange(loc, loc.getBlock().getBlockData());
                }

                //remove this entry
                iter.remove();
                //plugin.logDebug("remove older fake entry: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ", " + next.getType().toString() + " stime " + next.getStartTime());
            }
        }
    }


    /**
     * creates a sphere of fake block and sends it to the given player
     * @param player the player to be notified
     * @param loc center of the sphere
     * @param r radius of the sphere
     * @param blockData material of the fake block
     * @param duration delay until the block disappears again in s
     */
    public void imitatedSphere(Player player, Location loc, int r, BlockData blockData, FakeBlockType type, double duration)
    {
        if(loc == null || player == null)
            return;

        for(int x = -r; x <=r; x++)
        {
            for(int y = -r; y<=r; y++)
            {
                for(int z = -r; z<=r; z++)
                {
                    Location newL = loc.clone().add(x, y, z);
                    if(newL.distance(loc)<=r)
                    {
                        sendBlockChangeToPlayer(player, newL, blockData, type, duration);
                    }
                }
            }
        }
    }

    /**
     * creates a line of blocks at the give location
     * @param loc starting location of the line
     * @param direction direction of the line
     * @param offset offset from the starting point
     * @param length lenght of the line
     * @param player name of the player
     */
    public void imitateLine(final Player player, Location loc, Vector3 direction, int offset, int length, BlockStateHolder blockData, FakeBlockType type, double duration)
    {
        if(loc == null || player == null)
            return;

        BlockIterator iter = new BlockIterator(loc.getWorld(), loc.toVector(), new Vector(direction.getX(), direction.getY(), direction.getZ()), offset, length);
        while (iter.hasNext())
        {
            sendBlockChangeToPlayer(player, iter.next().getLocation(), BukkitAdapter.adapt(blockData), type, duration);
        }

    }

    /**
     * sends fake block to the given player
     * @param player player to display the blocks
     * @param loc location of the block
     * @param blockData type of the block
     * @param duration how long to remove the block in [s]
     */
    private void sendBlockChangeToPlayer(final Player player, final Location loc, BlockData blockData, FakeBlockType type, double duration)
    {
        //only create block in air
        if(loc.getBlock().isEmpty())
        {
            long time = System.currentTimeMillis();
            FakeBlockEntry fakeBlockEntry = new FakeBlockEntry(loc, player, type, (long) (duration*20.0));


            boolean found = false;
            for (FakeBlockEntry block : list)
            {
                if (block.equals(fakeBlockEntry))
                {
                    //renew entry
                    //plugin.logDebug("renew block at: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ", " + type.toString());
                    block.setStartTime(System.currentTimeMillis());
                    found = true;
                    //there is only one block here
                    break;
                }
            }
            if (!found)
            {
                //plugin.logDebug("new block at: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ", " + type.toString());
                //player.sendBlockChange(loc, material.getType(), (byte) material.getData());
                player.sendBlockChange(loc, blockData);
                list.add(fakeBlockEntry);
            }

            if (type == FakeBlockType.CRUISING)
                lastCruising = System.currentTimeMillis();

        }
    }

    /**
     * returns true if the distance is in between the min and max limits of the imitate block distance
     * @param player player the check
     * @param loc location of the block
     * @return true if the distance is in the limits
     */
    public boolean isBetweenLimits(Player player, Location loc)
    {
        if (player == null || loc == null)
            return false;

        double dist = player.getLocation().distance(loc);
        if (dist > plugin.getMyConfig().getImitatedBlockMinimumDistance() &&
            dist < plugin.getMyConfig().getImitatedBlockMaximumDistance())
            return true;
        return false;
    }

    /**
     * returns true if the distance is below max limit of the imitate block distance
     * @param player player the check
     * @param loc location of the block
     * @return true if the distance is smaller than upper limit
     */
    public boolean belowMaxLimit(Player player, Location loc)
    {
        if (player == null || loc == null)
            return false;

        double dist = player.getLocation().distance(loc);
        if (dist < plugin.getMyConfig().getImitatedBlockMaximumDistance())
            return true;
        return false;
    }

}