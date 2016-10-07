package org.quick.core.style;

import java.util.*;

import org.observe.Action;
import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.Subscription;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.quick.core.QuickElement;
import org.quick.core.QuickTemplate;
import org.quick.core.mgr.QuickState;

import com.google.common.reflect.TypeToken;

/**
 * A condition that can be evaluated against various attributes of a {@link QuickElement} to determine whether a style value from a
 * {@link StyleSheet} applies to the element
 */
public class StyleCondition implements Comparable<StyleCondition> {
	private final StateCondition theState;
	private final List<QuickTemplate.AttachPoint<?>> theRolePath;
	private final Set<String> theGroups;
	private final Class<? extends QuickElement> theType;

	private StyleCondition(StateCondition state, List<QuickTemplate.AttachPoint<?>> rolePath, Set<String> groups,
		Class<? extends QuickElement> type) {
		theState = state;
		theRolePath = rolePath == null ? Collections.emptyList() : Collections.unmodifiableList(rolePath);
		theGroups = Collections.unmodifiableSet(groups);
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
	public List<QuickTemplate.AttachPoint<?>> getRolePath() {
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

		if (theGroups.size() != o.theGroups.size())
			return o.theGroups.size() - theGroups.size();

		if (theType != o.theType) {
			int depth = getTypeDepth();
			int oDepth = o.getTypeDepth();
			if (depth != oDepth)
				return oDepth - depth;
		}

		return 0;
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

	public ObservableValue<Boolean> matches(StyleConditionInstance<?> value) {
		if (!theType.isAssignableFrom(value.getElementType()))
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
			groupMatches = new ObservableValue<Boolean>() {
				@Override
				public Subscription subscribe(Observer<? super ObservableValueEvent<Boolean>> observer) {
					boolean[] wasMatch = new boolean[1];
					Subscription sub = value.getGroups().simpleChanges().act(new Action<Object>() {
						@Override
						public void act(Object event) {
							boolean nowMatch = value.getGroups().containsAll(theGroups);
							if (wasMatch[0] != nowMatch) {
								observer.onNext(createChangeEvent(wasMatch[0], nowMatch, event));
								wasMatch[0] = nowMatch;
							}
						}
					});
					wasMatch[0] = value.getGroups().containsAll(theGroups);
					observer.onNext(createInitialEvent(wasMatch[0]));
					return sub;
				}

				@Override
				public boolean isSafe() {
					return value.getGroups().isSafe();
				}

				@Override
				public TypeToken<Boolean> getType() {
					return TypeToken.of(Boolean.TYPE);
				}

				@Override
				public Boolean get() {
					return value.getGroups().containsAll(theGroups);
				}
			};
	}

	/**
	 * @param element The element to test
	 * @return An observable boolean reflecting whether this condition currently applies to the given element
	 */
	public ObservableValue<Boolean> matches(QuickElement element) {
		if (!theType.isInstance(element))
			return ObservableValue.constant(false);

		ObservableValue<Boolean> stateMatches;
		if (theState == null)
			stateMatches = ObservableValue.constant(true);
		else
			stateMatches = theState.observeMatches(element.state().activeStates());

		ObservableValue<Boolean> groupMatches;
		if (theGroups.isEmpty())
			groupMatches = ObservableValue.constant(true);
		else
			groupMatches = element.atts().getHolder(StyleAttributes.group)
				.mapV(TypeToken.of(Boolean.class), groups -> {
					Set<String> elGroups = groups == null ? Collections.emptySet() : groups;
					return elGroups.containsAll(theGroups);
				}, true);

		ObservableValue<Boolean> rolePathMatches;
		if (theRolePath.isEmpty())
			rolePathMatches = ObservableValue.constant(true);
		else // TODO Make this observable. Probably easier when element children are observable collections
			rolePathMatches = ObservableValue.constant(rolePathMatches(element, theRolePath.size() - 1));

		return stateMatches.combineV((b1, b2, b3) -> b1 && b2 && b3, groupMatches, rolePathMatches);
	}

	/**
	 * @param element The element to test
	 * @param extraStates The extra states to test against
	 * @return An observable boolean reflecting whether this condition currently applies to the given element with the extra states
	 */
	public ObservableValue<Boolean> matches(QuickElement element, ObservableSet<QuickState> extraStates) {
		if (!theType.isInstance(element))
			return ObservableValue.constant(false);

		ObservableValue<Boolean> stateMatches;
		if (theState == null)
			stateMatches = ObservableValue.constant(true);
		else {
			ObservableSet<QuickState> allStates = ObservableSet
				.unique(ObservableCollection.flattenCollections(element.state().activeStates(), extraStates), Object::equals);
			stateMatches = theState.observeMatches(allStates);
		}

		ObservableValue<Boolean> groupMatches;
		if (theGroups.isEmpty())
			groupMatches = ObservableValue.constant(true);
		else
			groupMatches = element.atts().getHolder(StyleAttributes.group)
				.mapV(groups -> {
					Set<String> elGroups = groups == null ? Collections.emptySet() : groups;
					return elGroups.containsAll(theGroups);
				});

		ObservableValue<Boolean> rolePathMatches;
		if (theRolePath.isEmpty())
			rolePathMatches = ObservableValue.constant(true);
		else // TODO Make this observable. Probably easier when element children are observable collections
			rolePathMatches = ObservableValue.constant(rolePathMatches(element, theRolePath.size() - 1));

		return stateMatches.combineV((b1, b2, b3) -> b1 && b2 && b3, groupMatches, rolePathMatches);
	}

	private boolean rolePathMatches(QuickElement element, int index) {
		QuickTemplate.AttachPoint<?> role = theRolePath.get(index);
		if (element.atts().get(role.template.role) != role)
			return false;
		if (index == 0)
			return true;
		QuickElement owner = element.getParent();
		while (owner != null && !(owner instanceof QuickTemplate && ((QuickTemplate) owner).getTemplate() == role.template))
			owner = owner.getParent();
		if (owner == null)
			return false;
		return rolePathMatches(owner, index - 1);
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
