package org.quick.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import org.qommons.collect.BetterHashMap;
import org.qommons.collect.BetterMap;

public class DebugPlotter extends JComponent {
	public static final int HOVER_RESOLUTION = 4;
	public static final Supplier<Color> DEFAULT_COLOR = () -> Color.blue;
	public static final Supplier<String> DEFAULT_TEXT = () -> null;

	public static class ShapeHolder {
		final Shape shape;
		Supplier<Color> color;
		Supplier<String> text;

		ShapeHolder(Shape shape) {
			this.shape = shape;
			color = DEFAULT_COLOR;
			text = DEFAULT_TEXT;
		}

		public ShapeHolder setColor(Color color) {
			this.color = () -> color;
			return this;
		}

		public ShapeHolder setText(String text) {
			this.text = () -> text;
			return this;
		}

		public ShapeHolder setText(Supplier<String> text) {
			this.text = text;
			return this;
		}
	}

	private final BetterMap<Shape, ShapeHolder> theShapes;
	private final JPopupMenu thePopup;
	private final JLabel thePopupText;
	private Runnable theAction;

	public DebugPlotter() {
		theShapes = BetterHashMap.build().unsafe().buildMap();
		thePopup = new JPopupMenu();
		thePopupText = new JLabel();
		thePopup.add(thePopupText);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2)
					doubleClick();
			}
		});
		addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {}

			@Override
			public void mouseMoved(MouseEvent e) {
				Rectangle2D rect = new Rectangle2D.Double(e.getX() - HOVER_RESOLUTION / 2.0, e.getY() - HOVER_RESOLUTION / 2.0, //
					HOVER_RESOLUTION, HOVER_RESOLUTION);
				StringBuilder popupText = null;
				for (ShapeHolder shape : theShapes.values().reverse()) {
					if (shape.shape.intersects(rect)) {
						String text = shape.text.get();
						if (text != null) {
							if (popupText == null)
								popupText = new StringBuilder();
							else {
								popupText.insert(0, "<html>");
								popupText.append("<br>");
							}
							popupText.append(text);
						}
					}
				}
				if (popupText != null) {
					thePopupText.setText(popupText.toString());
					thePopup.show(DebugPlotter.this, e.getX(), e.getY());
				} else
					thePopup.setVisible(false);
			}
		});
	}

	public DebugPlotter setAction(Runnable action) {
		theAction = action;
		return this;
	}

	public ShapeHolder add(Shape shape) {
		ShapeHolder holder = theShapes.computeIfAbsent(shape, ShapeHolder::new);
		repaint();
		return holder;
	}

	public DebugPlotter remove(Shape shape) {
		theShapes.remove(shape);
		repaint();
		return this;
	}

	@Override
	public void removeAll() {
		theShapes.clear();
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(Color.white);
		g2d.fillRect(0, 0, getWidth(), getHeight());
		for (ShapeHolder shape : theShapes.values()) {
			g2d.setColor(shape.color.get());
			g2d.fill(shape.shape);
			g2d.draw(shape.shape);
		}
	}

	private void doubleClick() {
		if (theAction != null)
			theAction.run();
	}

	public JFrame showFrame(String title, Rectangle bounds) {
		JFrame frame = new JFrame(title);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(this, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		if (bounds != null)
			frame.setBounds(bounds);
		else
			frame.setSize(500, 400);
		frame.setVisible(true);
		return frame;
	}
}
