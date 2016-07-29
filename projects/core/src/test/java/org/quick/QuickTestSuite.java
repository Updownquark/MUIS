package org.quick;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.observe.ObservableTest;
import org.quick.core.style.StylesTest;

/** Runs all Quick unit tests */
@RunWith(Suite.class)
@Suite.SuiteClasses({ //
	ObservableTest.class, //
	PropertyTest.class, //
	StylesTest.class//
})
public class QuickTestSuite {
}
