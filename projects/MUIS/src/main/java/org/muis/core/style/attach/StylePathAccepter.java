package org.muis.core.style.attach;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.event.AttributeChangedEvent;
import org.muis.core.style.MuisStyle;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.stateful.MutableStatefulStyle;
import org.muis.core.style.stateful.StateExpression;

/** Accepts paths for the style attribute */
public class StylePathAccepter implements MuisAttribute.PropertyPathAccepter, org.observe.Action<AttributeChangedEvent<MuisStyle>> {
	/** The attribute path name for an element's self-style */
	public static final String SELF_STYLE = "self";

	/** The attribute path name for an element's heir-style */
	public static final String HEIR_STYLE = "heir";

	@Override
	public boolean accept(MuisElement element, String... path) {
		int idx = 0;
		if(idx < path.length && (path[idx].equals(SELF_STYLE) || path[idx].equals(HEIR_STYLE)))
			idx++;
		for(; idx < path.length; idx++) {
			String [] states = path[idx].split("_");
			for(String state : states) {
				if(state.startsWith("-"))
					state = state.substring(1);
				if(element.state().getState(state) == null)
					return false;
			}
		}
		return true;
	}

	@Override
	public void act(AttributeChangedEvent<MuisStyle> event) {
		MuisAttribute<MuisStyle> attr = event.getAttribute();
		MuisElement element = event.getElement();
		MutableStatefulStyle target;
		StateExpression expr = null;
		if(!(attr instanceof org.muis.core.MuisPathedAttribute)) {
			target = element.getStyle();
		} else {
			org.muis.core.MuisPathedAttribute<MuisStyle> pathed = (org.muis.core.MuisPathedAttribute<MuisStyle>) attr;
			int idx = 0;
			if(pathed.getPath()[0].equals(SELF_STYLE)) {
				target = element.getStyle().getSelf();
				idx++;
			} else if(pathed.getPath()[0].equals(HEIR_STYLE)) {
				target = element.getStyle().getHeir();
				idx++;
			} else
				target = element.getStyle();
			if(idx < pathed.getPath().length) {
				java.util.ArrayList<StateExpression> ors = new java.util.ArrayList<>();
				for(; idx < pathed.getPath().length; idx++) {
					String [] states = pathed.getPath()[idx].split("_");
					boolean [] nots = new boolean[states.length];
					for(int i = 0; i < states.length; i++)
						if(states[i].startsWith("-")) {
							nots[i] = true;
							states[i] = states[i].substring(1);
						}
					if(states.length == 0) {
						StateExpression simple = new StateExpression.Simple(element.state().getState(states[0]));
						if(nots[0])
							simple = simple.not();
						ors.add(simple);
					} else {
						StateExpression [] simples = new StateExpression[states.length];
						for(int s = 0; s < states.length; s++) {
							simples[s] = new StateExpression.Simple(element.state().getState(states[s]));
							if(nots[s])
								simples[s] = simples[s].not();
						}
						ors.add(new StateExpression.Or(simples));
					}
				}
				if(ors.size() == 1)
					expr = ors.get(0);
				else if(!ors.isEmpty())
					expr = new StateExpression.And(ors.toArray(new StateExpression[ors.size()]));
			}
		}
		java.util.HashSet<StyleAttribute<?>> clearStyleAtts = new java.util.HashSet<>();
		if(event.getOldValue() != null)
			for(StyleAttribute<?> styleAtt : event.getOldValue().attributes())
				clearStyleAtts.add(styleAtt);
		if(event.getValue() != null)
			for(StyleAttribute<?> styleAtt : event.getValue().attributes()) {
				clearStyleAtts.remove(styleAtt);
				target.set((StyleAttribute<Object>) styleAtt, expr, event.getValue().get(styleAtt, true).get());
			}
		for(StyleAttribute<?> styleAtt : clearStyleAtts)
			target.clear(styleAtt, expr);
	}
}
