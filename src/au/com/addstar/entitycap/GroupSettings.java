package au.com.addstar.entitycap;

import java.util.HashSet;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Tameable;

public class GroupSettings
{
	private HashSet<EntityType> mActiveTypes;
	private int mMaxAmount;
	private int mRadius;
	private boolean mAutoRun;
	private String mName;
	
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
	
	public void load(ConfigurationSection section) throws InvalidConfigurationException
	{
		mActiveTypes.clear();
		mName = section.getName();
		mMaxAmount = section.getInt("max_entities");
		mRadius = section.getInt("radius");
		mAutoRun = section.getBoolean("autorun");
		
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
