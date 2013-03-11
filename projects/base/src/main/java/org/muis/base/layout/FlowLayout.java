package org.muis.base.layout;

import static org.muis.core.layout.LayoutAttributes.*;
import static org.muis.core.layout.LayoutGuideType.*;
import static org.muis.core.layout.Orientation.horizontal;
import static org.muis.core.layout.Orientation.vertical;

import org.muis.base.layout.AbstractFlowLayout.BreakPolicy;
import org.muis.core.MuisElement;
import org.muis.core.layout.Direction;
import org.muis.core.layout.LayoutGuideType;
import org.muis.core.layout.Orientation;
import org.muis.core.layout.SizePolicy;
import org.muis.util.CompoundListener;

public class FlowLayout implements org.muis.core.MuisLayout {
	private final CompoundListener.MultiElementCompoundListener theListener;

	public FlowLayout() {
		theListener = CompoundListener.create(this);
		theListener.acceptAll(direction, AbstractFlowLayout.FLOW_BREAK).onChange(CompoundListener.layout);
		theListener.child().acceptAll(width, minWidth, maxWidth, height, minHeight, maxHeight).onChange(CompoundListener.layout);
	}

	@Override
	public void initChildren(MuisElement parent, MuisElement [] children) {
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
	public SizePolicy getHSizer(MuisElement parent, final MuisElement [] children) {
		Direction dir = parent.atts().get(direction);
		if(dir == null)
			dir = Direction.RIGHT;
		final BreakPolicy bp = parent.atts().get(AbstractFlowLayout.FLOW_BREAK) != null ? parent.atts().get(AbstractFlowLayout.FLOW_BREAK)
			: BreakPolicy.NEEDED;

		final boolean main = dir.getOrientation() == vertical;

		return new SizePolicy() {
			@Override
			public int getMinPreferred(int crossSize) {
				return BaseLayoutUtils.doLayout(children, vertical, minPref, bp, main, crossSize);
			}

			@Override
			public int getMaxPreferred(int crossSize) {
				return BaseLayoutUtils.doLayout(children, vertical, maxPref, bp, main, crossSize);
			}

			@Override
			public int getMin(int crossSize) {
				return BaseLayoutUtils.doLayout(children, vertical, min, bp, main, crossSize);
			}

			@Override
			public int getPreferred(int crossSize) {
				return BaseLayoutUtils.doLayout(children, vertical, pref, bp, main, crossSize);
			}

			@Override
			public int getMax(int crossSize) {
				return BaseLayoutUtils.doLayout(children, vertical, max, bp, main, crossSize);
			}

			@Override
			public int get(LayoutGuideType type, int crossSize) {
				return BaseLayoutUtils.doLayout(children, vertical, type, bp, main, crossSize);
			}

			@Override
			public float getStretch() {
				return 0;
				// TODO Auto-generated method stub
			}
		};
	}

	@Override
	public SizePolicy getWSizer(MuisElement parent, final MuisElement [] children) {
		Direction dir = parent.atts().get(direction);
		if(dir == null)
			dir = Direction.RIGHT;
		final BreakPolicy bp = parent.atts().get(AbstractFlowLayout.FLOW_BREAK) != null ? parent.atts().get(AbstractFlowLayout.FLOW_BREAK)
			: BreakPolicy.NEEDED;

		final boolean main = dir.getOrientation() == Orientation.horizontal;

		return new SizePolicy() {
			@Override
			public int getMinPreferred(int crossSize) {
				return BaseLayoutUtils.doLayout(children, horizontal, minPref, bp, main, crossSize);
			}

			@Override
			public int getMaxPreferred(int crossSize) {
				return BaseLayoutUtils.doLayout(children, horizontal, maxPref, bp, main, crossSize);
			}

			@Override
			public int getMin(int crossSize) {
				return BaseLayoutUtils.doLayout(children, horizontal, min, bp, main, crossSize);
			}

			@Override
			public int getPreferred(int crossSize) {
				return BaseLayoutUtils.doLayout(children, horizontal, pref, bp, main, crossSize);
			}

			@Override
			public int getMax(int crossSize) {
				return BaseLayoutUtils.doLayout(children, horizontal, max, bp, main, crossSize);
			}

			@Override
			public int get(LayoutGuideType type, int crossSize) {
				return BaseLayoutUtils.doLayout(children, horizontal, type, bp, main, crossSize);
			}

			@Override
			public float getStretch() {
				return 0;
				// TODO Auto-generated method stub
			}
		};
	}

	@Override
	public void layout(MuisElement parent, MuisElement [] children) {
		// TODO Auto-generated method stub

	}

	int doLayout(MuisElement [] children, boolean main, boolean forward, Orientation orientation, LayoutGuideType type, int crossSize,
		boolean doLayout) {
		switch(type){
		case min:
		case minPref:
			int mainDim=0;
			int crossDim=crossSize;
			for(MuisElement child : children){
				mainDim=child.bounds().get(orientation).getGuide().get(type, crossSize);
				crossDim+=
			}
		case pref:

		case max:
		case maxPref:

		}
	}
}
