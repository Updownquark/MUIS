package org.muis.core.event.boole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConditionTree<T, V> {
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

		public TypedPredicate<NF, NT> getCondition() {
			return theCondition;
		}

		public List<ConditionTreeNode<? super NT, ?>> getChildren() {
			return theExposedChildren;
		}

		public List<V> getValues() {
			return theExposedValues;
		}

		public <T1 extends NF, T2 extends NT> boolean add(TypedPredicate<T1, T2> condition, V... values) {
			if(condition == null || condition.equals(theCondition)) {
				for(V value : values)
					if(!theValues.contains(value))
						theValues.add(value);
				return true;
			}
			if(!(condition instanceof IntersectTypedPredicate && ((IntersectTypedPredicate<T1, ?, ?, T2>) condition).getFirst().equals(
				theCondition)))
				return false;
			addToChildren(((IntersectTypedPredicate<T1, T2, T2, T2>) condition).getSecond(), values);
			return true;
		}

		public <T1 extends NF, T2 extends NT> boolean remove(TypedPredicate<T1, T2> condition, V... values) {
			if(condition == null || condition.equals(theCondition)) {
				for(V value : values)
					theValues.remove(value);
				return true;
			}
			if(!(condition instanceof IntersectTypedPredicate && ((IntersectTypedPredicate<T1, ?, ?, T2>) condition).getFirst().equals(
				theCondition)))
				return false;
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
	}

	private ConditionTreeNode<T, T> theRoot;

	public ConditionTree() {
		theRoot = new ConditionTreeNode<>(new TypedPredicate<T, T>() {
			@Override
			public T cast(T value) {
				return value;
			}
		});
	}

	public <T1 extends T, T2 extends T1> void add(TypedPredicate<T1, T2> condition, V... values) {
		theRoot.add(condition, values);
	}

	public <T1 extends T, T2 extends T1> void remove(TypedPredicate<T1, T2> condition, V... values) {
		theRoot.remove(condition, values);
	}

	public <T1 extends T, T2 extends T1> List<V> get(T value) {
		ArrayList<V> ret = new ArrayList<>();
		theRoot.addFor(ret, value);
		return ret;
	}
}
