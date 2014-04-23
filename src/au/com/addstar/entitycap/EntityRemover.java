package au.com.addstar.entitycap;

import java.util.logging.Logger;

import org.bukkit.entity.Entity;

import au.com.addstar.entitycap.group.Callback;
import au.com.addstar.entitycap.group.EntityConcentrationMap;
import au.com.addstar.entitycap.group.EntityGroup;

public class EntityRemover implements Callback<EntityConcentrationMap>
{
	private GroupSettings mSettings;
	private boolean mPrintResults;
	private boolean mIncludeEmpty;
	private Logger mLog;
	
	public EntityRemover(GroupSettings settings, Logger logger, boolean printResult, boolean printEmpty)
	{
		mSettings = settings;
		mLog = logger;
		mPrintResults = printResult;
		mIncludeEmpty = printEmpty;
	}
	
	@Override
	public void onCompleted( EntityConcentrationMap data )
	{
		int total = 0;
		for(EntityGroup group : data.getAllGroups())
		{
			if(!mSettings.matches(group))
				continue;
			
			int count = 0;
			
			// TODO: Balance removal so it tries to keep the same number of different types (ie. dyed sheep)
			for(Entity ent : group.getEntities())
			{
				++count;
				if(ent.isValid() && count > mSettings.getMaxEntities())
				{
					ent.remove();
					++total;
				}
			}
		}
		
		if(total > 0 && mPrintResults)
			mLog.info(String.format("Removed %d entities from %s group", total, mSettings.getName()));
		else if(total == 0 && mIncludeEmpty)
			mLog.info(String.format("No entities were removed from %s", mSettings.getName()));
	}
}
