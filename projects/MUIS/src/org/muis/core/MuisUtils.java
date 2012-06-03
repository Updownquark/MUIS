package org.muis.core;

/**
 * A set of utilities to use with core MUIS components
 */
public class MuisUtils
{
	private MuisUtils()
	{
	}

	/**
	 * @param element The element to get the path of
	 * @return The path from the root to the given element
	 */
	public static MuisElement [] path(MuisElement element)
	{
		java.util.ArrayList<MuisElement> ret = new java.util.ArrayList<MuisElement>();
		while(element != null)
		{
			ret.add(element);
			element = element.getParent();
		}
		return ret.toArray(new MuisElement[ret.size()]);
	}

	/**
	 * @param el1 The first element
	 * @param el2 The second element
	 * @return The deepest element that is an ancestor of both arguments, or null if the two
	 *         elements are not in the same document tree
	 */
	public static MuisElement commonAncestor(MuisElement el1, MuisElement el2)
	{
		MuisElement [] path1 = path(el1);
		MuisElement [] path2 = path(el2);
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
	public static MuisElement [] getBranchPoint(MuisElement el1, MuisElement el2)
	{
		MuisElement [] path1 = path(el1);
		MuisElement [] path2 = path(el2);
		if(path1.length == 0 || path2.length == 0 || path1[0] != path2[0])
			return null;
		int i;
		for(i = 0; i < path1.length && i < path2.length && path1[i] == path2[i]; i++);
		MuisElement branch1 = i < path1.length ? path1[i] : null;
		MuisElement branch2 = i < path2.length ? path2[i] : null;
		return new MuisElement[] {path1[i - 1], branch1, branch2};
	}
}
