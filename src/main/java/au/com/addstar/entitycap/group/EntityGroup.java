package au.com.addstar.entitycap.group;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class EntityGroup implements Comparable<EntityGroup>
{
	private static final double defaultRadius = 8;
	private static int mNextId = 0;
	
	private final int mId;
	
	private double mRadius;
	private Location mLocation;
	
	private double mMeanDistance;
	
	private final ArrayList<Entity> mEntities;
	
	public EntityGroup(Location location)
	{
		mId = mNextId++;
		
		mLocation = location;
		mRadius = defaultRadius * defaultRadius;
		mEntities = new ArrayList<>();
	}
	
	public void addEntity(Entity entity)
	{
		mEntities.add(entity);
	}
	
	public Location getLocation()
	{
		return mLocation;
	}
	
	public double getRadius()
	{
		return Math.sqrt(mRadius);
	}
	
	public double getRadiusSq()
	{
		return mRadius;
	}
	
	public boolean isInGroup(Location location) {
		return mLocation.getWorld() == location.getWorld() && (mLocation.distanceSquared(location) <= mRadius);

	}
	
	public boolean isTooClose(Location location) {
		return mLocation.getWorld() == location.getWorld() && (mLocation.distanceSquared(location) <= (mRadius + (defaultRadius * defaultRadius)));

	}
	
	public void mergeWith(EntityGroup other)
	{
		Validate.isTrue(other.mLocation.getWorld() == mLocation.getWorld());

		// Shift this groups locations by the radius of the other group, towards the other group
		double radiusOther = other.getRadius();
		Vector vec = other.mLocation.toVector().subtract(mLocation.toVector()).normalize().multiply(radiusOther);
		mLocation.add(vec);
		
		// Now expand my radius so i still cover the area I had before
		mRadius += other.mRadius;
		
		mEntities.addAll(other.mEntities);
	}
	
	public void mergeWith(Location location)
	{
		Validate.isTrue(location.getWorld() == mLocation.getWorld());

		// Shift this groups locations by the radius of the other group, towards the other group
		Vector vec = location.toVector().subtract(mLocation.toVector()).normalize().multiply(defaultRadius);
		mLocation.add(vec);
		
		// Now expand my radius so i still cover the area I had before
		mRadius += defaultRadius * defaultRadius;
	}
	
	public boolean shouldMergeWith(EntityGroup other) {
		return mLocation.getWorld() == other.mLocation.getWorld() && (mLocation.distanceSquared(other.mLocation) < (mRadius + other.mRadius));

	}
	
	private double getSmallestDistanceToNeighbour(Entity entity, Location temp)
	{
		double min = Double.MAX_VALUE;
		Location loc = entity.getLocation();
		for(Entity ent : mEntities)
		{
			if(ent == entity)
				continue;
			
			ent.getLocation(temp);
			
			double dist = temp.distanceSquared(loc);
			if(dist < min)
				min = dist;
		}
		
		return min;
	}
	
	public void stripOutliers()
	{
		if(mEntities.size() <= 2)
			return;
		
		double mean = 0;
		Location temp = new Location(null, 0, 0, 0);
		
		ArrayList<Double> dists = new ArrayList<>(mEntities.size());
		
		for(Entity ent : mEntities)
		{
			double dist = getSmallestDistanceToNeighbour(ent, temp);
			
			mean += dist;
			dists.add(dist);
		}
		
		mean /= mEntities.size();
		mMeanDistance = mean;
		
		// Calculate STD dev
		double stdDev = 0;
		for(double dist : dists)
			stdDev += Math.pow(dist - mean, 2);
		
		stdDev = Math.sqrt(stdDev / mEntities.size());
		
		Iterator<Entity> it = mEntities.iterator();
		while(it.hasNext())
		{
			Entity ent = it.next();
			double dist = getSmallestDistanceToNeighbour(ent, temp);
			if(dist - mean > 2 * stdDev)
				it.remove();
		}
	}
	
	public void adjustBoundingSphere()
	{
		if(mEntities.size() < 3)
			return;
		
		Location temp = new Location(null, 0, 0, 0);
		
		Entity point1;
		Entity point2;
		
		point1 = getFurthestFrom(mEntities.get(0), temp);
		point2 = getFurthestFrom(point1, temp);
		
		mLocation = point1.getLocation().add(point2.getLocation()).multiply(0.5);
		mRadius = point1.getLocation().distance(point2.getLocation()) / 2D;
		
		while(true)
		{
			Entity outside = null;
			double dist = 0;
			
			for(Entity ent : mEntities)
			{
				ent.getLocation(temp);
				dist = temp.distance(mLocation);
				if(dist - 2 > mRadius)
				{
					outside = ent;
					break;
				}
			}
			
			if(outside == null)
				break;

			dist += mRadius;
			mRadius = dist / 2;
			
			Vector vec = temp.toVector().subtract(mLocation.toVector());
			vec.normalize();
			vec.multiply(mRadius);

			temp.subtract(vec);
			
			Location temp2 = mLocation;
			mLocation = temp;
			temp = temp2;

			Validate.isTrue(outside.getLocation().distance(mLocation) <= mRadius + 2, "Sphere did not move to encompas the point. Rad: " + mRadius + " Req: " + temp.distance(mLocation));
		}
		
		mRadius = mRadius * mRadius;
	}
	
	private Entity getFurthestFrom(Entity entity, Location temp)
	{
		double max = Double.MIN_VALUE;
		Entity maxEnt = entity;
		
		Location loc = entity.getLocation();
		for(Entity ent : mEntities)
		{
			if(ent == entity)
				continue;
			
			ent.getLocation(temp);
			
			double dist = temp.distanceSquared(loc);
			if(dist > max)
			{
				max = dist;
				maxEnt = ent;
			}
		}
		
		return maxEnt;
	}
	
	public List<Entity> getEntities()
	{
		return mEntities;
	}
	
	public float getDensity()
	{
		// Treat it as a 2D area
		return mEntities.size() / (float)(Math.PI * mRadius);
	}
	
	public float getSpacing()
	{
		return (float)mMeanDistance;
	}
	
	@Override
	public String toString()
	{
		return String.format("Group{%d,%d,%d,%s-%d Entities: %d}", mLocation.getBlockX(), mLocation.getBlockY(), mLocation.getBlockZ(), (mLocation.getWorld() != null)?mLocation.getWorld().getName():"", (int)getRadius(), mEntities.size());
	}

	@Override
	public int compareTo( EntityGroup o )
	{
		return Integer.compare(mEntities.size(), o.mEntities.size()) * -1; // Higher first
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof EntityGroup))
			return false;
		
		EntityGroup other = (EntityGroup)obj;
		
		return other.mId == mId;
	}
	
	@Override
	public int hashCode()
	{
		return mId;
	}
}
