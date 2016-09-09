package org.quick.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.quick.core.layout.LayoutAttributes.*;
import static org.quick.core.style.BackgroundStyle.color;

import org.junit.Test;
import org.observe.SimpleObservable;
import org.observe.collect.ObservableList;
import org.quick.core.QuickElement;
import org.quick.core.QuickException;
import org.quick.core.layout.Region;
import org.quick.core.mgr.ElementList;
import org.quick.core.style.*;
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

		QuickElement testEl = new QuickElement() {};
		SimpleObservable<Void> until = new SimpleObservable<>();
		listener.listen(testEl, testEl, until);

		assertNotNull(testEl.atts().getHolder(region));
		assertEquals(0, events[0]);
		try {
			testEl.atts().set(region, Region.left);
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(1, events[0]);
		try {
			testEl.atts().set(region, Region.right);
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(2, events[0]);
		ImmutableStyle style = org.quick.core.style.ImmutableStyle.build(null).setConstant(color, Colors.blue).build();
		try {
			testEl.atts().set(StyleAttributes.style, style);
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(3, events[0]);

		until.onNext(null);
		assertNull(testEl.atts().getHolder(region));
		try {
			testEl.atts().set(region, Region.bottom);
			assertTrue("Should have thrown a QuickException", false);
		} catch (QuickException e) {
		}
		style = org.quick.core.style.ImmutableStyle.build(null).setConstant(color, Colors.red).build();
		try {
			testEl.atts().set(StyleAttributes.style, style);
		} catch (QuickException e) {
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

		QuickElement testEl = new QuickElement() {
			@Override
			public ElementList<? extends QuickElement> getChildren() {
				return getChildManager();
			}
		};
		ObservableList<QuickElement> ch = (ObservableList<QuickElement>) testEl.ch();
		QuickElement firstChild = new QuickElement() {};
		ch.add(firstChild);
		SimpleObservable<Void> until = new SimpleObservable<>();
		listener.listen(testEl, testEl, until);

		assertNull(testEl.atts().getHolder(region));
		assertNotNull(firstChild.atts().getHolder(region));
		assertEquals(0, events[0]);
		try {
			firstChild.atts().set(region, Region.left);
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(1, events[0]);

		QuickElement secondChild = new QuickElement() {};
		ch.add(secondChild);
		assertNotNull(secondChild.atts().getHolder(region));
		assertEquals(1, events[0]);
		try {
			secondChild.atts().set(region, Region.left);
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(2, events[0]);
		ImmutableStyle style = org.quick.core.style.ImmutableStyle.build(null).setConstant(color, Colors.blue).build();
		try {
			testEl.atts().set(StyleAttributes.style, style);
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(2, events[0]);
		try {
			firstChild.atts().set(StyleAttributes.style, style);
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(3, events[0]);

		until.onNext(null);
		assertNull(firstChild.atts().getHolder(region));
		try {
			firstChild.atts().set(region, Region.bottom);
			assertTrue("Should have thrown a QuickException", false);
		} catch (QuickException e) {
		}
		assertNull(secondChild.atts().getHolder(region));
		try {
			secondChild.atts().set(region, Region.bottom);
			assertTrue("Should have thrown a QuickException", false);
		} catch (QuickException e) {
		}
		style = org.quick.core.style.ImmutableStyle.build(null).setConstant(color, Colors.red).build();
		try {
			firstChild.atts().set(StyleAttributes.style, style);
		} catch (QuickException e) {
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

		QuickElement testEl = new QuickElement() {
			@Override
			public ElementList<? extends QuickElement> getChildren() {
				return getChildManager();
			}
		};
		ObservableList<QuickElement> ch = (ObservableList<QuickElement>) testEl.ch();
		QuickElement firstChild = new QuickElement() {};
		ch.add(firstChild);
		SimpleObservable<Void> until = new SimpleObservable<>();
		listener.listen(testEl, testEl, until);

		assertNull(testEl.atts().getHolder(region));
		assertNotNull(firstChild.atts().getHolder(region));
		assertEquals(correctEvents, events[0]);
		assertNull(firstChild.atts().getHolder(width));
		assertNull(firstChild.atts().getHolder(height));
		assertNull(firstChild.atts().getHolder(right));
		assertNull(firstChild.atts().getHolder(left));
		assertNull(firstChild.atts().getHolder(top));
		assertNull(firstChild.atts().getHolder(bottom));
		try {
			firstChild.atts().set(region, Region.left);
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		correctEvents++;
		assertEquals(correctEvents, events[0]);
		assertNotNull(firstChild.atts().getHolder(width));
		assertNull(firstChild.atts().getHolder(height));
		assertNotNull(firstChild.atts().getHolder(right));
		assertNull(firstChild.atts().getHolder(left));
		assertNull(firstChild.atts().getHolder(top));
		assertNull(firstChild.atts().getHolder(bottom));
		ImmutableStyle style = org.quick.core.style.ImmutableStyle.build(null).setConstant(color, Colors.blue).build();
		try {
			firstChild.atts().set(StyleAttributes.style, style);
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		correctEvents++;
		assertEquals(correctEvents, events[0]);

		QuickElement secondChild = new QuickElement() {};
		ch.add(secondChild);
		assertNotNull(secondChild.atts().getHolder(region));
		assertEquals(correctEvents, events[0]);
		try {
			secondChild.atts().set(region, Region.top);
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		correctEvents++;
		assertEquals(correctEvents, events[0]);
		assertNull(secondChild.atts().getHolder(width));
		assertNotNull(secondChild.atts().getHolder(height));
		assertNull(secondChild.atts().getHolder(right));
		assertNull(secondChild.atts().getHolder(left));
		assertNull(secondChild.atts().getHolder(top));
		assertNotNull(secondChild.atts().getHolder(bottom));
		try {
			secondChild.atts().set(StyleAttributes.style, style);
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(correctEvents, events[0]);

		try {
			secondChild.atts().set(region, Region.right);
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		correctEvents++;
		assertEquals(correctEvents, events[0]);
		assertNotNull(secondChild.atts().getHolder(width));
		assertNull(secondChild.atts().getHolder(height));
		assertNull(secondChild.atts().getHolder(right));
		assertNotNull(secondChild.atts().getHolder(left));
		assertNull(secondChild.atts().getHolder(top));
		assertNull(secondChild.atts().getHolder(bottom));

		try {
			secondChild.atts().set(width, new Size(100, LengthUnit.pixels));
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		correctEvents++;
		assertEquals(correctEvents, events[0]);

		until.onNext(null);
		assertNull(firstChild.atts().getHolder(region));
		try {
			firstChild.atts().set(region, Region.bottom);
			assertTrue("Should have thrown a QuickException", false);
		} catch (QuickException e) {
		}
		assertNull(secondChild.atts().getHolder(region));
		try {
			secondChild.atts().set(region, Region.bottom);
			assertTrue("Should have thrown a QuickException", false);
		} catch (QuickException e) {
		}
		assertNull(firstChild.atts().getHolder(width));
		assertNull(firstChild.atts().getHolder(height));
		assertNull(firstChild.atts().getHolder(right));
		assertNull(firstChild.atts().getHolder(left));
		assertNull(firstChild.atts().getHolder(top));
		assertNull(firstChild.atts().getHolder(bottom));
		style = org.quick.core.style.ImmutableStyle.build(null).setConstant(color, Colors.red).build();
		try {
			firstChild.atts().set(StyleAttributes.style, style);
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(correctEvents, events[0]);
	}
}
