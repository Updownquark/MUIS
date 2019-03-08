/* Created Feb 23, 2009 by Andrew Butler */
package org.quick.core;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.observe.Observable;
import org.observe.ObservableValue;
import org.qommons.Transactable;
import org.quick.core.mgr.HierarchicalResourcePool;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.prop.DefaultExpressionContext;
import org.quick.core.prop.ExpressionContext;
import org.quick.core.style.DocumentStyleSheet;

/** Contains all data pertaining to a Quick application */
public class QuickDocument implements QuickParseEnv {
	private final QuickEnvironment theEnvironment;

	private final java.net.URL theLocation;

	private final ExpressionContext theContext;

	private final HierarchicalResourcePool theResourcePool;

	private final Transactable theLock;

	private final Observable<?> theDispose;

	private QuickClassView theClassView;

	private QuickHeadSection theHead;

	private BodyElement theRoot;

	private QuickMessageCenter theMessageCenter;

	private DocumentStyleSheet theDocumentStyle;

	/**
	 * Creates a document
	 *
	 * @param env The environment for the document
	 * @param location The location of the file that this document was generated from
	 * @param head The head section for this document
	 * @param classView The class view for the document
	 * @param dispose The observable that will fire to dispose of this document
	 */
	public QuickDocument(QuickEnvironment env, java.net.URL location, QuickHeadSection head, QuickClassView classView,
		Observable<?> dispose) {
		theEnvironment = env;
		theLocation = location;
		theHead = head;
		theClassView = classView;
		theLock = Transactable.transactable(new ReentrantReadWriteLock());
		theResourcePool = new HierarchicalResourcePool(this, env.getResourcePool(), theLock, true);
		theDispose = dispose;
		theDispose.take(1).act(d -> theResourcePool.setActive(false));

		theContext = DefaultExpressionContext.build().withParent(env.getContext())//
			.withValueGetter(name -> {
				Object model= getHead().getModel(name);
				if(model instanceof ObservableValue)
					return (ObservableValue<?>) model;
				else if (model != null)
					return ObservableValue.of(model);
				else
					return null;
			})//
			.build();
		theMessageCenter = new QuickMessageCenter(env, this, null);
		theDocumentStyle = DocumentStyleSheet.build(this);
		theRoot = new BodyElement();
	}

	/** @return This document's resource pool */
	public HierarchicalResourcePool getResourcePool() {
		return theResourcePool;
	}

	/** @return An observable that will fire when this document is disposed */
	public Observable<?> getDispose() {
		return theDispose;
	}
	/** @return The environment that this document was created in */
	public QuickEnvironment getEnvironment() {
		return theEnvironment;
	}

	@Override
	public QuickClassView cv() {
		return getClassView();
	}

	@Override
	public ExpressionContext getContext() {
		return theContext;
	}

	/** @return The class map that applies to the whole document */
	public QuickClassView getClassView() {
		return theClassView;
	}

	/** @return The location of the file that this document was generated from */
	public java.net.URL getLocation() {
		return theLocation;
	}

	/** @return The head section of this document */
	public QuickHeadSection getHead() {
		return theHead;
	}

	/** @return The style sheet for this document */
	public DocumentStyleSheet getStyle() {
		return theDocumentStyle;
	}

	/** @return The root element of the document */
	public BodyElement getRoot() {
		return theRoot;
	}

	/** Called to initialize the document after all the parsing and linking has been performed */
	public void postCreate() {
		theRoot.postCreate();
	}

	/** @return This document's message center */
	public QuickMessageCenter getMessageCenter() {
		return theMessageCenter;
	}

	/**
	 * Short-hand for {@link #getMessageCenter()}
	 *
	 * @return This document's message center
	 */
	@Override
	public QuickMessageCenter msg() {
		return getMessageCenter();
	}
}
