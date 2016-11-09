package org.quick.core.prop.antlr;

import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.quick.core.prop.antlr.QPPParser.ClassTypeContext;
import org.quick.core.prop.antlr.QPPParser.ConditionalExpressionContext;
import org.quick.core.prop.antlr.QPPParser.LiteralContext;
import org.quick.core.prop.antlr.QPPParser.PrimitiveTypeContext;

import com.google.common.reflect.TypeToken;

class ExpressionTypes {
	static class ParseError extends QPPExpression<ErrorNode> {
		public ParseError(ErrorNode ctx) {
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
		private final ExpressionTypes.QualifiedNameExpression theQualifier;
		private final String theName;

		public QualifiedNameExpression(ParseTree ctx, QualifiedNameExpression qualifier, String name) {
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

		public TypeExpression(N ctx) {
			super(ctx);
			theType = parseType(ctx);
		}

		public TypeToken<?> getType() {
			return theType;
		}

		protected abstract TypeToken<?> parseType(N ctx);
	}

	static class PrimitiveTypeExpression extends TypeExpression<PrimitiveTypeContext> {
		public PrimitiveTypeExpression(PrimitiveTypeContext ctx) {
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

	static class ClassTypeExpression extends ExpressionTypes.TypeExpression<ClassTypeContext> {
		private final String theName;
		private final List<ExpressionTypes.TypeExpression<?>> theTypeArgs;

		public ClassTypeExpression(ClassTypeContext ctx, String name, List<TypeExpression<?>> typeArgs) {
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

		public MethodInvocationExpression(ParserRuleContext ctx, QPPExpression<?> methodCtx, String name,
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

		public LiteralExpression(LiteralContext ctx) {
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

	static class IntegerLiteralExpression extends ExpressionTypes.LiteralExpression<Number> {
		public IntegerLiteralExpression(LiteralContext ctx) {
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

	static class FloatLiteralExpression extends ExpressionTypes.LiteralExpression<Number> {
		public FloatLiteralExpression(LiteralContext ctx) {
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

	static class BooleanLiteralExpression extends ExpressionTypes.LiteralExpression<Boolean> {
		public BooleanLiteralExpression(LiteralContext ctx) {
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

	static class CharLiteralExpression extends ExpressionTypes.LiteralExpression<Character> {
		public CharLiteralExpression(LiteralContext ctx) {
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

	static class StringLiteralExpression extends ExpressionTypes.LiteralExpression<String> {
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