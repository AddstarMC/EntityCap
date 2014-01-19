package au.com.addstar.entitycap;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EntityCapPlugin extends JavaPlugin
{
	private LinkedList<GroupSettings> mAutoGroups = new LinkedList<GroupSettings>();
	private LinkedList<GroupSettings> mAllGroups = new LinkedList<GroupSettings>();
	
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
	}
	
	public Collection<GroupSettings> getGroups(boolean includeManual)
	{
		if(includeManual)
			return Collections.unmodifiableList(mAllGroups);
		else
			return Collections.unmodifiableList(mAutoGroups);
	}
	
	public void runGroups(boolean includeManual)
	{
		Collection<GroupSettings> settings = getGroups(includeManual);
		
		int maxRadius = 0;
		for(GroupSettings setting : settings)
			maxRadius = Math.max(maxRadius, setting.getRadius());
		
		Location temp = new Location(null, 0, 0, 0);
		Location playerLoc = new Location(null, 0, 0, 0);
		
		int[] removeCounts = new int[settings.size()];
		boolean anyRemoved = false;
		
		for(Player player : Bukkit.getOnlinePlayers())
		{
			if(player.hasPermission("entitycap.bypass"))
				continue;
			
			List<Entity> nearby = player.getNearbyEntities(maxRadius, maxRadius, maxRadius);
			player.getLocation(playerLoc);
			
			int index = 0;
			for(GroupSettings group : settings)
			{
				if(group.warnOnly())
					continue;
				
				int radius = group.getRadius() * group.getRadius();
				int count = 0;
				
				for(Entity ent : nearby)
				{
					ent.getLocation(temp);
					if(ent.isValid() && group.matches(ent) && temp.distanceSquared(playerLoc) <= radius)
					{
						++count;
						if(count > group.getMaxEntities())
							ent.remove();
					}
				}
				
				if(count > group.getMaxEntities())
				{
					removeCounts[index++] += count - group.getMaxEntities();
					anyRemoved = true;
				}
				else
					++index;
			}
		}
		
		if(mNoisy && anyRemoved)
		{
			int index = 0;
			for(GroupSettings group : settings)
			{
				if(removeCounts[index] != 0)
					getLogger().info(String.format("Removed %d entities from %s group", removeCounts[index], group.getName()));
				++index;
			}
		}
		else if(mNoisy && includeManual)
		{
			getLogger().info("No entities were removed");
		}
	}
}
