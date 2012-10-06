package org.muis.core.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import org.jdom2.Element;
import org.muis.core.MuisClassView;
import org.muis.core.mgr.MuisMessageCenter;
import org.muis.core.style.sheet.StyleSheet;

import prisms.arch.PrismsConfig;
import prisms.lang.*;

/** Parses style sheets using the default style syntax */
public class DefaultStyleSheetParser implements StyleSheetParser {
	public static class ParsedAnimation extends ParsedItem {
		@Override
		public ParsedItem [] getDependents() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public static class ParsedType extends ParsedItem {
		@Override
		public ParsedItem [] getDependents() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}
	}

	public static class ParsedSection extends ParsedItem {
		@Override
		public ParsedItem [] getDependents() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}
	}

	public static class ParsedTypeSet extends ParsedItem {
		@Override
		public ParsedItem [] getDependents() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}
	}

	public static class ParsedGroupSet extends ParsedItem {
		@Override
		public ParsedItem [] getDependents() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}
	}

	public static class ParsedState extends ParsedItem {
		@Override
		public ParsedItem [] getDependents() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}
	}

	public static class ParsedAssignment extends ParsedItem {
		@Override
		public ParsedItem [] getDependents() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}
	}

	public static class ParsedComnment extends ParsedItem {
		@Override
		public ParsedItem [] getDependents() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public EvaluationResult evaluate(EvaluationEnvironment env, boolean asType, boolean withValues) throws EvaluationException {
			return null;
		}
	}

	private prisms.lang.PrismsParser theParser;

	public DefaultStyleSheetParser() {
		theParser = new prisms.lang.PrismsParser();
		try {
			theParser.configure(getStyleSheetDefConfig());
		} catch(MuisParseException | IOException e) {
			throw new IllegalStateException("Could not parse and read MSS.xml", e);
		}
	}

	@Override
	public StyleSheet parseStyleSheet(java.net.URL location, Reader reader, MuisClassView classView, MuisMessageCenter messager)
		throws IOException, MuisParseException {
		MSS mss=new MSS(;
		StringBuilder content = new StringBuilder();
		int read = reader.read();
		while(read >= 0) {
			content.append((char) read);
			read = reader.read();
		}
		prisms.lang.ParseMatch[] matches;
		try {
			matches = theParser.parseMatches(content.toString());
		} catch(ParseException e) {
			throw new MuisParseException(e.toString(), e);
		}
		return null;
	}

	private static PrismsConfig getStyleSheetDefConfig() throws IOException, MuisParseException {
		return getConfig(getStyleSheetDefXml());
	}

	private static PrismsConfig getConfig(Element element) {
		return new PrismsConfig.DefaultPrismsConfig(element.getName(), null, getSubConfigs(element));
	}

	private static PrismsConfig [] getSubConfigs(Element element) {
		ArrayList<PrismsConfig> ret = new ArrayList<>();
		for(org.jdom2.Attribute att : element.getAttributes()) {
			ret.add(new PrismsConfig.DefaultPrismsConfig(att.getName(), att.getValue(), new PrismsConfig[0]));
		}
		for(Element el : element.getChildren()) {
			ret.add(getConfig(el));
		}
		return ret.toArray(new PrismsConfig[ret.size()]);
	}

	private static Element getStyleSheetDefXml() throws IOException, MuisParseException {
		try {
			return new org.jdom2.input.SAXBuilder().build(
				new java.io.InputStreamReader(DefaultStyleSheetParser.class.getResourceAsStream("MSS.xml"))).getRootElement();
		} catch(org.jdom2.JDOMException e) {
			throw new MuisParseException("Could not parse MSS.xml", e);
		}
	}
}