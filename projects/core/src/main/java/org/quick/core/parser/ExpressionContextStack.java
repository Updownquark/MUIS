package org.quick.core.parser;

import java.util.*;

import org.quick.core.*;
import org.quick.core.style2.StateCondition;
import org.quick.core.style2.StyleCondition;

public class ExpressionContextStack {
	public class ExpressionContext {
		private final QuickClassView theClassView;

		private Class<? extends QuickElement> theType;

		final Set<String> theGroups;

		StateCondition theState;

		QuickTemplate.AttachPoint theTemplateRole;

		ExpressionContext(ExpressionContext parent) {
			theClassView = new QuickClassView(theEnv, parent == null ? theEnv.cv() : parent.theClassView, theToollkit);
			theGroups = new LinkedHashSet<>();
		}

		boolean isEmpty() {
			return theType == null && theGroups.isEmpty() && theState == null && theTemplateRole == null;
		}

		public QuickClassView getClassView() {
			return theClassView;
		}
	}

	private final QuickEnvironment theEnv;
	private final QuickToolkit theToollkit;

	private final LinkedList<ExpressionContext> theStack;

	public ExpressionContextStack(QuickEnvironment env, QuickToolkit toolkit) {
		theEnv = env;
		theToollkit = toolkit;
		theStack = new LinkedList<>();
	}

	public int size() {
		return theStack.size();
	}

	public void push() {
		ExpressionContext top = theStack.isEmpty() ? null : theStack.getLast();
		theStack.add(new ExpressionContext(top));
	}

	public void pop() {
		theStack.removeLast();
	}

	public ExpressionContext top() {
		return theStack.getLast();
	}

	public void addType(Class<? extends QuickElement> type) throws QuickParseException {
		Class<? extends QuickElement> topType = getTopType();
		if (!topType.isAssignableFrom(topType))
			throw new QuickParseException("Type " + type.getName() + " is not a sub type of a " + type.getName());
		if (topType != type)
			top().theType = type;
	}

	public void addGroup(String groupName) throws QuickParseException {
		top().theGroups.add(groupName);
	}

	public void setState(StateCondition state) {
		top().theState = state;
	}

	public void addAttachPoint(String attachPoint, QuickEnvironment env) throws QuickParseException {
		Class<? extends QuickElement> type = getTopType();
		if(type == null)
			type = QuickElement.class;
		if(!(QuickTemplate.class.isAssignableFrom(type)))
			throw new QuickParseException("Element type " + type.getName() + " is not templated--cannot specify attach point styles");
		QuickTemplate.TemplateStructure templateStruct;
		try {
			templateStruct = QuickTemplate.TemplateStructure.getTemplateStructure(env, (Class<? extends QuickTemplate>) type);
		} catch(QuickException e) {
			throw new QuickParseException("Could not parse template structure for " + type.getName(), e);
		}
		QuickTemplate.AttachPoint ap = templateStruct.getAttachPoint(attachPoint);
		if(ap == null)
			throw new QuickParseException("Template " + type.getName() + " has no attach point named \"" + attachPoint + "\"");
		top().theTemplateRole = ap;
		top().theType = ap.type;
	}

	public Class<? extends QuickElement> getTopType() {
		for(int i = theStack.size() - 1; i >= 0; i--) {
			if (theStack.get(i).theType != null)
				return theStack.get(i).theType;
		}
		return QuickElement.class;
	}

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

	public Set<String> getGroups() {
		Set<String> groups = new LinkedHashSet<>();
		for (ExpressionContext ctx : theStack)
			groups.addAll(ctx.theGroups);
		return groups;
	}

	public List<QuickTemplate.AttachPoint> getTemplatePath() {
		List<QuickTemplate.AttachPoint> templatePath = new ArrayList<>();
		for (ExpressionContext ctx : theStack)
			if (ctx.theTemplateRole != null)
				templatePath.add(ctx.theTemplateRole);
		return templatePath;
	}

	public StyleCondition asCondition() {
		return StyleCondition.build(getTopType()).setState(getState()).forGroups(getGroups()).forPath(getTemplatePath()).build();
	}
}