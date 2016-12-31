package org.quick.base.style;

import java.time.Duration;

import org.quick.core.prop.QuickProperty;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleDomain;

/** Styles relating specifically to buttons */
public class ButtonStyle implements StyleDomain {
	private StyleAttribute<?>[] theAttributes;

	private ButtonStyle() {
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr) {
		theAttributes = org.qommons.ArrayUtils.add(theAttributes, attr);
	}

	private static final ButtonStyle instance;

	/** The number of pixels that a "click" can move between its start and end and still be an actionable event */
	public static final StyleAttribute<Double> clickTolerance;
	/** The amount of time after a button press action before which repeated actions will begin */
	public static final StyleAttribute<Duration> actionRepeatDelay;
	/** The amount of time between repeated actions during a prolonged button press action */
	public static final StyleAttribute<Duration> actionRepeatFrequency;

	static {
		instance = new ButtonStyle();
		clickTolerance = StyleAttribute.build(instance, "click-tolerance", QuickPropertyType.floating, 5d)
			.validate(new QuickProperty.ComparableValidator<>(0d, Double.MAX_VALUE)).build();
		actionRepeatDelay = StyleAttribute.build(instance, "action-repeat-delay", QuickPropertyType.duration, Duration.ofMillis(500))
			.build();
		actionRepeatFrequency = StyleAttribute
			.build(instance, "action-repeat-frequency", QuickPropertyType.duration, Duration.ofMillis(100)).build();
		instance.register(clickTolerance);
	}

	/** @return The style domain for all button styles */
	public static ButtonStyle getDomainInstance() {
		return instance;
	}

	@Override
	public String getName() {
		return "bg";
	}

	@Override
	public java.util.Iterator<StyleAttribute<?>> iterator() {
		return org.qommons.IterableUtils.iterator(theAttributes, true);
	}
}
