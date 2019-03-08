package org.quick.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.quick.PropertyTest;
import org.quick.core.model.QuickDocumentTest;
import org.quick.core.style.StylesTest;
import org.quick.core.util.CompoundListenerTest;
import org.quick.widget.base.layout.LayoutTests;

/** Runs all Quick unit tests */
@RunWith(Suite.class)
@Suite.SuiteClasses({ //
	// ObserveTests.class, //
	PropertyTest.class, //
	StylesTest.class, //
	CompoundListenerTest.class, //
	QuickDocumentTest.class, //
	LayoutTests.class//
})
public class QuickTestSuite {
}
