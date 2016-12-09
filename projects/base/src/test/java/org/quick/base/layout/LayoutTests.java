package org.quick.base.layout;

import org.junit.Test;
import org.quick.core.LayoutContainer;
import org.quick.core.QuickElement;
import org.quick.core.QuickLayout;

public class LayoutTests {
	@Test
	public void testSimpleLayout() {
		LayoutContainer parent = new LayoutContainer() {
			@Override
			protected QuickLayout getDefaultLayout() {
				return new SimpleLayout();
			}
		};
		QuickElement child1 = new QuickElement() {};
	}
}
