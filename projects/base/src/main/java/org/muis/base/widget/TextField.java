package org.muis.base.widget;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.muis.core.event.MuisEvent;
import org.muis.core.mgr.MuisState;
import org.muis.core.model.SimpleDocumentModel;
import org.muis.core.model.SimpleDocumentModel.ContentChangeEvent;
import org.muis.core.model.SimpleDocumentModel.SelectionChangeEvent;
import org.muis.core.style.FontStyle;
import org.muis.core.tags.Template;

/** A simple widget that takes and displays text input from the user */
@Template(location = "../../../../text-field.muis")
public class TextField extends org.muis.core.MuisTemplate implements SimpleTextWidget {
	private SimpleDocumentModel theDocument;

	private volatile BufferedImage theCursorImage;

	private volatile long theLastCursorReset;

	/** Creates a text field */
	public TextField() {
		life().runWhen(new Runnable() {
			@Override
			public void run() {
				initDocument();
				getValueElement().state().addListener(org.muis.core.MuisConstants.States.FOCUS,
					new org.muis.core.mgr.StateEngine.StateListener() {
						org.muis.motion.Animation theCursorBlinkAnimation = new org.muis.motion.Animation() {
							@Override
							public boolean update(long time) {
								if(!getValueElement().state().is(org.muis.core.MuisConstants.States.FOCUS))
									return true;
								BufferedImage cursorImage = theCursorImage;
								if(cursorImage != null) {
									int cursor = getDocumentModel().getCursor();
									java.awt.geom.Point2D cursorLoc2D = getDocumentModel().getLocationAt(cursor,
										getElement(getTemplate().getAttachPoint("value")).bounds().getWidth());
									int x = (int) Math.round(cursorLoc2D.getX());
									int y = (int) Math.round(cursorLoc2D.getY());
									repaint(new Rectangle(x, y, cursorImage.getWidth(), cursorImage.getHeight()), true);
								} else
									repaint(null, true);
								return false;
							}

							@Override
							public long getMaxFrequency() {
								long blink = getStyle().getSelf().get(org.muis.base.style.TextEditStyle.cursorBlink);
								if(blink <= 0)
									return 100;
								return blink/2;
							}
						};

						@Override
						public void entered(MuisState state, MuisEvent<?> cause) {
							resetBlink();
							org.muis.motion.AnimationManager.get().start(theCursorBlinkAnimation);
						}

						@Override
						public void exited(MuisState state, MuisEvent<?> cause) {
						}
					});
				getDocumentModel().addContentListener(new SimpleDocumentModel.ContentListener() {
					@Override
					public void contentChanged(ContentChangeEvent evt) {
						if(evt.getIndex() == getDocumentModel().getCursor())
							resetCursorImage();
					}
				});
				getDocumentModel().addSelectionListener(new SimpleDocumentModel.SelectionListener() {
					@Override
					public void selectionChanged(SelectionChangeEvent evt) {
						resetCursorImage();
						resetBlink();
					}
				});
			}
		}, org.muis.core.MuisConstants.CoreStage.INIT_CHILDREN.toString(), 1);
	}

	/** @return The text element containing the text that is the value of this text field */
	protected org.muis.core.MuisTextElement getValueElement() {
		return (org.muis.core.MuisTextElement) getElement(getTemplate().getAttachPoint("value"));
	}

	/**
	 * Initializes the document for this text field. This may be overridden and used by subclasses but should never be called directly
	 * except as the super call from the extending method.
	 */
	protected void initDocument() {
		if(theDocument != null)
			throw new IllegalStateException("Document model is already initialized");
		theDocument = getValueElement().getDocumentModel();
		new org.muis.base.model.SimpleTextEditing().install(this);
	}

	@Override
	public SimpleDocumentModel getDocumentModel() {
		return theDocument;
	}

	@Override
	public void paintSelf(Graphics2D graphics, Rectangle area) {
		super.paintSelf(graphics, area);
		if(getValueElement().state().is(org.muis.core.MuisConstants.States.FOCUS) && isCursorOn()) {
			BufferedImage cursorImage = theCursorImage;
			if(cursorImage == null)
				theCursorImage = cursorImage = genCursorImage(graphics);
			java.awt.geom.Point2D cursorLoc2D = getDocumentModel().getLocationAt(getDocumentModel().getCursor(),
				getElement(getTemplate().getAttachPoint("value")).bounds().getWidth());
			java.awt.Point cursorLoc = new java.awt.Point((int) Math.round(cursorLoc2D.getX()), (int) Math.round(cursorLoc2D.getY()));
			System.out.println("Cursor on: " + cursorLoc.x + ", " + cursorLoc.y + ", " + cursorImage.getWidth() + ", "
				+ cursorImage.getHeight() + "; area=" + area);
			if(area == null || area.intersects(cursorLoc.x, cursorLoc.y, cursorImage.getWidth(), cursorImage.getHeight()))
				graphics.drawImage(cursorImage, cursorLoc.x, cursorLoc.y, cursorImage.getWidth(), cursorImage.getHeight(), null);
		} else
			System.out.println("Cursor off");
	}

	private boolean isCursorOn() {
		long interval = getStyle().getSelf().get(org.muis.base.style.TextEditStyle.cursorBlink);
		if(interval == 0)
			return true;
		if(interval < 0)
			return false;
		if(getDocumentModel().getSelectionAnchor() != getDocumentModel().getCursor())
			return true;
		long timeDiff = System.currentTimeMillis() - theLastCursorReset;
		return (timeDiff * 2 / interval) % 2 == 0;
	}

	private void resetBlink() {
		theLastCursorReset = System.currentTimeMillis();
	}

	private void resetCursorImage() {
		theCursorImage = null;
	}

	private BufferedImage genCursorImage(Graphics2D graphics) {
		org.muis.core.style.MuisStyle cursorStyle = getDocumentModel().getStyleAt(getDocumentModel().getCursor());
		java.awt.Font cursorFont = org.muis.util.MuisUtils.getFont(cursorStyle);
		float ascent = cursorFont.getLineMetrics("I", graphics.getFontRenderContext()).getAscent();
		float width = cursorFont.getItalicAngle() * ascent;
		if(width < 0)
			width = -width;
		width += 1;
		BufferedImage ret = new BufferedImage(Math.round(width), Math.round(ascent), BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D imgGraphics = (Graphics2D) ret.getGraphics();
		imgGraphics.setColor(org.muis.util.MuisUtils.getColor(cursorStyle.get(FontStyle.color), cursorStyle.get(FontStyle.transparency)));
		imgGraphics.setStroke(new java.awt.BasicStroke(cursorStyle.get(FontStyle.weight).floatValue()));
		if(cursorFont.getItalicAngle() > 0)
			imgGraphics.drawLine(0, (int) ascent, (int) width, 0);
		else
			imgGraphics.drawLine(0, 0, (int) width, (int) ascent);
		imgGraphics.dispose();
		return ret;
	}
}
