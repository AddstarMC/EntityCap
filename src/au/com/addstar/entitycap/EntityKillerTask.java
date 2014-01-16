package au.com.addstar.entitycap;

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
		mPlugin.runGroups(false);
	}

}
