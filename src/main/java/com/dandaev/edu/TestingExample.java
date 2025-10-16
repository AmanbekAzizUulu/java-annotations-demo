package com.dandaev.edu;

import com.dandaev.edu.tester.framework.TestFramework;
import com.dandaev.edu.tester.test.CalculatorTest;

public class TestingExample {
	public static void main(String[] args) throws Exception {
		TestFramework testFramework = new TestFramework();
		testFramework.runTests(CalculatorTest.class);
	}
}
