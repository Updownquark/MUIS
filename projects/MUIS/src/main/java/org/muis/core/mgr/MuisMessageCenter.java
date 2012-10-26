package org.muis.core.mgr;

import java.util.Iterator;

import org.muis.core.MuisDocument;
import org.muis.core.MuisElement;
import org.muis.core.MuisEnvironment;
import org.muis.core.mgr.MuisMessage.Type;

/** Defines a center that can store MUIS messages */
public class MuisMessageCenter implements Iterable<MuisMessage> {
	private java.util.concurrent.ConcurrentLinkedQueue<MuisMessage> theMessages;

	private MuisMessage.Type theWorstMessageType;

	private MuisEnvironment theEnvironment;

	private MuisDocument theDocument;

	private MuisElement theElement;

	/**
	 * Creates a message center
	 *
	 * @param env The environment that this message center is for
	 * @param doc The document that this message center is for (may be null for the message center is environment-level)
	 * @param element The element that this message center is for (may be null if this message center is document- or environment-level)
	 */
	public MuisMessageCenter(MuisEnvironment env, MuisDocument doc, MuisElement element) {
		theMessages = new java.util.concurrent.ConcurrentLinkedQueue<>();
		theEnvironment = env;
		theDocument = doc;
		theElement = element;
	}

	/** @return This message center's environment */
	public org.muis.core.MuisEnvironment getEnvironment() {
		if(theElement != null)
			return theElement.getDocument().getEnvironment();
		else if(theDocument != null)
			return theDocument.getEnvironment();
		else
			return theEnvironment;
	}

	/** @return This message center's document (will be null if this message center is environment-level) */
	public MuisDocument getDocument() {
		if(theElement != null)
			return theElement.getDocument();
		else
		return theDocument;
	}

	/** @return This message center's element (will be null if this message center is document- or environment-level) */
	public MuisElement getElement() {
		return theElement;
	}

	/**
	 * Records a message in this message center
	 *
	 * @param type The type of the message
	 * @param text The text of the message
	 * @param exception The exception which may have caused the message
	 * @param params Any parameters relevant to the message
	 */
	public void message(Type type, String text, Throwable exception, Object... params) {
		MuisMessage message;
		if(theElement != null)
			message = new MuisMessage(theElement, type, theElement.life().getStage(), text, exception, params);
		else if(theDocument != null)
			message = new MuisMessage(theDocument, type, theDocument.getRoot().life().getStage(), text, exception, params);
		else
			message = new MuisMessage(theEnvironment, type, org.muis.core.MuisConstants.CoreStage.READY.name(), text, exception, params);
		theMessages.add(message);
		if(theWorstMessageType == null || type.compareTo(theWorstMessageType) > 0)
			theWorstMessageType = type;
		if(theElement != null)
			theElement.fireEvent(new org.muis.core.event.MuisEvent<MuisMessage>(org.muis.core.MuisConstants.Events.MESSAGE_ADDED, message),
				false, true);
	}

	/**
	 * Records an fatal error in this message center. A fatal error disables this item so that it will not function. Short-hand for
	 * {@link #message(MuisMessage.Type, String, Throwable, Object...)}
	 *
	 * @param text A description of the error
	 * @param exception The exception that may have caused the error
	 * @param params Any parameters that may be relevant to the error
	 */
	public void fatal(String text, Throwable exception, Object... params) {
		message(MuisMessage.Type.FATAL, text, exception, params);
	}

	/**
	 * Records an fatal error in this message center. A fatal error disables this item so that it will not function. Short-hand for
	 * {@link #message(MuisMessage.Type, String, Throwable, Object...)}
	 *
	 * @param text A description of the error
	 * @param params Any parameters that may be relevant to the error
	 */
	public void fatal(String text, Object... params) {
		message(MuisMessage.Type.FATAL, text, null, params);
	}

	/**
	 * Records an error in this message center. Short-hand for {@link #message(MuisMessage.Type, String, Throwable, Object...)}
	 *
	 * @param text A description of the error
	 * @param exception The exception that may have caused the error
	 * @param params Any parameters that may be relevant to the error
	 */
	public void error(String text, Throwable exception, Object... params) {
		message(MuisMessage.Type.ERROR, text, exception, params);
	}

	/**
	 * Records an error in this message center. Short-hand for {@link #message(MuisMessage.Type, String, Throwable, Object...)}
	 *
	 * @param text A description of the error
	 * @param params Any parameters that may be relevant to the error
	 */
	public void error(String text, Object... params) {
		message(MuisMessage.Type.ERROR, text, null, params);
	}

	/**
	 * Records a warning in this message center. Short-hand for {@link #message(MuisMessage.Type, String, Throwable, Object...)}
	 *
	 * @param text A description of the warning
	 * @param params Any parameters that may be relevant to the warning
	 */
	public void warn(String text, Object... params) {
		message(MuisMessage.Type.WARNING, text, null, params);
	}

	/**
	 * Records a warning in this message center. Short-hand for {@link #message(MuisMessage.Type, String, Throwable, Object...)}
	 *
	 * @param text A description of the warning
	 * @param exception The exception that may have caused the warning
	 * @param params Any parameters that may be relevant to the warning
	 */
	public void warn(String text, Throwable exception, Object... params) {
		message(MuisMessage.Type.WARNING, text, exception, params);
	}

	/**
	 * Records a warning in this message center. Short-hand for {@link #message(MuisMessage.Type, String, Throwable, Object...)}
	 *
	 * @param text A description of the warning
	 * @param params Any parameters that may be relevant to the warning
	 */
	public void info(String text, Object... params) {
		message(MuisMessage.Type.INFO, text, null, params);
	}

	/** @return The worst type of message associated with this message center */
	public Type getWorstMessageType() {
		return theWorstMessageType;
	}

	/** @return The number of messages in this message center */
	public int getMessageCount() {
		return theMessages.size();
	}

	private void reEvalWorstMessage() {
		MuisMessage.Type type = null;
		for(MuisMessage message : theMessages)
			if(type == null || message.type.compareTo(type) > 0) {
				type = message.type;
				if(theWorstMessageType == type)
					break; // worst message type hasn't changed
			}
		if(theWorstMessageType == null ? type != null : theWorstMessageType != type)
			theWorstMessageType = type;
	}

	@Override
	public Iterator<MuisMessage> iterator() {
		return new Iterator<MuisMessage>() {
			private Iterator<MuisMessage> theWrapped = theMessages.iterator();

			private MuisMessage theLastMessage;

			@Override
			public boolean hasNext() {
				return theWrapped.hasNext();
			}

			@Override
			public MuisMessage next() {
				MuisMessage ret = theWrapped.next();
				theLastMessage = ret;
				return ret;
			}

			@Override
			public void remove() {
				theWrapped.remove();
				if(theLastMessage.type == theWorstMessageType)
					reEvalWorstMessage();
				if(theElement != null)
					theElement.fireEvent(new org.muis.core.event.MuisEvent<MuisMessage>(org.muis.core.MuisConstants.Events.MESSAGE_REMOVED,
						theLastMessage), false, true);
			}
		};
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append('{');
		for(MuisMessage message : theMessages) {
			if(ret.length() > 1)
				ret.append(',').append('\n');
			ret.append(message);
		}
		if(ret.length() > 1)
			ret.append('\n');
		ret.append('}');
		return ret.toString();
	}
}
