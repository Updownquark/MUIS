package org.quick.util;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.observe.Observable;
import org.observe.ObservableValueEvent;
import org.quick.core.QuickDefinedWidget;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleDomain;

/** A utility to accept and listen to attributes and listen to style attributes of an element or an element's children in a modular way. */
public interface CompoundListener<W extends QuickDefinedWidget> {
	/**
	 * Adds configured attributes and listeners to a widget
	 *
	 * @param widget The widget to add the listeners to
	 * @param root The root widget being listened to. Unless called internally, this will typically be the same as {@code element}.
	 * @param until An observable that signals the end of this listener's interest in the widget. When this observable fires, all attributes
	 *        added by this listener will be {@link org.quick.core.mgr.AttributeManager2.AttributeAcceptance#reject() rejected} and all
	 *        listeners will be removed.
	 */
	void listen(W widget, W root, Observable<?> until);

	/** Performs actions in response to configured events */
	public interface EventListener<W extends QuickDefinedWidget> {
		/**
		 * Called when a configured event occurs
		 *
		 * @param evented The widget that the event actually fired on
		 * @param root The root widget being listened to
		 * @param event The event that occurred
		 */
		void eventOccurred(W evented, W root, ObservableValueEvent<?> event);
	}

	/**
	 * Allows for {@link CompoundListener.CompoundListenerBuilder#when(Predicate, Consumer) conditions}
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

	/** Builds a compound listener */
	public interface CompoundListenerBuilder<W extends QuickDefinedWidget> {
		/**
		 * @param attr The attribute to accept in the element(s) that this listener applies to
		 * @return The listener for chaining
		 */
		default CompoundListenerBuilder<W> accept(QuickAttribute<?> attr) {
			return accept(attr, false, null);
		}

		/**
		 * @param <A> The type of the attribute to accept
		 * @param <V> The type of the initial value to set for the attribute
		 * @param attr The attribute to accept in the element(s) that this listener applies to
		 * @param value The initial value for the attribute (if it is not already set)
		 * @return The listener for chaining
		 */
		default <A, V extends A> CompoundListenerBuilder<W> accept(QuickAttribute<A> attr, V value) {
			return accept(attr, false, value);
		}

		/**
		 * @param attr The attribute to require in the element(s) that this listener applies to
		 * @return The listener for chaining
		 */
		default CompoundListenerBuilder<W> require(QuickAttribute<?> attr) {
			return accept(attr, true, null);
		}

		/**
		 * @param <A> The type of the attribute to accept
		 * @param <V> The type of the initial value to set for the attribute
		 * @param attr The attribute to require in the element(s) that this listener applies to
		 * @param value The initial value for the attribute (if it is not already set)
		 * @return The listener for chaining
		 */
		default <A, V extends A> CompoundListenerBuilder<W> require(QuickAttribute<A> attr, V value) {
			return accept(attr, true, value);
		}

		/**
		 * @param <A> The type of the attribute to accept
		 * @param <V> The type of the initial value to set for the attribute
		 * @param attr The attribute to accept in the element(s) that this listener applies to
		 * @param required Whether the attribute should be required or just accepted
		 * @param value The initial value for the attribute (if it is not already set)
		 * @return The listener for chaining
		 */
		<A, V extends A> CompoundListenerBuilder<W> accept(QuickAttribute<A> attr, boolean required, V value);

		/**
		 * A utility method for accepting multiple attributes at once
		 *
		 * @param attrs The attributes to accept
		 * @return The listener for chaining
		 */
		default CompoundListenerBuilder<W> acceptAll(QuickAttribute<?>... attrs) {
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
		default CompoundListenerBuilder<W> requireAll(QuickAttribute<?>... attrs) {
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
		CompoundListenerBuilder<W> watch(StyleAttribute<?> attr);

		/**
		 * @param domain The style domain to watch
		 * @return The listener for chaining
		 */
		default CompoundListenerBuilder<W> watchAll(StyleDomain domain) {
			for (StyleAttribute<?> attr : domain)
				watch(attr);
			return this;
		}

		/**
		 * @param attrs The style attributes to watch
		 * @return The listener for chaining
		 */
		default CompoundListenerBuilder<W> watchAll(StyleAttribute<?>... attrs) {
			for (StyleAttribute<?> attr : attrs)
				watch(attr);
			return this;
		}

		/**
		 * @param builder A function to build a listener to be applied to all children of elements passed to
		 *        {@link CompoundListener#listen(QuickDefinedWidget, QuickDefinedWidget, Observable)}
		 * @return This builder, for chaining
		 */
		CompoundListenerBuilder<W> child(Consumer<CompoundListenerBuilder<W>> builder);

		/**
		 * @param test The condition evaluator
		 * @param builder A function to build a listener to be applied to elements passed to
		 *        {@link CompoundListener#listen(QuickDefinedWidget, QuickDefinedWidget, Observable)} when they pass the given test
		 * @return This builder, for chaining
		 */
		CompoundListenerBuilder<W> when(Predicate<ElementMock> test, Consumer<CompoundListenerBuilder<W>> builder);

		/**
		 * @param run The runnable to execute when any attribute in the current chain changes
		 * @return The listener for chaining
		 */
		default CompoundListenerBuilder<W> onChange(Runnable run) {
			return onEvent((element, root, event) -> run.run());
		}

		/**
		 * @param listener The listener to call when any attribute in the current chain changes
		 * @return The listener for chaining
		 */
		CompoundListenerBuilder<W> onEvent(EventListener<W> listener);

		/** @return The configured listener */
		CompoundListener<W> build();
	}

	/**
	 * @param <W> The type of the widget
	 * @return A builder to make compound listeners
	 */
	public static <W extends QuickDefinedWidget> CompoundListenerBuilder<W> build() {
		return new CompoundListenerImpl.ElementListenerBuilder<>();
	}
}
