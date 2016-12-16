package org.quick;

import java.io.IOException;
import java.net.URL;

import org.quick.core.QuickClassView;
import org.quick.core.QuickDocument;
import org.quick.core.QuickEnvironment;
import org.quick.core.QuickHeadSection;
import org.quick.core.parser.QuickDocumentStructure;
import org.quick.core.parser.QuickParseException;

/** Utilities for testing Quick */
public class QuickTestUtils {
	/**
	 * A utility method to quickly parse a document XML
	 *
	 * @param url The URL location of the XML document
	 * @return The parsed document
	 * @throws IOException If the resource or others linked to it could not be found or read
	 * @throws QuickParseException If the resource or others linked to it contained fatal errors
	 */
	public static QuickDocument parseDoc(URL url) throws QuickParseException, IOException {
		QuickEnvironment env = QuickEnvironment.build().withDefaults().build();
		env.msg().addListener(msg -> {
			switch (msg.type) {
			case FATAL:
			case ERROR:
			case WARNING:
				throw new IllegalStateException(msg.toString(), msg.exception);
			case INFO:
			case DEBUG:
				System.out.println(msg);
				break;
			}
		});
		QuickDocumentStructure docStruct = env.getDocumentParser().parseDocument(url, new java.io.InputStreamReader(url.openStream()),
			env.cv(), env.msg());
		QuickHeadSection head = env.getContentCreator().createHeadFromStructure(docStruct.getHead(), env.getPropertyParser(), env);
		QuickDocument doc = new QuickDocument(env, docStruct.getLocation(), head, docStruct.getContent().getClassView());
		env.getContentCreator().fillDocument(doc, docStruct.getContent());
		return doc;
	}

	/**
	 * Creates an empty document from scratch
	 * 
	 * @return The new document
	 */
	public static QuickDocument createDocument() {
		QuickEnvironment env = QuickEnvironment.build().withDefaults().build();
		QuickClassView cv = new QuickClassView(env, env.cv(), null);
		QuickHeadSection head = QuickHeadSection.build().build();
		return new QuickDocument(env, null, head, cv);
	}
}
