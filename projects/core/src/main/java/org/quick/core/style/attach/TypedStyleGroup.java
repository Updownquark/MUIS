package org.quick.core.style.attach;

import java.util.List;

import org.observe.collect.ObservableSet;
import org.observe.collect.impl.ObservableArrayList;
import org.observe.util.ObservableUtils;
import org.quick.core.QuickDocument;
import org.quick.core.QuickElement;
import org.quick.core.style.sheet.FilteredStyleSheet;
import org.quick.core.style.sheet.TemplateRole;
import org.quick.core.style.stateful.AbstractStatefulStyle;
import org.quick.core.style.stateful.StatefulStyle;

import prisms.lang.Type;
import prisms.util.ArrayUtils;

/**
 * A TypedStyleGroup is a group in MUIS that holds members of a given type. This allows styles to be applied not only to named groups (
 * {@link NamedStyleGroup}) but also to specific sub-types within the named group.
 *
 * @param <E> The sub-type of QuickElement that this group holds
 */
public class TypedStyleGroup<E extends QuickElement> extends AbstractStatefulStyle {
	private QuickDocument theDocument;

	private TypedStyleGroup<? super E> theParent;

	private TypedStyleGroup<? extends E> [] theChildren;

	private final Class<E> theType;

	private E [] theMembers;

	private List<StatefulStyle> theDependenyController;

	/**
	 * Creates a typed style group
	 *
	 * @param doc The document that this style group exists in
	 * @param parent The parent style group that this group is a sub-type of
	 * @param type The type of elements that this group is to hold
	 */
	public TypedStyleGroup(QuickDocument doc, TypedStyleGroup<? super E> parent, Class<E> type) {
		this(doc, parent, type, null);
	}

	/**
	 * Creates a typed style group
	 *
	 * @param doc The document that this style group exists in
	 * @param parent The parent style group that this group is a sub-type of
	 * @param type The type of elements that this group is to hold
	 * @param name The name of the root
	 */
	protected TypedStyleGroup(QuickDocument doc, TypedStyleGroup<? super E> parent, Class<E> type, String name) {
		super(ObservableUtils.control(new ObservableArrayList<>(new Type(StatefulStyle.class))));
		theDependenyController = ObservableUtils.getController(getConditionalDependencies());
		theDocument = doc;
		theParent = parent;
		theType = type;
		theChildren = new TypedStyleGroup[0];
		theMembers = (E []) java.lang.reflect.Array.newInstance(type, 0);
		/* 3 potential dependencies:
		 * 1) style sheet entries for this group's name and type (if this group is an NamedStyleGroup or is descended from one)
		 * 2) style sheet entries for this group's type, group-unspecific (if this group is not a NamedStyleGroup and is not descended from one)
		 * 3) the parent type group, if it's not null
		 */
		NamedStyleGroup root = getRoot();
		ObservableSet<TemplateRole> roles = ObservableSet.constant(new Type(TemplateRole.class));
		if(root != null && root.getName() != null) // name==null Happens in the super constructor call for NamedStyleGroup
			theDependenyController.add(new FilteredStyleSheet<>(doc.getStyle(), root.getName(), type, roles));
		else
			theDependenyController.add(new FilteredStyleSheet<>(doc.getStyle(), name, type, roles));
		if(parent != null)
			theDependenyController.add(0, parent);
	}

	/** @return The document that this group exists in */
	public QuickDocument getDocument() {
		return theDocument;
	}

	/** @return The parent style group that this group is a sub-type of */
	public TypedStyleGroup<? super E> getParent() {
		return theParent;
	}

	/** @return The named group that is at the root of the hierarchy this style group is in, or null if the root is not a named group */
	public NamedStyleGroup getRoot() {
		TypedStyleGroup<?> root = this;
		while(root != null && !(root instanceof NamedStyleGroup))
			root = root.theParent;
		return (NamedStyleGroup) root;
	}

	/** @return This group's type */
	public Class<E> getType() {
		return theType;
	}

	/** @return The number of children groups in this group */
	public int getChildCount() {
		return theChildren.length;
	}

	/**
	 * @param idx The index of the child group to get
	 * @return This group's child group at the given index
	 */
	public TypedStyleGroup<? extends E> getChild(int idx) {
		return theChildren[idx];
	}

	/**
	 * Finds the group within this group's structure whose type is exactly <code>S</code>. If the group does not exist, this group is
	 * restructured to contain such a group.
	 *
	 * @param <S> The compile-time sub-type of QuickElement to get the group for
	 * @param type The runtime sub-type of QuickElement to get the group for
	 * @return The group descended from this style group whose type is <code>S</code>.
	 */
	public synchronized <S extends E> TypedStyleGroup<S> insertTypedGroup(Class<S> type) {
		if(type == theType)
			return (TypedStyleGroup<S>) this;
		if(type.isAssignableFrom(getType()))
			throw new IllegalArgumentException("Type " + type.getName() + " is not a sub-type of this group (" + this + ")'s type ("
				+ getType().getName() + ")");
		if(type.getSuperclass() != theType) {
			TypedStyleGroup<? super S> intermediate = (TypedStyleGroup<? super S>) insertTypedGroup((Class<? extends E>) type
				.getSuperclass());
			return intermediate.insertTypedGroup(type);
		}
		/* I can NOT get this to work with generics, so I'm doing it unchecked */
		@SuppressWarnings("rawtypes")
		TypedStyleGroup [] children = theChildren;
		for(int c = 0; c < children.length; c++)
			if(children[c].getType().isAssignableFrom(type))
				return children[c].insertTypedGroup(type.asSubclass(theChildren[c].getType()));
		TypedStyleGroup<S> ret = new TypedStyleGroup<>(theDocument, this, type);
		for(int c = 0; c < children.length; c++)
			if(type.isAssignableFrom(children[c].getType())) {
				ret.theChildren = ArrayUtils.add(ret.theChildren, children[c]);
				children[c].theDependenyController.remove(this);
				children[c].theDependenyController.add(ret);
				children[c].theParent = ret;
				children = ArrayUtils.remove(children, c);
				c--;
			}
		children = ArrayUtils.add(children, ret);
		theChildren = children;
		return ret;
	}

	/**
	 * Returns the most specific style group within this group whose type is the same as or a superclass of <code>s</code>. This may be
	 * <code>this</code>. This method does not modify this group's structure or content.
	 *
	 * @param <S> The compile-time sub-type of QuickElement to get the group for
	 * @param type The runtime sub-type of QuickElement to get the group for
	 * @return The most specific style group within this group whose type is the same as or a superclass of <code>s</code>. This may be
	 *         <code>this</code>.
	 */
	public <S extends E> TypedStyleGroup<? super S> getGroupForType(Class<S> type) {
		final TypedStyleGroup<? extends E> [] children = theChildren;
		/* I can NOT get this to work with generics, so I'm doing it unchecked */
		for(@SuppressWarnings("rawtypes")
		TypedStyleGroup child : children)
			if(child.getType().isAssignableFrom(type))
				return child.getGroupForType(type.asSubclass(child.getType()));
		return this;
	}

	/** @return The number of members in this group (not including its sub-typed groups) */
	public int getMemberCount() {
		return theMembers.length;
	}

	/**
	 * @param element The element to determine membership in this group
	 * @return Whether the given element is a member of this group
	 */
	public boolean isMember(E element) {
		TypedStyleGroup<?> group = getGroupForType((Class<? extends E>) element.getClass());
		return ArrayUtils.contains((E []) group.theMembers, element);
	}

	<T extends E> TypedStyleGroup<T> addMember(T member) {
		return addMember(member, theMembers.length);
	}

	<T extends E> TypedStyleGroup<T> addMember(T member, int index) {
		TypedStyleGroup<T> group = insertTypedGroup((Class<T>) member.getClass());
		if(index < 0)
			index = group.theMembers.length;
		group.theMembers = prisms.util.ArrayUtils.add(group.theMembers, member, index);
		return group;
	}

	void removeMember(E member) {
		TypedStyleGroup<?> group = getGroupForType((Class<? extends E>) member.getClass());
		theMembers = prisms.util.ArrayUtils.remove((E []) group.theMembers, member);
	}

	/**
	 * @return An iterable to iterate through every member in this group and its sub-typed groups. The iterator returned by this method is
	 *         an immutable capture of the content of this group and its sub-typed groups at the moment. Any changes made to this group or
	 *         its subgroups after the iterator is created are not reflected in the iterator's content, and the iterator itself is
	 *         immutable, so it cannot affect the content of this group.
	 */
	public Iterable<E> members() {
		return new MemberIterable<>(this, theMembers, theChildren, theType);
	}

	/**
	 * Creates an iterator to iterate through every member in this group, but not its sub-typed groups. The iterator returned by this method
	 * is an immutable capture of the content of this group at the moment. Any changes made to this group after the iterator is created are
	 * not reflected in the iterator's content, and the iterator itself is immutable, so it cannot affect the content of this group.
	 *
	 * @return An iterable that can return an iterator to iterate through this group's members without descending into this group's
	 *         sub-members
	 */
	public Iterable<E> shallowMembers() {
		return new MemberIterable<>(this, theMembers, new TypedStyleGroup[0], theType);
	}

	/**
	 * Creates an iterator to iterate through every member in this group and its sub-typed groups that is an instance of the given type. The
	 * iterator returned by this method is an immutable capture of the content of this group and its sub-typed groups at the moment. Any
	 * changes made to this group or its subgroups after the iterator is created are not reflected in the iterator's content, and the
	 * iterator itself is immutable, so it cannot affect the content of this group.
	 *
	 * The argument class may be an extension of {@link QuickElement} or an arbitrary interface. If a class is given whose type is not
	 * compatible with QuickElement, the iterator will of course be empty.
	 *
	 * @param <T> The compile-time type of the members to return
	 * @param type The runtime type of the members to return
	 * @return An iterable that can return an iterator to iterate through every member of this group and its subgroups that is also an
	 *         instance of the given class.
	 */
	public <T> Iterable<T> members(Class<T> type) {
		return new MemberIterable<>(this, theMembers, theChildren, type);
	}

	private static class MemberIterable<E extends QuickElement, T> implements Iterable<T> {
		private final TypedStyleGroup<E> theGroup;

		private final E [] theMembers;

		private final TypedStyleGroup<? extends E> [] theChildren;

		private final Class<T> theType;

		MemberIterable(TypedStyleGroup<E> group, E [] members, TypedStyleGroup<? extends E> [] children, Class<T> type) {
			theGroup = group;
			theMembers = members;
			theChildren = children;
			theType = type;
		}

		@Override
		public MemberIterator<E, T> iterator() {
			return new MemberIterator<>(theGroup, theMembers, theChildren, theType);
		}
	}

	private static class MemberIterator<E extends QuickElement, T> implements java.util.ListIterator<T> {
		private final TypedStyleGroup<E> theGroup;

		private final E [] members;

		private final TypedStyleGroup<? extends E> [] theChildren;

		private final Class<T> theType;

		private java.util.ListIterator<? extends T> [] childIters;

		private int memberIndex;

		private int childIndex;

		private int overallIndex;

		MemberIterator(TypedStyleGroup<E> group, E [] _members, TypedStyleGroup<? extends E> [] _children, Class<T> type) {
			theGroup = group;
			members = _members;
			theChildren = _children;
			theType = type;
			childIters = new java.util.ListIterator[theChildren.length];
			for(int c = 0; c < theChildren.length; c++)
				if(theGroup.getType().equals(theType))
					childIters[c] = (java.util.ListIterator<? extends T>) theChildren[c].members().iterator();
				else if(type.isInterface() || theType.isAssignableFrom(theChildren[c].getType()))
					childIters[c] = (java.util.ListIterator<T>) theChildren[c].members(type).iterator();
		}

		@Override
		public boolean hasNext() {
			if(memberIndex < members.length)
				return true;
			if(childIndex < theChildren.length) {
				while((childIters[childIndex] == null || !childIters[childIndex].hasNext()) && childIndex < theChildren.length)
					childIndex++;
				return childIndex < theChildren.length && childIters[childIndex] != null && childIters[childIndex].hasNext();
			}
			return false;
		}

		@Override
		public T next() {
			T ret = null;
			do {
				if(theType.isInstance(members[memberIndex]))
					ret = theType.cast(members[memberIndex]);
				memberIndex++;
			} while(memberIndex < members.length && ret == null);
			if(ret != null)
				return ret;
			if(childIndex < theChildren.length) {
				while((childIters[childIndex] == null || !childIters[childIndex].hasNext()) && childIndex < theChildren.length)
					childIndex++;
				if(childIndex == theChildren.length)
					throw new IndexOutOfBoundsException("No next member");
				ret = childIters[childIndex].next(); /* Let the child iterator throw the exception if needed */
			} else
				throw new IndexOutOfBoundsException("No next member");
			overallIndex++;
			return ret;
		}

		@Override
		public int nextIndex() {
			return overallIndex;
		}

		@Override
		public boolean hasPrevious() {
			if(memberIndex > 0)
				return true;
			if(childIndex > 0)
				return true;
			if(childIters[childIndex] != null)
				return childIters[childIndex].hasPrevious();
			return false;
		}

		@Override
		public T previous() {
			T ret = null;
			if(childIndex > 0 && (childIndex == theChildren.length || childIters[childIndex] == null))
				childIndex--;
			while(childIndex > 0 && (childIters[childIndex] == null || !childIters[childIndex].hasPrevious()))
				childIndex--;
			// theChildren may be 0-length
			if(childIndex < theChildren.length && childIters[childIndex] != null && childIters[childIndex].hasPrevious())
				ret = childIters[childIndex].previous();
			else if(memberIndex > 0)
				do {
					memberIndex--;
					if(theType.isInstance(members[memberIndex]))
						ret = theType.cast(members[memberIndex]);
				} while(memberIndex > 0 && ret == null);
			if(ret == null)
				throw new IndexOutOfBoundsException("No previous member");
			overallIndex--;
			return ret;
		}

		@Override
		public int previousIndex() {
			return overallIndex - 1;
		}

		@Override
		public void add(T e) {
			throw new UnsupportedOperationException("TypedStyleGroup iterators do not support modification");
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("TypedStyleGroup iterators do not support modification");
		}

		@Override
		public void set(T e) {
			throw new UnsupportedOperationException("TypedStyleGroup iterators do not support modification");
		}
	}
}
