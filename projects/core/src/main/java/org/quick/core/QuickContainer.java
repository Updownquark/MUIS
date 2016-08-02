package org.quick.core;

import org.quick.core.mgr.ElementList;

/**
 * <p>
 * Marks an element (typically, though technically any class may implement this) that logically (rather than "physically") contains a set of
 * elements. The content of a container may be different than its {@link QuickElement#getChildren() children}. An example of a container is a
 * combo or select box. Physically, a combo box contains a content pane (perhaps a text field), a button (with the down arrow), and a menu,
 * which contains the options that the user may select. Logically, though, a combo box just needs a set of options to be specified for it to
 * display. A combo box's {@link QuickElement#getChildren() children} will have the content pane, the button, and the menu; but its
 * {@link #getContent() content} will just have the options.
 * </p>
 *
 * <p>
 * Containers may be immutable (throwing {@link UnsupportedOperationException}s when modification methods are called), but even if they are
 * mutable, because the contents have a logical relationship to the parent, modifications to the content list will have unspecified
 * consequences for the container's children. It is not in the contract that the container even add the new content to the physical element
 * hierarchy. In addition, the container may check any added content and throw an {@link IllegalArgumentException} if the content does not
 * meet the requirements for the container's content for any reason. Hence, it is unwise to use any of the modification methods of the
 * content list, particularly the addition or setter methods, in a general context. The calling code should understand more of the contract
 * of the specific container than just that it implements this interface. The non-modification methods of the list may be used in any
 * context.
 * </p>
 *
 * <p>
 * Concrete classes implementing this interface should typically declare the type of element contained, instead of being generic containers
 * themselves.
 * </p>
 *
 * <p>
 * Another context this interface might be used in is that an element (or other structure) might expose more than one element container. For
 * example, a tab container might expose its main content either by implementing this interface or by providing a method that returns a
 * QuickContainer implementation; and it might also wish to expose the widgets that represent its tabs. It might provide a getTabs() method
 * that returns a QuickContainer implementation whose type is more specific than just QuickElement. In this case, the getTabs() method might
 * return the tab container widget, or it might simply return QuickContainer&lt;TabWidget&gt;, whose implementation is undefined and does not
 * extend QuickElement, thus exposing the set of tab widgets without exposing any more of the tab container's inner structure.
 * </p>
 *
 * @param <E> The type of element contained in this container
 */
public interface QuickContainer<E extends QuickElement> {
	/**
	 * @return The logical contents of this container
	 * @see QuickContainer
	 */
	ElementList<E> getContent();
}
