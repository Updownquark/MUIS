package org.muis.core.style.sheet;

import java.util.Set;

import org.muis.core.MuisElement;
import org.muis.core.rx.DefaultObservableSet;
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
	private Set<TemplateRole> theTPController;

	/**
	 * @param styleSheet The style sheet to get the style information from
	 * @param groupName The group name to filter by
	 * @param type The element type to filter by
	 */
	public FilteredStyleSheet(StyleSheet styleSheet, String groupName, Class<E> type) {
		this(styleSheet, groupName, type, new DefaultObservableSet<>());
		theTPController = ((DefaultObservableSet<TemplateRole>) theTemplatePaths).control(null);
	}

	/**
	 * @param styleSheet The style sheet to get the style information from
	 * @param groupName The group name to filter by
	 * @param type The element type to filter by
	 * @param roles The remotely-controlled set of template paths to filter by
	 */
	protected FilteredStyleSheet(StyleSheet styleSheet, String groupName, Class<E> type, ObservableSet<TemplateRole> roles) {
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

	/** @param path The path to add to the filtering on this style sheet */
	public void addTemplatePath(TemplateRole path) {
		if(theTPController == null)
			throw new UnsupportedOperationException("This style sheet is not mutable");
		theTPController.add(path);
	}

	/** @param path The path to remove from the filtering on this style sheet */
	public void removeTemplatePath(TemplateRole path) {
		if(theTPController == null)
			throw new UnsupportedOperationException("This style sheet is not mutable");
		theTPController.remove(path);
	}

	/**
	 * @param oldPath The path to remove from the filtering on this style sheet
	 * @param newPath The path to add to the filtering on this style sheet
	 */
	public void replaceTemplatePath(TemplateRole oldPath, TemplateRole newPath) {
		if(theTPController == null)
			throw new UnsupportedOperationException("This style sheet is not mutable");
		theTPController.remove(oldPath);
		theTPController.add(newPath);
	}

	/** @param newPaths The paths to set for the filtering on this style sheet */
	protected void setTemplatePaths(TemplateRole [] newPaths) {
		if(theTPController == null)
			throw new UnsupportedOperationException("This style sheet is not mutable");
		theTPController.clear();
		theTPController.addAll(java.util.Arrays.asList(newPaths));
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
			if(matchesFilter(sev.getExpression()))
				return new StyleExpressionValue<>(sev.getExpression().getState(), sev.getValue());
			else
				return null;
		});
	}
}
