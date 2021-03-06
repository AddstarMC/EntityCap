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

	private boolean debug = EntityCapPlugin.instance.isDebug();
	
	// Chunk only settings
	private boolean mIsChunkOnly;
	private int mChunkRadius;
	
	public GroupSettings()
	{
	}
	
	public String getName()
	{
		return mName;
	}
	
	public boolean matches(Entity e) {
		if(debug){
            boolean filtermatch = mFilter.matches(e);
            boolean isOld = e.getTicksLived() >= mMinTicksLived;
			EntityCapPlugin.instance.getLogger().info("Entity: " + e.getName()+" tested against: " + mFilter.toString() + "- Result : " + filtermatch);
			EntityCapPlugin.instance.getLogger().info("Entity Old: " + isOld);
            return filtermatch && isOld ;
        }else{
		    return mFilter.matches(e) && e.getTicksLived() >= mMinTicksLived;
        }
	}
	
	public boolean matches(EntityGroup group)
	{
		if(debug){
			EntityCapPlugin.instance.getLogger().info("Group: "+ group.getEntities().iterator().next().getType().name()+ "  match again:" + this.mName +" : " + (group.getEntities().size() > mMaxAmount && group.getDensity() > mMaxDensity)

			);
		}
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
		mChunkRadius = section.getInt("chunk_radius",0);
		mWorlds = new HashSet<>();
		mWorldsBlacklist = EntityCapPlugin.defineWorldForSecion(section,mWorlds);
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
				if(type.isAlive())
					mFilter.addType(type);
			}
		}
	}

	@Override
	public String toString() {
		return "GroupSettings{" +
				"mFilter=" + mFilter +
				", mMaxAmount=" + mMaxAmount +
				", mMaxDensity=" + mMaxDensity +
				", mMinTicksLived=" + mMinTicksLived +
				", mAutoRun=" + mAutoRun +
				", mName='" + mName + '\'' +
				", mDisableKill=" + mDisableKill +
				", mWarnThreshold=" + mWarnThreshold +
				", mWorlds=" + mWorlds +
				", mWorldsBlacklist=" + mWorldsBlacklist +
				", mIsChunkOnly=" + mIsChunkOnly +
				", mChunkRadius=" + mChunkRadius+
				'}';
	}
}
