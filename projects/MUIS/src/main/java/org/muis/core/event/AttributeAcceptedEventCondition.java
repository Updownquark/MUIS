package org.muis.core.event;

import org.muis.core.event.boole.TPAnd;
import org.muis.core.event.boole.TypedPredicate;

/** Allows advanced filtering on {@link AttributeAcceptedEvent}s */
public class AttributeAcceptedEventCondition implements MuisEventCondition<AttributeAcceptedEvent> {
	/** Filters all {@link AttributeAcceptedEvent}s */
	public static final TypedPredicate<MuisEvent, AttributeAcceptedEvent> base = new TypedPredicate<MuisEvent, AttributeAcceptedEvent>() {
		@Override
		public AttributeAcceptedEvent cast(MuisEvent value) {
			return value instanceof AttributeAcceptedEvent ? (AttributeAcceptedEvent) value : null;
		}
	};

	/** Filters {@link AttributeAcceptedEvent}s on their {@link AttributeAcceptedEvent#isAccepted() accepted} attribute */
	public static class AttributeAcceptPredicate implements TypedPredicate<AttributeAcceptedEvent, AttributeAcceptedEvent> {
		private final boolean isAccepted;

		private AttributeAcceptPredicate(boolean accept) {
			isAccepted = accept;
		}

		@Override
		public AttributeAcceptedEvent cast(AttributeAcceptedEvent value) {
			return value.isAccepted() == isAccepted ? value : null;
		}
	}

	/** Filters {@link AttributeAcceptedEvent}s on their {@link AttributeAcceptedEvent#isRequired() required} attribute */
	public static class AttributeRequiredPredicate implements TypedPredicate<AttributeAcceptedEvent, AttributeAcceptedEvent> {
		private final boolean isRequired;

		private AttributeRequiredPredicate(boolean required) {
			isRequired = required;
		}

		@Override
		public AttributeAcceptedEvent cast(AttributeAcceptedEvent value) {
			return value.isRequired() == isRequired ? value : null;
		}
	}

	/** Filters {@link AttributeAcceptedEvent}s that are accept events */
	public static final AttributeAcceptPredicate predicateAccept = new AttributeAcceptPredicate(true);
	/** Filters {@link AttributeAcceptedEvent}s that are reject events */
	public static final AttributeAcceptPredicate predicateReject = new AttributeAcceptPredicate(false);
	/** Filters {@link AttributeAcceptedEvent}s that are accept events for required attributes */
	public static final AttributeRequiredPredicate predicateRequired = new AttributeRequiredPredicate(true);
	/** Filters {@link AttributeAcceptedEvent}s that are accept events for optional attributes */
	public static final AttributeRequiredPredicate predicateOptional = new AttributeRequiredPredicate(false);

	/** Filters all {@link AttributeAcceptedEvent}s */
	public static final AttributeAcceptedEventCondition attAccept = new AttributeAcceptedEventCondition();

	private Boolean isAccepted;
	private Boolean isRequired;

	private AttributeAcceptedEventCondition() {
		isAccepted = null;
		isRequired = null;
	}

	@Override
	public TypedPredicate<MuisEvent, AttributeAcceptedEvent> getTester() {
		TypedPredicate<MuisEvent, AttributeAcceptedEvent> ret = base;
		if(isAccepted != null)
			ret = new TPAnd<>(ret, isAccepted ? predicateAccept : predicateReject);
		if(isRequired != null)
			ret = new TPAnd<>(ret, isRequired ? predicateRequired : predicateOptional);
		return ret;
	}

	/** @return A filter for accept events for required or optional attributes */
	public AttributeAcceptedEventCondition accept() {
		if(Boolean.TRUE.equals(isAccepted) && Boolean.FALSE.equals(isRequired))
			return this;
		AttributeAcceptedEventCondition ret = clone();
		ret.isAccepted = true;
		ret.isRequired = false;
		return ret;
	}

	/** @return A filter for reject events */
	public AttributeAcceptedEventCondition reject() {
		if(Boolean.FALSE.equals(isAccepted) && Boolean.FALSE.equals(isRequired))
			return this;
		AttributeAcceptedEventCondition ret = clone();
		ret.isAccepted = false;
		ret.isRequired = false;
		return ret;
	}

	/** @return A filter for accept events for required attributes */
	public AttributeAcceptedEventCondition required() {
		if(Boolean.TRUE.equals(isAccepted) && Boolean.TRUE.equals(isRequired))
			return this;
		AttributeAcceptedEventCondition ret = clone();
		ret.isAccepted = true;
		ret.isRequired = true;
		return ret;
	}

	/** @return A filter for accept events for optional attributes */
	public AttributeAcceptedEventCondition optional() {
		if(Boolean.TRUE.equals(isAccepted) && Boolean.FALSE.equals(isRequired))
			return this;
		AttributeAcceptedEventCondition ret = clone();
		ret.isAccepted = true;
		ret.isRequired = false;
		return ret;
	}

	@Override
	protected AttributeAcceptedEventCondition clone() {
		try {
			return (AttributeAcceptedEventCondition) super.clone();
		} catch(CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}
}
