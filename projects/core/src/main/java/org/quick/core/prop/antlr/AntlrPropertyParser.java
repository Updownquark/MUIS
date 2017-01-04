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
import org.observe.ObservableAction;
import org.observe.ObservableValue;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickParseEnv;
import org.quick.core.parser.AbstractPropertyParser;
import org.quick.core.parser.QuickParseException;
import org.quick.core.prop.antlr.QPPParser.*;

import com.google.common.reflect.TypeToken;

/** Uses ANTLR to parse Quick properties */
public class AntlrPropertyParser extends AbstractPropertyParser {
	/** @param env The Quick environment to parse in */
	public AntlrPropertyParser(QuickEnvironment env) {
		super(env);
	}

	@Override
	protected <T> ObservableValue<?> parseDefaultValue(QuickParseEnv parseEnv, TypeToken<T> type, String value) throws QuickParseException {
		QPPExpression parsed = compile(value);
		searchForErrors(parsed);
		boolean action = TypeToken.of(ObservableAction.class).isAssignableFrom(type);
		return AntlrPropertyEvaluator.evaluateTypeless(parseEnv, type, parsed, action, action);
	}

	@Override
	public TypeToken<?> parseType(QuickParseEnv parseEnv, String expression) throws QuickParseException {
		ExpressionTypes.Type typeExpr;
		try {
			// lexer splits input into tokens
			ANTLRInputStream input = new ANTLRInputStream(expression);
			TokenStream tokens = new CommonTokenStream(new QPPLexer(input));

			// parser generates abstract syntax tree
			QPPParser parser = new QPPParser(tokens);

			// acquire parse result
			ParseTreeWalker walker = new ParseTreeWalker();
			QPPCompiler compiler = new QPPCompiler();
			walker.walk(compiler, parser.type());
			typeExpr = (ExpressionTypes.Type) compiler.getExpression();
		} catch (RecognitionException e) {
			throw new QuickParseException("Parsing failed for " + expression, e);
		} catch (IllegalStateException e) {
			throw new QuickParseException("Parsing failed for " + expression, e);
		}
		return AntlrPropertyEvaluator.evaluateType(parseEnv, typeExpr);
	}

	private <T> QPPExpression compile(String expression) throws QuickParseException {
		try {
			// lexer splits input into tokens
			ANTLRInputStream input = new ANTLRInputStream(expression);
			TokenStream tokens = new CommonTokenStream(new QPPLexer(input));

			// parser generates abstract syntax tree
			QPPParser parser = new QPPParser(tokens);

			// acquire parse result
			ParseTreeWalker walker = new ParseTreeWalker();
			QPPCompiler compiler = new QPPCompiler();
			walker.walk(compiler, parser.compoundExpression());
			return compiler.getExpression();
		} catch (RecognitionException e) {
			throw new QuickParseException("Parsing failed for " + expression, e);
		} catch (IllegalStateException e) {
			throw new QuickParseException("Parsing failed for " + expression, e);
		}
	}

	private void searchForErrors(QPPExpression expr) throws QuickParseException {
		// TODO
	}

	private static class QPPCompiler extends QPPBaseListener {
		private final Map<ParserRuleContext, QPPExpression> theDanglingExpressions;

		public QPPCompiler() {
			theDanglingExpressions = new HashMap<>();
		}

		public QPPExpression getExpression() {
			if (theDanglingExpressions.size() != 1)
				throw new IllegalStateException();
			return theDanglingExpressions.values().iterator().next();
		}

		private void push(QPPExpression expression) {
			push(expression, expression.getContext());
		}

		private void push(QPPExpression expression, ParserRuleContext ctx) {
			if (ctx == null)
				throw new NullPointerException();
			if (ctx.exception != null)
				throw ctx.exception;
			theDanglingExpressions.put(ctx, expression);
		}

		private void ascend(ParserRuleContext inner, ParserRuleContext outer) {
			if (outer.exception != null)
				throw outer.exception;
			push(pop(inner), outer);
		}

		private QPPExpression pop(ParserRuleContext ctx) {
			if (ctx == null)
				throw new NullPointerException();
			QPPExpression exp = theDanglingExpressions.remove(ctx);
			if (exp == null)
				throw new IllegalStateException(
					"Expression " + ctx.getText() + ", type " + ctx.getClass().getSimpleName() + " was not evaluated. Perhaps exit"
						+ ctx.getClass().getSimpleName().substring(0, ctx.getClass().getSimpleName().length() - "Context".length())
						+ "() is not implemented.");
			return exp;
		}

		@Override
		public void visitErrorNode(ErrorNode arg0) {
			throw new IllegalStateException("Unexpected token ( " + arg0.getSymbol().getText() + ", type "
				+ QPPParser.VOCABULARY.getDisplayName(arg0.getSymbol().getType()) + " ) at position " + arg0.getSymbol().getStartIndex());
		}

		@Override
		public void visitTerminal(TerminalNode node) {
			// Don't think there's anything to do here, but might be useful to have it for debugging sometime
		}

		@Override
		public void exitEveryRule(ParserRuleContext ctx) {
			// Don't think there's anything to do here, but might be useful to have it for debugging sometime
		}

		@Override
		public void exitLiteral(LiteralContext ctx) {
			switch (ctx.start.getType()) {
			case QPPParser.IntegerLiteral:
				push(new ExpressionTypes.IntegerLiteral(ctx));
				break;
			case QPPParser.FloatingPointLiteral:
				push(new ExpressionTypes.FloatLiteral(ctx));
				break;
			case QPPParser.BooleanLiteral:
				push(new ExpressionTypes.BooleanLiteral(ctx));
				break;
			case QPPParser.CharacterLiteral:
				push(new ExpressionTypes.CharLiteral(ctx));
				break;
			case QPPParser.StringLiteral:
				push(new ExpressionTypes.StringLiteral(ctx));
				break;
			default:
				throw new IllegalStateException("Unrecognized literal type: " + QPPParser.VOCABULARY.getDisplayName(ctx.start.getType())
					+ " (" + ctx.start.getType() + ") at position " + ctx.start.getStartIndex());
			}
		}

		@Override
		public void exitCompoundExpression(CompoundExpressionContext ctx) {
			if (ctx.expression().size() == 1)
				ascend(ctx.expression().get(0), ctx);
			else
				push(new ExpressionTypes.CompoundExpression(ctx,
					ctx.expression().stream().map(exp -> pop(exp)).collect(Collectors.toList())));
		}

		@Override
		public void exitExpressionName(ExpressionNameContext ctx) {
			if (ctx.ambiguousName() != null)
				push(new ExpressionTypes.QualifiedName(ctx, (ExpressionTypes.QualifiedName) pop(ctx.ambiguousName()),
					ctx.Identifier().getText()));
			else
				push(new ExpressionTypes.QualifiedName(ctx, null, ctx.Identifier().getText()));
		}

		@Override
		public void exitAmbiguousName(AmbiguousNameContext ctx) {
			if (ctx.ambiguousName() != null)
				push(new ExpressionTypes.QualifiedName(ctx, (ExpressionTypes.QualifiedName) pop(ctx.ambiguousName()),
					ctx.Identifier().getText()));
			else
				push(new ExpressionTypes.QualifiedName(ctx, null, ctx.Identifier().getText()));
		}

		@Override
		public void exitAssignment(AssignmentContext ctx) {
			push(new ExpressionTypes.BinaryOperation(ctx, ctx.assignmentOperator().getText(), pop(ctx.leftHandSide()),
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
			push(new ExpressionTypes.FieldAccess(ctx, pop(ctx.primary()), ctx.Identifier().getText()));
		}

		@Override
		public void exitArrayAccess(ArrayAccessContext ctx) {
			QPPExpression result;
			result = new ExpressionTypes.ArrayAccess(ctx,
				pop(ctx.expressionName() != null ? ctx.expressionName() : ctx.primaryNoNewArray_lfno_arrayAccess()),
				pop(ctx.expression(0)));
			for (int i = 0; i < ctx.primaryNoNewArray_lf_arrayAccess().size(); i++)
				result = new ExpressionTypes.ArrayAccess(ctx, result, pop(ctx.expression(i + 1)));
			push(result);
		}

		@Override
		public void exitArrayAccess_lfno_primary(ArrayAccess_lfno_primaryContext ctx) {
			QPPExpression result;
			result = new ExpressionTypes.ArrayAccess(ctx, pop(
				ctx.expressionName() != null ? ctx.expressionName() : ctx.primaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary()),
				pop(ctx.expression(0)));
			for (int i = 0; i < ctx.primaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primary().size(); i++)
				result = new ExpressionTypes.ArrayAccess(ctx, result, pop(ctx.expression(i + 1)));
			push(result);
		}

		@Override
		public void exitClassInstanceCreationExpression(ClassInstanceCreationExpressionContext ctx) {
			StringBuilder typeName = new StringBuilder(ctx.Identifier(0).getText());
			for (int i = 1; i < ctx.Identifier().size(); i++)
				typeName.append('.').append(ctx.Identifier(i).getText());
			List<ExpressionTypes.Type> classTypeArgs;
			if (ctx.typeArgumentsOrDiamond() == null)
				classTypeArgs = null;
			else if (ctx.typeArgumentsOrDiamond().typeArguments() == null)
				classTypeArgs = Collections.emptyList();
			else
				classTypeArgs = parseTypeArguments(ctx.typeArgumentsOrDiamond().typeArguments());
			push(new ExpressionTypes.Constructor(ctx, new ExpressionTypes.ClassType(ctx, typeName.toString(), classTypeArgs),
				parseTypeArguments(ctx.typeArguments()), parseArguments(ctx.argumentList())));
		}

		@Override
		public void exitCastExpression(CastExpressionContext ctx) {
			if (ctx.primitiveType() != null)
				push(new ExpressionTypes.Cast(ctx, (ExpressionTypes.PrimitiveType) pop(ctx.primitiveType()), pop(ctx.unaryExpression())));
			else
				push(
					new ExpressionTypes.Cast(ctx, (ExpressionTypes.Type) pop(ctx.referenceType()), pop(ctx.unaryExpressionNotPlusMinus())));
		}

		@Override
		public void exitEqualityExpression(EqualityExpressionContext ctx) {
			if (ctx.equalityExpression() != null)
				push(new ExpressionTypes.BinaryOperation(ctx, getOperator(ctx), pop(ctx.equalityExpression()),
					pop(ctx.relationalExpression())));
			else
				ascend(ctx.relationalExpression(), ctx);
		}

		private String getOperator(EqualityExpressionContext ctx) {
			return ((TerminalNode) ctx.getChild(1)).getText();
		}

		@Override
		public void exitRelationalExpression(RelationalExpressionContext ctx) {
			if (ctx.referenceType() != null)
				push(new ExpressionTypes.BinaryOperation(ctx, "instanceof", pop(ctx.relationalExpression()), pop(ctx.referenceType())));
			else if (ctx.relationalExpression() != null)
				push(new ExpressionTypes.BinaryOperation(ctx, getOperator(ctx), pop(ctx.relationalExpression()),
					pop(ctx.shiftExpression())));
			else
				ascend(ctx.shiftExpression(), ctx);
		}

		private String getOperator(RelationalExpressionContext ctx) {
			return ((TerminalNode) ctx.getChild(1)).getText();
		}

		@Override
		public void exitConditionalOrExpression(ConditionalOrExpressionContext ctx) {
			if (ctx.conditionalOrExpression() != null)
				push(new ExpressionTypes.BinaryOperation(ctx, "||", pop(ctx.conditionalOrExpression()),
					pop(ctx.conditionalAndExpression())));
			else
				ascend(ctx.conditionalAndExpression(), ctx);
		}

		@Override
		public void exitConditionalAndExpression(ConditionalAndExpressionContext ctx) {
			if (ctx.conditionalAndExpression() != null)
				push(new ExpressionTypes.BinaryOperation(ctx, "&&", pop(ctx.conditionalAndExpression()), pop(ctx.inclusiveOrExpression())));
			else
				ascend(ctx.inclusiveOrExpression(), ctx);
		}

		@Override
		public void exitInclusiveOrExpression(InclusiveOrExpressionContext ctx) {
			if (ctx.inclusiveOrExpression() != null)
				push(new ExpressionTypes.BinaryOperation(ctx, "|", pop(ctx.inclusiveOrExpression()), pop(ctx.exclusiveOrExpression())));
			else
				ascend(ctx.exclusiveOrExpression(), ctx);
		}

		@Override
		public void exitExclusiveOrExpression(ExclusiveOrExpressionContext ctx) {
			if (ctx.exclusiveOrExpression() != null)
				push(new ExpressionTypes.BinaryOperation(ctx, "^", pop(ctx.exclusiveOrExpression()), pop(ctx.andExpression())));
			else
				ascend(ctx.andExpression(), ctx);
		}

		@Override
		public void exitAndExpression(AndExpressionContext ctx) {
			if (ctx.andExpression() != null)
				push(new ExpressionTypes.BinaryOperation(ctx, "&", pop(ctx.andExpression()), pop(ctx.equalityExpression())));
			else
				ascend(ctx.equalityExpression(), ctx);
		}

		@Override
		public void exitConditionalExpression(ConditionalExpressionContext ctx) {
			if (ctx.expression() != null)
				push(new ExpressionTypes.Conditional(ctx, pop(ctx.conditionalOrExpression()), pop(ctx.expression()),
					pop(ctx.conditionalExpression())));
			else
				ascend(ctx.conditionalOrExpression(), ctx);
		}

		@Override
		public void exitShiftExpression(ShiftExpressionContext ctx) {
			if (ctx.shiftExpression() != null)
				push(new ExpressionTypes.BinaryOperation(ctx, getOperator(ctx), pop(ctx.shiftExpression()), pop(ctx.additiveExpression())));
			else
				ascend(ctx.additiveExpression(), ctx);
		}

		private String getOperator(ShiftExpressionContext ctx) {
			return ((TerminalNode) ctx.getChild(1)).getText();
		}

		@Override
		public void exitAdditiveExpression(AdditiveExpressionContext ctx) {
			if (ctx.additiveExpression() != null)
				push(new ExpressionTypes.BinaryOperation(ctx, getOperator(ctx), pop(ctx.additiveExpression()),
					pop(ctx.multiplicativeExpression())));
			else
				ascend(ctx.multiplicativeExpression(), ctx);
		}

		private String getOperator(AdditiveExpressionContext ctx) {
			return ((TerminalNode) ctx.getChild(1)).getText();
		}

		@Override
		public void exitMultiplicativeExpression(MultiplicativeExpressionContext ctx) {
			if (ctx.multiplicativeExpression() != null)
				push(new ExpressionTypes.BinaryOperation(ctx, getOperator(ctx), pop(ctx.multiplicativeExpression()),
					pop(ctx.unaryExpression())));
			else
				ascend(ctx.unaryExpression(), ctx);
		}

		private String getOperator(MultiplicativeExpressionContext ctx) {
			return ((TerminalNode) ctx.getChild(1)).getText();
		}

		@Override
		public void exitUnaryExpression(UnaryExpressionContext ctx) {
			if (ctx.preIncrementExpression() != null)
				ascend(ctx.preIncrementExpression(), ctx);
			else if (ctx.preDecrementExpression() != null)
				ascend(ctx.preDecrementExpression(), ctx);
			else if (ctx.unaryExpression() != null)
				push(new ExpressionTypes.UnaryOperation(ctx, getOperator(ctx), true, pop(ctx.unaryExpression())));
			else
				ascend(ctx.unaryExpressionNotPlusMinus(), ctx);
		}

		private String getOperator(UnaryExpressionContext ctx) {
			return ((TerminalNode) ctx.getChild(0)).getText();
		}

		@Override
		public void exitUnaryExpressionNotPlusMinus(UnaryExpressionNotPlusMinusContext ctx) {
			if (ctx.postfixExpression() != null)
				ascend(ctx.postfixExpression(), ctx);
			else if (ctx.castExpression() != null)
				ascend(ctx.castExpression(), ctx);
			else
				push(new ExpressionTypes.UnaryOperation(ctx, getOperator(ctx), true, pop(ctx.unaryExpression())));
		}

		private String getOperator(UnaryExpressionNotPlusMinusContext ctx) {
			return ctx.getChild(0).getText();
		}

		@Override
		public void exitPostfixExpression(PostfixExpressionContext ctx) {
			QPPExpression operand;
			if (ctx.primary() != null)
				operand = pop(ctx.primary());
			else
				operand = pop(ctx.expressionName());
			QPPExpression result = operand;
			int inc = 0, dec = 0, u = 0;
			for (int i = 1; i < ctx.getChildCount(); i++) {
				ParseTree child = ctx.getChild(i);
				if (inc < ctx.postIncrementExpression_lf_postfixExpression().size()
					&& child == ctx.postIncrementExpression_lf_postfixExpression(inc)) {
					result = new ExpressionTypes.UnaryOperation(ctx, child.getText(), false, result);
					inc++;
				} else if (dec < ctx.postDecrementExpression_lf_postfixExpression().size()
					&& child == ctx.postDecrementExpression_lf_postfixExpression(dec)) {
					result = new ExpressionTypes.UnaryOperation(ctx, child.getText(), false, result);
					dec++;
				} else if (u < ctx.unitName().size() && child == ctx.unitName(u)) {
					result = new ExpressionTypes.UnitValue(ctx, result, child.getText());
					u++;
				}
			}
			push(result, ctx);
		}

		@Override
		public void exitPreIncrementExpression(PreIncrementExpressionContext ctx) {
			push(new ExpressionTypes.UnaryOperation(ctx, "++", true, pop(ctx.unaryExpression())));
		}

		@Override
		public void exitPostIncrementExpression(PostIncrementExpressionContext ctx) {
			push(new ExpressionTypes.UnaryOperation(ctx, "++", false, pop(ctx.postfixExpression())));
		}

		@Override
		public void exitPreDecrementExpression(PreDecrementExpressionContext ctx) {
			push(new ExpressionTypes.UnaryOperation(ctx, "--", true, pop(ctx.unaryExpression())));
		}

		@Override
		public void exitPostDecrementExpression(PostDecrementExpressionContext ctx) {
			push(new ExpressionTypes.UnaryOperation(ctx, "--", false, pop(ctx.postfixExpression())));
		}

		@Override
		public void exitPrimary(PrimaryContext ctx) {
			QPPExpression init;
			if (ctx.primaryNoNewArray_lfno_primary() != null)
				init = pop(ctx.primaryNoNewArray_lfno_primary());
			else
				init = pop(ctx.arrayCreationExpression());
			for (PrimaryNoNewArray_lf_primaryContext mod : ctx.primaryNoNewArray_lf_primary()) {
				init = modify(init, mod);
			}
			push(init, ctx);
		}

		private QPPExpression modify(QPPExpression init, PrimaryNoNewArray_lf_primaryContext mod) {
			if (mod.fieldAccess_lf_primary() != null)
				return new ExpressionTypes.FieldAccess(mod, init, mod.fieldAccess_lf_primary().Identifier().getText());
			else if (mod.arrayAccess_lf_primary() != null)
				return modify(init, mod.arrayAccess_lf_primary());
			else if (mod.methodInvocation_lf_primary() != null)
				return new ExpressionTypes.MethodInvocation(mod, init, mod.methodInvocation_lf_primary().Identifier().getText(),
					parseTypeArguments(mod.methodInvocation_lf_primary().typeArguments()),
					parseArguments(mod.methodInvocation_lf_primary().argumentList()));
			else
				throw new IllegalStateException("Unrecognized type of " + mod.getClass().getSimpleName() + " modifier");
		}

		private QPPExpression modify(QPPExpression init, ArrayAccess_lf_primaryContext mod) {
			init = modify(init, mod.primaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primary());
			init = new ExpressionTypes.ArrayAccess(mod, init, pop(mod.expression(0)));
			for (int i = 1; i < mod.expression().size(); i++)
				init = new ExpressionTypes.ArrayAccess(mod.primaryNoNewArray_lf_primary_lf_arrayAccess_lf_primary(i - 1), init,
					pop(mod.expression(i)));
			return init;
		}

		private QPPExpression modify(QPPExpression init, PrimaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primaryContext mod) {
			if (mod.fieldAccess_lf_primary() != null)
				return new ExpressionTypes.FieldAccess(mod, init, mod.fieldAccess_lf_primary().Identifier().getText());
			else if (mod.methodInvocation_lf_primary() != null)
				return new ExpressionTypes.MethodInvocation(mod, init, mod.methodInvocation_lf_primary().Identifier().getText(),
					parseTypeArguments(mod.methodInvocation_lf_primary().typeArguments()),
					parseArguments(mod.methodInvocation_lf_primary().argumentList()));
			else
				throw new IllegalStateException("Unrecognized type of " + mod.getClass().getSimpleName() + " modifier");
		}

		private ExpressionTypes.Type typeFor(QPPExpression expr) {
			if (expr instanceof ExpressionTypes.Type)
				return (ExpressionTypes.Type) expr;
			else
				return new ExpressionTypes.ClassType(expr.getContext(), ((ExpressionTypes.QualifiedName) expr).print(), null);
		}

		@Override
		public void exitPrimaryNoNewArray_lfno_primary(PrimaryNoNewArray_lfno_primaryContext ctx) {
			if (ctx.literal() != null)
				ascend(ctx.literal(), ctx);
			else if (ctx.placeholder() != null)
				ascend(ctx.placeholder(), ctx);
			else if (ctx.expression() != null)
				push(new ExpressionTypes.Parenthetic(ctx, pop(ctx.expression())));
			else if (ctx.arrayAccess_lfno_primary() != null)
				ascend(ctx.arrayAccess_lfno_primary(), ctx);
			else if (ctx.methodInvocation_lfno_primary() != null)
				ascend(ctx.methodInvocation_lfno_primary(), ctx);
			else {
				// typeName ('[' ']')* '.' 'class'
				// unannPrimitiveType ('[' ']')* '.' 'class'
				// 'void' '.' 'class'
				int dims = 0;
				for (int i = 0; i < ctx.getChildCount(); i++) {
					if (ctx.getChild(i).getText().equals("["))
						dims++;
				}
				if (dims > 0) {
					if (ctx.typeName() != null)
						push(new ExpressionTypes.ArrayType(ctx, typeFor(pop(ctx.typeName())), dims));
					else
						push(new ExpressionTypes.ArrayType(ctx, (ExpressionTypes.Type) pop(ctx.unannPrimitiveType()), dims));
				} else
					push(new ExpressionTypes.PrimitiveType(ctx, void.class));
			}
		}

		@Override
		public void exitPrimaryNoNewArray_lfno_arrayAccess(PrimaryNoNewArray_lfno_arrayAccessContext ctx) {
			if (ctx.literal() != null)
				ascend(ctx.literal(), ctx);
			else if (ctx.placeholder() != null)
				ascend(ctx.placeholder(), ctx);
			else if (ctx.expression() != null)
				push(new ExpressionTypes.Parenthetic(ctx, pop(ctx.expression())));
			else if (ctx.classInstanceCreationExpression() != null)
				ascend(ctx.classInstanceCreationExpression(), ctx);
			else if (ctx.fieldAccess() != null)
				ascend(ctx.fieldAccess(), ctx);
			else if (ctx.methodInvocation() != null)
				ascend(ctx.methodInvocation(), ctx);
			else {
				// typeName ('[' ']')* '.' 'class'
				// 'void' '.' 'class'
				int dims = 0;
				for (int i = 0; i < ctx.getChildCount(); i++) {
					if (ctx.getChild(i).getText().equals("["))
						dims++;
				}
				if (dims > 0)
					push(new ExpressionTypes.ArrayType(ctx, typeFor(pop(ctx.typeName())), dims));
				else
					push(new ExpressionTypes.PrimitiveType(ctx, void.class));
			}
		}

		@Override
		public void exitPrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary(
			PrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primaryContext ctx) {
			if (ctx.literal() != null)
				ascend(ctx.literal(), ctx);
			else if (ctx.placeholder() != null)
				ascend(ctx.placeholder(), ctx);
			else if (ctx.expression() != null)
				push(new ExpressionTypes.Parenthetic(ctx, pop(ctx.expression())));
			else if (ctx.methodInvocation_lfno_primary() != null)
				ascend(ctx.methodInvocation_lfno_primary(), ctx);
			else {
				// typeName ('[' ']')* '.' 'class'
				// unannPrimitiveType ('[' ']')* '.' 'class'
				// 'void' '.' 'class'
				int dims = 0;
				for (int i = 0; i < ctx.getChildCount(); i++) {
					if (ctx.getChild(i).getText().equals("["))
						dims++;
				}
				if (dims > 0) {
					if (ctx.typeName() != null)
						push(new ExpressionTypes.ArrayType(ctx, typeFor(pop(ctx.typeName())), dims));
					else
						push(new ExpressionTypes.ArrayType(ctx, (ExpressionTypes.Type) pop(ctx.unannPrimitiveType()), dims));
				}
					push(new ExpressionTypes.PrimitiveType(ctx, void.class));
			}
		}

		@Override
		public void exitArrayCreationExpression(ArrayCreationExpressionContext ctx) {
			ExpressionTypes.Type type = (ExpressionTypes.Type) (ctx.primitiveType() != null ? pop(ctx.primitiveType())
				: pop(ctx.classType()));
			int dims = dimCount(ctx.dims());
			if (ctx.dimExprs() != null) {
				dims += ctx.dimExprs().dimExpr().size();
				List<QPPExpression> sizes = ctx.dimExprs().dimExpr().stream().map(dimExpr -> pop(dimExpr.expression()))
					.collect(Collectors.toList());
				push(new ExpressionTypes.ArrayInitializer(ctx, type, dims, sizes, null));
			} else {
				List<QPPExpression> elements = ctx.arrayInitializer().variableInitializerList().variableInitializer().stream()
					.map(varInit -> pop(varInit.expression())).collect(Collectors.toList());
				push(new ExpressionTypes.ArrayInitializer(ctx, type, dims, null, elements));
			}
		}

		private int dimCount(DimsContext dims) {
			int dim = 0;
			for (int i = 0; i < dims.getChildCount(); i++)
				if (dims.getChild(i).getText().equals("["))
					dim++;
			return dim;
		}

		@Override
		public void exitExpression(ExpressionContext ctx) {
			ascend(ctx.assignmentExpression(), ctx);
		}

		@Override
		public void exitConstantExpression(ConstantExpressionContext ctx) {
			ascend(ctx.expression(), ctx);
		}

		@Override
		public void exitMethodInvocation(MethodInvocationContext ctx) {
			push(methodFor(ctx, c -> c.argumentList(), c -> c.typeArguments(), c -> c.methodName(), c -> c.Identifier(), c -> c.typeName(),
				c -> c.expressionName(), c -> c.primary()));
		}

		@Override
		public void exitMethodInvocation_lf_primary(MethodInvocation_lf_primaryContext ctx) {
			push(methodFor(ctx, c -> c.argumentList(), c -> c.typeArguments(), null, c -> c.Identifier(), null, null, null));
		}

		@Override
		public void exitMethodInvocation_lfno_primary(MethodInvocation_lfno_primaryContext ctx) {
			push(methodFor(ctx, //
				c -> c.argumentList(), c -> c.typeArguments(), c -> c.methodName(), c -> c.Identifier(), c -> c.typeName(),
				c -> c.expressionName(), null));
		}

		private <C extends ParserRuleContext> ExpressionTypes.MethodInvocation methodFor(C ctx,
			Function<C, ArgumentListContext> argumentList, Function<C, TypeArgumentsContext> typeArguments,
			Function<C, MethodNameContext> methodName, Function<C, TerminalNode> identifier, Function<C, TypeNameContext> typeName,
			Function<C, ExpressionNameContext> expressionName, Function<C, PrimaryContext> primary) {
			ExpressionTypes.MethodInvocation exp;
			List<QPPExpression> args = parseArguments(argumentList.apply(ctx));
			List<ExpressionTypes.Type> typeArgs = typeArguments == null ? null : parseTypeArguments(typeArguments.apply(ctx));
			if (methodName != null && methodName.apply(ctx) != null) {
				exp = new ExpressionTypes.MethodInvocation(ctx, null, methodName.apply(ctx).getText(), typeArgs, args);
			} else if (typeName != null && typeName.apply(ctx) != null) {
				exp = new ExpressionTypes.MethodInvocation(ctx, pop(typeName.apply(ctx)), identifier.apply(ctx).getText(), typeArgs, args);
			} else if (expressionName != null && expressionName.apply(ctx) != null) {
				exp = new ExpressionTypes.MethodInvocation(ctx, pop(expressionName.apply(ctx)), identifier.apply(ctx).getText(), typeArgs,
					args);
			} else if (primary != null && primary.apply(ctx) != null) {
				exp = new ExpressionTypes.MethodInvocation(ctx, pop(primary.apply(ctx)), identifier.apply(ctx).getText(), typeArgs, args);
			} else {
				exp = new ExpressionTypes.MethodInvocation(ctx, null, identifier.apply(ctx).getText(), typeArgs, args);
			}
			return exp;
		}

		List<QPPExpression> parseArguments(ArgumentListContext argumentList) {
			return argumentList == null ? Collections.emptyList()
				: argumentList.expression().stream().map(x -> pop(x)).collect(Collectors.toList());
		}

		List<ExpressionTypes.Type> parseTypeArguments(TypeArgumentsContext typeArguments) {
			return typeArguments == null ? null : typeArguments.typeArgumentList().typeArgument().stream()
				.map(a -> (ExpressionTypes.Type) pop(a)).collect(Collectors.toList());
		}

		@Override
		public void exitTypeName(TypeNameContext ctx) {
			if (ctx.packageOrTypeName() != null)
				push(new ExpressionTypes.QualifiedName(ctx, (ExpressionTypes.QualifiedName) pop(ctx.packageOrTypeName()),
					ctx.Identifier().getText()));
			else
				push(new ExpressionTypes.QualifiedName(ctx, null, ctx.Identifier().getText()));
		}

		@Override
		public void exitPackageOrTypeName(PackageOrTypeNameContext ctx) {
			if (ctx.packageOrTypeName() != null)
				push(new ExpressionTypes.QualifiedName(ctx, (ExpressionTypes.QualifiedName) pop(ctx.packageOrTypeName()),
					ctx.Identifier().getText()));
			else
				push(new ExpressionTypes.QualifiedName(ctx, null, ctx.Identifier().getText()));
		}

		@Override
		public void exitType(TypeContext ctx) {
			if (ctx.primitiveType() != null)
				ascend(ctx.primitiveType(), ctx);
			else
				ascend(ctx.referenceType(), ctx);
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
			push(new ExpressionTypes.ClassType(ctx, ctx.Identifier().getText(), parseTypeArguments(ctx.typeArguments())));
		}

		@Override
		public void exitIntegralType(IntegralTypeContext ctx) {
			Class<?> type;
			switch (ctx.getText()) {
			case "byte":
				type = Byte.TYPE;
				break;
			case "short":
				type = Short.TYPE;
				break;
			case "int":
				type = Integer.TYPE;
				break;
			case "long":
				type = Long.TYPE;
				break;
			case "char":
				type = Character.TYPE;
				break;
			default:
				throw new IllegalStateException("Unrecognized integral type: " + ctx.getText());
			}
			push(new ExpressionTypes.PrimitiveType(ctx, type));
		}

		@Override
		public void exitFloatingPointType(FloatingPointTypeContext ctx) {
			Class<?> type;
			switch (ctx.getText()) {
			case "float":
				type = Float.TYPE;
				break;
			case "double":
				type = Double.TYPE;
				break;
			default:
				throw new IllegalStateException("Unrecognized floating point type: " + ctx.getText());
			}
			push(new ExpressionTypes.PrimitiveType(ctx, type));
		}

		@Override
		public void exitPrimitiveType(PrimitiveTypeContext ctx) {
			if (ctx.numericType() != null)
				ascend(ctx.numericType(), ctx);
			else
				push(new ExpressionTypes.PrimitiveType(ctx, Boolean.TYPE));
		}

		@Override
		public void exitUnannPrimitiveType(UnannPrimitiveTypeContext ctx) {
			if (ctx.numericType() != null)
				ascend(ctx.numericType(), ctx);
			else
				push(new ExpressionTypes.PrimitiveType(ctx, Boolean.TYPE));
		}

		@Override
		public void exitNumericType(NumericTypeContext ctx) {
			if (ctx.integralType() != null)
				ascend(ctx.integralType(), ctx);
			else if (ctx.floatingPointType() != null)
				ascend(ctx.floatingPointType(), ctx);
			else
				throw new IllegalStateException();
		}

		@Override
		public void exitArrayType(ArrayTypeContext ctx) {
			int dim = dimCount(ctx.dims());
			QPPExpression type;
			if (ctx.primitiveType() != null)
				type = pop(ctx.primitiveType());
			else if (ctx.classType() != null)
				type = pop(ctx.classType());
			else
				throw new IllegalStateException();
			push(new ExpressionTypes.ArrayType(ctx, (ExpressionTypes.Type) type, dim));
		}

		@Override
		public void exitPlaceholder(PlaceholderContext ctx) {
			push(new ExpressionTypes.Placeholder(ctx, ctx.IntegerLiteral().getText()));
		}
	}
}
