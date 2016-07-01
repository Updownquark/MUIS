package org.quick.core.style;

import java.awt.Cursor;

import org.observe.ObservableValue;
import org.qommons.IterableUtils;
import org.quick.core.layout.LayoutAttributes;
import org.quick.core.prop.QuickProperty;
import org.quick.core.prop.QuickPropertyType;

import com.google.common.reflect.TypeToken;

/**
 * Contains style attributes pertaining to the background of a widget. These styles are supported by {@link org.quick.core.QuickElement} and
 * all of its subclasses unless the {@link org.quick.core.QuickElement#paintSelf(java.awt.Graphics2D, java.awt.Rectangle)} method is
 * overridden in a way that ignores them.
 */
public class BackgroundStyle implements StyleDomain {
	/** Pre-defined cursor types */
	public static enum PreDefinedCursor {
		/** The default cursor */
		def("default", Cursor.DEFAULT_CURSOR),
		/** The wait cursor */
		wait("wait", Cursor.WAIT_CURSOR),
		/** The hand cursor */
		hand("hand", Cursor.HAND_CURSOR),
		/** The move cursor */
		move("move", Cursor.MOVE_CURSOR),
		/** The crosshair cursor */
		crosshair("crosshair", Cursor.CROSSHAIR_CURSOR),
		/** The text cursor */
		text("text", Cursor.TEXT_CURSOR),
		/** The north-resize cursor */
		northResize("n-resize", Cursor.N_RESIZE_CURSOR),
		/** The east-resize cursor */
		eastResize("e-resize", Cursor.E_RESIZE_CURSOR),
		/** The south-resize cursor */
		southResize("s-resize", Cursor.S_RESIZE_CURSOR),
		/** The west-resize cursor */
		westResize("w-resize", Cursor.W_RESIZE_CURSOR),
		/** The north-east-resize cursor */
		northEastResize("ne-resize", Cursor.NE_RESIZE_CURSOR),
		/** The south-east-resize cursor */
		southEastResize("se-resize", Cursor.SE_RESIZE_CURSOR),
		/** The south-west-resize cursor */
		southWestResize("sw-resize", Cursor.SW_RESIZE_CURSOR),
		/** The north-west-resize cursor */
		northWestResize("nw-resize", Cursor.NW_RESIZE_CURSOR);

		/** The value to display for this cursor and the value to parse to this cursor */
		public final String display;

		/** The cursor type to pass to {@link Cursor#getPredefinedCursor(int)} */
		public final int type;

		private PreDefinedCursor(String disp, int typ) {
			display = disp;
			type = typ;
		}
	}

	/** The property type for cursor properties */
	public static QuickPropertyType<Cursor> CURSOR_PROPERTY_TYPE = QuickPropertyType.build("cursor", TypeToken.of(Cursor.class))//
		.buildContext(ctx -> {
			for (PreDefinedCursor preDef : PreDefinedCursor.values())
				ctx.withValue(preDef.display,
					ObservableValue.constant(TypeToken.of(Cursor.class), Cursor.getPredefinedCursor(preDef.type)));
		})//
		.build();

	private StyleAttribute<?>[] theAttributes;

	private BackgroundStyle() {
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr) {
		theAttributes = org.qommons.ArrayUtils.add(theAttributes, attr);
	}

	private static final BackgroundStyle instance;

	/** The texture to render an element's background with */
	public static final StyleAttribute<? extends Texture> texture;

	/** The color of a widget's background */
	public static final StyleAttribute<java.awt.Color> color;

	/** The transparency of a widget's background. Does not apply to the entire widget. */
	public static final StyleAttribute<Double> transparency;

	/** The radius of widget corners */
	public static final StyleAttribute<Size> cornerRadius;

	/** The cursor for the mouse to appear as when hovering over a widget */
	public static final StyleAttribute<Cursor> cursor;

	static {
		instance = new BackgroundStyle();
		texture = StyleAttribute.build(instance, "texture", QuickPropertyType.forTypeInstance(Texture.class), new BaseTexture()).build();
		instance.register(texture);
		color = StyleAttribute.build(instance, "color", QuickPropertyType.color, new java.awt.Color(255, 255, 255)).build();
		instance.register(color);
		transparency = StyleAttribute.build(instance, "transparency", QuickPropertyType.floating, 0d)
			.validate(new QuickProperty.ComparableValidator<>(0d, 1d)).build();
		instance.register(transparency);
		cornerRadius = StyleAttribute.build(instance, "corner-radius", LayoutAttributes.sizeType, new Size())
			.validate(new QuickProperty.ComparableValidator<>(new Size(), null)).build();
		instance.register(cornerRadius);
		cursor = StyleAttribute.build(instance, "cursor", CURSOR_PROPERTY_TYPE, Cursor.getDefaultCursor()).build();
		instance.register(cursor);
	}

	/** @return The style domain for all background styles */
	public static BackgroundStyle getDomainInstance() {
		return instance;
	}

	@Override
	public String getName() {
		return "bg";
	}

	@Override
	public java.util.Iterator<StyleAttribute<?>> iterator() {
		return IterableUtils.iterator(theAttributes, true);
	}
}
