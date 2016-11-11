package org.quick.core.prop.antlr;

import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.quick.core.prop.antlr.QPPParser.ClassTypeContext;
import org.quick.core.prop.antlr.QPPParser.ConditionalExpressionContext;
import org.quick.core.prop.antlr.QPPParser.LiteralContext;

import com.google.common.reflect.TypeToken;

class ExpressionTypes {
	static class QualifiedName extends QPPExpression<ParserRuleContext> {
		private final ExpressionTypes.QualifiedName theQualifier;
		private final String theName;

		public QualifiedName(ParserRuleContext ctx, QualifiedName qualifier, String name) {
			super(ctx);
			theQualifier = qualifier;
			theName = name;
		}

		public ExpressionTypes.QualifiedName getQualifier() {
			return theQualifier;
		}

		public String getName() {
			return theName;
		}

		@Override
		public String print() {
			if (theQualifier != null)
				return theQualifier + "." + theName;
			else
				return theName;
		}
	}

	static abstract class Type<N extends ParserRuleContext> extends QPPExpression<N> {
		private final TypeToken<?> theType;

		public Type(N ctx) {
			super(ctx);
			theType = parseType(ctx);
		}

		public TypeToken<?> getType() {
			return theType;
		}

		protected abstract TypeToken<?> parseType(N ctx);
	}

	static class ClassType extends ExpressionTypes.Type<ClassTypeContext> {
		private final String theName;
		private final List<ExpressionTypes.Type<?>> theTypeArgs;

		public ClassType(ClassTypeContext ctx, String name, List<Type<?>> typeArgs) {
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

	static class MemberAccess extends QPPExpression<ParserRuleContext> {
		private final QPPExpression<?> theMemberContext;
		private final String theName;

		public MemberAccess(ParserRuleContext ctx, QPPExpression<?> memberContext, String name) {
			super(ctx);
			theMemberContext = memberContext;
			theName = name;
		}

		public QPPExpression<?> getMemberContext() {
			return theMemberContext;
		}

		public String getName() {
			return theName;
		}

		@Override
		public String print() {
			return theMemberContext.print() + "." + theName;
		}
	}

	static class FieldAccess extends MemberAccess {
		public FieldAccess(ParserRuleContext ctx, QPPExpression<?> methodCtx, String name) {
			super(ctx, methodCtx, name);
		}
	}

	static class MethodInvocation extends MemberAccess {
		private final boolean isSuper;
		private final List<Type<?>> theTypeArguments;
		private final List<QPPExpression<?>> theArguments;

		public MethodInvocation(ParserRuleContext ctx, QPPExpression<?> methodCtx, String name,
			List<Type<?>> typeArgs, List<QPPExpression<?>> args) {
			super(ctx, methodCtx, name);
			isSuper = false;
			theTypeArguments = typeArgs;
			theArguments = args;
		}

		protected MethodInvocation(ParserRuleContext ctx, QPPExpression<?> methodCtx, boolean isSuper, String name,
			List<Type<?>> typeArgs, List<QPPExpression<?>> args) {
			super(ctx, methodCtx, name);
			this.isSuper = isSuper;
			theTypeArguments = typeArgs;
			theArguments = args;
		}

		public boolean isSuper() {
			return isSuper;
		}

		public List<Type<?>> getTypeArguments() {
			return theTypeArguments;
		}

		public List<QPPExpression<?>> getArguments() {
			return theArguments;
		}

		@Override
		public String print() {
			StringBuilder print = new StringBuilder();
			if (getMemberContext() != null)
				print.append(getMemberContext().print()).append('.');
			if (isSuper)
				print.append("super.");
			print.append(getName());
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

	// static class Type extends QPPExpression {
	// private final List<String> theQualifiedName;
	// private final List<TypeParameter> theParameters;
	// }
	//
	// static class TypeParameter extends QPPExpression {
	// private final String theTypeParamName;
	// private final boolean theBoundExtends;
	// private final Type theBoundType;
	// }

	static class Parenthetic extends QPPExpression<ParserRuleContext> {
		private final QPPExpression<?> theContents;

		public Parenthetic(ParserRuleContext ctx, QPPExpression<?> contents) {
			super(ctx);
			theContents = contents;
		}

		public QPPExpression<?> getContents() {
			return theContents;
		}
	}

	static class Conditional extends QPPExpression<ConditionalExpressionContext> {
		private final QPPExpression<?> theCondition;
		private final QPPExpression<?> theAffirmative;
		private final QPPExpression<?> theNegative;

		public Conditional(ConditionalExpressionContext ctx, QPPExpression<?> condition, QPPExpression<?> affirmative,
			QPPExpression<?> negative) {
			super(ctx);
			theCondition = condition;
			theAffirmative = affirmative;
			theNegative = negative;
		}

		public QPPExpression<?> getCondition() {
			return theCondition;
		}

		public QPPExpression<?> getAffirmative() {
			return theAffirmative;
		}

		public QPPExpression<?> getNegative() {
			return theNegative;
		}

		@Override
		public String print() {
			return "(" + theCondition.print() + ")" + " ? " + "(" + theAffirmative.print() + ")" + " : " + "(" + theNegative.print()
				+ ")";
		}
	}

	static class Operation extends QPPExpression<ParserRuleContext> {
		private final String theName;
		private final QPPExpression<?> thePrimaryOperand;

		public Operation(ParserRuleContext ctx, String name, QPPExpression<?> left) {
			super(ctx);
			theName = name;
			thePrimaryOperand = left;
		}

		public String getName() {
			return theName;
		}

		public QPPExpression<?> getPrimaryOperand() {
			return thePrimaryOperand;
		}
	}

	static class BinaryOperation extends Operation {
		private final QPPExpression<?> theRight;

		public BinaryOperation(ParserRuleContext ctx, String name, QPPExpression<?> left, QPPExpression<?> right) {
			super(ctx, name, left);
			theRight = right;
		}

		public QPPExpression<?> getRight() {
			return theRight;
		}

		@Override
		public String print() {
			return "(" + getPrimaryOperand().print() + ") " + getName() + " (" + theRight.print() + ")";
		}
	}

	static class UnaryOperation extends Operation {
		private boolean isPreOp;

		public UnaryOperation(ParserRuleContext ctx, String name, boolean preOp, QPPExpression<?> operand) {
			super(ctx, name, operand);
			isPreOp = preOp;
		}

		public boolean isPreOp() {
			return isPreOp;
		}

		public void setPreOp(boolean isPreOp) {
			this.isPreOp = isPreOp;
		}

		@Override
		public String print() {
			if (isPreOp)
				return getName() + " (" + getPrimaryOperand().print() + ")";
			else
				return "(" + getPrimaryOperand().print() + ") " + getName();
		}
	}

	static class Cast extends QPPExpression<ParserRuleContext> {
		private final QPPExpression<?> theType;
		private final QPPExpression<?> theValue;

		public Cast(ParserRuleContext ctx, QPPExpression<?> type, QPPExpression<?> value) {
			super(ctx);
			theType = type;
			theValue = value;
		}

		public QPPExpression<?> getType() {
			return theType;
		}

		public QPPExpression<?> getValue() {
			return theValue;
		}

		@Override
		public String print() {
			return new StringBuilder().append('(').append(theType.print()).append(") ").append(theValue.print()).toString();
		}
	}

	static class ArrayAccess extends QPPExpression<ParserRuleContext> {
		private final QPPExpression<?> theArray;
		private final QPPExpression<?> theIndex;

		public ArrayAccess(ParserRuleContext ctx, QPPExpression<?> array, QPPExpression<?> index) {
			super(ctx);
			theArray = array;
			theIndex = index;
		}

		public QPPExpression<?> getArray() {
			return theArray;
		}

		public QPPExpression<?> getIndex() {
			return theIndex;
		}

		@Override
		public String print() {
			return new StringBuilder().append('(').append(theArray.print()).append(")[").append(theIndex.print()).append(']').toString();
		}
	}

	static class ArrayInitializer extends QPPExpression<ParserRuleContext> {
		private final QPPExpression<?> theType;
		private final List<QPPExpression<?>> theSizes;
		private final List<QPPExpression<?>> theElements;

		public ArrayInitializer(ParserRuleContext ctx, QPPExpression<?> type, List<QPPExpression<?>> sizes,
			List<QPPExpression<?>> elements) {
			super(ctx);
			theType = type;
			theSizes = sizes;
			theElements = elements;
		}

		public QPPExpression<?> getType() {
			return theType;
		}

		public List<QPPExpression<?>> getSizes() {
			return theSizes;
		}

		public List<QPPExpression<?>> getElements() {
			return theElements;
		}

		@Override
		public String print() {
			StringBuilder print = new StringBuilder().append("new ").append(theType.print());
			if (theSizes != null) {
				for (QPPExpression<?> size : theSizes)
					print.append('[').append(size.print()).append(']');
			} else {
				print.append('{');
				for (int i = 0; i < theElements.size(); i++) {
					if (i > 0)
						print.append(", ");
					print.append(theElements.get(i).print());
				}
				print.append('}');
			}
			return print.toString();
		}
	}

	static class Constructor extends QPPExpression<ParserRuleContext> {
		private final QPPExpression<?> theType;
		private final boolean isDiamond;
		private final List<Type<?>> theTypeArguments;
		private final List<QPPExpression<?>> theArguments;

		public Constructor(ParserRuleContext ctx, QPPExpression<?> type, boolean diamond, List<Type<?>> typeArgs,
			List<QPPExpression<?>> args) {
			super(ctx);
			theType = type;
			isDiamond = diamond;
			theTypeArguments = typeArgs;
			theArguments = args;
		}

		public QPPExpression<?> getType() {
			return theType;
		}

		public boolean isDiamond() {
			return isDiamond;
		}

		public List<Type<?>> getTypeArguments() {
			return theTypeArguments;
		}

		public List<QPPExpression<?>> getArguments() {
			return theArguments;
		}

		@Override
		public String print() {
			StringBuilder print = new StringBuilder().append("new ").append(theType.print());
			if (isDiamond)
				print.append("<>");
			else if (theTypeArguments != null) {
				print.append('<');
				for (int i = 0; i < theTypeArguments.size(); i++) {
					if (i > 0)
						print.append(", ");
					print.append(theTypeArguments.get(i).print());
				}
				print.append('>');
			}
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

	static class UnitValue extends QPPExpression<ParserRuleContext> {
		private final QPPExpression<?> theValue;
		private final String theUnit;

		public UnitValue(ParserRuleContext ctx, QPPExpression<?> value, String unit) {
			super(ctx);
			theValue = value;
			theUnit = unit;
		}

		public QPPExpression<?> getValue() {
			return theValue;
		}

		public String getUnit() {
			return theUnit;
		}

		@Override
		public String print() {
			return new StringBuilder().append('(').append(theValue.print()).append(") ").append(theUnit).toString();
		}
	}

	static class Placeholder extends QPPExpression<ParserRuleContext> {
		private final String theName;

		public Placeholder(ParserRuleContext ctx, String name) {
			super(ctx);
			theName = name;
		}

		public String getName() {
			return theName;
		}

		@Override
		public String print() {
			return toString();
		}
	}

	static abstract class Literal<T> extends QPPExpression<LiteralContext> {
		private final TypeToken<? extends T> theType;
		private final T theValue;

		public Literal(LiteralContext ctx) {
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

	static class NullLiteral extends Literal<Void> {
		public NullLiteral(LiteralContext ctx) {
			super(ctx);
		}

		@Override
		protected TypeToken<? extends Void> getType(LiteralContext ctx) {
			return TypeToken.of(Void.class);
		}

		@Override
		protected Void parseValue(LiteralContext ctx) {
			return null;
		}
	}

	static abstract class NumberLiteral extends Literal<Number> {
		public NumberLiteral(LiteralContext ctx) {
			super(ctx);
		}
	}

	static class IntegerLiteral extends NumberLiteral {
		public IntegerLiteral(LiteralContext ctx) {
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

	static class FloatLiteral extends NumberLiteral {
		public FloatLiteral(LiteralContext ctx) {
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

	static class BooleanLiteral extends ExpressionTypes.Literal<Boolean> {
		public BooleanLiteral(LiteralContext ctx) {
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

	static class CharLiteral extends ExpressionTypes.Literal<Character> {
		public CharLiteral(LiteralContext ctx) {
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

	static class StringLiteral extends ExpressionTypes.Literal<String> {
		protected StringLiteral(LiteralContext ctx) {
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
