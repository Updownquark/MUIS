package org.quick.base.widget;

import java.awt.Graphics2D;
import java.util.*;
import java.util.function.Consumer;

import org.observe.*;
import org.observe.collect.ObservableCollection;
import org.qommons.Transaction;
import org.qommons.collect.CollectionElement;
import org.qommons.collect.ElementId;
import org.qommons.collect.MutableCollectionElement;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.QuickElement;
import org.quick.core.QuickTemplate;
import org.quick.core.layout.*;
import org.quick.core.model.ModelAttributes;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.Template;
import org.quick.widget.base.layout.BaseLayoutUtils;
import org.quick.widget.core.QuickElementCapture;
import org.quick.widget.core.layout.SizeGuide;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

@QuickElementType(attributes = { //
	@AcceptAttribute(declaringClass = ModelAttributes.class, field = "name", required = false)//
})
@Template(location = "../../../../table-column.qts")
public class TableColumn<R, C> extends QuickTemplate {
	private AttachPoint<QuickElement> theRendererAP;
	private AttachPoint<QuickElement> theHoverAP;
	private AttachPoint<QuickElement> theEditorAP;

	private ElementId theColumnElement;

	private QuickElement theRenderer;
	private QuickElement theHover;
	private QuickElement theEditor;

	private SettableValue<MutableCollectionElement<R>> theHoverElement;
	private SettableValue<MutableCollectionElement<R>> theEditorElement;
	private SimpleObservable<Void> theHoverRefresh;
	private ObservableValue<R> theHoverValue;
	private SimpleObservable<Void> theEditorRefresh;
	private ObservableValue<R> theEditorValue;

	public TableColumn() {
		life().runWhen(() -> {
			ObservableCollection<R> rows = getTable().getRows();
			TypeToken<MutableCollectionElement<R>> mceType = new TypeToken<MutableCollectionElement<R>>() {}
				.where(new TypeParameter<R>() {}, rows.getType());
			theHoverElement = new SimpleSettableValue<>(mceType, true, getAttributeLocker(), null);
			theEditorElement = new SimpleSettableValue<>(mceType, true, getAttributeLocker(), null);
			theHoverRefresh = new SimpleObservable<>(null, false, getAttributeLocker(), null);
			theHoverValue = theHoverElement.refresh(theHoverRefresh).map(rows.getType(), el -> el.get());
			theEditorRefresh = new SimpleObservable<>(null, false, getAttributeLocker(), null);
			theEditorValue = theEditorElement.refresh(theEditorRefresh).map(rows.getType(), el -> el.get());

			getResourcePool().poolValue(getTable().getHoveredColumn()).changes().act(new Consumer<ObservableValueEvent<ElementId>>() {
				private Subscription theHoverElSub;

				@Override
				public void accept(ObservableValueEvent<ElementId> evt) {
					if(evt.getNewValue()==null || !evt.getNewValue().equals(getColumnElement())){
						if (theHoverElSub != null) {
							theHoverElSub.unsubscribe();
							theHoverElSub = null;
							theHoverElement.set(null, evt);
						} else {
							theHoverElSub = getResourcePool().poolValue(getTable().getHoveredRow()).changes().act(evt2 -> {
								theHoverElement.set(rows.mutableElement(evt2.getNewValue()), evt2);
							});
						}
					}
				}
			});
			getResourcePool().poolValue(getTable().getSelectedColumn()).changes().act(new Consumer<ObservableValueEvent<ElementId>>() {
				private Subscription theEditorElSub;

				@Override
				public void accept(ObservableValueEvent<ElementId> evt) {
					if (evt.getNewValue() == null || !evt.getNewValue().equals(getColumnElement())) {
						if (theEditorElSub != null) {
							theEditorElSub.unsubscribe();
							theEditorElSub = null;
							theEditorElement.set(null, evt);
						} else {
							theEditorElSub = getResourcePool().poolValue(getTable().getSelectedRow()).changes().act(evt2 -> {
								theEditorElement.set(rows.mutableElement(evt2.getNewValue()), evt2);
							});
						}
					}
				}
			});
			getResourcePool().build(rows::onChange, evt -> {
				switch (evt.getType()) {
				case add:
					break;
				case remove:
					break; // The table takes care of this
				case set:
					if (evt.getElementId().equals(CollectionElement.getElementId(theHoverElement.get())))
						theHoverRefresh.onNext(null);
					if (evt.getElementId().equals(CollectionElement.getElementId(theEditorElement.get())))
						theEditorRefresh.onNext(null);
					break;
				}
			}).onSubscribe(r -> {
				if (getTable().getHoveredRow().equals(CollectionElement.getElementId(theHoverElement.get())))
					theHoverRefresh.onNext(null);
				if (getTable().getSelectedRow().equals(CollectionElement.getElementId(theEditorElement.get())))
					theEditorRefresh.onNext(null);
			}).unsubscribe((r, s) -> s.unsubscribe());

			theRendererAP = (AttachPoint<QuickElement>) getTemplate().getAttachPoint("renderer");
			theHoverAP = (AttachPoint<QuickElement>) getTemplate().getAttachPoint("hover");
			theEditorAP = (AttachPoint<QuickElement>) getTemplate().getAttachPoint("editor");

			// Inject the row value into the renderer, hover, and editor models
			addModelValue(getTable().getRows().getType(), getTable().atts().get(Table.rowValueModelName).get(),
				getTable().atts().get(Table.rowElementModelName).get(), false);

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
						// TODO Install column and row model values
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
	}

	public Table<R, C> getTable() {
		QuickElement parent = getParent().get();
		if (parent != null && !(parent instanceof Table)) {
			msg().fatal("table-column elements must be the child of a table", "parent", parent);
			return null;
		}
		return (Table<R, C>) parent;
	}

	public ObservableValue<MutableCollectionElement<R>> getHoverElement() {
		return theHoverElement.unsettable();
	}

	public ObservableValue<MutableCollectionElement<R>> getEditorElement() {
		return theEditorElement.unsettable();
	}

	public ObservableValue<R> getHoverValue() {
		return theHoverValue;
	}

	public ObservableValue<R> getEditorValue() {
		return theEditorValue;
	}

	void initColumnElement(ElementId columnElement) {
		theColumnElement = columnElement;
	}

	public ElementId getColumnElement() {
		return theColumnElement;
	}

	void initColumnValue(TypeToken<C> columnType, String columnValueName, String columnElementName) {
		addModelValue(columnType, columnValueName, columnElementName, true);
	}

	void setColumnValue(MutableCollectionElement<C> columnValue) {
		int todo = todo; // TODO
	}

	private <T> void addModelValue(TypeToken<T> type, String valueName, String elementName, boolean forLocal) {
		int todo = todo; // TODO
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
	}

	private void updateHover() {
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
