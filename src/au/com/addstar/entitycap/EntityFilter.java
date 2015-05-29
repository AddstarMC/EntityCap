package au.com.addstar.entitycap;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Weather;
import org.bukkit.inventory.InventoryHolder;

public class EntityFilter
{
	private List<FilterAction> actions;
	
	public EntityFilter()
	{
		actions = new ArrayList<FilterAction>();
	}
	
	public static EntityFilter from(ConfigurationSection section) throws InvalidConfigurationException
	{
		EntityFilter filter = new EntityFilter();
		for (String key : section.getKeys(false))
		{
			String value = section.getString(key);
			boolean include;
			if (value.equalsIgnoreCase("ignore"))
				include = false;
			else if (value.equalsIgnoreCase("include"))
				include = true;
			else
			{
				throw new InvalidConfigurationException(
						"Filter error in group " + section.getParent().getName() + " for type " + key + ": Unknown action " + value + 
						". Possible values are 'ignore', or 'include'");
			}
			
			// Try a filter category
			FilterAction action = null;
			for (FilterCategory c : FilterCategory.values())
			{
				if (c.name().equalsIgnoreCase(key))
				{
					action = new FilterAction(c, include);
					break;
				}
			}
			
			if (action == null)
			{
				// Try an entity type
				for (EntityType t : EntityType.values())
				{
					if (t.name().equalsIgnoreCase(key))
					{
						action = new FilterAction(t, include);
						break;
					}
				}
				
				if (action == null)
				{
					// No other options
					throw new InvalidConfigurationException(
							"Filter error in group " + section.getParent().getName() + ": Unknown type " + key + 
							". Possible values are " + StringUtils.join(FilterCategory.values(), ", ") + " or an entity type");
				}
			}
			
			filter.actions.add(action);
		}
		
		return filter;
	}
	
	public void addType(EntityType type)
	{
		actions.add(new FilterAction(type, true));
	}
	
	public boolean matches(Entity e)
	{
		// Dont match players or NPCs
		if (e instanceof HumanEntity)
			return false;
		
		// Dont match 'pets'
		if(e instanceof Tameable)
		{
			if(((Tameable)e).isTamed())
				return false;
		}
		
		if(e instanceof LivingEntity)
		{
			if(((LivingEntity)e).getCustomName() != null)
				return false;
		}
		
		// Dont match things being ridden
		if(e.getPassenger() != null)
			return false;
		
		// Get the highest level match
		int bestScore = -1;
		FilterAction best = null;
		for (FilterAction action : actions)
		{
			// Dont bother checking when it wont help
			if (action.getPriority() <= bestScore)
				continue;
			
			if (action.matches(e))
			{
				best = action;
				bestScore = action.getPriority();
			}
		}
		
		// No matching filter action. We will assume it is not part of the set to be on the safe side
		if (best == null)
			return false;
		
		return best.include;
	}
	
	private enum FilterCategory
	{
		Animals(1),
		Mobs(1),
		Vehicles(2),
		Inventories(3),
		Specials(1),
		All(0);
		
		private int mPriority;
		
		private FilterCategory(int priority)
		{
			mPriority = priority;
		}
		
		public int getPriority()
		{
			return mPriority;
		}
	}
	
	private static class FilterAction
	{
		public EntityType type;
		public FilterCategory category;
		
		public boolean include;
		
		public FilterAction(EntityType type, boolean include)
		{
			this.type = type;
			this.include = include;
		}
		
		public FilterAction(FilterCategory category, boolean include)
		{
			this.category = category;
			this.include = include;
		}
		
		public int getPriority()
		{
			if (category != null)
				return category.getPriority();
			else
				return 10;
		}
		
		public boolean matches(Entity entity)
		{
			if (entity instanceof HumanEntity)
				return false;
			
			if (type != null)
				return entity.getType() == type;
			
			switch (category)
			{
			case All:
				return true;
			case Vehicles:
				return entity instanceof Vehicle && !(entity instanceof Pig);
			case Animals:
				return entity instanceof Animals;
			case Mobs:
				return entity instanceof Monster || entity instanceof Ghast || entity instanceof Slime;
			case Inventories:
				return entity instanceof InventoryHolder || entity instanceof ItemFrame || entity instanceof ArmorStand;
			case Specials:
				return 
					entity instanceof Projectile ||
					entity instanceof Item ||
					entity instanceof Hanging ||
					entity instanceof Explosive ||
					entity instanceof EnderCrystal ||
					entity instanceof EnderSignal ||
					entity instanceof FallingBlock ||
					entity instanceof Weather ||
					entity instanceof ExperienceOrb ||
					entity instanceof Firework ||
					entity instanceof ArmorStand;
			}
			
			return false;
		}
		
		@Override
		public String toString()
		{
			if (category != null)
				return category.toString() + ": " + (include ? "include" : "ignore");
			else
				return type.toString() + ": " + (include ? "include" : "ignore");
		}
	}
}