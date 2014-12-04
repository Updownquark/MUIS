package org.muis.core.style.attach;

import java.util.List;

import org.muis.core.rx.DefaultObservableList;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleAttributeEvent;
import org.muis.core.style.sheet.FilteredStyleSheet;
import org.muis.core.style.sheet.TemplateRole;
import org.muis.core.style.stateful.AbstractInternallyStatefulStyle;
import org.muis.core.style.stateful.MutableStatefulStyle;
import org.muis.core.style.stateful.StateExpression;
import org.muis.core.style.stateful.StatefulStyle;

import prisms.lang.Type;

/** Represents a style set that applies only to a particular element and not to its descendants */
public class ElementSelfStyle extends AbstractInternallyStatefulStyle implements MutableStatefulStyle, org.muis.core.style.MuisStyle {
	private final ElementStyle theElStyle;

	private FilteredStyleSheet<?> theStyleSheet;

	private List<StatefulStyle> theDependencyController;

	/** @param elStyle The element style that this self style is for */
	public ElementSelfStyle(ElementStyle elStyle) {
		super(new DefaultObservableList<>(new Type(StatefulStyle.class)), elStyle.getElement().state().activeStates());
		theDependencyController = ((DefaultObservableList<StatefulStyle>) getConditionalDependencies()).control(null);
		theElStyle = elStyle;
		theDependencyController.add(elStyle);
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
					theDependencyController.add(theStyleSheet);
					// Add a dependency for typed, non-grouped style sheet attributes
					allChanges().act(event -> theElStyle.getElement().events().fire(event));
				}, org.muis.core.MuisConstants.CoreStage.INIT_SELF.toString(), 1);
	}

	/** @return The element style that depends on this self-style */
	public ElementStyle getElementStyle() {
		return theElStyle;
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

	@Override
	public <T> StyleAttributeEvent<T> mapEvent(StyleAttribute<T> attr, org.muis.core.rx.ObservableValueEvent<T> event) {
		StyleAttributeEvent<T> superMap = org.muis.core.style.MuisStyle.super.mapEvent(attr, event);
		return new StyleAttributeEvent<>(theElStyle.getElement(), superMap.getRootStyle(), this, attr, superMap.getOldValue(),
			superMap.getValue(), superMap.getCause());
	}

	@Override
	public String toString() {
		return "style.self of " + theElStyle.getElement();
	}
}
