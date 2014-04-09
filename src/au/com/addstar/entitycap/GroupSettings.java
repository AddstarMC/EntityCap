package au.com.addstar.entitycap;

import java.util.HashSet;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;

public class GroupSettings
{
	private HashSet<EntityType> mActiveTypes;
	private int mMaxAmount;
	private int mRadius;
	private boolean mAutoRun;
	private String mName;
	private boolean mDisableKill;
	private int mWarnThreshold;
	
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
		
		if(e instanceof Tameable)
			return !((Tameable)e).isTamed();
		
		if(e instanceof LivingEntity)
			return ((LivingEntity) e).getCustomName() == null;
		
		return true;
	}
	
	public int getRadius()
	{
		return mRadius;
	}
	
	public int getMaxEntities()
	{
		return mMaxAmount;
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
		mRadius = section.getInt("radius");
		mAutoRun = section.getBoolean("autorun");
		mWarnThreshold = section.getInt("warn_threshold", 0);
		mDisableKill = section.getBoolean("check_only", false);
		
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
