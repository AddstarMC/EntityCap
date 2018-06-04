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
	private final List<FilterAction> actions;
	
	public EntityFilter()
	{
		actions = new ArrayList<>();
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		for(FilterAction action: actions){
			result.append(action.toString());
		}
		return result.toString();
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
		if(EntityCapPlugin.instance.isDebug())EntityCapPlugin.instance.getLogger().info("Filter: " + filter.toString());
		return filter;
	}
	
	public void addType(EntityType type)
	{
		actions.add(new FilterAction(type, true));
	}
	
	public boolean matches(Entity e) {
        boolean debug = EntityCapPlugin.instance.isDebug();
        if(debug){EntityCapPlugin.instance.getLogger().info("Entity: " + e.getName() + " testing :" + this.toString());}
        // Dont match players or NPCs
        if (e instanceof HumanEntity) {
            if (debug) {
                EntityCapPlugin.instance.getLogger().info("   Human: true ");
            }
            return false;
        }

        // Dont match 'pets'
        if (e instanceof Tameable) {
            if (((Tameable) e).isTamed()) {
                if (debug) {
                    EntityCapPlugin.instance.getLogger().info("   Tamed: true ");
                }
                return false;
            }
        }


        if (e instanceof LivingEntity) {
            if (e.getCustomName() != null) {
                if (debug) {
                    EntityCapPlugin.instance.getLogger().info("   CustomName : true ");
                }
                return false;
            }
        }

        // Dont match things being ridden
        if (e.getPassengers() != null) {
            if (debug) {
                EntityCapPlugin.instance.getLogger().info("   Has Passengers : true ");
            }
            return false;
        }
        // Get the highest level match
        int bestScore = -1;
        FilterAction best = null;
        for (FilterAction action : actions) {
            // Dont bother checking when it wont help
            if (action.getPriority() <= bestScore)
                continue;

            if (action.matches(e)) {
                best = action;
                bestScore = action.getPriority();
            }
        }
        if(debug) {
            if (best != null)
                EntityCapPlugin.instance.getLogger().info("   Best Match: " + best.type.toString() + " Include: " + best.include);
            if (best == null)
                EntityCapPlugin.instance.getLogger().info("   Filter was unable to get any match and is false");
        }
        // No matching filter action. We will assume it is not part of the set to be on the safe side
        return best != null && best.include;

    }
	
	private enum FilterCategory
	{
		Animals(1),
		Mobs(1),
		Vehicles(2),
		Inventories(3),
		Specials(1),
		All(0);
		
		private final int mPriority;
		
		FilterCategory(int priority)
		{
			mPriority = priority;
		}
		
		int getPriority()
		{
			return mPriority;
		}
	}
	
	private static class FilterAction
	{
		EntityType type;
		FilterCategory category;
		
		final boolean include;
		
		FilterAction(EntityType type, boolean include)
		{
			this.type = type;
			this.include = include;
		}
		
		FilterAction(FilterCategory category, boolean include)
		{
			this.category = category;
			this.include = include;
		}
		
		int getPriority()
		{
			if (category != null)
				return category.getPriority();
			else
				return 10;
		}
		
		boolean matches(Entity entity)
		{
		    boolean debug = EntityCapPlugin.instance.isDebug();
			if (entity instanceof HumanEntity)
				return false;
			
			if (type != null) {
                if (debug) EntityCapPlugin.instance.getLogger().info("  FilterAction match on Type is: " + (entity.getType() == type));
                return entity.getType() == type;
            }
			
			switch (category)
			{
			case All:
                if (debug) EntityCapPlugin.instance.getLogger().info("  FilterAction match on ALL");
				return true;
			case Vehicles:
                if(entity instanceof Vehicle && !(entity instanceof Pig)) {
                    if (debug) EntityCapPlugin.instance.getLogger().info("  FilterAction match on ALL");
                    return true;
                }
                case Animals:
				if(entity instanceof Animals){
                    if (debug) EntityCapPlugin.instance.getLogger().info("  FilterAction match on Animal");
                    return true;
                }
                case Mobs:

				if(entity instanceof Monster || entity instanceof Ghast || entity instanceof Slime){
                    if (debug) EntityCapPlugin.instance.getLogger().info("  FilterAction match on Mob");
                    return true;
                };
			case Inventories:
				if(entity instanceof InventoryHolder || entity instanceof ItemFrame || entity instanceof ArmorStand){
                    if (debug) EntityCapPlugin.instance.getLogger().info("  FilterAction match on Inventories");
                    return true;
                };
			case Specials:
				if(
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
					entity instanceof ArmorStand){
                    if (debug) EntityCapPlugin.instance.getLogger().info("  Filter match on Special");
                    return true;
                }
                default:
                    return false;
			}
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
