package org.quick.test;

import org.observe.SettableValue;
import org.quick.core.QuickContainer;
import org.quick.core.QuickElement;
import org.quick.core.QuickTemplate;
import org.quick.core.QuickTemplate.AttachPoint;
import org.quick.core.tags.Template;

@Template(location = "template1.qts")
public class Templated1 extends QuickTemplate {
	public Templated1() {}

	@Override
	public <E extends QuickElement> QuickContainer<E> getContainer(AttachPoint<E> attach) throws IllegalArgumentException {
		return super.getContainer(attach);
	}

	@Override
	public <E extends QuickElement> SettableValue<E> getElement(AttachPoint<E> attach) {
		return super.getElement(attach);
	}
}