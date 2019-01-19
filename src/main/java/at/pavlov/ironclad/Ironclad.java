package at.pavlov.ironclad;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.UUID;
import java.util.logging.Logger;

import at.pavlov.ironclad.API.IroncladAPI;
import at.pavlov.ironclad.Enum.MessageEnum;
import at.pavlov.ironclad.craft.CraftManager;
import at.pavlov.ironclad.craft.Craft;
import at.pavlov.ironclad.craft.DesignStorage;
import at.pavlov.ironclad.config.*;
import at.pavlov.ironclad.listener.*;
import at.pavlov.ironclad.craft.CraftMovementManager;
import at.pavlov.ironclad.scheduler.FakeBlockHandler;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import at.pavlov.ironclad.craft.CraftDesign;
import at.pavlov.ironclad.dao.PersistenceDatabase;

public final class Ironclad extends JavaPlugin
{
	private PluginManager pm;
	private final Logger logger = Logger.getLogger("Minecraft");

    private final Config config;
    private final FakeBlockHandler fakeBlockHandler;
    private final CraftMovementManager craftMovementManager;

    private final IroncladAPI ironcladAPI;
    private Economy economy;
	
	//Listener
    private final BlockListener blockListener;
	private final PlayerListener playerListener;
	private final EntityListener entityListener;
	private final SignListener signListener;
    private final Commands commands;
	
	// database
	private final PersistenceDatabase persistenceDatabase;
	private Connection connection = null;

	private final String craftDatabase = "craftlist_2_4_6";
	private final String whitelistDatabase = "whitelist_2_4_6";


	public Ironclad()
	{
		super();

        //setup all classes
        this.config = new Config(this);
        this.craftMovementManager = new CraftMovementManager(this);
        this.fakeBlockHandler = new FakeBlockHandler(this);
        this.ironcladAPI = new IroncladAPI(this);

        this.persistenceDatabase = new PersistenceDatabase(this);

        this.blockListener = new BlockListener(this);
        this.playerListener = new PlayerListener(this);
        this.entityListener = new EntityListener(this);
        this.signListener = new SignListener(this);
        this.commands = new Commands(this);

    }

    public static Ironclad getPlugin() {
        return (Ironclad) Bukkit.getPluginManager().getPlugin("Ironclad");
    }

	public void onDisable()
	{
		logger.info(getLogPrefix() + "Ironclad plugin shutting down 1.");
		getServer().getScheduler().cancelTasks(this);

		logger.info(getLogPrefix() + "Ironclad plugin shutting down 2.");
		// save database on shutdown
		logger.info(getLogPrefix() + "Wait until scheduler is finished");
		while(getPlugin().getPersistenceDatabase().isSaveTaskRunning()){
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info(getLogPrefix() + "Scheduler finished");
		persistenceDatabase.saveAllCrafts(false);
		logger.info(getLogPrefix() + "Ironclad plugin shutting down 3.");
		if (connection != null) {
			try {
				logger.info(getLogPrefix() + "Ironclad plugin shutting down 4.");
				connection.close();
			} catch (SQLException e) {
				logger.info(getLogPrefix() + "Ironclad plugin shutting down 5.");
				e.printStackTrace();
			}
		}
		logger.info(getLogPrefix() + "Ironclad plugin v" + getPluginDescription().getVersion() + " has been disabled");
	}

	public void onEnable()
	{
        long startTime = System.nanoTime();

		//load some global variables
		pm = getServer().getPluginManager();
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		
		//inform the user if worldedit it missing
		if (!checkWorldEdit())
		{
			//no worldEdit has been loaded. Disable plugin
			console.sendMessage(ChatColor.RED + "[Ironclad] Please install WorldEdit, else Ironclad can't load.");
			console.sendMessage(ChatColor.RED + "[Ironclad] Plugin is now disabled.");
			
			pm.disablePlugin(this);
			return;
		}
        setupEconomy();
		

		try
		{
			pm.registerEvents(blockListener, this);
			pm.registerEvents(playerListener, this);
			pm.registerEvents(entityListener, this);
			pm.registerEvents(signListener, this);
			//call command executer
			getCommand("ironclad").setExecutor(commands);

			// load config
			config.loadConfig();

			// Initialize the database
			getServer().getScheduler().runTaskAsynchronously(this, new Runnable()
			{
				public void run()
				{
					try {
						openConnection();
						Statement statement = connection.createStatement();
						statement.close();
						getPlugin().logInfo("Connected to database");
					} catch (ClassNotFoundException | SQLException e) {
						e.printStackTrace();
					}
					//create the tables for the database in case they don't exist
					persistenceDatabase.createTables();
					// load ironclad from database
					persistenceDatabase.loadCrafts();
				}
			});


			// setting up Aiming Mode Task
			craftMovementManager.initMovementExecute();
            fakeBlockHandler.setupScheduler();

			// save ironclad
			getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
			{
				public void run()
				{
					persistenceDatabase.saveAllCrafts(true);
				}
			}, 6000L, 6000L);

			Metrics metrics = new Metrics(this);

            logDebug("Time to enable ironclad: " + new DecimalFormat("0.00").format((System.nanoTime() - startTime)/1000000.0) + "ms");

            // Plugin succesfully enabled
            logger.info(getLogPrefix() + "Ironclad plugin v" + getPluginDescription().getVersion() + " has been enabled");
		}
		catch (Exception ex)
		{
			// Plugin failed to enable
			logSevere(String.format("[%s v%s] could not be enabled!", getDescription().getName(), getDescription().getVersion()));

			// Print the stack trace of the actual cause
			Throwable t = ex;
			while (t != null)
			{
				if (t.getCause() == null)
				{
					logSevere(String.format("[%s v%s] exception:", getDescription().getName(), getDescription().getVersion()));
					t.printStackTrace();
				}

				t = t.getCause();
			}
		}
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }




	// set up ebean database
	private void openConnection() throws SQLException, ClassNotFoundException
	{
		String driver = getConfig().getString("database.driver", "org.sqlite.JDBC");
		String url = getConfig().getString("database.url", "jdbc:sqlite:{DIR}{NAME}.db");
		String username = getConfig().getString("database.username", "bukkit");
		String password = getConfig().getString("database.password", "walrus");
		//String serializable = getConfig().getString("database.isolation", "SERIALIZABLE");

		url = url.replace("{DIR}{NAME}.db", "plugins/Ironclad/Ironclad.db");

		if (connection != null && !connection.isClosed()) {
			return;
		}

		synchronized (this) {
			if (connection != null && !connection.isClosed()) {
				return;
			}
			Class.forName(driver);
			connection = DriverManager.getConnection(url, username, password);
		}
    }

	public boolean hasConnection() {
		return this.connection != null;
	}

	public boolean isPluginEnabled()
	{
		return this.isEnabled();
	}

	public final Config getMyConfig()
	{
		return config;
	}

	public void disablePlugin()
	{
		pm.disablePlugin(this);
	}

	private String getLogPrefix()
	{
		return "[" + getPluginDescription().getName() + "] ";
	}

	public void logSevere(String msg)
	{
		//msg = ChatColor.translateAlternateColorCodes('&', msg);
		this.logger.severe(getLogPrefix() + ChatColor.stripColor(msg));
	}
	
	public void logInfo(String msg)
	{
		//msg = ChatColor.translateAlternateColorCodes('&', msg);
		this.logger.info(getLogPrefix() + ChatColor.stripColor(msg));
	}

	public void logDebug(String msg)
	{
		if (config.isDebugMode())
			this.logger.info(getLogPrefix() + ChatColor.stripColor(msg));
	}

	public void broadcast(String msg)
	{
		this.getServer().broadcastMessage(msg);
	}

	public PluginDescriptionFile getPluginDescription()
	{
		return this.getDescription();
	}
	
	/**
	 * checks if WorldEdit is running
	 * @return true is WorldEdit is running
	 */
	private boolean checkWorldEdit()
	{
		Plugin plug = pm.getPlugin("WorldEdit");
        return plug != null;
    }

    public Connection getConnection(){
		return this.connection;
	}

	public PersistenceDatabase getPersistenceDatabase()
	{
		return persistenceDatabase;
	}

	public CraftManager getCraftManager()
	{
		return this.config.getCraftManager();
	}

	public PlayerListener getPlayerListener()
	{
		return playerListener;
	}

	public SignListener getSignListener()
	{
		return signListener;
	}

	public DesignStorage getDesignStorage()
	{
		return this.config.getDesignStorage();
	}
	
	public CraftDesign getCraftDesign(Craft craft)
	{
		return getDesignStorage().getDesign(craft);
	}
	
	public CraftDesign getCraftDesign(String designId)
	{
		return getDesignStorage().getDesign(designId);
	}

    public Craft getCraft(UUID id)
    {
        return CraftManager.getCraft(id);
    }

	public EntityListener getEntityListener()
	{
		return entityListener;
	}
	
	public void sendMessage(Player player, Craft craft, MessageEnum message)
	{
		this.config.getUserMessages().sendMessage(message, player, craft);
	}

	public void createCraft(Craft craft, boolean saveToDatabase)
	{
		this.getCraftManager().createCraft(craft, saveToDatabase);
	}

	public void spawnCraft(CraftDesign craftDesign, Location location, BlockFace direction, UUID playerUID){
		Craft craft = new Craft(craftDesign, location.getWorld().getUID(), location.toVector(), direction, playerUID);
		craft.create();
		//this.getCraftManager().createCraft(craft, true);
	}

    public IroncladAPI getIroncladAPI() {
        return ironcladAPI;
    }

    public BlockListener getBlockListener() {
        return blockListener;
    }

    public FakeBlockHandler getFakeBlockHandler() {
        return fakeBlockHandler;
    }

    public Commands getCommandListener() {
        return commands;
    }

    public Economy getEconomy(){
        return this.economy;
    }

	public String getCraftDatabase() {
		return craftDatabase;
	}

	public String getWhitelistDatabase() {
		return whitelistDatabase;
	}

	public CraftMovementManager getCraftMovementManager() {
		return craftMovementManager;
	}
}
