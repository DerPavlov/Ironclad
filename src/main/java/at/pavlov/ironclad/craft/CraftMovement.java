package at.pavlov.ironclad.craft;

import at.pavlov.ironclad.Enum.MessageEnum;
import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.config.Config;
import at.pavlov.ironclad.config.UserMessages;
import at.pavlov.ironclad.utils.IroncladUtil;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.*;


public class CraftMovement {
    private final Ironclad plugin;
    private final UserMessages userMessages;
    private final Config config;

    //<Player,craft name>
    private HashMap<UUID, UUID> inPilotingMode = new HashMap<UUID, UUID>();
    //<craft uid, timespamp>
    private HashMap<UUID, Long> lastInteraction = new HashMap<UUID, Long>();


    /**
     * Constructor
     * @param plugin Ironclad main class
     */
	public CraftMovement(Ironclad plugin) {
        this.plugin = plugin;
        this.config = plugin.getMyConfig();
        this.userMessages = plugin.getMyConfig().getUserMessages();
    }

    /**
     * starts the scheduler which moves the crafts in minecraft
     */
    public void initMovementExecute()
    {
        //changing angles for aiming mode
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
        {
            public void run()
            {
                //long startTime = System.nanoTime();
                //updateAimingMode();
                //plugin.logDebug("Time update aiming: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");
            }
        }, 1L, 1L);
    }

    public void moveCraft(BlockFace blockFace){

    }

    /**
     *
     */
    public void moveCraft(){

    }

    /**
     * moves the craft in the direction of the given vector
     * @param movement vector of movement
     */
    public void moveCraft(Vector movement){


    }

    /**
     * switches aming mode for this craft
     * @param player - player in aiming mode
     * @param craft - operated craft
     */
    public void pilotingMode(Player player, Craft craft)
    {
        if (player == null)
            return;

        boolean isAimingMode = inPilotingMode.containsKey(player.getUniqueId());
        if (isAimingMode)
        {
            if (craft == null)
                craft = getCraftInAimingMode(player);
        }
        //enable aiming mode. Sentry cannons can't be operated by players
        else if(craft != null)
        {
            //check if player has permission to aim
            if (player.hasPermission(craft.getCraftDesign().getPermissionPiloting()))
            {
                //check if pilot is on the craft
                if (craft.isEntityOnShip(player))
                {
                    MessageEnum message = enablePilotingMode(player, craft);
                    userMessages.sendMessage(message, player, craft);
                }
                else
                {
                    userMessages.sendMessage(MessageEnum.AimingModeTooFarAway, player, craft);
                }

            }
            else
            {
                //no Permission to aim
                userMessages.sendMessage(MessageEnum.PermissionErrorAdjust, player, craft);
            }
        }
    }

    /**
     * enable the aiming mode
     * @param player player how operates the craft
     * @param craft the craft in piloting mode
     * @return message for the user
     */
    public MessageEnum enablePilotingMode(Player player, Craft craft)
    {
        if (player == null)
            return null;

        if (!player.hasPermission(craft.getCraftDesign().getPermissionPiloting()))
            return MessageEnum.PermissionErrorAutoaim;

        inPilotingMode.put(player.getUniqueId(), craft.getUID());

        IroncladUtil.playSound(player.getEyeLocation(), craft.getCraftDesign().getSoundEnablePilotingMode());

        return MessageEnum.PilotingModeEnabled;

    }


    /**
     * disables the aiming mode for this player
     * @param player - player in aiming mode
     * @return message for the player
     */
    public MessageEnum disableAimingMode(Player player)
    {
        //player.playSound(player.getEyeLocation(), Sound.MINECART_BASE, 0.25f, 0.75f);
        Craft cannon = getCraftInAimingMode(player);
        if (cannon!=null)
            IroncladUtil.playSound(player.getEyeLocation(), cannon.getCraftDesign().getSoundDisablePilotingMode());
        return disableAimingMode(player, cannon);
    }

    /**
     * disables the aiming mode for this player
     * @param player player in aiming mode
     * @param craft operated craft
     * @return message for the player
     */
    public MessageEnum disableAimingMode(Player player, Craft craft)
    {
        if (player == null)
            return null;

        if (inPilotingMode.containsKey(player.getUniqueId()))
        {
            //player in map -> remove
            inPilotingMode.remove(player.getUniqueId());

            return MessageEnum.AimingModeDisabled;
        }
        return null;
    }


	public boolean isInPilotingMode(UUID player) {
		return player != null && inPilotingMode.containsKey(player);
	}

    /**
     * returns the craft of the player if he is in aiming mode
     * @param player the player who is in aiming mode
     * @return the craft which is in aiming mode by the given player
     */
    public Craft getCraftInAimingMode(Player player)
    {
        if (player == null)
            return null;
        //return the craft of the player if he is in aiming mode
        return CraftManager.getCraft(inPilotingMode.get(player.getUniqueId()));
    }
}
