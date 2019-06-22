package au.com.addstar.entitycap;

import au.com.addstar.entitycap.group.Callback;
import au.com.addstar.entitycap.group.EntityConcentrationMap;
import au.com.addstar.entitycap.group.EntityGroup;

import java.util.logging.Logger;


public class EntityChecker implements Callback<EntityConcentrationMap>
{
	private final GroupSettings mSettings;
	private final CheckReport mReporter;
	private final Logger log = EntityCapPlugin.instance.getLogger();
	private boolean debug = EntityCapPlugin.instance.isDebug();
	
	public EntityChecker(GroupSettings settings, CheckReport reporter)
	{
		mSettings = settings;
		mReporter = reporter;
	}
	
	@Override
	public void onCompleted( EntityConcentrationMap data )
	{
		if(debug)log.info("EntityCap: processing "+  data.getAllGroups().size() + " groups in " + mSettings.getName() + ":");
		for(EntityGroup group : data.getAllGroups())
		{
			if(debug)log.info("  Group has " +  group.getEntities().size()+ " entities, density ->" + group.getDensity());

			if(!mSettings.matchesWarn(group)) {
				if (debug) log.info("  Group did not match  warn settings");
				continue;
			}
			mReporter.reportGroup(mSettings, group.getEntities().size(), group.getLocation());
		}
		
		mReporter.groupDone(mSettings);
	}
}
