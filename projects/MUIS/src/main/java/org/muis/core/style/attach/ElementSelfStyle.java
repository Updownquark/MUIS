package org.muis.core.style.attach;

import org.muis.core.mgr.MuisState;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleAttributeEvent;
import org.muis.core.style.sheet.FilteredStyleSheet;
import org.muis.core.style.sheet.TemplateRole;
import org.muis.core.style.stateful.AbstractInternallyStatefulStyle;
import org.muis.core.style.stateful.MutableStatefulStyle;
import org.muis.core.style.stateful.StateExpression;

import prisms.lang.Type;

/** Represents a style set that applies only to a particular element and not to its descendants */
public class ElementSelfStyle extends AbstractInternallyStatefulStyle implements MutableStatefulStyle {
	private final ElementStyle theElStyle;

	private FilteredStyleSheet<?> theStyleSheet;

	/** @param elStyle The element style that this self style is for */
	public ElementSelfStyle(ElementStyle elStyle) {
		theElStyle = elStyle;
		addDependency(elStyle);
		theElStyle
			.getElement()
			.life()
			.runWhen(
				() -> {
					org.muis.core.rx.DefaultObservableSet<TemplateRole> templateRoles = new org.muis.core.rx.DefaultObservableSet<>(
						new Type(TemplateRole.class));;
					java.util.Set<TemplateRole> controller = templateRoles.control(null);
					theStyleSheet = new FilteredStyleSheet<>(theElStyle.getElement().getDocument().getStyle(), null, theElStyle
						.getElement().getClass(), templateRoles);
					// Add listener to modify the filtered style sheet's template path
					TemplatePathListener tpl = new TemplatePathListener();
					tpl.addListener(new TemplatePathListener.Listener() {
						@Override
						public void pathAdded(TemplateRole path) {
							controller.add(path);
						}

						@Override
						public void pathRemoved(TemplateRole path) {
							controller.remove(path);
						}

						@Override
						public void pathChanged(TemplateRole oldPath, TemplateRole newPath) {
							controller.remove(oldPath);
							controller.add(newPath);
						}
					});
					tpl.listen(theElStyle.getElement());
					addDependency(theStyleSheet);
					// Add a dependency for typed, non-grouped style sheet attributes
					allChanges().act(
						(StyleAttributeEvent<?> event) -> {
							StyleAttributeEvent<Object> evt = (StyleAttributeEvent<Object>) event;
							evt = new StyleAttributeEvent<>(theElStyle.getElement(), evt.getRootStyle(), evt.getLocalStyle(), evt
								.getAttribute(), evt.getValue());
							theElStyle.getElement().events().fire(evt);
						});
				}, org.muis.core.MuisConstants.CoreStage.INIT_SELF.toString(), 1);
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
