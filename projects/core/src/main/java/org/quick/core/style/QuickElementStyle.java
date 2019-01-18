package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.observe.util.TypeTokens;
import org.quick.core.QuickElement;
import org.quick.core.QuickTemplate.TemplateStructure.RoleAttribute;
import org.quick.core.mgr.QuickState;
import org.quick.core.prop.QuickAttribute;

import com.google.common.reflect.TypeToken;

/** The style on a {@link QuickElement} */
public class QuickElementStyle implements QuickStyle {
	private final QuickElement theElement;
	private final ObservableSet<QuickState> theExtraStates;
	private final ObservableSet<String> theExtraGroups;
	private final ObservableSet<StyleAttribute<?>> theAttributes;
	private StyleConditionInstance<?> theCondition;
	private ObservableValue<StyleConditionInstance<?>> theParentCondition;

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

		ObservableValue<QuickStyle> localStyle = theElement.atts().get(StyleAttributes.style);
		if (localStyle == null)
			localStyle = theElement.atts().watchFor(StyleAttributes.style);
		ObservableSet<StyleAttribute<?>> localAttrs = ObservableSet
			.flattenValue(localStyle.map(s -> s == null ? ObservableSet.of(StyleAttribute.TYPE) : s.attributes()));
		StyleSheet sheet = theElement.getDocument() == null ? null : theElement.getDocument().getStyle();
		ObservableCollection.CollectionDataFlow<?, ?, StyleAttribute<?>> parentAttrs = ObservableSet
			.flattenValue(theElement.getParent().map(p -> p == null ? ObservableSet.of(StyleAttribute.TYPE) : p.getStyle().attributes()))
			.flow().filter(att -> att.isInherited() ? null : "Not inherited");
		ObservableCollection<ObservableCollection.CollectionDataFlow<?, ?, StyleAttribute<?>>> toFlatten;
		if (sheet == null)
			toFlatten = ObservableCollection.of(ATTR_CDF_TYPE, localAttrs.flow(), parentAttrs);
		else
			toFlatten = ObservableCollection.of(ATTR_CDF_TYPE, localAttrs.flow(), parentAttrs, sheet.attributes().flow());
		theAttributes = toFlatten.flow().flatMap(StyleAttribute.TYPE, f -> f).distinct().collect();
	}

	private static final TypeToken<ObservableCollection.CollectionDataFlow<?, ?, StyleAttribute<?>>> ATTR_CDF_TYPE = new TypeToken<ObservableCollection.CollectionDataFlow<?, ?, StyleAttribute<?>>>() {};

	/** @return The condition instance representing this style */
	public StyleConditionInstance<?> getCondition() {
		if (theCondition == null)
			theCondition = StyleConditionInstance.of(theElement, theExtraStates, theExtraGroups);
		return theCondition;
	}

	protected ObservableValue<StyleConditionInstance<?>> getParentCondition() {
		if (theParentCondition == null)
			theParentCondition = theElement.getParent()
				.map(p -> p == null ? null : StyleConditionInstance.of(p, theExtraStates, theExtraGroups));
		return theParentCondition;
	}

	@Override
	public QuickStyle forExtraStates(ObservableCollection<QuickState> extraStates) {
		ObservableSet<QuickState> allExtraStates;
		if (theExtraStates != null)
			allExtraStates = ObservableCollection.flattenCollections(TypeTokens.get().of(QuickState.class), theExtraStates, extraStates)
				.distinct().collect();
		else if (extraStates instanceof ObservableSet)
			allExtraStates = (ObservableSet<QuickState>) extraStates;
		else
			allExtraStates = extraStates.flow().distinct().collect();
		return new QuickElementStyle(theElement, allExtraStates, theExtraGroups);
	}

	@Override
	public QuickStyle forExtraGroups(ObservableCollection<String> extraGroups) {
		ObservableSet<String> allExtraGroups;
		if (theExtraGroups != null)
			allExtraGroups = ObservableCollection.flattenCollections(TypeTokens.get().STRING, theExtraGroups, extraGroups).distinct()
				.collect();
		else if (extraGroups instanceof ObservableSet)
			allExtraGroups = (ObservableSet<String>) extraGroups;
		else
			allExtraGroups = extraGroups.flow().distinct().collect();
		return new QuickElementStyle(theElement, theExtraStates, allExtraGroups);
	}

	/** @return The element that this style is for */
	public QuickElement getElement() {
		return theElement;
	}

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		return theAttributes;
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
			TypeToken<StyleConditionValue<T>> scvType = TypeTokens.get().keyFor(StyleConditionValue.class)
				.getCompoundType(attr.getType().getType());
			ObservableValue<ObservableValue<StyleConditionValue<T>>> layered = getParentCondition().map(scvType, pCondition -> {
				if (pCondition == null)
					return null;
				return sheet.getBestMatch(pCondition, attr);
			});
			ObservableValue<StyleConditionValue<T>> parentSSMatch = ObservableValue.flatten(layered);
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
		if (localStyle == null)
			localStyle = element.atts().watchFor(StyleAttributes.style);
		ObservableValue<T> localValue = ObservableValue.flatten(localStyle.map(s -> s == null ? null : s.get(attr, false)));
		if (attr.isInherited() && !isTemplateParent(element, bottom)) {
			TypeToken<ObservableValue<T>> obsType = TypeTokens.get().keyFor(ObservableValue.class)
				.getCompoundType(attr.getType().getType());
			ObservableValue<T> parentLocalValue = ObservableValue.flatten(element.getParent().map(obsType, p -> {
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
