package org.muis.base.layout;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.muis.base.util.GeneticSolver;
import org.muis.base.util.GeneticSolver.GeneticResults;
import org.muis.base.util.GeneticSolver.GeneticSolution;
import org.muis.base.util.GeneticSolver.Genome;
import org.muis.core.MuisElement;

public class GeneticLayout {
    public static class LayoutScratchPad implements Iterable<MuisElement> {
        private final MuisElement[] theElements;

        private final HashMap<MuisElement, Rectangle> theBounds;

        LayoutScratchPad(MuisElement[] els) {
            theElements = els;
            theBounds = new HashMap<>();
            for (MuisElement el : theElements) {
                theBounds.put(el, new Rectangle(el.bounds().getX(), el.bounds().getY(), el.bounds().getWidth(), el.bounds().getHeight()));
            }
        }

        @Override
        public Iterator<MuisElement> iterator() {
            return prisms.util.ArrayUtils.iterator(theElements, true);
        }

        public int size() {
            return theElements.length;
        }

        public Rectangle get(MuisElement element) {
            return theBounds.get(element);
        }
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

    public LayoutScratchPad layout(int width, int height, MuisElement container, MuisElement[] children, LayoutScratchPad... firstTries) {
        if (firstTries.length == 0)
            throw new IllegalArgumentException("At least 1 first try must be given");
        Genome[] init = new Genome[firstTries.length];
        for (int i = 0; i < firstTries.length; i++)
            init[i] = convert(firstTries[i]);
        GeneticSolver<GeneticLayoutMD> solver = new GeneticSolver<>(new LayoutTesterMaker());
        solver.setMinFitness(.995f);
        solver.setPopulationCapacity(250);
        GeneticResults results = solver.solve(new GeneticLayoutMD(container, new Dimension(width, height), children), init);
        Iterator<GeneticSolution> solns = results.iterator();
        if (!solns.hasNext())
            return null;
        return apply(solns.next().getGenome(), new LayoutScratchPad(children));
	}

    private static class GeneticLayoutMD {
		MuisElement container;

		Dimension size;

		MuisElement [] elements;

		GeneticLayoutMD(MuisElement cont, Dimension sz, MuisElement [] els) {
			container = cont;
			size = sz;
			elements = els;
		}
	}

	private class LayoutTester implements GeneticSolver.GenomeTester {
		private MuisElement theContainer;

		private Dimension theSize;

        private LayoutScratchPad theScratchPad;

		LayoutTester(MuisElement container, Dimension size, MuisElement [] elements) {
			theContainer = container;
			theSize = size;
            theScratchPad = new LayoutScratchPad(elements);
		}

		@Override
		public float getFitness(Genome genome) {
            apply(genome, theScratchPad);
			float ret = 0;
			for(LayoutConstraintHolder holder : theConstraints) {
				if(holder.variableCost) {
					ret += holder.constraint.getViolation(theContainer, theSize, theScratchPad) * holder.cost;
				} else if(holder.constraint.isViolated(theContainer, theSize, theScratchPad)) {
					ret += holder.cost;
				}
			}
            return 1f / ret;
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

    private static LayoutScratchPad apply(Genome genome, LayoutScratchPad pad) {
        if (pad.size() * 4 != genome.getGenes().length)
            throw new IllegalArgumentException("The given layout configuration and genome do not match");
        int g = 0;
        for (MuisElement el : pad) {
            Rectangle bounds = pad.get(el);
            bounds.x = Math.round(genome.getGenes()[g]);
            bounds.y = Math.round(genome.getGenes()[g + 1]);
            bounds.width = Math.round(genome.getGenes()[g + 2]);
            bounds.height = Math.round(genome.getGenes()[g + 3]);
            g += 4;
        }
        return pad;
    }

    public static Genome convert(LayoutScratchPad pad) {
        Genome ret = new Genome(pad.size() * 4);
        int g = 0;
        for (MuisElement el : pad) {
            Rectangle bounds = pad.get(el);
            ret.getGenes()[g] = bounds.x;
            ret.getGenes()[g + 1] = bounds.y;
            ret.getGenes()[g + 2] = bounds.width;
            ret.getGenes()[g + 3] = bounds.height;
            g += 4;
        }
        return ret;
    }
}
