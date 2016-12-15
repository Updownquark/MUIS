package org.quick.util;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.observe.Observable;
import org.observe.ObservableValueEvent;
import org.quick.core.QuickElement;
import org.quick.core.QuickException;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleDomain;

/** A utility to accept and listen to attributes and listen to style attributes of an element or an element's children in a modular way. */
public interface CompoundListener {
	/**
	 * Adds configured attributes and listeners to an element
	 *
	 * @param element The element to add the listeners to
	 * @param root The root element being listened to. Unless called internally, this will typically be the same as {@code element}.
	 * @param until An observable that signals the end of this listener's interest in the element. When this observable fires, all
	 *        attributes added by this listener will be {@link org.quick.core.mgr.AttributeManager#reject(Object, QuickAttribute...)
	 *        rejected} and all listeners will be removed.
	 */
	void listen(QuickElement element, QuickElement root, Observable<?> until);

	/** Performs actions in response to configured events */
	public interface EventListener {
		/**
		 * Called when a configured event occurs
		 *
		 * @param evented The element that the event actually fired on
		 * @param root The root element being listened to
		 * @param event The event that occurred
		 */
		void eventOccurred(QuickElement evented, QuickElement root, ObservableValueEvent<?> event);
	}

	/**
	 * Allows for {@link CompoundListenerBuilder#when(Predicate, Consumer) conditions}
	 */
	public interface ElementMock {
		/**
		 * @param <T> The type of the attribute
		 * @param attr The attribute to get the value of
		 * @return The value of the attribute in the element that this mock represents
		 */
		<T> T getAttribute(QuickAttribute<T> attr);

		/**
		 * @param <T> The type of the attribute
		 * @param attr The style attribute to get the value of
		 * @return The value of the style attribute in the element that this mock represents
		 */
		<T> T getStyle(StyleAttribute<T> attr);
	}

	/** A utility change listener to cause a layout action in the element or parent element */
	public static final EventListener layout = (evented, root, event) -> {
		evented.relayout(false);
	};

	/** A utility change listener to fire a {@link org.quick.core.event.SizeNeedsChangedEvent} on the element or parent element */
	public static final EventListener sizeNeedsChanged = (evented, root, event) -> {
		evented.events().fire(new org.quick.core.event.SizeNeedsChangedEvent(evented, null));
	};

	/** Builds a compound listener */
	public interface CompoundListenerBuilder {
		/**
		 * @param attr The attribute to accept in the element(s) that this listener applies to
		 * @return The listener for chaining
		 */
		default CompoundListenerBuilder accept(QuickAttribute<?> attr) {
			return accept(attr, false, null);
		}

		/**
		 * @param <A> The type of the attribute to accept
		 * @param <V> The type of the initial value to set for the attribute
		 * @param attr The attribute to accept in the element(s) that this listener applies to
		 * @param value The initial value for the attribute (if it is not already set)
		 * @return The listener for chaining
		 * @throws IllegalArgumentException If {@link org.quick.core.mgr.AttributeManager#accept(Object, QuickAttribute, Object)} throws a
		 *         {@link QuickException}
		 */
		default <A, V extends A> CompoundListenerBuilder accept(QuickAttribute<A> attr, V value) throws IllegalArgumentException {
			return accept(attr, false, value);
		}

		/**
		 * @param attr The attribute to require in the element(s) that this listener applies to
		 * @return The listener for chaining
		 */
		default CompoundListenerBuilder require(QuickAttribute<?> attr) {
			return accept(attr, true, null);
		}

		/**
		 * @param <A> The type of the attribute to accept
		 * @param <V> The type of the initial value to set for the attribute
		 * @param attr The attribute to require in the element(s) that this listener applies to
		 * @param value The initial value for the attribute (if it is not already set)
		 * @return The listener for chaining
		 * @throws IllegalArgumentException If {@link org.quick.core.mgr.AttributeManager#accept(Object, QuickAttribute, Object)} throws a
		 *         {@link QuickException}
		 */
		default <A, V extends A> CompoundListenerBuilder require(QuickAttribute<A> attr, V value) throws IllegalArgumentException {
			return accept(attr, true, value);
		}

		/**
		 * @param <A> The type of the attribute to accept
		 * @param <V> The type of the initial value to set for the attribute
		 * @param attr The attribute to accept in the element(s) that this listener applies to
		 * @param required Whether the attribute should be required or just accepted
		 * @param value The initial value for the attribute (if it is not already set)
		 * @return The listener for chaining
		 * @throws IllegalArgumentException If {@link org.quick.core.mgr.AttributeManager#accept(Object, QuickAttribute, Object)} throws a
		 *         {@link QuickException}
		 */
		<A, V extends A> CompoundListenerBuilder accept(QuickAttribute<A> attr, boolean required, V value) throws IllegalArgumentException;

		/**
		 * A utility method for accepting multiple attributes at once
		 *
		 * @param attrs The attributes to accept
		 * @return The listener for chaining
		 */
		default CompoundListenerBuilder acceptAll(QuickAttribute<?>... attrs) {
			for (QuickAttribute<?> attr : attrs)
				accept(attr);
			return this;
		}

		/**
		 * A utility method for requiring multiple attributes at once
		 *
		 * @param attrs The attributes to require
		 * @return The listener for chaining
		 */
		default CompoundListenerBuilder requireAll(QuickAttribute<?>... attrs) {
			for (QuickAttribute<?> attr : attrs)
				require(attr);
			return this;
		}

		/**
		 * Watches a style attribute. When the attribute's value changes, any change listeners registered on this listener will fire.
		 *
		 * @param attr The style attribute to listen for
		 * @return The listener for chaining
		 */
		CompoundListenerBuilder watch(StyleAttribute<?> attr);

		/**
		 * @param domain The style domain to watch
		 * @return The listener for chaining
		 */
		default CompoundListenerBuilder watchAll(StyleDomain domain) {
			for (StyleAttribute<?> attr : domain)
				watch(attr);
			return this;
		}

		/**
		 * @param attrs The style attributes to watch
		 * @return The listener for chaining
		 */
		default CompoundListenerBuilder watchAll(StyleAttribute<?>... attrs) {
			for (StyleAttribute<?> attr : attrs)
				watch(attr);
			return this;
		}

		/**
		 * @param builder A function to build a listener to be applied to all children of elements passed to
		 *        {@link CompoundListener#listen(QuickElement, QuickElement, Observable)}
		 * @return This builder, for chaining
		 */
		CompoundListenerBuilder child(Consumer<CompoundListenerBuilder> builder);

		/**
		 * @param test The condition evaluator
		 * @param builder A function to build a listener to be applied to elements passed to
		 *        {@link CompoundListener#listen(QuickElement, QuickElement, Observable)} when they pass the given test
		 * @return This builder, for chaining
		 */
		CompoundListenerBuilder when(Predicate<ElementMock> test, Consumer<CompoundListenerBuilder> builder);

		/**
		 * @param run The runnable to execute when any attribute in the current chain changes
		 * @return The listener for chaining
		 */
		default CompoundListenerBuilder onChange(Runnable run) {
			return onEvent((element, root, event) -> run.run());
		}

		/**
		 * @param listener The listener to call when any attribute in the current chain changes
		 * @return The listener for chaining
		 */
		CompoundListenerBuilder onEvent(EventListener listener);

		/** @return The configured listener */
		CompoundListener build();
	}

	/** @return A builder to make compound listeners */
	public static CompoundListenerBuilder build() {
		return new CompoundListenerImpl.ElementListenerBuilder();
	}
}
