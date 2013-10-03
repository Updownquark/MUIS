package org.muis.core.model;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import org.muis.core.model.MuisDocumentModel.StyledSequence;
import org.muis.core.style.MuisStyle;

/** Stores and displays text in MUIS */
public interface MuisDocumentModel extends CharSequence, Iterable<StyledSequence> {
	/** A piece of text with style attributes */
	public static interface StyledSequence extends CharSequence {
		/** @return The style for this sequence */
		MuisStyle getStyle();
	}

	/** A piece of text with style attributes and metrics information. The content of a StyledSequenceMetrics never contains a line break. */
	public static interface StyledSequenceMetric extends StyledSequence {
		/** @return The width of this piece of text */
		float getWidth();

		/** @return The height of this piece of text */
		float getHeight();

		/** @return The baseline of this piece of text */
		float getBaseline();

		/** @return Whether this sequence is the first of a new line. This may return false for the first line. */
		boolean isNewLine();

		@Override
		StyledSequenceMetric subSequence(int start, int end);

		/**
		 * @param advance The x-coordinate, in pixels, relative to the left edge of this sequence
		 * @return The position, in characters, along this sequence that the given advance hits
		 */
		float getHitPosition(float advance);

		/**
		 * @param position The position, in characters, along this sequence
		 * @return The x-coordinate, in pixels, relative to the beginning of this sequence of the given position
		 */
		float getLocation(float position);

		/**
		 * Draws this sequence
		 *
		 * @param graphics The graphics to draw the text sequence to
		 * @param x The x-coordinate of the top left corner of the graphics to draw the text to
		 * @param y The y-coordinate of the top left corner of the graphics to draw the text to
		 */
		void draw(Graphics2D graphics, float x, float y);
	}

	/**
	 * @param position The position to get the style for
	 * @return The style at the given position
	 */
	MuisStyle getStyleAt(int position);

	/**
	 * @param position The position to begin iteration from
	 * @return This document's content starting from the given position
	 */
	Iterable<StyledSequence> iterateFrom(int position);

	/**
	 * @param start The starting position within the document
	 * @param breakWidth The width, in pixels, to break lines off at in the document
	 * @return The metrics for this document. The first metric's {@link StyledSequenceMetric#isNewLine() newLine} attribute may be false
	 *         even if a new line occurred at position start-1.
	 */
	Iterable<StyledSequenceMetric> metrics(int start, float breakWidth);

	/**
	 * @param x The x-coordinate, in pixels, relative to this document's top left corner
	 * @param y The y-coordinate, in pixels, relative to this document's top left corner
	 * @param breakWidth The width, in pixels, to break lines off at in the document
	 * @return The character position at the given point in the document
	 */
	float getPositionAt(float x, float y, int breakWidth);

	/**
	 * @param position The character position in the document
	 * @param breakWidth The width, in pixels, to break lines off at in the document
	 * @return The position in the document at the given character at the top of the line
	 */
	Point2D getLocationAt(float position, int breakWidth);

	/**
	 * Draws a portion of this document
	 *
	 * @param graphics The graphics to draw on
	 * @param window The window within this document to draw
	 * @param breakWidth The width, in pixels, to break lines off at in the document
	 */
	void draw(Graphics2D graphics, Rectangle window, int breakWidth);
}
