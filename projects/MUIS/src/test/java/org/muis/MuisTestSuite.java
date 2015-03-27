package org.muis;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.muis.core.style.StylesTest;
import org.observe.ObservableTest;

/** Runs all MUIS unit tests */
@RunWith(Suite.class)
@Suite.SuiteClasses({ObservableTest.class, StylesTest.class})
public class MuisTestSuite {
}
