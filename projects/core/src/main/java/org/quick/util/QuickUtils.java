package org.quick.util;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextAttribute;
import java.net.URL;
import java.util.*;

import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.qommons.ArrayUtils;
import org.quick.core.QuickElement;
import org.quick.core.QuickException;
import org.quick.core.event.UserEvent;
import org.quick.core.style.BackgroundStyle;
import org.quick.core.style.FontStyle;
import org.quick.core.style.QuickStyle;
import org.quick.core.tags.State;
import org.quick.core.tags.StateSupport;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

/** A set of utilities to use with core Quick elements */
public class QuickUtils {
	private QuickUtils() {
	}

	/**
	 * @param element The element to get the most distant ancestor of
	 * @return The most distant ancestor of the given element
	 */
	public static QuickElement getRoot(QuickElement element) {
		while(element.getParent() != null)
			element = element.getParent();
		return element;
	}

	/**
	 * @param element The element to get the path of
	 * @return The path from the root to the given element
	 */
	public static QuickElement [] path(QuickElement element) {
		ArrayList<QuickElement> ret = new ArrayList<>();
		while(element != null) {
			ret.add(element);
			element = element.getParent();
		}
		return ArrayUtils.reverse(ret.toArray(new QuickElement[ret.size()]));
	}

	/**
	 * @param element The element to get the document depth of
	 * @return The number of ancestors the given element has
	 */
	public static int getDepth(QuickElement element) {
		int ret = 0;
		while(element.getParent() != null) {
			ret++;
			element = element.getParent();
		}
		return ret;
	}

	/**
	 * @param ancestor The ancestor element to check
	 * @param descendant The descendant element to check
	 * @return Whether ancestor is the same as or an ancestor of descendant
	 */
	public static boolean isAncestor(QuickElement ancestor, QuickElement descendant) {
		QuickElement parent = descendant;
		while(parent != null && parent != ancestor)
			parent = parent.getParent();
		return parent == ancestor;
	}

	/**
	 * @param el1 The first element
	 * @param el2 The second element
	 * @return The deepest element that is an ancestor of both arguments, or null if the two elements are not in the same document tree
	 */
	public static QuickElement commonAncestor(QuickElement el1, QuickElement el2) {
		QuickElement [] path1 = path(el1);
		QuickElement test = el2;
		while(test != null && !ArrayUtils.contains(path1, test))
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
	public static QuickElement [] getBranchPoint(QuickElement el1, QuickElement el2) {
		QuickElement [] path1 = path(el1);
		QuickElement [] path2 = path(el2);
		if(path1.length == 0 || path2.length == 0 || path1[0] != path2[0])
			return null;
		int i;
		for(i = 0; i < path1.length && i < path2.length && path1[i] == path2[i]; i++);
		QuickElement branch1 = i < path1.length ? path1[i] : null;
		QuickElement branch2 = i < path2.length ? path2[i] : null;
		return new QuickElement[] {path1[i - 1], branch1, branch2};
	}

	/**
	 * Translates a rectangle from one element's coordinates into another's
	 *
	 * @param area The area to translate--not modified
	 * @param el1 The element whose coordinates <code>area</code> is in. May be null if the area is in the document root's coordinate system
	 * @param el2 The element whose coordinates to translate <code>area</code> to
	 * @return <code>area</code> translated from <code>el1</code>'s coordinates to <code>el2</code>'s
	 */
	public static Rectangle relative(Rectangle area, QuickElement el1, QuickElement el2) {
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
	public static Point relative(Point point, QuickElement el1, QuickElement el2) {
		if(el1 == null)
			el1 = getRoot(el2);
		Point ret = new Point(point);
		QuickElement common = commonAncestor(el1, el2);
		if(common == null)
			return null;
		QuickElement parent = el2;
		while(parent != common) {
			ret.x -= parent.bounds().getX();
			ret.y -= parent.bounds().getY();
			parent = parent.getParent();
		}
		parent = el1;
		while(parent != common) {
			ret.x += parent.bounds().getX();
			ret.y += parent.bounds().getY();
			parent = parent.getParent();
		}
		return ret;
	}

	/**
	 * @param element The element to get the position of
	 * @return The position of the top-left corner of the element relative to the document root
	 */
	public static Point getDocumentPosition(QuickElement element) {
		int x = 0;
		int y = 0;
		QuickElement el = element;
		while(el.getParent() != null) {
			x += el.bounds().getX();
			y += el.bounds().getY();
			el = el.getParent();
		}
		return new Point(x, y);
	}

	/**
	 * @param reference The URL to be the reference of the relative path
	 * @param relativePath The path relative to the reference URL to resolve
	 * @return A URL that is equivalent to <code>relativePath</code> resolved with reference to <code>reference</code>
	 * @throws QuickException If the given path is not either an absolute URL or a relative path
	 */
	public static URL resolveURL(URL reference, final String relativePath) throws QuickException {
		java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*:/");
		java.util.regex.Matcher matcher = urlPattern.matcher(relativePath);
		if(matcher.find() && matcher.start() == 0)
			try {
				return new URL(relativePath);
			} catch(java.net.MalformedURLException e) {
				throw new QuickException("Malformed URL " + relativePath, e);
			}
		String file = reference.getFile();
		int slashIdx = file.lastIndexOf('/');
		if(slashIdx >= 0)
			file = file.substring(0, slashIdx);
		String [] cp = relativePath.split("[/\\\\]");
		while(cp.length > 0 && cp[0].equals("..")) {
			slashIdx = file.lastIndexOf('/');
			if(slashIdx < 0)
				throw new QuickException("Cannot resolve " + relativePath + " with respect to " + reference);
			file = file.substring(0, slashIdx);
			cp = ArrayUtils.remove(cp, 0);
		}
		for(String cps : cp)
			file += "/" + cps;
		try {
			return new URL(reference.getProtocol(), reference.getHost(), file);
		} catch(java.net.MalformedURLException e) {
			throw new QuickException("Cannot resolve \"" + file + "\"", e);
		}
	}

	/**
	 * Derives a partially transparent color
	 *
	 * @param base The base color
	 * @param transparency The transparency of the color to make
	 * @return The resulting partially transparent color
	 */
	public static Color getColor(Color base, double transparency) {
		if(transparency == 0)
			return base;
		else if(transparency == 1)
			return org.quick.core.style.Colors.transparent;
		else if(transparency < 0 || transparency > 1)
			throw new IllegalArgumentException("Illegal transparency value " + transparency + ". Must be between 0 and 1.");
		return new Color((base.getRGB() & 0xffffff) | ((int) ((1 - transparency) * 256)) << 24, true);
	}

	/**
	 * Gets the (potentially partially transparent) background color for a style
	 *
	 * @param style The style to get the background color for
	 * @return The background color to paint for the style
	 */
	public static ObservableValue<Color> getBackground(QuickStyle style) {
		return style.get(BackgroundStyle.color).combineV(QuickUtils::getColor, style.get(BackgroundStyle.transparency));
	}

	/**
	 * Gets the (potentially partially transparent) font color for a style
	 *
	 * @param style The style to get the font color for
	 * @return The font color to paint for the style
	 */
	public static ObservableValue<Color> getFontColor(QuickStyle style) {
		return style.get(FontStyle.color).combineV(QuickUtils::getColor, style.get(FontStyle.transparency));
	}

	/**
	 * @param style The style to derive the font from
	 * @return The font to use to render text in the specified style
	 */
	public static ObservableValue<java.awt.Font> getFont(QuickStyle style) {
		java.util.Map<java.text.AttributedCharacterIterator.Attribute, Object> attribs = new java.util.HashMap<>();
		ObservableValue<String> family = style.get(FontStyle.family);
		ObservableValue<Color> color = getFontColor(style);
		ObservableValue<Boolean> kerning = style.get(FontStyle.kerning);
		ObservableValue<Boolean> ligs = style.get(FontStyle.ligatures);
		ObservableValue<FontStyle.Underline> underline = style.get(FontStyle.underline);
		ObservableValue<Double> weight = style.get(FontStyle.weight);
		ObservableValue<Boolean> strike = style.get(FontStyle.strike);
		ObservableValue<Double> slant = style.get(FontStyle.slant);
		return ObservableValue.assemble(TypeToken.of(java.awt.Font.class), () -> {
			attribs.put(TextAttribute.FAMILY, family.get());
			attribs.put(TextAttribute.BACKGROUND, org.quick.core.style.Colors.transparent);
			attribs.put(TextAttribute.FOREGROUND, color.get());
			if(kerning.get())
				attribs.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
			if(ligs.get())
				attribs.put(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
			attribs.put(TextAttribute.SIZE, style.get(FontStyle.size));
			switch (underline.get()) {
			case none:
				break;
			case on:
				attribs.put(TextAttribute.INPUT_METHOD_UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
				break;
			case heavy:
				attribs.put(TextAttribute.INPUT_METHOD_UNDERLINE, TextAttribute.UNDERLINE_LOW_TWO_PIXEL);
				break;
			case dashed:
				attribs.put(TextAttribute.INPUT_METHOD_UNDERLINE, TextAttribute.UNDERLINE_LOW_DASHED);
				break;
			case dotted:
				attribs.put(TextAttribute.INPUT_METHOD_UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
				break;
			}
			attribs.put(TextAttribute.WEIGHT, weight.get());
			attribs.put(TextAttribute.STRIKETHROUGH, strike.get());
			attribs.put(TextAttribute.POSTURE, slant.get());
			return java.awt.Font.getFont(attribs);
		}, family, color, kerning, ligs, underline, weight, strike, slant);
	}

	/**
	 * @param type The type of element
	 * @return All states supported by the given element type (as annotated by {@link StateSupport})
	 */
	public static org.quick.core.mgr.QuickState[] getStatesFor(Class<? extends QuickElement> type) {
		ArrayList<org.quick.core.mgr.QuickState> ret = new ArrayList<>();
		Class<?> type2 = type;
		while(QuickElement.class.isAssignableFrom(type2)) {
			StateSupport states = type2.getAnnotation(StateSupport.class);
			if(states != null)
				for(State state : states.value()) {
					try {
						ret.add(new org.quick.core.mgr.QuickState(state.name(), state.priority()));
					} catch(IllegalArgumentException e) {
					}
				}
			type2 = type2.getSuperclass();
		}
		return ret.toArray(new org.quick.core.mgr.QuickState[ret.size()]);
	}

	/**
	 * @param event The observable event
	 * @return The user event that ultimately caused the observable event (may be null)
	 */
	public static UserEvent getUserEvent(ObservableValueEvent<?> event) {
		while(event != null) {
			if(event.getCause() instanceof UserEvent)
				return (UserEvent) event.getCause();
			else if(event.getCause() instanceof ObservableValueEvent)
				event = (ObservableValueEvent<?>) event.getCause();
			else
				event = null;
		}
		return null;
	}

	/**
	 * @param field The name of the java field
	 * @return The XML-ified name
	 */
	public static String javaToXML(String field) {
		StringBuilder name = new StringBuilder();
		for (int c = 0; c < field.length(); c++) {
			if (Character.isUpperCase(field.charAt(c))) {
				if (name.length() > 0)
					name.append('-');
				name.append(Character.toLowerCase(field.charAt(c)));
			} else
				name.append(field.charAt(c));
		}
		return name.toString();
	}

	/**
	 * Constructs an immutable copy of a map of lists
	 *
	 * @param value The value to copy
	 * @return The copied, immutable value
	 */
	public static <K, V> Map<K, List<V>> immutable(Map<K, List<V>> value) {
		Map<K, List<V>> fns = new LinkedHashMap<>();
		for (Map.Entry<K, List<V>> entry : value.entrySet()) {
			fns.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
		}
		return Collections.unmodifiableMap(fns);
	}

	/**
	 * Checks whether a variable of type <code>left</code> may be assigned a value of type <code>right</code>. This is more lenient than
	 * {@link TypeToken#isAssignableFrom(TypeToken)} because this method allows for auto (un)boxing and conversion between compatible
	 * primitive types (e.g. float f=0).
	 * 
	 * @param left The type of the variable to assign
	 * @param right The type of the value being assigned
	 * @return Whether the assignment is allowable
	 */
	public static boolean isAssignableFrom(TypeToken<?> left, TypeToken<?> right) {
		if (left.isAssignableFrom(right) || left.wrap().isAssignableFrom(right)) // Auto boxing/unboxing
			return true;
		// Handle primitive conversions
		Class<?> primTypeLeft = left.unwrap().getRawType();
		if (!primTypeLeft.isPrimitive() || !TypeToken.of(Number.class).isAssignableFrom(right.wrap()))
			return false;
		Class<?> primTypeRight = right.unwrap().getRawType();
		if (primTypeLeft == Double.TYPE)
			return primTypeRight == Float.TYPE || primTypeRight == Long.TYPE || primTypeRight == Integer.TYPE || primTypeRight == Short.TYPE
				|| primTypeRight == Byte.TYPE;
		else if (primTypeLeft == Float.TYPE)
			return primTypeRight == Long.TYPE || primTypeRight == Integer.TYPE || primTypeRight == Short.TYPE || primTypeRight == Byte.TYPE;
		else if (primTypeLeft == Long.TYPE)
			return primTypeRight == Integer.TYPE || primTypeRight == Short.TYPE || primTypeRight == Byte.TYPE;
		else if (primTypeLeft == Integer.TYPE)
			return primTypeRight == Short.TYPE || primTypeRight == Byte.TYPE;
		else if (primTypeLeft == Short.TYPE)
			return primTypeRight == Byte.TYPE;
		return false;
	}

	/**
	 * @param type The type to make an array type of
	 * @return An array type whose component type is <code>type</code>
	 */
	public static <T> TypeToken<T[]> arrayTypeOf(TypeToken<T> type) {
		return new TypeToken<T[]>() {}.where(new TypeParameter<T>() {}, type);
	}

	/**
	 * For types that pass {@link #isAssignableFrom(TypeToken, TypeToken)}, this method converts the given value to the correct run-time
	 * type
	 *
	 * @param type The type to convert to
	 * @param value The value to convert
	 * @return The converted value
	 */
	public static <T> T convert(TypeToken<T> type, Object value) {
		if (type.isPrimitive() && value == null)
			throw new NullPointerException("null cannot be assigned to " + type);
		if (type.isAssignableFrom(value.getClass()) || type.wrap().isAssignableFrom(value.getClass()))
			return (T) value;
		Class<?> primType = type.unwrap().getRawType();
		if (!primType.isPrimitive() || !(value instanceof Number))
			throw new IllegalArgumentException(value.getClass() + " cannot be converted to " + type);
		if (primType == Double.TYPE)
			return (T) Double.valueOf(((Number) value).doubleValue());
		else if (primType == Float.TYPE)
			return (T) Float.valueOf(((Number) value).floatValue());
		else if (primType == Long.TYPE)
			return (T) Long.valueOf(((Number) value).longValue());
		else if (primType == Integer.TYPE)
			return (T) Integer.valueOf(((Number) value).intValue());
		else if (primType == Short.TYPE)
			return (T) Short.valueOf(((Number) value).shortValue());
		else if (primType == Byte.TYPE)
			return (T) Byte.valueOf(((Number) value).byteValue());
		else
			throw new IllegalArgumentException(value.getClass() + " cannot be converted to " + type);
	}
}
