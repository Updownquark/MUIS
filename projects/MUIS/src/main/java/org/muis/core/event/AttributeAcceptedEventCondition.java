package org.muis.core.event;

import org.muis.core.event.boole.TypedPredicate;

public class AttributeAcceptedEventCondition implements MuisEventCondition<AttributeAcceptedEvent> {
	public static final TypedPredicate<MuisEvent, AttributeAcceptedEvent> base = new TypedPredicate<MuisEvent, AttributeAcceptedEvent>() {
		@Override
		public AttributeAcceptedEvent cast(MuisEvent value) {
			return value instanceof AttributeAcceptedEvent ? (AttributeAcceptedEvent) value : null;
		}
	};

	public static final AttributeAcceptedEventCondition attAccept = new AttributeAcceptedEventCondition();

	private Boolean isAccepted;
	private Boolean isRequired;

	private AttributeAcceptedEventCondition() {
		isAccepted = null;
		isRequired = null;
	}

	@Override
	public TypedPredicate<MuisEvent, AttributeAcceptedEvent> getTester() {
		// TODO Auto-generated method stub
		return null;
	}

	public AttributeAcceptedEventCondition accept() {
		if(Boolean.TRUE.equals(isAccepted))
			return this;
		AttributeAcceptedEventCondition ret = clone();
		ret.isAccepted = true;
		return ret;
	}

	public AttributeAcceptedEventCondition reject() {
		if(Boolean.FALSE.equals(isAccepted))
			return this;
		AttributeAcceptedEventCondition ret = clone();
		ret.isAccepted = false;
		return ret;
	}

	public AttributeAcceptedEventCondition required() {
		if(Boolean.TRUE.equals(isRequired))
			return this;
		AttributeAcceptedEventCondition ret = clone();
		ret.isRequired = true;
		return ret;
	}

	public AttributeAcceptedEventCondition optional() {
		if(Boolean.FALSE.equals(isRequired))
			return this;
		AttributeAcceptedEventCondition ret = clone();
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
