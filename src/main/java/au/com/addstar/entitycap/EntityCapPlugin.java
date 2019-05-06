package au.com.addstar.entitycap;

import java.io.File;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import au.com.addstar.entitycap.group.EntityConcentrationMap;

public class EntityCapPlugin extends JavaPlugin implements Listener
{
	private final LinkedList<GroupSettings> mAutoGroups = new LinkedList<>();
	private final LinkedList<GroupSettings> mAllGroups = new LinkedList<>();
	
	private final LinkedList<GroupSettings> mChunkGroups = new LinkedList<>();
	
	private HashSet<String> mWorlds;
	private boolean mWorldsBlacklist;
	private boolean mResetTicksLived;
	
	private boolean mNoisy = true;

    public boolean isDebug() {
        return mDebug;
    }

    private boolean mDebug = false;
	private int mInterval;

	public static EntityCapPlugin instance;
	
	@Override
	public void onEnable()
	{
		instance = this;
		saveDefaultConfig();
		
		try
		{
			loadConfig();
		}
		catch(InvalidConfigurationException e)
		{
			getLogger().severe("Error loading config: ");
			getLogger().severe(e.getMessage());
			setEnabled(false);
			return;
		}
		
		Objects.requireNonNull(getCommand("entitycap")).setExecutor(new CapCommand(this));
		
		SpecialLog.initialize(new File(getDataFolder(), "removals.log"));
		
		Bukkit.getScheduler().runTaskTimer(this, new EntityKillerTask(this), mInterval, mInterval);
		Bukkit.getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable()
	{
		SpecialLog.shutdown();
	}
	
	private void loadConfig() throws InvalidConfigurationException
	{
		mAllGroups.clear();
		mAutoGroups.clear();
		
		FileConfiguration config = getConfig();
		mDebug = config.getBoolean("debug", false);
		mNoisy = config.getBoolean("output_to_console", false);
		mInterval = config.getInt("ticks_between_run", 1200);
		if(mInterval <= 0)
			throw new InvalidConfigurationException("ticks_between_run must be greater than 0");
		
		for(String key : config.getKeys(false))
		{
			if(!config.isConfigurationSection(key))
				continue;
			
			GroupSettings group = new GroupSettings();
			group.load(Objects.requireNonNull(config.getConfigurationSection(key)));
			
			if (group.isChunkOnly())
				mChunkGroups.add(group);
			else
			{
				mAllGroups.add(group);
				if(group.shouldAutorun())
					mAutoGroups.add(group);
			}
			
			getLogger().info(String.format("Loaded group %s: AutoRun: %s", group.getName(), group.shouldAutorun()));
		}
		mWorlds = new HashSet<>();
		mWorldsBlacklist = defineWorldForSecion(config,mWorlds);
		mResetTicksLived = config.getBoolean("vehicle_reset_ticks_lived", false);
	}

	public static boolean defineWorldForSecion(ConfigurationSection section, HashSet<String> worldSet) {
		if(section.isList("worlds"))
		{
			List<String> worlds = section.getStringList("worlds");
			for(String world : worlds)
				worldSet.add(world.toLowerCase());
		}
		return section.getBoolean("worlds_is_blacklist", true);
}

	public Collection<GroupSettings> getGroups(boolean includeManual)
	{
		if(includeManual)
			return Collections.unmodifiableList(mAllGroups);
		else
			return Collections.unmodifiableList(mAutoGroups);
	}
	
	private boolean allowWorldGlobal(World world)
	{
		if(mWorldsBlacklist)
			return !mWorlds.contains(world.getName().toLowerCase());
		else
			return mWorlds.contains(world.getName().toLowerCase());
	}
	
	private void runGroup(GroupSettings settings, boolean printResult, boolean printEmpty)
	{
		if(settings.warnOnly())
			return;
		
		EntityConcentrationMap map = new EntityConcentrationMap(settings, this);
		
		for(World world : Bukkit.getWorlds())
		{
			if(allowWorldGlobal(world) && settings.allowWorld(world))
				map.queueWorld(world);
		}
		
		map.build(new EntityRemover(settings, getLogger(), printResult, printEmpty));
	}
	
	public void runGroups(boolean includeManual)
	{
		for(GroupSettings settings : getGroups(includeManual))
			runGroup(settings, mNoisy, includeManual);
	}
	
	public void checkGroup(GroupSettings settings, CheckReport report)
	{
		EntityConcentrationMap map = new EntityConcentrationMap(settings, this);
		report.waitForGroup(settings);
		
		for(World world : Bukkit.getWorlds())
		{
			if(allowWorldGlobal(world) && settings.allowWorld(world))
				map.queueWorld(world);
		}
		
		map.build(new EntityChecker(settings, report));
	}
	
	public void checkGroups(boolean includeManual, CommandSender sender)
	{
		CheckReport report = new CheckReport(sender);
		for(GroupSettings settings : getGroups(includeManual))
			checkGroup(settings, report);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onVehicleLeave(VehicleExitEvent event)
	{
		if(mResetTicksLived)
			event.getVehicle().setTicksLived(0);
	}
	
	@EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
	private void onEntitySpawn(EntitySpawnEvent event)
	{
		// Check per-chunk limits
		for (GroupSettings settings : mChunkGroups)
		{
			if (!settings.matches(event.getEntity()))
				continue;
			
			// Prevent spawning if limit is exceeded
			Chunk chunk = event.getLocation().getChunk();
			int count = 0;
			for (Entity ent : chunk.getEntities())
			{
				if (settings.matches(ent))
					++count;
			}
			
			if (count + 1 > settings.getMaxEntities())
				event.setCancelled(true);
		}
	}
}
