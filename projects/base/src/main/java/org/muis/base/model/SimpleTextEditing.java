package org.muis.base.model;

import org.muis.base.widget.SimpleTextWidget;
import org.muis.core.MuisElement;
import org.muis.core.event.CharInputEvent;
import org.muis.core.model.MuisBehavior;

public class SimpleTextEditing implements MuisBehavior<SimpleTextWidget> {
	private org.muis.core.event.CharInputListener theInputListener;

	public SimpleTextEditing() {
		theInputListener = new org.muis.core.event.CharInputListener(false) {
			@Override
			public void keyTyped(CharInputEvent evt, MuisElement element) {
				charInput((SimpleTextWidget) element, evt.getChar());
			}
		};
	}

	@Override
	public void install(SimpleTextWidget element) {
		// TODO Auto-generated method stub

	}

	@Override
	public void uninstall(SimpleTextWidget element) {
		// TODO Auto-generated method stub

	}

	protected void charInput(SimpleTextWidget widget, char ch) {
		// TODO Auto-generated method stub

	}
}
