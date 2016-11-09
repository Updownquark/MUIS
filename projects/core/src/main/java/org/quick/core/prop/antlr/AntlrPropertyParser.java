package org.quick.core.prop.antlr;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.observe.ObservableValue;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickParseEnv;
import org.quick.core.parser.QuickParseException;
import org.quick.core.parser.QuickPropertyParser;
import org.quick.core.prop.QuickProperty;
import org.quick.core.prop.antlr.AntlrPropertyParser.ExpressionTypes.QualifiedNameExpression;
import org.quick.core.prop.antlr.AntlrPropertyParser.ExpressionTypes.TypeExpression;
import org.quick.core.prop.antlr.QPPParser.*;

import com.google.common.reflect.TypeToken;

public class AntlrPropertyParser implements QuickPropertyParser {
	private final QuickEnvironment theEnv;

	public AntlrPropertyParser(QuickEnvironment env) {
		theEnv = env;
	}

	@Override
	public QuickEnvironment getEnvironment() {
		return theEnv;
	}

	@Override
	public <T> ObservableValue<T> parseProperty(QuickProperty<T> property, QuickParseEnv parseEnv, String value)
		throws QuickParseException {
		// TODO Auto-generated method stub
		return null;
	}

	private <T> QPPExpression<?> compile(String expression) throws QuickParseException {
		try {
			// lexer splits input into tokens
			ANTLRInputStream input = new ANTLRInputStream(expression);
			TokenStream tokens = new CommonTokenStream(new QPPLexer(input));

			// parser generates abstract syntax tree
			QPPParser parser = new QPPParser(tokens);
			// QPPParser.expression_return ret = parser.expression();

			// acquire parse result
			ParseTreeWalker walker = new ParseTreeWalker();
			QPPCompiler compiler = new QPPCompiler();
			walker.walk(compiler, parser.expression());
			return compiler.getExpression();
		} catch (RecognitionException e) {
			throw new QuickParseException("Recognition exception is never thrown, only declared.");
		}
	}

	public static void main(String[] args) {
		AntlrPropertyParser parser = new AntlrPropertyParser(null);

		try {
			parser.compile("0");
			System.err.println();
			// Literals
			System.out.println(parser.compile("0").print());
			System.out.println(parser.compile("1").print());
			System.out.println(parser.compile("0xff1l").print());
			System.out.println(parser.compile("0.5").print());
			System.out.println(parser.compile("0x1.ffp-1f").print());
			System.out.println(parser.compile("true").print());
			System.out.println(parser.compile("0x1.ffp-1f").print());
			System.out.println(parser.compile("\"\"").print());
			System.out.println(parser.compile("\"A string\\u00b0\\t8 4\"").print());
			System.out.println(parser.compile("'\"'").print());
			System.out.println(parser.compile("'\\u00b0'").print());

			System.out.println(parser.compile("method(0)").print());
			System.out.println(parser.compile("method(0, 1)").print());
			System.out.println(parser.compile("ctx.method(0, 1)").print());
			System.out.println(parser.compile("type.super.method(0, 1)").print());
			System.out.println(parser.compile("type.<X, T>method(0, 1)").print());

			System.out.println(parser.compile("c++").print());
			System.out.println(parser.compile("c=10").print());
			System.out.println(parser.compile("c+=10").print());
			System.out.println(parser.compile("a.b.c+=10").print());
		} catch (QuickParseException e) {
			e.printStackTrace();
		}
	}

	static abstract class QPPExpression<N extends ParseTree> {
		private final N theCtx;
		private String theError;

		protected QPPExpression(N ctx) {
			theCtx = ctx;
		}

		public N getContext() {
			return theCtx;
		}

		protected void error(String error) {
			if (theError == null)
				theError = error;
			else
				theError += "\n" + error;
		}

		@Override
		public String toString() {
			return theCtx.getText();
		}

		public String print() {
			return toString();
		}
	}

	static class ExpressionTypes {
		static class ParseError extends QPPExpression<ErrorNode> {
			protected ParseError(ErrorNode ctx) {
				super(ctx);
			}
		}

		static class InterpreterError extends QPPExpression<ParseTree> {
			private final String theError;

			public InterpreterError(ParseTree ctx, String error) {
				super(ctx);
				theError = error;
			}

			@Override
			public String print() {
				return super.print() + ": " + theError;
			}
		}

		static abstract class ArgExpression<N extends ParseTree> extends QPPExpression<N> {
			private final List<QPPExpression<?>> theArguments;

			protected ArgExpression(N ctx, List<QPPExpression<?>> arguments) {
				super(ctx);
				theArguments = arguments;
			}
		}

		// static class ConstructorExpression extends ArgExpression {
		// private final TypeExpression theType;
		//
		// protected ConstructorExpression(ParseTree ctx, TypeExpression type, List<QPPExpression> arguments) {
		// super(ctx, arguments);
		// theType = type;
		// }
		// }

		static class QualifiedNameExpression extends QPPExpression<ParseTree> {
			private final QualifiedNameExpression theQualifier;
			private final String theName;

			protected QualifiedNameExpression(ParseTree ctx, QualifiedNameExpression qualifier, String name) {
				super(ctx);
				theQualifier = qualifier;
				theName = name;
			}

			@Override
			public String print() {
				if (theQualifier != null)
					return theQualifier + "." + theName;
				else
					return theName;
			}
		}

		static abstract class TypeExpression<N extends ParseTree> extends QPPExpression<N> {
			private final TypeToken<?> theType;

			protected TypeExpression(N ctx) {
				super(ctx);
				theType = parseType(ctx);
			}

			public TypeToken<?> getType() {
				return theType;
			}

			protected abstract TypeToken<?> parseType(N ctx);
		}

		static class PrimitiveTypeExpression extends TypeExpression<PrimitiveTypeContext> {
			protected PrimitiveTypeExpression(PrimitiveTypeContext ctx) {
				super(ctx);
			}

			@Override
			protected TypeToken<?> parseType(PrimitiveTypeContext ctx) {
				switch (ctx.getText()) {
				case "boolean":
					return TypeToken.of(Boolean.TYPE);
				case "char":
					return TypeToken.of(Character.TYPE);
				case "byte":
					return TypeToken.of(Byte.TYPE);
				case "short":
					return TypeToken.of(Short.TYPE);
				case "int":
					return TypeToken.of(Integer.TYPE);
				case "long":
					return TypeToken.of(Long.TYPE);
				case "float":
					return TypeToken.of(Float.TYPE);
				case "double":
					return TypeToken.of(Double.TYPE);
				default:
					error("Unrecognized primitive type: " + ctx.getText());
					return TypeToken.of(Object.class);
				}
			}
		}

		static class ClassTypeExpression extends TypeExpression<ClassTypeContext> {
			private final String theName;
			private final List<TypeExpression<?>> theTypeArgs;

			protected ClassTypeExpression(ClassTypeContext ctx, String name, List<TypeExpression<?>> typeArgs) {
				super(ctx);
				theName = name;
				theTypeArgs = typeArgs;
			}

			@Override
			protected TypeToken<?> parseType(ClassTypeContext ctx) {
				// TODO Auto-generated method stub
				return null;
			}
		}

		static class MethodInvocationExpression extends QPPExpression<ParserRuleContext> {
			private final QPPExpression<?> theMethodContext;
			private final boolean isSuper;
			private final String theName;
			private final List<TypeExpression<?>> theTypeArguments;
			private final List<QPPExpression<?>> theArguments;

			protected MethodInvocationExpression(ParserRuleContext ctx, QPPExpression<?> methodCtx, String name,
				List<TypeExpression<?>> typeArgs, List<QPPExpression<?>> args) {
				super(ctx);
				theMethodContext = methodCtx;
				isSuper = false;
				theName = name;
				theTypeArguments = typeArgs;
				theArguments = args;
			}

			protected MethodInvocationExpression(ParserRuleContext ctx, QPPExpression<?> methodCtx, boolean isSuper, String name,
				List<TypeExpression<?>> typeArgs, List<QPPExpression<?>> args) {
				super(ctx);
				theMethodContext = methodCtx;
				this.isSuper = isSuper;
				theName = name;
				theTypeArguments = typeArgs;
				theArguments = args;
			}

			@Override
			public String print() {
				StringBuilder print = new StringBuilder();
				if (theMethodContext != null)
					print.append(theMethodContext.print()).append('.');
				if (isSuper)
					print.append("super.");
				print.append(theName);
				print.append('(');
				for (int i = 0; i < theArguments.size(); i++) {
					if (i != 0)
						print.append(", ");
					print.append(theArguments.get(i).print());
				}
				print.append(')');
				return print.toString();
			}
		}

		// static class TypeExpression extends QPPExpression {
		// private final List<String> theQualifiedName;
		// private final List<TypeParameter> theParameters;
		// }
		//
		// static class TypeParameter extends QPPExpression {
		// private final String theTypeParamName;
		// private final boolean theBoundExtends;
		// private final TypeExpression theBoundType;
		// }

		static class ConditionalExpression extends QPPExpression<ConditionalExpressionContext> {
			private final QPPExpression<?> theCondition;
			private final QPPExpression<?> theAffirmative;
			private final QPPExpression<?> theNegative;

			public ConditionalExpression(ConditionalExpressionContext ctx, QPPExpression<?> condition, QPPExpression<?> affirmative,
				QPPExpression<?> negative) {
				super(ctx);
				theCondition = condition;
				theAffirmative = affirmative;
				theNegative = negative;
			}

			@Override
			public String print() {
				return "(" + theCondition.print() + ")" + " ? " + "(" + theAffirmative.print() + ")" + " : " + "(" + theNegative.print()
					+ ")";
			}
		}

		static class BinaryExpression<N extends ParseTree> extends QPPExpression<N> {
			private final String theName;
			private final QPPExpression<?> theLeft;
			private final QPPExpression<?> theRight;

			public BinaryExpression(N ctx, String name, QPPExpression<?> left, QPPExpression<?> right) {
				super(ctx);
				theName = name;
				theLeft = left;
				theRight = right;
			}

			@Override
			public String print() {
				return "(" + theLeft.print() + ") " + theName + " (" + theRight.print() + ")";
			}
		}

		static class UnaryExpression<N extends ParseTree> extends QPPExpression<N> {
			private final String theName;
			private boolean isPreOp;
			private final QPPExpression<?> theOperand;

			public UnaryExpression(N ctx, String name, boolean preOp, QPPExpression<?> operand) {
				super(ctx);
				theName = name;
				isPreOp = preOp;
				theOperand = operand;
			}

			@Override
			public String print() {
				if (isPreOp)
					return theName + " (" + theOperand.print() + ")";
				else
					return "(" + theOperand.print() + ") " + theName;
			}
		}

		static abstract class LiteralExpression<T> extends QPPExpression<LiteralContext> {
			private final TypeToken<? extends T> theType;
			private final T theValue;

			protected LiteralExpression(LiteralContext ctx) {
				super(ctx);
				theType = getType(ctx);
				theValue = parseValue(ctx);
			}

			public TypeToken<? extends T> getType() {
				return theType;
			}

			public T getValue() {
				return theValue;
			}

			protected abstract TypeToken<? extends T> getType(LiteralContext ctx);

			protected abstract T parseValue(LiteralContext ctx);

			@Override
			public String print() {
				return super.print() + ": " + theValue + " (" + theType + ")";
			}
		}

		static class IntegerLiteralExpression extends LiteralExpression<Number> {
			protected IntegerLiteralExpression(LiteralContext ctx) {
				super(ctx);
			}

			@Override
			protected TypeToken<? extends Number> getType(LiteralContext ctx) {
				String text = ctx.getText();
				if (text.endsWith("l") || text.endsWith("L"))
					return TypeToken.of(Long.TYPE);
				else
					return TypeToken.of(Integer.TYPE);
			}

			@Override
			protected Number parseValue(LiteralContext ctx) {
				String text = ctx.getText();
				boolean isLong = getType().getRawType() == Long.TYPE;
				boolean isHex = text.startsWith("0x") || text.startsWith("0X");
				boolean isBin = !isHex && (text.startsWith("0b") || text.startsWith("0B"));
				boolean isOct = !isHex && !isBin && text.length() > 1 && text.startsWith("0");

				StringBuilder content = new StringBuilder(text);
				if (isLong)
					content.deleteCharAt(content.length() - 1);
				if (isHex || isBin)
					content.delete(0, 2);
				else if (isOct)
					content.deleteCharAt(0);

				for (int i = content.length() - 1; i >= 0; i--) {
					if (content.charAt(i) == '_') {
						content.deleteCharAt(i);
						i--;
					}
				}
				int radix;
				if (isHex)
					radix = 16;
				else if (isOct)
					radix = 8;
				else if (isBin)
					radix = 2;
				else
					radix = 10;

				if (isLong)
					return Long.valueOf(content.toString(), radix);
				else
					return Integer.valueOf(content.toString(), radix);
			}
		}

		static class FloatLiteralExpression extends LiteralExpression<Number> {
			protected FloatLiteralExpression(LiteralContext ctx) {
				super(ctx);
			}

			@Override
			protected TypeToken<? extends Number> getType(LiteralContext ctx) {
				String text = ctx.getText();
				if (text.endsWith("f") || text.endsWith("F"))
					return TypeToken.of(Float.TYPE);
				else
					return TypeToken.of(Double.TYPE);
			}

			@Override
			protected Number parseValue(LiteralContext ctx) {
				String text = ctx.getText();
				boolean isFloat = getType().getRawType() == Float.TYPE;
				if (isFloat)
					return Float.parseFloat(text);
				else
					return Double.parseDouble(text);
			}
		}

		static class BooleanLiteralExpression extends LiteralExpression<Boolean> {
			protected BooleanLiteralExpression(LiteralContext ctx) {
				super(ctx);
			}

			@Override
			protected TypeToken<Boolean> getType(LiteralContext ctx) {
				return TypeToken.of(Boolean.TYPE);
			}

			@Override
			protected Boolean parseValue(LiteralContext ctx) {
				return Boolean.valueOf(ctx.getText());
			}
		}

		static class CharLiteralExpression extends LiteralExpression<Character> {
			protected CharLiteralExpression(LiteralContext ctx) {
				super(ctx);
			}

			@Override
			protected TypeToken<Character> getType(LiteralContext ctx) {
				return TypeToken.of(Character.TYPE);
			}

			@Override
			protected Character parseValue(LiteralContext ctx) {
				String text = ctx.getText();
				return unescapeJavaString(text.substring(1, text.length() - 1)).charAt(0); // Take off the quotes and unescape
			}
		}

		static class StringLiteralExpression extends LiteralExpression<String> {
			protected StringLiteralExpression(LiteralContext ctx) {
				super(ctx);
			}

			@Override
			protected TypeToken<String> getType(LiteralContext ctx) {
				return TypeToken.of(String.class);
			}

			@Override
			protected String parseValue(LiteralContext ctx) {
				String text = ctx.getText();
				return unescapeJavaString(text.substring(1, text.length() - 1)); // Take off the quotes and unescape
			}
		}

		/**
		 * <p>
		 * Copied from <a href="https://gist.github.com/uklimaschewski/6741769">https://gist.github.com/uklimaschewski/6741769</a>
		 * </p>
		 *
		 * Unescapes a string that contains standard Java escape sequences.
		 * <ul>
		 * <li><strong>&#92;b &#92;f &#92;n &#92;r &#92;t &#92;" &#92;'</strong> : BS, FF, NL, CR, TAB, double and single quote.</li>
		 * <li><strong>&#92;X &#92;XX &#92;XXX</strong> : Octal character specification (0 - 377, 0x00 - 0xFF).</li>
		 * <li><strong>&#92;uXXXX</strong> : Hexadecimal based Unicode character.</li>
		 * </ul>
		 *
		 * @param st A string optionally containing standard java escape sequences.
		 * @return The translated string.
		 */
		public static String unescapeJavaString(String st) {

			StringBuilder sb = new StringBuilder(st.length());

			for (int i = 0; i < st.length(); i++) {
				char ch = st.charAt(i);
				if (ch == '\\') {
					char nextChar = (i == st.length() - 1) ? '\\' : st.charAt(i + 1);
					// Octal escape?
					if (nextChar >= '0' && nextChar <= '7') {
						String code = "" + nextChar;
						i++;
						if ((i < st.length() - 1) && st.charAt(i + 1) >= '0' && st.charAt(i + 1) <= '7') {
							code += st.charAt(i + 1);
							i++;
							if ((i < st.length() - 1) && st.charAt(i + 1) >= '0' && st.charAt(i + 1) <= '7') {
								code += st.charAt(i + 1);
								i++;
							}
						}
						sb.append((char) Integer.parseInt(code, 8));
						continue;
					}
					switch (nextChar) {
					case '\\':
						ch = '\\';
						break;
					case 'b':
						ch = '\b';
						break;
					case 'f':
						ch = '\f';
						break;
					case 'n':
						ch = '\n';
						break;
					case 'r':
						ch = '\r';
						break;
					case 't':
						ch = '\t';
						break;
					case '\"':
						ch = '\"';
						break;
					case '\'':
						ch = '\'';
						break;
					// Hex Unicode: u????
					case 'u':
						if (i >= st.length() - 5) {
							ch = 'u';
							break;
						}
						int code = Integer.parseInt("" + st.charAt(i + 2) + st.charAt(i + 3) + st.charAt(i + 4) + st.charAt(i + 5), 16);
						sb.append(Character.toChars(code));
						i += 5;
						continue;
					}
					i++;
				}
				sb.append(ch);
			}
			return sb.toString();
		}
	}

	private static class QPPCompiler extends QPPBaseListener {
		private final Map<ParseTree, QPPExpression<?>> theDanglingExpressions;

		public QPPCompiler() {
			theDanglingExpressions = new HashMap<>();
		}

		public QPPExpression<?> getExpression() {
			if (theDanglingExpressions.size() != 1)
				throw new IllegalStateException();
			return theDanglingExpressions.values().iterator().next();
		}

		private void push(QPPExpression<?> expression) {
			push(expression, expression.getContext());
		}

		private void push(QPPExpression<?> expression, ParseTree ctx) {
			theDanglingExpressions.put(ctx, expression);
		}

		private void ascend(ParseTree inner, ParseTree outer) {
			push(pop(inner), outer);
		}

		private QPPExpression<?> pop(ParseTree ctx) {
			QPPExpression<?> exp = theDanglingExpressions.remove(ctx);
			if (exp == null)
				throw new IllegalStateException(
					"Expression " + ctx.getText() + ", type " + ctx.getClass().getSimpleName() + " was not evaluated. Perhaps exit"
						+ ctx.getClass().getSimpleName().substring(0, ctx.getClass().getSimpleName().length() - "Context".length())
						+ "() is not implemented.");
			return exp;
		}

		@Override
		public void visitErrorNode(ErrorNode arg0) {
			push(new ExpressionTypes.ParseError(arg0));
		}

		@Override
		public void visitTerminal(TerminalNode node) {
			// TODO Auto-generated method stub
			super.visitTerminal(node);
		}

		@Override
		public void exitEveryRule(ParserRuleContext ctx) {
			// TODO Auto-generated method stub
			super.exitEveryRule(ctx);
		}

		@Override
		public void exitLiteral(LiteralContext ctx) {
			switch (ctx.start.getType()) {
			case QPPParser.IntegerLiteral:
				push(new ExpressionTypes.IntegerLiteralExpression(ctx));
				break;
			case QPPParser.FloatingPointLiteral:
				push(new ExpressionTypes.FloatLiteralExpression(ctx));
				break;
			case QPPParser.BooleanLiteral:
				push(new ExpressionTypes.BooleanLiteralExpression(ctx));
				break;
			case QPPParser.CharacterLiteral:
				push(new ExpressionTypes.CharLiteralExpression(ctx));
				break;
			case QPPParser.StringLiteral:
				push(new ExpressionTypes.StringLiteralExpression(ctx));
				break;
			default:
				push(new ExpressionTypes.InterpreterError(ctx,
					"Unrecognized literal type: " + QPPParser.tokenNames[ctx.start.getType()] + " (" + ctx.start.getType() + ")"));
				break;
			}
		}

		@Override
		public void exitExpressionName(ExpressionNameContext ctx) {
			if(ctx.ambiguousName()!=null)
				push(new ExpressionTypes.QualifiedNameExpression(ctx, (QualifiedNameExpression) pop(ctx.ambiguousName()),
					ctx.Identifier().getText()));
			else
				push(new ExpressionTypes.QualifiedNameExpression(ctx, null, ctx.Identifier().getText()));
		}

		@Override
		public void exitAmbiguousName(AmbiguousNameContext ctx) {
			if(ctx.ambiguousName()!=null)
				push(new ExpressionTypes.QualifiedNameExpression(ctx, (QualifiedNameExpression) pop(ctx.ambiguousName()),
					ctx.Identifier().getText()));
			else
				push(new ExpressionTypes.QualifiedNameExpression(ctx, null, ctx.Identifier().getText()));
		}

		@Override
		public void exitAssignment(AssignmentContext ctx) {
			push(new ExpressionTypes.BinaryExpression<>(ctx, ctx.assignmentOperator().getText(), pop(ctx.leftHandSide()),
				pop(ctx.expression())));
		}

		@Override
		public void exitAssignmentExpression(AssignmentExpressionContext ctx) {
			if (ctx.conditionalExpression() != null)
				ascend(ctx.conditionalExpression(), ctx);
			else
				ascend(ctx.assignment(), ctx);
		}

		@Override
		public void exitLeftHandSide(LeftHandSideContext ctx) {
			if (ctx.expressionName() != null)
				ascend(ctx.expressionName(), ctx);
			else if (ctx.fieldAccess() != null)
				ascend(ctx.fieldAccess(), ctx);
			else
				ascend(ctx.arrayAccess(), ctx);
		}

		@Override
		public void exitFieldAccess(FieldAccessContext ctx) {
			if (ctx.primary() != null)
				push(new ExpressionTypes.QualifiedNameExpression(ctx, (QualifiedNameExpression) pop(ctx.primary()),
					ctx.Identifier().getText()));
			else if (ctx.typeName() != null)
				push(new ExpressionTypes.QualifiedNameExpression(ctx, (QualifiedNameExpression) pop(ctx.typeName()),
					"super." + ctx.Identifier().getText()));
			else
				push(new ExpressionTypes.QualifiedNameExpression(ctx, null, "super." + ctx.Identifier().getText()));
		}

		@Override
		public void exitFieldAccess_lf_primary(FieldAccess_lf_primaryContext ctx) {
			// TODO Auto-generated method stub
			super.exitFieldAccess_lf_primary(ctx);
		}

		@Override
		public void exitFieldAccess_lfno_primary(FieldAccess_lfno_primaryContext ctx) {
			// TODO Auto-generated method stub
			super.exitFieldAccess_lfno_primary(ctx);
		}

		@Override
		public void exitArrayAccess(ArrayAccessContext ctx) {
			// TODO Auto-generated method stub
			super.exitArrayAccess(ctx);
		}

		@Override
		public void exitArrayAccess_lf_primary(ArrayAccess_lf_primaryContext ctx) {
			// TODO Auto-generated method stub
			super.exitArrayAccess_lf_primary(ctx);
		}

		@Override
		public void exitArrayAccess_lfno_primary(ArrayAccess_lfno_primaryContext ctx) {
			// TODO Auto-generated method stub
			super.exitArrayAccess_lfno_primary(ctx);
		}

		@Override
		public void exitClassInstanceCreationExpression(ClassInstanceCreationExpressionContext ctx) {
			// TODO Auto-generated method stub
			super.exitClassInstanceCreationExpression(ctx);
		}

		@Override
		public void exitType(TypeContext ctx) {
			// TODO Auto-generated method stub
			super.exitType(ctx);
		}

		@Override
		public void exitMethodReference_lfno_primary(MethodReference_lfno_primaryContext ctx) {
			// TODO Auto-generated method stub
			super.exitMethodReference_lfno_primary(ctx);
		}

		@Override
		public void exitCastExpression(CastExpressionContext ctx) {
			// TODO Auto-generated method stub
			super.exitCastExpression(ctx);
		}

		@Override
		public void exitVariableInitializerList(VariableInitializerListContext ctx) {
			// TODO Auto-generated method stub
			super.exitVariableInitializerList(ctx);
		}

		@Override
		public void exitEqualityExpression(EqualityExpressionContext ctx) {
			if (ctx.equalityExpression() != null)
				push(new ExpressionTypes.BinaryExpression<>(ctx, getOperator(ctx), pop(ctx.equalityExpression()),
					pop(ctx.relationalExpression())));
			else
				ascend(ctx.relationalExpression(), ctx);
		}

		private String getOperator(EqualityExpressionContext ctx) {
			return "== or !=";
		}

		@Override
		public void exitRelationalExpression(RelationalExpressionContext ctx) {
			if (ctx.referenceType() != null)
				push(new ExpressionTypes.BinaryExpression<>(ctx, "instanceof", pop(ctx.relationalExpression()), pop(ctx.referenceType())));
			else if (ctx.relationalExpression() != null)
				push(new ExpressionTypes.BinaryExpression<>(ctx, getOperator(ctx), pop(ctx.relationalExpression()),
					pop(ctx.shiftExpression())));
			else
				ascend(ctx.shiftExpression(), ctx);
		}

		private String getOperator(RelationalExpressionContext ctx) {
			return "relation";
		}

		@Override
		public void exitConditionalOrExpression(ConditionalOrExpressionContext ctx) {
			if (ctx.conditionalOrExpression() != null)
				push(new ExpressionTypes.BinaryExpression<>(ctx, "||", pop(ctx.conditionalOrExpression()),
					pop(ctx.conditionalAndExpression())));
			else
				ascend(ctx.conditionalAndExpression(), ctx);
		}

		@Override
		public void exitConditionalAndExpression(ConditionalAndExpressionContext ctx) {
			if (ctx.conditionalAndExpression() != null)
				push(new ExpressionTypes.BinaryExpression<>(ctx, "&&", pop(ctx.conditionalAndExpression()),
					pop(ctx.inclusiveOrExpression())));
			else
				ascend(ctx.inclusiveOrExpression(), ctx);
		}

		@Override
		public void exitInclusiveOrExpression(InclusiveOrExpressionContext ctx) {
			if (ctx.inclusiveOrExpression() != null)
				push(new ExpressionTypes.BinaryExpression<>(ctx, "|", pop(ctx.inclusiveOrExpression()), pop(ctx.exclusiveOrExpression())));
			else
				ascend(ctx.exclusiveOrExpression(), ctx);
		}

		@Override
		public void exitExclusiveOrExpression(ExclusiveOrExpressionContext ctx) {
			if (ctx.exclusiveOrExpression() != null)
				push(new ExpressionTypes.BinaryExpression<>(ctx, "^", pop(ctx.exclusiveOrExpression()), pop(ctx.andExpression())));
			else
				ascend(ctx.andExpression(), ctx);
		}

		@Override
		public void exitAndExpression(AndExpressionContext ctx) {
			if (ctx.andExpression() != null)
				push(new ExpressionTypes.BinaryExpression<>(ctx, "&", pop(ctx.andExpression()), pop(ctx.equalityExpression())));
			else
				ascend(ctx.equalityExpression(), ctx);
		}

		@Override
		public void exitConditionalExpression(ConditionalExpressionContext ctx) {
			if (ctx.expression() != null)
				push(new ExpressionTypes.ConditionalExpression(ctx, pop(ctx.conditionalOrExpression()), pop(ctx.expression()),
					pop(ctx.conditionalExpression())));
			else
				ascend(ctx.conditionalOrExpression(), ctx);
		}

		@Override
		public void exitShiftExpression(ShiftExpressionContext ctx) {
			if (ctx.shiftExpression() != null)
				push(new ExpressionTypes.BinaryExpression<>(ctx, getOperator(ctx), pop(ctx.shiftExpression()),
					pop(ctx.additiveExpression())));
			else
				ascend(ctx.additiveExpression(), ctx);
		}

		private String getOperator(ShiftExpressionContext ctx) {
			return "shift";
		}

		@Override
		public void exitAdditiveExpression(AdditiveExpressionContext ctx) {
			if (ctx.additiveExpression() != null)
				push(new ExpressionTypes.BinaryExpression<>(ctx, getOperator(ctx), pop(ctx.additiveExpression()),
					pop(ctx.multiplicativeExpression())));
			else
				ascend(ctx.multiplicativeExpression(), ctx);
		}

		private String getOperator(AdditiveExpressionContext ctx) {
			return "additive";
		}

		@Override
		public void exitMultiplicativeExpression(MultiplicativeExpressionContext ctx) {
			if (ctx.multiplicativeExpression() != null)
				push(new ExpressionTypes.BinaryExpression<>(ctx, getOperator(ctx), pop(ctx.multiplicativeExpression()),
					pop(ctx.unaryExpression())));
			else
				ascend(ctx.unaryExpression(), ctx);
		}

		private String getOperator(MultiplicativeExpressionContext ctx) {
			return "multiplicative";
		}

		@Override
		public void exitUnaryExpression(UnaryExpressionContext ctx) {
			if (ctx.preIncrementExpression() != null)
				ascend(ctx.preIncrementExpression(), ctx);
			else if (ctx.preDecrementExpression() != null)
				ascend(ctx.preDecrementExpression(), ctx);
			else if (ctx.unaryExpression() != null)
				push(new ExpressionTypes.UnaryExpression<>(ctx, getOperator(ctx), true, pop(ctx.unaryExpression())));
			else
				ascend(ctx.unaryExpressionNotPlusMinus(), ctx);
		}

		private String getOperator(UnaryExpressionContext ctx) {
			return "plus/minus";
		}

		@Override
		public void exitUnaryExpressionNotPlusMinus(UnaryExpressionNotPlusMinusContext ctx) {
			if (ctx.postfixExpression() != null)
				ascend(ctx.postfixExpression(), ctx);
			else if (ctx.castExpression() != null)
				ascend(ctx.castExpression(), ctx);
			else
				push(new ExpressionTypes.UnaryExpression<>(ctx, getOperator(ctx), true, pop(ctx.unaryExpression())));
		}

		private String getOperator(UnaryExpressionNotPlusMinusContext ctx) {
			return "~ or !";
		}

		@Override
		public void exitPostfixExpression(PostfixExpressionContext ctx) {
			if (ctx.primary() != null)
				ascend(ctx.primary(), ctx);
			else if (ctx.expressionName() != null)
				ascend(ctx.expressionName(), ctx);
			else
				super.exitPostfixExpression(ctx); // TODO Partial implementation
		}

		@Override
		public void exitPreIncrementExpression(PreIncrementExpressionContext ctx) {
			push(new ExpressionTypes.UnaryExpression<>(ctx, "++", true, pop(ctx.unaryExpression())));
		}

		@Override
		public void exitPostIncrementExpression(PostIncrementExpressionContext ctx) {
			// TODO Auto-generated method stub
			super.exitPostIncrementExpression(ctx);
		}

		@Override
		public void exitPostIncrementExpression_lf_postfixExpression(PostIncrementExpression_lf_postfixExpressionContext ctx) {
			// TODO Auto-generated method stub
			super.exitPostIncrementExpression_lf_postfixExpression(ctx);
		}

		@Override
		public void exitPreDecrementExpression(PreDecrementExpressionContext ctx) {
			push(new ExpressionTypes.UnaryExpression<>(ctx, "--", true, pop(ctx.unaryExpression())));
		}

		@Override
		public void exitPostDecrementExpression(PostDecrementExpressionContext ctx) {
			// TODO Auto-generated method stub
			super.exitPostDecrementExpression(ctx);
		}

		@Override
		public void exitPostDecrementExpression_lf_postfixExpression(PostDecrementExpression_lf_postfixExpressionContext ctx) {
			// TODO Auto-generated method stub
			super.exitPostDecrementExpression_lf_postfixExpression(ctx);
		}

		@Override
		public void exitPrimary(PrimaryContext ctx) {
			QPPExpression<?> init;
			if (ctx.primaryNoNewArray_lfno_primary() != null)
				init = pop(ctx.primaryNoNewArray_lfno_primary());
			else
				init = pop(ctx.arrayCreationExpression());
			if (ctx.primaryNoNewArray_lf_primary().isEmpty())
				push(init, ctx);
			else
				super.exitPrimary(ctx); // TODO Partial implementation
		}

		@Override
		public void exitPrimaryNoNewArray_lfno_primary(PrimaryNoNewArray_lfno_primaryContext ctx) {
			if (ctx.literal() != null)
				ascend(ctx.literal(), ctx);
			else if (ctx.expression() != null)
				ascend(ctx.expression(), ctx);
			else if (ctx.classInstanceCreationExpression_lfno_primary() != null)
				ascend(ctx.classInstanceCreationExpression_lfno_primary(), ctx);
			else if (ctx.fieldAccess_lfno_primary() != null)
				ascend(ctx.fieldAccess_lfno_primary(), ctx);
			else if (ctx.arrayAccess_lfno_primary() != null)
				ascend(ctx.arrayAccess_lfno_primary(), ctx);
			else if (ctx.methodInvocation_lfno_primary() != null)
				ascend(ctx.methodInvocation_lfno_primary(), ctx);
			else if (ctx.methodReference_lfno_primary() != null)
				ascend(ctx.methodReference_lfno_primary(), ctx);
			else
				super.exitPrimaryNoNewArray_lfno_primary(ctx); // TODO Partial implementation
		}

		@Override
		public void exitPrimaryNoNewArray_lf_primary(PrimaryNoNewArray_lf_primaryContext ctx) {
			// TODO Auto-generated method stub
			super.exitPrimaryNoNewArray_lf_primary(ctx);
		}

		@Override
		public void exitPrimaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primary(
			PrimaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primaryContext ctx) {
			// TODO Auto-generated method stub
			super.exitPrimaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primary(ctx);
		}

		@Override
		public void exitPrimaryNoNewArray(PrimaryNoNewArrayContext ctx) {
			// TODO Auto-generated method stub
			super.exitPrimaryNoNewArray(ctx);
		}

		@Override
		public void exitPrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary(
			PrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primaryContext ctx) {
			// TODO Auto-generated method stub
			super.exitPrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary(ctx);
		}

		@Override
		public void exitPrimaryNoNewArray_lfno_arrayAccess(PrimaryNoNewArray_lfno_arrayAccessContext ctx) {
			// TODO Auto-generated method stub
			super.exitPrimaryNoNewArray_lfno_arrayAccess(ctx);
		}

		@Override
		public void exitPrimaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primary(
			PrimaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primaryContext ctx) {
			// TODO Auto-generated method stub
			super.exitPrimaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primary(ctx);
		}

		@Override
		public void exitPrimaryNoNewArray_lf_arrayAccess(PrimaryNoNewArray_lf_arrayAccessContext ctx) {
			// TODO Auto-generated method stub
			super.exitPrimaryNoNewArray_lf_arrayAccess(ctx);
		}

		@Override
		public void exitPrimaryNoNewArray_lf_primary_lf_arrayAccess_lf_primary(
			PrimaryNoNewArray_lf_primary_lf_arrayAccess_lf_primaryContext ctx) {
			// TODO Auto-generated method stub
			super.exitPrimaryNoNewArray_lf_primary_lf_arrayAccess_lf_primary(ctx);
		}

		@Override
		public void exitClassInstanceCreationExpression_lfno_primary(ClassInstanceCreationExpression_lfno_primaryContext ctx) {
			// TODO Auto-generated method stub
			super.exitClassInstanceCreationExpression_lfno_primary(ctx);
		}

		@Override
		public void exitArrayInitializer(ArrayInitializerContext ctx) {
			// TODO Auto-generated method stub
			super.exitArrayInitializer(ctx);
		}

		@Override
		public void exitDimExprs(DimExprsContext ctx) {
			// TODO Auto-generated method stub
			super.exitDimExprs(ctx);
		}

		@Override
		public void exitFloatingPointType(FloatingPointTypeContext ctx) {
			// TODO Auto-generated method stub
			super.exitFloatingPointType(ctx);
		}

		@Override
		public void exitDimExpr(DimExprContext ctx) {
			// TODO Auto-generated method stub
			super.exitDimExpr(ctx);
		}

		@Override
		public void exitIntegralType(IntegralTypeContext ctx) {
			// TODO Auto-generated method stub
			super.exitIntegralType(ctx);
		}

		@Override
		public void exitWildcardBounds(WildcardBoundsContext ctx) {
			// TODO Auto-generated method stub
			super.exitWildcardBounds(ctx);
		}

		@Override
		public void exitArrayCreationExpression(ArrayCreationExpressionContext ctx) {
			// TODO Auto-generated method stub
			super.exitArrayCreationExpression(ctx);
		}

		@Override
		public void exitTypeBound(TypeBoundContext ctx) {
			// TODO Auto-generated method stub
			super.exitTypeBound(ctx);
		}

		@Override
		public void exitTypeVariable(TypeVariableContext ctx) {
			// TODO Auto-generated method stub
			super.exitTypeVariable(ctx);
		}

		@Override
		public void exitTypeParameter(TypeParameterContext ctx) {
			// TODO Auto-generated method stub
			super.exitTypeParameter(ctx);
		}

		@Override
		public void exitDims(DimsContext ctx) {
			// TODO Auto-generated method stub
			super.exitDims(ctx);
		}

		@Override
		public void exitUnannPrimitiveType(UnannPrimitiveTypeContext ctx) {
			// TODO Auto-generated method stub
			super.exitUnannPrimitiveType(ctx);
		}

		@Override
		public void exitAdditionalBound(AdditionalBoundContext ctx) {
			// TODO Auto-generated method stub
			super.exitAdditionalBound(ctx);
		}

		@Override
		public void exitWildcard(WildcardContext ctx) {
			// TODO Auto-generated method stub
			super.exitWildcard(ctx);
		}

		@Override
		public void exitMethodReference_lf_primary(MethodReference_lf_primaryContext ctx) {
			// TODO Auto-generated method stub
			super.exitMethodReference_lf_primary(ctx);
		}

		@Override
		public void exitPackageName(PackageNameContext ctx) {
			// TODO Auto-generated method stub
			super.exitPackageName(ctx);
		}

		@Override
		public void exitPrimitiveType(PrimitiveTypeContext ctx) {
			// TODO Auto-generated method stub
			super.exitPrimitiveType(ctx);
		}

		@Override
		public void exitVariableInitializer(VariableInitializerContext ctx) {
			// TODO Auto-generated method stub
			super.exitVariableInitializer(ctx);
		}

		@Override
		public void exitExpression(ExpressionContext ctx) {
			ascend(ctx.assignmentExpression(), ctx);
		}

		@Override
		public void exitConstantExpression(ConstantExpressionContext ctx) {
			// TODO Auto-generated method stub
			super.exitConstantExpression(ctx);
		}

		@Override
		public void exitMethodInvocation(MethodInvocationContext ctx) {
			// TODO how to recognize type.super.method()?
			methodFor(ctx, c -> c.argumentList(), c -> c.typeArguments(), c -> c.methodName(), c -> c.Identifier(), c -> c.typeName(),
				c -> c.expressionName(), c -> c.primary(), false);
		}

		@Override
		public void exitMethodInvocation_lf_primary(MethodInvocation_lf_primaryContext ctx) {
			// TODO how to recognize type.super.method()?
			methodFor(ctx, c -> c.argumentList(), c -> c.typeArguments(), null, c -> c.Identifier(), null, null, null, false);
		}

		@Override
		public void exitMethodInvocation_lfno_primary(MethodInvocation_lfno_primaryContext ctx) {
			// TODO how to recognize type.super.method()?
			methodFor(ctx, //
				c -> c.argumentList(), c -> c.typeArguments(), c -> c.methodName(), c -> c.Identifier(), c -> c.typeName(),
				c -> c.expressionName(), null, false);
		}

		private <C extends ParserRuleContext> void methodFor(C ctx, Function<C, ArgumentListContext> argumentList,
			Function<C, TypeArgumentsContext> typeArguments, Function<C, MethodNameContext> methodName,
			Function<C, TerminalNode> identifier, Function<C, TypeNameContext> typeName, Function<C, ExpressionNameContext> expressionName,
			Function<C, PrimaryContext> primary, boolean isSuper) {
			ExpressionTypes.MethodInvocationExpression exp;
			List<QPPExpression<?>> args = argumentList.apply(ctx).expression().stream().map(x -> pop(x)).collect(Collectors.toList());
			List<TypeExpression<?>> typeArgs = typeArguments == null ? null : parseTypeArguments(typeArguments.apply(ctx));
			if (methodName != null && methodName.apply(ctx) != null) {
				exp = new ExpressionTypes.MethodInvocationExpression(ctx, null, isSuper, methodName.apply(ctx).getText(), typeArgs, args);
			} else if (typeName != null && typeName.apply(ctx) != null) {
				exp = new ExpressionTypes.MethodInvocationExpression(ctx, pop(typeName.apply(ctx)), isSuper,
					identifier.apply(ctx).getText(), typeArgs, args);
			} else if (expressionName != null && expressionName.apply(ctx) != null) {
				exp = new ExpressionTypes.MethodInvocationExpression(ctx, pop(expressionName.apply(ctx)), isSuper,
					identifier.apply(ctx).getText(), typeArgs, args);
			} else if (primary != null && primary.apply(ctx) != null) {
				exp = new ExpressionTypes.MethodInvocationExpression(ctx, pop(primary.apply(ctx)), isSuper, identifier.apply(ctx).getText(),
					typeArgs, args);
			} else {
				exp = new ExpressionTypes.MethodInvocationExpression(ctx, null, isSuper, identifier.apply(ctx).getText(), typeArgs, args);
			}
			push(exp);
		}

		List<QPPExpression<?>> parseArguments(ArgumentListContext argumentList) {
			return argumentList == null ? Collections.emptyList()
				: argumentList.expression().stream().map(x -> pop(x)).collect(Collectors.toList());
		}

		List<TypeExpression<?>> parseTypeArguments(TypeArgumentsContext typeArguments) {
			return typeArguments == null ? null : typeArguments.typeArgumentList().typeArgument().stream()
				.map(a -> (TypeExpression<?>) pop(a)).collect(Collectors.toList());
		}

		@Override
		public void exitTypeArgument(TypeArgumentContext ctx) {
			if (ctx.referenceType() != null)
				ascend(ctx.referenceType(), ctx);
			else
				ascend(ctx.wildcard(), ctx);
		}

		@Override
		public void exitTypeName(TypeNameContext ctx) {
			if (ctx.packageOrTypeName() != null)
				push(new ExpressionTypes.QualifiedNameExpression(ctx, (QualifiedNameExpression) pop(ctx.packageOrTypeName()),
					ctx.Identifier().getText()));
			else
				push(new ExpressionTypes.QualifiedNameExpression(ctx, null, ctx.Identifier().getText()));
		}

		@Override
		public void exitPackageOrTypeName(PackageOrTypeNameContext ctx) {
			if (ctx.packageOrTypeName() != null)
				push(new ExpressionTypes.QualifiedNameExpression(ctx, (QualifiedNameExpression) pop(ctx.packageOrTypeName()),
					ctx.Identifier().getText()));
			else
				push(new ExpressionTypes.QualifiedNameExpression(ctx, null, ctx.Identifier().getText()));
		}

		@Override
		public void exitReferenceType(ReferenceTypeContext ctx) {
			if (ctx.classType() != null)
				ascend(ctx.classType(), ctx);
			else
				ascend(ctx.arrayType(), ctx);
		}

		@Override
		public void exitClassType(ClassTypeContext ctx) {
			push(new ExpressionTypes.ClassTypeExpression(ctx, ctx.Identifier().getText(), parseTypeArguments(ctx.typeArguments())));
		}

		@Override
		public void exitArrayType(ArrayTypeContext ctx) {
			// TODO Auto-generated method stub
			super.exitArrayType(ctx);
		}

		@Override
		public void exitMethodName(MethodNameContext ctx) {
			// TODO Auto-generated method stub
			super.exitMethodName(ctx);
		}

		@Override
		public void exitClassInstanceCreationExpression_lf_primary(ClassInstanceCreationExpression_lf_primaryContext ctx) {
			// TODO Auto-generated method stub
			super.exitClassInstanceCreationExpression_lf_primary(ctx);
		}

		@Override
		public void exitMethodReference(MethodReferenceContext ctx) {
			// TODO Auto-generated method stub
			super.exitMethodReference(ctx);
		}

		@Override
		public void exitNumericType(NumericTypeContext ctx) {
			// TODO Auto-generated method stub
			super.exitNumericType(ctx);
		}

		@Override
		public void exitTypeArgumentsOrDiamond(TypeArgumentsOrDiamondContext ctx) {
			// TODO Auto-generated method stub
			super.exitTypeArgumentsOrDiamond(ctx);
		}
	}
}
