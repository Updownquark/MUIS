package org.quick.core.mgr;

import java.util.ArrayList;
import java.util.Iterator;

import org.quick.core.QuickDocument;
import org.quick.core.QuickElement;
import org.quick.core.QuickEnvironment;
import org.quick.core.event.QuickEvent;
import org.quick.core.mgr.QuickMessage.Type;

import prisms.util.ArrayUtils;

/** Defines a center that can store MUIS messages */
public class QuickMessageCenter implements Iterable<QuickMessage> {
	/** A message fired in an element when a message is added to or removed from it */
	public static class QuickMessageEvent implements org.quick.core.event.QuickEvent {
		private final QuickElement theElement;
		private final QuickMessage theMessage;
		private final boolean isRemove;
		private final QuickEvent theCause;

		QuickMessageEvent(QuickElement element, QuickMessage message, boolean remove, QuickEvent cause) {
			theElement = element;
			theMessage = message;
			isRemove = remove;
			theCause = cause;
		}

		@Override
		public QuickElement getElement() {
			return theElement;
		}

		@Override
		public QuickEvent getCause() {
			return theCause;
		}

		/** @return The message that was added or removed */
		public QuickMessage getMessage() {
			return theMessage;
		}

		/** @return Whether the message was added or removed */
		public boolean isRemove() {
			return isRemove;
		}

		@Override
		public boolean isOverridden() {
			return false;
		}
	}

	/** Receives notifications new messages to a message center or one of its children */
	public static interface QuickMessageListener {
		/** @param msg The message received */
		void messageReceived(QuickMessage msg);
	}

	private final QuickEnvironment theEnvironment;

	private final QuickDocument theDocument;

	private final QuickElement theElement;

	private final java.util.concurrent.ConcurrentLinkedQueue<QuickMessage> theMessages;
	private java.util.concurrent.CopyOnWriteArrayList<QuickMessageListener> theListeners;

	private QuickMessage.Type theWorstMessageType;

	/**
	 * Creates a message center
	 *
	 * @param env The environment that this message center is for
	 * @param doc The document that this message center is for (may be null for the message center is environment-level)
	 * @param element The element that this message center is for (may be null if this message center is document- or environment-level)
	 */
	public QuickMessageCenter(QuickEnvironment env, QuickDocument doc, QuickElement element) {
		theEnvironment = env;
		theDocument = doc;
		theElement = element;
		theMessages = new java.util.concurrent.ConcurrentLinkedQueue<>();
		theListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
	}

	/** @return This message center's environment */
	public org.quick.core.QuickEnvironment getEnvironment() {
		if(theElement != null)
			return theElement.getDocument().getEnvironment();
		else if(theDocument != null)
			return theDocument.getEnvironment();
		else
			return theEnvironment;
	}

	/** @return This message center's document (will be null if this message center is environment-level) */
	public QuickDocument getDocument() {
		if(theElement != null)
			return theElement.getDocument();
		else
			return theDocument;
	}

	/** @return This message center's element (will be null if this message center is document- or environment-level) */
	public QuickElement getElement() {
		return theElement;
	}

	/**
	 * Records a message in this message center
	 *
	 * @param type The type of the message
	 * @param text The text of the message
	 * @param cause The cause of the message
	 * @param exception The exception which may have caused the message
	 * @param params Any parameters relevant to the message
	 */
	public void message(Type type, String text, QuickEvent cause, Throwable exception, Object... params) {
		QuickMessage message;
		if(theElement != null)
			message = new QuickMessage(theElement, type, theElement.life().getStage(), text, exception, params);
		else if(theDocument != null)
			message = new QuickMessage(theDocument, type, theDocument.getRoot().life().getStage(), text, exception, params);
		else
			message = new QuickMessage(theEnvironment, type, org.quick.core.QuickConstants.CoreStage.READY.name(), text, exception, params);
		message(message, cause);
	}

	private void message(QuickMessage message, QuickEvent cause) {
		theMessages.add(message);
		if(theWorstMessageType == null || message.type.compareTo(theWorstMessageType) > 0)
			theWorstMessageType = message.type;
		fireListeners(message, cause);

		if(theElement != null) {
			if(theElement.getParent() != null)
				theElement.getParent().msg().message(message, cause);
			else
				theElement.getDocument().msg().message(message, cause);
		} else if(theDocument != null)
			theEnvironment.msg().message(message, cause);
	}

	/**
	 * Records an fatal error in this message center. A fatal error disables this item so that it will not function. Short-hand for
	 * {@link #message(QuickMessage.Type, String, QuickEvent, Throwable, Object...)}
	 *
	 * @param text A description of the error
	 * @param exception The exception that may have caused the error
	 * @param params Any parameters that may be relevant to the error
	 */
	public void fatal(String text, Throwable exception, Object... params) {
		message(QuickMessage.Type.FATAL, text, null, exception, params);
	}

	/**
	 * Records an fatal error in this message center. A fatal error disables this item so that it will not function. Short-hand for
	 * {@link #message(QuickMessage.Type, String, QuickEvent, Throwable, Object...)}
	 *
	 * @param text A description of the error
	 * @param cause The cause of the message
	 * @param exception The exception that may have caused the error
	 * @param params Any parameters that may be relevant to the error
	 */
	public void fatal(String text, QuickEvent cause, Throwable exception, Object... params) {
		message(QuickMessage.Type.FATAL, text, cause, exception, params);
	}

	/**
	 * Records an fatal error in this message center. A fatal error disables this item so that it will not function. Short-hand for
	 * {@link #message(QuickMessage.Type, String, QuickEvent, Throwable, Object...)}
	 *
	 * @param text A description of the error
	 * @param params Any parameters that may be relevant to the error
	 */
	public void fatal(String text, Object... params) {
		message(QuickMessage.Type.FATAL, text, null, null, params);
	}

	/**
	 * Records an error in this message center. Short-hand for {@link #message(QuickMessage.Type, String, QuickEvent, Throwable, Object...)}
	 *
	 * @param text A description of the error
	 * @param exception The exception that may have caused the error
	 * @param params Any parameters that may be relevant to the error
	 */
	public void error(String text, Throwable exception, Object... params) {
		message(QuickMessage.Type.ERROR, text, null, exception, params);
	}

	/**
	 * Records an error in this message center. Short-hand for {@link #message(QuickMessage.Type, String, QuickEvent, Throwable, Object...)}
	 *
	 * @param text A description of the error
	 * @param cause The cause of the message
	 * @param exception The exception that may have caused the error
	 * @param params Any parameters that may be relevant to the error
	 */
	public void error(String text, QuickEvent cause, Throwable exception, Object... params) {
		message(QuickMessage.Type.ERROR, text, cause, exception, params);
	}

	/**
	 * Records an error in this message center. Short-hand for {@link #message(QuickMessage.Type, String, QuickEvent, Throwable, Object...)}
	 *
	 * @param text A description of the error
	 * @param params Any parameters that may be relevant to the error
	 */
	public void error(String text, Object... params) {
		message(QuickMessage.Type.ERROR, text, null, null, params);
	}

	/**
	 * Records a warning in this message center. Short-hand for {@link #message(QuickMessage.Type, String, QuickEvent, Throwable, Object...)}
	 *
	 * @param text A description of the warning
	 * @param cause The cause of the message
	 * @param exception The exception that may have caused the warning
	 * @param params Any parameters that may be relevant to the warning
	 */
	public void warn(String text, QuickEvent cause, Throwable exception, Object... params) {
		message(QuickMessage.Type.WARNING, text, cause, exception, params);
	}

	/**
	 * Records a warning in this message center. Short-hand for {@link #message(QuickMessage.Type, String, QuickEvent, Throwable, Object...)}
	 *
	 * @param text A description of the warning
	 * @param exception The exception that may have caused the warning
	 * @param params Any parameters that may be relevant to the warning
	 */
	public void warn(String text, Throwable exception, Object... params) {
		message(QuickMessage.Type.WARNING, text, null, exception, params);
	}

	/**
	 * Records a warning in this message center. Short-hand for {@link #message(QuickMessage.Type, String, QuickEvent, Throwable, Object...)}
	 *
	 * @param text A description of the warning
	 * @param params Any parameters that may be relevant to the warning
	 */
	public void warn(String text, Object... params) {
		message(QuickMessage.Type.WARNING, text, null, null, params);
	}

	/**
	 * Records an information message in this message center. Short-hand for
	 * {@link #message(QuickMessage.Type, String, QuickEvent, Throwable, Object...)}
	 *
	 * @param text A description of the message
	 * @param params Any parameters that may be relevant to the message
	 */
	public void info(String text, Object... params) {
		message(QuickMessage.Type.INFO, text, null, null, params);
	}

	/**
	 * Records a debugging message in this message center. Short-hand for
	 * {@link #message(QuickMessage.Type, String, QuickEvent, Throwable, Object...)}
	 *
	 * @param text A description of the message
	 * @param params Any parameters that may be relevant to the message
	 */
	public void debug(String text, Object... params) {
		message(QuickMessage.Type.DEBUG, text, null, null, params);
	}

	/** @return The worst type of message associated with this message center */
	public Type getWorstMessageType() {
		return theWorstMessageType;
	}

	/** @return The number of messages in this message center */
	public int getMessageCount() {
		return theMessages.size();
	}

	/** @return The worst type of messages in this element and its children */
	public QuickMessage.Type getDeepWorstMessageType() {
		QuickMessage.Type ret = getWorstMessageType();
		if(theElement == null)
			return ret;
		for(QuickElement child : theElement.getChildren()) {
			QuickMessage.Type childType = child.msg().getWorstMessageType();
			if(ret == null || ret.compareTo(childType) < 0)
				ret = childType;
		}
		return ret;
	}

	/** @return All messages in this element or any of its children */
	public Iterable<QuickMessage> allMessages() {
		if(theElement == null)
			return this;
		ArrayList<Iterable<QuickMessage>> centers = new ArrayList<>();
		centers.add(this);
		for(QuickElement child : theElement.getChildren())
			centers.add(child.msg().allMessages());
		return ArrayUtils.iterable((Iterable<QuickMessage> []) centers.toArray(new Iterable[centers.size()]));
	}

	/** @param listener The listener to be notified when a new message is received by this message center or one of its children */
	public void addListener(QuickMessageListener listener) {
		theListeners.add(listener);
	}

	/** @param listener The listener to stop receiving message notifications */
	public void removeListener(QuickMessageListener listener) {
		theListeners.remove(listener);
	}

	private void fireListeners(QuickMessage msg, QuickEvent cause) {
		for(QuickMessageListener listener : theListeners)
			listener.messageReceived(msg);
		if(theElement != null)
			theElement.events().fire(new QuickMessageEvent(theElement, msg, false, cause));
	}

	private void reEvalWorstMessage() {
		QuickMessage.Type type = null;
		for(QuickMessage message : theMessages)
			if(type == null || message.type.compareTo(type) > 0) {
				type = message.type;
				if(theWorstMessageType == type)
					break; // worst message type hasn't changed
			}
		if(theWorstMessageType == null ? type != null : theWorstMessageType != type)
			theWorstMessageType = type;
	}

	@Override
	public Iterator<QuickMessage> iterator() {
		return new Iterator<QuickMessage>() {
			private Iterator<QuickMessage> theWrapped = theMessages.iterator();

			private QuickMessage theLastMessage;

			@Override
			public boolean hasNext() {
				return theWrapped.hasNext();
			}

			@Override
			public QuickMessage next() {
				QuickMessage ret = theWrapped.next();
				theLastMessage = ret;
				return ret;
			}

			@Override
			public void remove() {
				theWrapped.remove();
				if(theLastMessage.type == theWorstMessageType)
					reEvalWorstMessage();
				if(theElement != null)
					theElement.events().fire(new QuickMessageEvent(theElement, theLastMessage, true, null));
			}
		};
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append('{');
		for(QuickMessage message : theMessages) {
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
