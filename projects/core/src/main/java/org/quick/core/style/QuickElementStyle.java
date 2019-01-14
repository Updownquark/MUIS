package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.quick.core.QuickElement;
import org.quick.core.QuickTemplate.TemplateStructure.RoleAttribute;
import org.quick.core.mgr.QuickState;
import org.quick.core.prop.QuickAttribute;

/** The style on a {@link QuickElement} */
public class QuickElementStyle implements QuickStyle {
	private final QuickElement theElement;
	private final ObservableSet<QuickState> theExtraStates;
	private final ObservableSet<String> theExtraGroups;
	private StyleConditionInstance<?> theCondition;

	/** @param element The element that this style is for */
	public QuickElementStyle(QuickElement element) {
		this(element, null, null);
	}

	/**
	 * @param element The element that this style is for
	 * @param extraStates Extra states, if any to use for determining style from style sheets
	 * @param extraGroups Extra groups, if any to use for determining style from style sheets
	 */
	public QuickElementStyle(QuickElement element, ObservableSet<QuickState> extraStates, ObservableSet<String> extraGroups) {
		theElement = element;
		theExtraStates = extraStates;
		theExtraGroups = extraGroups;
	}

	/** @return The condition instance representing this style */
	public StyleConditionInstance<?> getCondition() {
		if (theCondition == null)
			theCondition = StyleConditionInstance.of(theElement, theExtraStates, theExtraGroups);
		return theCondition;
	}

	@Override
	public QuickStyle forExtraStates(ObservableCollection<QuickState> extraStates) {
		ObservableSet<QuickState> allExtraStates;
		if (theExtraStates == null) {
			if (extraStates instanceof ObservableSet)
				allExtraStates = (ObservableSet<QuickState>) extraStates;
			else
				allExtraStates = ObservableSet.unique(extraStates, Object::equals);
		} else
			allExtraStates = ObservableSet.unique(ObservableCollection.flattenCollections(theExtraStates, extraStates), Object::equals);
		return new QuickElementStyle(theElement, allExtraStates, theExtraGroups);
	}

	@Override
	public QuickStyle forExtraGroups(ObservableCollection<String> extraGroups) {
		ObservableSet<String> allExtraGroups;
		if (theExtraStates == null) {
			if (extraGroups instanceof ObservableSet)
				allExtraGroups = (ObservableSet<String>) extraGroups;
			else
				allExtraGroups = ObservableSet.unique(extraGroups, Object::equals);
		} else
			allExtraGroups = ObservableSet.unique(ObservableCollection.flattenCollections(theExtraGroups, extraGroups), Object::equals);
		return new QuickElementStyle(theElement, theExtraStates, allExtraGroups);
	}

	/** @return The element that this style is for */
	public QuickElement getElement() {
		return theElement;
	}

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		ObservableValue<QuickStyle> localStyle = theElement.atts().get(StyleAttributes.style);
		ObservableSet<StyleAttribute<?>> localAttrs = ObservableSet.flattenValue(localStyle.map(s -> s.attributes()));
		StyleSheet sheet = theElement.getDocument() == null ? null : theElement.getDocument().getStyle();
		ObservableSet<StyleAttribute<?>> parentAtts = ObservableSet
			.flattenValue(theElement.getParent().map(p -> p.getStyle().attributes())).filterStatic(att -> att.isInherited());
		ObservableCollection<StyleAttribute<?>> flattened;
		if (sheet == null)
			flattened = ObservableCollection.flattenCollections(localAttrs, parentAtts);
		else
			flattened = ObservableCollection.flattenCollections(localAttrs, parentAtts, sheet.attributes());
		return ObservableSet.unique(flattened, Object::equals);
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		QuickStyle localStyle = theElement.atts().get(StyleAttributes.style).get();
		if (localStyle != null && localStyle.isSet(attr))
			return true;
		StyleSheet sheet = theElement.getDocument().getStyle();
		if (sheet.isSet(getCondition(), attr))
			return true;
		if (attr.isInherited()) {
			QuickElement parent = theElement.getParent().get();
			// TODO not exactly right, because the parent's style may not have this style's extra states
			if (parent != null && parent.getStyle().isSet(attr))
				return true;
		}
		return false;
	}

	@Override
	public <T> ObservableValue<T> get(StyleAttribute<T> attr, boolean withDefault) {
		ObservableValue<T> localValue = getLocalValue(theElement, theElement, attr);
		if (theElement.getDocument() == null)
			return localValue;
		StyleSheet sheet = theElement.getDocument().getStyle();
		ObservableValue<StyleConditionValue<T>> ssMatch = sheet.getBestMatch(getCondition(), attr);
		ObservableValue<T> ssValue;
		if (attr.isInherited()) {
			ObservableValue<StyleConditionValue<T>> parentSSMatch = ObservableValue.flatten(theElement.getParent().map(p -> {
				if (p == null)
					return null;
				StyleConditionInstance<?> pCondition = StyleConditionInstance.of(p, theExtraStates, theExtraGroups);
				return sheet.getBestMatch(pCondition, attr);
			}));
			ssMatch = ssMatch.combine(null, (ss, pSS) -> {
				if (ss == null && pSS == null)
					return null;
				else if (ss == null)
					return pSS;
				else if (pSS == null)
					return ss;
				int comp = ss.compareTo(pSS);
				if (comp <= 0)
					return ss;
				else
					return pSS;
			}, parentSSMatch, null);
		}
		if (withDefault)
			ssValue = ObservableValue.flatten(ssMatch, () -> attr.getDefault());
		else
			ssValue = ObservableValue.flatten(ssMatch);
		return ObservableValue.firstValue(attr.getType().getType(), null, null, localValue, ssValue);
	}

	private static <T> ObservableValue<T> getLocalValue(QuickElement bottom, QuickElement element, StyleAttribute<T> attr) {
		ObservableValue<QuickStyle> localStyle = element.atts().get(StyleAttributes.style);
		ObservableValue<T> localValue = ObservableValue.flatten(localStyle.map(s -> s.get(attr, false)));
		if (attr.isInherited() && !isTemplateParent(element, bottom)) {
			ObservableValue<T> parentLocalValue = ObservableValue.flatten(element.getParent().map(p -> {
				if (p == null)
					return null;
				return getLocalValue(bottom, p, attr);
			}));
			localValue = ObservableValue.firstValue(attr.getType().getType(), null, null, localValue, parentLocalValue);
		}
		return localValue;
	}

	private static boolean isTemplateParent(QuickElement parent, QuickElement child) {
		if (parent == null)
			return false;
		for (QuickAttribute<?> attr : child.atts().attributes()) {
			if (attr instanceof RoleAttribute && ((RoleAttribute) attr).getTemplate().getDefiner().isInstance(parent))
				return true;
		}
		return false;
	}
}
