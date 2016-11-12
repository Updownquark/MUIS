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

public class AntlrPropertyParser extends AbstractPropertyParser {
	public AntlrPropertyParser(QuickEnvironment env) {
		super(env);
	}

	@Override
	protected <T> ObservableValue<?> parseDefaultValue(QuickParseEnv parseEnv, TypeToken<T> type, String value) throws QuickParseException {
		QPPExpression<?> parsed = compile(value);
		searchForErrors(parsed);
		boolean action = TypeToken.of(ObservableAction.class).isAssignableFrom(type);
		return AntlrPropertyEvaluator.evaluateTypeless(parseEnv, type, parsed, action, action);
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
			throw new QuickParseException("Parsing failed for " + expression, e);
		} catch (IllegalStateException e) {
			throw new QuickParseException("Parsing failed for " + expression, e);
		}
	}

	private void searchForErrors(QPPExpression<?> expr) throws QuickParseException {
		// TODO
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

			System.out.println(parser.compile("10px"));
			System.out.println(parser.compile("10%"));
		} catch (QuickParseException e) {
			e.printStackTrace();
		}
	}

	private static class QPPCompiler extends QPPBaseListener {
		private final Map<ParserRuleContext, QPPExpression<?>> theDanglingExpressions;

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

		private void push(QPPExpression<?> expression, ParserRuleContext ctx) {
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

		private QPPExpression<?> pop(ParserRuleContext ctx) {
			if (ctx == null)
				throw new NullPointerException();
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
			throw new IllegalStateException("Unexpected token ( " + arg0.getSymbol().getText() + ", type "
				+ QPPParser.tokenNames[arg0.getSymbol().getType()] + " ) at position " + arg0.getSymbol().getStartIndex());
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
				throw new IllegalStateException("Unrecognized literal type: " + QPPParser.tokenNames[ctx.start.getType()] + " ("
					+ ctx.start.getType() + ") at position " + ctx.start.getStartIndex());
			}
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
			if (ctx.primary() != null)
				push(
					new ExpressionTypes.QualifiedName(ctx, (ExpressionTypes.QualifiedName) pop(ctx.primary()), ctx.Identifier().getText()));
			else if (ctx.typeName() != null)
				push(new ExpressionTypes.QualifiedName(ctx, (ExpressionTypes.QualifiedName) pop(ctx.typeName()),
					"super." + ctx.Identifier().getText()));
			else
				push(new ExpressionTypes.QualifiedName(ctx, null, "super." + ctx.Identifier().getText()));
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
				push(new ExpressionTypes.BinaryOperation(ctx, "&&", pop(ctx.conditionalAndExpression()),
					pop(ctx.inclusiveOrExpression())));
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
				push(new ExpressionTypes.BinaryOperation(ctx, getOperator(ctx), pop(ctx.shiftExpression()),
					pop(ctx.additiveExpression())));
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
			QPPExpression<?> operand;
			if(ctx.primary()!=null)
				operand = pop(ctx.primary());
			else
				operand = pop(ctx.expressionName());
			QPPExpression<?> result = operand;
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
			push(new ExpressionTypes.UnaryOperation(ctx, "--", true, pop(ctx.unaryExpression())));
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
		public void exitPrimaryNoNewArray(PrimaryNoNewArrayContext ctx) {
			if (ctx.literal() != null)
				ascend(ctx.literal(), ctx);
			else if (ctx.placeholder() != null)
				ascend(ctx.placeholder(), ctx);
			// typeName ('[' ']')* '.' 'class'
			// 'void' '.' 'class'
			// 'this'
			// typeName '.' 'this'
			else if (ctx.expression() != null)
				push(new ExpressionTypes.Parenthetic(ctx, pop(ctx.expression())));
			else if (ctx.classInstanceCreationExpression() != null)
				ascend(ctx.classInstanceCreationExpression(), ctx);
			else if (ctx.fieldAccess() != null)
				ascend(ctx.fieldAccess(), ctx);
			else if (ctx.arrayAccess() != null)
				ascend(ctx.arrayAccess(), ctx);
			else if (ctx.methodInvocation() != null)
				ascend(ctx.methodInvocation(), ctx);
			else
				super.exitPrimaryNoNewArray(ctx); // TODO Partial implementation
		}

		@Override
		public void exitPrimaryNoNewArray_lf_primary(PrimaryNoNewArray_lf_primaryContext ctx) {
			if (ctx.classInstanceCreationExpression_lf_primary() != null)
				ascend(ctx.classInstanceCreationExpression_lf_primary(), ctx);
			else if (ctx.fieldAccess_lf_primary() != null)
				ascend(ctx.fieldAccess_lf_primary(), ctx);
			else if (ctx.arrayAccess_lf_primary() != null)
				ascend(ctx.arrayAccess_lf_primary(), ctx);
			else if (ctx.methodInvocation_lf_primary() != null)
				ascend(ctx.methodInvocation_lf_primary(), ctx);
			else
				super.exitPrimaryNoNewArray_lf_primary(ctx);
		}

		@Override
		public void exitPrimaryNoNewArray_lfno_primary(PrimaryNoNewArray_lfno_primaryContext ctx) {
			if (ctx.literal() != null)
				ascend(ctx.literal(), ctx);
			else if (ctx.placeholder() != null)
				ascend(ctx.placeholder(), ctx);
			// typeName ('[' ']')* '.' 'class'
			// unannPrimitiveType ('[' ']')* '.' 'class'
			// 'void' '.' 'class'
			// 'this'
			// typeName '.' 'this'
			else if (ctx.expression() != null)
				push(new ExpressionTypes.Parenthetic(ctx, pop(ctx.expression())));
			else if (ctx.classInstanceCreationExpression_lfno_primary() != null)
				ascend(ctx.classInstanceCreationExpression_lfno_primary(), ctx);
			else if (ctx.fieldAccess_lfno_primary() != null)
				ascend(ctx.fieldAccess_lfno_primary(), ctx);
			else if (ctx.arrayAccess_lfno_primary() != null)
				ascend(ctx.arrayAccess_lfno_primary(), ctx);
			else if (ctx.methodInvocation_lfno_primary() != null)
				ascend(ctx.methodInvocation_lfno_primary(), ctx);
			else
				super.exitPrimaryNoNewArray_lfno_primary(ctx); // TODO Partial implementation
		}

		@Override
		public void exitPrimaryNoNewArray_lf_arrayAccess(PrimaryNoNewArray_lf_arrayAccessContext ctx) {
			// TODO What do I do here? No contents
			super.exitPrimaryNoNewArray_lf_arrayAccess(ctx);
		}

		@Override
		public void exitPrimaryNoNewArray_lfno_arrayAccess(PrimaryNoNewArray_lfno_arrayAccessContext ctx) {
			if (ctx.literal() != null)
				ascend(ctx.literal(), ctx);
			else if (ctx.placeholder() != null)
				ascend(ctx.placeholder(), ctx);
			// typeName ('[' ']')* '.' 'class'
			// 'void' '.' 'class'
			// 'this'
			// typeName '.' 'this'
			else if (ctx.expression() != null)
				push(new ExpressionTypes.Parenthetic(ctx, pop(ctx.expression())));
			else if (ctx.classInstanceCreationExpression() != null)
				ascend(ctx.classInstanceCreationExpression(), ctx);
			else if (ctx.fieldAccess() != null)
				ascend(ctx.fieldAccess(), ctx);
			else if (ctx.methodInvocation() != null)
				ascend(ctx.methodInvocation(), ctx);
			else
				super.exitPrimaryNoNewArray_lfno_arrayAccess(ctx); // TODO Partial implementation
		}

		@Override
		public void exitPrimaryNoNewArray_lf_primary_lf_arrayAccess_lf_primary(
			PrimaryNoNewArray_lf_primary_lf_arrayAccess_lf_primaryContext ctx) {
			// TODO What do I do here? No contents
			super.exitPrimaryNoNewArray_lf_primary_lf_arrayAccess_lf_primary(ctx);
		}

		@Override
		public void exitPrimaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primary(
			PrimaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primaryContext ctx) {
			if (ctx.classInstanceCreationExpression_lf_primary() != null)
				ascend(ctx.classInstanceCreationExpression_lf_primary(), ctx);
			else if (ctx.fieldAccess_lf_primary() != null)
				ascend(ctx.fieldAccess_lf_primary(), ctx);
			else if (ctx.methodInvocation_lf_primary() != null)
				ascend(ctx.methodInvocation_lf_primary(), ctx);
			else
				super.exitPrimaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primary(ctx); // TODO Partial implementation
		}

		@Override
		public void exitPrimaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primary(
			PrimaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primaryContext ctx) {
			// TODO What do I do here? No contents
			super.exitPrimaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primary(ctx);
		}

		@Override
		public void exitPrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary(
			PrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primaryContext ctx) {
			if (ctx.literal() != null)
				ascend(ctx.literal(), ctx);
			else if (ctx.placeholder() != null)
				ascend(ctx.placeholder(), ctx);
			// typeName ('[' ']')* '.' 'class'
			// unannPrimitiveType ('[' ']')* '.' 'class'
			// 'void' '.' 'class'
			// 'this'
			// typeName '.' 'this'
			else if (ctx.expression() != null)
				push(new ExpressionTypes.Parenthetic(ctx, pop(ctx.expression())));
			else if (ctx.classInstanceCreationExpression_lfno_primary() != null)
				ascend(ctx.classInstanceCreationExpression_lfno_primary(), ctx);
			else if (ctx.fieldAccess_lfno_primary() != null)
				ascend(ctx.fieldAccess_lfno_primary(), ctx);
			else if (ctx.methodInvocation_lfno_primary() != null)
				ascend(ctx.methodInvocation_lfno_primary(), ctx);
			else
				super.exitPrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary(ctx); // TODO Partial implementation
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
			ExpressionTypes.MethodInvocation exp;
			List<QPPExpression<?>> args = parseArguments(argumentList.apply(ctx));
			List<ExpressionTypes.Type<?>> typeArgs = typeArguments == null ? null : parseTypeArguments(typeArguments.apply(ctx));
			if (methodName != null && methodName.apply(ctx) != null) {
				exp = new ExpressionTypes.MethodInvocation(ctx, null, isSuper, methodName.apply(ctx).getText(), typeArgs, args);
			} else if (typeName != null && typeName.apply(ctx) != null) {
				exp = new ExpressionTypes.MethodInvocation(ctx, pop(typeName.apply(ctx)), isSuper, identifier.apply(ctx).getText(),
					typeArgs, args);
			} else if (expressionName != null && expressionName.apply(ctx) != null) {
				exp = new ExpressionTypes.MethodInvocation(ctx, pop(expressionName.apply(ctx)), isSuper, identifier.apply(ctx).getText(),
					typeArgs, args);
			} else if (primary != null && primary.apply(ctx) != null) {
				exp = new ExpressionTypes.MethodInvocation(ctx, pop(primary.apply(ctx)), isSuper, identifier.apply(ctx).getText(), typeArgs,
					args);
			} else {
				exp = new ExpressionTypes.MethodInvocation(ctx, null, isSuper, identifier.apply(ctx).getText(), typeArgs, args);
			}
			push(exp);
		}

		List<QPPExpression<?>> parseArguments(ArgumentListContext argumentList) {
			return argumentList == null ? Collections.emptyList()
				: argumentList.expression().stream().map(x -> pop(x)).collect(Collectors.toList());
		}

		List<ExpressionTypes.Type<?>> parseTypeArguments(TypeArgumentsContext typeArguments) {
			return typeArguments == null ? null : typeArguments.typeArgumentList().typeArgument().stream()
				.map(a -> (ExpressionTypes.Type<?>) pop(a)).collect(Collectors.toList());
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
		public void exitNumericType(NumericTypeContext ctx) {
			// TODO Auto-generated method stub
			super.exitNumericType(ctx);
		}

		@Override
		public void exitTypeArgumentsOrDiamond(TypeArgumentsOrDiamondContext ctx) {
			// TODO Auto-generated method stub
			super.exitTypeArgumentsOrDiamond(ctx);
		}

		@Override
		public void exitPlaceholder(PlaceholderContext ctx) {
			push(new ExpressionTypes.Placeholder(ctx, ctx.IntegerLiteral().getText()));
		}
	}
}
