package org.wam.core;

/**
 * A set of utilities to use with core WAM components
 */
public class WamUtils
{
	private WamUtils()
	{
	}

	/**
	 * @param element The element to get the path of
	 * @return The path from the root to the given element
	 */
	public static WamElement [] path(WamElement element)
	{
		java.util.ArrayList<WamElement> ret = new java.util.ArrayList<WamElement>();
		while(element != null)
		{
			ret.add(element);
			element = element.getParent();
		}
		return ret.toArray(new WamElement[ret.size()]);
	}

	/**
	 * @param el1 The first element
	 * @param el2 The second element
	 * @return The deepest element that is an ancestor of both arguments, or null if the two
	 *         elements are not in the same document tree
	 */
	public static WamElement commonAncestor(WamElement el1, WamElement el2)
	{
		WamElement [] path1 = path(el1);
		WamElement [] path2 = path(el2);
		if(path1.length == 0 || path2.length == 0 || path1[0] != path2[0])
			return null;
		int i;
		for(i = 0; i < path1.length && i < path2.length && path1[i] == path2[i]; i++);
		return path1[i - 1];
	}

	/**
	 * Finds the point at which 2 elements branch from a common ancestor
	 * 
	 * @param el1 The first element
	 * @param el2 The second element
	 * @return A 3-element array containing:
	 *         <ol>
	 *         <li>the common ancestor
	 *         <li>The ancestor's child that is an ancestor of <code>el1</code> or null if
	 *         <code>el1</code> is the common ancestor</li>
	 *         <li>The ancestor's child that is an ancestor of <code>el2</code> or null if
	 *         <code>el2</code> is the common ancestor</li>
	 *         </ol>
	 *         or null if there is no common ancestor between the two elements
	 */
	public static WamElement [] getBranchPoint(WamElement el1, WamElement el2)
	{
		WamElement [] path1 = path(el1);
		WamElement [] path2 = path(el2);
		if(path1.length == 0 || path2.length == 0 || path1[0] != path2[0])
			return null;
		int i;
		for(i = 0; i < path1.length && i < path2.length && path1[i] == path2[i]; i++);
		WamElement branch1 = i < path1.length ? path1[i] : null;
		WamElement branch2 = i < path2.length ? path2[i] : null;
		return new WamElement[] {path1[i - 1], branch1, branch2};
	}
}
