package org.muis.core.style;

import org.muis.core.MuisElement;

import prisms.util.ArrayUtils;

/**
 * A stateful style that gets all its style information from a {@link StyleSheet}, filtered by a group name and an element type
 *
 * @param <E> The element type that this style filters by
 */
public class FilteredStyleSheet<E extends MuisElement> implements StatefulStyle {
	private final StyleSheet theStyleSheet;

	private final String theGroupName;

	private final Class<E> theType;

	private final java.util.concurrent.ConcurrentLinkedQueue<StyleExpressionListener> theListeners;

	/**
	 * @param styleSheet The style sheet to get the style information from
	 * @param groupName The group name to filter by
	 * @param type The element type to filter by
	 */
	public FilteredStyleSheet(StyleSheet styleSheet, String groupName, Class<E> type) {
		theStyleSheet = styleSheet;
		theGroupName = groupName;
		if(type == null)
			type = (Class<E>) MuisElement.class;
		theType = type;
		styleSheet.addListener(new StyleGroupTypeExpressionListener() {
			@Override
			public void eventOccurred(StyleGroupTypeExpressionEvent<?, ?> evt) {
				if(matchesFilter(evt.getGroupName(), evt.getType()))
					styleChanged(evt.getAttribute(), evt.getExpression());
			}
		});
		theListeners = new java.util.concurrent.ConcurrentLinkedQueue<>();
	}

	/** @return The style sheet that this style gets its style information from */
	public StyleSheet getStyleSheet() {
		return theStyleSheet;
	}

	/** @return The name of the group that this style filters by */
	public String getGroupName() {
		return theGroupName;
	}

	/** @return The element type that this style filters by */
	public Class<E> getType() {
		return theType;
	}

	/**
	 * @param groupName The name of the group to check
	 * @param type The element type to check
	 * @return Whether a {@link StyleGroupTypeExpressionValue} with the given group name and type matches this filter such that its
	 *         attribute value will be exposed from this style's {@link StatefulStyle} methods
	 */
	public boolean matchesFilter(String groupName, Class<? extends MuisElement> type) {
		return ArrayUtils.equals(groupName, theGroupName) && type == theType;
	}

	@Override
	public StatefulStyle [] getStatefulDependencies() {
		return new StatefulStyle[0];
	}

	@Override
	public Iterable<StyleAttribute<?>> allLocal() {
		return new Iterable<StyleAttribute<?>>() {
			@Override
			public java.util.Iterator<StyleAttribute<?>> iterator() {
				return ArrayUtils.conditionalIterator(theStyleSheet.allAttrs().iterator(),
					new ArrayUtils.Accepter<StyleAttribute<?>, StyleAttribute<?>>() {
						@Override
						public StyleAttribute<?> accept(StyleAttribute<?> value) {
							for(StyleGroupTypeExpressionValue<?, ?> exp : theStyleSheet.getExpressions(value))
								if(matchesFilter(exp.getGroupName(), exp.getType()))
									return value;
							return null;
						}
					}, false);
			}
		};
	}

	@Override
	public Iterable<StyleAttribute<?>> allAttrs() {
		return allLocal();
	}

	@Override
	public <T> StyleExpressionValue<T> [] getLocalExpressions(StyleAttribute<T> attr) {
		StyleGroupTypeExpressionValue<?, T> [] exprs = theStyleSheet.getExpressions(attr);
		java.util.ArrayList<StyleExpressionValue<T>> ret = new java.util.ArrayList<>();
		for(StyleGroupTypeExpressionValue<?, T> exp : exprs)
			if(matchesFilter(exp.getGroupName(), exp.getType()))
				ret.add(new StyleExpressionValue<T>(exp.getExpression(), exp.getValue()));
		return ret.toArray(new StyleExpressionValue[ret.size()]);
	}

	@Override
	public <T> StyleExpressionValue<T> [] getExpressions(StyleAttribute<T> attr) {
		return getLocalExpressions(attr);
	}

	@Override
	public void addListener(StyleExpressionListener listener) {
		if(listener != null)
			theListeners.add(listener);
	}

	@Override
	public void removeListener(StyleExpressionListener listener) {
		theListeners.remove(listener);
	}

	void styleChanged(StyleAttribute<?> attr, StateExpression exp) {
		StyleExpressionEvent<?> evt = new StyleExpressionEvent<>(this, this, attr, exp);
		for(StyleExpressionListener listener : theListeners)
			listener.eventOccurred(evt);
	}
}
