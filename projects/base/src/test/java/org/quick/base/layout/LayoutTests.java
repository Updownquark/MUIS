package org.quick.base.layout;

import static org.junit.Assert.assertEquals;
import static org.quick.core.layout.LayoutAttributes.*;
import static org.quick.core.style.LengthUnit.lexips;
import static org.quick.core.style.LengthUnit.percent;
import static org.quick.core.style.LengthUnit.pixels;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.observe.Observable;
import org.quick.core.*;
import org.quick.core.layout.SimpleSizeGuide;
import org.quick.core.layout.SizeGuide;
import org.quick.core.style.Position;
import org.quick.core.style.Size;

public class LayoutTests {
	static class TestLayout implements QuickLayout {
		private SizeGuide theWSizer;
		private SizeGuide theHSizer;

		public TestLayout setW(SizeGuide wSizer) {
			theWSizer = wSizer;
			return this;
		}

		public TestLayout setH(SizeGuide hSizer) {
			theHSizer = hSizer;
			return this;
		}

		@Override
		public void install(QuickElement parent, Observable<?> until) {}

		@Override
		public SizeGuide getWSizer(QuickElement parent, QuickElement[] children) {
			return theWSizer;
		}

		@Override
		public SizeGuide getHSizer(QuickElement parent, QuickElement[] children) {
			return theHSizer;
		}

		@Override
		public void layout(QuickElement parent, QuickElement[] children) {}
	}

	@Test
	public void testSimpleLayout() throws QuickException {
		QuickDocument doc = org.quick.QuickTestUtils.createDocument();
		LayoutContainer parent = new LayoutContainer() {
			@Override
			protected QuickLayout getDefaultLayout() {
				return new SimpleLayout();
			}
		};
		parent.init(doc, null, doc.cv(), null, null, null);
		TestLayout child1Layout = new TestLayout().setW(new SimpleSizeGuide(10, 20, 30, 40, 50));
		LayoutContainer child1 = new LayoutContainer() {
			@Override
			protected QuickLayout getDefaultLayout() {
				return child1Layout;
			}
		};
		child1.init(doc, null, doc.cv(), parent, null, null);
		child1.initChildren(Collections.emptyList());
		parent.initChildren(Arrays.asList(child1));
		parent.postCreate();
		assertEquals(10, parent.getWSizer().getMin(100, false));
		assertEquals(20, parent.getWSizer().getMinPreferred(100, false));
		assertEquals(30, parent.getWSizer().getPreferred(100, false));
		assertEquals(40, parent.getWSizer().getMaxPreferred(100, false));
		assertEquals(Integer.MAX_VALUE, parent.getWSizer().getMax(100, false));

		child1.atts()//
			.set(left, new Position(20, pixels))//
			.set(right, new Position(30, pixels));
		assertEquals(30, parent.getWSizer().getMin(100, false));
		assertEquals(30, parent.getWSizer().getMinPreferred(100, false));
		assertEquals(30, parent.getWSizer().getPreferred(100, false));
		assertEquals(30, parent.getWSizer().getMaxPreferred(100, false));
		assertEquals(Integer.MAX_VALUE, parent.getWSizer().getMax(100, false));

		child1.atts()//
			.set(right, new Position(30, lexips));
		assertEquals(60, parent.getWSizer().getMin(100, false));
		assertEquals(70, parent.getWSizer().getMinPreferred(100, false));
		assertEquals(80, parent.getWSizer().getPreferred(100, false));
		assertEquals(90, parent.getWSizer().getMaxPreferred(100, false));
		assertEquals(100, parent.getWSizer().getMax(100, false));

		child1.atts()//
			.set(minWidth, new Size(30, pixels));
		assertEquals(80, parent.getWSizer().getMin(100, false));
		assertEquals(80, parent.getWSizer().getMinPreferred(100, false));
		assertEquals(80, parent.getWSizer().getPreferred(100, false));
		assertEquals(90, parent.getWSizer().getMaxPreferred(100, false));
		assertEquals(100, parent.getWSizer().getMax(100, false));

		child1.atts()//
			.set(maxWidth, new Size(40, pixels));
		assertEquals(80, parent.getWSizer().getMin(100, false));
		assertEquals(80, parent.getWSizer().getMinPreferred(100, false));
		assertEquals(80, parent.getWSizer().getPreferred(100, false));
		assertEquals(90, parent.getWSizer().getMaxPreferred(100, false));
		assertEquals(90, parent.getWSizer().getMax(100, false));

		child1.atts()//
			.set(minWidth, null)//
			.set(maxWidth, null)//
			.set(width, new Size(50, percent));
		assertEquals(100, parent.getWSizer().getMin(100, false));
		assertEquals(100, parent.getWSizer().getMinPreferred(100, false));
		assertEquals(100, parent.getWSizer().getPreferred(100, false));
		assertEquals(100, parent.getWSizer().getMaxPreferred(100, false));
		assertEquals(100, parent.getWSizer().getMax(100, false));

		child1.atts()//
			.set(minWidth, new Size(25, percent))//
			.set(maxWidth, null)//
			.set(width, null);
		assertEquals(66, parent.getWSizer().getMin(100, false));
		assertEquals(70, parent.getWSizer().getMinPreferred(100, false));
		assertEquals(80, parent.getWSizer().getPreferred(100, false));
		assertEquals(90, parent.getWSizer().getMaxPreferred(100, false));
		assertEquals(100, parent.getWSizer().getMax(100, false));

		child1.atts()//
			.set(maxWidth, new Size(40, percent))//
			.set(width, null);
		assertEquals(66, parent.getWSizer().getMin(100, false));
		assertEquals(70, parent.getWSizer().getMinPreferred(100, false));
		assertEquals(80, parent.getWSizer().getPreferred(100, false));
		assertEquals(83, parent.getWSizer().getMaxPreferred(100, false));
		assertEquals(83, parent.getWSizer().getMax(100, false));

		child1.atts()//
			.set(minWidth, null)//
			.set(maxWidth, null)//
			.set(left, new Position(50, lexips))//
			.set(right, new Position(80, pixels));
		assertEquals(80, parent.getWSizer().getMin(100, false));
		assertEquals(90, parent.getWSizer().getMinPreferred(100, false));
		assertEquals(100, parent.getWSizer().getPreferred(100, false));
		assertEquals(110, parent.getWSizer().getMaxPreferred(100, false));
		assertEquals(120, parent.getWSizer().getMax(100, false));

		child1.atts()//
			.set(left, new Position(40, lexips))//
			.set(right, new Position(80, pixels));
		assertEquals(80, parent.getWSizer().getMin(100, false));
		assertEquals(80, parent.getWSizer().getMinPreferred(100, false));
		assertEquals(90, parent.getWSizer().getPreferred(100, false));
		assertEquals(100, parent.getWSizer().getMaxPreferred(100, false));
		assertEquals(110, parent.getWSizer().getMax(100, false));
	}
}
