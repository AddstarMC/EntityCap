package au.com.addstar.entitycap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

class CheckReport
{
	private final HashMap<GroupSettings, StringBuilder> mReport;
	private final HashSet<GroupSettings> mWaiting;
	private final CommandSender mSender;
	
	public CheckReport(CommandSender sender)
	{
		mReport = new HashMap<>();
		mWaiting = new HashSet<>();
		mSender = sender;
	}
	
	public void waitForGroup(GroupSettings group)
	{
		mWaiting.add(group);
	}
	
	public void reportGroup(GroupSettings group, int count, Location location)
	{
        StringBuilder builder = mReport.computeIfAbsent(group, k -> new StringBuilder());

        if(builder.length() != 0)
			builder.append(", ");
		
		if(count >= group.getMaxEntities())
			builder.append(ChatColor.translateAlternateColorCodes('&', String.format("%d,%d,%d(&c%d&7)", location.getBlockX(), location.getBlockY(), location.getBlockZ(), count)));
		else
			builder.append(ChatColor.translateAlternateColorCodes('&', String.format("%d,%d,%d(&e%d&7)", location.getBlockX(), location.getBlockY(), location.getBlockZ(), count)));
	}
	
	public void groupDone(GroupSettings group)
	{
		mWaiting.remove(group);
		if(mWaiting.isEmpty())
			output();
	}
	
	private void output()
	{
		mSender.sendMessage(ChatColor.GOLD + "Rules being exceeded currently: ");
		
		if(mReport.isEmpty())
			mSender.sendMessage(ChatColor.GREEN + " None");
		else
		{
			for(Entry<GroupSettings, StringBuilder> entry : mReport.entrySet())
				mSender.sendMessage(ChatColor.YELLOW + " " + entry.getKey().getName() + ChatColor.GRAY + ": " + entry.getValue().toString());
		}
	}
}
