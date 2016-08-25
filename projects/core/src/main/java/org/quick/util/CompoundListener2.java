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

public interface CompoundListener2 {
	public interface EventListener {
		void eventOccurred(QuickElement root, ObservableValueEvent<?> event);
	}

	public interface ElementMock {
		<T> T getAttribute(QuickAttribute<T> attr);

		<T> T getStyle(StyleAttribute<T> attr);
	}

	void doListen(QuickElement element, Observable<?> until);

	public interface CompoundListenerBuilder<B extends CompoundListenerBuilder<B>> {
		/**
		 * @param attr The attribute to accept in the element(s) that this listener applies to
		 * @return The listener for chaining
		 */
		default B accept(QuickAttribute<?> attr) {
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
		default <A, V extends A> B accept(QuickAttribute<A> attr, V value) throws IllegalArgumentException {
			return accept(attr, false, value);
		}

		/**
		 * @param attr The attribute to require in the element(s) that this listener applies to
		 * @return The listener for chaining
		 */
		default B require(QuickAttribute<?> attr) {
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
		default <A, V extends A> B require(QuickAttribute<A> attr, V value) throws IllegalArgumentException {
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
		<A, V extends A> B accept(QuickAttribute<A> attr, boolean required, V value) throws IllegalArgumentException;

		/**
		 * A utility method for accepting multiple attributes at once
		 *
		 * @param attrs The attributes to accept
		 * @return The listener for chaining
		 */
		default B acceptAll(QuickAttribute<?>... attrs) {
			B chain = (B) this;
			for (QuickAttribute<?> attr : attrs)
				chain = require(attr);
			return chain;
		}

		/**
		 * A utility method for requiring multiple attributes at once
		 *
		 * @param attrs The attributes to require
		 * @return The listener for chaining
		 */
		default B requireAll(QuickAttribute<?>... attrs) {
			B chain = (B) this;
			for (QuickAttribute<?> attr : attrs)
				chain = require(attr);
			return chain;
		}

		/**
		 * Watches a style attribute. When the attribute's value changes, any change listeners registered on this listener will fire.
		 *
		 * @param attr The style attribute to listen for
		 * @return The listener for chaining
		 */
		B watch(StyleAttribute<?> attr);

		/**
		 * @param domain The style domain to watch
		 * @return The listener for chaining
		 */
		default B watchAll(StyleDomain domain) {
			B chain = (B) this;
			for (StyleAttribute<?> attr : domain)
				chain = watch(attr);
			return chain;
		}

		/**
		 * @param attrs The style attributes to watch
		 * @return The listener for chaining
		 */
		default B watchAll(StyleAttribute<?>... attrs) {
			B chain = (B) this;
			for (StyleAttribute<?> attr : attrs)
				chain = watch(attr);
			return chain;
		}

		B when(Predicate<ElementMock> test, Consumer<CompoundListenerBuilder<?>> builder);

		/**
		 * @param run The runnable to execute when any attribute in the current chain changes
		 * @return The listener for chaining
		 */
		default B onChange(Runnable run) {
			return onEvent((element, event) -> run.run());
		}

		/**
		 * @param listener The listener to call when any attribute in the current chain changes
		 * @return The listener for chaining
		 */
		B onEvent(EventListener listener);

		CompoundListener2 build();
	}

	public class RootListenerBuilder implements CompoundListenerBuilder<RootListenerBuilder> {

	}

	public abstract class ChainedListener extends CompoundListener2 {}
}
