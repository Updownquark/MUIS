package org.quick.core.style;

import java.util.Objects;

import org.quick.core.prop.QuickProperty;

public class AppliedStyleModifierProperty<T> {
	private final StyleModifierProperty<T> theProperty;
	private final String theInstanceName;
	private final QuickProperty<T> theTarget;

	public AppliedStyleModifierProperty(StyleModifierProperty<T> property, String instanceName, QuickProperty<T> target) {
		theProperty = property;
		theInstanceName = instanceName;
		theTarget = target;
	}

	public StyleModifierProperty<T> getProperty() {
		return theProperty;
	}

	public String getInstanceName() {
		return theInstanceName;
	}

	public QuickProperty<T> getTarget() {
		return theTarget;
	}

	@Override
	public int hashCode() {
		return Objects.hash(theProperty, theInstanceName, theTarget);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof AppliedStyleModifierProperty))
			return false;
		AppliedStyleModifierProperty<?> prop = (AppliedStyleModifierProperty<?>) obj;
		return prop.theProperty.equals(theProperty) && Objects.equals(theInstanceName, prop.theInstanceName)
			&& theTarget.equals(prop.theTarget);
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(theProperty);
		if (theInstanceName != null)
			str.append(" (").append(theInstanceName).append(')');
		str.append("->").append(theTarget);
		return str.toString();
	}
}
