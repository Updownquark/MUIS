package org.quick.core.style;

import java.util.*;

import org.observe.ObservableValue;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.quick.core.QuickElement;
import org.quick.core.QuickTemplate;
import org.quick.core.QuickTemplate.AttachPoint;
import org.quick.core.mgr.QuickState;

import com.google.common.reflect.TypeToken;

/**
 * A condition that can be evaluated against various attributes of a {@link QuickElement} to determine whether a style value from a
 * {@link StyleSheet} applies to the element
 */
public class StyleCondition implements Comparable<StyleCondition> {
	private final StateCondition theState;
	private final List<AttachPoint<?>> theRolePath;
	private final NavigableSet<String> theGroups;
	private final Class<? extends QuickElement> theType;

	private StyleCondition(StateCondition state, List<AttachPoint<?>> rolePath, Set<String> groups,
		Class<? extends QuickElement> type) {
		theState = state;
		theRolePath = rolePath == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(rolePath));
		theGroups = Collections.unmodifiableNavigableSet(new TreeSet<>(groups));
		if (!QuickElement.class.isAssignableFrom(type))
			throw new IllegalArgumentException("The type of a condition must extend " + QuickElement.class.getName());
		theType = type;
	}

	/** @return The condition on the element's {@link QuickElement#state() state} that must be met for this condition to apply */
	public StateCondition getState() {
		return theState;
	}

	/**
	 * @return The condition on the element's template {@link org.quick.core.QuickTemplate.TemplateStructure#role role} that must be met for
	 *         this condition to apply
	 */
	public List<AttachPoint<?>> getRolePath() {
		return theRolePath;
	}

	/** @return The groups that the element must belong to for this condition to apply */
	public Set<String> getGroups() {
		return theGroups;
	}

	/** @return The sub-type of QuickElement that this condition applies to */
	public Class<? extends QuickElement> getType() {
		return theType;
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

		if (theRolePath.size() != o.theRolePath.size())
			return o.theRolePath.size() - theRolePath.size();
		Iterator<AttachPoint<?>> apIter1 = theRolePath.iterator();
		Iterator<AttachPoint<?>> apIter2 = o.theRolePath.iterator();
		while (apIter1.hasNext()) {
			int comp = compare(apIter1.next(), apIter2.next());
			if (comp != 0)
				return comp;
		}

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
			return ObservableValue.constant(false);

		ObservableValue<Boolean> stateMatches;
		if (theState == null)
			stateMatches = ObservableValue.constant(true);
		else
			stateMatches = theState.observeMatches(value.getState());

		ObservableValue<Boolean> groupMatches;
		if (theGroups.isEmpty())
			groupMatches = ObservableValue.constant(true);
		else
			groupMatches = value.getGroups().observeContainsAll(ObservableCollection.constant(TypeToken.of(String.class), theGroups));

		ObservableValue<Boolean> rolePathMatches;
		if (theRolePath.isEmpty())
			rolePathMatches = ObservableValue.constant(true);
		else
			rolePathMatches = value.getRolePaths().observeContainsAll(
				ObservableCollection.constant(new TypeToken<List<QuickTemplate.AttachPoint<?>>>() {}, Arrays.asList(theRolePath)));

		return stateMatches.combineV((b1, b2, b3) -> b1 && b2 && b3, groupMatches, rolePathMatches);
	}

	/**
	 * @param element The element to test
	 * @return An observable boolean reflecting whether this condition currently applies to the given element
	 */
	public ObservableValue<Boolean> matches(QuickElement element) {
		return matches(StyleConditionInstance.of(element));
	}

	/**
	 * @param element The element to test
	 * @param extraStates The extra states to test against
	 * @return An observable boolean reflecting whether this condition currently applies to the given element with the extra states
	 */
	public ObservableValue<Boolean> matches(QuickElement element, ObservableSet<QuickState> extraStates) {
		return matches(StyleConditionInstance.of(element, extraStates));
	}

	@Override
	public int hashCode() {
		return Objects.hash(theState, theRolePath, theGroups, theType);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof StyleCondition))
			return false;
		StyleCondition c = (StyleCondition) obj;
		return Objects.equals(theState, c.theState) && theRolePath.equals(c.theRolePath) && theGroups.equals(c.theGroups)
			&& theType == c.theType;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(theType.getSimpleName());

		for (QuickTemplate.AttachPoint<?> role : theRolePath)
			str.append('.').append(role.name);

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
		private List<QuickTemplate.AttachPoint<?>> theRolePath;
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
		 * @param rolePath The role path for the condition
		 * @return This builder
		 */
		public Builder forPath(List<QuickTemplate.AttachPoint<?>> rolePath) {
			theRolePath = rolePath;
			return this;
		}

		/**
		 * @param rolePath The role path for the condition
		 * @return This builder
		 */
		public Builder forPath(QuickTemplate.AttachPoint<?>... rolePath) {
			return forPath(Arrays.asList(rolePath));
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
			return new StyleCondition(theState, theRolePath, theGroups, theType);
		}
	}
}
