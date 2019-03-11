package org.quick.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.observe.Observable;
import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.SimpleObservable;
import org.qommons.collect.ElementId;
import org.quick.core.QuickElement;
import org.quick.core.mgr.AttributeManager2;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.style.StyleAttribute;
import org.quick.util.CompoundListener.CompoundListenerBuilder;
import org.quick.util.CompoundListener.ElementMock;
import org.quick.util.CompoundListener.EventListener;

class CompoundListenerImpl {
	private static class AttributeAccept<T> {
		final QuickAttribute<T> attr;
		final boolean required;
		final T init;

		AttributeAccept(QuickAttribute<T> attr, boolean required, T init) {
			this.attr = attr;
			this.required = required;
			this.init = init;
		}

		@Override
		public String toString() {
			StringBuilder str = new StringBuilder(attr.getName()).append('(');
			if (init != null)
				str.append(init).append(", ");
			str.append(required ? "required" : "optional");
			str.append(')');
			return str.toString();
		}
	}

	static class ElementListenerBuilder<W> implements CompoundListenerBuilder<W> {
		private final Function<? super W, ? extends QuickElement> theElementGetter;
		private final BiFunction<W, ? super QuickElement, ? extends W> theChildGetter;
		private final List<AttributeAccept<?>> theAttributes;
		private final List<StyleAttribute<?>> theStyleAttributes;
		private final List<EventListener<W>> theEventListeners;
		private final List<CompoundListener<W>> theSubListeners;

		ElementListenerBuilder(Function<? super W, ? extends QuickElement> element,
			BiFunction<W, ? super QuickElement, ? extends W> childGetter) {
			theElementGetter = element;
			theChildGetter = childGetter;
			theAttributes = new ArrayList<>();
			theStyleAttributes = new ArrayList<>();
			theEventListeners = new ArrayList<>();
			theSubListeners = new ArrayList<>();
		}

		@Override
		public <A, V extends A> CompoundListenerBuilder<W> accept(QuickAttribute<A> attr, boolean required, V value) {
			theAttributes.add(new AttributeAccept<>(attr, required, value));
			return this;
		}

		@Override
		public CompoundListenerBuilder<W> watch(StyleAttribute<?> attr) {
			theStyleAttributes.add(attr);
			return this;
		}

		@Override
		public CompoundListenerBuilder<W> child(Consumer<CompoundListenerBuilder<W>> builder) {
			ElementListenerBuilder<W> childBuilder = new ElementListenerBuilder<>(theElementGetter, theChildGetter);
			builder.accept(childBuilder);
			theSubListeners.add(new ChildListener<>(childBuilder.build(), theElementGetter, theChildGetter));
			return this;
		}

		@Override
		public CompoundListenerBuilder<W> when(Predicate<ElementMock> test, Consumer<CompoundListenerBuilder<W>> builder) {
			ElementListenerBuilder<W> condBuilder = new ElementListenerBuilder<>(theElementGetter, theChildGetter);
			builder.accept(condBuilder);
			theSubListeners.add(new ConditionalListener<>(test, theElementGetter, condBuilder.build()));
			return this;
		}

		@Override
		public CompoundListenerBuilder<W> onEvent(EventListener<W> listener) {
			theEventListeners.add(listener);
			return this;
		}

		@Override
		public CompoundListener<W> build() {
			return new ElementListener<>(theElementGetter, theAttributes, theStyleAttributes, theEventListeners, theSubListeners);
		}
	}

	private static class ElementListener<W> implements CompoundListener<W> {
		private final Function<? super W, ? extends QuickElement> theElementGetter;
		private final List<AttributeAccept<?>> theAttributes;
		private final List<StyleAttribute<?>> theStyleAttributes;
		private final List<EventListener<W>> theEventListeners;
		private final List<CompoundListener<W>> theSubListeners;
		private final Map<QuickElement, ElementListening> theElementAttributeAccepts;

		ElementListener(Function<? super W, ? extends QuickElement> element, List<AttributeAccept<?>> attrs, List<StyleAttribute<?>> styles,
			List<EventListener<W>> listeners, List<CompoundListener<W>> subs) {
			theElementGetter = element;
			theAttributes = Collections.unmodifiableList(new ArrayList<>(attrs));
			theStyleAttributes = Collections.unmodifiableList(new ArrayList<>(styles));
			theEventListeners = Collections.unmodifiableList(new ArrayList<>(listeners));
			theSubListeners = Collections.unmodifiableList(new ArrayList<>(subs));
			theElementAttributeAccepts = new HashMap<>();
		}

		@Override
		public void listen(W widget, W root, Observable<?> until) {
			if (theElementAttributeAccepts.containsKey(widget))
				return;
			Observer<ObservableValueEvent<?>> events = new Observer<ObservableValueEvent<?>>() {
				@Override
				public <E extends ObservableValueEvent<?>> void onNext(E event) {
					for (EventListener<W> listener : theEventListeners)
						listener.eventOccurred(widget, root, event);
				}

				@Override
				public <V extends ObservableValueEvent<?>> void onCompleted(V value) {}
			};
			theElementAttributeAccepts.put(el(widget), new ElementListening(el(widget)));
			for (AttributeAccept<?> attr : theAttributes)
				el(widget).atts().get(attr.attr).changes().noInit().takeUntil(until).subscribe(events);
			for (StyleAttribute<?> attr : theStyleAttributes)
				el(widget).getStyle().get(attr).changes().noInit().takeUntil(until).subscribe(events);
			until.take(1).act(evt -> theElementAttributeAccepts.remove(widget).reject());
			for (CompoundListener<W> sub : theSubListeners)
				sub.listen(widget, root, until);
		}

		private QuickElement el(W widget) {
			return theElementGetter.apply(widget);
		}

		private class ElementListening {
			final AttributeManager2.AttributeAcceptance[] theAccepts;

			ElementListening(QuickElement element) {
				theAccepts = new AttributeManager2.AttributeAcceptance[theAttributes.size()];
				for (int i = 0; i < theAttributes.size(); i++) {
					accept(element, i, theAttributes.get(i));
				}
			}

			private <T> void accept(QuickElement element, int i, AttributeAccept<T> attr) {
				element.atts().accept(attr.attr, ElementListener.this, accept -> {
					theAccepts[i] = accept;
					accept.required(attr.required);
					if (attr.init != null) {
						try {
							accept.init(attr.init);
						} catch (IllegalArgumentException e) {
							element.msg().error("Initial value for attribute " + attr.attr + " is unacceptable", e);
						}
					}
				});
			}

			void reject() {
				for (AttributeManager2.AttributeAcceptance accept : theAccepts)
					if (accept != null)
						accept.reject();
			}
		}
	}

	private static class ChildListener<W> implements CompoundListener<W> {
		private final CompoundListener<W> theElementListener;
		private final Function<? super W, ? extends QuickElement> theElementGetter;
		private final BiFunction<W, ? super QuickElement, ? extends W> theChildGetter;

		ChildListener(CompoundListener<W> elListener, Function<? super W, ? extends QuickElement> elementGetter,
			BiFunction<W, ? super QuickElement, ? extends W> childGetter) {
			theElementListener = elListener;
			theElementGetter = elementGetter;
			theChildGetter = childGetter;
		}

		@Override
		public void listen(W widget, W root, Observable<?> until) {
			Map<ElementId, SimpleObservable<Void>> childSubs = new HashMap<>();
			theElementGetter.apply(widget).ch().subscribe(evt -> {
				switch (evt.getType()) {
				case add:
					SimpleObservable<Void> sub = new SimpleObservable<>(null, false, false);
					childSubs.put(evt.getElementId(), sub);
					theElementListener.listen(theChildGetter.apply(widget, evt.getNewValue()), root,
						until == null ? sub : Observable.or(until, sub));
					break;
				case remove:
					childSubs.remove(evt.getElementId()).onNext(null);
					break;
				case set:
					if (evt.getOldValue() != evt.getNewValue()) {
						sub = childSubs.get(evt.getElementId());
						sub.onNext(null);
						theElementListener.listen(theChildGetter.apply(widget, evt.getNewValue()), root,
							until == null ? sub : Observable.or(until, sub));
					}
					break;
				}
			}, true);
		}
	}

	/** A listener that applies another listener when a condition is met */
	private static class ConditionalListener<W> implements CompoundListener<W> {
		private final Predicate<ElementMock> theCondition;
		private final Function<? super W, ? extends QuickElement> theElementGetter;
		private final CompoundListener<W> theElementListener;

		ConditionalListener(Predicate<ElementMock> condition, Function<? super W, ? extends QuickElement> elementGetter,
			CompoundListener<W> elListener) {
			theCondition = condition;
			theElementGetter = elementGetter;
			theElementListener = elListener;
		}

		@Override
		public void listen(W widget, W root, Observable<?> until) {
			ElementMockImpl<W> mock = new ElementMockImpl<>(widget, theElementGetter.apply(widget), root, until, theElementListener,
				theCondition);
			mock.start();
		}
	}

	private static class ElementMockImpl<W> implements ElementMock {
		private final W theWidget;
		private final QuickElement theElement;
		private final W theRoot;
		private final SimpleObservable<Void> theChangeObservable;
		private final Observable<?> theUntil;
		private final CompoundListener<W> theListener;
		private final Predicate<ElementMock> theCondition;

		ElementMockImpl(W widget, QuickElement element, W root, Observable<?> until, CompoundListener<W> listener,
			Predicate<ElementMock> condition) {
			theWidget = widget;
			theElement = element;
			theRoot = root;
			theChangeObservable = new SimpleObservable<>();
			theUntil = Observable.or(until, theChangeObservable);
			theListener = listener;
			theCondition = condition;
		}

		@Override
		public <T> T getAttribute(QuickAttribute<T> attr) {
			ObservableValue<T> value = theElement.atts().get(attr);
			Observable<?> onChange = value.changes().noInit().takeUntil(theUntil);
			onChange.act(evt -> {
				changed();
			});
			return value.get();
		}

		@Override
		public <T> T getStyle(StyleAttribute<T> attr) {
			ObservableValue<T> value = theElement.getStyle().get(attr);
			Observable<?> onChange = value.changes().noInit().takeUntil(theUntil);
			onChange.act(evt -> {
				changed();
			});
			return value.get();
		}

		private synchronized void changed() {
			theChangeObservable.onNext(null);
			boolean active = theCondition.test(this);
			if (active)
				theListener.listen(theWidget, theRoot, theUntil);
		}

		void start() {
			boolean active = theCondition.test(this);
			if (active)
				theListener.listen(theWidget, theRoot, theUntil);
		}
	}
}
