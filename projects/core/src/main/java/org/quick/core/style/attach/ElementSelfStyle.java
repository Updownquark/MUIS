package org.quick.core.style.attach;

import java.util.List;

import org.observe.ObservableValue;
import org.observe.collect.impl.ObservableArrayList;
import org.observe.util.ObservableUtils;
import org.quick.core.QuickElement;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.sheet.FilteredStyleSheet;
import org.quick.core.style.sheet.TemplateRole;
import org.quick.core.style.stateful.AbstractInternallyStatefulStyle;
import org.quick.core.style.stateful.MutableStatefulStyle;
import org.quick.core.style.stateful.StateExpression;
import org.quick.core.style.stateful.StatefulStyle;

import com.google.common.reflect.TypeToken;

/** Represents a style set that applies only to a particular element and not to its descendants */
public class ElementSelfStyle extends AbstractInternallyStatefulStyle implements MutableStatefulStyle, org.quick.core.style.QuickStyle {
	private final ElementStyle theElStyle;

	private FilteredStyleSheet<?> theStyleSheet;

	private List<StatefulStyle> theDependencyController;

	/** @param elStyle The element style that this self style is for */
	public ElementSelfStyle(ElementStyle elStyle) {
		super(elStyle.getElement().msg(), ObservableUtils.control(new ObservableArrayList<>(TypeToken.of(StatefulStyle.class))),
			elStyle.getElement().state().activeStates());
		theDependencyController = ObservableUtils.getController(getConditionalDependencies());
		theElStyle = elStyle;
		theDependencyController.add(elStyle);
		theElStyle
			.getElement()
			.life()
			.runWhen(
				() -> {
					org.observe.collect.impl.ObservableHashSet<TemplateRole> templateRoles = new org.observe.collect.impl.ObservableHashSet<>(
						TypeToken.of(TemplateRole.class));
					theStyleSheet = new FilteredStyleSheet<>(theElStyle.getElement().getDocument().getStyle(), null, theElStyle
						.getElement().getClass(), templateRoles.immutable());
					// Add listener to modify the filtered style sheet's template path
					TemplatePathListener tpl = new TemplatePathListener();
					tpl.addListener(new TemplatePathListener.Listener() {
						@Override
						public void pathAdded(TemplateRole path) {
							templateRoles.add(path);
						}

						@Override
						public void pathRemoved(TemplateRole path) {
							templateRoles.remove(path);
						}

						@Override
						public void pathChanged(TemplateRole oldPath, TemplateRole newPath) {
							templateRoles.remove(oldPath);
							templateRoles.add(newPath);
						}
					});
					tpl.listen(theElStyle.getElement());
					theDependencyController.add(theStyleSheet);
					// Add a dependency for typed, non-grouped style sheet attributes
					allChanges().act(event -> theElStyle.getElement().events().fire(event));
				}, org.quick.core.QuickConstants.CoreStage.INIT_SELF.toString(), 1);
	}

	/** @return The element style that depends on this self-style */
	public ElementStyle getElementStyle() {
		return theElStyle;
	}

	@Override
	public QuickElement getElement() {
		return theElStyle.getElement();
	}

	@Override
	public <T> ElementSelfStyle set(StyleAttribute<T> attr, ObservableValue<T> value) throws ClassCastException, IllegalArgumentException {
		super.set(attr, value);
		return this;
	}

	@Override
	public <T> ElementSelfStyle set(StyleAttribute<T> attr, StateExpression exp, ObservableValue<T> value)
		throws ClassCastException, IllegalArgumentException {
		super.set(attr, exp, value);
		return this;
	}

	@Override
	public ElementSelfStyle clear(StyleAttribute<?> attr) {
		super.clear(attr);
		return this;
	}

	@Override
	public ElementSelfStyle clear(StyleAttribute<?> attr, StateExpression exp) {
		super.clear(attr, exp);
		return this;
	}

	@Override
	public String toString() {
		return "style.self of " + theElStyle.getElement();
	}
}
