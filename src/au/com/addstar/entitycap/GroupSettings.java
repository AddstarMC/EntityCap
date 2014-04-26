package au.com.addstar.entitycap;

import java.util.HashSet;
import java.util.List;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import au.com.addstar.entitycap.group.EntityGroup;

public class GroupSettings
{
	private HashSet<EntityType> mActiveTypes;
	private int mMaxAmount;
	private double mMaxDensity;
	private int mMinTicksLived; 
	
	private boolean mAutoRun;
	private String mName;
	private boolean mDisableKill;
	private int mWarnThreshold;
	
	private HashSet<String> mWorlds;
	private boolean mWorldsBlacklist;
	
	public GroupSettings()
	{
		mActiveTypes = new HashSet<EntityType>();
	}
	
	public String getName()
	{
		return mName;
	}
	
	public boolean matches(Entity e)
	{
		if(!mActiveTypes.contains(e.getType()))
			return false;
		
		if(e.getTicksLived() < mMinTicksLived)
			return false;
		
		if(e instanceof Tameable)
		{
			if(((Tameable)e).isTamed())
				return false;
		}
		
		if(e instanceof LivingEntity)
		{
			if(((LivingEntity) e).getCustomName() != null)
				return false;
		}
		
		if(e.getPassenger() != null)
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
	
	public void load(ConfigurationSection section) throws InvalidConfigurationException
	{
		mActiveTypes.clear();
		mName = section.getName();
		mMaxAmount = section.getInt("max_entities");
		mMaxDensity = section.getDouble("max_density", 0);
		mMinTicksLived = section.getInt("min_ticks_lived", 0);
		mAutoRun = section.getBoolean("autorun");
		mWarnThreshold = section.getInt("warn_threshold", 0);
		mDisableKill = section.getBoolean("check_only", false);
		
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
		
		List<String> types = section.getStringList("mob_types");
		if(types == null)
			throw new InvalidConfigurationException("mob_types is not a list");
		
		for(String typeName : types)
		{
			EntityType type = EntityType.valueOf(typeName);
			if(type != null && type.isAlive())
				mActiveTypes.add(type);
		}
	}
}
