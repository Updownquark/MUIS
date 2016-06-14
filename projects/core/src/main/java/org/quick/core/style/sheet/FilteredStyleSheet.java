package org.quick.core.style.sheet;

import org.observe.collect.ObservableList;
import org.observe.collect.ObservableOrderedCollection;
import org.observe.collect.ObservableSet;
import org.qommons.ArrayUtils;
import org.quick.core.QuickElement;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleExpressionValue;
import org.quick.core.style.stateful.StateExpression;
import org.quick.core.style.stateful.StatefulStyle;

import com.google.common.reflect.TypeToken;

/**
 * A stateful style that gets all its style information from a {@link StyleSheet}, filtered by a group name and an element type
 *
 * @param <E> The element type that this style filters by
 */
public class FilteredStyleSheet<E extends QuickElement> implements StatefulStyle {
	private final StyleSheet theStyleSheet;

	private final String theGroupName;

	private final Class<E> theType;

	private ObservableSet<TemplateRole> theTemplatePaths;

	/**
	 * @param styleSheet The style sheet to get the style information from
	 * @param groupName The group name to filter by
	 * @param type The element type to filter by
	 * @param roles The remotely-controlled set of template paths to filter by
	 */
	public FilteredStyleSheet(StyleSheet styleSheet, String groupName, Class<E> type, ObservableSet<TemplateRole> roles) {
		theStyleSheet = styleSheet;
		theGroupName = groupName;
		if(type == null)
			type = (Class<E>) QuickElement.class;
		theType = type;
		theTemplatePaths = roles;
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
	public ObservableSet<TemplateRole> getTemplateRoles() {
		return theTemplatePaths;
	}

	/**
	 * @param expr The expression to check
	 * @return Whether a {@link StateGroupTypeExpression} with the given group name and type matches this filter such that its attribute
	 *         value will be exposed from this style's {@link StatefulStyle} methods
	 */
	public boolean matchesFilter(StateGroupTypeExpression<?> expr) {
		if(expr.getGroupName() != null && !ArrayUtils.equals(expr.getGroupName(), theGroupName))
			return false;
		if(!expr.getType().isAssignableFrom(theType))
			return false;
		if(expr.getTemplateRole() == null)
			return true;
		for(TemplateRole path : theTemplatePaths)
			if(path.containsPath(expr.getTemplateRole()))
				return true;
		return false;
	}

	@Override
	public ObservableList<StatefulStyle> getConditionalDependencies() {
		return ObservableList.constant(TypeToken.of(StatefulStyle.class));
	}

	@Override
	public ObservableSet<StyleAttribute<?>> allLocal() {
		ObservableSet<StyleAttribute<?>> ret = theStyleSheet.allAttrs();
		ret = ret.refresh(theTemplatePaths.changes());
		ret = ret.filter(attr -> {
			for(StyleExpressionValue<StateGroupTypeExpression<?>, ?> sev : theStyleSheet.getExpressions(attr))
				if(matchesFilter(sev.getExpression()))
					return true;
			return false;
		});
		return ret;
	}

	@Override
	public <T> ObservableOrderedCollection<StyleExpressionValue<StateExpression, T>> getLocalExpressions(StyleAttribute<T> attr) {
		return theStyleSheet.getExpressions(attr).refresh(theTemplatePaths.changes()).filterMap(sev -> {
			StyleAttribute<T> att = attr;
			if(sev == null)
				return null;
			if(matchesFilter(sev.getExpression()))
				return new StyleExpressionValue<>(sev.getExpression().getState(), sev, null);
			else
				return null;
		});
	}

	@Override
	public String toString() {
		return theStyleSheet + ".filter(" + theType.getSimpleName() + ", " + theGroupName + ", " + theTemplatePaths + ")";
	}
}
