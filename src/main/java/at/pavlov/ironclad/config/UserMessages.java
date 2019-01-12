package at.pavlov.ironclad.config;

import java.io.*;
import java.util.*;

import at.pavlov.ironclad.Enum.MessageEnum;
import at.pavlov.ironclad.cannon.Craft;
import at.pavlov.ironclad.utils.IroncladUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import at.pavlov.ironclad.Ironclad;

public class UserMessages {
	private FileConfiguration customLanguage = null;
	private File customLanguageFile = null;
	
	
	private final HashMap<String, String> messageMap = new HashMap<String, String>();


	private final Ironclad plugin;
	
	public UserMessages(Ironclad plugin){
		this.plugin = plugin;
	}
		 
	public void loadLanguage()
	{
		this.loadCustom("localization");
		
		//copy german language
		File localizationGerman = new File(plugin.getDataFolder(), "localization/localization_german.yml");

        localizationGerman.getParentFile().mkdirs();
		if (!localizationGerman.exists())
		{
			IroncladUtil.copyFile(plugin.getResource("localization/localization_german.yml"), localizationGerman);
		}
		//copy english language
		File localizationEnglish = new File(plugin.getDataFolder(), "localization/localization_english.yml");
		if (!localizationEnglish.exists())
		{
			IroncladUtil.copyFile(plugin.getResource("localization/localization.yml"), localizationEnglish);
		}
	}
		
	/**
	 * load the localization file from the disk
	 * @param filename - name of the file
	 */
	private void loadCustom(String filename)
	{
		reloadCustomLanguage(filename);
		customLanguage.options().copyDefaults(true);
		saveCustomLanguage();
		
		//load all messages
		for (MessageEnum keyEnum : MessageEnum.values())
		{
			String key = keyEnum.getString();
			String entry = getEntry(key);
			if (!entry.equals(""))
			{
				messageMap.put(key, entry);
			}
			else
			{
				plugin.logSevere("Missing entry " + key + " in the localization file");
				messageMap.put(key, "Missing entry");
			}
		}
	}

	/**
	 * get a message string from the user messages
	 * @param key the requested message
	 * @return the message
	 */
	private String getEntry(String key)
	{
		String entry = customLanguage.getString(key);
		String replace;
		
		if (entry == null)
		{
			entry = "missing string: " + key;
			plugin.logSevere("Missing string " + key + " in localization file: ");
		}
		
		
		//replace red color
		replace = "" + ChatColor.RED;
		entry = entry.replace("ChatColor.RED ", replace);
		entry = entry.replace("RED ", replace);
		//replace green color
		replace = "&A";
		entry = entry.replace("ChatColor.GREEN ", replace);
		entry = entry.replace("GREEN ", replace);
		//replace yellow color
		replace = "&E";
		entry = entry.replace("ChatColor.YELLOW ", replace);
		entry = entry.replace("YELLOW ", replace);
		//replace gold color
		replace = "&6";
		entry = entry.replace("ChatColor.GOLD ", replace);
		entry = entry.replace("GOLD ", replace);
		entry = ChatColor.translateAlternateColorCodes('&', entry);
		
		//replace new line
		replace = "\n ";
		entry =  entry.replace("NEWLINE ", replace);
		//plugin.logDebug(entry);
		return ChatColor.translateAlternateColorCodes('&', entry);
	}



	private void reloadCustomLanguage(String filename)
	{
	    if (customLanguageFile == null) 
	    {
	    	customLanguageFile = new File(getDataFolder(), filename + ".yml");
	    }
	    customLanguage = YamlConfiguration.loadConfiguration(customLanguageFile);
	 
	    // Look for defaults in the jar
        try {
            Reader defConfigStream = new InputStreamReader(plugin.getResource("localization/" + filename + ".yml"), "UTF8");
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            customLanguage.setDefaults(defConfig);
        } catch (UnsupportedEncodingException e) {
            plugin.logSevere("Unsupported encoding: " + e);
        }

	}


	private String getDataFolder()
	{
		return "plugins/Ironclad/localization/";
	}


	private void saveCustomLanguage()
	{
	    if (customLanguage == null || customLanguageFile == null) 
	    {
	    return;
	    }
	    try 
	    {
	        customLanguage.save(customLanguageFile);
	    } catch (IOException ex) 
	    {
	        plugin.logSevere("Could not save config to " + customLanguageFile);
	    }
	}

	/**
	 * sends a message to the player
     * @param messageEnum message to display
     * @param player which player gets this message
     * @param craft which craft parameter will be displayed
     */
	public void sendMessage(MessageEnum messageEnum, UUID player, Craft craft)
	{
		//no player no message
		if (player != null)
            sendMessage(messageEnum, Bukkit.getPlayer(player), craft);
	}
	
	/**
	 * sends a message to the player
     * @param messageEnum message to display
     * @param player which player gets this message
     */
	public void sendMessage(MessageEnum messageEnum, Player player)
	{
        sendMessage(messageEnum, player, null);
	}
	
	/**
	 * sends a message to the player
     * @param messageEnum message to display
     * @param player which player gets this message
     * @param craft which craft parameter will be displayed
     */
	public void sendMessage(MessageEnum messageEnum, Player player, Craft craft)
	{
		//no player no message
		if (player == null) return;
		if (messageEnum == null) return;
		
		//get message from map
		String message = getMessage(messageEnum, player, craft);
		
		//send message to player
		sendMessage(message, player);
	}

    public void sendImpactMessage(Player player, Location impact, boolean canceled)
    {
        //no player no message
        if (player == null)
            return;
        //no permission no message
        if (!player.hasPermission("ironclad.player.impactMessage"))
            return;

        Location playerLoc = player.getLocation();

        String message;
        MessageEnum messageEnum;

        if (!canceled) {
            //the projectile exploded
            messageEnum = MessageEnum.ProjectileExplosion;
        }
        else {
            //the explosion was canceled
            messageEnum = MessageEnum.ProjectileCanceled;
        }

        message = messageMap.get(messageEnum.getString());

        if (message == null){
            plugin.logSevere("No " + messageEnum.getString() + " in localization file");
            return;
        }
        //if the message is something like this Explosion: '' it will pass quietly
        if (message.isEmpty()) {
            return;
        }
        //replace tags
        message = message.replace("IMPACT_X", Integer.toString(impact.getBlockX()));
        message = message.replace("IMPACT_Y", Integer.toString(impact.getBlockY()));
        message = message.replace("IMPACT_Z", Integer.toString(impact.getBlockZ()));
        message = message.replace("IMPACT_DISTANCE", Long.toString(Math.round(impact.distance(playerLoc))));
        message = message.replace("IMPACT_YDIFF", Integer.toString(impact.getBlockY() - playerLoc.getBlockY()));

        if (message != null)
            sendMessage(message, player);
    }
	
	/**
	 * returns the message from the Map
	 * @param messageEnum message to display
	 * @param player which player gets this message
	 * @param craft which craft parameter will be displayed
	 * @return the requested message from the localization file
	 */
    private String getMessage(MessageEnum messageEnum, Player player, Craft craft)
	{
		//no message
		if (messageEnum == null) return null;
	
		String message = messageMap.get(messageEnum.getString());
		
		//return if message was not found
		if (message == null)
		{
			plugin.logSevere("Message " + messageEnum.getString() + " not found.");
			return null;
		}
        //if the message is something like this Explosion: '' it will pass quietly
        if (message.isEmpty())
            return null;
		
		if (craft != null)
		{
			//craft message name
            if (craft.getCraftName()!=null)
                message = message.replace("CRAFT_NAME", craft.getCraftName());
            message = message.replace("CRAFT", craft.getCraftDesign().getMessageName());
			message = message.replace("DESCRIPTION", craft.getCraftDesign().getDescription());
			if (craft.getOwner() != null){
				OfflinePlayer offplayer = Bukkit.getOfflinePlayer(craft.getOwner());
				if (offplayer != null)
					message = message.replace("OWNER", offplayer.getName());
			}
			//economy
			if (plugin.getEconomy() != null) {
				message = message.replace("BUILD_COSTS", plugin.getEconomy().format(craft.getCraftDesign().getEconomyBuildingCost()));
				message = message.replace("DISMANTLING_REFUND", plugin.getEconomy().format(craft.getCraftDesign().getEconomyDismantlingRefund()));
				message = message.replace("DESTRUCTION_REFUND", plugin.getEconomy().format(craft.getCraftDesign().getEconomyDestructionRefund()));
			}
        }

        if (player != null)
		{
			//replace the number of ironclad
            message = message.replace("PLAYER", player.getName());
			message = message.replace("LIMIT", Integer.toString(plugin.getCraftManager().getNumberOfCrafts(player.getUniqueId())));
		}
		return message;
	}

	/**
	 * returns a message from the map
	 * @param messageEnum message to display
	 * @return the requested message from the localization file
	 */
	public String getMessage(MessageEnum messageEnum)
	{
		return getMessage(messageEnum, null, null);
	}
	
	/**
	 * sends a message to the player which can span several lines. Line break with '\n'.
	 * @param string message to send
	 * @param player which player gets this message
	 */
	private void sendMessage(String string, Player player)
	{
		if (string == null) return;
		if (player == null) return;
        if (string.equals(" "))  return;
		
		String[] message = string.split("\n "); // Split everytime the "\n" into a new array value

		for (String aMessage : message) {
			plugin.logDebug("Message for " + player.getName() + ": " + aMessage);
			player.sendMessage(aMessage); // Send each argument in the message
		}
	}
}
