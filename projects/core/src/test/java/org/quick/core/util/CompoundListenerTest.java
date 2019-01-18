package org.quick.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.quick.core.layout.LayoutAttributes.bottom;
import static org.quick.core.layout.LayoutAttributes.height;
import static org.quick.core.layout.LayoutAttributes.left;
import static org.quick.core.layout.LayoutAttributes.region;
import static org.quick.core.layout.LayoutAttributes.right;
import static org.quick.core.layout.LayoutAttributes.top;
import static org.quick.core.layout.LayoutAttributes.width;
import static org.quick.core.style.BackgroundStyle.color;

import org.junit.Test;
import org.observe.SimpleObservable;
import org.observe.collect.ObservableCollection;
import org.quick.QuickTestUtils;
import org.quick.core.QuickDocument;
import org.quick.core.QuickElement;
import org.quick.core.layout.Region;
import org.quick.core.style.Colors;
import org.quick.core.style.ImmutableStyle;
import org.quick.core.style.LengthUnit;
import org.quick.core.style.Size;
import org.quick.core.style.StyleAttributes;
import org.quick.util.CompoundListener;

/** Tests {@link CompoundListener} */
public class CompoundListenerTest {
	/** Tests the basic attribute functionality */
	@Test
	public void testBasic() {
		int[] events = new int[1];
		CompoundListener listener = CompoundListener.build()//
			.accept(region).watch(color).onChange(() -> {
				events[0]++;
			})//
			.build();

		org.quick.core.QuickDocument doc = org.quick.QuickTestUtils.createDocument();
		QuickElement testEl = new QuickElement() {};
		testEl.init(doc, null, doc.cv(), null, null, null);
		SimpleObservable<Void> until = new SimpleObservable<>();
		listener.listen(testEl, testEl, until);

		assertNotNull(testEl.atts().get(region));
		assertEquals(0, events[0]);
		try {
			testEl.atts().get(region).set(Region.left, null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(1, events[0]);
		try {
			testEl.atts().get(region).set(Region.right, null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(2, events[0]);
		ImmutableStyle style = org.quick.core.style.ImmutableStyle.build(null).setConstant(color, Colors.blue).build();
		try {
			testEl.atts().get(StyleAttributes.style).set(style, null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(3, events[0]);

		until.onNext(null);
		assertNull(testEl.atts().get(region));
		try {
			testEl.atts().setValue(region, Region.bottom, null);
			assertTrue("Should have thrown an exception", false);
		} catch (IllegalArgumentException e) {
		}
		style = org.quick.core.style.ImmutableStyle.build(null).setConstant(color, Colors.red).build();
		try {
			testEl.atts().get(StyleAttributes.style).set(style, null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(3, events[0]);
	}

	/** Tests listening for each child under a parent */
	@Test
	public void testChild() {
		int[] events = new int[1];
		CompoundListener listener = CompoundListener.build()//
			.child(childBuilder -> {
				childBuilder.accept(region).watch(color).onChange(() -> {
					events[0]++;
				});
			})//
			.build();

		org.quick.core.QuickDocument doc = org.quick.QuickTestUtils.createDocument();
		QuickElement testEl = new QuickElement() {
			@Override
			public ObservableCollection<? extends QuickElement> getPhysicalChildren() {
				return getChildManager();
			}
		};
		testEl.init(doc, null, doc.cv(), null, null, null);
		ObservableCollection<QuickElement> ch = (ObservableCollection<QuickElement>) testEl.ch();
		QuickElement firstChild = new QuickElement() {};
		firstChild.init(doc, null, doc.cv(), testEl, null, null);
		ch.add(firstChild);
		SimpleObservable<Void> until = new SimpleObservable<>();
		listener.listen(testEl, testEl, until);

		assertNull(testEl.atts().get(region));
		assertNotNull(firstChild.atts().get(region));
		assertEquals(0, events[0]);
		try {
			firstChild.atts().get(region).set(Region.left, null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(1, events[0]);

		QuickElement secondChild = new QuickElement() {};
		secondChild.init(doc, null, doc.cv(), testEl, null, null);
		ch.add(secondChild);
		assertNotNull(secondChild.atts().get(region));
		assertEquals(1, events[0]);
		try {
			secondChild.atts().get(region).set(Region.left, null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(2, events[0]);
		ImmutableStyle style = org.quick.core.style.ImmutableStyle.build(null).setConstant(color, Colors.blue).build();
		try {
			testEl.atts().get(StyleAttributes.style).set(style, null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(2, events[0]);
		try {
			firstChild.atts().get(StyleAttributes.style).set(style, null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(3, events[0]);

		until.onNext(null);
		assertNull(firstChild.atts().get(region));
		try {
			firstChild.atts().setValue(region, Region.bottom, null);
			assertTrue("Should have thrown an exception", false);
		} catch (IllegalArgumentException e) {
		}
		assertNull(secondChild.atts().get(region));
		try {
			secondChild.atts().setValue(region, Region.bottom, null);
			assertTrue("Should have thrown an exception", false);
		} catch (IllegalArgumentException e) {
		}
		style = org.quick.core.style.ImmutableStyle.build(null).setConstant(color, Colors.red).build();
		try {
			firstChild.atts().get(StyleAttributes.style).set(style, null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(3, events[0]);
	}

	/** Tests listening to only each child in a parent that passes a test */
	@Test
	public void testCondition() {
		int[] events = new int[1];
		int correctEvents = 0;
		CompoundListener listener = CompoundListener.build()//
			.child(builder -> {
				builder.accept(region).onChange(() -> {
					events[0]++;
				});
				builder.when(el -> el.getAttribute(region) == Region.left, builder2 -> {
					builder2.acceptAll(width, right).watch(color).onChange(() -> {
						events[0]++;
					});
				});
				builder.when(el -> el.getAttribute(region) == Region.right, builder2 -> {
					builder2.acceptAll(width, left).onChange(() -> {
						events[0]++;
					});
				});
				builder.when(el -> el.getAttribute(region) == Region.top, builder2 -> {
					builder2.acceptAll(height, bottom).onChange(() -> {
						events[0]++;
					});
				});
				builder.when(el -> el.getAttribute(region) == Region.bottom, builder2 -> {
					builder2.acceptAll(height, top).onChange(() -> {
						events[0]++;
					});
				});
			})//
			.build();

		QuickDocument doc = QuickTestUtils.createDocument();
		QuickElement testEl = new QuickElement() {
			@Override
			public ObservableCollection<? extends QuickElement> getPhysicalChildren() {
				return getChildManager();
			}
		};
		testEl.init(doc, null, doc.cv(), null, null, null);
		ObservableCollection<QuickElement> ch = (ObservableCollection<QuickElement>) testEl.ch();
		QuickElement firstChild = new QuickElement() {};
		firstChild.init(doc, null, doc.cv(), testEl, null, null);
		ch.add(firstChild);
		SimpleObservable<Void> until = new SimpleObservable<>();
		listener.listen(testEl, testEl, until);

		assertNull(testEl.atts().get(region));
		assertNotNull(firstChild.atts().get(region));
		assertEquals(correctEvents, events[0]);
		assertNull(firstChild.atts().get(width));
		assertNull(firstChild.atts().get(height));
		assertNull(firstChild.atts().get(right));
		assertNull(firstChild.atts().get(left));
		assertNull(firstChild.atts().get(top));
		assertNull(firstChild.atts().get(bottom));
		try {
			firstChild.atts().get(region).set(Region.left, null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		correctEvents++;
		assertEquals(correctEvents, events[0]);
		assertNotNull(firstChild.atts().get(width));
		assertNull(firstChild.atts().get(height));
		assertNotNull(firstChild.atts().get(right));
		assertNull(firstChild.atts().get(left));
		assertNull(firstChild.atts().get(top));
		assertNull(firstChild.atts().get(bottom));
		ImmutableStyle style = org.quick.core.style.ImmutableStyle.build(null).setConstant(color, Colors.blue).build();
		try {
			firstChild.atts().get(StyleAttributes.style).set(style, null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		correctEvents++;
		assertEquals(correctEvents, events[0]);

		QuickElement secondChild = new QuickElement() {};
		secondChild.init(doc, null, doc.cv(), testEl, null, null);
		ch.add(secondChild);
		assertNotNull(secondChild.atts().get(region));
		assertEquals(correctEvents, events[0]);
		try {
			secondChild.atts().get(region).set(Region.top, null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		correctEvents++;
		assertEquals(correctEvents, events[0]);
		assertNull(secondChild.atts().get(width));
		assertNotNull(secondChild.atts().get(height));
		assertNull(secondChild.atts().get(right));
		assertNull(secondChild.atts().get(left));
		assertNull(secondChild.atts().get(top));
		assertNotNull(secondChild.atts().get(bottom));
		try {
			secondChild.atts().get(StyleAttributes.style).set(style, null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(correctEvents, events[0]);

		try {
			secondChild.atts().get(region).set(Region.right, null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		correctEvents++;
		assertEquals(correctEvents, events[0]);
		assertNotNull(secondChild.atts().get(width));
		assertNull(secondChild.atts().get(height));
		assertNull(secondChild.atts().get(right));
		assertNotNull(secondChild.atts().get(left));
		assertNull(secondChild.atts().get(top));
		assertNull(secondChild.atts().get(bottom));

		try {
			secondChild.atts().get(width).set(new Size(100, LengthUnit.pixels), null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		correctEvents++;
		assertEquals(correctEvents, events[0]);

		until.onNext(null);
		assertNull(firstChild.atts().get(region));
		try {
			firstChild.atts().setValue(region, Region.bottom, null);
			assertTrue("Should have thrown an exception", false);
		} catch (IllegalArgumentException e) {
		}
		assertNull(secondChild.atts().get(region));
		try {
			secondChild.atts().setValue(region, Region.bottom, null);
			assertTrue("Should have thrown an exception", false);
		} catch (IllegalArgumentException e) {
		}
		assertNull(firstChild.atts().get(width));
		assertNull(firstChild.atts().get(height));
		assertNull(firstChild.atts().get(right));
		assertNull(firstChild.atts().get(left));
		assertNull(firstChild.atts().get(top));
		assertNull(firstChild.atts().get(bottom));
		style = org.quick.core.style.ImmutableStyle.build(null).setConstant(color, Colors.red).build();
		try {
			firstChild.atts().get(StyleAttributes.style).set(style, null);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(correctEvents, events[0]);
	}
}
