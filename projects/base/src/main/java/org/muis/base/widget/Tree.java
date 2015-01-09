package org.muis.base.widget;

import java.awt.Point;
import java.util.ArrayList;

import org.muis.base.model.TreePath;
import org.muis.base.model.TreePathSet;
import org.muis.base.model.TreeSelectionModel;
import org.muis.core.*;
import org.muis.core.event.MouseEvent;
import org.muis.core.layout.*;
import org.muis.core.rx.ObservableTree;
import org.muis.core.style.StyleAttribute;

import prisms.lang.Type;

public class Tree extends MuisElement {
	/** Styles specific to trees */
	public static final class TreeStyle implements org.muis.core.style.StyleDomain {
		private StyleAttribute<?> [] theAttributes;

		private TreeStyle() {
			theAttributes = new StyleAttribute[0];
		}

		private void register(StyleAttribute<?> attr) {
			theAttributes = prisms.util.ArrayUtils.add(theAttributes, attr);
		}

		private static final TreeStyle instance;

		/** The indent of the each tree level, in pixels */
		public static final StyleAttribute<Long> indent;

		static {
			instance = new TreeStyle();
			indent = new StyleAttribute<>(instance, "indent", MuisProperty.intAttr, 10L, new MuisProperty.ComparableValidator<>(0L, 1000L));
			instance.register(indent);
		}

		/** @return The style domain for all tree styles */
		public static TreeStyle getDomainInstance() {
			return instance;
		}

		@Override
		public String getName() {
			return "tree-style";
		}

		@Override
		public java.util.Iterator<StyleAttribute<?>> iterator() {
			return prisms.util.ArrayUtils.iterator(theAttributes, true);
		}
	}

	public static final MuisProperty.PropertyType<ObservableTree<?>> modelType = new MuisProperty.PrismsParsedPropertyType<>(new Type(
		ObservableTree.class, new Type(Object.class, true)));

	public static final MuisProperty.PropertyType<ValueRenderer<?>> renderType = new MuisProperty.PrismsParsedPropertyType<>(new Type(
		ValueRenderer.class, new Type(Object.class, true)));

	public static final MuisProperty.PropertyType<TreeSelectionModel<?>> selectionType = new MuisProperty.PrismsParsedPropertyType<>(
		new Type(TreeSelectionModel.class, new Type(Object.class, true)));

	public static final MuisProperty.PropertyType<TreePathSet<?>> expansionType = new MuisProperty.PrismsParsedPropertyType<>(new Type(
		TreePathSet.class, new Type(Object.class, true)));

	public static final MuisAttribute<ObservableTree<?>> model = new MuisAttribute<>("model", modelType);

	public static final MuisAttribute<ValueRenderer<?>> render = new MuisAttribute<>("render", renderType);

	public static final MuisAttribute<ValueRenderer<?>> hover = new MuisAttribute<>("hover", renderType);

	public static final MuisAttribute<ValueRenderer<?>> edit = new MuisAttribute<>("edit", renderType);

	public static final MuisAttribute<TreeSelectionModel<?>> selection = new MuisAttribute<>("selection", selectionType);

	public static final MuisAttribute<TreePathSet<?>> expansion = new MuisAttribute<>("expansion", expansionType);

	private Point theHoverPoint;

	public Tree() {
		life().runWhen(
			() -> {
				Object wanter = new Object();
				atts().require(wanter, model);
				atts().require(wanter, render);
				atts().accept(wanter, hover);
				atts().accept(wanter, edit);
				try {
					atts().require(wanter, selection, new org.muis.base.model.impl.DefaultTreeSelectionModel<>(new Type(Object.class)));
					atts().require(wanter, expansion, new org.muis.base.model.impl.DefaultTreePathSet<>(new Type(Object.class)));
				} catch(MuisException e) {
					msg().error("Should not happen!", e);
				}
				events().filterMap(MouseEvent.mouse.addTypes(MouseEvent.MouseEventType.moved)).act(
					evt -> theHoverPoint = evt.getPosition(this));
			}, MuisConstants.CoreStage.INITIALIZED.toString(), 1);
	}

	@Override
	public SizeGuide getWSizer() {
		int indent = getStyle().getSelf().get(TreeStyle.indent).get().intValue();
		ObservableTree<?> treeModel = atts().get(model);
		return new HSizeGuide(treeModel, indent, atts().get(selection), atts().get(expansion), (ValueRenderer<Object>) atts().get(render),
			(ValueRenderer<Object>) atts().get(hover), (ValueRenderer<Object>) atts().get(edit));
	}

	@Override
	public SizeGuide getHSizer() {
		int indent = getStyle().getSelf().get(TreeStyle.indent).get().intValue();
		ObservableTree<?> treeModel = atts().get(model);
		return new VSizeGuide(treeModel, indent, atts().get(selection), atts().get(expansion), (ValueRenderer<Object>) atts().get(render),
			(ValueRenderer<Object>) atts().get(hover), (ValueRenderer<Object>) atts().get(edit));
	}

	private TreePath<?> getHoverNode() {
	}

	private class HSizeGuide implements SizeGuide {
		private final ObservableTree<?> theModel;

		private final int theIndent;

		private final TreeSelectionModel<?> theSelection;

		private final TreePathSet<?> theExpansion;

		private final ValueRenderer<Object> theRender;

		private final ValueRenderer<Object> theHover;

		private final ValueRenderer<Object> theEdit;

		public HSizeGuide(ObservableTree<?> treeModel, int indent, TreeSelectionModel<?> select, TreePathSet<?> expand,
			ValueRenderer<Object> rend, ValueRenderer<Object> hov, ValueRenderer<Object> ed) {
			theModel = treeModel;
			theIndent = indent;
			theSelection = select;
			theExpansion = expand;
			theRender = rend;
			theHover = hov;
			theEdit = ed;
		}

		@Override
		public int getMin(int crossSize, boolean csMax) {
			return get(LayoutGuideType.min, crossSize, csMax);
		}

		@Override
		public int getMinPreferred(int crossSize, boolean csMax) {
			return get(LayoutGuideType.minPref, crossSize, csMax);
		}

		@Override
		public int getPreferred(int crossSize, boolean csMax) {
			return get(LayoutGuideType.pref, crossSize, csMax);
		}

		@Override
		public int getMaxPreferred(int crossSize, boolean csMax) {
			return get(LayoutGuideType.maxPref, crossSize, csMax);
		}

		@Override
		public int getMax(int crossSize, boolean csMax) {
			return get(LayoutGuideType.max, crossSize, csMax);
		}

		@Override
		public int get(LayoutGuideType type, int crossSize, boolean csMax) {
			TreePath<Object> [] rows = getRows();
			int hSize = getBounds().getWidth();
			//Get the heights for each visible row
			LayoutUtils.LayoutInterpolation<LayoutSize []> result = LayoutUtils.interpolate(new LayoutUtils.LayoutChecker<LayoutSize []>() {
				@Override
				public LayoutSize [] getLayoutValue(LayoutGuideType type) {
					LayoutSize [] ret = new LayoutSize[rows.length];
					for(int i = 0; i < ret.length; i++) {
						ret[i] = new LayoutSize();
						LayoutUtils.getSize(getRendererFor(rows[i]), Orientation.vertical, type, hSize, crossSize, true, ret[i]);
					}
					return ret;
				}

				@Override
				public int getSize(LayoutSize [] layoutValue) {
					LayoutSize ret = new LayoutSize();
					for(int i = 0; i < layoutValue.length; i++)
						ret.add(layoutValue[i]);
					return ret.getTotal(hSize);
				}
			}, crossSize, null, null);

			int vSize=getBounds().getHeight();
			int max=0;
			for(int r=0;r<rows.length;r++){
				int rowCrossSize = (int) (result.lowerValue[r].getTotal(vSize) + result.proportion
					* (result.upperValue[r].getTotal(vSize) - result.lowerValue[r].getTotal(vSize)));
				int size = getRendererFor(rows[r]).getBounds().get(Orientation.horizontal).getGuide().get(type, rowCrossSize, false);
				size += theIndent * (rows[r].size() - 1);
				if(size > max)
					max = size;
			}
			return max;
		}

		@Override
		public int getBaseline(int size) {
			TreePath<Object> path = new TreePath<>(new Object[] {theModel.get()});
			ValueRenderer<Object> rend = getRendererFor(path);
			return rend.getBounds().getHorizontal().getGuide().getBaseline(size);
		}

		private TreePath<Object> [] getRows() {
			ArrayList<TreePath<Object>> ret = new ArrayList<>();
			addNodeRow(ret, theModel, new ArrayList<>());
			return ret.toArray(new TreePath[ret.size()]);
		}

		private void addNodeRow(ArrayList<TreePath<Object>> ret, ObservableTree<?> node, ArrayList<Object> path) {
			Object value = node.get();
			path.add(value);
			TreePath<Object> tp = new TreePath<>(path);
			ret.add(tp);
			if(theExpansion.contains(tp)) {
				for(ObservableTree<?> child : node.getChildren())
					addNodeRow(ret, child, path);
			}
		}

		private ValueRenderer<Object> getRendererFor(TreePath<?> path) {
			if(path.equals(theSelection.getAnchor()))
				return theEdit;
			else if(path.equals(getHoverNode()))
				return theHover;
			else {
				theRender.renderFor(path.target(), theSelection.contains(path));
				return theRender;
			}
		}
	}

	private class VSizeGuide implements SizeGuide {
		private final ObservableTree<?> theModel;

		private final int theIndent;

		private final TreeSelectionModel<?> theSelection;

		private final TreePathSet<?> theExpansion;

		private final ValueRenderer<Object> theRender;

		private final ValueRenderer<Object> theHover;

		private final ValueRenderer<Object> theEdit;

		public VSizeGuide(ObservableTree<?> treeModel, int indent, TreeSelectionModel<?> select, TreePathSet<?> expand,
			ValueRenderer<Object> rend, ValueRenderer<Object> hov, ValueRenderer<Object> ed) {
			theModel = treeModel;
			theIndent = indent;
			theSelection = select;
			theExpansion = expand;
			theRender = rend;
			theHover = hov;
			theEdit = ed;
		}

		@Override
		public int getMin(int crossSize, boolean csMax) {
			return get(LayoutGuideType.min, crossSize, csMax);
		}

		@Override
		public int getMinPreferred(int crossSize, boolean csMax) {
			return get(LayoutGuideType.minPref, crossSize, csMax);
		}

		@Override
		public int getPreferred(int crossSize, boolean csMax) {
			return get(LayoutGuideType.pref, crossSize, csMax);
		}

		@Override
		public int getMaxPreferred(int crossSize, boolean csMax) {
			return get(LayoutGuideType.maxPref, crossSize, csMax);
		}

		@Override
		public int getMax(int crossSize, boolean csMax) {
			return get(LayoutGuideType.max, crossSize, csMax);
		}

		@Override
		public int get(LayoutGuideType type, int crossSize, boolean csMax) {
			TreePath<Object> [] rows = getRows();
			int hSize = getBounds().getWidth();
		}

		@Override
		public int getBaseline(int size) {
			TreePath<Object> path = new TreePath<>(new Object[] {theModel.get()});
			ValueRenderer<Object> rend = getRendererFor(path);
			return rend.getBounds().getVertical().getGuide().getBaseline(size);
		}

		private TreePath<Object> [] getRows() {
			ArrayList<TreePath<Object>> ret = new ArrayList<>();
			addNodeRow(ret, theModel, new ArrayList<>());
			return ret.toArray(new TreePath[ret.size()]);
		}

		private void addNodeRow(ArrayList<TreePath<Object>> ret, ObservableTree<?> node, ArrayList<Object> path) {
			Object value = node.get();
			path.add(value);
			TreePath<Object> tp = new TreePath<>(path);
			ret.add(tp);
			if(theExpansion.contains(tp)) {
				for(ObservableTree<?> child : node.getChildren())
					addNodeRow(ret, child, path);
			}
		}

		private ValueRenderer<Object> getRendererFor(TreePath<?> path) {
			if(path.equals(theSelection.getAnchor()))
				return theEdit;
			else if(path.equals(getHoverNode()))
				return theHover;
			else {
				theRender.renderFor(path.target(), theSelection.contains(path));
				return theRender;
			}
		}
	}
}
