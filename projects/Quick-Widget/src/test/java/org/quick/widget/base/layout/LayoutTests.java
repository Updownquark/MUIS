package org.quick.widget.base.layout;

import static org.junit.Assert.assertEquals;
import static org.quick.core.layout.LayoutAttributes.left;
import static org.quick.core.layout.LayoutAttributes.maxWidth;
import static org.quick.core.layout.LayoutAttributes.minWidth;
import static org.quick.core.layout.LayoutAttributes.right;
import static org.quick.core.layout.LayoutAttributes.width;
import static org.quick.core.style.LengthUnit.lexips;
import static org.quick.core.style.LengthUnit.percent;
import static org.quick.core.style.LengthUnit.pixels;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.observe.Observable;
import org.quick.core.LayoutContainer;
import org.quick.core.QuickDocument;
import org.quick.core.QuickException;
import org.quick.core.layout.Orientation;
import org.quick.core.style.Position;
import org.quick.core.style.Size;
import org.quick.widget.core.LayoutContainerWidget;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;
import org.quick.widget.core.layout.QuickWidgetLayout;
import org.quick.widget.core.layout.SimpleSizeGuide;
import org.quick.widget.core.layout.SizeGuide;

/** Tests the layout implementations in base */
public class LayoutTests {
	static class TestLayout implements QuickWidgetLayout {
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
		public void install(QuickWidget parent, Observable<?> until) {}

		@Override
		public SizeGuide getSizer(QuickWidget parent, Iterable<? extends QuickWidget> children, Orientation orientation) {
			return orientation.isVertical() ? theHSizer : theWSizer;
		}

		@Override
		public void layout(QuickWidget parent, List<? extends QuickWidget> children) {}
	}

	/**
	 * Tests {@link SimpleLayout}
	 *
	 * @throws QuickException If an error occurs setting attributes
	 */
	@Test
	public void testSimpleLayout() throws QuickException {
		QuickDocument elDoc = org.quick.QuickTestUtils.createDocument();
		QuickWidgetDocument doc = new QuickWidgetDocument(null, elDoc);
		LayoutContainer parentEl = new LayoutContainer();
		LayoutContainerWidget parent = new LayoutContainerWidget(doc, parentEl, null) {
			@Override
			protected QuickWidgetLayout getDefaultLayout() {
				return new SimpleLayout();
			}
		};
		parentEl.init(doc.getQuickDoc(), null, doc.getQuickDoc().cv(), null, null, null);
		TestLayout child1Layout = new TestLayout().setW(new SimpleSizeGuide(10, 20, 30, 40, 50));
		LayoutContainer child1El = new LayoutContainer();
		child1El.init(doc.getQuickDoc(), null, doc.getQuickDoc().cv(), parentEl, null, null);
		child1El.initChildren(Collections.emptyList());
		parentEl.initChildren(Arrays.asList(child1El));
		parentEl.postCreate();
		LayoutContainerWidget child1 = new LayoutContainerWidget(doc, child1El, parent) {
			@Override
			protected QuickWidgetLayout getDefaultLayout() {
				return child1Layout;
			}
		};
		assertEquals(10, parent.getSizer(Orientation.horizontal).getMin(100, false));
		assertEquals(20, parent.getSizer(Orientation.horizontal).getMinPreferred(100, false));
		assertEquals(30, parent.getSizer(Orientation.horizontal).getPreferred(100, false));
		assertEquals(40, parent.getSizer(Orientation.horizontal).getMaxPreferred(100, false));
		assertEquals(Integer.MAX_VALUE, parent.getSizer(Orientation.horizontal).getMax(100, false));

		child1.getElement().atts()//
			.setValue(left, new Position(20, pixels), null)//
			.setValue(right, new Position(30, pixels), null);
		assertEquals(30, parent.getSizer(Orientation.horizontal).getMin(100, false));
		assertEquals(30, parent.getSizer(Orientation.horizontal).getMinPreferred(100, false));
		assertEquals(30, parent.getSizer(Orientation.horizontal).getPreferred(100, false));
		assertEquals(30, parent.getSizer(Orientation.horizontal).getMaxPreferred(100, false));
		assertEquals(Integer.MAX_VALUE, parent.getSizer(Orientation.horizontal).getMax(100, false));

		child1.getElement().atts()//
			.setValue(right, new Position(30, lexips), null);
		assertEquals(60, parent.getSizer(Orientation.horizontal).getMin(100, false));
		assertEquals(70, parent.getSizer(Orientation.horizontal).getMinPreferred(100, false));
		assertEquals(80, parent.getSizer(Orientation.horizontal).getPreferred(100, false));
		assertEquals(90, parent.getSizer(Orientation.horizontal).getMaxPreferred(100, false));
		assertEquals(100, parent.getSizer(Orientation.horizontal).getMax(100, false));

		child1.getElement().atts()//
			.setValue(minWidth, new Size(30, pixels), null);
		assertEquals(80, parent.getSizer(Orientation.horizontal).getMin(100, false));
		assertEquals(80, parent.getSizer(Orientation.horizontal).getMinPreferred(100, false));
		assertEquals(80, parent.getSizer(Orientation.horizontal).getPreferred(100, false));
		assertEquals(90, parent.getSizer(Orientation.horizontal).getMaxPreferred(100, false));
		assertEquals(100, parent.getSizer(Orientation.horizontal).getMax(100, false));

		child1.getElement().atts()//
			.setValue(maxWidth, new Size(40, pixels), null);
		assertEquals(80, parent.getSizer(Orientation.horizontal).getMin(100, false));
		assertEquals(80, parent.getSizer(Orientation.horizontal).getMinPreferred(100, false));
		assertEquals(80, parent.getSizer(Orientation.horizontal).getPreferred(100, false));
		assertEquals(90, parent.getSizer(Orientation.horizontal).getMaxPreferred(100, false));
		assertEquals(90, parent.getSizer(Orientation.horizontal).getMax(100, false));

		child1.getElement().atts()//
			.setValue(minWidth, null, null)//
			.setValue(maxWidth, null, null)//
			.setValue(width, new Size(50, percent), null);
		assertEquals(100, parent.getSizer(Orientation.horizontal).getMin(100, false));
		assertEquals(100, parent.getSizer(Orientation.horizontal).getMinPreferred(100, false));
		assertEquals(100, parent.getSizer(Orientation.horizontal).getPreferred(100, false));
		assertEquals(100, parent.getSizer(Orientation.horizontal).getMaxPreferred(100, false));
		assertEquals(100, parent.getSizer(Orientation.horizontal).getMax(100, false));

		child1.getElement().atts()//
			.setValue(minWidth, new Size(25, percent), null)//
			.setValue(maxWidth, null, null)//
			.setValue(width, null, null);
		assertEquals(66, parent.getSizer(Orientation.horizontal).getMin(100, false));
		assertEquals(70, parent.getSizer(Orientation.horizontal).getMinPreferred(100, false));
		assertEquals(80, parent.getSizer(Orientation.horizontal).getPreferred(100, false));
		assertEquals(90, parent.getSizer(Orientation.horizontal).getMaxPreferred(100, false));
		assertEquals(100, parent.getSizer(Orientation.horizontal).getMax(100, false));

		child1.getElement().atts()//
			.setValue(maxWidth, new Size(40, percent), null)//
			.setValue(width, null, null);
		assertEquals(66, parent.getSizer(Orientation.horizontal).getMin(100, false));
		assertEquals(70, parent.getSizer(Orientation.horizontal).getMinPreferred(100, false));
		assertEquals(80, parent.getSizer(Orientation.horizontal).getPreferred(100, false));
		assertEquals(83, parent.getSizer(Orientation.horizontal).getMaxPreferred(100, false));
		assertEquals(83, parent.getSizer(Orientation.horizontal).getMax(100, false));

		child1.getElement().atts()//
			.setValue(minWidth, null, null)//
			.setValue(maxWidth, null, null)//
			.setValue(left, new Position(50, lexips), null)//
			.setValue(right, new Position(80, pixels), null);
		assertEquals(80, parent.getSizer(Orientation.horizontal).getMin(100, false));
		assertEquals(90, parent.getSizer(Orientation.horizontal).getMinPreferred(100, false));
		assertEquals(100, parent.getSizer(Orientation.horizontal).getPreferred(100, false));
		assertEquals(110, parent.getSizer(Orientation.horizontal).getMaxPreferred(100, false));
		assertEquals(120, parent.getSizer(Orientation.horizontal).getMax(100, false));

		child1.getElement().atts()//
			.setValue(left, new Position(40, lexips), null)//
			.setValue(right, new Position(80, pixels), null);
		assertEquals(80, parent.getSizer(Orientation.horizontal).getMin(100, false));
		assertEquals(80, parent.getSizer(Orientation.horizontal).getMinPreferred(100, false));
		assertEquals(90, parent.getSizer(Orientation.horizontal).getPreferred(100, false));
		assertEquals(100, parent.getSizer(Orientation.horizontal).getMaxPreferred(100, false));
		assertEquals(110, parent.getSizer(Orientation.horizontal).getMax(100, false));
	}
}
