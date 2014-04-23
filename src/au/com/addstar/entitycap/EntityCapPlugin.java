package au.com.addstar.entitycap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import au.com.addstar.entitycap.group.EntityConcentrationMap;

public class EntityCapPlugin extends JavaPlugin
{
	private LinkedList<GroupSettings> mAutoGroups = new LinkedList<GroupSettings>();
	private LinkedList<GroupSettings> mAllGroups = new LinkedList<GroupSettings>();
	
	private HashSet<String> mWorlds;
	private boolean mWorldsBlacklist;
	
	private boolean mNoisy = true;
	private int mInterval;
	
	@Override
	public void onEnable()
	{
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
		
		getCommand("entitycap").setExecutor(new CapCommand(this));
		
		Bukkit.getScheduler().runTaskTimer(this, new EntityKillerTask(this), mInterval, mInterval);
	}
	
	private void loadConfig() throws InvalidConfigurationException
	{
		mAllGroups.clear();
		mAutoGroups.clear();
		
		FileConfiguration config = getConfig();
		
		mNoisy = config.getBoolean("output_to_console", false);
		mInterval = config.getInt("ticks_between_run", 1200);
		if(mInterval <= 0)
			throw new InvalidConfigurationException("ticks_between_run must be greater than 0");
		
		for(String key : config.getKeys(false))
		{
			if(!config.isConfigurationSection(key))
				continue;
			
			GroupSettings group = new GroupSettings();
			group.load(config.getConfigurationSection(key));
			
			mAllGroups.add(group);
			if(group.shouldAutorun())
				mAutoGroups.add(group);
			
			getLogger().info(String.format("Loaded group %s: AutoRun: %s", group.getName(), group.shouldAutorun()));
		}
		
		if(config.isList("worlds"))
		{
			List<String> worlds = config.getStringList("worlds");
			mWorlds = new HashSet<String>(worlds.size());
			for(String world : worlds)
				mWorlds.add(world.toLowerCase());
		}
		else
			mWorlds = new HashSet<String>();
		
		mWorldsBlacklist = config.getBoolean("worlds_is_blacklist", true);
	}
	
	public Collection<GroupSettings> getGroups(boolean includeManual)
	{
		if(includeManual)
			return Collections.unmodifiableList(mAllGroups);
		else
			return Collections.unmodifiableList(mAutoGroups);
	}
	
	public boolean allowWorldGlobal(World world)
	{
		if(mWorldsBlacklist)
			return !mWorlds.contains(world.getName().toLowerCase());
		else
			return mWorlds.contains(world.getName().toLowerCase());
	}
	
	public void runGroup(GroupSettings settings, boolean printResult, boolean printEmpty)
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
	
//	{
//		Collection<GroupSettings> settings = getGroups(includeManual);
//		
//		int maxRadius = 0;
//		for(GroupSettings setting : settings)
//			maxRadius = Math.max(maxRadius, setting.getRadius());
//		
//		Location temp = new Location(null, 0, 0, 0);
//		Location playerLoc = new Location(null, 0, 0, 0);
//		
//		int[] removeCounts = new int[settings.size()];
//		boolean anyRemoved = false;
//		
//		for(Player player : Bukkit.getOnlinePlayers())
//		{
//			if(player.hasPermission("entitycap.bypass"))
//				continue;
//			
//			List<Entity> nearby = player.getNearbyEntities(maxRadius, maxRadius, maxRadius);
//			player.getLocation(playerLoc);
//			
//			int index = 0;
//			for(GroupSettings group : settings)
//			{
//				if(group.warnOnly())
//					continue;
//				
//				int radius = group.getRadius() * group.getRadius();
//				int count = 0;
//				
//				for(Entity ent : nearby)
//				{
//					ent.getLocation(temp);
//					if(ent.isValid() && group.matches(ent) && temp.distanceSquared(playerLoc) <= radius)
//					{
//						++count;
//						if(count > group.getMaxEntities())
//							ent.remove();
//					}
//				}
//				
//				if(count > group.getMaxEntities())
//				{
//					removeCounts[index++] += count - group.getMaxEntities();
//					anyRemoved = true;
//				}
//				else
//					++index;
//			}
//		}
//		
//		if(mNoisy && anyRemoved)
//		{
//			int index = 0;
//			for(GroupSettings group : settings)
//			{
//				if(removeCounts[index] != 0)
//					getLogger().info(String.format("Removed %d entities from %s group", removeCounts[index], group.getName()));
//				++index;
//			}
//		}
//		else if(mNoisy && includeManual)
//		{
//			getLogger().info("No entities were removed");
//		}
//	}
}
