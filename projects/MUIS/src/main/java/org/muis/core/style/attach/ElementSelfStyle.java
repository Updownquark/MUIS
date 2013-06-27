package org.muis.core.style.attach;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.MuisTemplate;
import org.muis.core.mgr.MuisState;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleAttributeEvent;
import org.muis.core.style.StyleListener;
import org.muis.core.style.sheet.FilteredStyleSheet;
import org.muis.core.style.stateful.AbstractInternallyStatefulStyle;
import org.muis.core.style.stateful.MutableStatefulStyle;
import org.muis.core.style.stateful.StateExpression;

/** Represents a style set that applies only to a particular element and not to its descendants */
public class ElementSelfStyle extends AbstractInternallyStatefulStyle implements MutableStatefulStyle {
	private final ElementStyle theElStyle;

	private FilteredStyleSheet<?> theStyleSheet;

	private java.util.List<MuisAttribute<MuisTemplate.AttachPoint>> theTemplateRoles;

	/** @param elStyle The element style that this self style is for */
	public ElementSelfStyle(ElementStyle elStyle) {
		theElStyle = elStyle;
		theTemplateRoles = new java.util.ArrayList<>();
		addDependency(elStyle);
		theElStyle.getElement().life().runWhen(new Runnable() {
			@Override
			public void run() {
				theStyleSheet = new FilteredStyleSheet<>(theElStyle.getElement().getDocument().getStyle(), null, theElStyle.getElement()
					.getClass(), theTemplateRoles);
				addDependency(theStyleSheet);
				// Add a dependency for typed, non-grouped style sheet attributes
				addListener(new StyleListener() {
					@Override
					public void eventOccurred(StyleAttributeEvent<?> event) {
						theElStyle.getElement().fireEvent(event, false, false);
					}
				});
			}
		}, org.muis.core.MuisConstants.CoreStage.INIT_SELF.toString(), 1);
		elStyle.getElement().addListener(org.muis.core.MuisConstants.Events.ATTRIBUTE_ACCEPTED,
			new org.muis.core.event.MuisEventListener<MuisAttribute<?>>() {
				@Override
				public void eventOccurred(org.muis.core.event.MuisEvent<MuisAttribute<?>> event, MuisElement element) {
					if(event.getValue().getType() instanceof MuisTemplate.TemplateStructure.RoleAttributeType)
						theTemplateRoles.add((MuisAttribute<MuisTemplate.AttachPoint>) event.getValue());
				}

				@Override
				public boolean isLocal() {
					return true;
				}
			});
	}

	/** @return The element style that depends on this self-style */
	public ElementStyle getElementStyle() {
		return theElStyle;
	}

	/* Overridden to enable access by ElementStyle */
	@Override
	protected void setState(MuisState... newState) {
		super.setState(newState);
	}

	/* Overridden to enable access by ElementStyle */
	@Override
	protected void addState(MuisState state) {
		super.addState(state);
	}

	/* Overridden to enable access by ElementStyle */
	@Override
	protected void removeState(MuisState state) {
		super.removeState(state);
	}

	@Override
	public <T> void set(StyleAttribute<T> attr, T value) throws ClassCastException, IllegalArgumentException {
		super.set(attr, value);
	}

	@Override
	public <T> void set(StyleAttribute<T> attr, StateExpression exp, T value) throws ClassCastException, IllegalArgumentException {
		super.set(attr, exp, value);
	}

	@Override
	public void clear(StyleAttribute<?> attr) {
		super.clear(attr);
	}

	@Override
	public void clear(StyleAttribute<?> attr, StateExpression exp) {
		super.clear(attr, exp);
	}
}
