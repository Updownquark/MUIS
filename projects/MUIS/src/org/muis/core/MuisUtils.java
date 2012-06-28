package org.muis.core;

import java.awt.Point;
import java.awt.Rectangle;
import java.net.URL;
import java.util.ArrayList;

/** A set of utilities to use with core MUIS elements */
public class MuisUtils
{
	private MuisUtils()
	{
	}

	/**
	 * @param element The element to get the most distant ancestor of
	 * @return The most distant ancestor of the given element
	 */
	public static MuisElement getRoot(MuisElement element)
	{
		while(element.getParent() != null)
			element = element.getParent();
		return element;
	}

	/**
	 * @param element The element to get the path of
	 * @return The path from the root to the given element
	 */
	public static MuisElement [] path(MuisElement element)
	{
		ArrayList<MuisElement> ret = new ArrayList<MuisElement>();
		while(element != null)
		{
			ret.add(element);
			element = element.getParent();
		}
		return ret.toArray(new MuisElement[ret.size()]);
	}

	/**
	 * @param ancestor The ancestor element to check
	 * @param descendant The descendant element to check
	 * @return Whether ancestor is the same as or an ancestor of descendant
	 */
	public static boolean isAncestor(MuisElement ancestor, MuisElement descendant)
	{
		MuisElement parent = descendant;
		while(parent != null && parent != ancestor)
			parent = parent.getParent();
		return parent == ancestor;
	}

	/**
	 * @param el1 The first element
	 * @param el2 The second element
	 * @return The deepest element that is an ancestor of both arguments, or null if the two elements are not in the same document tree
	 */
	public static MuisElement commonAncestor(MuisElement el1, MuisElement el2)
	{
		MuisElement [] path1 = path(el1);
		MuisElement test = el2;
		while(test != null && !prisms.util.ArrayUtils.contains(path1, test))
			test = test.getParent();
		return test;
	}

	/**
	 * Finds the point at which 2 elements branch from a common ancestor
	 *
	 * @param el1 The first element
	 * @param el2 The second element
	 * @return A 3-element array containing:
	 *         <ol>
	 *         <li>the common ancestor
	 *         <li>The ancestor's child that is an ancestor of <code>el1</code> or null if <code>el1</code> is the common ancestor</li>
	 *         <li>The ancestor's child that is an ancestor of <code>el2</code> or null if <code>el2</code> is the common ancestor</li>
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

	/**
	 * Translates a rectangle from one element's coordinates into another's
	 *
	 * @param area The area to translate--not modified
	 * @param el1 The element whose coordinates <code>area</code> is in. May be null if the area is in the document root's coordinate system
	 * @param el2 The element whose coordinates to translate <code>area</code> to
	 * @return <code>area</code> translated from <code>el1</code>'s coordinates to <code>el2</code>'s
	 */
	public static Rectangle relative(Rectangle area, MuisElement el1, MuisElement el2)
	{
		Point relP = relative(area.getLocation(), el1, el2);
		return new Rectangle(relP.x, relP.y, area.width, area.height);
	}

	/**
	 * Translates a point from one element's coordinates into another's
	 *
	 * @param point The point to translate--not modified
	 * @param el1 The element whose coordinates <code>point</code> is in. May be null if the point is in the document root's coordinate
	 *            system
	 * @param el2 The element whose coordinates to translate <code>point</code> to
	 * @return <code>point</code> translated from <code>el1</code>'s coordinates to <code>el2</code>'s
	 */
	public static Point relative(Point point, MuisElement el1, MuisElement el2)
	{
		if(el1 == null)
			el1 = getRoot(el2);
		Point ret = new Point(point);
		MuisElement common = commonAncestor(el1, el2);
		if(common == null)
			return null;
		MuisElement parent = el2;
		while(parent != common)
		{
			ret.x -= parent.getX();
			ret.y -= parent.getY();
			parent = parent.getParent();
		}
		parent = el1;
		while(parent != common)
		{
			ret.x += parent.getX();
			ret.y += parent.getY();
			parent = parent.getParent();
		}
		return ret;
	}

	/**
	 * Sinks into the element hierarchy by position using the cached bounds of the elements
	 *
	 * @param root The element hierarchy to fire the mouse event for
	 * @param x The x-position of the mouse event within the element
	 * @param y The y-position of the mouse event within the element
	 * @return The capture of each element in the hierarchy of root that the event occurred over
	 */
	public static MuisElementCapture captureEventTargets(MuisElement root, int x, int y)
	{
		return captureEventTargets(new MuisElementCapture(null, root, x, y), x, y);
	}

	private static MuisElementCapture captureEventTargets(MuisElementCapture root, int x, int y)
	{
		MuisElement [] children = prisms.util.ArrayUtils.reverse(MuisElement.sortByZ(root.element.getChildren()));
		for(MuisElement child : children)
		{
			Rectangle bounds = child.getCacheBounds();
			int relX = x - bounds.x;
			int relY = y - bounds.y;
			if(relX >= 0 && relY >= 0 && relX < bounds.width && relY < bounds.height)
			{
				MuisElementCapture childCapture = captureEventTargets(child, relX, relY);
				root.addChild(childCapture);
				boolean isClickThrough = true;
				for(MuisElementCapture mec : childCapture)
					if(!mec.element.isClickThrough())
					{
						isClickThrough = false;
						break;
					}
				if(!isClickThrough)
					break;
			}
		}
		return root;
	}

	/**
	 * @param reference The URL to be the reference of the relative path
	 * @param relativePath The path relative to the reference URL to resolve
	 * @return A URL that is equivalent to <code>relativePath</code> resolved with reference to <code>reference</code>
	 * @throws MuisException If the given path is not either an absolute URL or a relative path
	 */
	public static URL resolveURL(URL reference, final String relativePath) throws MuisException
	{
		java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*:/");
		java.util.regex.Matcher matcher = urlPattern.matcher(relativePath);
		if(matcher.find() && matcher.start() == 0)
			try
			{
				return new URL(relativePath);
			} catch(java.net.MalformedURLException e)
			{
				throw new MuisException("Malformed URL " + relativePath, e);
			}
		String file = reference.getFile();
		int slashIdx = file.lastIndexOf('/');
		if(slashIdx >= 0)
			file = file.substring(0, slashIdx);
		String [] cp = relativePath.split("[/\\\\]");
		while(cp.length > 0 && cp[0].equals(".."))
		{
			slashIdx = file.lastIndexOf('/');
			if(slashIdx < 0)
				throw new MuisException("Cannot resolve " + relativePath + " with respect to " + reference);
			file = file.substring(0, slashIdx);
			cp = prisms.util.ArrayUtils.remove(cp, 0);
		}
		for(String cps : cp)
			file += "/" + cps;
		try
		{
			return new URL(reference.getProtocol(), reference.getHost(), file);
		} catch(java.net.MalformedURLException e)
		{
			throw new MuisException("Cannot resolve \"" + file + "\"", e);
		}
	}

	public static java.awt.Font getFont(org.muis.core.style.MuisStyle style)
	{
		java.awt.Font.getFont(null)
	}
}
