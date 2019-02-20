package org.quick.base.widget;

import java.awt.Graphics2D;
import java.util.Iterator;
import java.util.Objects;

import org.observe.SimpleSettableValue;
import org.observe.collect.ObservableCollection;
import org.qommons.collect.ElementId;
import org.quick.core.*;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.event.MouseEvent;
import org.quick.core.event.MouseEvent.MouseEventType;
import org.quick.core.layout.LayoutAttributes;
import org.quick.core.layout.SizeGuide;
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
			rows.changes().act(evt -> {
				updateHover();
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
			if (getTable().atts().get(Table.columns).get() != null) {
				String columnValueName = getTable().atts().get(Table.columnValueModelName).get();
				String columnElementName = getTable().atts().get(Table.columnElementModelName).get();
				// TODO Inject the column value into the renderer, hover, and editor
			}
			// TODO Inject the appropriate row value into the renderer, hover, and editor
			// TODO Also deactivate the attach points somehow when they are not in use
			// (so the editor is not listening to the last selected value, for example)

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
			events().filterMap(MouseEvent.mouse.addTypes(MouseEventType.moved)).act(evt -> {
				theHoveredPoint = evt.getPosition(TableColumn.this);
				updateHover();
			});
			events().filterMap(MouseEvent.mouse.addTypes(MouseEventType.exited)).act(evt -> {
				theHoveredPoint = null;
				updateHover();
			});
			events().filterMap(MouseEvent.mouse.addTypes(MouseEventType.pressed)).act(evt -> {
				ElementId row = getTable().getRowAt(evt.getPosition(TableColumn.this));
				if (isSelected && Objects.equals(theSelectedRow, row))
					return;
				getTable().setSelected(this, row);
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
		else if (row.equals(theSelectedRow))
			theEditorValue.set(getTable().getRows().getElement(row).get(), null);
		else
			repaint(getCellBounds(row), false);
	}

	public void setSelectedRow(ElementId row) {
		if (row != theSelectedRow) {
			theSelectedRow = row;
			if (isSelected) {
				if (row == null) {
					// Disable editor
				} else
					theEditorValue.set(getTable().getRows().getElement(row).get(), null);
				relayout(false);
			} else
				repaint(getCellBounds(row), false);
		}
	}

	public void setSelected(boolean selected) {
		if (isSelected == selected)
			return;
		isSelected = selected;
		relayout(false);
	}

	private void updateHover() {
		ElementId row;
		if (theHoveredPoint == null)
			row = null;
		else {
			row = getTable().getRowAt(theHoveredPoint);
			if (isSelected && Objects.equals(theSelectedRow, row))
				row = null;
		}
		if (Objects.equals(theHoveredRow, row))
			return;
		theHoveredRow = row;
		relayout(false);
	}

	public SizeGuide getCellSize(Object row) {
	}

	public Rectangle getCellBounds(ElementId row) {
		Table table = getTable();
		int rowPos = table.getRowPosition(table.getRows().getElementsBefore(row));
		int rowSize = table.getRowLength(rowPos);
		if (table.atts().get(LayoutAttributes.orientation).get().isVertical())
			return new Rectangle(bounds().getX(), rowPos, bounds().getWidth(), rowSize);
		else
			return new Rectangle(rowPos, bounds().getY(), rowSize, bounds().getHeight());
	}

	@Override
	public void doLayout() {
		// TODO Move and set the row value for
		// TODO Auto-generated method stub
	}

	@Override
	public void paintSelf(Graphics2D graphics, Rectangle area) {
		super.paintSelf(graphics, area);
		if (getTable().getRows().isEmpty())
			return;
		// Render all cells overlapping the given rectangle
		if (area == null)
			area = new Rectangle(0, 0, bounds().getX(), bounds().getY());
		int start, end;
		if (getTable().atts().get(LayoutAttributes.orientation).get().isVertical()) {
			start = area.y;
			end = area.getMaxY();
		} else {
			start = area.x;
			end = area.getMaxX();
		}
		ElementId row = getTable().getRowAt(area.getPosition());
		if (row == null) {
			if (getTable().getRowPosition(0) >= start)
				row = getTable().getRows().getTerminalElement(true).getElementId();
			else
				return;
		}
		ElementId fRow = row;
		int index = getTable().getRows().getElementsBefore(row);
		int position = getTable().getRowPosition(index);
		paintChildren(new Iterable<QuickElement>() {
			@Override
			public Iterator<QuickElement> iterator() {
				return new Iterator<QuickElement>() {
					ElementId theRow = fRow;
					int theIndex = index;
					int thePosition = position;

					@Override
					public boolean hasNext() {
						// TODO Auto-generated method stub
					}

					@Override
					public QuickElement next() {
						// TODO Auto-generated method stub
					}
				};
			}
		}, graphics, area);
	}

	@Override
	public QuickElementCapture[] paintChildren(Graphics2D graphics, Rectangle area) {
		// TODO Delegate to the active editor
	}
}
