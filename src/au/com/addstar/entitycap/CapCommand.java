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
		if(args.length == 0)
		{
			sender.sendMessage(ChatColor.RED + String.format("Usage: /%s check <player> [rule] [rule2] ... [ruleN]", label));
			return;
		}
		
		Player[] players;
		
		if(args[0].equals("ALL"))
			players = Bukkit.getOnlinePlayers();
		else
		{
			Player player = Bukkit.getPlayer(args[0]);
			if(player == null)
			{
				sender.sendMessage(ChatColor.RED + "Unknown player " + args[0]);
				return;
			}
			players = new Player[] {player};
		}
		
		Collection<GroupSettings> allGroups = mPlugin.getGroups(true);
		LinkedList<GroupSettings> matching = new LinkedList<GroupSettings>();
		
		for(int i = 1; i < args.length; ++i)
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
		
		if(!matching.isEmpty())
			allGroups = matching;
		
		// Group check begin:
		
		int maxRadius = 0;
		for(GroupSettings setting : allGroups)
			maxRadius = Math.max(maxRadius, setting.getRadius());
		
		Location temp = new Location(null, 0, 0, 0);
		Location playerLoc = new Location(null, 0, 0, 0);
		String[] playerLists = new String[allGroups.size()];
		Arrays.fill(playerLists, "");

		boolean[] matches = new boolean[allGroups.size()];
		boolean matchedAny = false;
		
		for(Player player : players)
		{
			player.getLocation(playerLoc);
			List<Entity> nearby = player.getNearbyEntities(maxRadius, maxRadius, maxRadius);
			
			int index = 0;
			for(GroupSettings group : allGroups)
			{
				int radius = group.getRadius() * group.getRadius();
				int count = 0;
				
				for(Entity ent : nearby)
				{
					ent.getLocation(temp);
					if(ent.isValid() && group.matches(ent) && temp.distanceSquared(playerLoc) <= radius)
						++count;
				}
				
				if(count > group.getMaxEntities() || (count > group.getWarnThreshold() && group.getWarnThreshold() != 0))
				{
					matches[index] = true;
					if(!playerLists[index].isEmpty())
						playerLists[index] += ", ";
					playerLists[index] += String.format("%s(%d)", player.getName(), count);
					
					matchedAny = true;
				}
				++index;
			}
			
			if(players.length == 1)
			{
				if(matchedAny)
				{
					sender.sendMessage(ChatColor.GOLD + String.format("%s is exceeding the following rules:", player.getName()));
					index = 0;
					for(GroupSettings group : allGroups)
					{
						if(matches[index])
							sender.sendMessage(ChatColor.GRAY + "* " + ChatColor.YELLOW + group.getName());
						++index;
					}
				}
				else
					sender.sendMessage(ChatColor.GREEN + String.format("%s is not exceeding any rules", player.getName()));
			}
		}
		
		if(players.length != 1)
		{
			sender.sendMessage(ChatColor.GOLD + "Rules being exceeded currently: ");
			if(!matchedAny)
				sender.sendMessage(ChatColor.GREEN + " None");
			else
			{
				int index = 0;
				for(GroupSettings group : allGroups)
				{
					if(matches[index])
						sender.sendMessage(ChatColor.YELLOW + " " + group.getName() + ChatColor.GRAY + ": " + playerLists[index]);
					
					++index;
				}
			}
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
