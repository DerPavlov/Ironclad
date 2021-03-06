package at.pavlov.ironclad.config;


import at.pavlov.ironclad.Ironclad;
import at.pavlov.ironclad.craft.CraftManager;
import at.pavlov.ironclad.craft.DesignStorage;
import at.pavlov.ironclad.container.ItemHolder;
import at.pavlov.ironclad.utils.IroncladUtil;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author DerPavlov
 *
 */



public class Config
{
	//general
	private boolean debugMode;
    private boolean relayExplosionEvent;
    private int maxBlockUpdateSize;
    private int claimEdgeLength;

	//build limits
	private boolean buildLimitEnabled;
	private int buildLimitA;
	private int buildLimitB;
    //keepProjectileAlive
    private boolean keepAliveEnabled;
    private double keepAliveTeleportDistance;
	//tools
	private ItemHolder toolAdjust = new ItemHolder("minecraft:air");
	private ItemHolder toolCruising = new ItemHolder("minecraft:clock");
	private ItemHolder toolFiring = new ItemHolder("minecraft:flint_and_steel");
    private ItemHolder toolRamrod = new ItemHolder("minecraft:stick");
	private ItemHolder toolRotating = new ItemHolder("minecraft:rail");
    private ItemHolder toolThermometer = new ItemHolder("minecraft:gold_nugget");

    private int imitatedBlockMinimumDistance;
    private int imitatedBlockMaximumDistance;
    private int imitatedSoundMaximumDistance;
    private float imitatedSoundMaximumVolume;

    private boolean imitatedExplosionEnabled;
    private int imitatedExplosionSphereSize;
    private BlockState imitatedExplosionMaterial;
    private double imitatedExplosionTime;

    private boolean imitatedCruisingEnabled;
    private int imitatedCruisingLineLength;
    private BlockState imitatedCruisingMaterial;
    private double imitatedCruisingTime;

    private boolean imitatedFiringEffectEnabled;
    private BlockState imitatedFireMaterial;
    private BlockState imitatedSmokeMaterial;
    private double imitatedFiringTime;

    private boolean imitatedPredictorEnabled;
    private int imitatedPredictorIterations;
    private double imitatedPredictorDistance;
    private BlockState imitatedPredictorMaterial;
    private double imitatedPredictorTime;


    //superbreakerBlocks
    private ArrayList<BlockState> superbreakerBlocks = new ArrayList<>();

    //unbreakableBlocks
    private ArrayList<BlockState> unbreakableBlocks = new ArrayList<BlockState>();

    //cancelEventForLoadingItem
    private List<ItemHolder> cancelItems = new ArrayList<>();


    private final UserMessages userMessage;
	private final Ironclad plugin;
	private final DesignStorage designStorage;
    private final CraftManager craftManager;

	public Config(Ironclad plugin)
	{
		this.plugin = plugin;
		userMessage = new UserMessages(this.plugin);
		designStorage = new DesignStorage(this.plugin);
        craftManager = new CraftManager(plugin, userMessage, this);
	}

	public void loadConfig()
	{
		// copy the default config to the disk if it does not exist
		plugin.saveDefaultConfig();

        plugin.logDebug("load Config");
        plugin.reloadConfig();

		//general
		setDebugMode(plugin.getConfig().getBoolean("general.debugMode", false));
        setRelayExplosionEvent(plugin.getConfig().getBoolean("general.relayExplosionEvent", false));
        setMaxBlockUpdateSize(plugin.getConfig().getInt("general.claimEdgeLength", 500));
        setClaimEdgeLength(plugin.getConfig().getInt("general.claimEdgeLength", 60));

		//limitOfCrafts
		setBuildLimitEnabled(plugin.getConfig().getBoolean("craftLimits.useLimits", true));
		setBuildLimitA(plugin.getConfig().getInt("craftLimits.buildLimitA", 10));
		setBuildLimitB(plugin.getConfig().getInt("craftLimits.buildLimitB", 2));

        //keepProjectileAlive
        setKeepAliveEnabled(plugin.getConfig().getBoolean("keepProjectileAlive.enabled", true));
        setKeepAliveTeleportDistance(plugin.getConfig().getDouble("keepProjectileAlive.teleportProjectile", 5.0));

		//tools
		setToolAdjust(new ItemHolder(plugin.getConfig().getString("tools.adjust", "minecraft:air")));
		setToolCruising(new ItemHolder(plugin.getConfig().getString("tools.autoaim", "minecraft:clock")));
		setToolFiring(new ItemHolder(plugin.getConfig().getString("tools.firing", "minecraft:flint_and_steel")));
        setToolRamrod(new ItemHolder(plugin.getConfig().getString("tools.ramrod", "minecraft:stick")));
		setToolRotating(new ItemHolder(plugin.getConfig().getString("tools.adjust", "minecraft:rail")));
        setToolThermometer(new ItemHolder(plugin.getConfig().getString("tools.thermometer", "minecraft:gold_nugget")));

        //imitated effects
        setImitatedBlockMinimumDistance(plugin.getConfig().getInt("imitatedEffects.minimumBlockDistance", 40));
        setImitatedBlockMaximumDistance(plugin.getConfig().getInt("imitatedEffects.maximumBlockDistance", 200));
        setImitatedSoundMaximumDistance(plugin.getConfig().getInt("imitatedEffects.maximumSoundDistance", 200));
        setImitatedSoundMaximumVolume((float) plugin.getConfig().getDouble("imitatedEffects.maximumSoundVolume", 0.8));

        //imitated explosions
        setImitatedExplosionEnabled(plugin.getConfig().getBoolean("imitatedEffects.explosion.enabled", false));
        setImitatedExplosionSphereSize(plugin.getConfig().getInt("imitatedEffects.explosion.sphereSize", 2));
        setImitatedExplosionMaterial(IroncladUtil.createBlockData(plugin.getConfig().getString("imitatedEffects.explosion.material", "minecraft:glowstone")));
        setImitatedExplosionTime(plugin.getConfig().getDouble("imitatedEffects.explosion.time", 1.0));

        //imitated cruising
        setImitatedCruisingEnabled(plugin.getConfig().getBoolean("imitatedEffects.cruising.enabled", false));
        setImitatedCruisingLineLength(plugin.getConfig().getInt("imitatedEffects.cruising.length", 5));
        setImitatedCruisingMaterial(IroncladUtil.createBlockData(plugin.getConfig().getString("imitatedEffects.cruising.block", "minecraft:glass")));
        setImitatedCruisingTime(plugin.getConfig().getDouble("imitatedEffects.cruising.time", 1.0));

        //imitated firing effects
        setImitatedFiringEffectEnabled(plugin.getConfig().getBoolean("imitatedEffects.firing.enabled", false));
        setImitatedFireMaterial(IroncladUtil.createBlockData(plugin.getConfig().getString("imitatedEffects.firing.fireBlock", "minecraft:glowstone")));
        setImitatedSmokeMaterial(IroncladUtil.createBlockData(plugin.getConfig().getString("imitatedEffects.firing.smokeBlock", "'minecraft:cobweb")));
        setImitatedFiringTime(plugin.getConfig().getDouble("imitatedEffects.firing.time", 1.0));

        //imitaded predictor
        setImitatedPredictorEnabled(plugin.getConfig().getBoolean("imitatedEffects.predictor.enabled", true));
        setImitatedPredictorIterations(plugin.getConfig().getInt("imitatedEffects.predictor.maxIterations", 500));
        setImitatedPredictorDistance(plugin.getConfig().getDouble("imitatedEffects.predictor.maxDistance", 400.0));
        setImitatedPredictorMaterial(IroncladUtil.createBlockData(plugin.getConfig().getString("imitatedEffects.predictor.material", "minecraft:glowstone")));
        setImitatedPredictorTime(plugin.getConfig().getDouble("imitatedEffects.predictor.time", 1.0));

        //superbreakerBlocks
        setSuperbreakerBlocks(IroncladUtil.toBlockDataList(plugin.getConfig().getStringList("superbreakerBlocks")));
        //if this list is empty add some blocks
        if (superbreakerBlocks.size() == 0)
        {
            plugin.logInfo("superbreakerBlock list is empty");
        }

        //unbreakableBlocks
        setUnbreakableBlocks(IroncladUtil.toBlockDataList(plugin.getConfig().getStringList("unbreakableBlocks")));
        if (unbreakableBlocks.size() == 0)
        {
            plugin.logInfo("unbreakableBlocks list is empty");
        }

        //cancelEventForLoadingItem
        setCancelItems(IroncladUtil.toItemHolderList(plugin.getConfig().getStringList("cancelEventForLoadingItem")));

		//load other configs
		designStorage.loadCraftDesigns();
        craftManager.updateCrafts();
		userMessage.loadLanguage();

	}


	/**
	 * returns the class UserMessages
	 * @return
	 */
	public UserMessages getUserMessages()
	{
		return userMessage;
	}

	public DesignStorage getDesignStorage()
	{
		return designStorage;
	}

	public boolean isBuildLimitEnabled()
	{
		return buildLimitEnabled;
	}

	void setBuildLimitEnabled(boolean buildLimitEnabled)
	{
		this.buildLimitEnabled = buildLimitEnabled;
	}

	public int getBuildLimitA()
	{
		return buildLimitA;
	}

	void setBuildLimitA(int buildLimitA)
	{
		this.buildLimitA = buildLimitA;
	}

	public int getBuildLimitB()
	{
		return buildLimitB;
	}

	void setBuildLimitB(int buildLimitB)
	{
		this.buildLimitB = buildLimitB;
	}

	public ItemHolder getToolAdjust()
	{
		return toolAdjust;
	}

	void setToolAdjust(ItemHolder toolAdjust)
	{
		this.toolAdjust = toolAdjust;
	}

	public ItemHolder getToolCruising()
	{
		return toolCruising;
	}

	void setToolCruising(ItemHolder toolCruising)
	{
		this.toolCruising = toolCruising;
	}

	public ItemHolder getToolRotating()
	{
		return toolRotating;
	}

	void setToolRotating(ItemHolder toolRotating)
	{
		this.toolRotating = toolRotating;
	}

	public boolean isDebugMode()
	{
		return debugMode;
	}

	void setDebugMode(boolean debugMode)
	{
		this.debugMode = debugMode;
	}

	public ItemHolder getToolFiring()
	{
		return toolFiring;
	}

	void setToolFiring(ItemHolder toolFiring)
	{
		this.toolFiring = toolFiring;
	}


    public ArrayList<BlockState> getSuperbreakerBlocks() {
        return superbreakerBlocks;
    }

    void setSuperbreakerBlocks(ArrayList<BlockState> superbreakerBlocks) {
        this.superbreakerBlocks = superbreakerBlocks;
    }

    public ArrayList<BlockState> getUnbreakableBlocks() {
        return unbreakableBlocks;
    }

    void setUnbreakableBlocks(ArrayList<BlockState> unbreakableBlocks) {
        this.unbreakableBlocks = unbreakableBlocks;
    }

    public CraftManager getCraftManager() {
        return craftManager;
    }

    public ItemHolder getToolThermometer() {
        return toolThermometer;
    }

    public void setToolThermometer(ItemHolder toolThermometer) {
        this.toolThermometer = toolThermometer;
    }

    public ItemHolder getToolRamrod() {
        return toolRamrod;
    }

    public void setToolRamrod(ItemHolder toolRamrod) {
        this.toolRamrod = toolRamrod;
    }

    public List<ItemHolder> getCancelItems() {
        return cancelItems;
    }

    public void setCancelItems(List<ItemHolder> cancelItems) {
        this.cancelItems = cancelItems;
    }

    public boolean isCancelItem(ItemStack item)
    {
        for (ItemHolder item2 : getCancelItems())
        {
            if (item2.equalsFuzzy(item))
                return true;
        }
        return false;
    }

    public BlockStateHolder getImitatedExplosionMaterial() {
        return imitatedExplosionMaterial;
    }

    public void setImitatedExplosionMaterial(BlockState imitatedExplosionMaterial) {
        this.imitatedExplosionMaterial = imitatedExplosionMaterial;
    }

    public double getImitatedExplosionTime() {
        return imitatedExplosionTime;
    }

    public void setImitatedExplosionTime(double imitatedExplosionTime) {
        this.imitatedExplosionTime = imitatedExplosionTime;
    }

    public BlockStateHolder getImitatedCruisingMaterial() {
        return imitatedCruisingMaterial;
    }

    public void setImitatedCruisingMaterial(BlockState imitatedCruisingMaterial) {
        this.imitatedCruisingMaterial = imitatedCruisingMaterial;
    }

    public BlockStateHolder getImitatedFireMaterial() {
        return imitatedFireMaterial;
    }

    public void setImitatedFireMaterial(BlockState imitatedFireMaterial) {
        this.imitatedFireMaterial = imitatedFireMaterial;
    }

    public BlockStateHolder getImitatedSmokeMaterial() {
        return imitatedSmokeMaterial;
    }

    public void setImitatedSmokeMaterial(BlockState imitatedSmokeMaterial) {
        this.imitatedSmokeMaterial = imitatedSmokeMaterial;
    }

    public boolean isImitatedCruisingEnabled() {
        return imitatedCruisingEnabled;
    }

    public void setImitatedCruisingEnabled(boolean imitatedCruisingEnabled) {
        this.imitatedCruisingEnabled = imitatedCruisingEnabled;
    }

    public boolean isImitatedFiringEffectEnabled() {
        return imitatedFiringEffectEnabled;
    }

    public void setImitatedFiringEffectEnabled(boolean imitatedFiringEffectEnabled) {
        this.imitatedFiringEffectEnabled = imitatedFiringEffectEnabled;
    }

    public int getImitatedCruisingLineLength() {
        return imitatedCruisingLineLength;
    }

    public void setImitatedCruisingLineLength(int imitatedCruisingLineLength) {
        this.imitatedCruisingLineLength = imitatedCruisingLineLength;
    }

    public double getImitatedBlockMinimumDistance() {
        return imitatedBlockMinimumDistance;
    }

    public void setImitatedBlockMinimumDistance(int imitatedBlockMinimumDistance) {
        this.imitatedBlockMinimumDistance = imitatedBlockMinimumDistance;
    }

    public double getImitatedBlockMaximumDistance() {
        return imitatedBlockMaximumDistance;
    }

    public void setImitatedBlockMaximumDistance(int imitatedBlockMaximumDistance) {
        this.imitatedBlockMaximumDistance = imitatedBlockMaximumDistance;
    }

    public int getImitatedSoundMaximumDistance() {
        return imitatedSoundMaximumDistance;
    }

    public void setImitatedSoundMaximumDistance(int imitatedSoundMaximumDistance) {
        this.imitatedSoundMaximumDistance = imitatedSoundMaximumDistance;
    }

    public int getImitatedExplosionSphereSize() {
        return imitatedExplosionSphereSize;
    }

    public void setImitatedExplosionSphereSize(int imitatedExplosionSphereSize) {
        this.imitatedExplosionSphereSize = imitatedExplosionSphereSize;
    }

    public boolean isImitatedExplosionEnabled() {
        return imitatedExplosionEnabled;
    }

    public void setImitatedExplosionEnabled(boolean imitatedExplosionEnabled) {
        this.imitatedExplosionEnabled = imitatedExplosionEnabled;
    }

    public double getImitatedCruisingTime() {
        return imitatedCruisingTime;
    }

    public void setImitatedCruisingTime(double imitatedCruisingTime) {
        this.imitatedCruisingTime = imitatedCruisingTime;
    }

    public double getImitatedFiringTime() {
        return imitatedFiringTime;
    }

    public void setImitatedFiringTime(double imitatedFiringTime) {
        this.imitatedFiringTime = imitatedFiringTime;
    }

    public boolean isKeepAliveEnabled() {
        return keepAliveEnabled;
    }

    public void setKeepAliveEnabled(boolean keepAliveEnabled) {
        this.keepAliveEnabled = keepAliveEnabled;
    }

    public double getKeepAliveTeleportDistance() {
        return keepAliveTeleportDistance;
    }

    public void setKeepAliveTeleportDistance(double keepAliveTeleportDistance) {
        this.keepAliveTeleportDistance = keepAliveTeleportDistance;
    }

    public boolean isImitatedPredictorEnabled() {
        return imitatedPredictorEnabled;
    }

    public void setImitatedPredictorEnabled(boolean imitatedPredictorEnabled) {
        this.imitatedPredictorEnabled = imitatedPredictorEnabled;
    }

    public int getImitatedPredictorIterations() {
        return imitatedPredictorIterations;
    }

    public void setImitatedPredictorIterations(int imitatedPredictorIterations) {
        this.imitatedPredictorIterations = imitatedPredictorIterations;
    }

    public double getImitatedPredictorDistance() {
        return imitatedPredictorDistance;
    }

    public void setImitatedPredictorDistance(double imitatedPredictorDistance) {
        this.imitatedPredictorDistance = imitatedPredictorDistance;
    }

    public BlockStateHolder getImitatedPredictorMaterial() {
        return imitatedPredictorMaterial;
    }

    public void setImitatedPredictorMaterial(BlockState imitatedPredictorMaterial) {
        this.imitatedPredictorMaterial = imitatedPredictorMaterial;
    }

    public double getImitatedPredictorTime() {
        return imitatedPredictorTime;
    }

    public void setImitatedPredictorTime(double imitatedPredictorTime) {
        this.imitatedPredictorTime = imitatedPredictorTime;
    }

    public boolean isRelayExplosionEvent() {
        return relayExplosionEvent;
    }

    public void setRelayExplosionEvent(boolean relayExplosionEvent) {
        this.relayExplosionEvent = relayExplosionEvent;
    }

    public float getImitatedSoundMaximumVolume() {
        return imitatedSoundMaximumVolume;
    }

    public void setImitatedSoundMaximumVolume(float imitatedSoundMaximumVolume) {
        this.imitatedSoundMaximumVolume = imitatedSoundMaximumVolume;
    }

    public int getClaimEdgeLength() {
        return claimEdgeLength;
    }

    public void setClaimEdgeLength(int claimEdgeLength) {
        this.claimEdgeLength = claimEdgeLength;
    }

    public int getMaxBlockUpdateSize() {
        return maxBlockUpdateSize;
    }

    public void setMaxBlockUpdateSize(int maxBlockUpdateSize) {
        this.maxBlockUpdateSize = maxBlockUpdateSize;
    }
}
