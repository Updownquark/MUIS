package org.muis.core.style.sheet;

import java.util.List;
import java.util.Map;

import org.muis.core.MuisElement;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionEvent;
import org.muis.core.style.StyleExpressionListener;
import org.muis.core.style.StyleExpressionValue;
import org.muis.core.style.stateful.StateExpression;
import org.muis.core.style.stateful.StatefulStyle;

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

	private TemplateRole [] theTemplatePaths;

	private final java.util.concurrent.ConcurrentLinkedQueue<StyleExpressionListener<StatefulStyle, StateExpression>> theListeners;

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
		theTemplatePaths = new TemplateRole[0];
		styleSheet.addListener(new StyleExpressionListener<StyleSheet, StateGroupTypeExpression<?>>() {
			@Override
			public void eventOccurred(StyleExpressionEvent<StyleSheet, StateGroupTypeExpression<?>, ?> evt) {
				if(matchesFilter(evt.getExpression()))
					styleChanged(evt.getAttribute(), evt.getExpression().getState());
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

	/** @return The template roles that this filter accepts */
	public TemplateRole [] getTemplateRoles() {
		return theTemplatePaths.clone();
	}

	/** @param path The path to add to the filtering on this style sheet */
	public void addTemplatePath(TemplateRole path) {
		for(TemplateRole p : theTemplatePaths)
			if(p.containsPath(path))
				return;
		TemplateRole [] paths = ArrayUtils.add(theTemplatePaths, path);
		setTemplatePaths(paths);
	}

	/** @param path The path to remove from the filtering on this style sheet */
	public void removeTemplatePath(TemplateRole path) {
		int index = ArrayUtils.indexOf(theTemplatePaths, path);
		if(index < 0)
			return;
		TemplateRole [] paths = ArrayUtils.remove(theTemplatePaths, index);
		setTemplatePaths(paths);
	}

	/**
	 * @param oldPath The path to remove from the filtering on this style sheet
	 * @param newPath The path to add to the filtering on this style sheet
	 */
	public void replaceTemplatePath(TemplateRole oldPath, TemplateRole newPath) {
		TemplateRole [] paths = theTemplatePaths;
		boolean add = true;
		for(TemplateRole p : paths)
			if(p.containsPath(newPath)) {
				add = false;
				break;
			}
		int index = ArrayUtils.indexOf(paths, oldPath);
		boolean remove = index >= 0;
		if(remove) {
			paths = ArrayUtils.remove(paths, index);
			if(add)
				paths = ArrayUtils.add(paths, newPath, index);
		} else if(add)
			paths = ArrayUtils.add(paths, newPath);
		if(remove || add)
			setTemplatePaths(paths);
	}

	/** @param newPaths The paths to set for the filtering on this style sheet */
	protected void setTemplatePaths(TemplateRole [] newPaths) {
		TemplateRole [] oldState = theTemplatePaths;
		theTemplatePaths = newPaths;
		Map<StyleAttribute<?>, List<StateExpression>> changedExprs = new java.util.HashMap<>();
		for(StyleAttribute<?> attr : allLocal()) {
			changedExprs.put(attr, new java.util.ArrayList<StateExpression>());
			for(StyleExpressionValue<StateGroupTypeExpression<?>, ?> sev : theStyleSheet.getExpressions(attr)) {
				StateGroupTypeExpression<?> expr = sev.getExpression();
				if(expr == null)
					continue;
				boolean oldMatch = matches(expr.getTemplateRole(), oldState);
				boolean newMatch = matches(expr.getTemplateRole(), newPaths);
				if(oldMatch != newMatch)
					changedExprs.get(attr).add(expr.getState());
			}
		}
		for(Map.Entry<StyleAttribute<?>, List<StateExpression>> value : changedExprs.entrySet())
			for(StateExpression exp : value.getValue())
				styleChanged(value.getKey(), exp);
	}

	private static boolean matches(TemplateRole path, TemplateRole [] paths) {
		if(path == null)
			return true;
		for(TemplateRole p : paths)
			if(p.containsPath(path))
				return true;
		return false;
	}

	/**
	 * @param expr The expression to check
	 * @return Whether a {@link StateGroupTypeExpression} with the given group name and type matches this filter such that its attribute
	 *         value will be exposed from this style's {@link StatefulStyle} methods
	 */
	public boolean matchesFilter(StateGroupTypeExpression<?> expr) {
		if(!ArrayUtils.equals(expr.getGroupName(), theGroupName) || !expr.getType().isAssignableFrom(theType))
			return false;
		if(expr.getTemplateRole() == null)
			return true;
		for(TemplateRole path : theTemplatePaths)
			if(path.containsPath(expr.getTemplateRole()))
				return true;
		return false;
	}

	@Override
	public StatefulStyle [] getConditionalDependencies() {
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
							for(StyleExpressionValue<StateGroupTypeExpression<?>, ?> exp : theStyleSheet.getExpressions(value))
								if(matchesFilter(exp.getExpression()))
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
	public <T> StyleExpressionValue<StateExpression, T> [] getLocalExpressions(StyleAttribute<T> attr) {
		StyleExpressionValue<StateGroupTypeExpression<?>, T> [] exprs = theStyleSheet.getExpressions(attr);
		java.util.ArrayList<StyleExpressionValue<StateExpression, T>> ret = new java.util.ArrayList<>();
		for(StyleExpressionValue<StateGroupTypeExpression<?>, T> exp : exprs)
			if(matchesFilter(exp.getExpression()))
				ret.add(new StyleExpressionValue<>(exp.getExpression().getState(), exp.getValue()));
		return ret.toArray(new StyleExpressionValue[ret.size()]);
	}

	@Override
	public <T> StyleExpressionValue<StateExpression, T> [] getExpressions(StyleAttribute<T> attr) {
		return getLocalExpressions(attr);
	}

	@Override
	public void addListener(StyleExpressionListener<StatefulStyle, StateExpression> listener) {
		if(listener != null)
			theListeners.add(listener);
	}

	@Override
	public void removeListener(StyleExpressionListener<StatefulStyle, StateExpression> listener) {
		theListeners.remove(listener);
	}

	void styleChanged(StyleAttribute<?> attr, StateExpression exp) {
		StyleExpressionEvent<StatefulStyle, StateExpression, ?> evt = new StyleExpressionEvent<StatefulStyle, StateExpression, Object>(
			this, this, (StyleAttribute<Object>) attr, exp);
		for(StyleExpressionListener<StatefulStyle, StateExpression> listener : theListeners)
			listener.eventOccurred(evt);
	}
}
