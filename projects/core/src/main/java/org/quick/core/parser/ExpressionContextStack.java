package org.quick.core.parser;

import java.util.*;

import org.quick.core.*;
import org.quick.core.style2.StateCondition;
import org.quick.core.style2.StyleCondition;

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
		Class<? extends QuickElement> topType = getTopType();
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
		Class<? extends QuickElement> type = getTopType();
		if(type == null)
			type = QuickElement.class;
		if(!(QuickTemplate.class.isAssignableFrom(type)))
			throw new QuickParseException("Element type " + type.getName() + " is not templated--cannot specify attach point styles");
		QuickTemplate.TemplateStructure templateStruct;
		try {
			templateStruct = QuickTemplate.TemplateStructure.getTemplateStructure(theEnv, (Class<? extends QuickTemplate>) type);
		} catch(QuickException e) {
			throw new QuickParseException("Could not parse template structure for " + type.getName(), e);
		}
		QuickTemplate.AttachPoint<?> ap = templateStruct.getAttachPoint(attachPoint);
		if(ap == null)
			throw new QuickParseException("Template " + type.getName() + " has no attach point named \"" + attachPoint + "\"");
		top().theTemplateRole = ap;
		top().theType = ap.type;
	}

	/** @return The element type for the top of the context stack */
	public Class<? extends QuickElement> getTopType() {
		for(int i = theStack.size() - 1; i >= 0; i--) {
			if (theStack.get(i).theType != null)
				return theStack.get(i).theType;
		}
		return QuickElement.class;
	}

	/** @return The state condition for the top of the context stack */
	public StateCondition getState() {
		StateCondition state = null;
		for (ExpressionContext ctx : theStack) {
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
		Set<String> groups = new LinkedHashSet<>();
		for (ExpressionContext ctx : theStack)
			groups.addAll(ctx.theGroups);
		return groups;
	}

	/** @return The template path for the top of the context stack */
	public List<QuickTemplate.AttachPoint<?>> getTemplatePath() {
		List<QuickTemplate.AttachPoint<?>> templatePath = new ArrayList<>();
		for (ExpressionContext ctx : theStack)
			if (ctx.theTemplateRole != null)
				templatePath.add(ctx.theTemplateRole);
		return templatePath;
	}

	/** @return The style condition representing the current context */
	public StyleCondition asCondition() {
		return StyleCondition.build(getTopType()).setState(getState()).forGroups(getGroups()).forPath(getTemplatePath()).build();
	}
}