package org.muis.core.style.sheet;

import org.muis.core.rx.ObservableValue;
import org.muis.core.style.StyleAttribute;

/** Represents a style sheet embedded in or referred to (directly or indirectly) from a MUIS document */
public class ParsedStyleSheet extends MutableAnimatedStyleSheet implements prisms.util.Sealable {
	private boolean isSealed;

	private java.net.URL theLocation;

	@Override
	public boolean isSealed() {
		return isSealed;
	}

	@Override
	public void seal() {
		isSealed = true;
	}

	/** @throws SealedException If this style sheet has been sealed */
	protected void assertUnsealed() throws SealedException {
		if(isSealed)
			throw new SealedException(this);
	}

	/**
	 * @return The location of the resource that this style sheet was parsed from. This information is not guaranteed to be available even
	 *         for style sheets generated by the default parser.
	 */
	public java.net.URL getLocation() {
		return theLocation;
	}

	/** @param location The location of the resource that this style sheet was parsed from */
	public void setLocation(java.net.URL location) {
		assertUnsealed();
		theLocation = location;
	}

	@Override
	public void addVariable(AnimatedVariable var) {
		assertUnsealed();
		super.addVariable(var);
	}

	@Override
	public void removeVariable(AnimatedVariable var) {
		assertUnsealed();
		super.removeVariable(var);
	}

	@Override
	public void startAnimation() {
		assertUnsealed();
		super.startAnimation();
	}

	@Override
	public void stopAnimation() {
		assertUnsealed();
		super.stopAnimation();
	}

	@Override
	public <T> void setAnimatedValue(StyleAttribute<T> attr, StateGroupTypeExpression<?> expr, ObservableValue<? extends T> value) {
		assertUnsealed();
		super.setAnimatedValue(attr, expr, value);
	}

	@Override
	public <T> void set(StyleAttribute<T> attr, StateGroupTypeExpression<?> exp, T value) throws ClassCastException,
		IllegalArgumentException {
		assertUnsealed();
		super.set(attr, exp, value);
	}

	@Override
	public <T> void set(StyleAttribute<T> attr, T value) throws ClassCastException, IllegalArgumentException {
		assertUnsealed();
		super.set(attr, value);
	}

	@Override
	public void clear(StyleAttribute<?> attr) {
		assertUnsealed();
		super.clear(attr);
	}

	@Override
	public void clear(StyleAttribute<?> attr, StateGroupTypeExpression<?> exp) {
		assertUnsealed();
		super.clear(attr, exp);
	}

	@Override
	public String toString() {
		String path = theLocation.getPath();
		int lastSlash = path.lastIndexOf('/');
		if(lastSlash >= 0)
			path = path.substring(lastSlash + 1);
		return path;
	}
}
