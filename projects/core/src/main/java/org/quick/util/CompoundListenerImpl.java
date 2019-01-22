package org.quick.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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

	static class ElementListenerBuilder implements CompoundListenerBuilder {
		private final List<AttributeAccept<?>> theAttributes;
		private final List<StyleAttribute<?>> theStyleAttributes;
		private final List<EventListener> theEventListeners;
		private final List<CompoundListener> theSubListeners;

		ElementListenerBuilder() {
			theAttributes = new ArrayList<>();
			theStyleAttributes = new ArrayList<>();
			theEventListeners = new ArrayList<>();
			theSubListeners = new ArrayList<>();
		}

		@Override
		public <A, V extends A> CompoundListenerBuilder accept(QuickAttribute<A> attr, boolean required, V value) {
			theAttributes.add(new AttributeAccept<>(attr, required, value));
			return this;
		}

		@Override
		public CompoundListenerBuilder watch(StyleAttribute<?> attr) {
			theStyleAttributes.add(attr);
			return this;
		}

		@Override
		public CompoundListenerBuilder child(Consumer<CompoundListenerBuilder> builder) {
			ElementListenerBuilder childBuilder = new ElementListenerBuilder();
			builder.accept(childBuilder);
			theSubListeners.add(new ChildListener(childBuilder.build()));
			return this;
		}

		@Override
		public CompoundListenerBuilder when(Predicate<ElementMock> test, Consumer<CompoundListenerBuilder> builder) {
			ElementListenerBuilder condBuilder = new ElementListenerBuilder();
			builder.accept(condBuilder);
			theSubListeners.add(new ConditionalListener(test, condBuilder.build()));
			return this;
		}

		@Override
		public CompoundListenerBuilder onEvent(EventListener listener) {
			theEventListeners.add(listener);
			return this;
		}

		@Override
		public CompoundListener build() {
			return new ElementListener(theAttributes, theStyleAttributes, theEventListeners, theSubListeners);
		}
	}

	private static class ElementListener implements CompoundListener {
		private final List<AttributeAccept<?>> theAttributes;
		private final List<StyleAttribute<?>> theStyleAttributes;
		private final List<EventListener> theEventListeners;
		private final List<CompoundListener> theSubListeners;
		private final Map<QuickElement, ElementListening> theElementAttributeAccepts;

		ElementListener(List<AttributeAccept<?>> attrs, List<StyleAttribute<?>> styles, List<EventListener> listeners,
			List<CompoundListener> subs) {
			theAttributes = Collections.unmodifiableList(new ArrayList<>(attrs));
			theStyleAttributes = Collections.unmodifiableList(new ArrayList<>(styles));
			theEventListeners = Collections.unmodifiableList(new ArrayList<>(listeners));
			theSubListeners = Collections.unmodifiableList(new ArrayList<>(subs));
			theElementAttributeAccepts = new HashMap<>();
		}

		@Override
		public void listen(QuickElement element, QuickElement root, Observable<?> until) {
			if (theElementAttributeAccepts.containsKey(element))
				return;
			Observer<ObservableValueEvent<?>> events = new Observer<ObservableValueEvent<?>>() {
				@Override
				public <E extends ObservableValueEvent<?>> void onNext(E event) {
					for (EventListener listener : theEventListeners)
						listener.eventOccurred(element, root, event);
				}

				@Override
				public <V extends ObservableValueEvent<?>> void onCompleted(V value) {}
			};
			theElementAttributeAccepts.put(element, new ElementListening(element));
			for (AttributeAccept<?> attr : theAttributes)
				element.atts().get(attr.attr).changes().noInit().takeUntil(until).subscribe(events);
			for (StyleAttribute<?> attr : theStyleAttributes)
				element.getStyle().get(attr).changes().noInit().takeUntil(until).subscribe(events);
			until.take(1).act(evt -> theElementAttributeAccepts.remove(element).reject());
			for (CompoundListener sub : theSubListeners)
				sub.listen(element, root, until);
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

	private static class ChildListener implements CompoundListener {
		private final CompoundListener theElementListener;

		ChildListener(CompoundListener elListener) {
			theElementListener = elListener;
		}

		@Override
		public void listen(QuickElement element, QuickElement root, Observable<?> until) {
			Map<ElementId, SimpleObservable<Void>> childSubs = new HashMap<>();
			element.ch().subscribe(evt -> {
				switch (evt.getType()) {
				case add:
					SimpleObservable<Void> sub = new SimpleObservable<>(null, false, false);
					childSubs.put(evt.getElementId(), sub);
					theElementListener.listen(evt.getNewValue(), root, until == null ? sub : Observable.or(until, sub));
					break;
				case remove:
					childSubs.remove(evt.getElementId()).onNext(null);
					break;
				case set:
					if (evt.getOldValue() != evt.getNewValue()) {
						sub = childSubs.get(evt.getElementId());
						sub.onNext(null);
						theElementListener.listen(evt.getNewValue(), root, until == null ? sub : Observable.or(until, sub));
					}
					break;
				}
			}, true);
		}
	}

	/** A listener that applies another listener when a condition is met */
	private static class ConditionalListener implements CompoundListener {
		private final Predicate<ElementMock> theCondition;
		private final CompoundListener theElementListener;

		ConditionalListener(Predicate<ElementMock> condition, CompoundListener elListener) {
			theCondition = condition;
			theElementListener = elListener;
		}

		@Override
		public void listen(QuickElement element, QuickElement root, Observable<?> until) {
			ElementMockImpl mock = new ElementMockImpl(element, root, until, theElementListener, theCondition);
			mock.start();
		}
	}

	private static class ElementMockImpl implements ElementMock {
		private final QuickElement theElement;
		private final QuickElement theRoot;
		private final SimpleObservable<Void> theChangeObservable;
		private final Observable<?> theUntil;
		private final CompoundListener theListener;
		private final Predicate<ElementMock> theCondition;

		ElementMockImpl(QuickElement element, QuickElement root, Observable<?> until, CompoundListener listener,
			Predicate<ElementMock> condition) {
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
				theListener.listen(theElement, theRoot, theUntil);
		}

		void start() {
			boolean active = theCondition.test(this);
			if (active)
				theListener.listen(theElement, theRoot, theUntil);
		}
	}
}
