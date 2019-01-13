package at.pavlov.ironclad.listener;

import java.util.*;

import at.pavlov.ironclad.Enum.CommandList;
import at.pavlov.ironclad.Enum.SelectCraft;
import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.craft.Craft;

import at.pavlov.ironclad.craft.CraftDesign;
import at.pavlov.ironclad.craft.CraftManager;
import at.pavlov.ironclad.utils.IroncladUtil;
import com.google.common.base.Joiner;
import net.milkbowl.vault.economy.EconomyResponse;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import at.pavlov.ironclad.config.Config;
import at.pavlov.ironclad.Enum.MessageEnum;
import at.pavlov.ironclad.config.UserMessages;
import at.pavlov.ironclad.dao.PersistenceDatabase;


public class Commands implements TabExecutor
{
    private final Ironclad plugin;
    private final Config config;
    private final UserMessages userMessages;
    private final PersistenceDatabase persistenceDatabase;

    //<player,command to be performed>;
    private HashMap<UUID, SelectCraft> craftSelector = new HashMap<>();
    //<player,playerUID>;
    private HashMap<UUID,UUID> whitelistPlayer = new HashMap<>();



    public Commands(Ironclad plugin)
    {
        this.plugin = plugin;
        config = this.plugin.getMyConfig();
        userMessages = this.plugin.getMyConfig().getUserMessages();
        persistenceDatabase = this.plugin.getPersistenceDatabase();
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {

        Player player = null;
        if (sender instanceof Player)
        {
            player = (Player) sender;
        }

        if (cmd.getName().equalsIgnoreCase("ironclad"))
        {
            if (args.length >= 1)
            {
                //############## console and player commands ######################
                //ironclad reload
                if (args[0].equalsIgnoreCase("reload") )
                {
                    if (player == null || player.hasPermission("ironclad.admin.reload"))
                    {
                        // reload config
                        config.loadConfig();
                        sendMessage(sender, ChatColor.GREEN + "[Ironclad] Config loaded");
                    }
                    else
                        plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                    return true;
                }
                //ironclad save
                else if (args[0].equalsIgnoreCase("save"))
                {
                    if (player == null || player.hasPermission("ironclad.admin.reload"))
                    {
                        // save database
                        persistenceDatabase.saveAllCrafts(true);
                        sendMessage(sender, ChatColor.GREEN + "Ironclad database saved ");
                    }
                    else
                        plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                    return true;
                }
                //ironclad load
                else if (args[0].equalsIgnoreCase("load"))
                {
                    if (player == null || player.hasPermission("ironclad.admin.reload"))
                    {
                        // load database
                        persistenceDatabase.loadCrafts();
                        sendMessage(sender, ChatColor.GREEN + "Ironclad database loaded ");
                    }
                    else
                        plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                    return true;
                }
                //ironclad reset
                else if(args[0].equalsIgnoreCase("reset") && (player == null || player.hasPermission("ironclad.admin.reset")))
                {
                    //try first if there is no player "all" or "all_players"
                    OfflinePlayer offall = IroncladUtil.getOfflinePlayer("all");
                    OfflinePlayer offallplayers = IroncladUtil.getOfflinePlayer("all_players");
                    if (args.length >= 2 && (
                            (args[1].equals("all") && (offall==null || !offall.hasPlayedBefore()))||
                            (args[1].equals("all_players") && (offallplayers==null || !offallplayers.hasPlayedBefore()))))
                    {
                        //remove all ironclad
                        persistenceDatabase.deleteAllCrafts();
                        plugin.getCraftManager().deleteAllCrafts();
                        sendMessage(sender, ChatColor.GREEN + "All ironclad have been deleted");
                    }
                    else if (args.length >= 2 && args[1] != null)
                    {
                        // delete all craft entries for this player
                        OfflinePlayer offplayer = IroncladUtil.getOfflinePlayer(args[1]);
                        if (offplayer != null && offplayer.hasPlayedBefore())
                        {
                            boolean b1 = plugin.getCraftManager().deleteCrafts(offplayer.getUniqueId());
                            persistenceDatabase.deleteCrafts(offplayer.getUniqueId());
                            if (b1)
                            {
                                //there was an entry in the list
                                sendMessage(sender, ChatColor.GREEN + userMessages.getMessage(MessageEnum.CraftsReseted).replace("PLAYER", args[1]));
                            }
                            else
                            {
                                sendMessage(sender, ChatColor.RED + "Player " + ChatColor.GOLD + args[1] + ChatColor.RED + " has no ironclad.");
                            }
                        }
                        else
                        {
                            sendMessage(sender, ChatColor.RED + "Player " + ChatColor.GOLD + args[1] + ChatColor.RED + " not found");
                        }
                    }
                    else
                    {
                        sendMessage(sender, ChatColor.GREEN + "Missing player name " + ChatColor.GOLD + "'/ironclad reset <NAME>' or '/ironclad reset all' or '/ironclad reset all_players'");
                    }
                    return true;
                }
                //ironclad list
                else if(args[0].equalsIgnoreCase("list") && (player == null || player.hasPermission("ironclad.admin.list")))
                {
                    if (args.length >= 2)
                    {
                        //additional player name
                        OfflinePlayer offplayer = IroncladUtil.getOfflinePlayer(args[1]);
                        if (offplayer != null && offplayer.hasPlayedBefore()) {
                            sendMessage(sender, ChatColor.GREEN + "Craft list for " + ChatColor.GOLD + offplayer.getName() + ChatColor.GREEN + ":");
                            for (Craft craft : CraftManager.getCraftList().values()) {
                                if (craft.getOwner() != null && craft.getOwner().equals(offplayer.getUniqueId()))
                                    sendMessage(sender, ChatColor.GREEN + "Name:" + ChatColor.GOLD + craft.getCraftName() + ChatColor.GREEN + " design:" + ChatColor.GOLD + craft.getCraftDesign().getDesignName() + ChatColor.GREEN + " location:" + ChatColor.GOLD + craft.getOffset().toString());
                            }
                        }
                    }
                    else
                    {
                        //plot all ironclad
                        sendMessage(sender, ChatColor.GREEN + "List of all ironclad:");
                        for (Craft craft : CraftManager.getCraftList().values())
                        {
                            if (craft.getOwner() != null) {
                                OfflinePlayer owner = Bukkit.getOfflinePlayer(craft.getOwner());
                                sendMessage(sender, ChatColor.GREEN + "Name:" + ChatColor.GOLD + craft.getCraftName() + ChatColor.GREEN + " owner:" + ChatColor.GOLD + owner.getName() + ChatColor.GREEN + " location:" + ChatColor.GOLD + craft.getOffset().toString());
                            }
                        }
                    }
                    return true;
                }
                //ironclad create
                else if(args[0].equalsIgnoreCase(CommandList.CREATE.getCommand()))
                {
                    if (player != null && player.hasPermission(CommandList.CREATE.getPermission())) {
                        if (args.length >= 2) {
                            //check if the design name is valid
                            if (config.getDesignStorage().hasDesign(args[1])) {
                                sendMessage(sender, ChatColor.GREEN + "[Ironclad] Create design: " + ChatColor.GOLD + args[1]);
                                CraftDesign cannonDesign = config.getDesignStorage().getDesign(args[1]);
                                Ironclad.getPlugin().spawnCraft(cannonDesign, player.getLocation(), BlockFace.NORTH, player.getUniqueId());
                            }
                            else
                                sendMessage(sender, ChatColor.RED + "[Ironclad] Design not found Available designs are: " + StringUtils.join(plugin.getMyConfig().getDesignStorage().getDesignIds(),", "));
                        }
                        else
                            sendMessage(sender, ChatColor.RED + "[Ironclad] Usage: '/ironclad create <design>'");
                    }
                    else
                        plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                    return true;
                }
                //ironclad permissions
                else if(args[0].equalsIgnoreCase("permissions"))
                {
                    if (player == null || player.hasPermission("ironclad.admin.permissions"))
                    {
                        //given name in args[1]
                        if (args.length >= 2 && args[1]!=null)
                        {
                            Player permPlayer = Bukkit.getPlayer(args[1]);
                            if (permPlayer!=null)
                                displayAllPermissions(sender, permPlayer);
                            else
                                sendMessage(sender, ChatColor.GREEN + "Player not found. Usage: " + ChatColor.GOLD + "'/ironclad permissions <NAME>'");
                        }
                        //the command sender is also a player - return the permissions of the sender
                        else if (player != null)
                        {
                            displayAllPermissions(sender, player);
                        }
                        else
                            sendMessage(sender, ChatColor.GREEN + "Missing player name " + ChatColor.GOLD + "'/ironclad permissions <NAME>'");
                    }
                    else
                        plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                    return true;
                }



                //################### Player only commands #####################
                else if (player != null)
                {
                    //ironclad build
                    if (args[0].equalsIgnoreCase("build"))
                    {
                        if (!player.hasPermission("ironclad.player.command"))
                        {
                            plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                            return true;
                        }
                        // how to build a craft
                        userMessages.sendMessage(MessageEnum.HelpBuild, player);
                    }
                    //ironclad fire
                    else if (args[0].equalsIgnoreCase("pilot"))
                    {
                        if (!player.hasPermission("ironclad.player.command"))
                        {
                            plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                            return true;
                        }
                        // how to fire
                        userMessages.sendMessage(MessageEnum.HelpPilot, player);
                    }
                    //ironclad commands
                    else if (args[0].equalsIgnoreCase("commands"))
                    {
                        if (!player.hasPermission("ironclad.player.command"))
                        {
                            plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                            return true;
                        }
                        displayCommands(player);
                    }
                    //buy craft
                    else if(args[0].equalsIgnoreCase("buy"))
                    {
                        if (!player.hasPermission("ironclad.player.build"))
                        {
                            plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                            return true;
                        }
                        toggleBuyCannon(player, SelectCraft.BUY_CRAFT);
                        return true;
                    }
                    //rename craft
                    else if(args[0].equalsIgnoreCase("rename"))
                    {
                        if (!player.hasPermission("ironclad.player.rename"))
                        {
                            plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                            return true;
                        }
                        if (args.length >= 3 && args[1]!=null  && args[2]!=null)
                        {
                            //selection done by a string '/ironclad rename OLD NEW'
                            Craft craft = CraftManager.getCraft(args[1]);
                            if (craft != null)
                            {
                                MessageEnum message = plugin.getCraftManager().renameCraft(player, craft, args[2]);
                                userMessages.sendMessage(message, player, craft);
                            }
                        }
                        else
                            sendMessage(sender, ChatColor.RED + "Usage '/ironclad rename <OLD_NAME> <NEW_NAME>'");
                        return true;
                    }
                    //get information of craft
                    else if(args[0].equalsIgnoreCase("info"))
                    {
                        if (!player.hasPermission("ironclad.player.info"))
                        {
                            plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                            return true;
                        }
                        toggleCraftSelector(player, SelectCraft.INFO);
                    }
                    //get name of craft
                    else if(args[0].equalsIgnoreCase("dismantle"))
                    {
                        if (!player.hasPermission("ironclad.player.dismantle") && !player.hasPermission("ironclad.admin.dismantle"))
                        {
                            plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                            return true;
                        }
                        toggleCraftSelector(player, SelectCraft.DISMANTLE);
                    }
                    //list ironclad of this player name
                    else if(args[0].equalsIgnoreCase("list"))
                    {
                        if (!player.hasPermission("ironclad.player.list"))
                        {
                            plugin.logDebug("[Ironclad] Missing permission 'ironclad.player.list' for command /ironclad " + args[0]);
                            return true;
                        }
                        sendMessage(sender, ChatColor.GREEN + "Craft list for " + ChatColor.GOLD + player.getName() + ChatColor.GREEN + ":");
                        for (Craft craft : CraftManager.getCraftList().values())
                        {
                            if (craft.getOwner() != null && craft.getOwner().equals(player.getUniqueId()))
                                sendMessage(sender, ChatColor.GREEN + "Name:" + ChatColor.GOLD + craft.getCraftName() + ChatColor.GREEN + " design:" +
                                        ChatColor.GOLD + craft.getCraftDesign().getDesignName() + ChatColor.GREEN + " loc: " + ChatColor.GOLD + craft.getOffset().toString());
                        }
                        //create craft limit
                        int buildlimit = plugin.getCraftManager().getCraftBuiltLimit(player);
                        if (buildlimit < Integer.MAX_VALUE){
                            int ncannon = plugin.getCraftManager().getNumberOfCrafts(player.getUniqueId());
                            int newIronclad = buildlimit - ncannon;
                            if (newIronclad > 0)
                                sendMessage(sender, ChatColor.GREEN + "You can build " + ChatColor.GOLD + newIronclad + ChatColor.GREEN + " additional ironclad");
                            else
                                sendMessage(sender, ChatColor.RED + "You reached your maximum number of ironclad");
                        }
                    }
                    //ironclad reset
                    else if(args[0].equalsIgnoreCase("reset"))
                    {
                        if (!player.hasPermission("ironclad.player.reset"))
                        {
                            plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                            return true;
                        }
                        // delete all craft entries for this player
                        persistenceDatabase.deleteCrafts(player.getUniqueId());
                        plugin.getCraftManager().deleteCrafts(player.getUniqueId());
                        userMessages.sendMessage(MessageEnum.CraftsReseted, player);
                    }
                    //get blockdata
                    else if(args[0].equalsIgnoreCase("blockdata"))
                    {
                        if (!player.hasPermission("ironclad.player.blockdata"))
                        {
                            plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                            return true;
                        }
                        toggleCraftSelector(player, SelectCraft.BLOCK_DATA);
                    }
                    //claim ironclad in the surrounding
                    else if(args[0].equalsIgnoreCase(CommandList.CLAIM.getCommand()))
                    {
                        if (!player.hasPermission(CommandList.CLAIM.getPermission()))
                        {
                            plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                            return true;
                        }
                        userMessages.sendMessage(MessageEnum.CmdClaimCraftStarted, player);
                        Ironclad.getPlugin().getCraftManager().claimCraftsInBox(player.getLocation(), player.getUniqueId());
                        userMessages.sendMessage(MessageEnum.CmdClaimCraftsFinished, player);

                    }
                    //no help message if it is forbidden for this player
                    else
                    {
                        if (!player.hasPermission("ironclad.player.command"))
                        {
                            plugin.logDebug("[Ironclad] " + sender.getName() + " has no permission for command /ironclad " + args[0]);
                            return true;
                        }
                        // display help
                        userMessages.sendMessage(MessageEnum.HelpText, player);
                        return true;
                    }
                }
                else
                {
                    plugin.logDebug("This command can only be used by a player");
                    return false;
                }



            }
            //console command
            else
            {
                //no help message if it is forbidden for this player
                if(player != null)
                {
                    if(player.hasPermission("ironclad.player.command"))
                    {
                        // display help
                        userMessages.sendMessage(MessageEnum.HelpText, player);
                    }
                    else
                    {
                        plugin.logInfo("Player has no permission: ironclad.player.command");
                    }

                }
                else
                {
                    plugin.logInfo("Ironclad plugin v" + plugin.getPluginDescription().getVersion() + " is running");
                }
            }
            return true;
        }
        return false;
    }

    /**
     * sends a message to the console of the player. Console messages will be striped form color
     * @param sender player or console
     * @param str message
     */
    private void sendMessage(CommandSender sender, String str)
    {
        if (sender == null)
            return;

        //strip color of console messages
        if (!(sender instanceof Player))
            str = ChatColor.stripColor(str);

        sender.sendMessage(str);
    }

    /**
     * this player will be removed from the selecting mode
     * @param player the player will be removed
     * @param cmd this command will be performed when the craft is selected
     */
    public void addCraftSelector(Player player, SelectCraft cmd)
    {
        if (player == null || cmd == null)
            return;

        if (!isSelectingMode(player))
        {
            craftSelector.put(player.getUniqueId(),cmd);
            if (isBlockSelectingMode(player))
                userMessages.sendMessage(MessageEnum.CmdSelectBlock, player);
            else
                userMessages.sendMessage(MessageEnum.CmdSelectCraft, player);
        }
    }

    /**
     * this player will be removed from the selecting mode
     * @param player the player will be removed
     */
    public void removeCraftSelector(Player player)
    {
        if (player == null)
            return;

        if (isSelectingMode(player))
        {
            craftSelector.remove(player.getUniqueId());
            userMessages.sendMessage(MessageEnum.CmdSelectCanceled, player);
        }
    }

    /**
     * selecting mode will be toggled
     * @param player the player using the selecting mode
     * @param cmd this command will be performed when the craft is selected
     */
    public void toggleCraftSelector(Player player, SelectCraft cmd)
    {
        if (player == null)
            return;

        if (isSelectingMode(player))
            removeCraftSelector(player);
        else
            addCraftSelector(player, cmd);
    }

    /**
     * this player will be removed from the buying mode
     * @param player the player will be removed
     * @param cmd this command will be performed when the craft is selected
     */
    public void addBuyCannon(Player player, SelectCraft cmd)
    {
        if (player == null || cmd == null)
            return;

        if (!isSelectingMode(player))
        {
            craftSelector.put(player.getUniqueId(),cmd);
            userMessages.sendMessage(MessageEnum.CmdBuyCraft, player);
        }
    }

    /**
     * this player will be removed from the buying mode
     * @param player the player will be removed
     */
    public void removeBuyCannon(Player player)
    {
        if (player == null)
            return;

        if (isSelectingMode(player))
        {
            craftSelector.remove(player.getUniqueId());
            userMessages.sendMessage(MessageEnum.CmdSelectCanceled, player);
        }
    }

    /**
     * buying mode will be toggled
     * @param player the player using the selecting mode
     * @param cmd this command will be performed when the craft is selected
     */
    public void toggleBuyCannon(Player player, SelectCraft cmd)
    {
        if (player == null)
            return;

        if (isSelectingMode(player))
            removeBuyCannon(player);
        else
            addBuyCannon(player, cmd);
    }


    /**
     * Checks if this player is in selecting mode
     * @param player player to check
     * @return true if in selecting mode
     */
    public boolean isSelectingMode(Player player) {
        return player != null && craftSelector.containsKey(player.getUniqueId());
    }

    public boolean isBlockSelectingMode(Player player){
        SelectCraft cmd = craftSelector.get(player.getUniqueId());
        return cmd.equals(SelectCraft.BLOCK_DATA);
    }

    /**
     * adds a new selected craft for this player
     * @param player player that selected the craft
     * @param block the selected block
     */
    public void setSelectedBlock(Player player, Block block)
    {
        if (player == null || block == null)
            return;

        SelectCraft cmd = craftSelector.get(player.getUniqueId());
        if (cmd != null)
        {
            switch (cmd){
                case BLOCK_DATA:{
                    player.sendMessage(block.getBlockData().getAsString());
                    break;
                }
            }
        }
        craftSelector.remove(player.getUniqueId());
    }


    /**
     * adds a new selected craft for this player
     * @param player player that selected the craft
     * @param craft the selected craft
     */
    public void setSelectedCannon(Player player, Craft craft)
    {
        if (player == null || craft == null)
            return;

        SelectCraft cmd = craftSelector.get(player.getUniqueId());
        if (cmd != null)
        {
            switch (cmd){
                case INFO:{
                    userMessages.sendMessage(MessageEnum.CraftInfo, player, craft);
                    IroncladUtil.playSound(craft.getMuzzle(), craft.getCraftDesign().getSoundSelected());
                    break;
                }
                case DISMANTLE:{
                    plugin.getCraftManager().dismantleCraft(craft, player);
                    break;
                }
                case BUY_CRAFT:{
                    if (craft.isPaid()){
                        userMessages.sendMessage(MessageEnum.ErrorAlreadyPaid, player, craft);
                        IroncladUtil.playErrorSound(craft.getMuzzle());
                    }
                    else{
                        //redraw money if required
                        if (plugin.getEconomy() != null && craft.getCraftDesign().getEconomyBuildingCost() > 0) {
                            EconomyResponse r = plugin.getEconomy().withdrawPlayer(player, craft.getCraftDesign().getEconomyBuildingCost());
                            if (!r.transactionSuccess()) {
                                userMessages.sendMessage(MessageEnum.ErrorNoMoney, player, craft);
                                IroncladUtil.playErrorSound(craft.getMuzzle());
                            }
                            else {
                                craft.boughtByPlayer(player.getUniqueId());
                                //IroncladUtil.playSound();
                                userMessages.sendMessage(MessageEnum.CmdPaidCraft, player, craft);
                                IroncladUtil.playSound(craft.getMuzzle(), craft.getCraftDesign().getSoundSelected());
                            }
                        }
                    }
                    break;
                }
            }
        }
        craftSelector.remove(player.getUniqueId());
    }

    /**
     * displays the given permission of the player
     * @param sender command sender
     * @param player the permission of this player will be checked
     * @param permission permission as string
     */
    private void displayPermission(CommandSender sender, Player player, String permission)
    {
        if (player == null || permission == null) return;

        //request permission
        boolean hasPerm = player.hasPermission(permission);
        //add some color
        String perm;
        if (hasPerm)
            perm = ChatColor.GREEN + "TRUE";
        else
            perm = ChatColor.RED + "FALSE";
        sendMessage(sender, ChatColor.YELLOW + permission + ": " + perm);
    }


    /**
     * display all default permissions of the player to the sender
     * @param sender command sender
     * @param permPlayer the permission of this player will be checked
     */
    private void displayAllPermissions(CommandSender sender, Player permPlayer)
    {
        sendMessage(sender, ChatColor.GREEN + "Permissions for " + ChatColor.GOLD + permPlayer.getName() + ChatColor.GREEN + ":");
        displayPermission(sender, permPlayer, "ironclad.player.command");
        displayPermission(sender, permPlayer, "ironclad.player.info");
        displayPermission(sender, permPlayer, "ironclad.player.help");
        displayPermission(sender, permPlayer, "ironclad.player.rename");
        displayPermission(sender, permPlayer, "ironclad.player.build");
        displayPermission(sender, permPlayer, "ironclad.player.dismantle");
        displayPermission(sender, permPlayer, "ironclad.player.redstone");
        displayPermission(sender, permPlayer, "ironclad.player.load");
        displayPermission(sender, permPlayer, "ironclad.player.adjust");
        displayPermission(sender, permPlayer, "ironclad.player.fire");
        displayPermission(sender, permPlayer, "ironclad.player.autoaim");
        displayPermission(sender, permPlayer, "ironclad.player.observer");
        displayPermission(sender, permPlayer, "ironclad.player.tracking");
        displayPermission(sender, permPlayer, "ironclad.player.autoreload");
        displayPermission(sender, permPlayer, "ironclad.player.thermometer");
        displayPermission(sender, permPlayer, "ironclad.player.ramrod");
        displayPermission(sender, permPlayer, "ironclad.player.target");
        displayPermission(sender, permPlayer, "ironclad.player.whitelist");
        displayPermission(sender, permPlayer, "ironclad.player.reset");
        displayPermission(sender, permPlayer, "ironclad.player.list");
        displayPermission(sender, permPlayer, "ironclad.projectile.default");
        displayPermission(sender, permPlayer, "ironclad.limit.limitA");
        displayPermission(sender, permPlayer, "ironclad.limit.limitB");
        int newBuildlimit = plugin.getCraftManager().getNewBuildLimit(permPlayer);
        if (newBuildlimit==Integer.MAX_VALUE)
            sendMessage(sender, ChatColor.YELLOW + "no Permission ironclad.limit.x (with 0<=x<=100)");
        else
            displayPermission(sender, permPlayer, "ironclad.limit." + newBuildlimit);
        int numberCrafts = plugin.getCraftManager().getNumberOfCrafts(permPlayer.getUniqueId());
        int maxCrafts = plugin.getCraftManager().getCraftBuiltLimit(permPlayer);
        if (maxCrafts == Integer.MAX_VALUE)
            sendMessage(sender, ChatColor.YELLOW + "Built ironclad: " + ChatColor.GOLD + numberCrafts);
        else
            sendMessage(sender, ChatColor.YELLOW + "Built ironclad: " + ChatColor.GOLD + numberCrafts + "/" + maxCrafts);
        displayPermission(sender, permPlayer, "ironclad.admin.reload");
        displayPermission(sender, permPlayer, "ironclad.admin.reset");
        displayPermission(sender, permPlayer, "ironclad.admin.list");
        displayPermission(sender, permPlayer, "ironclad.admin.create");
        displayPermission(sender, permPlayer, "ironclad.admin.dismantle");
        displayPermission(sender, permPlayer, "ironclad.admin.give");
        displayPermission(sender, permPlayer, "ironclad.admin.permissions");
        displayPermission(sender, permPlayer, "ironclad.admin.blockdata");
    }

    /**
     * displays the given permission of the player
     * @param player the permission of this player will be checked
     * @param permission permission as string
     */
    private void displayCommand(Player player, String command, String permission)
    {
        if (player == null) return;

        if (permission == null || player.hasPermission(permission))
            sendMessage(player, ChatColor.YELLOW + command);
    }


    /**
     * displays all possible commands for the player
     * @param player the permission of this player will be checked
     */
    private void displayCommands(Player player) {
        List<CommandList> playerCmd = new ArrayList<>();
        List<CommandList> adminCmd = new ArrayList<>();
        for (CommandList cmd : CommandList.values()) {
            if (cmd.isAdminCmd())
                adminCmd.add(cmd);
            else
                playerCmd.add(cmd);
        }
        sendMessage(player, ChatColor.GOLD + "Player commands:" + ChatColor.YELLOW);
        for (CommandList cmd : playerCmd)
            displayCommand(player, cmd.getUsage(), cmd.getPermission());

        sendMessage(player, ChatColor.GOLD + "Admin commands:" + ChatColor.YELLOW);
        for (CommandList cmd : adminCmd)
            displayCommand(player, cmd.getUsage(), cmd.getPermission());
    }


    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command cmd, String commandLabel, String[] args) {
        List<String> cmdList = new ArrayList<>();
        if (cmd.getName().equalsIgnoreCase("ironclad"))
        {
            String[] split = new String[args.length + 1];
            System.arraycopy(args, 0, split, 1, args.length);
            split[0] = cmd.getName();

            String full = Joiner.on(" ").join(split);
            for (CommandList commandList : CommandList.values()){
                if (commandList.getUsage().contains(full) && (commandList.getPermission() == null || commandSender.hasPermission(commandList.getPermission())))
                    cmdList.add(commandList.getUsage().substring(full.lastIndexOf(" ")+2));
            }

        }
        return cmdList;
    }
}
