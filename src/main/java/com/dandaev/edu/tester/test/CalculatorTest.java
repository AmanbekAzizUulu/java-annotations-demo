package com.dandaev.edu.tester.test;

import com.dandaev.edu.annotations.testing.framework.AfterAll;
import com.dandaev.edu.annotations.testing.framework.AfterEach;
import com.dandaev.edu.annotations.testing.framework.BeforeAll;
import com.dandaev.edu.annotations.testing.framework.BeforeEach;
import com.dandaev.edu.annotations.testing.framework.Test;
import com.dandaev.edu.entities.Calculator;

public class CalculatorTest {
	private Calculator calculator;

	@BeforeAll
	public static void setUpClass() {
		System.out.println("Setting up test class...");
	}

	@AfterAll
	public static void tearDownClass() {
		System.out.println("Tearing down test class...");
	}

	@BeforeEach
	public void setUp() {
		calculator = new Calculator();
		System.out.println("Setting up test...");
	}

	@AfterEach
	public void tearDown() {
		System.out.println("Tearing down test...");
	}

	@Test
	public void testAddition() {
		int result = calculator.add(2, 3);
		if (result != 5) {
			throw new AssertionError("Expected 5, but got " + result);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDivisionByZero() {
		calculator.divide(10, 0);
	}

	@Test(timeout = 1000L)
	public void testPerformance() {
		// Длительная операция
		long sum = 0;
		for (int i = 0; i < 1000000; i++) {
			sum += i;
		}
	}

	@Test
	public void testFailingTest() {
		throw new RuntimeException("This test is designed to fail");
	}
}
