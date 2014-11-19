package org.muis.core.style.sheet;

import org.muis.core.MuisElement;
import org.muis.core.rx.ObservableList;
import org.muis.core.rx.ObservableSet;
import org.muis.core.style.StyleAttribute;
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
			type = (Class<E>) MuisElement.class;
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
		return theStyleSheet.getConditionalDependencies().mapC(ss -> new FilteredStyleSheet<>(ss, theGroupName, theType, theTemplatePaths));
	}

	@Override
	public ObservableSet<StyleAttribute<?>> allLocal() {
		ObservableSet<StyleAttribute<?>> ret = theStyleSheet.allLocal();
		ret = ret.combineC(theTemplatePaths.changes(), (attr, v) -> attr);
		ret = ret.filterC(attr -> {
			return !getLocalExpressions(attr).isEmpty();
		});
		return ret;
	}

	@Override
	public <T> ObservableList<StyleExpressionValue<StateExpression, T>> getLocalExpressions(StyleAttribute<T> attr) {
		return theStyleSheet.getLocalExpressions(attr).combineC(theTemplatePaths.changes(), (sev, v) -> sev).filterMapC(sev -> {
			if(sev == null)
				return null;
			if(matchesFilter(sev.getExpression()))
				return new StyleExpressionValue<>(sev.getExpression().getState(), sev);
			else
				return null;
		});
	}

	@Override
	public String toString() {
		return theStyleSheet + ".filter(" + theType.getSimpleName() + ", " + theGroupName + ", " + theTemplatePaths + ")";
	}
}
