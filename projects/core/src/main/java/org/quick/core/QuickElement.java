/* Created Feb 23, 2009 by Andrew Butler */
package org.quick.core;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import org.observe.ObservableValue;
import org.observe.SettableValue;
import org.observe.collect.ObservableCollection;
import org.observe.util.TypeTokens;
import org.qommons.Transactable;
import org.qommons.Transaction;
import org.qommons.collect.CollectionLockingStrategy;
import org.qommons.collect.StampedLockingStrategy;
import org.qommons.tree.BetterTreeList;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.QuickConstants.States;
import org.quick.core.mgr.AttributeManager2;
import org.quick.core.mgr.HierarchicalResourcePool;
import org.quick.core.mgr.QuickLifeCycleManager;
import org.quick.core.mgr.QuickLifeCycleManager.Controller;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.mgr.QuickState;
import org.quick.core.mgr.StateEngine;
import org.quick.core.model.QuickAppModel;
import org.quick.core.prop.DefaultExpressionContext;
import org.quick.core.prop.ExpressionContext;
import org.quick.core.style.QuickElementStyle;
import org.quick.core.style.StyleAttributes;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.QuickTagUtils;
import org.quick.core.tags.State;

/** The base display element in Quick. Contains base methods to administer content (children, style, placement, etc.) */
@QuickElementType(
	attributes = { @AcceptAttribute(declaringClass = StyleAttributes.class, field = "style"),
		@AcceptAttribute(declaringClass = StyleAttributes.class, field = "group") },
	states={@State(name = States.CLICK_NAME, priority = States.CLICK_PRIORITY),
		@State(name = States.RIGHT_CLICK_NAME, priority = States.RIGHT_CLICK_PRIORITY),
		@State(name = States.MIDDLE_CLICK_NAME, priority = States.MIDDLE_CLICK_PRIORITY),
		@State(name = States.HOVER_NAME, priority = States.HOVER_PRIORITY),
		@State(name = States.FOCUS_NAME, priority = States.FOCUS_PRIORITY),
		@State(name = States.TEXT_SELECTION_NAME, priority = States.TEXT_SELECTION_PRIORITY)})
public abstract class QuickElement implements QuickParseEnv {
	private final QuickLifeCycleManager theLifeCycleManager;

	private QuickLifeCycleManager.Controller theLifeCycleController;

	private final ReentrantReadWriteLock theAttributeLocker;

	private final CollectionLockingStrategy theContentLocker;

	private final StateEngine theStateEngine;

	private final QuickMessageCenter theMessageCenter;

	private final HierarchicalResourcePool theResourcePool;

	private QuickDocument theDocument;

	private QuickToolkit theToolkit;

	private final SettableValue<QuickElement> theParent;

	private QuickClassView theClassView;

	private ExpressionContext theContext;

	private QuickAppModel theSelfModel;

	private String theNamespace;

	private String theTagName;

	private final AttributeManager2 theAttributeManager;

	private final ObservableCollection<QuickElement> theChildren;

	private final ObservableCollection<QuickElement> theExposedChildren;

	private final QuickElementStyle theStyle;

	private boolean isFocusable;

	/** Creates a Quick element */
	public QuickElement() {
		theAttributeLocker = new ReentrantReadWriteLock();
		theContentLocker = new StampedLockingStrategy();
		theResourcePool = new HierarchicalResourcePool(this, null, Transactable.transactable(theAttributeLocker), true);
		theParent = new org.observe.SimpleSettableValue<>(TypeTokens.get().of(QuickElement.class), true, theAttributeLocker, null)//
			.filterAccept(el -> el == QuickElement.this ? "An element cannot have itself as a parent" : null);
		theParent.changes().act(evt -> {
			if (evt.getOldValue() != null)
				evt.getOldValue().theChildren.remove(QuickElement.this);
			theResourcePool.setParent(evt.getNewValue() == null ? null : evt.getNewValue().getResourcePool());
		});
		theMessageCenter = new QuickMessageCenter(null, null, this);
		theLifeCycleManager = new QuickLifeCycleManager(this, (Controller controller) -> {
			theLifeCycleController = controller;
		}, CoreStage.READY.toString());
		theStateEngine = new StateEngine(this);
		String lastStage = null;
		for(CoreStage stage : CoreStage.values())
			if(stage != CoreStage.OTHER && stage != CoreStage.READY) {
				theLifeCycleManager.addStage(stage.toString(), lastStage);
				lastStage = stage.toString();
			}
		theChildren = ObservableCollection.create(TypeTokens.get().of(QuickElement.class), new BetterTreeList<>(theContentLocker));
		theExposedChildren = theChildren.flow().unmodifiable(false).collect();
		theAttributeManager = new AttributeManager2(this, theAttributeLocker);
		theStyle = new QuickElementStyle(this);
		theSelfModel = QuickAppModel.empty("this");
		List<Runnable> childRemoves = new ArrayList<>();
		theChildren.subscribe(evt -> {
			switch (evt.getType()) {
			case add:
				childRemoves.add(evt.getIndex(), registerChild(//
					evt.getNewValue()));
				break;
			case remove:
				Runnable remove = childRemoves.remove(evt.getIndex());
				remove.run();
				break;
			case set:
				if (evt.getOldValue() != evt.getNewValue()) {
					remove = childRemoves.get(evt.getIndex());
					remove.run();
					childRemoves.set(evt.getIndex(), registerChild(//
						evt.getNewValue()));
				}
				break;
			}
		}, true);
		addAnnotatedStates();
		theLifeCycleController.advance(CoreStage.PARSE_SELF.toString());
	}

	private void addAnnotatedStates() {
		for (QuickState state : QuickTagUtils.getStatesFor(getClass())) {
			try {
				theStateEngine.addState(state);
			} catch(IllegalArgumentException e) {
				msg().warn(e.getMessage(), "state", state);
			}
		}
	}

	/** @return A locker controlling threaded access to this element's single-valued attributes */
	public ReentrantReadWriteLock getAttributeLocker() {
		return theAttributeLocker;
	}

	/** @return A locker controlling threaded access to this element's multi-valued attributes (e.g. children) */
	public CollectionLockingStrategy getContentLocker() {
		return theContentLocker;
	}

	/** @return This element's resource pool */
	public HierarchicalResourcePool getResourcePool() {
		return theResourcePool;
	}

	/** @return The document that this element belongs to */
	public final QuickDocument getDocument() {
		return theDocument;
	}

	/** @return The document that this element belongs to */
	public final QuickDocument doc() {
		return theDocument;
	}

	/** @return The tool kit that this element belongs to */
	public final QuickToolkit getToolkit() {
		return theToolkit;
	}

	/** @return The Quick class view that allows for instantiation of child elements */
	public final QuickClassView getClassView() {
		return theClassView;
	}

	/** @return The Quick class view that allows for instantiation of child elements */
	@Override
	public final QuickClassView cv() {
		return theClassView;
	}

	/**
	 * Returns a message center that allows messaging on this element
	 *
	 * @return This element's message center
	 */
	@Override
	public QuickMessageCenter msg() {
		return theMessageCenter;
	}

	/** @return The namespace that this tag was instantiated in */
	public final String getNamespace() {
		return theNamespace;
	}

	/** @return The name of the tag that was used to instantiate this element */
	public final String getTagName() {
		return theTagName;
	}

	/** @return The state engine that controls this element's states */
	public StateEngine getStateEngine() {
		return theStateEngine;
	}

	/**
	 * Short-hand for {@link #getStateEngine()}
	 *
	 * @return The state engine that controls this element's states
	 */
	public StateEngine state() {
		return getStateEngine();
	}

	/** @return The manager of this element's attributes */
	public AttributeManager2 getAttributeManager() {
		return theAttributeManager;
	}

	/**
	 * Short-hand for {@link #getAttributeManager()}
	 *
	 * @return The manager of this element's attributes
	 */
	public AttributeManager2 atts() {
		return getAttributeManager();
	}

	/** @return The style that modifies this element's appearance */
	public final QuickElementStyle getStyle() {
		return theStyle;
	}

	// Life cycle methods

	/**
	 * Returns a life cycle manager that allows subclasses to customize and hook into the life cycle for this element.
	 *
	 * @return The life cycle manager for this element
	 */
	public QuickLifeCycleManager getLifeCycleManager() {
		return theLifeCycleManager;
	}

	/**
	 * Short-hand for {@link #getLifeCycleManager()}
	 *
	 * @return The life cycle manager for this element
	 */
	public QuickLifeCycleManager life() {
		return getLifeCycleManager();
	}

	/**
	 * Initializes an element's core information
	 *
	 * @param doc The document that this element belongs to
	 * @param toolkit The toolkit that this element belongs to
	 * @param classView The class view for this element
	 * @param parent The parent that this element is under
	 * @param namespace The namespace used to create this element
	 * @param tagName The tag name used to create this element
	 */
	public final void init(QuickDocument doc, QuickToolkit toolkit, QuickClassView classView, QuickElement parent, String namespace,
		String tagName) {
		theLifeCycleController.advance(CoreStage.INIT_SELF.toString());
		if(doc == null)
			throw new IllegalArgumentException("Cannot create an element without a document");
		if(theDocument != null)
			throw new IllegalStateException("An element cannot be initialized twice", null);
		theDocument = doc;
		theToolkit = toolkit;
		theNamespace = namespace;
		theTagName = tagName;
		theClassView = classView;
		QuickAppModel selfModel = getSelfModel();
		if (selfModel == null)
			throw new NullPointerException("Self model is not initialized yet");
		DefaultExpressionContext.Builder ctxBuilder = DefaultExpressionContext.build()//
			.withParent(theDocument.getContext())//
			.withValue("this", ObservableValue.of(selfModel));
		theContext = ctxBuilder.build();
		setParent(parent);
		addAnnotatedAttributes();
		theLifeCycleController.advance(CoreStage.PARSE_CHILDREN.toString());
	}

	private void addAnnotatedAttributes() {
		Object wanter = new Object();
		List<QuickTagUtils.AcceptedAttributeStruct<?>> atts;
		try {
			atts = QuickTagUtils.getAcceptedAttributes(getClass());
		} catch (RuntimeException e) {
			theMessageCenter.fatal("Could not parse attributes on element type " + getClass().getSimpleName() + " from annotation", e);
			return;
		}
		for (QuickTagUtils.AcceptedAttributeStruct<?> att : atts) {
			theAttributeManager.accept(att.attribute, wanter, a -> a.required(att.annotation.required()));
			if (att.annotation.defaultValue().length() > 0)
				try {
					theAttributeManager.set(att.attribute, att.annotation.defaultValue(), this);
				} catch (QuickException e) {
					theMessageCenter.error("Could not set default value " + att.annotation.defaultValue() + " for attribute "
						+ att.attribute + " from annotation", e);
				}
		}
	}

	/**
	 * Initializes an element's descendants
	 *
	 * @param children The child elements specified in the Quick XML
	 */
	public void initChildren(List<QuickElement> children) {
		theLifeCycleController.advance(CoreStage.INIT_CHILDREN.toString());
		try (Transaction t = theChildren.lock(true, null)) {
			theChildren.clear();
			theChildren.addAll(children);
		}
		theLifeCycleController.advance(CoreStage.INITIALIZED.toString());
	}

	/**
	 * Called when a child is introduced to this parent
	 *
	 * @param child The child that has been added to this parent
	 * @return A runnable that will be executed when the element is no longer a child of this element
	 */
	protected Runnable registerChild(QuickElement child) {
		if(child.getParent() != this)
			child.setParent(this);

		// Need to catch the child up to where the parent is in the life cycle
		if (child.life().isAfter(CoreStage.INIT_SELF.name()) < 0 && life().isAfter(CoreStage.PARSE_CHILDREN.name()) > 0) {
			QuickToolkit tk;
			if (child.getClass().getClassLoader() instanceof QuickToolkit)
				tk = (QuickToolkit) child.getClass().getClassLoader();
			else
				tk = getDocument().getEnvironment().getCoreToolkit();
			QuickClassView classView = new QuickClassView(getDocument().getEnvironment(), theClassView, tk);
			child.init(getDocument(), tk, classView, this, null, null);
		}
		if (child.life().isAfter(CoreStage.INIT_CHILDREN.name()) < 0 && life().isAfter(CoreStage.INITIALIZED.name()) > 0) {
			child.initChildren(Collections.emptyList());
		}
		if (child.life().isAfter(CoreStage.STARTUP.name()) < 0 && life().isAfter(CoreStage.READY.name()) > 0) {
			child.postCreate();
		}

		return () -> {
			if (child.getParent() == this)
				child.setParent(null);
		};
	}

	/** Called to initialize an element after all the parsing and linking has been performed */
	public final void postCreate() {
		theLifeCycleController.advance(CoreStage.STARTUP.toString());
		for(QuickElement child : theChildren)
			child.postCreate();
		theLifeCycleController.advance(CoreStage.READY.toString());
	}

	// End life cycle methods

	@Override
	public ExpressionContext getContext() {
		return theContext;
	}

	/** @return The "this" model for this element */
	public QuickAppModel getSelfModel() {
		return theSelfModel;
	}

	// Hierarchy methods

	/** @return This element's parent in the DOM tree */
	public final ObservableValue<QuickElement> getParent() {
		return theParent.unsettable();
	}

	/**
	 * Sets this element's parent after initialization
	 *
	 * @param parent The new parent for this element
	 */
	protected final void setParent(QuickElement parent) {
		if (theParent.get() == parent)
			return;
		theParent.set(parent, null);
	}

	/** @return An list of the elements immediately contained by this element. By default, this list is immutable. */
	public ObservableCollection<? extends QuickElement> getPhysicalChildren() {
		return theExposedChildren;
	}

	/**
	 * Short-hand for {@link #getPhysicalChildren()}
	 *
	 * @return An unmodifiable list of the elements immediately contained by this element
	 */
	public ObservableCollection<? extends QuickElement> ch() {
		return getPhysicalChildren();
	}

	/**
	 * @return A list of the logical contents of this element. By default, this is the same as its {@link #getPhysicalChildren() physical
	 *         children}.
	 */
	public ObservableCollection<? extends QuickElement> getLogicalChildren() {
		return theExposedChildren;
	}

	/** @return An augmented, modifiable {@link List} of this element's children */
	protected ObservableCollection<QuickElement> getChildManager() {
		return theChildren;
	}

	// End hierarchy methods

	/** @return Whether this element is able to accept the focus for the document */
	public boolean isFocusable() {
		return isFocusable;
	}

	/** @param focusable Whether this element should be focusable */
	protected final void setFocusable(boolean focusable) {
		isFocusable = focusable;
	}

	/**
	 * Generates an XML-representation of this element's content
	 *
	 * @param indent The indention string to use for each level away from the margin
	 * @param deep Whether to print this element's children
	 * @return The XML string representing this element
	 */
	public final String asXML(String indent, boolean deep) {
		StringBuilder ret = new StringBuilder();
		appendXML(ret, indent, 0, deep);
		return ret.toString();
	}

	/**
	 * Appends this element's XML-representation to a string builder
	 *
	 * @param str The string builder to append to
	 * @param indent The indention string to use for each level away from the margin
	 * @param level The depth of this element in the structure being printed
	 * @param deep Whether to print this element's children
	 */
	protected final void appendXML(StringBuilder str, String indent, int level, boolean deep) {
		for(int L = 0; L < level; L++)
			str.append(indent);
		str.append('<');
		if(theNamespace != null)
			str.append(theNamespace).append(':');
		str.append(theTagName);
		if (!theAttributeManager.getAllAttributes().isEmpty())
			str.append(' ').append(theAttributeManager.toString());
		if(!deep || theChildren.isEmpty()) {
			str.append(' ').append('/').append('>');
			return;
		}
		str.append('>');
		if(deep) {
			for(QuickElement child : theChildren) {
				str.append('\n');
				child.appendXML(str, indent, level + 1, deep);
			}
			str.append('\n');
		}
		for(int L = 0; L < level; L++)
			str.append(indent);
		str.append('<').append('/');
		if(theNamespace != null)
			str.append(theNamespace).append(':');
		str.append(theTagName).append('>');
	}

	@Override
	public String toString() {
		return asXML("", false);
	}

	@Override
	public final boolean equals(Object o) {
		return super.equals(o);
	}

	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	public QuickElement copy(QuickElement parent, Consumer<QuickElement> postProcess) throws IllegalStateException {
		QuickElement copy = initCopy();
		Runnable attFinishTemp = copy.tempAcceptAttributes(this);
		if (life().isAfter(CoreStage.INIT_SELF.name()) > 1)
			copy.init(theDocument, theToolkit, theClassView, parent, theNamespace, theTagName);
		Runnable chFinishTemp = null;
		if (life().isAfter(CoreStage.INIT_CHILDREN.name()) > 1)
			chFinishTemp = copy.initCopyChildren(this);
		if (postProcess != null)
			postProcess.accept(copy);
		attFinishTemp.run();
		if (chFinishTemp != null)
			chFinishTemp.run();
		return copy;
	}

	protected QuickElement initCopy() {
		QuickElement copy;
		try {
			copy = getClass().getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
			| NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException("Unable to copy " + this, e);
		}
		return copy;
	}

	private Runnable tempAcceptAttributes(QuickElement copyFrom) {
		String duplicate = "duplicate";
		List<AttributeManager2.AttributeAcceptance> tempAcceptance = new ArrayList<>();
		for (AttributeManager2.AttributeValue<?> attr : copyFrom.atts().getAllAttributes()) {
			ObservableValue<?> container = attr.getContainer().get();
			atts().accept(attr.getAttribute(), duplicate, acc -> {
				tempAcceptance.add(acc);
				if (container != null)
					((AttributeManager2.IndividualAttributeAcceptance<Object>) acc).initContainer(container);
				else
					((AttributeManager2.IndividualAttributeAcceptance<Object>) acc).init(attr.get());
			});
		}
		return () -> {
			for (AttributeManager2.AttributeAcceptance acc : tempAcceptance)
				acc.reject();
			tempAcceptance.clear();
		};
	}

	private Runnable initCopyChildren(QuickElement copyFrom) {
		List<QuickElement> children = new ArrayList<>(copyFrom.theChildren);
		List<QuickElement> newChildren = new ArrayList<>(children.size());
		List<Runnable> finishTemp = new ArrayList<>();
		for (int i = 0; i < children.size(); i++) {
			QuickElement child = children.get(i);
			QuickElement childCopy = child.initCopy();
			finishTemp.add(childCopy.tempAcceptAttributes(child));
			newChildren.add(childCopy);
			childCopy.init(theDocument, theToolkit, child.theClassView, this, child.theNamespace, child.theTagName);
		}
		initChildren(children);
		for (int i = 0; i < children.size(); i++)
			finishTemp.add(newChildren.get(i).initCopyChildren(children.get(i)));
		return () -> {
			for (Runnable ft : finishTemp)
				ft.run();
			finishTemp.clear();
		};
	}
}
