/* Created Mar 23, 2009 by Andrew */
package org.muis.browser;

import java.awt.Cursor;
import java.awt.Graphics2D;

import org.muis.core.MuisDocument;
import org.muis.core.mgr.MuisMessage;

/** A browser that renders MUIS documents */
public class MuisBrowser extends javax.swing.JPanel {
	javax.swing.JTextField theAddressBar;

	private MuisContentPane theContentPane;

	private java.awt.Component theDebugPanel;

	private boolean theDataLock;

	private org.muis.core.mgr.MuisMessageCenter.MuisMessageListener theMessageListener;

	/** Creates a MUIS browser */
	public MuisBrowser() {
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			System.err.println("Could not install system L&F");
			e.printStackTrace();
		}
		setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
		theAddressBar = new javax.swing.JTextField(250);
		add(theAddressBar);
		theAddressBar.setMaximumSize(theAddressBar.getPreferredSize());
		theContentPane = new MuisContentPane();
		add(theContentPane);
		theContentPane.setPreferredSize(new java.awt.Dimension(800, 600));
		theAddressBar.addKeyListener(new java.awt.event.KeyAdapter() {
			@Override
			public void keyReleased(java.awt.event.KeyEvent evt) {
				if(evt.getKeyCode() != java.awt.event.KeyEvent.VK_ENTER)
					return;
				goToAddress(theAddressBar.getText());
			}
		});
		theMessageListener = msg -> {
			printMessage(msg);
		};
	}

	/** @param debug The component to draw debug graphics on */
	public void setDebugPanel(java.awt.Component debug) {
		theDebugPanel = debug;
	}

	/** @param address The address of the document to get */
	public void goToAddress(String address) {
		if(!theDataLock && !theAddressBar.getText().equals(address)) {
			theDataLock = true;
			try {
				theAddressBar.setText(address);
			} finally {
				theDataLock = false;
			}
		}
		java.net.URL url;
		try {
			url = new java.net.URL(address);
		} catch(java.net.MalformedURLException e) {
			throw new IllegalArgumentException(address + " is not a valid URL", e);
		}
		org.muis.core.MuisEnvironment env = new org.muis.core.MuisEnvironment();
		env.setParser(new org.muis.core.parser.MuisDomParser(env));
		env.setContentCreator(new org.muis.core.parser.MuisContentCreator());
		MuisDocument muisDoc;
		try {
			org.muis.core.parser.MuisDocumentStructure docStruct = env.getParser().parseDocument(env, url,
				new java.io.InputStreamReader(url.openStream()));
			muisDoc = new MuisDocument(env, env.getParser(), docStruct.getLocation(), docStruct.getHead());
			muisDoc.setGraphics(new MuisDocument.GraphicsGetter() {
				@Override
				public Graphics2D getGraphics() {
					return (Graphics2D) theContentPane.getGraphics();
				}

				@Override
				public void setCursor(Cursor cursor) {
					theContentPane.setCursor(cursor);
				}
			});
			if(theDebugPanel != null)
				muisDoc.setDebugGraphics(new MuisDocument.GraphicsGetter() {
					@Override
					public Graphics2D getGraphics() {
						return theDebugPanel == null ? null : (Graphics2D) theDebugPanel.getGraphics();
					}

					@Override
					public void setCursor(Cursor cursor) {
						if(theDebugPanel == null)
							theDebugPanel.setCursor(cursor);
					}
				});
			env.getContentCreator().fillDocument(muisDoc, docStruct.getContent());
		} catch(java.io.IOException e) {
			throw new IllegalArgumentException("Could not access address " + address, e);
		} catch(org.muis.core.parser.MuisParseException e) {
			throw new IllegalArgumentException("Could not parse XML document at " + address, e);
		}

		try {
			muisDoc.postCreate();
			theContentPane.setContent(muisDoc);
			muisDoc.getRoot().bounds().act(event -> {
				System.out.println("Bounds " + event.getValue());
			});
			java.awt.Window window = getWindow();
			if(window instanceof java.awt.Frame)
				((java.awt.Frame) window).setTitle(muisDoc.getHead().getTitle());
			repaint();
		} catch(RuntimeException e) {
			e.printStackTrace();
		}
		muisDoc.msg().addListener(theMessageListener);
		for(MuisMessage msg : env.msg())
			printMessage(msg);
		for(MuisMessage msg : muisDoc.msg().allMessages())
			printMessage(msg);
	}

	private void printMessage(MuisMessage msg) {
		switch (msg.type) {
		case FATAL:
		case ERROR:
		case WARNING:
			System.err.println((msg.element == null ? "" : msg.element.toString() + "\n\t") + msg.type + ": " + msg.text);
			if(msg.exception != null)
				msg.exception.printStackTrace();
			break;
		case INFO:
			System.out.println((msg.element == null ? "" : msg.element.toString() + "\n\t") + msg.type + ": " + msg.text);
			if(msg.exception != null)
				msg.exception.printStackTrace(System.out);
			break;
		}
	}

	/** @return The content pane that renders content pointed to from this browser */
	public MuisContentPane getContentPane() {
		return theContentPane;
	}

	private java.awt.Window getWindow() {
		java.awt.Component parent = getParent();
		while(parent != null && !(parent instanceof java.awt.Window))
			parent = parent.getParent();
		return (java.awt.Window) parent;
	}

	private static final boolean DEBUG = false;

	/**
	 * Starts up a MuisBrowser
	 *
	 * @param args Command-line arguments. If any are supplied, the first one is used as the initial address of the MUIS document to
	 *            display.
	 */
	public static void main(String [] args) {
		MuisBrowser browser = new MuisBrowser();
		final javax.swing.JFrame frame = new javax.swing.JFrame("MUIS Browser");
		frame.setContentPane(browser);
		frame.setBounds(0, 0, 900, 600);
		frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		if(DEBUG) {
			final javax.swing.JFrame debugFrame = new javax.swing.JFrame("MUIS Graphics Debug");
			javax.swing.JPanel debugContent = new javax.swing.JPanel();
			debugFrame.setContentPane(debugContent);
			browser.setDebugPanel(debugContent);
			debugFrame.setSize(frame.getSize());
			debugFrame.setDefaultCloseOperation(javax.swing.JFrame.DO_NOTHING_ON_CLOSE);
			debugFrame.setVisible(true);
			frame.addComponentListener(new java.awt.event.ComponentAdapter() {
				@Override
				public void componentResized(java.awt.event.ComponentEvent e) {
					debugFrame.setSize(frame.getSize());
				}
			});
		}

		if(args.length > 0)
			browser.goToAddress(args[0]);
	}
}
