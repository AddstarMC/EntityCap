package au.com.addstar.entitycap;

import au.com.addstar.entitycap.group.EntityConcentrationMap;

public class EntityKillerTask implements Runnable
{
	private EntityCapPlugin mPlugin;
	
	public EntityKillerTask(EntityCapPlugin plugin)
	{
		mPlugin = plugin;
	}
			
	@Override
	public void run()
	{
		if(!EntityConcentrationMap.isRunning())
			mPlugin.runGroups(false);
	}

}
