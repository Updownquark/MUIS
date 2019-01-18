package org.quick.core.model;

import java.io.IOException;

import org.junit.Test;
import org.qommons.Transaction;
import org.quick.core.QuickDocument;
import org.quick.core.QuickTextElement;
import org.quick.core.parser.QuickParseException;

import com.google.common.reflect.TypeToken;

/** Tests for {@link QuickDocumentModel} */
public class QuickDocumentTest {
	/**
	 * Tests {@link QuickDocumentModel#flatten(org.observe.ObservableValue)}
	 *
	 * @throws IOException If the Quick document needed by the test cannot be found or read
	 * @throws QuickParseException If the Quick document needed by the test cannot be parsed
	 */
	@Test
	public void testFlattenedDoc() throws QuickParseException, IOException {
		org.observe.SimpleSettableValue<QuickDocumentModel> docModelObs = new org.observe.SimpleSettableValue<>(
			TypeToken.of(QuickDocumentModel.class), true);
		QuickDocumentModel flatDoc=QuickDocumentModel.flatten(docModelObs);

		org.observe.ObservableTester changes = new org.observe.ObservableTester(flatDoc.changes());

		changes.checkOps(0);

		QuickDocument doc = org.quick.QuickTestUtils.parseDoc(QuickDocumentTest.class.getResource("quickDocTest.qml"));
		QuickTextElement textEl = (QuickTextElement) doc.getRoot().getPhysicalChildren().get(0);
		SimpleDocumentModel simpleDoc = new SimpleDocumentModel(textEl);
		docModelObs.set(simpleDoc, null);

		changes.checkOps(0, 1); // Could be zero events since the doc is empty, but could be one and that's ok

		simpleDoc.append('a');
		changes.checkOps(1);

		simpleDoc.append("bra cadabra");
		changes.checkOps(1);

		simpleDoc.delete(4, simpleDoc.length());
		changes.checkOps(1);

		try (Transaction t = simpleDoc.holdForWrite(null)) {
			simpleDoc.append(" cadabra");
			simpleDoc.delete(0, 5);
		}
		changes.checkOps(2);

		docModelObs.set(simpleDoc, null);
		changes.checkOps(0);

		simpleDoc.insert(0, "abra ");
		changes.checkOps(1);

		SimpleDocumentModel otherDoc = new SimpleDocumentModel(textEl);
		otherDoc.setText("blah blah");
		docModelObs.set(otherDoc, null);
		changes.checkOps(2); // clear and populate

		simpleDoc.clear();
		changes.checkOps(0);

		otherDoc.append(" abra cadabra");
		changes.checkOps(1);
	}
}
