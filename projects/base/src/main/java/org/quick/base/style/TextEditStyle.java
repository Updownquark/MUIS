package org.quick.base.style;

import java.time.Duration;

import org.quick.core.prop.QuickProperty;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.style.StyleAttribute;

/** Style attributes relevant to text editors */
public class TextEditStyle implements org.quick.core.style.StyleDomain {
	private StyleAttribute<?>[] theAttributes;

	private TextEditStyle() {
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr) {
		theAttributes = org.qommons.ArrayUtils.add(theAttributes, attr);
	}

	private static final TextEditStyle instance;

	/** The amount of time between cursor blinks, in milliseconds. 0 means always on, -1 means always off */
	public static final StyleAttribute<Duration> cursorBlink;

	static {
		instance = new TextEditStyle();
		cursorBlink = new StyleAttribute<>(instance, "cursor-blink", QuickPropertyType.duration, Duration.ofSeconds(1),
			new QuickProperty.ComparableValidator<>(Duration.ofMillis(-1), Duration.ofMinutes(1)));
		instance.register(cursorBlink);
	}

	/** @return The style domain for all text editor styles */
	public static TextEditStyle getDomainInstance() {
		return instance;
	}

	@Override
	public String getName() {
		return "text-edit-style";
	}

	@Override
	public java.util.Iterator<StyleAttribute<?>> iterator() {
		return org.qommons.IterableUtils.iterator(theAttributes, true);
	}
}
