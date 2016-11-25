package org.quick.core.model;

import org.junit.Test;
import org.qommons.Transaction;
import org.quick.core.QuickDocument;
import org.quick.core.QuickTextElement;

import com.google.common.reflect.TypeToken;

public class QuickDocumentTest {
	@Test
	public void testSimpleDocModel() throws Exception {
		org.observe.SimpleSettableValue<QuickDocumentModel> docModelObs = new org.observe.SimpleSettableValue<>(
			TypeToken.of(QuickDocumentModel.class), true);
		QuickDocumentModel flatDoc=QuickDocumentModel.flatten(docModelObs);

		org.observe.ObservableTester changes = new org.observe.ObservableTester(flatDoc.changes());

		changes.checkOps(0);

		QuickDocument doc = org.quick.QuickTestUtils.parseDoc(QuickDocumentTest.class.getResource("quickDocTest.qml"));
		QuickTextElement textEl = (QuickTextElement) doc.getRoot().getPhysicalChildren().get(0);
		SimpleDocumentModel simpleDoc = new SimpleDocumentModel(textEl);

		changes.checkOps(0, 1); // Could be zero events since the doc is empty, but could be one an that's ok

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
		changes.checkOps(1);
	}
}
