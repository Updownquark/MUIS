package org.quick.widget.base;

import java.util.function.Consumer;

import org.quick.base.widget.Spinner;
import org.quick.core.QuickTextElement;
import org.quick.widget.core.QuickTemplateWidget;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;
import org.quick.widget.core.event.KeyBoardEvent;

public class SpinnerWidget extends QuickTemplateWidget {
	public SpinnerWidget(QuickWidgetDocument doc, Spinner element, QuickWidget parent) {
		super(doc, element, parent);
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

	@Override
	public Spinner getElement() {
		return (Spinner) super.getElement();
	}

	/**
	 * @param up Whether to get the up or down button
	 * @return The button adjuster (up or down) for this spinner
	 */
	protected ButtonWidget getAdjust(boolean up) {
		return (ButtonWidget) getElement(getTemplate().getAttachPoint(up ? "up" : "down")).get();
	}
}
