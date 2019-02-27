package org.quick.base.widget;

import java.awt.Graphics2D;
import java.util.*;

import org.observe.SimpleSettableValue;
import org.observe.collect.ObservableCollection;
import org.qommons.Transaction;
import org.qommons.collect.CollectionElement;
import org.qommons.collect.ElementId;
import org.quick.base.layout.BaseLayoutUtils;
import org.quick.core.*;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.event.MouseEvent;
import org.quick.core.event.MouseEvent.MouseEventType;
import org.quick.core.layout.*;
import org.quick.core.model.ModelAttributes;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.Template;

import com.google.common.reflect.TypeToken;

@QuickElementType(attributes = { //
	@AcceptAttribute(declaringClass = ModelAttributes.class, field = "name", required = false)//
})
@Template(location = "../../../../table-column.qts")
public class TableColumn extends QuickTemplate {
	private AttachPoint<QuickElement> theRendererAP;
	private AttachPoint<QuickElement> theHoverAP;
	private AttachPoint<QuickElement> theEditorAP;

	private boolean isSelected;
	private ElementId theSelectedRow;
	private ElementId theEditingRow;
	private Point theHoveredPoint;
	private ElementId theHoveredRow;

	private QuickElement theRenderer;
	private QuickElement theHover;
	private QuickElement theEditor;

	private SimpleSettableValue<Object> theRenderValue;
	private SimpleSettableValue<Object> theHoverValue;
	private SimpleSettableValue<Object> theEditorValue;

	public TableColumn() {
		theSelectedRow = theHoveredRow = null;
		life().runWhen(() -> {
			ObservableCollection<?> rows = getTable().atts().get(Table.rows).get();
			getResourcePool().build(rows::onChange, evt -> {
				switch(evt.getType()){
				case add:
					break;
				case remove:
					if(theSelectedRow!=null && !theSelectedRow.isPresent())
						setSelectedRow(null);
					break;
				case set:
					if(theSelectedRow!=null && theSelectedRow.equals(evt.getElementId())){
						if(isSelected)
							theEditorValue.set(rows.getElement(theSelectedRow).get(), evt);
					}
					break;
				}
			}).onSubscribe(r -> {
				updateHover();
				if (theSelectedRow != null && !theSelectedRow.isPresent())
					setSelectedRow(null);

			}).unsubscribe((r, s) -> s.unsubscribe());
			getResourcePool().pool(rows.simpleChanges()).act(v -> updateHover());
			getResourcePool().pool(rows.changes()).act(evt -> {
				Orientation orientation=getTable().atts().get(LayoutAttributes.orientation).get();
				int firstRowPos=getTable().getRowPosition(evt.elements.get(0).index);
				switch(evt.type){
				case add:
				case remove:
					if(orientation.isVertical())
						repaint(new Rectangle(0, firstRowPos, bounds().getWidth(), bounds().getHeight()-firstRowPos), false);
					else
						repaint(new Rectangle(firstRowPos, 0, bounds().getWidth()-firstRowPos, bounds().getHeight()), false);
					break;
				case set:
					int lastRowEnd = getTable().getRowPosition(evt.elements.get(evt.elements.size() - 1).index)//
						+ getTable().getRowLength(evt.elements.get(evt.elements.size() - 1).index);
					if (orientation.isVertical())
						repaint(new Rectangle(0, firstRowPos, bounds().getWidth(), lastRowEnd), false);
					else
						repaint(new Rectangle(firstRowPos, 0, lastRowEnd, bounds().getHeight()), false);
					break;
				}
			});

			TypeToken<Object> type = (TypeToken<Object>) rows.getType();
			theRenderValue = new SimpleSettableValue<>(type, true);
			theHoverValue = new SimpleSettableValue<>(type, true);
			theEditorValue = new SimpleSettableValue<>(type, true);

			theRendererAP = (AttachPoint<QuickElement>) getTemplate().getAttachPoint("renderer");
			theHoverAP = (AttachPoint<QuickElement>) getTemplate().getAttachPoint("hover");
			theEditorAP = (AttachPoint<QuickElement>) getTemplate().getAttachPoint("editor");

			String rowValueName = getTable().atts().get(Table.rowValueModelName).get();
			String rowElementName = getTable().atts().get(Table.rowElementModelName).get();
			// TODO Inject the row value into the renderer, hover, and editor models
			if (getTable().atts().get(Table.columns).get() != null) {
				String columnValueName = getTable().atts().get(Table.columnValueModelName).get();
				String columnElementName = getTable().atts().get(Table.columnElementModelName).get();
				// TODO Inject the column value into the renderer, hover, and editor models
			}

			class RenderHoverEditor {
				private boolean isCausingChange;
				private QuickElement configuredHover;
				private QuickElement configuredEditor;

				void renderChanged(QuickElement render, Object cause) {
					if (configuredHover == null && configuredEditor == null) {
						installCopy(render, theHoverAP, cause);
						installCopy(render, theEditorAP, cause);
					}
				}

				void hoverChanged(QuickElement hover, Object cause) {
					if (isCausingChange)
						return;
					configuredHover = hover;
					if (configuredEditor == null)
						installCopy(hover, theEditorAP, cause);
				}

				void editorChanged(QuickElement editor, Object cause) {
					if (isCausingChange)
						return;
					configuredEditor = editor;
					if (configuredHover == null)
						installCopy(editor, theHoverAP, cause);
				}

				private void installCopy(QuickElement element, AttachPoint<QuickElement> ap, Object cause) {
					isCausingChange = true;
					element.copy(TableColumn.this, el -> {
						el.atts().setValue(getTemplate().role, ap, cause);
						if (ap == theHoverAP)
							theHover = el;
						else
							theEditor = el;
						getElement(ap).set(el, cause);
					});
					isCausingChange = false;
				}
			}
			RenderHoverEditor rhe = new RenderHoverEditor();
			getElement(theRendererAP).changes().act(evt -> {
				if (evt.isInitial())
					theRenderer = evt.getNewValue();
				else
					rhe.renderChanged(evt.getNewValue(), evt);
			});
			getElement(theHoverAP).changes().act(evt -> {
				if (evt.isInitial()) {
					rhe.configuredHover = evt.getNewValue();
					theHover = evt.getNewValue();
				} else
					rhe.hoverChanged(evt.getNewValue(), evt);
			});
			getElement(theEditorAP).changes().act(evt -> {
				if (evt.isInitial()) {
					rhe.configuredEditor = evt.getNewValue();
					theEditor = evt.getNewValue();
				} else
					rhe.editorChanged(evt.getNewValue(), evt);
			});
		}, CoreStage.INIT_CHILDREN, 1);
		life().runWhen(() -> {
			ObservableCollection<?> rows = getTable().atts().get(Table.rows).get();
			events().filterMap(MouseEvent.mouse.addTypes(MouseEventType.moved)).act(evt -> {
				theHoveredPoint = evt.getPosition(TableColumn.this);
				updateHover();
			});
			events().filterMap(MouseEvent.mouse.addTypes(MouseEventType.exited)).act(evt -> {
				theHoveredPoint = null;
				updateHover();
			});
			events().filterMap(MouseEvent.mouse.addTypes(MouseEventType.pressed)).act(evt -> {
				int row = getTable().getRowAt(evt.getPosition(TableColumn.this));
				ElementId rowId = row < 0 ? null : rows.getElement(row).getElementId();
				if (isSelected && Objects.equals(theSelectedRow, rowId))
					return;
				getTable().setSelected(this, rowId);
				setSelected(true);
			});
		}, CoreStage.STARTUP, 1);
	}

	protected Table getTable() {
		QuickElement parent = getParent().get();
		if (parent != null && !(parent instanceof Table)) {
			msg().fatal("table-column elements must be the child of a table", "parent", parent);
			return null;
		}
		return (Table) parent;
	}

	public void rowUpdated(ElementId row) {
		if (row.equals(theHoveredRow))
			theHoverValue.set(getTable().getRows().getElement(row).get(), null);
		else if (row.equals(theEditingRow))
			theEditorValue.set(getTable().getRows().getElement(row).get(), null);
		else
			repaint(getCellBounds(row), false);
	}

	public void setSelectedRow(ElementId row) {
		if (Objects.equals(row, theSelectedRow))
			return;
		else if (isSelected)
			setEditingRow(row);
		else {
			if (row != null)
				repaint(getCellBounds(row), false);
			if (theSelectedRow != null)
				repaint(getCellBounds(theSelectedRow), false);
		}
		theSelectedRow = row;
	}

	public void setSelected(boolean selected) {
		if (isSelected == selected)
			return;
		isSelected = selected;
		if (isSelected)
			setEditingRow(theSelectedRow);
		else
			relayout(false);
	}

	private void setEditingRow(ElementId row) {
		boolean wasActive = theEditingRow != null;
		if (row == null) {
			if (wasActive) {
				// Disable editor
				theEditor.getResourcePool().setActive(false);
				try (Transaction t = theEditor.holdEvents(true, false)) {
					theEditor.bounds().setBounds(-1, -1, 0, 0);
				}
				updateHover();
				sizeNeedsChanged();
				relayout(false);
			}
		} else if (!Objects.equals(row, theEditingRow)) {
			theEditingRow = row;
			theEditorValue.set(getTable().getRows().getElement(row).get(), null);
			try (Transaction t = theEditor.holdEvents(true, false)) {
				theEditor.bounds().set(getCellBounds(row), null);
			}
			if (!wasActive) {
				// Enable editor
				theEditor.getResourcePool().setActive(true);
			}
			updateHover();
			sizeNeedsChanged();
			relayout(false);
		}
	}

	private void updateHover() {
		ElementId row;
		if (theHoveredPoint == null)
			row = null;
		else {
			int rowIdx = getTable().getRowAt(theHoveredPoint);
			row = rowIdx < 0 ? null : getTable().getRows().getElement(rowIdx).getElementId();
			if (isSelected && Objects.equals(theSelectedRow, row))
				row = null;
		}
		boolean wasActive = theHoveredRow != null;
		if (row == null) {
			if (wasActive) {
				// TODO Disable hover
				theHover.getResourcePool().setActive(false);
				try (Transaction t = theEditor.holdEvents(true, false)) {
					theHover.bounds().setBounds(-1, -1, 0, 0);
				}
				sizeNeedsChanged();
				relayout(false);
			}
		} else if (!Objects.equals(row, theHoveredRow)) {
			theHoveredRow = row;
			theHoverValue.set(getTable().getRows().getElement(row).get(), null);
			try (Transaction t = theEditor.holdEvents(true, false)) {
				theHover.bounds().set(getCellBounds(row), null);
			}
			if (!wasActive) {
				// Enable hover
				theHover.getResourcePool().setActive(true);
			}
			sizeNeedsChanged();
			relayout(false);
		}
	}

	public SizeGuide getCellSize(Object row) {}

	public Rectangle getCellBounds(ElementId row) {
		Table table = getTable();
		int index = table.getRows().getElementsBefore(row);
		int rowPos = table.getRowPosition(index);
		int rowSize = table.getRowLength(index);
		int padding = 0; // TODO Margin/padding between columns?
		if (table.atts().get(LayoutAttributes.orientation).get().isVertical())
			return new Rectangle(padding, rowPos, bounds().getWidth(), rowSize);
		else
			return new Rectangle(rowPos, padding, rowSize, bounds().getHeight());
	}

	@Override
	public SizeGuide getSizer(Orientation orientation) {
		if (orientation == getTable().atts().get(LayoutAttributes.orientation).get())
			return getTable().getSizer(orientation);
		else {
			ObservableCollection<?> rows = getTable().getRows();
			return new SizeGuide.GenericSizeGuide() {
				@Override
				public int get(LayoutGuideType type, int crossSize, boolean csMax) {
					return BaseLayoutUtils.getBoxLayoutCrossSize(new Iterable<QuickElement>() {
						@Override
						public Iterator<QuickElement> iterator() {
							return new Iterator<QuickElement>() {
								private ElementId theRow = CollectionElement.getElementId(rows.getTerminalElement(true));

								@Override
								public boolean hasNext() {
									return theRow != null;
								}

								@Override
								public QuickElement next() {
									try (Transaction t = theRenderer.holdEvents(true, true)) {
										theRenderValue.set(rows.getElement(theRow).get(), null);
										theRow = CollectionElement.getElementId(rows.getAdjacentElement(theRow, true));
										if (theRow == null)
											theRenderValue.set(null, null);
									}
									return theRenderer;
								}
							};
						}
					}, orientation.opposite(), type, crossSize, csMax, new LayoutSize());
				}

				@Override
				public int getBaseline(int size) {
					return 0;
				}
			};
		}
	}

	@Override
	public void doLayout() {
		if (theHoveredRow != null)
			theHover.bounds().set(getCellBounds(theHoveredRow), null);
		if (isSelected && theSelectedRow != null)
			theEditor.bounds().set(getCellBounds(theSelectedRow), null);
	}

	@Override
	public void paintSelf(Graphics2D graphics, Rectangle area) {
		super.paintSelf(graphics, area);
		Table table = getTable();
		ObservableCollection<?> rows = table.getRows();
		if (rows.isEmpty())
			return;
		// Render all cells overlapping the given rectangle
		if (area == null)
			area = new Rectangle(0, 0, bounds().getX(), bounds().getY());
		Orientation orientation = table.atts().get(LayoutAttributes.orientation).get();
		int start, end;
		if (orientation.isVertical()) {
			start = area.y;
			end = area.getMaxY();
		} else {
			start = area.x;
			end = area.getMaxX();
		}
		int rowIdx = table.getRowAt(area.getPosition());
		if (rowIdx < 0) {
			if (table.getRowPosition(0) >= start)
				rowIdx = 0;
			else
				return;
		}
		ElementId row = rows.getElement(rowIdx).getElementId();
		int initIndex = rowIdx;
		int initPosition = table.getRowPosition(initIndex);
		if (initPosition < end) {
			paintChildren(new Iterable<QuickElement>() {
				@Override
				public Iterator<QuickElement> iterator() {
					return new Iterator<QuickElement>() {
						ElementId theRow = row;
						int theIndex = initIndex;
						int thePosition = initPosition;

						@Override
						public boolean hasNext() {
							return thePosition < end;
						}

						@Override
						public QuickElement next() {
							try (Transaction t = theRenderer.holdEvents(true, true)) {
								theRenderValue.set(rows.getElement(theRow).get(), null);
								int padding = 0; // TODO Margin/padding between columns?
								if (orientation.isVertical())
									theRenderer.bounds().setBounds(padding, thePosition, bounds().getWidth(), table.getRowLength(theIndex));
								else
									theRenderer.bounds().setBounds(thePosition, padding, table.getRowLength(theIndex),
										bounds().getHeight());
							}
							theRow = CollectionElement.getElementId(rows.getAdjacentElement(theRow, true));
							theIndex++;
							thePosition = table.getRowPosition(theIndex);
							return theRenderer;
						}
					};
				}
			}, graphics, area);
			try (Transaction t = theRenderer.holdEvents(true, true)) {
				theRenderValue.set(null, null);
			}
		}
	}

	@Override
	public QuickElementCapture[] paintChildren(Graphics2D graphics, Rectangle area) {
		List<QuickElement> children = new ArrayList<>(2);
		if (theHoveredRow != null)
			children.add(theHover);
		if (isSelected && theSelectedRow != null)
			children.add(theEditor);
		if (area == null)
			area = new Rectangle(0, 0, bounds().getWidth(), bounds().getHeight());
		return paintChildren(children, graphics, area);
	}
}
