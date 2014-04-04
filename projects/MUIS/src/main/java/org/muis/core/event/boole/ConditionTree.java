package org.muis.core.event.boole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A tree that stores values by condition
 *
 * @param <T> The supertype of value that this tree's conditions can operate against
 * @param <V> The type of value that this tree stores
 */
public class ConditionTree<T, V> {
	/**
	 * A node in a condition tree
	 *
	 * @param <NF> The type of objects that this node's condition can test
	 * @param <NT> The type of objects that this node's condition can produce
	 */
	public class ConditionTreeNode<NF extends T, NT extends NF> {
		private final TypedPredicate<NF, NT> theCondition;

		private final List<ConditionTreeNode<? super NT, ?>> theChildren;
		private final List<ConditionTreeNode<? super NT, ?>> theExposedChildren;
		private final List<V> theValues;
		private final List<V> theExposedValues;

		ConditionTreeNode(TypedPredicate<NF, NT> condition) {
			theCondition = condition;
			theChildren = new ArrayList<>();
			theExposedChildren = Collections.unmodifiableList(theChildren);
			theValues = new ArrayList<>();
			theExposedValues = Collections.unmodifiableList(theValues);
		}

		/** @return This node's condition */
		public TypedPredicate<NF, NT> getCondition() {
			return theCondition;
		}

		/** @return This node's children */
		public List<ConditionTreeNode<? super NT, ?>> getChildren() {
			return theExposedChildren;
		}

		/** @return All values stored for this node's exact condition */
		public List<V> getValues() {
			return theExposedValues;
		}

		/**
		 * @param condition The condition to store the values for
		 * @param values The values to store
		 * @return Whether the condition could be found under this subtree, i.e. whether the values could be stored in this subtree
		 */
		public <T1 extends NF, T2 extends NT> boolean add(TypedPredicate<T1, T2> condition, V... values) {
			if(condition == null || condition.equals(theCondition)) {
				for(V value : values)
					if(!theValues.contains(value))
						theValues.add(value);
				return true;
			}
			if(theCondition != null
				&& !(condition instanceof IntersectTypedPredicate && ((IntersectTypedPredicate<T1, ?, ?, T2>) condition).getFirst().equals(
					theCondition)))
				return false;
			if(theCondition == null)
				addToChildren((TypedPredicate<T2, T2>) condition, values);
			else
				addToChildren(((IntersectTypedPredicate<T1, T2, T2, T2>) condition).getSecond(), values);
			return true;
		}

		/**
		 * @param condition The condition to remove the values for
		 * @param values The values to remove
		 * @return Whether the condition could be found under this subtree
		 */
		public <T1 extends NF, T2 extends NT> boolean remove(TypedPredicate<T1, T2> condition, V... values) {
			if(condition == null || condition.equals(theCondition)) {
				for(V value : values)
					theValues.remove(value);
				return true;
			}
			if(theCondition == null
				&& !(condition instanceof IntersectTypedPredicate && ((IntersectTypedPredicate<T1, ?, ?, T2>) condition).getFirst().equals(
					theCondition)))
				return false;
			if(theCondition == null)
				removeFromChildren((TypedPredicate<T2, T2>) condition, values);
			else
				removeFromChildren(((IntersectTypedPredicate<T1, T2, T2, T2>) condition).getSecond(), values);
			return true;
		}

		@SuppressWarnings("rawtypes")
		private <T1 extends NT, T2 extends T1> void addToChildren(TypedPredicate<T1, T2> condition, V... values) {
			for(ConditionTreeNode<? super NT, ?> child : theChildren) {
				if(((ConditionTreeNode<T1, T1>) child).add(condition, values))
					return;
			}
			ConditionTreeNode<T1, T2> newNode = new ConditionTreeNode<>(condition);
			for(V value : values)
				if(!newNode.theValues.contains(value))
					newNode.theValues.add(value);
			((List<ConditionTreeNode>) (List<?>) theChildren).add(newNode);
		}

		private <T1 extends NT, T2 extends T1> void removeFromChildren(TypedPredicate<T1, T2> condition, V... values) {
			for(ConditionTreeNode<? super NT, ?> child : theChildren) {
				if(((ConditionTreeNode<T1, T1>) child).remove(condition, values))
					return;
			}
		}

		private void addFor(List<V> values, NF test) {
			NT next = theCondition == null ? (NT) test : theCondition.cast(test);
			if(next == null)
				return;
			values.addAll(theValues);
			for(ConditionTreeNode<? super NT, ?> child : theChildren)
				child.addFor(values, next);
		}

		/** @param entries The list to add all values stored by condition in this subtree to */
		@SuppressWarnings("rawtypes")
		public void addEntries(List<Map.Entry<? extends TypedPredicate<? extends NF, ?>, List<V>>> entries) {
			if(!theValues.isEmpty())
				entries.add(new SimpleMapEntry<>(theCondition, theExposedValues));
			for(ConditionTreeNode<? super NT, ?> child : theChildren)
				((ConditionTreeNode) child).addEntries(entries);
		}
	}

	private ConditionTreeNode<T, T> theRoot;

	/** Creates a ConditionTree */
	public ConditionTree() {
		theRoot = new ConditionTreeNode<>(null);
	}

	/**
	 * @param condition The condition to store the values under
	 * @param values The values to store
	 */
	public <T1 extends T, T2 extends T1> void add(TypedPredicate<T1, T2> condition, V... values) {
		theRoot.add(condition, values);
	}

	/**
	 * @param condition The condition to remove values from
	 * @param values The values to remove
	 */
	public <T1 extends T, T2 extends T1> void remove(TypedPredicate<T1, T2> condition, V... values) {
		theRoot.remove(condition, values);
	}

	/**
	 * @param value The value to test against and get values for conditions that match
	 * @return All values stored in this tree for conditions that match the given value
	 */
	public <T1 extends T, T2 extends T1> List<V> get(T value) {
		ArrayList<V> ret = new ArrayList<>();
		theRoot.addFor(ret, value);
		return ret;
	}

	/** @return All values stored by condition in this tree */
	public List<Map.Entry<? extends TypedPredicate<? extends T, ?>, List<V>>> entries() {
		ArrayList<Map.Entry<? extends TypedPredicate<? extends T, ?>, List<V>>> ret = new ArrayList<>();
		theRoot.addEntries(ret);
		return ret;
	}

	@SuppressWarnings("hiding")
	private class SimpleMapEntry<K, V> implements Map.Entry<K, V> {
		private final K theKey;
		private final V theValue;

		SimpleMapEntry(K key, V value) {
			theKey = key;
			theValue = value;
		}

		@Override
		public K getKey() {
			return theKey;
		}

		@Override
		public V getValue() {
			return theValue;
		}

		@Override
		public V setValue(V value) {
			throw new UnsupportedOperationException();
		}
	}
}
