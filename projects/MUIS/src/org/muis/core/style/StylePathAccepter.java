package org.muis.core.style;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.event.AttributeChangedEvent;
import org.muis.core.event.MuisEvent;

/** Accepts paths for the style attribute */
public class StylePathAccepter implements MuisAttribute.PropertyPathAccepter, org.muis.core.event.MuisEventListener<Object> {
	/** The attribute path name for an element's self-style */
	public static final String SELF_STYLE = "self";

	/** The attribute path name for an element's heir-style */
	public static final String HEIR_STYLE = "heir";

	@Override
	public boolean accept(MuisElement element, String... path) {
		// TODO come back and add stateful styles
		if(path.length != 1)
			return false;
		return path[0].equals(SELF_STYLE) || path[0].equals(HEIR_STYLE);
	}

	@Override
	public void eventOccurred(MuisEvent<Object> event, MuisElement element) {
		if(!(event instanceof AttributeChangedEvent))
			return;
		AttributeChangedEvent<MuisStyle> ace = (AttributeChangedEvent<MuisStyle>) (MuisEvent<?>) event;
		MuisAttribute<MuisStyle> attr = ace.getAttribute();
		MuisStyle target;
		if(!(attr instanceof org.muis.core.MuisPathedAttribute)) {
			target = element.getStyle();
		} else {
			org.muis.core.MuisPathedAttribute<MuisStyle> pathed = (org.muis.core.MuisPathedAttribute<MuisStyle>) attr;
			if(pathed.getPath()[0].equals(SELF_STYLE)) {
				target = element.getStyle().getSelf();
			} else if(pathed.getPath()[0].equals(HEIR_STYLE)) {
				target = element.getStyle().getHeir();
			} else {
				// TODO come back and add stateful styles
				throw new IllegalStateException("Pathed style attribute " + attr + " not recognized");
			}
		}
		java.util.HashSet<StyleAttribute<?>> styleAtts = new java.util.HashSet<>();
		for(StyleAttribute<?> styleAtt : ace.getOldValue())
			styleAtts.add(styleAtt);
		for(StyleAttribute<?> styleAtt : ace.getValue()) {
			styleAtts.remove(styleAtt);
			target.set((StyleAttribute<Object>) styleAtt, ace.getValue().get(styleAtt));
		}
		for(StyleAttribute<?> styleAtt : styleAtts)
			target.clear(styleAtt);
	}

	@Override
	public boolean isLocal() {
		return true;
	}
}
