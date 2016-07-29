package org.quick.core.parser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.quick.core.*;
import org.quick.core.style.sheet.StateGroupTypeExpression;
import org.quick.core.style.sheet.TemplateRole;
import org.quick.core.style.stateful.StateExpression;

public class ExpressionContextStack implements Iterable<StateGroupTypeExpression<?>> {
	public class ExpressionContext {
		private final QuickClassView theClassView;

		private final ArrayList<Class<? extends QuickElement>> theTypes;

		final ArrayList<String> theGroups;

		StateExpression theState;

		TemplateRole theTemplatePath;

		ExpressionContext(ExpressionContext parent) {
			theClassView = new QuickClassView(theEnv, parent == null ? theEnv.cv() : parent.theClassView, theToollkit);
			theTypes = new ArrayList<>();
			theGroups = new ArrayList<>();
		}

		boolean isEmpty() {
			return theTypes.isEmpty() && theGroups.isEmpty() && theState == null && theTemplatePath == null;
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
		for(int i = theStack.size() - 1; i >= 0; i--) {
			if(!theStack.get(i).theTypes.isEmpty()) {
				boolean isSubType = false;
				for(Class<?> preType : theStack.get(i).theTypes) {
					if(preType.equals(type))
						throw new QuickParseException("Type " + type.getSimpleName() + " is already in this category");
					if(preType.isAssignableFrom(type)) {
						isSubType = true;
						break;
					}
				}
				if(!isSubType) {
					throw new QuickParseException("Type " + type.getSimpleName() + " is not a sub type of a type in a super-category");
				}
			}
		}
		top().theTypes.add(type);
	}

	public void addGroup(String groupName) throws QuickParseException {
		for(int i = 0; i < theStack.size() - 1; i++) {
			if(theStack.get(i).theTemplatePath != null)
				break;
			if(!theStack.get(i).theGroups.isEmpty())
				throw new QuickParseException(
					"Groups are not hierarchical--a category with a group cannot contain a category with a group");
		}
		top().theGroups.add(groupName);
	}

	public void setState(StateExpression state) {
		top().theState = state;
	}

	public void addAttachPoint(String attachPoint, QuickEnvironment env) throws QuickParseException {
		Class<? extends QuickElement> type = null;
		for(int i = theStack.size() - 1; i >= 0; i--) {
			if(!theStack.get(i).theTypes.isEmpty()) {
				if(theStack.get(i).theTypes.size() > 1)
					throw new QuickParseException("Cannot attach-point specific styles to more than one type at once");
				type = theStack.get(i).theTypes.get(0);
				break;
			}
		}
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
		Set<String> parentGroups = new LinkedHashSet<>();
		for(int i = theStack.size() - 1; i >= 0; i--)
			if(!theStack.get(i).theGroups.isEmpty()) {
				parentGroups.addAll(theStack.get(i).theGroups);
				break;
			}
		top().theTemplatePath = new TemplateRole(ap, parentGroups, (Class<? extends QuickTemplate>) type, getTopTemplateRole());
		top().theTypes.add(ap.type);
	}

	public Class<? extends QuickElement> [] getTopTypes() {
		for(int i = theStack.size() - 1; i >= 0; i--) {
			if(theStack.get(i).theTemplatePath != null)
				return new Class[0];
			if(!theStack.get(i).theTypes.isEmpty())
				return theStack.get(i).theTypes.toArray(new Class[theStack.get(i).theTypes.size()]);
		}
		return new Class[0];
	}

	public TemplateRole getTopTemplateRole() {
		for(int i = theStack.size() - 1; i >= 0; i--) {
			if(theStack.get(i).theTemplatePath != null)
				return theStack.get(i).theTemplatePath;
		}
		return null;
	}

	@Override
	public java.util.Iterator<StateGroupTypeExpression<?>> iterator() {
		if(top().isEmpty()) {
			return new java.util.Iterator<StateGroupTypeExpression<?>>() {
				@Override
				public boolean hasNext() {
					return false;
				}

				@Override
				public StateGroupTypeExpression<?> next() {
					return null;
				}

				@Override
				public void remove() {
				}
			};
		}
		return new java.util.Iterator<StateGroupTypeExpression<?>>() {
			private Class<? extends QuickElement> [] theIterableTypes;

			private ArrayList<String> theIterableGroups;

			private StateExpression theOverallState;

			private TemplateRole theTemplatePath;

			private int theTypeIdx;

			private int theGroupIdx;

			private boolean hasCalledNext;

			{
				theIterableTypes = getTopTypes();
				theTemplatePath = getTopTemplateRole();
				theIterableGroups = new ArrayList<>();
				for(int i = theStack.size() - 1; i >= 0; i--) {
					if(theStack.get(i).theTemplatePath != null)
						break;
					if(theIterableGroups.isEmpty())
						theIterableGroups.addAll(theStack.get(i).theGroups);
				}
				for(ExpressionContext ctx : theStack) {
					if(ctx.theState != null) {
						if(theOverallState == null)
							theOverallState = ctx.theState;
						else
							theOverallState = theOverallState.and(ctx.theState);
					}
				}
			}

			@Override
			public boolean hasNext() {
				return !hasCalledNext || theTypeIdx < theIterableTypes.length || theGroupIdx < theIterableGroups.size();
			}

			@Override
			public StateGroupTypeExpression<?> next() {
				StateGroupTypeExpression<?> ret;
				if(theGroupIdx < theIterableGroups.size()) {
					if(theTypeIdx < theIterableTypes.length) {
						ret = new StateGroupTypeExpression<>(theOverallState, theIterableGroups.get(theGroupIdx),
							theIterableTypes[theTypeIdx++], theTemplatePath);
					} else {
						ret = new StateGroupTypeExpression<>(theOverallState, theIterableGroups.get(theGroupIdx), null, theTemplatePath);
					}
					if(theTypeIdx >= theIterableTypes.length) {
						theGroupIdx++;
						theTypeIdx = 0;
					}
				} else if(theIterableGroups.isEmpty()) {
					if(theTypeIdx < theIterableTypes.length) {
						ret = new StateGroupTypeExpression<>(theOverallState, null, theIterableTypes[theTypeIdx++], theTemplatePath);
					} else if(theIterableTypes.length == 0) {
						ret = new StateGroupTypeExpression<>(theOverallState, null, null, theTemplatePath);
					} else
						throw new java.util.NoSuchElementException();
				} else
					throw new java.util.NoSuchElementException();
				hasCalledNext = true;
				return ret;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}