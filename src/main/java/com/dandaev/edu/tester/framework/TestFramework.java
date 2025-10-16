package com.dandaev.edu.tester.framework;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.dandaev.edu.annotations.testing.framework.AfterAll;
import com.dandaev.edu.annotations.testing.framework.AfterEach;
import com.dandaev.edu.annotations.testing.framework.BeforeAll;
import com.dandaev.edu.annotations.testing.framework.BeforeEach;
import com.dandaev.edu.annotations.testing.framework.Test;

/**
 * Класс {@code TestFramework} реализует простую систему тестирования,
 * аналогичную JUnit, на основе пользовательских аннотаций.
 *
 * Поддерживаемые аннотации:
 * <ul>
 * <li>{@link BeforeAll} — выполняется один раз перед всеми тестами</li>
 * <li>{@link BeforeEach} — выполняется перед каждым тестом</li>
 * <li>{@link Test} — обозначает метод теста</li>
 * <li>{@link AfterEach} — выполняется после каждого теста</li>
 * <li>{@link AfterAll} — выполняется один раз после всех тестов</li>
 * </ul>
 *
 * Поддерживаются также:
 * <ul>
 * <li>Ожидаемое исключение (через параметр {@code expected})</li>
 * <li>Тест с таймаутом (через параметр {@code timeout})</li>
 * </ul>
 *
 * Пример использования:
 *
 * <pre>
 * TestFramework framework = new TestFramework();
 * framework.runTests(MyTestClass.class);
 * </pre>
 */
public class TestFramework {
	private int testsPassed = 0; // Количество успешно пройденных тестов
	private int testsFailed = 0; // Количество проваленных тестов

	/**
	 * Запускает выполнение всех тестов в указанном классе.
	 *
	 * @param testClass класс, содержащий тестовые методы
	 * @throws Exception если при выполнении методов возникают ошибки
	 */
	public void runTests(Class<?> testClass) throws Exception {
		System.out.println("Running tests for: " + testClass.getSimpleName());

		// Создаём экземпляр тестируемого класса
		Object testInstance = testClass.getDeclaredConstructor().newInstance();

		// Выполняем методы, помеченные @BeforeAll
		runAnnotatedMethods(testInstance, BeforeAll.class);

		// Находим все методы с аннотацией @Test
		Method[] methods = testClass.getDeclaredMethods();
		List<Method> testMethods = new ArrayList<>();

		for (Method method : methods) {
			if (method.isAnnotationPresent(Test.class)) {
				testMethods.add(method);
			}
		}

		// Запускаем каждый тест
		for (Method testMethod : testMethods) {
			// Выполняем @BeforeEach перед тестом
			runAnnotatedMethods(testInstance, BeforeEach.class);

			// Выполняем сам тест
			runTest(testInstance, testMethod);

			// Выполняем @AfterEach после теста
			runAnnotatedMethods(testInstance, AfterEach.class);
		}

		// После всех тестов выполняем методы с @AfterAll
		runAnnotatedMethods(testInstance, AfterAll.class);

		// Печатаем сводку
		printResults();
	}

	/**
	 * Выполняет отдельный тестовый метод и обрабатывает возможные исключения и
	 * таймауты.
	 *
	 * @param testInstance экземпляр тестируемого класса
	 * @param testMethod   метод, помеченный @Test
	 */
	private void runTest(Object testInstance, Method testMethod) {
		Test annotation = testMethod.getAnnotation(Test.class);
		String testName = testMethod.getName();

		try {
			long startTime = System.currentTimeMillis();

			// Проверка наличия таймаута
			if (annotation.timeout() > 0) {
				runTestWithTimeout(testInstance, testMethod, annotation.timeout());
			} else {
				testMethod.invoke(testInstance);
			}

			long duration = System.currentTimeMillis() - startTime;

			// Проверяем, ожидалось ли исключение
			if (annotation.expected() != Test.None.class) {
				System.out.println("FAIL: " + testName + " - Expected exception: " +
						annotation.expected().getSimpleName() + " but none was thrown");
				testsFailed++;
			} else {
				System.out.println("PASS: " + testName + " (" + duration + "ms)");
				testsPassed++;
			}

		} catch (InvocationTargetException e) {
			// Ловим исключение, брошенное внутри теста
			Throwable actualException = e.getTargetException();

			// Проверяем, совпадает ли оно с ожидаемым
			if (annotation.expected().isInstance(actualException)) {
				System.out.println("PASS: " + testName + " - Correctly threw: " +
						actualException.getClass().getSimpleName());
				testsPassed++;
			} else {
				System.out.println("FAIL: " + testName + " - Unexpected exception: " +
						actualException.getClass().getSimpleName() + ": " + actualException.getMessage());
				testsFailed++;
			}
		} catch (Exception e) {
			System.out.println("FAIL: " + testName + " - " + e.getMessage());
			testsFailed++;
		}
	}

	/**
	 * Выполняет тест с ограничением по времени выполнения (timeout).
	 *
	 * @param testInstance экземпляр тестового класса
	 * @param testMethod   тестовый метод
	 * @param timeout      время ожидания в миллисекундах
	 * @throws Exception если тест не уложился в таймаут
	 */
	private void runTestWithTimeout(Object testInstance, Method testMethod, long timeout)
			throws Exception {
		// Запускаем тест в отдельном потоке
		Thread testThread = new Thread(() -> {
			try {
				testMethod.invoke(testInstance);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		testThread.start();
		testThread.join(timeout); // ждём указанное время

		// Проверяем, не завис ли тест
		if (testThread.isAlive()) {
			testThread.interrupt();
			throw new RuntimeException("Test timed out after " + timeout + "ms");
		}
	}

	/**
	 * Выполняет все методы, аннотированные указанной аннотацией.
	 *
	 * @param testInstance экземпляр тестового класса
	 * @param annotation   аннотация, например @BeforeAll, @AfterEach и т.д.
	 * @throws Exception при ошибке выполнения метода
	 */
	private void runAnnotatedMethods(Object testInstance, Class<? extends Annotation> annotation)
			throws Exception {
		Method[] methods = testInstance.getClass().getDeclaredMethods();

		for (Method method : methods) {
			if (method.isAnnotationPresent(annotation)) {
				method.invoke(testInstance);
			}
		}
	}

	/**
	 * Печатает итоговую статистику по всем тестам.
	 */
	private void printResults() {
		System.out.println("\n=== TEST RESULTS ===");
		System.out.println("Total: " + (testsPassed + testsFailed));
		System.out.println("Passed: " + testsPassed);
		System.out.println("Failed: " + testsFailed);
		System.out.println("Success rate: " +
				(testsPassed * 100 / (testsPassed + testsFailed)) + "%");
	}
}
