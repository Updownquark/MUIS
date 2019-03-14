package org.quick.base.widget;

import java.util.function.Consumer;

import org.observe.*;
import org.observe.collect.ObservableCollection;
import org.observe.util.TypeTokens;
import org.qommons.collect.CollectionElement;
import org.qommons.collect.ElementId;
import org.qommons.collect.MutableCollectionElement;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.QuickElement;
import org.quick.core.QuickTemplate;
import org.quick.core.model.ModelAttributes;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.Template;

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

	private ElementId theColumnId;

	private final SettableValue<QuickElement> theRenderer;
	private final SettableValue<QuickElement> theHover;
	private final SettableValue<QuickElement> theEditor;

	private ObservableValue<MutableCollectionElement<C>> theColumnElement;
	private SimpleObservable<Void> theColumnRefresh;
	private ObservableValue<C> theColumnValue;
	private SettableValue<CollectionElement<R>> theRenderElement;
	private ObservableValue<R> theRenderValue;
	private SettableValue<MutableCollectionElement<R>> theHoverElement;
	private SimpleObservable<Void> theHoverRefresh;
	private ObservableValue<R> theHoverValue;
	private SettableValue<MutableCollectionElement<R>> theEditorElement;
	private SimpleObservable<Void> theEditorRefresh;
	private ObservableValue<R> theEditorValue;

	public TableColumn() {
		theRenderer = new SimpleSettableValue<>(TypeTokens.get().of(QuickElement.class), false, getAttributeLocker(), null);
		theHover = new SimpleSettableValue<>(TypeTokens.get().of(QuickElement.class), false, getAttributeLocker(), null);
		theEditor = new SimpleSettableValue<>(TypeTokens.get().of(QuickElement.class), false, getAttributeLocker(), null);

		life().runWhen(() -> {
			ObservableCollection<R> rows = getTable().getRows();
			theRenderElement = new SimpleSettableValue<>(
				new TypeToken<CollectionElement<R>>() {}.where(new TypeParameter<R>() {}, rows.getType()), true, getAttributeLocker(),
				null);
			theRenderValue = theRenderElement.map(rows.getType(), el -> el.get());
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
					if (evt.getNewValue() == null || !evt.getNewValue().equals(getColumnId())) {
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
					if (evt.getNewValue() == null || !evt.getNewValue().equals(getColumnId())) {
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
							theHover.set(el, cause);
						else
							theEditor.set(el, cause);
						getElement(ap).set(el, cause);
					});
					isCausingChange = false;
				}
			}
			RenderHoverEditor rhe = new RenderHoverEditor();
			getElement(theRendererAP).changes().act(evt -> {
				if (evt.isInitial())
					theRenderer.set(evt.getNewValue(), evt);
				else
					rhe.renderChanged(evt.getNewValue(), evt);
			});
			getElement(theHoverAP).changes().act(evt -> {
				if (evt.isInitial()) {
					rhe.configuredHover = evt.getNewValue();
					theHover.set(evt.getNewValue(), evt);
				} else
					rhe.hoverChanged(evt.getNewValue(), evt);
			});
			getElement(theEditorAP).changes().act(evt -> {
				if (evt.isInitial()) {
					rhe.configuredEditor = evt.getNewValue();
					theEditor.set(evt.getNewValue(), evt);
				} else
					rhe.editorChanged(evt.getNewValue(), evt);
			});

			// TODO Inject the row values into the renderer, hover, and editor models
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

	public ObservableValue<MutableCollectionElement<C>> getColumnElement() {
		return theColumnElement;
	}

	public ObservableValue<C> getColumnValue() {
		return theColumnValue;
	}

	public SettableValue<CollectionElement<R>> getRenderElement() {
		return theRenderElement;
	}

	public ObservableValue<R> getRenderValue() {
		return theRenderValue;
	}

	public ObservableValue<MutableCollectionElement<R>> getHoverElement() {
		return theHoverElement.unsettable();
	}

	public ObservableValue<R> getHoverValue() {
		return theHoverValue;
	}

	public ObservableValue<MutableCollectionElement<R>> getEditorElement() {
		return theEditorElement.unsettable();
	}

	public ObservableValue<R> getEditorValue() {
		return theEditorValue;
	}

	public SettableValue<QuickElement> getRenderer() {
		return theRenderer;
	}

	public SettableValue<QuickElement> getHover() {
		return theHover;
	}

	public SettableValue<QuickElement> getEditor() {
		return theEditor;
	}

	void initColumnId(ElementId columnId) {
		theColumnId = columnId;
	}

	public ElementId getColumnId() {
		return theColumnId;
	}

	void setColumnValue(MutableCollectionElement<C> columnValue, String columnElementName, String columnValueName) {
		theColumnElement = ObservableValue.of(
			new TypeToken<MutableCollectionElement<C>>() {}.where(new TypeParameter<C>() {}, getTable().getColumnValues().getType()),
			columnValue);
		theColumnRefresh = new SimpleObservable<>(null, false, getAttributeLocker(), null);
		theColumnValue = theColumnElement.refresh(theColumnRefresh).map(getTable().getColumnValues().getType(), el -> el.get());
		// TODO Inject the column values into the renderer, hover, and editor models
	}

	void updateColumnValue() {
		theColumnRefresh.onNext(null);
	}

	@Override
	public TableColumn<R, C> copy(QuickElement parent, Consumer<QuickElement> postProcess) throws IllegalStateException {
		return (TableColumn<R, C>) super.copy(parent, postProcess);
	}
}
