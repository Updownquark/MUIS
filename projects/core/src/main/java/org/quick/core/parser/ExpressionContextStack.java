package org.quick.core.parser;

import java.util.*;

import org.quick.core.*;
import org.quick.core.QuickCache.CacheException;
import org.quick.core.QuickTemplate.AttachPoint;
import org.quick.core.style.StateCondition;
import org.quick.core.style.StyleCondition;

/** Used for parsing hierarchical style sheets */
public class ExpressionContextStack {
	/** A point in the expression context hierarchy */
	public class ExpressionContext {
		private final QuickClassView theClassView;

		private Class<? extends QuickElement> theType;

		final Set<String> theGroups;

		StateCondition theState;

		QuickTemplate.AttachPoint<?> theTemplateRole;

		ExpressionContext(ExpressionContext parent) {
			theClassView = new QuickClassView(theEnv, parent == null ? theEnv.cv() : parent.theClassView, theToollkit);
			theGroups = new LinkedHashSet<>();
		}

		boolean isEmpty() {
			return theType == null && theGroups.isEmpty() && theState == null && theTemplateRole == null;
		}

		/** @return The class view at the this point in the context hierarchy */
		public QuickClassView getClassView() {
			return theClassView;
		}

		@Override
		public String toString() {
			StringBuilder str = new StringBuilder();
			if (theType != null)
				str.append(theType.getSimpleName());
			if (!theGroups.isEmpty()) {
				str.append(theGroups);
			}
			if (theState != null)
				str.append('[').append(theState.toString()).append(']');
			if (theTemplateRole != null)
				str.append('(').append(theTemplateRole).append(')');
			return str.toString();
		}
	}

	private final QuickEnvironment theEnv;
	private final QuickToolkit theToollkit;

	private final LinkedList<ExpressionContext> theStack;

	/**
	 * @param env The environment to parse in
	 * @param toolkit The toolkit to parse for
	 */
	public ExpressionContextStack(QuickEnvironment env, QuickToolkit toolkit) {
		theEnv = env;
		theToollkit = toolkit;
		theStack = new LinkedList<>();
	}

	/** @return The depth of the stack */
	public int size() {
		return theStack.size();
	}

	/** Creates a new context at the top of the stack */
	public void push() {
		ExpressionContext top = theStack.isEmpty() ? null : theStack.getLast();
		theStack.add(new ExpressionContext(top));
	}

	/** Removes the context at the top of the stack */
	public void pop() {
		theStack.removeLast();
	}

	/** @return The context at the top of the stack */
	public ExpressionContext top() {
		return theStack.getLast();
	}

	/**
	 * Sets the type for the top of the stack
	 *
	 * @param type The type to set
	 * @throws QuickParseException If the given type is not valid for this point in the context stack
	 */
	public void setType(Class<? extends QuickElement> type) throws QuickParseException {
		Class<? extends QuickElement> topType = getType();
		if (!topType.isAssignableFrom(topType))
			throw new QuickParseException("Type " + type.getName() + " is not a sub type of a " + type.getName());
		if (topType != type)
			top().theType = type;
	}

	/**
	 * Adds a group to the top of the stack
	 *
	 * @param groupName The name of the group to add
	 */
	public void addGroup(String groupName) {
		top().theGroups.add(groupName);
	}

	/**
	 * Sets the state condition for the top of the stack
	 *
	 * @param state The state condition
	 */
	public void setState(StateCondition state) {
		top().theState = state;
	}

	/**
	 * Sets the template role (attach point) for the top of the stack
	 *
	 * @param attachPoint The attach point or the top of the stack
	 * @throws QuickParseException If the given attach point is not valid for this point in the context stack
	 */
	public void addAttachPoint(String attachPoint) throws QuickParseException {
		Class<? extends QuickElement> type = getType();
		if(type == null)
			type = QuickElement.class;
		if(!(QuickTemplate.class.isAssignableFrom(type)))
			throw new QuickParseException("Element type " + type.getName() + " is not templated--cannot specify attach point styles");
		QuickTemplate.TemplateStructure templateStruct;
		try {
			templateStruct = QuickTemplate.TemplateStructure.getTemplateStructure(theEnv, (Class<? extends QuickTemplate>) type);
		} catch (CacheException e) {
			if (e.isFirstThrown())
				throw new QuickParseException("Could not parse template structure for " + type.getName(), e.getCause());
			else
				return; // Already reported--just ignore this piece
		}
		QuickTemplate.AttachPoint<?> ap = templateStruct.getAttachPoint(attachPoint);
		if(ap == null)
			throw new QuickParseException("Template " + type.getName() + " has no attach point named \"" + attachPoint + "\"");
		top().theTemplateRole = ap;
		top().theType = ap.type;
	}

	/** @return The element type for the top of the context stack */
	public Class<? extends QuickElement> getType() {
		return getType(theStack);
	}

	private <T> Iterable<T> descend(LinkedList<T> list) {
		return () -> list.descendingIterator();
	}

	private Class<? extends QuickElement> getType(LinkedList<ExpressionContext> stack) {
		for (ExpressionContext ctx : descend(stack)) {
			if (ctx.theType != null)
				return ctx.theType;
			else if (ctx.theTemplateRole != null)
				return ctx.theTemplateRole.type;
		}
		return QuickElement.class;
	}

	/** @return The state condition for the top of the context stack */
	public StateCondition getState() {
		return getState(theStack);
	}

	private StateCondition getState(LinkedList<ExpressionContext> stack) {
		StateCondition state = null;
		for (ExpressionContext ctx : stack) {
			if (ctx.theTemplateRole != null)
				state = null; // Only return the state after the last template
			if (ctx.theState != null) {
				if (state == null)
					state = ctx.theState;
				else
					state = state.and(ctx.theState);
			}
		}
		return state;
	}

	/** @return The set of groups for the top of the context stack */
	public Set<String> getGroups() {
		return getGroups(theStack);
	}

	private Set<String> getGroups(LinkedList<ExpressionContext> stack) {
		Set<String> groups = new LinkedHashSet<>();
		for (ExpressionContext ctx : stack) {
			if (ctx.theTemplateRole != null)
				groups.clear(); // Only return the groups after the last template
			groups.addAll(ctx.theGroups);
		}
		return groups;
	}

	/** @return The template role for the top of the context stack */
	public QuickTemplate.AttachPoint<?> getRole() {
		return getRole(theStack);
	}

	private AttachPoint<?> getRole(LinkedList<ExpressionContext> stack) {
		for (ExpressionContext ctx : stack)
			if (ctx.theTemplateRole != null)
				return ctx.theTemplateRole;
		return null;
	}

	/** @return The parent condition for the template role on the top of the context stack */
	public StyleCondition getParentCondition() {
		return getParentCondition(theStack);
	}

	private StyleCondition getParentCondition(LinkedList<ExpressionContext> stack) {
		Iterator<ExpressionContext> iter = stack.descendingIterator();
		while (iter.hasNext()) {
			ExpressionContext ctx = iter.next();
			if (ctx.theTemplateRole != null) {
				break;
			}
		}
		if (iter.hasNext()) {
			LinkedList<ExpressionContext> subStack = new LinkedList<>();
			do {
				subStack.add(iter.next());
			} while (iter.hasNext());
			Collections.reverse(subStack);
			return asCondition(subStack);
		} else
			return null;
	}

	/** @return The style condition representing the current context */
	public StyleCondition asCondition() {
		return asCondition(theStack);
	}

	private StyleCondition asCondition(LinkedList<ExpressionContext> stack) {
		return StyleCondition.build(getType(stack)).setState(getState(stack)).forGroups(getGroups(stack))
			.forRole(getRole(stack), getParentCondition(stack)).build();
	}
}