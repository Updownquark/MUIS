package org.muis.core.style;

import java.awt.Cursor;

import org.muis.core.MuisException;
import org.muis.core.MuisParseEnv;
import org.muis.core.MuisProperty;
import org.muis.core.rx.ObservableValue;

import prisms.lang.Type;

/**
 * Contains style attributes pertaining to the background of a widget. These styles are supported by {@link org.muis.core.MuisElement} and
 * all of its subclasses unless the {@link org.muis.core.MuisElement#paintSelf(java.awt.Graphics2D, java.awt.Rectangle)} method is
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
	public static MuisProperty.PropertyType<Cursor> CURSOR_PROPERTY_TYPE = new MuisProperty.AbstractPropertyType<Cursor>() {
		@Override
		public Type getType() {
			return new Type(Cursor.class);
		}

		@Override
		public ObservableValue<? extends Cursor> parse(MuisParseEnv env, String parseValue) throws MuisException {
			ObservableValue<?> modelValue = MuisProperty.parseExplicitObservable(env, parseValue, false);
			if(modelValue != null) {
				if(modelValue.getType().canAssignTo(Cursor.class))
					return (ObservableValue<? extends Cursor>) modelValue;
				else if(modelValue.getType().canAssignTo(String.class)) {
					return ((ObservableValue<String>) modelValue).mapV(str -> {
						try {
							return parseCursor(str);
						} catch(Exception e) {
							throw new IllegalStateException(e);
						}
					});
				} else
					throw new MuisException("Model value " + parseValue + " is not of type cursor");
			}
			return ObservableValue.constant(parseCursor(parseValue));
		}

		private Cursor parseCursor(String text) throws MuisException {
			for(PreDefinedCursor preDef : PreDefinedCursor.values()) {
				if(preDef.display.equals(text))
					return Cursor.getPredefinedCursor(preDef.type);
			}
			// TODO Support custom cursors
			throw new MuisException("Custom cursors are not supported yet");
		}

		@Override
		public Cursor cast(Object obj) {
			if(obj instanceof Cursor)
				return (Cursor) obj;
			else
				return null;
		}
	};

	private StyleAttribute<?> [] theAttributes;

	private BackgroundStyle() {
		theAttributes = new StyleAttribute[0];
	}

	private void register(StyleAttribute<?> attr) {
		theAttributes = prisms.util.ArrayUtils.add(theAttributes, attr);
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
		texture = new StyleAttribute<>(instance, "texture", new MuisProperty.MuisTypeInstanceProperty<>(Texture.class), new BaseTexture());
		instance.register(texture);
		color = new StyleAttribute<>(instance, "color", MuisProperty.colorAttr, new java.awt.Color(255, 255, 255));
		instance.register(color);
		transparency = new StyleAttribute<>(instance, "transparency", MuisProperty.floatAttr, 0d, new MuisProperty.ComparableValidator<>(
			0d, 1d));
		instance.register(transparency);
		cornerRadius = new StyleAttribute<>(instance, "corner-radius", SizePropertyType.instance, new Size(),
			new MuisProperty.ComparableValidator<>(new Size(), null));
		instance.register(cornerRadius);
		cursor = new StyleAttribute<>(instance, "cursor", CURSOR_PROPERTY_TYPE, Cursor.getDefaultCursor());
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
		return prisms.util.ArrayUtils.iterator(theAttributes, true);
	}
}
