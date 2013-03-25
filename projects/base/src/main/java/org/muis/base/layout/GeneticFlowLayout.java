package org.muis.base.layout;

import static org.muis.core.layout.LayoutAttributes.*;
import static org.muis.core.style.LayoutStyles.margin;
import static org.muis.core.style.LayoutStyles.padding;

import java.awt.Dimension;

import org.muis.base.layout.AbstractFlowLayout.BreakPolicy;
import org.muis.base.layout.GeneticLayout.LayoutConstraint;
import org.muis.base.layout.GeneticLayout.LayoutScratchPad;
import org.muis.core.MuisElement;
import org.muis.core.MuisLayout;
import org.muis.core.layout.Direction;
import org.muis.core.layout.Orientation;
import org.muis.core.layout.SizeGuide;
import org.muis.util.CompoundListener;

public class GeneticFlowLayout implements MuisLayout {
    public static final LayoutConstraint breakConstraint = new LayoutConstraint() {
        @Override
        public boolean isViolated(MuisElement container, Dimension size, LayoutScratchPad layout) {
            BreakPolicy bp = container.atts().get(AbstractFlowLayout.FLOW_BREAK);
            if (bp == null)
                bp = AbstractFlowLayout.BreakPolicy.NEEDED;
            Direction dir = container.atts().get(direction);
            if (dir == null)
                dir = Direction.RIGHT;
            Orientation orient = dir.getOrientation();
            Orientation cross = orient.opposite();

            return false;
            // TODO Auto-generated method stub
        }

        @Override
        public float getViolation(MuisElement container, Dimension size, LayoutScratchPad layout) {
            return 0;
            // TODO Auto-generated method stub
        }
    };

    public static final LayoutConstraint alignConstraint;

    private final CompoundListener.MultiElementCompoundListener theListener;

    private final GeneticLayout theGeneticLayout;

    /** Creates a flow layout */
    public GeneticFlowLayout() {
        theListener = CompoundListener.create(this);
        theListener.acceptAll(direction, AbstractFlowLayout.FLOW_BREAK, alignment, crossAlignment).watchAll(margin, padding)
                .onChange(CompoundListener.layout);
        theListener.child().acceptAll(width, minWidth, maxWidth, height, minHeight, maxHeight).onChange(CompoundListener.layout);

        theGeneticLayout = new GeneticLayout();
        theGeneticLayout.addConstraint(LayoutConstraints.margin, true, 100);
        theGeneticLayout.addConstraint(LayoutConstraints.padding, true, 100);
        theGeneticLayout.addConstraint(LayoutConstraints.sizing, true, 10);
        theGeneticLayout.addConstraint(LayoutConstraints.whiteSpace, true, 10);
        theGeneticLayout.addConstraint(breakConstraint, true, 50);
        theGeneticLayout.addConstraint(alignConstraint, true, 35);
    }

    @Override
    public void initChildren(MuisElement parent, MuisElement[] children) {
        theListener.listenerFor(parent);
    }

    @Override
    public void remove(MuisElement parent) {
        theListener.dropFor(parent);
    }

    @Override
    public void childAdded(MuisElement parent, MuisElement child) {
    }

    @Override
    public void childRemoved(MuisElement parent, MuisElement child) {
    }

    @Override
    public SizeGuide getWSizer(MuisElement parent, MuisElement[] children) {
        return new org.muis.core.layout.AbstractSizeGuide(){
            private LayoutScratchPad theCachedLayout;

            private int theCachedCrossSize;

            private LayoutScratchPad getLayout(int crossSize){
                if(theCachedCrossSize==crossSize)
                    return theCachedLayout;
                theCachedLayout=theGeneticLayout.layout(Integer., crossSize, parent, children, null)
            }

            @Override
            public int getMinPreferred(int crossSize) {
                return 0;
                // TODO Auto-generated method stub
            }

            @Override
            public int getMaxPreferred(int crossSize) {
                return 0;
                // TODO Auto-generated method stub
            }

            @Override
            public int getMin(int crossSize) {
                return 0;
                // TODO Auto-generated method stub
            }

            @Override
            public int getPreferred(int crossSize) {
                return 0;
                // TODO Auto-generated method stub
            }

            @Override
            public int getMax(int crossSize) {
                return 0;
                // TODO Auto-generated method stub
            }
        };
    }

    @Override
    public SizeGuide getHSizer(MuisElement parent, MuisElement[] children) {
        return null;
        // TODO Auto-generated method stub
    }

    @Override
    public void layout(MuisElement parent, MuisElement[] children) {
        // TODO Auto-generated method stub
    }
}
