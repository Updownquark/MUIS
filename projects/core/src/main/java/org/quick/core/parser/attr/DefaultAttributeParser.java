package org.quick.core.parser.attr;

import java.awt.Color;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.observe.ObservableValue;
import org.quick.core.QuickEnvironment;
import org.quick.core.parser.QuickAttributeParser;
import org.quick.core.parser.attr.QuickAttrParser.ConstantExpressionContext;
import org.quick.core.prop.ExpressionContext;
import org.quick.core.prop.QuickProperty;
import org.quick.core.prop.QuickPropertyType;

import com.google.common.reflect.TypeToken;

public class DefaultAttributeParser implements QuickAttributeParser {
	private final QuickEnvironment theEnvironment;

	public DefaultAttributeParser(QuickEnvironment env) {
		theEnvironment = env;
	}

	@Override
	public QuickEnvironment getEnvironment() {
		return theEnvironment;
	}

	@Override
	public <T> ObservableValue<T> parseProperty(QuickPropertyType<T> type, ExpressionContext ctx, String value) {
		ANTLRInputStream in = new ANTLRInputStream(value);
		QuickAttrLexer lexer = new QuickAttrLexer(in);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		QuickAttrParser parser = new QuickAttrParser(tokens);
		ParseTreeWalker walker = new ParseTreeWalker();
		ConstantExpressionContext ctx = parser.constantExpression();
		walker.walk(new ParseTreeListener() {
			@Override
			public void enterEveryRule(ParserRuleContext arg0) {
				System.out.println("Enter " + QuickAttrParser.ruleNames[arg0.getRuleIndex()] + ": " + arg0.getText());
			}

			@Override
			public void exitEveryRule(ParserRuleContext arg0) {
				System.out.println("Exit " + QuickAttrParser.ruleNames[arg0.getRuleIndex()] + ": " + arg0.getText());
			}

			@Override
			public void visitErrorNode(ErrorNode arg0) {
				System.out.println("Error " + arg0.getText());
			}

			@Override
			public void visitTerminal(TerminalNode arg0) {
				System.out.println("Terminal " + QuickAttrParser.ruleNames[arg0.getSymbol().getTokenIndex()] + ": " + arg0.getText());
			}
		}, ctx);
		// ParseTree tree = parser.compilationUnit();
		// TODO Auto-generated method stub
		return null;
	}

	public static void main(String[] args) {
		DefaultAttributeParser parser = new DefaultAttributeParser(null);
		QuickPropertyType<Color> type = QuickPropertyType.build(TypeToken.of(Color.class)).withValues(new QuickProperty.ColorValueSupply()).build();
		parser.parseProperty(type, "rgb(245+10, 0, 255)");
		// parser.parseProperty(type, "green");
	}
}
