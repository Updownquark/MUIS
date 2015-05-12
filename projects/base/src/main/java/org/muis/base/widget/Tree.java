package org.muis.base.widget;

import java.awt.*;
import java.util.ArrayList;

import org.muis.base.model.TreePath;
import org.muis.base.model.TreePathSet;
import org.muis.base.model.TreeSelectionModel;
import org.muis.core.*;
import org.muis.core.event.MouseEvent;
import org.muis.core.layout.LayoutGuideType;
import org.muis.core.layout.SizeGuide;
import org.muis.core.style.LayoutStyle;
import org.muis.core.style.Size;
import org.muis.core.style.StyleAttribute;
import org.muis.core.tags.Template;
import org.observe.Observable;
import org.observe.ObservableValue;
import org.observe.SettableValue;
import org.observe.datastruct.ObservableTree;

import prisms.lang.Type;

/** A graphical display of hierarchical data */
@Template(location = "../../../../tree.muis")
public class Tree extends MuisTemplate {
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

		/** The thickness of the trace lines, in pixels */
		public static final StyleAttribute<Long> traceThickness;

		/** The color of the trace lines */
		public static final StyleAttribute<Color> traceColor;

		/** The length of the dashes in the trace lines */
		public static final StyleAttribute<Double> traceDashLength;

		/** The distance between dashes in the trace lines */
		public static final StyleAttribute<Double> traceDashInterim;

		static {
			instance = new TreeStyle();
			indent = new StyleAttribute<>(instance, "indent", MuisProperty.intAttr, 10L, new MuisProperty.ComparableValidator<>(0L, 1000L));
			instance.register(indent);
			traceThickness = new StyleAttribute<>(instance, "trace-thickness", MuisProperty.intAttr, 1L,
				new MuisProperty.ComparableValidator<>(0L, 1000L));
			instance.register(traceThickness);
			traceColor = new StyleAttribute<>(instance, "trace-thickness", MuisProperty.colorAttr, new Color(0, 0, 0));
			instance.register(traceColor);
			traceDashLength = new StyleAttribute<>(instance, "trace-dash-length", MuisProperty.floatAttr, 3d,
				new MuisProperty.ComparableValidator<>(1d, 1000d));
			instance.register(traceDashLength);
			traceDashInterim = new StyleAttribute<>(instance, "trace-dash-interim", MuisProperty.floatAttr, 3d,
				new MuisProperty.ComparableValidator<>(0d, 1000d));
			instance.register(traceDashInterim);
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
	public static final MuisProperty.PropertyType<TreeSelectionModel<?>> selectionType = new MuisProperty.PrismsParsedPropertyType<>(
		new Type(TreeSelectionModel.class, new Type(Object.class, true)));
	public static final MuisProperty.PropertyType<TreePathSet<?>> expansionType = new MuisProperty.PrismsParsedPropertyType<>(new Type(
		TreePathSet.class, new Type(Object.class, true)));

	public static final MuisAttribute<ObservableTree<?>> model = new MuisAttribute<>("model", modelType);
	public static final MuisAttribute<TreeSelectionModel<?>> selection = new MuisAttribute<>("selection", selectionType);
	public static final MuisAttribute<TreePathSet<?>> expansion = new MuisAttribute<>("expansion", expansionType);

	private TreePath<?> theHoverNode;

	/** Creates a tree */
	public Tree() {
		/* TODO
		 * * Add attribute and element listeners to re-render when anything changes
		 * * Add model listeners to re-render when the model changes
		 * * Need to manage selection
		 * * Need to manage expansion
		 */
		life().runWhen(
			() -> {
				Object wanter = new Object();
				atts().require(wanter, model);
				try {
					atts().require(wanter, selection, new org.muis.base.model.impl.DefaultTreeSelectionModel<>(new Type(Object.class)));
					atts().require(wanter, expansion, new org.muis.base.model.impl.DefaultTreePathSet<>(new Type(Object.class)));
				} catch(MuisException e) {
					msg().error("Should not happen!", e);
				}
				events().filterMap(MouseEvent.mouse.addTypes(MouseEvent.MouseEventType.moved)).act(evt -> hover(evt.getPosition(this)));
				org.muis.core.style.MuisStyle ss = getStyle().getSelf();
				Observable.or(ss.get(TreeStyle.indent).value(), ss.get(LayoutStyle.margin).value(), ss.get(LayoutStyle.padding).value())
					.act(value -> {
						relayout(false);
					});
				Observable.or(ss.get(TreeStyle.traceThickness).value(), ss.get(TreeStyle.traceColor).value(),
					ss.get(TreeStyle.traceDashLength).value(), ss.get(TreeStyle.traceDashInterim).value()).act(value -> {
					repaint(null, false);
				});
				((TreePathSet<Object>) atts().get(expansion)).add(new TreePath<>((ObservableTree<Object>) atts().get(model)));
			}, MuisConstants.CoreStage.INITIALIZED.toString(), 1);
	}

	@Override
	public SizeGuide getWSizer() {
		int indent = getStyle().getSelf().get(TreeStyle.indent).get().intValue();
		Size margin = getStyle().getSelf().get(LayoutStyle.margin).get();
		Size padding = getStyle().getSelf().get(LayoutStyle.padding).get();
		ObservableTree<?> treeModel = atts().get(model);
		return new HSizeGuide(treeModel, indent, margin, padding, atts().get(selection), atts().get(expansion), getRenderer(), getHover(),
			getEditor());
	}

	@Override
	public SizeGuide getHSizer() {
		int indent = getStyle().getSelf().get(TreeStyle.indent).get().intValue();
		Size margin = getStyle().getSelf().get(LayoutStyle.margin).get();
		Size padding = getStyle().getSelf().get(LayoutStyle.padding).get();
		ObservableTree<?> treeModel = atts().get(model);
		return new VSizeGuide(treeModel, indent, margin, padding, atts().get(selection), atts().get(expansion), getRenderer(), getHover(),
			getEditor());
	}

	@Override
	public void doLayout() {
		ObservableTree<?> treeModel = atts().get(model);
		TreeSelectionModel<?> select = atts().get(selection);
		TreePathSet<?> expand = atts().get(expansion);
		ValueRenderer<Object> renderer = (ValueRenderer<Object>) getRenderer();
		ValueRenderer<Object> hover = (ValueRenderer<Object>) getHover();
		ValueRenderer<Object> editor = (ValueRenderer<Object>) getEditor();
		TreePath<?> [] rows = getRows(treeModel, select, expand, renderer, hover, editor);
		int indent = getStyle().getSelf().get(TreeStyle.indent).get().intValue();
		Size margin = getStyle().getSelf().get(LayoutStyle.margin).get();
		Size padding = getStyle().getSelf().get(LayoutStyle.padding).get();

		int width = getBounds().getWidth();
		int height = getBounds().getHeight();

		int y = margin.evaluate(height);
		boolean editDone = select.getAnchor() == null;
		boolean hoverDone = theHoverNode == null;
		for(TreePath<?> row : rows) {
			if(editDone && hoverDone)
				break;

			int rowHeight;
			ValueRenderer<?> render = null;
			if(row.equals(select.getAnchor())) {
				render = editor;
				rowHeight = editor.bounds().getVertical().getGuide().getPreferred(Integer.MAX_VALUE, true);
				editDone = true;
			} else if(row.equals(theHoverNode)) {
				render = hover;
				rowHeight = editor.bounds().getVertical().getGuide().getPreferred(Integer.MAX_VALUE, true);
				hoverDone = true;
			} else {
				renderer.renderFor(row.target(), select.contains(row), false);
				rowHeight = renderer.bounds().getVertical().getGuide().getPreferred(Integer.MAX_VALUE, true);
			}
			if(render != null) {
				int x = margin.evaluate(width) + (row.size() - 1) * indent;
				SizeGuide hGuide = render.bounds().getHorizontal().getGuide();
				int rowSpace = width - (row.size() - 1) * indent - margin.evaluate(width) * 2;
				int min = hGuide.getMin(rowHeight, false);
				int rowWidth;
				if(rowSpace < min)
					rowWidth = min;
				else {
					int maxPref = hGuide.getMaxPreferred(rowHeight, false);
					if(rowSpace <= maxPref)
						rowWidth = rowSpace;
					else
						rowWidth = maxPref;
				}
				render.bounds().setBounds(x, y, rowWidth, rowHeight);
			}

			y += rowHeight + padding.evaluate(height);
		}
		Image expandImg = getExpand();
		Image collapseImg = getCollapse();
		expandImg.getBounds().setBounds(width, height, expandImg.getImage().getWidth(), expandImg.getImage().getHeight());
		collapseImg.getBounds().setBounds(width, height, collapseImg.getImage().getWidth(), collapseImg.getImage().getHeight());
	}

	@Override
	public MuisElementCapture [] paintChildren(Graphics2D graphics, Rectangle area) {
		ObservableTree<?> treeModel = atts().get(model);
		TreeSelectionModel<?> select = atts().get(selection);
		TreePathSet<?> expand = atts().get(expansion);
		ValueRenderer<Object> renderer = (ValueRenderer<Object>) getRenderer();
		ValueRenderer<Object> hover = (ValueRenderer<Object>) getHover();
		ValueRenderer<Object> editor = (ValueRenderer<Object>) getEditor();
		Image expandImg = getExpand();
		Image collapseImg = getCollapse();
		TreePath<?> [] rows = getRows(treeModel, select, expand, renderer, hover, editor);
		int indent = getStyle().getSelf().get(TreeStyle.indent).get().intValue();
		Size margin = getStyle().getSelf().get(LayoutStyle.margin).get();
		int padding = getStyle().getSelf().get(LayoutStyle.padding).get().evaluate(bounds().getHeight());
		Color traceColor = getStyle().getSelf().get(TreeStyle.traceColor).get();
		int traceThickness = getStyle().getSelf().get(TreeStyle.traceThickness).get().intValue();
		float dashLength = getStyle().getSelf().get(TreeStyle.traceDashLength).get().floatValue();
		float dashInterim = getStyle().getSelf().get(TreeStyle.traceDashInterim).get().floatValue();

		MuisElementCapture selectedCapture = null;
		int y = margin.evaluate(bounds().getHeight());
		try {
			for(TreePath<?> row : rows) {
				if(area != null && y >= area.y + area.height)
					break;

				ValueRenderer<Object> render;
				if(row.equals(select.getAnchor())) {
					render = editor;
				} else if(row.equals(theHoverNode)) {
					render = hover;
				} else {
					render = renderer;
					render.renderFor(row.target(), false, false);
				}

				Rectangle childArea = render.bounds().getBounds();
				int childX = margin.evaluate(bounds().getWidth()) + indent * (row.size() - 1);
				childArea.x = childX;
				childArea.y = y;
				childArea = childArea.intersection(area);
				childArea.x -= childX;
				childArea.y -= y;
				// Draw the expand/collapse icon and trace lines
				if(row.size() > 1) { // No icon or trace lines to draw for root node
					Image icon = expand.contains(row) ? collapseImg : expandImg;
					Rectangle iconArea = icon.bounds().getBounds();
					int iconDX = (indent - icon.bounds().getWidth()) / 2;
					int iconX = iconDX + (row.size() - 2) * indent;
					iconArea.x = iconX;
					int iconDY = (childArea.height - icon.bounds().getHeight()) / 2;
					int iconY = y + iconDY;
					iconArea.y = iconY;
					iconArea = iconArea.intersection(area);
					iconArea.x -= iconX;
					iconArea.y -= iconY;
					graphics.translate(iconX, iconY);
					icon.paint(graphics, iconArea);
					if(traceColor.getAlpha() > 0 && traceThickness > 0) {
						BasicStroke stroke;
						if(dashInterim == 0)
							stroke = new BasicStroke(traceThickness);
						else
							stroke = new BasicStroke(traceThickness, 0, 0, 1, new float[] {dashLength, dashInterim}, 0);
						Stroke oldStroke = graphics.getStroke();
						Color oldColor = graphics.getColor();
						graphics.setStroke(stroke);
						graphics.setColor(traceColor);
						try {
							// Draw trace lines
							if(iconDY > 0)// Vertical line from the top-middle of the icon up to the top of the cell
								graphics.drawLine(icon.bounds().getWidth() / 2, -1, icon.bounds().getWidth() / 2, -(iconDY + padding + 1));
							if(iconDX > 0) // Horizontal line from right-middle of the icon to the edge of the renderer
								graphics.drawLine(icon.bounds().getWidth(), icon.bounds().getHeight() / 2, icon.bounds().getWidth()
									+ iconDX, icon.bounds().getHeight() / 2);
						} finally {
							graphics.setStroke(oldStroke);
							graphics.setColor(oldColor);
						}
					}
					graphics.translate(-iconX, -iconY);
				}
				graphics.translate(childX, y);
				MuisElementCapture capture= render.paint(graphics, childArea);
				graphics.translate(-childX, -y);
				if(row.equals(select.getAnchor()))
					selectedCapture=capture;
				y += childArea.height;
				y += padding;
			}
		} finally {
			renderer.bounds().setBounds(0, 0, 0, 0);
		}
		return selectedCapture == null ? new MuisElementCapture[0] : new MuisElementCapture[] {selectedCapture};
	}

	public ValueRenderer<?> getRenderer() {
		return (ValueRenderer<?>) getElement(getTemplate().getAttachPoint("renderer"));
	}

	public ValueRenderer<?> getHover() {
		ValueRenderer<?> ret = (ValueRenderer<?>) getElement(getTemplate().getAttachPoint("hover"));
		if(ret == null)
			ret = getRenderer();
		return ret;
	}

	public ValueRenderer<?> getEditor() {
		ValueRenderer<?> ret = (ValueRenderer<?>) getElement(getTemplate().getAttachPoint("editor"));
		if(ret == null)
			ret = getHover();
		return ret;
	}

	@Override
	protected boolean isStamp(MuisElement child) {
		AttachPoint role = child.atts().get(getTemplate().role);
		return role != null && (role.name.equals("renderer") || role.name.equals("hover"));
	}

	public TreePath<?> getHoverNode() {
		return theHoverNode;
	}

	protected Image getExpand() {
		return (Image) getElement(getTemplate().getAttachPoint("expand"));
	}

	protected Image getCollapse() {
		return (Image) getElement(getTemplate().getAttachPoint("collapse"));
	}

	private void hover(Point point) {
		ValueRenderer<?> hover = getHover();
		int hoverMinY = hover.bounds().getY();
		int hoverMaxY = hoverMinY + hover.bounds().getHeight();
		// TODO Don't forget to fire the exited event on the old hover node if it's not the editing node (happens automatically for editing
		// node)
		if(point.y < hoverMinY) {
			// TODO Search backwards until we find the next hover node
			relayout(false);
			// TODO Don't forget to fire the entered event on the new hover node if it's not the editing node (happens automatically for
			// editing node)
		} else if(point.y >= hoverMaxY) {
			// TODO Search forwards until we find the next hover node
			relayout(false);
			// TODO Don't forget to fire the entered event on the new hover node if it's not the editing node (happens automatically for
			// editing node)
		}
		// TODO Propagate the hover event to the hover node. May need to do all the subsequent todos in post-runnables given to the relayout
		// method
	}

	public TreePath<?> [] getRows() {
		return getRows(atts().get(model), atts().get(selection), atts().get(expansion), getRenderer(), getHover(), getEditor());
	}

	private TreePath<?> [] getRows(ObservableTree<?> treeModel, TreeSelectionModel<?> select, TreePathSet<?> expand, ValueRenderer<?> rend,
		ValueRenderer<?> hov, ValueRenderer<?> ed) {
		ArrayList<TreePath<Object>> ret = new ArrayList<>();
		addNodeRow(ret, (ObservableTree<Object>) treeModel, new ArrayList<>(), expand);
		return ret.toArray(new TreePath[ret.size()]);
	}

	private void addNodeRow(ArrayList<TreePath<Object>> ret, ObservableTree<Object> node, ArrayList<ObservableTree<Object>> path,
		TreePathSet<?> expand) {
		path.add(node);
		TreePath<Object> tp = new TreePath<>(path);
		ret.add(tp);
		if(expand.contains(tp)) {
			for(ObservableTree<Object> child : node.getChildren())
				addNodeRow(ret, child, path, expand);
		}
	}

	private ValueRenderer<?> getRendererFor(TreePath<?> path, TreeSelectionModel<?> select, ValueRenderer<?> render,
		ValueRenderer<?> hover, ValueRenderer<?> edit) {
		if(path.equals(select.getAnchor()))
			return edit;
		else if(path.equals(getHoverNode()))
			return hover;
		else {
			((ValueRenderer<Object>) render).renderFor(readOnly(path.target()), select.contains(path), false);
			return render;
		}
	}

	private static <T> ObservableValue<T> readOnly(ObservableValue<T> target) {
		if(!(target instanceof SettableValue))
			return target;
		else
			return ((SettableValue<T>) target).unsettable();
	}

	private class HSizeGuide implements SizeGuide {
		private final ObservableTree<?> theModel;

		private final int theIndent;

		private final Size theMargin;

		private final TreeSelectionModel<?> theSelection;

		private final TreePathSet<?> theExpansion;

		private final ValueRenderer<Object> theRender;

		private final ValueRenderer<Object> theHover;

		private final ValueRenderer<Object> theEdit;

		public HSizeGuide(ObservableTree<?> treeModel, int indent, Size margin, Size padding, TreeSelectionModel<?> select,
			TreePathSet<?> expand, ValueRenderer<?> rend, ValueRenderer<?> hov, ValueRenderer<?> ed) {
			theModel = treeModel;
			theIndent = indent;
			theMargin = margin;
			theSelection = select;
			theExpansion = expand;
			theRender = (ValueRenderer<Object>) rend;
			theHover = (ValueRenderer<Object>) hov;
			theEdit = (ValueRenderer<Object>) ed;
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
			TreePath<?> [] rows = getRows();
			int max = 0;
			for(TreePath<?> row : rows) {
				ValueRenderer<?> render = getRendererFor(row);
				int size = render.getBounds().getHorizontal().getGuide()
					.get(type, render.getBounds().getVertical().getGuide().getPreferred(Integer.MAX_VALUE, true), false);
				size += theIndent * (row.size() - 1);
				if(size > max)
					max = size;
			}
			max += theMargin.evaluate(max) * 2;
			return max;
		}

		@Override
		public int getBaseline(int size) {
			TreePath<Object> path = new TreePath<>((ObservableTree<Object>) theModel);
			ValueRenderer<?> rend = getRendererFor(path);
			return rend.getBounds().getHorizontal().getGuide().getBaseline(size);
		}

		private TreePath<?> [] getRows() {
			return Tree.this.getRows(theModel, theSelection, theExpansion, theRender, theHover, theEdit);
		}

		private ValueRenderer<?> getRendererFor(TreePath<?> path) {
			return Tree.this.getRendererFor(path, theSelection, theRender, theHover, theEdit);
		}
	}

	private class VSizeGuide implements SizeGuide {
		private final ObservableTree<?> theModel;

		private final int theIndent;

		private final Size theMargin;

		private final Size thePadding;

		private final TreeSelectionModel<?> theSelection;

		private final TreePathSet<?> theExpansion;

		private final ValueRenderer<Object> theRender;

		private final ValueRenderer<Object> theHover;

		private final ValueRenderer<Object> theEdit;

		public VSizeGuide(ObservableTree<?> treeModel, int indent, Size margin, Size padding, TreeSelectionModel<?> select,
			TreePathSet<?> expand, ValueRenderer<?> rend, ValueRenderer<?> hov, ValueRenderer<?> ed) {
			theModel = treeModel;
			theIndent = indent;
			theMargin = margin;
			thePadding = padding;
			theSelection = select;
			theExpansion = expand;
			theRender = (ValueRenderer<Object>) rend;
			theHover = (ValueRenderer<Object>) hov;
			theEdit = (ValueRenderer<Object>) ed;
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
			if(type == LayoutGuideType.max)
				return Integer.MAX_VALUE;
			else
				type = LayoutGuideType.pref;
			TreePath<?> [] rows = getRows();
			int height = bounds().getHeight();
			int sum = 0;
			for(int r = 0; r < rows.length; r++) {
				ValueRenderer<?> renderer = getRendererFor(rows[r]);
				int rowCrossSize = crossSize - (rows[r].size() - 1) * theIndent;
				sum = SizeGuide.add(sum, renderer.bounds().getVertical().getGuide().get(type, rowCrossSize, csMax));
				if(r > 0)
					sum = SizeGuide.add(sum, thePadding.evaluate(height));
				if(sum == Integer.MAX_VALUE)
					break;
			}
			sum += theMargin.evaluate(sum) * 2;
			return sum;
		}

		@Override
		public int getBaseline(int size) {
			TreePath<Object> path = new TreePath<>((ObservableTree<Object>) theModel);
			ValueRenderer<?> rend = getRendererFor(path);
			return rend.getBounds().getVertical().getGuide().getBaseline(size);
		}

		private TreePath<?> [] getRows() {
			return Tree.this.getRows(theModel, theSelection, theExpansion, theRender, theHover, theEdit);
		}

		private ValueRenderer<?> getRendererFor(TreePath<?> path) {
			return Tree.this.getRendererFor(path, theSelection, theRender, theHover, theEdit);
		}
	}
}
