package au.com.addstar.entitycap;

import au.com.addstar.entitycap.group.EntityConcentrationMap;

class EntityKillerTask implements Runnable
{
	private final EntityCapPlugin mPlugin;
	
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
