package org.muis.base.layout;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.muis.base.util.GeneticSolver;
import org.muis.base.util.GeneticSolver.Genome;
import org.muis.core.MuisElement;

public class GeneticLayout {
	public static interface LayoutScratchPad extends Iterable<MuisElement> {
		public int size();

		public Rectangle get(MuisElement element);
	}

	public static interface LayoutConstraint {
		boolean isViolated(MuisElement container, Dimension size, LayoutScratchPad layout);

		float getViolation(MuisElement container, Dimension size, LayoutScratchPad layout);
	}

	private static class LayoutConstraintHolder {
		final LayoutConstraint constraint;

		final boolean variableCost;

		final float cost;

		LayoutConstraintHolder(LayoutConstraint cnstrnt, boolean vc, float cst) {
			constraint = cnstrnt;
			variableCost = vc;
			cost = cst;
		}
	}

	private ArrayList<LayoutConstraintHolder> theConstraints;

	public GeneticLayout() {
		theConstraints = new ArrayList<>();
	}

	public void addConstraint(LayoutConstraint constraint, boolean variableCost, float cost) {
		theConstraints.add(new LayoutConstraintHolder(constraint, variableCost, cost));
	}

	public void layout(int width, int height, MuisElement container, MuisElement... children) {
	}

	private class GeneticLayoutMD {
		MuisElement container;

		Dimension size;

		MuisElement [] elements;

		GeneticLayoutMD(MuisElement cont, Dimension sz, MuisElement [] els) {
			container = cont;
			size = sz;
			elements = els;
		}
	}

	private class ScratchPadImpl implements LayoutScratchPad {
		private MuisElement [] theElements;

		private HashMap<MuisElement, Rectangle> theBounds;

		ScratchPadImpl(MuisElement [] els) {
			theElements = els;
			theBounds = new HashMap<>();
			for(MuisElement el : theElements) {
				theBounds.put(el, new Rectangle(el.bounds().getX(), el.bounds().getY(), el.bounds().getWidth(), el.bounds().getHeight()));
			}
		}

		@Override
		public Iterator<MuisElement> iterator() {
			return prisms.util.ArrayUtils.iterator(theElements, true);
		}

		@Override
		public int size() {
			return theElements.length;
		}

		@Override
		public Rectangle get(MuisElement element) {
			return theBounds.get(element);
		}
	}

	private class LayoutTester implements GeneticSolver.GenomeTester {
		private MuisElement theContainer;

		private Dimension theSize;

		private ScratchPadImpl theScratchPad;

		LayoutTester(MuisElement container, Dimension size, MuisElement [] elements) {
			theContainer = container;
			theSize = size;
			theScratchPad = new ScratchPadImpl(elements);
		}

		@Override
		public float getFitness(Genome genome) {
			float ret = 0;
			for(LayoutConstraintHolder holder : theConstraints) {
				if(holder.variableCost) {
					ret += holder.constraint.getViolation(theContainer, theSize, theScratchPad) * holder.cost;
				} else if(holder.constraint.isViolated(theContainer, theSize, theScratchPad)) {
					ret += holder.cost;
				}
			}
			return ret;
		}

		@Override
		public void close() {
		}
	}

	private class LayoutTesterMaker implements GeneticSolver.GenomeTesterMaker<GeneticLayoutMD> {
		@Override
		public LayoutTester createTester(GeneticLayoutMD metadata) {
			return new LayoutTester(metadata.container, metadata.size, metadata.elements);
		}
	}
}
