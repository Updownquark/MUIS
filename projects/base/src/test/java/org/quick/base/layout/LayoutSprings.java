package org.quick.base.layout;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.qommons.IntList;
import org.quick.base.layout.LayoutSpring.SeriesSpring;
import org.quick.base.layout.LayoutSpring.SimpleSpring;

public class LayoutSprings {
	private final float TOL = 1E-9f;

	@Test
	public void testSimpleSpring() {
		SimpleSpring.Builder builder = SimpleSpring.build(10, 10000, 1000);
		builder.with(100, 100, 100);
		try {
			builder.with(90, 10, 10);
			Assert.fail("Should not be allowed");
		} catch (IllegalArgumentException e) {
		}
		try {
			builder.with(110, 110, 110);
			Assert.fail("Should not be allowed");
		} catch (IllegalArgumentException e) {
		}
		try {
			builder.with(110, 10, 11);
			Assert.fail("Should not be allowed");
		} catch (IllegalArgumentException e) {
		}
		builder.with(150, 10, 1);
		builder.with(200, 0, 0);
		builder.with(350, -10, -100);
		builder.with(1000, -1000, -1000);
		SizeTickIterator iter = builder.build().ticks();

		assertEquals(new IntList(new int[] { 10, 100, 150, 200, 350, 1000 }), iter.getAllTicks());
		assertEquals(100, iter.getSize(100));
		assertEquals(350, iter.getSize(-50));
		assertEquals(Integer.MAX_VALUE, iter.getSize(-LayoutSpring.MAX_TENSION));
		assertEquals(0, iter.getSize(LayoutSpring.MAX_TENSION));
		assertEquals(0, iter.getTension(200), 0);
		assertEquals(-10, iter.getTension(350), 0);
		assertEquals(-5, iter.getTension(275), TOL);
		assertEquals(-307.6923, iter.getTension(500), .0001);
		assertEquals(55, iter.getTension(125), TOL);
	}

	@Test
	public void testSeriesSpring() {
		SimpleSpring spring1 = SimpleSpring//
			.build(10, 10000, 1000)//
			.with(100, 100, 100)//
			.with(150, 10, 1)//
			.with(200, 0, 0)//
			.with(350, -10, -100)//
			.with(1000, -1000, -1000)//
			.build();
		SimpleSpring spring2 = SimpleSpring//
			.build(15, 15000, 1500)//
			.with(175, 150, 150)//
			.with(215, 15, 1.5f)//
			.with(400, 0, 0)//
			.with(500, -15, -150)//
			.with(1000, -1500, -1500)//
			.build();

		SizeTickIterator iter = new SeriesSpring(Arrays.asList(spring1, spring2)).ticks();

		assertEquals(new IntList(new int[] { 25, }), iter.getAllTicks());
	}
}
