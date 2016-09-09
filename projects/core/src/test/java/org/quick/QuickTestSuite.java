package org.quick;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.observe.ObservableTest;
import org.quick.core.style.StylesTest;
import org.quick.core.util.CompoundListenerTest;

/** Runs all Quick unit tests */
@RunWith(Suite.class)
@Suite.SuiteClasses({ //
	ObservableTest.class, //
	PropertyTest.class, //
	StylesTest.class, //
	CompoundListenerTest.class//
})
public class QuickTestSuite {
}
