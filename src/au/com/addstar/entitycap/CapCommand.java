package au.com.addstar.entitycap;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class CapCommand implements CommandExecutor 
{
	private EntityCapPlugin mPlugin;
	
	public CapCommand(EntityCapPlugin plugin)
	{
		mPlugin = plugin;
	}
	
	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if(args.length < 1)
			return false;
		
		String sub = args[0];
		String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

		if(sub.equalsIgnoreCase("check"))
		{
			if(!sender.hasPermission("entitycap.check"))
				sender.sendMessage(ChatColor.RED + "You do not have permission to use that.");
			else
				handleCheck(sender, label, subArgs);
		}
		else if(sub.equalsIgnoreCase("info"))
		{
			if(!sender.hasPermission("entitycap.info"))
				sender.sendMessage(ChatColor.RED + "You do not have permission to use that.");
			else
				handleInfo(sender, label, subArgs);
		}
		else if(sub.equalsIgnoreCase("run"))
		{
			if(!sender.hasPermission("entitycap.run"))
				sender.sendMessage(ChatColor.RED + "You do not have permission to use that.");
			else
				handleRun(sender, label, subArgs);
		}
		
		return true;
	}
	
	private void handleInfo( CommandSender sender, String label, String[] args)
	{
		if(args.length != 2)
		{
			sender.sendMessage(ChatColor.RED + String.format("Usage: /%s info <player> <radius>", label));
			return;
		}
		
		Player player = Bukkit.getPlayer(args[0]);
		if(player == null)
		{
			sender.sendMessage(ChatColor.RED + "Unknown player " + args[0]);
			return;
		}
		
		int radius;
		
		try
		{
			radius = Integer.parseInt(args[1]);
		}
		catch(NumberFormatException e)
		{
			sender.sendMessage(ChatColor.RED + "Radius must be an whole number above 0");
			return;
		}
		
		if(radius <= 0)
		{
			sender.sendMessage(ChatColor.RED + "Radius must be an whole number above 0");
			return;
		}
		
		List<Entity> nearby = player.getNearbyEntities(radius, radius, radius);
		Location playerLoc = player.getLocation();
		Location temp = new Location(null, 0, 0, 0);
		int radiusSq = radius * radius;
		
		int[] counts = new int[EntityType.values().length];
		int total = 0;
		
		for(Entity entity : nearby)
		{
			entity.getLocation(temp);
			if(entity.isValid() && entity.getType().isAlive() && temp.distanceSquared(playerLoc) <= radiusSq)
			{
				++total;
				++counts[entity.getType().ordinal()];
			}
		}
		
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&6[EntityCap] &e%d &fentities are within &e%d &fradius of &e%s&f:", total, radius, player.getName())));
		for(EntityType type : EntityType.values())
		{
			int count = counts[type.ordinal()];
			if(count > 0)
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("  &7%ss: &e%d", type.name(), count)));
		}
	}
	
	private void handleCheck( CommandSender sender, String label, String[] args)
	{
		Collection<GroupSettings> allGroups = mPlugin.getGroups(true);
		LinkedList<GroupSettings> matching = new LinkedList<GroupSettings>();
		
		for(int i = 0; i < args.length; ++i)
		{
			boolean found = false;
			for(GroupSettings group : allGroups)
			{
				if(group.getName().equalsIgnoreCase(args[i]))
				{
					found = true;
					matching.add(group);
					break;
				}
			}
			
			if(!found)
			{
				sender.sendMessage(ChatColor.RED + "Unknown rule " + args[i]);
				return;
			}
		}
		
		sender.sendMessage(ChatColor.GOLD + "Starting check");
		
		if(matching.isEmpty())
			mPlugin.checkGroups(true, sender);
		else
		{
			CheckReport report = new CheckReport(sender);
			for(GroupSettings group : matching)
				mPlugin.checkGroup(group, report);
		}
	}
	
	private void handleRun(CommandSender sender, String label, String[] args)
	{
		if(args.length != 0)
		{
			sender.sendMessage(ChatColor.RED + String.format("Usage: /%s run", label));
			return;
		}
		
		sender.sendMessage(ChatColor.YELLOW + "Running all rules.");
		mPlugin.runGroups(true);
	}
}
