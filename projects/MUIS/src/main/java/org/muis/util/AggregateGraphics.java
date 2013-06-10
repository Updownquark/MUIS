package org.muis.util;

import java.awt.*;
import java.awt.RenderingHints.Key;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/** Distributes graphics operations to multiple graphics objects */
public class AggregateGraphics extends Graphics2D {
	private Graphics2D [] theComponents;

	/** @param components The graphics objects to operate on */
	public AggregateGraphics(Graphics2D... components) {
		theComponents = components;
	}

	@Override
	public void draw(Shape s) {
		for(Graphics2D component : theComponents)
			component.draw(s);
	}

	@Override
	public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
		boolean ret = true;
		for(int c = 0; c < theComponents.length; c++) {
			if(c == 0)
				ret = theComponents[c].drawImage(img, xform, obs);
			else
				theComponents[c].drawImage(img, xform, null);
		}
		return ret;
	}

	@Override
	public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
		for(Graphics2D component : theComponents)
			component.drawImage(img, op, x, y);
	}

	@Override
	public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
		for(Graphics2D component : theComponents)
			component.drawRenderedImage(img, xform);
	}

	@Override
	public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
		for(Graphics2D component : theComponents)
			component.drawRenderableImage(img, xform);
	}

	@Override
	public void drawString(String str, int x, int y) {
		for(Graphics2D component : theComponents)
			component.drawString(str, x, y);
	}

	@Override
	public void drawString(String str, float x, float y) {
		for(Graphics2D component : theComponents)
			component.drawString(str, x, y);
	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, int x, int y) {
		for(Graphics2D component : theComponents)
			component.drawString(iterator, x, y);
	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, float x, float y) {
		for(Graphics2D component : theComponents)
			component.drawString(iterator, x, y);
	}

	@Override
	public void drawGlyphVector(GlyphVector g, float x, float y) {
		for(Graphics2D component : theComponents)
			component.drawGlyphVector(g, x, y);
	}

	@Override
	public void fill(Shape s) {
		for(Graphics2D component : theComponents)
			component.fill(s);
	}

	@Override
	public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
		if(theComponents.length == 0)
			return false;
		else
			return theComponents[0].hit(rect, s, onStroke);
	}

	@Override
	public GraphicsConfiguration getDeviceConfiguration() {
		if(theComponents.length == 0)
			return null;
		else
			return theComponents[0].getDeviceConfiguration();
	}

	@Override
	public void setComposite(Composite comp) {
		for(Graphics2D component : theComponents)
			component.setComposite(comp);
	}

	@Override
	public void setPaint(Paint paint) {
		for(Graphics2D component : theComponents)
			component.setPaint(paint);
	}

	@Override
	public void setStroke(Stroke s) {
		for(Graphics2D component : theComponents)
			component.setStroke(s);
	}

	@Override
	public void setRenderingHint(Key hintKey, Object hintValue) {
		for(Graphics2D component : theComponents)
			component.setRenderingHint(hintKey, hintValue);
	}

	@Override
	public Object getRenderingHint(Key hintKey) {
		if(theComponents.length == 0)
			return null;
		else
			return theComponents[0].getRenderingHint(hintKey);
	}

	@Override
	public void setRenderingHints(Map<?, ?> hints) {
		for(Graphics2D component : theComponents)
			component.setRenderingHints(hints);
	}

	@Override
	public void addRenderingHints(Map<?, ?> hints) {
		for(Graphics2D component : theComponents)
			component.addRenderingHints(hints);
	}

	@Override
	public RenderingHints getRenderingHints() {
		if(theComponents.length == 0)
			return null;
		else
			return theComponents[0].getRenderingHints();
	}

	@Override
	public void translate(int x, int y) {
		for(Graphics2D component : theComponents)
			component.translate(x, y);
	}

	@Override
	public void translate(double tx, double ty) {
		for(Graphics2D component : theComponents)
			component.translate(tx, ty);
	}

	@Override
	public void rotate(double theta) {
		for(Graphics2D component : theComponents)
			component.rotate(theta);
	}

	@Override
	public void rotate(double theta, double x, double y) {
		for(Graphics2D component : theComponents)
			component.rotate(theta, x, y);
	}

	@Override
	public void scale(double sx, double sy) {
		for(Graphics2D component : theComponents)
			component.scale(sx, sy);
	}

	@Override
	public void shear(double shx, double shy) {
		for(Graphics2D component : theComponents)
			component.shear(shx, shy);
	}

	@Override
	public void transform(AffineTransform Tx) {
		for(Graphics2D component : theComponents)
			component.transform(Tx);
	}

	@Override
	public void setTransform(AffineTransform Tx) {
		for(Graphics2D component : theComponents)
			component.setTransform(Tx);
	}

	@Override
	public AffineTransform getTransform() {
		if(theComponents.length == 0)
			return null;
		else
			return theComponents[0].getTransform();
	}

	@Override
	public Paint getPaint() {
		if(theComponents.length == 0)
			return null;
		else
			return theComponents[0].getPaint();
	}

	@Override
	public Composite getComposite() {
		if(theComponents.length == 0)
			return null;
		else
			return theComponents[0].getComposite();
	}

	@Override
	public void setBackground(Color color) {
		for(Graphics2D component : theComponents)
			component.setBackground(color);
	}

	@Override
	public Color getBackground() {
		if(theComponents.length == 0)
			return null;
		else
			return theComponents[0].getBackground();
	}

	@Override
	public Stroke getStroke() {
		if(theComponents.length == 0)
			return null;
		else
			return theComponents[0].getStroke();
	}

	@Override
	public void clip(Shape s) {
		for(Graphics2D component : theComponents)
			component.clip(s);
	}

	@Override
	public FontRenderContext getFontRenderContext() {
		if(theComponents.length == 0)
			return null;
		else
			return theComponents[0].getFontRenderContext();
	}

	@Override
	public Graphics create() {
		Graphics2D [] copies = new Graphics2D[theComponents.length];
		for(int c = 0; c < copies.length; c++)
			copies[c] = (Graphics2D) theComponents[c].create();
		return new AggregateGraphics(copies);
	}

	@Override
	public Color getColor() {
		if(theComponents.length == 0)
			return null;
		else
			return theComponents[0].getColor();
	}

	@Override
	public void setColor(Color c) {
		for(Graphics2D component : theComponents)
			component.setColor(c);
	}

	@Override
	public void setPaintMode() {
		for(Graphics2D component : theComponents)
			component.setPaintMode();
	}

	@Override
	public void setXORMode(Color c1) {
		for(Graphics2D component : theComponents)
			component.setXORMode(c1);
	}

	@Override
	public Font getFont() {
		if(theComponents.length == 0)
			return null;
		else
			return theComponents[0].getFont();
	}

	@Override
	public void setFont(Font font) {
		for(Graphics2D component : theComponents)
			component.setFont(font);
	}

	@Override
	public FontMetrics getFontMetrics(Font f) {
		if(theComponents.length == 0)
			return null;
		else
			return theComponents[0].getFontMetrics(f);
	}

	@Override
	public Rectangle getClipBounds() {
		if(theComponents.length == 0)
			return null;
		else
			return theComponents[0].getClipBounds();
	}

	@Override
	public void clipRect(int x, int y, int width, int height) {
		for(Graphics2D component : theComponents)
			component.clipRect(x, y, width, height);
	}

	@Override
	public void setClip(int x, int y, int width, int height) {
		for(Graphics2D component : theComponents)
			component.setClip(x, y, width, height);
	}

	@Override
	public Shape getClip() {
		if(theComponents.length == 0)
			return null;
		else
			return theComponents[0].getClip();
	}

	@Override
	public void setClip(Shape clip) {
		for(Graphics2D component : theComponents)
			component.setClip(clip);
	}

	@Override
	public void copyArea(int x, int y, int width, int height, int dx, int dy) {
		for(Graphics2D component : theComponents)
			component.copyArea(x, y, width, height, dx, dy);
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
		for(Graphics2D component : theComponents)
			component.drawLine(x1, y1, x2, y2);
	}

	@Override
	public void fillRect(int x, int y, int width, int height) {
		for(Graphics2D component : theComponents)
			component.fillRect(x, y, width, height);
	}

	@Override
	public void clearRect(int x, int y, int width, int height) {
		for(Graphics2D component : theComponents)
			component.clearRect(x, y, width, height);
	}

	@Override
	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		for(Graphics2D component : theComponents)
			component.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
	}

	@Override
	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		for(Graphics2D component : theComponents)
			component.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
	}

	@Override
	public void drawOval(int x, int y, int width, int height) {
		for(Graphics2D component : theComponents)
			component.drawOval(x, y, width, height);
	}

	@Override
	public void fillOval(int x, int y, int width, int height) {
		for(Graphics2D component : theComponents)
			component.fillOval(x, y, width, height);
	}

	@Override
	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		for(Graphics2D component : theComponents)
			component.drawArc(x, y, width, height, startAngle, arcAngle);
	}

	@Override
	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		for(Graphics2D component : theComponents)
			component.fillArc(x, y, width, height, startAngle, arcAngle);
	}

	@Override
	public void drawPolyline(int [] xPoints, int [] yPoints, int nPoints) {
		for(Graphics2D component : theComponents)
			component.drawPolyline(xPoints, yPoints, nPoints);
	}

	@Override
	public void drawPolygon(int [] xPoints, int [] yPoints, int nPoints) {
		for(Graphics2D component : theComponents)
			component.drawPolygon(xPoints, yPoints, nPoints);
	}

	@Override
	public void fillPolygon(int [] xPoints, int [] yPoints, int nPoints) {
		for(Graphics2D component : theComponents)
			component.fillPolygon(xPoints, yPoints, nPoints);
	}

	@Override
	public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
		boolean ret = true;
		for(int c = 0; c < theComponents.length; c++) {
			if(c == 0)
				ret = theComponents[c].drawImage(img, x, y, observer);
			else
				theComponents[c].drawImage(img, x, y, null);
		}
		return ret;
	}

	@Override
	public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
		boolean ret = true;
		for(int c = 0; c < theComponents.length; c++) {
			if(c == 0)
				ret = theComponents[c].drawImage(img, x, y, width, height, observer);
			else
				theComponents[c].drawImage(img, x, y, width, height, null);
		}
		return ret;
	}

	@Override
	public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
		boolean ret = true;
		for(int c = 0; c < theComponents.length; c++) {
			if(c == 0)
				ret = theComponents[c].drawImage(img, x, y, bgcolor, observer);
			else
				theComponents[c].drawImage(img, x, y, bgcolor, null);
		}
		return ret;
	}

	@Override
	public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
		boolean ret = true;
		for(int c = 0; c < theComponents.length; c++) {
			if(c == 0)
				ret = theComponents[c].drawImage(img, x, y, width, height, bgcolor, observer);
			else
				theComponents[c].drawImage(img, x, y, width, height, bgcolor, null);
		}
		return ret;
	}

	@Override
	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
		boolean ret = true;
		for(int c = 0; c < theComponents.length; c++) {
			if(c == 0)
				ret = theComponents[c].drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
			else
				theComponents[c].drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
		}
		return ret;
	}

	@Override
	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor,
		ImageObserver observer) {
		boolean ret = true;
		for(int c = 0; c < theComponents.length; c++) {
			if(c == 0)
				ret = theComponents[c].drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
			else
				theComponents[c].drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, null);
		}
		return ret;
	}

	@Override
	public void dispose() {
		for(Graphics2D component : theComponents)
			component.dispose();
	}
}
