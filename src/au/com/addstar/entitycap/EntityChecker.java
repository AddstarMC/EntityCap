package au.com.addstar.entitycap;

import au.com.addstar.entitycap.group.Callback;
import au.com.addstar.entitycap.group.EntityConcentrationMap;
import au.com.addstar.entitycap.group.EntityGroup;

public class EntityChecker implements Callback<EntityConcentrationMap>
{
	private GroupSettings mSettings;
	private CheckReport mReporter;
	
	public EntityChecker(GroupSettings settings, CheckReport reporter)
	{
		mSettings = settings;
		mReporter = reporter;
	}
	
	@Override
	public void onCompleted( EntityConcentrationMap data )
	{
		for(EntityGroup group : data.getAllGroups())
		{
			if(!mSettings.matchesWarn(group))
				continue;
			
			mReporter.reportGroup(mSettings, group.getEntities().size(), group.getLocation());
		}
		
		mReporter.groupDone(mSettings);
	}
}
