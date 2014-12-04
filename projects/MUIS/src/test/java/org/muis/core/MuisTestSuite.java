package org.muis.core;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.muis.core.rx.ObservableTest;
import org.muis.core.style.StylesTest;

/** Runs all MUIS unit tests */
@RunWith(Suite.class)
@Suite.SuiteClasses({ObservableTest.class, StylesTest.class})
public class MuisTestSuite {
}
