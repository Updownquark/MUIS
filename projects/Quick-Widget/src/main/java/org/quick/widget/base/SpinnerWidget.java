package org.quick.widget.base;

import java.util.function.Consumer;

import org.quick.base.widget.Spinner;
import org.quick.core.QuickDefinedWidget;
import org.quick.core.QuickException;
import org.quick.core.QuickTextElement;
import org.quick.widget.core.QuickTemplateWidget;
import org.quick.widget.core.QuickWidgetDocument;
import org.quick.widget.core.event.KeyBoardEvent;

public class SpinnerWidget<E extends Spinner> extends QuickTemplateWidget<E> {
	@Override
	public void init(QuickWidgetDocument document, E element, QuickDefinedWidget<QuickWidgetDocument, ?> parent) throws QuickException {
		super.init(document, element, parent);
		events().filterMap(KeyBoardEvent.key).act(new Consumer<KeyBoardEvent>() {
			private ButtonWidget.ClickControl theUpControl;
			private ButtonWidget.ClickControl theDownControl;

			@Override
			public void accept(KeyBoardEvent event) {
				theUpControl = ButtonWidget.release(theUpControl, event);
				theDownControl = ButtonWidget.release(theDownControl, event);
				if (Boolean.TRUE.equals(getElement().atts().get(QuickTextElement.multiLine).get())) {
					return;
				}
				if (event.getKeyCode() == KeyBoardEvent.KeyCode.UP_ARROW) {
					if (event.wasPressed())
						theUpControl = getAdjust(true).press(event);
				} else if (event.getKeyCode() == KeyBoardEvent.KeyCode.DOWN_ARROW) {
					if (event.wasPressed())
						theDownControl = getAdjust(false).press(event);
				}
			}
		});
	}

	/**
	 * @param up Whether to get the up or down button
	 * @return The button adjuster (up or down) for this spinner
	 */
	protected ButtonWidget getAdjust(boolean up) {
		return (ButtonWidget) getChild(getElement().getAdjust(up));
	}
}
