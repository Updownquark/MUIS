package org.quick.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.quick.core.layout.LayoutAttributes.*;

import org.junit.Test;
import org.observe.SimpleObservable;
import org.observe.collect.ObservableList;
import org.quick.core.QuickElement;
import org.quick.core.QuickException;
import org.quick.core.layout.Region;
import org.quick.core.mgr.ElementList;
import org.quick.core.style.LengthUnit;
import org.quick.core.style.Size;
import org.quick.util.CompoundListener;

public class CompoundListenerTest {
	@Test
	public void testBasic() {
		int[] events = new int[1];
		CompoundListener listener = CompoundListener.build()//
			.accept(region).onChange(() -> {
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
		until.onNext(null);
		assertNull(testEl.atts().getHolder(region));
		try {
			testEl.atts().set(region, Region.bottom);
			assertTrue("Should have thrown a QuickException", false);
		} catch (QuickException e) {
		}
	}

	@Test
	public void testChild() {
		int[] events = new int[1];
		CompoundListener listener = CompoundListener.build()//
			.child(childBuilder -> {
				childBuilder.accept(region).onChange(() -> {
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
	}

	@Test
	public void testCondition() {
		int[] events = new int[1];
		CompoundListener listener = CompoundListener.build()//
			.child(builder -> {
				builder.accept(region).onChange(() -> {
					events[0]++;
				});
				builder.when(el -> el.getAttribute(region) == Region.left, builder2 -> {
					builder2.acceptAll(width, right).onChange(() -> {
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
		assertEquals(0, events[0]);
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
		assertEquals(1, events[0]);
		assertNotNull(firstChild.atts().getHolder(width));
		assertNull(firstChild.atts().getHolder(height));
		assertNotNull(firstChild.atts().getHolder(right));
		assertNull(firstChild.atts().getHolder(left));
		assertNull(firstChild.atts().getHolder(top));
		assertNull(firstChild.atts().getHolder(bottom));

		QuickElement secondChild = new QuickElement() {};
		ch.add(secondChild);
		assertNotNull(secondChild.atts().getHolder(region));
		assertEquals(1, events[0]);
		try {
			secondChild.atts().set(region, Region.top);
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(2, events[0]);
		assertNull(secondChild.atts().getHolder(width));
		assertNotNull(secondChild.atts().getHolder(height));
		assertNull(secondChild.atts().getHolder(right));
		assertNull(secondChild.atts().getHolder(left));
		assertNull(secondChild.atts().getHolder(top));
		assertNotNull(secondChild.atts().getHolder(bottom));

		try {
			secondChild.atts().set(region, Region.right);
		} catch (QuickException e) {
			throw new IllegalStateException(e);
		}
		assertEquals(3, events[0]);
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
		assertEquals(4, events[0]);
	}
}
