package org.quick.core.style;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.observe.ObservableValue;
import org.observe.collect.ObservableCollection;
import org.observe.util.TypeTokens;
import org.quick.core.QuickElement;
import org.quick.core.QuickTemplate.AttachPoint;

/**
 * A condition that can be evaluated against various attributes of a {@link QuickElement} to determine whether a style value from a
 * {@link StyleSheet} applies to the element
 */
public class StyleCondition implements Comparable<StyleCondition> {
	private final Class<? extends QuickElement> theType;
	private final StateCondition theState;
	private final NavigableSet<String> theGroups;
	private final AttachPoint<?> theRole;
	private final StyleCondition theParent;

	private StyleCondition(Class<? extends QuickElement> type, StateCondition state, Set<String> groups, AttachPoint<?> role,
		StyleCondition parent) {
		if (!QuickElement.class.isAssignableFrom(type))
			throw new IllegalArgumentException("The type of a condition must extend " + QuickElement.class.getName());
		theType = type;
		theState = state;
		theGroups = Collections.unmodifiableNavigableSet(new TreeSet<>(groups));
		theRole = role;
		theParent = parent;
	}

	/** @return The sub-type of QuickElement that this condition applies to */
	public Class<? extends QuickElement> getType() {
		return theType;
	}

	/** @return The condition on the element's {@link QuickElement#state() state} that must be met for this condition to apply */
	public StateCondition getState() {
		return theState;
	}

	/** @return The groups that the element must belong to for this condition to apply */
	public Set<String> getGroups() {
		return theGroups;
	}

	/** @return The template role that the element must satisfy for this condition to apply */
	public AttachPoint<?> getRole() {
		return theRole;
	}

	/**
	 * @return The style condition that must apply to the templated element that defines this condition's role for this condition to apply.
	 *         Will be non-null IFF {@link #getRole()} is non-null.
	 */
	public StyleCondition getParent() {
		return theParent;
	}

	@Override
	public int compareTo(StyleCondition o) {
		if (this == o)
			return 0;
		int compare;
		if(theState!=null){
			if(o.theState==null)
				return -1;
			compare=theState.compareTo(o.theState);
			if(compare!=0)
				return compare;
		} else if(o.theState!=null)
			return 1;

		if (theRole != null) {
			if (o.theRole == null)
				return -1;
			int comp = theParent.compareTo(o.theParent);
			if (comp != 0)
				return comp;
			comp = compare(theRole, o.theRole);
			if (comp != 0)
				return comp;
		} else if (o.theRole != null)
			return 1;

		if (theGroups.size() != o.theGroups.size())
			return o.theGroups.size() - theGroups.size();
		Iterator<String> groupIter1 = theGroups.iterator();
		Iterator<String> groupIter2 = o.theGroups.iterator();
		while (groupIter1.hasNext()) {
			int comp = groupIter1.next().compareToIgnoreCase(groupIter2.next());
			if (comp != 0)
				return comp;
			comp = groupIter1.next().compareTo(groupIter2.next());
			if (comp != 0)
				return comp;
		}

		if (theType != o.theType) {
			int depth = getTypeDepth();
			int oDepth = o.getTypeDepth();
			if (depth != oDepth)
				return oDepth - depth;
			int comp = theType.getSimpleName().compareToIgnoreCase(o.theType.getSimpleName());
			if (comp != 0)
				return comp;
			return theType.getName().compareTo(o.theType.getName());
		}

		return 0;
	}

	private int compare(AttachPoint<?> ap1, AttachPoint<?> ap2) {
		String def1 = ap1.template.getDefiner().getName();
		String def2 = ap2.template.getDefiner().getName();
		int comp = def1.compareToIgnoreCase(def2);
		if (comp != 0)
			return comp;
		comp = def1.compareTo(def2);
		if (comp != 0)
			return comp;
		comp = ap1.name.compareToIgnoreCase(ap2.name);
		if (comp != 0)
			return comp;
		comp = ap1.name.compareTo(ap2.name);
		return comp;
	}

	/** @return The number of subclasses away from {@link QuickElement} that this condition's {@link #getType() type} is */
	public int getTypeDepth() {
		int depth = 0;
		Class<?> type = theType;
		while (type != QuickElement.class) {
			depth++;
			type = type.getSuperclass();
		}
		return depth;
	}

	/**
	 * @param elementType The element type to test
	 * @return Whether this condition matches the given type
	 */
	public boolean matchesType(Class<? extends QuickElement> elementType) {
		return theType.isAssignableFrom(elementType);
	}

	/**
	 * @param value The style condition to test
	 * @return An observable boolean reflecting whether this condition currently applies to the given condition
	 */
	public ObservableValue<Boolean> matches(StyleConditionInstance<?> value) {
		if (!matchesType(value.getElementType()))
			return ObservableValue.of(false);

		ObservableValue<Boolean> stateMatches;
		if (theState == null)
			stateMatches = ObservableValue.of(true);
		else
			stateMatches = theState.observeMatches(value.getState());

		ObservableValue<Boolean> groupMatches;
		if (theGroups.isEmpty())
			groupMatches = ObservableValue.of(true);
		else
			groupMatches = value.getGroups().observeContainsAll(ObservableCollection.of(TypeTokens.get().STRING, theGroups));

		ObservableValue<Boolean> roleMatches;
		if (theRole == null)
			roleMatches = ObservableValue.of(true);
		else
			roleMatches = ObservableValue.flatten(value.getRoleParent(theRole).map(parent -> {
				if (parent != null)
					return theParent.matches(parent);
				else
					return ObservableValue.of(TypeTokens.get().BOOLEAN, false);
			}));

		return stateMatches.combine((b1, b2, b3) -> b1 && b2 && b3, groupMatches, roleMatches);
	}

	@Override
	public int hashCode() {
		return Objects.hash(theState, theRole, theParent, theGroups, theType);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof StyleCondition))
			return false;
		StyleCondition c = (StyleCondition) obj;
		return Objects.equals(theState, c.theState) && Objects.equals(theRole, c.theRole) && Objects.equals(theParent, c.theParent)
			&& theGroups.equals(c.theGroups) && theType == c.theType;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();

		if (theParent != null) {
			str.append(theParent.toString());
			str.append('.').append(theRole.name);
		} else
			str.append(theType.getSimpleName());

		if (!theGroups.isEmpty()) {
			str.append('[');
			boolean first = true;
			for (String group : theGroups) {
				if (!first)
					str.append(',');
				first = false;
				str.append(group);
			}

			str.append(']');
		}

		if (theState != null) {
			str.append('(').append(theState).append(')');
		}

		return str.toString();
	}

	/**
	 * @param type The sub-type of element to build the condition for
	 * @return A builder to build a {@link StyleCondition}
	 */
	public static Builder build(Class<? extends QuickElement> type) {
		return new Builder(type);
	}

	/** Builds {@link StyleCondition}s */
	public static class Builder {
		private final Class<? extends QuickElement> theType;
		private StateCondition theState;
		private AttachPoint<?> theRole;
		private StyleCondition theParent;
		private Set<String> theGroups;

		private Builder(Class<? extends QuickElement> type) {
			if (!QuickElement.class.isAssignableFrom(type))
				throw new IllegalArgumentException("The type of a condition must extend " + QuickElement.class.getName());
			theType = type;
			theGroups = new LinkedHashSet<>();
		}

		/**
		 * @param state The state for the condition
		 * @return This builder
		 */
		public Builder setState(StateCondition state) {
			theState = state;
			return this;
		}

		/**
		 * @param role The role for the condition
		 * @param parent the style condition for the templated element that this condition applies to the given role of
		 * @return This builder
		 */
		public Builder forRole(AttachPoint<?> role, StyleCondition parent) {
			theRole = role;
			theParent = parent;
			return this;
		}

		/**
		 * @param group The group set for the condition
		 * @return This builder
		 */
		public Builder forGroups(Collection<String> group) {
			theGroups.addAll(group);
			return this;
		}

		/**
		 * @param group The group set for the condition
		 * @return This builder
		 */
		public Builder forGroup(String... group) {
			for (String g : group)
				theGroups.add(g);
			return this;
		}

		/** @return The new style condition */
		public StyleCondition build() {
			return new StyleCondition(theType, theState, theGroups, theRole, theParent);
		}
	}
}
