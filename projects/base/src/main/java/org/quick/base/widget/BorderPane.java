package org.quick.base.widget;

/** Wraps an element (or set thereof) in a border */
public class BorderPane extends SimpleContainer {
	/** Creates a border pane */
	public BorderPane() {
	}

	/** @return The panel containing the contents of this border */
	public Block getContentPane() {
		return (Block) getElement(getTemplate().getAttachPoint("contents")).get();
	}
}
