package au.com.addstar.entitycap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Villager;

import com.google.common.collect.HashMultimap;

import au.com.addstar.entitycap.group.Callback;
import au.com.addstar.entitycap.group.EntityConcentrationMap;
import au.com.addstar.entitycap.group.EntityGroup;

public class EntityRemover implements Callback<EntityConcentrationMap>
{
	private final GroupSettings mSettings;
	private final boolean mPrintResults;
	private final boolean mIncludeEmpty;
	private final Logger mLog;
	
	public EntityRemover(GroupSettings settings, Logger logger, boolean printResult, boolean printEmpty)
	{
		mSettings = settings;
		mLog = logger;
		mPrintResults = printResult;
		mIncludeEmpty = printEmpty;
	}
	
	private int entityToNumber(Entity ent)
	{
		int base = ent.getType().ordinal() * 100;
		
		if(ent instanceof Sheep)
			base += ((Sheep)ent).getColor().ordinal();
		else if(ent instanceof Villager)
			base += ((Villager)ent).getProfession().ordinal();
		else if(ent instanceof Creeper)
			base += (((Creeper)ent).isPowered() ? 1 : 0);
		return base;
	}
	
	@Override
	public void onCompleted( EntityConcentrationMap data )
	{
		int total = 0;
		ArrayList<String> logLines = new ArrayList<>();
		for(EntityGroup group : data.getAllGroups())
		{
			if(!mSettings.matches(group))
				continue;

			// Put entities into bins, each bin represents a type (including differences between the same type, eg. sheep color)
			HashMultimap<Integer, Entity> binMap = HashMultimap.create();
			for(Entity ent : group.getEntities())
				binMap.put(entityToNumber(ent), ent);
			
			ArrayList<LinkedList<Entity>> bins = new ArrayList<>();
			for(Collection<Entity> bin : binMap.asMap().values())
				bins.add(new LinkedList<>(bin));

			
			// Process the removal
			int requiredCount = Math.max(mSettings.getMaxEntities(), (int)(mSettings.getMaxDensity() * (Math.PI * group.getRadiusSq())));
			int toRemove = group.getEntities().size() - requiredCount;
			
			int actual = 0;
			HashSet<EntityType> types = new HashSet<>();
			for (LinkedList<Entity> bin : bins) {
				float percent = bin.size() / (float) group.getEntities().size();
				int rc = (int) Math.round(percent * toRemove);

				for (; rc > 0 && !bin.isEmpty(); --rc) {
					Entity ent = bin.removeFirst();
					if (ent.isValid()) {
						types.add(ent.getType());
						ent.remove();
						++total;
						++actual;
					} else
						++rc;
				}
			}
			
			logLines.add(String.format("* Removing %d from %s radius %f", actual, String.format("%s,%d,%d,%d", group.getLocation().getWorld().getName(), group.getLocation().getBlockX(), group.getLocation().getBlockY(), group.getLocation().getBlockZ()), group.getRadius()));
			logLines.add("  - Types: " + types);
		}
		
		if(total > 0 && mPrintResults)
			mLog.info(String.format("Removed %d entities from %s group", total, mSettings.getName()));
		else if(total == 0 && mIncludeEmpty)
			mLog.info(String.format("No entities were removed from %s", mSettings.getName()));
		
		if (total > 0)
		{
			SpecialLog.log(String.format("Removed %d entities from %s group:", total, mSettings.getName()));
			SpecialLog.log(logLines);
		}
	}
}
