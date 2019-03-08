package org.quick.widget.base;

import org.observe.ObservableAction;
import org.observe.SettableValue;
import org.observe.util.TypeTokens;
import org.quick.base.BaseConstants;
import org.quick.base.widget.ToggleButton;
import org.quick.core.QuickConstants;
import org.quick.core.mgr.StateEngine;
import org.quick.core.model.ModelAttributes;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;

import com.google.common.reflect.TypeToken;

public class ToggleButtonWidget extends ButtonWidget {
	private StateEngine.StateController theSelectedController;

	private SettableValue<Boolean> theValue;

	public ToggleButtonWidget(QuickWidgetDocument doc, ToggleButton element, QuickWidget parent) {
		super(doc, element, parent);
		theSelectedController = getElement().state().control(BaseConstants.States.SELECTED);
		getElement().life().runWhen(() -> {
			if (getElement().atts().get(ModelAttributes.action).get() == null) {
				try {
					getElement().atts().get(ModelAttributes.action).set(ObservableAction.nullAction(TypeTokens.get().OBJECT, null), null);
				} catch (IllegalArgumentException e) {
					getElement().msg().error("Could not set default value for action", e);
				}
			}
		}, QuickConstants.CoreStage.STARTUP.toString(), -1);
		getElement().life().runWhen(() -> {
			theValue.changes().act(evt -> theSelectedController.setActive(evt.getNewValue(), evt));
		}, QuickConstants.CoreStage.INITIALIZED.toString(), 1);
	}

	@Override
	public ToggleButton getElement() {
		return (ToggleButton) super.getElement();
	}

	@Override
	protected ObservableAction<?> createAction() {
		theValue = getElement().atts().get(ModelAttributes.selected);
		theValue = theValue.map(theValue.getType(), b -> b == null ? false : b, b -> b, null);
		return ObservableAction.and(TypeToken.of(Object.class), ObservableAction.flatten(getElement().atts().get(ModelAttributes.action)),
			theValue.assignmentTo(theValue.map(v -> !v)));
	}
}
