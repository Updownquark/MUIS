package org.muis.base.layout;

import java.awt.Rectangle;

import org.muis.core.MuisElement;
import org.muis.core.layout.SimpleSizePolicy;
import org.muis.core.layout.SizePolicy;

public class LazyFlowLayout extends AbstractFlowLayout
{
	@Override
	public void setBreakPolicy(BreakPolicy policy)
	{
		if(policy == BreakPolicy.SQUARE)
			throw new IllegalArgumentException("Square break policy is not valid with " + getClass().getName());
		super.setBreakPolicy(policy);
	}

	@Override
	protected SizePolicy getMajorSize(MuisElement [] children, int minorSize)
	{
		if(children.length == 1)
			return getChildSizer(children[0], true, minorSize);
		SimpleSizePolicy ret = new SimpleSizePolicy();
		if(children.length == 0)
			return ret;

		SizePolicy [] childSizers = new SizePolicy [children.length];
		for(int i = 0; i < children.length; i++)
			childSizers[i] = getChildSizer(children[i], true, -1);

		int maxStretch = -Integer.MAX_VALUE;
		for(SizePolicy sizer : childSizers)
			if(sizer.getStretch() > maxStretch)
				maxStretch = sizer.getStretch();
		ret.setStretch(maxStretch);

		VirtualLayout vl = new VirtualLayout();
		vl.setContainerSize(Integer.MAX_VALUE, minorSize);
		VirtualLayout.LayoutHoleIterator iter = vl.iterator();

		int tryMinorSize = minorSize > 0 ? minorSize : Integer.MAX_VALUE;
		// max size
		for(int i = 0; i < children.length; i++)
			placeChild(children, i, childSizers[i], null, iter, Integer.MAX_VALUE, tryMinorSize, true);
		ret.setMax(vl.getUsedWidth());
		vl.clearConstraints();

		// preferred size
		for(int i = 0; i < children.length; i++)
			placeChild(children, i, childSizers[i], null, iter, -1, tryMinorSize, true);
		ret.setPreferred(vl.getUsedWidth());

		if(minorSize <= 0)
		{
			iter.setVertical(true);
			// min size
			for(int i = 0; i < children.length; i++)
				placeChild(children, i, childSizers[i], null, iter, 0, 0, getBreakPolicy() != BreakPolicy.NEVER);
			ret.setMin(vl.getUsedWidth());
			vl.clearConstraints();
		}
		else
		{
			vl.setContainerSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
			iter.setVertical(true);
			// min size
			for(int i = 0; i < children.length; i++)
				placeChild(children, i, childSizers[i], null, iter, 0, 0, getBreakPolicy() == BreakPolicy.NEEDED);
			if(vl.getUsedHeight() > minorSize)
			{
				// Perform several operations to try to satisfy the minorSize while keeping the minimum major size as
				// small as possible.
				boolean [] onRow = new boolean [children.length];
				if(getBreakPolicy() != BreakPolicy.NEVER)
				{
					// 1) Stack elements more than one to a row where possible without increasing the overall major size
					// at all
					int w = vl.getUsedWidth();
					onRow[0] = true;
					for(int i = 1; i < vl.getConstraintCount(); i++)
					{
						Rectangle c1 = vl.getConstraint(i - 1);
						Rectangle c2 = vl.getConstraint(i);
						if(c1.x + c1.width + c2.width > w)
							continue;
						onRow[i] = true;
						while(vl.getConstraintCount() > i)
							vl.removeConstraint(vl.getConstraintCount() - 1);
						// Try to place the child on the same row as the previous child
						placeChild(children, i, childSizers[i], null, iter, 0, 0, true);
						for(int j = i + 1; j < children.length; j++)
							placeChild(children, j, childSizers[j], null, iter, 0, 0, false);
						if(vl.getUsedHeight() <= minorSize)
							break;
					}
				}
				else
					for(int i = 0; i < onRow.length; i++)
						onRow[i] = true;
				if(vl.getUsedHeight() > minorSize)
				{
					// 2) Find out how much space can be gained by increasing inverse-dependent elements to their
					// preferred major sizes.
					boolean [] inverse = new boolean [children.length];
					for(int i = 0; i < children.length; i++)
					{
						if(childSizers[i].getPreferred() <= childSizers[i].getMin())
							continue; // Preferred size same as min size
						SizePolicy minPolicy = getChildSizer(children[i], false, childSizers[i].getMin());
						SizePolicy prefPolicy = getChildSizer(children[i], false, childSizers[i].getPreferred());
						if(minPolicy.getMin() <= prefPolicy.getMin())
							// Not an inverse-dependent element--using preferred major size doesn't gain anything
							continue;
						inverse[i] = true;
						while(vl.getConstraintCount() > i)
							vl.removeConstraint(vl.getConstraintCount() - 1);
						// Try to place the child using its preferred major size
						placeChild(children, i, childSizers[i], null, iter, -1, 0, onRow[i]);
						for(int j = i + 1; j < children.length; j++)
							placeChild(children, j, childSizers[j], null, iter, 0, 0, false);
						if(vl.getUsedHeight() <= minorSize)
							break;
					}
					if(vl.getUsedHeight() > minorSize)
					{
						if(getBreakPolicy() != BreakPolicy.NEVER)
						{
							// 3) Stack elements on the same rows as cheaply as possible, one by one until either the
							// minorSize is met or all elements are on a single row
							int minW;
							int minWIndex;
							do
							{
								minW = -1;
								minWIndex = -1;
								for(int i = 1; i < children.length; i++)
								{
									if(onRow[i])
										continue; // Already on the next row as its parent.
									Rectangle c1 = vl.getConstraint(i - 1);
									Rectangle c2 = vl.getConstraint(i);
									int w2 = c1.x + c1.width + c2.width;
									if(minW < 0 || w2 < minW)
									{
										minW = w2;
										minWIndex = i;
									}
								}
								if(minWIndex >= 0)
								{
									onRow[minWIndex] = true;
									while(vl.getConstraintCount() > minWIndex)
										vl.removeConstraint(vl.getConstraintCount() - 1);
									int type = inverse[minWIndex] ? -1 : 0;
									// Try to place the child on the same row as its predecessor
									placeChild(children, minWIndex, childSizers[minWIndex], null, iter, type, 0,
										onRow[minWIndex]);
									for(int j = minWIndex + 1; j < children.length; j++)
									{
										type = inverse[j] ? -1 : 0;
										placeChild(children, j, childSizers[j], null, iter, type, 0, onRow[j]);
									}
								}
							} while(minWIndex >= 0 && vl.getUsedHeight() > minorSize);
						}
						if(vl.getUsedHeight() > minorSize)
							// 4) Shrink the inverse-dependent elements that are too long down to the maximum of their
							// min length and the minorSize
							for(int i = 0; i < children.length; i++)
							{
								if(!inverse[i])
									continue;
								Rectangle c = vl.getConstraint(i);
								if(c.y + c.height <= minorSize)
									continue;
								SizePolicy minSizer = getChildSizer(children[i], false, -1);
								if(minSizer.getMin() >= c.height)
									continue;
								while(vl.getConstraintCount() > i)
									vl.removeConstraint(vl.getConstraintCount() - 1);
								placeChild(children, i, null, minSizer, iter, 0, minSizer.getMin(), onRow[i]);
								for(int j = i + 1; j < children.length; j++)
								{
									int type = inverse[j] ? -1 : 0;
									placeChild(children, j, childSizers[j], null, iter, type, 0, onRow[j]);
								}
							}
					}
					int diff = minorSize - vl.getUsedHeight();
					if(diff > 0)
					{
						// Redistribute the extra size to inverse-dependent elements based on their stretch factor
						// TODO
					}
				}
			}
			ret.setMin(vl.getUsedWidth());
		}

		return ret;
	}

	@Override
	protected SizePolicy getMinorSize(MuisElement [] children, int majorSize)
	{
		if(children.length == 1)
			return getChildSizer(children[0], false, majorSize);
		SimpleSizePolicy ret = new SimpleSizePolicy();
		if(children.length == 0)
			return ret;

		SizePolicy [] childSizers = new SizePolicy [children.length];
		for(int i = 0; i < children.length; i++)
			childSizers[i] = getChildSizer(children[i], true, -1);

		int maxStretch = -Integer.MAX_VALUE;
		for(SizePolicy sizer : childSizers)
			if(sizer.getStretch() > maxStretch)
				maxStretch = sizer.getStretch();
		ret.setStretch(maxStretch);

		VirtualLayout vl = new VirtualLayout();
		vl.setContainerSize(majorSize, Integer.MAX_VALUE);
		VirtualLayout.LayoutHoleIterator iter = vl.iterator();

		// max size
		for(int i = 0; i < children.length; i++)
			placeChild(children, i, null, childSizers[i], iter, -1, Integer.MAX_VALUE,
				getBreakPolicy() != BreakPolicy.NEVER);
		ret.setMax(vl.getUsedWidth());
		vl.clearConstraints();

		// preferred size
		for(int i = 0; i < children.length; i++)
			placeChild(children, i, null, childSizers[i], iter, -1, -1, true);
		ret.setPreferred(vl.getUsedWidth());

		if(majorSize <= 0)
		{
			iter.setVertical(true);
			// min size
			for(int i = 0; i < children.length; i++)
				placeChild(children, i, null, childSizers[i], iter, 0, 0, true);
			ret.setMin(vl.getUsedWidth());
			vl.clearConstraints();
		}
		else
		{
			vl.setContainerSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
			iter.setVertical(true);
			// min size
			for(int i = 0; i < children.length; i++)
				placeChild(children, i, null, childSizers[i], iter, 0, 0, true);
			if(vl.getUsedWidth() > majorSize)
			{
				// Perform several operations to try to satisfy the majorSize while keeping the minimum minor size as
				// small as possible.
				boolean [] onCol = new boolean [children.length];
				if(getBreakPolicy() != BreakPolicy.NEVER)
				{
					// 1) Stack elements more than one to a column where possible without increasing the overall minor
					// size
					// at all
					int h = vl.getUsedHeight();
					onCol[0] = true;
					for(int i = 1; i < vl.getConstraintCount(); i++)
					{
						Rectangle c1 = vl.getConstraint(i - 1);
						Rectangle c2 = vl.getConstraint(i);
						if(c1.y + c1.height + c2.height > h)
							continue;
						onCol[i] = true;
						while(vl.getConstraintCount() > i)
							vl.removeConstraint(vl.getConstraintCount() - 1);
						// Try to place the child on the same row as the previous child
						placeChild(children, i, null, childSizers[i], iter, 0, 0, false);
						for(int j = i + 1; j < children.length; j++)
							placeChild(children, j, null, childSizers[j], iter, 0, 0, true);
						if(vl.getUsedWidth() <= majorSize)
							break;
					}
				}
				if(vl.getUsedWidth() > majorSize)
				{
					// 2) Find out how much space can be gained by increasing inverse-dependent elements to their
					// preferred minor sizes.
					boolean [] inverse = new boolean [children.length];
					for(int i = 0; i < children.length; i++)
					{
						if(childSizers[i].getPreferred() <= childSizers[i].getMin())
							continue; // Preferred size same as min size
						SizePolicy minPolicy = getChildSizer(children[i], true, childSizers[i].getMin());
						SizePolicy prefPolicy = getChildSizer(children[i], true, childSizers[i].getPreferred());
						if(minPolicy.getMin() <= prefPolicy.getMin())
							// Not an inverse-dependent element--using preferred minor size doesn't gain anything
							continue;
						inverse[i] = true;
						while(vl.getConstraintCount() > i)
							vl.removeConstraint(vl.getConstraintCount() - 1);
						// Try to place the child using its preferred minor size
						placeChild(children, i, null, childSizers[i], iter, 0, -1, !onCol[i]);
						for(int j = i + 1; j < children.length; j++)
							placeChild(children, j, null, childSizers[j], iter, 0, 0, true);
						if(vl.getUsedWidth() <= majorSize)
							break;
					}
					if(vl.getUsedWidth() > majorSize)
					{
						if(getBreakPolicy() != BreakPolicy.NEVER)
						{
							// 3) Stack elements on the same columns as cheaply as possible, one by one until either the
							// majorSize is met or all elements are on a single column
							int minH;
							int minHIndex;
							do
							{
								minH = -1;
								minHIndex = -1;
								for(int i = 1; i < children.length; i++)
								{
									if(onCol[i])
										continue; // Already on the next row as its parent.
									Rectangle c1 = vl.getConstraint(i - 1);
									Rectangle c2 = vl.getConstraint(i);
									int h2 = c1.y + c1.height + c2.height;
									if(minH < 0 || h2 < minH)
									{
										minH = h2;
										minHIndex = i;
									}
								}
								if(minHIndex >= 0)
								{
									onCol[minHIndex] = true;
									while(vl.getConstraintCount() > minHIndex)
										vl.removeConstraint(vl.getConstraintCount() - 1);
									int type = inverse[minHIndex] ? -1 : 0;
									// Try to place the child on the same row as its predecessor
									placeChild(children, minHIndex, null, childSizers[minHIndex], iter, 0, type,
										!onCol[minHIndex]);
									for(int j = minHIndex + 1; j < children.length; j++)
									{
										type = inverse[j] ? -1 : 0;
										placeChild(children, j, null, childSizers[j], iter, 0, type, !onCol[j]);
									}
								}
							} while(minHIndex >= 0 && vl.getUsedWidth() > majorSize);
						}
						if(vl.getUsedWidth() > majorSize)
							// 4) Shrink the inverse-dependent elements that are too long down to the maximum of their
							// min length and the majorSize
							for(int i = 0; i < children.length; i++)
							{
								if(!inverse[i])
									continue;
								Rectangle c = vl.getConstraint(i);
								if(c.x + c.width <= majorSize)
									continue;
								SizePolicy minSizer = getChildSizer(children[i], true, -1);
								if(minSizer.getMin() >= c.width)
									continue;
								while(vl.getConstraintCount() > i)
									vl.removeConstraint(vl.getConstraintCount() - 1);
								placeChild(children, i, minSizer, null, iter, minSizer.getMin(), 0, onCol[i]);
								for(int j = i + 1; j < children.length; j++)
								{
									int type = inverse[j] ? -1 : 0;
									placeChild(children, j, null, childSizers[j], iter, 0, type, !onCol[j]);
								}
							}
					}
					int diff = majorSize - vl.getUsedWidth();
					if(diff > 0)
					{
						// Redistribute the extra size to inverse-dependent elements based on their stretch factor
						// TODO
					}
				}
			}
			ret.setMin(vl.getUsedWidth());
			vl.clearConstraints();
		}
		return ret;
	}

	@Override
	public void layout(MuisElement parent, MuisElement [] children)
	{
		// TODO Auto-generated method stub

	}

	private void placeChild(MuisElement [] children, int index, SizePolicy majorSizer, SizePolicy minorSizer,
		VirtualLayout.LayoutHoleIterator iter, int prefMajSize, int prefMinSize, boolean alongAxis)
	{
		/* Note: If either prefMajSize or prefMinSize is -1, use the other one to get the first sizer that the second
		 * sizer will be based on.  If neither are -1 but one is 0, use the other one as well.  If neither are -1 or 0,
		 * use alongAxis : major : minor sizer first. */
		iter.reset();
		// TODO
	}
}
