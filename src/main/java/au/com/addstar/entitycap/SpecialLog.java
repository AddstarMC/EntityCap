package au.com.addstar.entitycap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Collection;

class SpecialLog
{
	public static void log(String message)
	{
		if(mStream != null)
			mStream.println(String.format("[%s] %s", DateFormat.getDateTimeInstance().format(System.currentTimeMillis()), message));
	}
	
	public static void log(Collection<String> lines)
	{
		if(mStream == null)
			return;
		
		for(String line : lines)
			log(line);
		
		mStream.flush();
	}
	
	public static void flush()
	{
		if(mStream != null)
			mStream.flush();
	}
	
	private static PrintWriter mStream;
	
	public static boolean initialize(File file)
	{
		try
		{
			mStream = new PrintWriter(new FileOutputStream(file, true));
			return true;
		}
		catch(IOException e)
		{
			return false;
		}
	}
	
	public static void shutdown()
	{
		if (mStream != null)
			mStream.close();
	}
}
