package at.pavlov.ironclad.listener;

import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.craft.Craft;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import at.pavlov.ironclad.craft.CraftManager;
import at.pavlov.ironclad.config.Config;
import at.pavlov.ironclad.config.UserMessages;

public class SignListener implements Listener
{
	@SuppressWarnings("unused")
	private Config config;
	@SuppressWarnings("unused")
	private UserMessages userMessages;
	private final Ironclad plugin;
	private final CraftManager cannonManager;

	
	public SignListener(Ironclad plugin)
	{
		this.plugin = plugin;
		this.config = this.plugin.getMyConfig();
		this.userMessages = this.plugin.getMyConfig().getUserMessages();
		this.cannonManager = this.plugin.getCraftManager();
	}
	
	/**
	 * Sign place event
	 * @param event
	 */
	@EventHandler
	public void signChange(SignChangeEvent event)
	{
		if (event.getBlock().getType() == Material.WALL_SIGN)
		{
			Block block = event.getBlock();
			Sign s = (Sign) event.getBlock().getState();
			
			//get block which is the sign attached to
			BlockFace signFace = ((org.bukkit.material.Sign) s.getData()).getFacing();
			Block cannonBlock = block.getRelative(signFace.getOppositeFace());
			

			//get craft from location and creates a craft if not existing
	        Craft craft = cannonManager.getCraft(cannonBlock.getLocation(), event.getPlayer().getUniqueId());
			
	        //get craft from the sign
			Craft craftFromSign = cannonManager.getCraft(event.getLine(0));
			
			//if the sign is placed against a craft - no problem
			//if the sign has the name of other craft - change it
			if(craft == null && craftFromSign != null)
			{
				//this sign is in conflict with ironclad
				event.getPlayer().sendMessage(ChatColor.RED + "This sign is in conflict with ironclad");
				event.setLine(0, "[Ironclad]");
				event.setLine(1, "Player");
			}

            //if there is a craft and the sign is mounted on the sign interface
			if (craft != null && craft.isCannonSign(block.getLocation()))
			{
				event.setLine(0, craft.getSignString(0));
				event.setLine(1, craft.getSignString(1));
				event.setLine(2, craft.getSignString(2));
				event.setLine(3, craft.getSignString(3));
			}
		}
	}
}

