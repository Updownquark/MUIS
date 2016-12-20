/* Created Mar 23, 2009 by Andrew */
package org.quick.browser;

import java.awt.Cursor;
import java.awt.Graphics2D;

import org.quick.core.QuickDocument;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickHeadSection;
import org.quick.core.mgr.QuickMessage;
import org.quick.core.parser.SimpleParseEnv;

/** A browser that renders Quick documents */
public class QuickBrowser extends javax.swing.JPanel {
	private static final boolean RENDER_DEBUG = false;

	javax.swing.JTextField theAddressBar;

	private QuickContentPane theContentPane;

	private java.awt.Component theDebugPanel;

	private boolean theDataLock;

	private org.quick.core.mgr.QuickMessageCenter.QuickMessageListener theMessageListener;

	/** Creates a Quick browser */
	public QuickBrowser() {
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.err.println("Could not install system L&F");
			e.printStackTrace();
		}
		setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
		theAddressBar = new javax.swing.JTextField(250);
		add(theAddressBar);
		theAddressBar.setMaximumSize(theAddressBar.getPreferredSize());
		theContentPane = new QuickContentPane();
		add(theContentPane);
		theContentPane.setPreferredSize(new java.awt.Dimension(800, 600));
		theAddressBar.addKeyListener(new java.awt.event.KeyAdapter() {
			@Override
			public void keyReleased(java.awt.event.KeyEvent evt) {
				if (evt.getKeyCode() != java.awt.event.KeyEvent.VK_ENTER)
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
		if (!theDataLock && !theAddressBar.getText().equals(address)) {
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
		} catch (java.net.MalformedURLException e) {
			throw new IllegalArgumentException(address + " is not a valid URL", e);
		}
		org.quick.core.QuickEnvironment env = QuickEnvironment.build().withDefaults().build();
		QuickDocument quickDoc;
		try {
			org.quick.core.parser.QuickDocumentStructure docStruct = env.getDocumentParser().parseDocument(url,
				new java.io.InputStreamReader(url.openStream()));
			QuickHeadSection head = env.getContentCreator().createHeadFromStructure(docStruct.getHead(), env.getPropertyParser(),
				new SimpleParseEnv(docStruct.getHead().getClassView(), env.msg(), env.getContext()));
			quickDoc = new QuickDocument(env, docStruct.getLocation(), head, docStruct.getContent().getClassView());
			if (theDebugPanel != null)
				quickDoc.setDebugGraphics(new QuickDocument.GraphicsGetter() {
					@Override
					public Graphics2D getGraphics() {
						return theDebugPanel == null ? null : (Graphics2D) theDebugPanel.getGraphics();
					}

					@Override
					public void updated() {
						theDebugPanel.repaint();
					}

					@Override
					public void setCursor(Cursor cursor) {
						if (theDebugPanel == null)
							theDebugPanel.setCursor(cursor);
					}
				});
			env.getContentCreator().fillDocument(quickDoc, docStruct.getContent());
		} catch (java.io.IOException e) {
			throw new IllegalArgumentException("Could not access address " + address, e);
		} catch (org.quick.core.parser.QuickParseException e) {
			throw new IllegalArgumentException("Could not parse XML document at " + address, e);
		}

		try {
			quickDoc.postCreate();
			theContentPane.setContent(quickDoc);
			quickDoc.getRoot().bounds().act(event -> {
				System.out.println("Bounds " + event.getValue());
			});
			java.awt.Window window = getWindow();
			if (window instanceof java.awt.Frame)
				((java.awt.Frame) window).setTitle(quickDoc.getHead().getTitle());
			repaint();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		quickDoc.msg().addListener(theMessageListener);
		for (QuickMessage msg : env.msg())
			printMessage(msg);
		for (QuickMessage msg : quickDoc.msg().allMessages())
			printMessage(msg);
	}

	private void printMessage(QuickMessage msg) {
		switch (msg.type) {
		case FATAL:
		case ERROR:
		case WARNING:
			System.err.println((msg.element == null ? "" : msg.element.toString() + "\n\t") + msg.type + ": " + msg.text);
			if (msg.exception != null)
				msg.exception.printStackTrace();
			break;
		default:
			System.out.println((msg.element == null ? "" : msg.element.toString() + "\n\t") + msg.type + ": " + msg.text);
			if (msg.exception != null)
				msg.exception.printStackTrace(System.out);
			break;
		}
	}

	/** @return The content pane that renders content pointed to from this browser */
	public QuickContentPane getContentPane() {
		return theContentPane;
	}

	private java.awt.Window getWindow() {
		java.awt.Component parent = getParent();
		while (parent != null && !(parent instanceof java.awt.Window))
			parent = parent.getParent();
		return (java.awt.Window) parent;
	}

	/**
	 * Starts up a QuickBrowser
	 *
	 * @param args Command-line arguments. If any are supplied, the first one is used as the initial address of the Quick document to
	 *        display.
	 */
	public static void main(String[] args) {
		QuickBrowser browser = new QuickBrowser();
		final javax.swing.JFrame frame = new javax.swing.JFrame("Quick Browser");
		frame.setContentPane(browser);
		frame.setBounds(0, 0, 900, 600);
		frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		if (RENDER_DEBUG) {
			final javax.swing.JFrame debugFrame = new javax.swing.JFrame("Quick Graphics Debug");
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

		if (args.length > 0)
			browser.goToAddress(args[0]);
	}
}
