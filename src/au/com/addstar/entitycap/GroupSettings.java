package au.com.addstar.entitycap;

import java.util.HashSet;
import java.util.List;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import au.com.addstar.entitycap.group.EntityGroup;

public class GroupSettings
{
	private EntityFilter mFilter;
	private int mMaxAmount;
	private double mMaxDensity;
	private int mMinTicksLived; 
	
	private boolean mAutoRun;
	private String mName;
	private boolean mDisableKill;
	private int mWarnThreshold;
	
	private HashSet<String> mWorlds;
	private boolean mWorldsBlacklist;
	
	// Chunk only settings
	private boolean mIsChunkOnly;
	
	public GroupSettings()
	{
	}
	
	public String getName()
	{
		return mName;
	}
	
	public boolean matches(Entity e)
	{
		if (!mFilter.matches(e))
			return false;
		
		if(e.getTicksLived() < mMinTicksLived)
			return false;
		
		return true;
	}
	
	public boolean matches(EntityGroup group)
	{
		return group.getEntities().size() > mMaxAmount && group.getDensity() > mMaxDensity;
	}
	
	public boolean matchesWarn(EntityGroup group)
	{
		int count = (mWarnThreshold == 0 ? mMaxAmount : mWarnThreshold);
		return group.getEntities().size() > count && group.getDensity() > mMaxDensity;
	}
	
	public boolean allowWorld(World world)
	{
		if(mWorldsBlacklist)
			return !mWorlds.contains(world.getName().toLowerCase());
		else
			return mWorlds.contains(world.getName().toLowerCase());
	}
	
	public double getMaxDensity()
	{
		return mMaxDensity;
	}
	
	public int getMaxEntities()
	{
		return mMaxAmount;
	}
	
	public int getMinTicksLived()
	{
		return mMinTicksLived;
	}
	
	public boolean shouldAutorun()
	{
		return mAutoRun;
	}
	
	public boolean warnOnly()
	{
		return mDisableKill;
	}
	
	public int getWarnThreshold()
	{
		return mWarnThreshold;
	}
	
	public boolean isChunkOnly()
	{
		return mIsChunkOnly;
	}
	
	public void load(ConfigurationSection section) throws InvalidConfigurationException
	{
		mName = section.getName();
		mMaxAmount = section.getInt("max_entities");
		mMaxDensity = section.getDouble("max_density", 0);
		mMinTicksLived = section.getInt("min_ticks_lived", 0);
		mAutoRun = section.getBoolean("autorun");
		mWarnThreshold = section.getInt("warn_threshold", 0);
		mDisableKill = section.getBoolean("check_only", false);
		
		// Chunk specific
		mIsChunkOnly = section.getBoolean("chunk_limit", false);
		
		if(section.isList("worlds"))
		{
			List<String> worlds = section.getStringList("worlds");
			mWorlds = new HashSet<String>(worlds.size());
			for(String world : worlds)
				mWorlds.add(world.toLowerCase());
		}
		else
			mWorlds = new HashSet<String>();
		
		mWorldsBlacklist = section.getBoolean("worlds_is_blacklist", true);
		
		if (section.isConfigurationSection("filter"))
			mFilter = EntityFilter.from(section.getConfigurationSection("filter"));
		else
			mFilter = new EntityFilter();
		
		// Load deprecated list
		if (section.isList("mob_types"))
		{
			List<String> types = section.getStringList("mob_types");
			
			for(String typeName : types)
			{
				EntityType type = EntityType.valueOf(typeName);
				if(type != null && type.isAlive())
					mFilter.addType(type);
			}
		}
	}
}
