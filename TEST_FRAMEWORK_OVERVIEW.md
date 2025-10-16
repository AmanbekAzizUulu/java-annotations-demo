Разберём этот пример подробно, шаг за шагом, и объясним что именно происходит в каждом месте кода, какие варианты контроля ошибок предусмотрены, и какие тонкие места стоит знать.

Я пройду по двум уровням:

1. Короткая ориентировка — что делает каждая часть (аннотации, тестовый фреймворк, тестовый класс).
2. Подробное пошаговое объяснение выполнения `TestingExample.main` — что происходит в рантайме на каждом этапе, какие исключения ловятся и как считается статистика.

---

## 1. Короткая ориентировка — блоки кода и их назначение

**Аннотации**

* `@Test` — помечает тестовый метод. Параметры:

  * `expected()` — класс ожидаемого исключения (по умолчанию `Test.None.class`, т.е. исключение не ожидается).
  * `timeout()` — миллисекунды, в которых тест должен завершиться; `0` — без таймаута.
  * Внутри `Test` объявлен `class None extends Throwable` как «маркер», означающий «исключение не ожидается».
* `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll` — lifecycle-методы (выполняются перед каждым тестом, после каждого теста, перед всеми тестами и после всех тестов соответственно).
* Все аннотации имеют `@Retention(RetentionPolicy.RUNTIME)` — значит они доступны в рантайме через reflection.

**TestFramework**

* Содержит счётчики `testsPassed` и `testsFailed`.
* `runTests(Class<?> testClass)` — основной метод: создаёт экземпляр тестового класса, выполняет все lifecycle-методы и сами тесты, считает результаты.
* `runTest(...)` — запускает одиночный тест: учитывает таймаут, ожидаемые исключения, фиксирует PASS/FAIL и время.
* `runTestWithTimeout(...)` — простая реализация таймаута: выполняет метод в отдельном потоке, ждёт `join(timeout)`; если поток жив — прерывает и бросает `RuntimeException`.
* `runAnnotatedMethods(...)` — находит все методы с данной аннотацией и вызывает их (через `method.invoke(testInstance)`).
* `printResults()` — печатает сводку.

**CalculatorTest**

* Определяет lifecycle-методы (`setUpClass`, `tearDownClass`, `setUp`, `tearDown`) и тесты:

  * `testAddition()` — проверка `add`.
  * `testDivisionByZero()` — помечен `expected = IllegalArgumentException.class`.
  * `testPerformance()` — помечен `timeout = 1000L`.
  * `testFailingTest()` — всегда бросает `RuntimeException`.
* Вспомогательный `Calculator` реализует `add` и `divide`.

---

## 2. Подробный пошаговый проход (что происходит при запуске `main`)

Предположим: выполняем `TestingExample.main()`.

### Шаг 0 — старт

* `main` создаёт `TestFramework` и вызывает `testFramework.runTests(CalculatorTest.class)`.

---

### Шаг 1 — инициализация: создание экземпляра тестового класса

* В `runTests`:

  * `System.out.println("Running tests for: " + testClass.getSimpleName());` — печать заголовка.
  * `Object testInstance = testClass.getDeclaredConstructor().newInstance();`

    * Создаётся **экземпляр** `CalculatorTest` через конструктор по умолчанию.
    * Важно: `runAnnotatedMethods` для `@BeforeAll`/`@AfterAll` будет искать методы по `testInstance.getClass().getDeclaredMethods()` и вызывать их **на этом экземпляре**. Статические методы можно вызывать через экземпляр — это допустимо в рефлексии (хотя семантически `@BeforeAll` обычно должен быть static).

---

### Шаг 2 — выполнение `@BeforeAll` методов

* `runAnnotatedMethods(testInstance, BeforeAll.class)`:

  * Перебираются все declared methods класса `CalculatorTest`.
  * Если метод помечен `@BeforeAll` (в нашем примере — `setUpClass`), то он вызывается через `method.invoke(testInstance);`
  * В `CalculatorTest` `setUpClass()` печатает `Setting up test class...`.
* Замечание: обычно `@BeforeAll` делают `static`. Здесь `setUpClass` объявлен `public static`, но `invoke` на статическом методе через объект работает — JVM игнорирует объект. Однако это может выглядеть чуть странно; более явный способ — `method.invoke(null)` для static. Но текущая реализация работает.

---

### Шаг 3 — сбор тестовых методов

* Код собирает все методы, помеченные `@Test`:

  * Проход по `testClass.getDeclaredMethods()` и `if (method.isAnnotationPresent(Test.class)) testMethods.add(method);`.
  * В `CalculatorTest` соберутся: `testAddition`, `testDivisionByZero`, `testPerformance`, `testFailingTest`.
* Порядок: порядок методов возвращаемый `getDeclaredMethods()` не гарантируется — фактический порядок может отличаться от исходного. Если нужен детерминированный порядок — надо сортировать.

---

### Шаг 4 — цикл по всем тестам

Для каждого `testMethod` из списка выполняется последовательность:

#### 4.1 Выполнение `@BeforeEach`

* `runAnnotatedMethods(testInstance, BeforeEach.class)`:

  * Находит `setUp()` и вызывает его на `testInstance`.
  * `setUp()` создаёт `calculator = new Calculator();` и печатает `Setting up test...`.

#### 4.2 Выполнение самого теста — `runTest(testInstance, testMethod)`

`runTest` делает следующее:

1. Получает аннотацию: `Test annotation = testMethod.getAnnotation(Test.class);`

2. Замер времени `startTime = System.currentTimeMillis();`

3. Если `annotation.timeout() > 0` — вызывает `runTestWithTimeout(...)`, иначе — `testMethod.invoke(testInstance);`

   * **Если тест выполняется в основном потоке** (нет таймаута), любое исключение, выброшенное в теле теста, будет упаковано в `InvocationTargetException`.
   * **Если таймаут указан**, тест запускается в отдельном потоке.

4. После успешного завершения (т.е. метод завершился без исключения):

   * Вычисляется `duration`.
   * Проверяется `if (annotation.expected() != Test.None.class)`:

     * Если ожидалось исключение, но ничего не было выброшено — это **FAIL** (пишем `Expected exception: X but none was thrown` и `testsFailed++`).
     * Если исключение не ожидалось — **PASS** (печать `"PASS: name (Nms)"`, `testsPassed++`).

5. Если при `invoke` возникло `InvocationTargetException` (т.е. тест бросил исключение):

   * `actualException = e.getTargetException();` — это реальное исключение, которое бросил тест.
   * Если `annotation.expected().isInstance(actualException)` — значит ожидаемое исключение совпало с фактическим:

     * Печатаем `PASS: ... - Correctly threw: <класс>`, увеличиваем `testsPassed`.
   * Иначе:

     * Печатаем `FAIL: ... - Unexpected exception: <класс>: <message>`, `testsFailed++`.

6. Любые другие `Exception` (рефлексивные ошибки) попадают в последний `catch` и считаются `FAIL`.

Применимо к каждому тесту `CalculatorTest`:

* `testAddition()`:

  * Нет `expected`, нет `timeout`.
  * Выполняется `calculator.add(2,3)` => `5`. Ничего не выброшено.
  * `annotation.expected() == Test.None.class` => PASS, подсчёт passed++.

* `testDivisionByZero()`:

  * Аннотация: `expected = IllegalArgumentException.class`.
  * При выполнении `calculator.divide(10, 0)` метод `divide` делает `if (b == 0) throw new IllegalArgumentException("Division by zero");`
  * Исключение выбрасывается, рефлексия возвращает `InvocationTargetException`. В `catch` мы делаем `actualException = e.getTargetException()`, проверяем `annotation.expected().isInstance(actualException)` — это `true`.
  * Результат: PASS (потому что тест **ожидал** это исключение).

* `testPerformance()`:

  * Аннотация `timeout = 1000L`.
  * `runTestWithTimeout` создаёт новый `Thread testThread = new Thread(() -> { testMethod.invoke(testInstance); })`.
  * `testThread.start()` — тест выполняется в новом потоке. `testThread.join(timeout)` — основной поток ждёт `timeout` миллисекунд.
  * В примере цикл суммирования 1_000_000 итераций — почти наверняка завершится быстрее чем 1000ms, поэтому `testThread` завершится, `isAlive()` вернёт `false`, и тест считается успешным (если он не бросил исключение).
  * Если код внутри теста застрянет и не завершится за `timeout`, тогда `testThread` останется жив; код `testThread.interrupt()` вызовет прерывание потока, а затем `runTestWithTimeout` бросит `RuntimeException("Test timed out after " + timeout + "ms")`. Это исключение попадёт в `runTest` в самом верхнем `try` (вне `InvocationTargetException`) и будет поймано как `catch (Exception e)` -> `FAIL: <test> - <message>`.

  Замечания про таймаут:

  * `interrupt()` не гарантирует немедленное завершение — только устанавливает флаг и прерывает блокирующие операции. Если тест игнорирует прерывания или выполняет CPU-bound цикл, поток может не завершиться сразу.
  * Более надёжный таймаут требует более сложной логики (например, `ExecutorService` + `Future.get(timeout, unit)` и прерывание/отключение задач).

* `testFailingTest()`:

  * Бросает `new RuntimeException("This test is designed to fail")`.
  * Это исключение не совпадает с `Test.None` (т.е. исключение не было ожидаемо) — в `catch (InvocationTargetException e)` мы увидим, что `annotation.expected().isInstance(actualException)` — false (поскольку expected == None.class).
  * Результат: **FAIL** с печатью `Unexpected exception: RuntimeException: This test is designed to fail`, и `testsFailed++`.

#### 4.3 Выполнение `@AfterEach`

* `runAnnotatedMethods(testInstance, AfterEach.class)` — вызывается `tearDown()` и печатает `Tearing down test...`.

---

### Шаг 5 — после всех тестов — `@AfterAll`

* После завершения цикла по всем тестам `runAnnotatedMethods(testInstance, AfterAll.class)` вызывается `tearDownClass()`, печатает `Tearing down test class...`.

---

### Шаг 6 — вывод результатов

* `printResults()`:

  * Считает `total = testsPassed + testsFailed`.
  * Печатает `Total`, `Passed`, `Failed`.
  * `Success rate: (testsPassed * 100 / total) + "%"`.
  * **Важно**: если `total == 0` (нет тестов), тогда происходит деление на ноль и будет `ArithmeticException`. В данном примере тестов > 0, но в общем случае стоит проверять `if (total == 0) ...`.

---

## Дополнительные важные замечания и возможные улучшения

1. **Статические методы `@BeforeAll/@AfterAll`:**

   * Сейчас `runAnnotatedMethods` всегда вызывает `method.invoke(testInstance)`. Если метод `static`, `invoke` с объектом тоже работает, но иногда лучше делать `method.invoke(null)` для статических методов или проверять `Modifier.isStatic(method.getModifiers())`.

2. **Доступ к private-методам:**

   * `getDeclaredMethods()` возвращает private методы, но `method.invoke(...)` бросит `IllegalAccessException`, если метод private и не был `setAccessible(true)`. В текущем коде все lifecycle метод/тесты объявлены `public`, но если они private — нужно `method.setAccessible(true)` перед вызовом.

3. **Порядок выполнения тестов не гарантирован:**

   * Порядок методов из `getDeclaredMethods()` не определён; если нужен порядок — сортируйте по имени или используйте дополнительную аннотацию.

4. **Timeout реализация:**

   * Как уже упомянуто, `interrupt()` не гарантирует остановку; лучше использовать `ExecutorService` + `Future` и `future.get(timeout, unit)` — тогда можно отменить задачу и безопаснее управлять ресурсами.

5. **Success rate — деление на ноль:**

   * Добавьте проверку `if (total == 0) print "No tests run"` или выводить `0%`.

6. **Логирование и отчётность:**

   * Можно выводить stacktrace при неожидаемых исключениях, добавлять суммарный вывод времени выполнения, сохранять результаты в XML/JSON для CI.

7. **Отдельный экземпляр на тест / общий экземпляр:**

   * Сейчас создаётся **один** экземпляр `testInstance` для всего набора тестов. В JUnit обычно либо создаётся новый экземпляр на каждый тест (изолированность состояния) либо специально документируется поведение. Здесь состояние `calculator` в `setUp` перезаписывается, так что это работает, но стоит явно указать семантику.

8. **Аннотация `Test.None`:**

   * Использована как sentinel value. Это стандартный паттерн.

9. **Исключения в lifecycle-методах:**

   * Если `@BeforeEach` бросит исключение, текущая реализация не прерывает выполнение остальных тестов — произойдёт `InvocationTargetException` и `runTest` не будет вызван (поскольку `runAnnotatedMethods` вызывается до `runTest` и он выбросит, попав в `throws Exception` в `runTests`). Текущее поведение: `runTests` объявлен `throws Exception`, поэтому исключение в `@BeforeEach` прерывает весь запуск — можно менять поведение чтобы помечать тест как `FAILED` и продолжать.

---

## Итог — что увидим в консоли (примерный вывод)

Порядок строк может отличаться, но примерно:

```
Running tests for: CalculatorTest
Setting up test class...
Setting up test...
✅ PASS: testAddition (5ms)
Tearing down test...
Setting up test...
✅ PASS: testDivisionByZero - Correctly threw: IllegalArgumentException
Tearing down test...
Setting up test...
✅ PASS: testPerformance (25ms)
Tearing down test...
Setting up test...
❌ FAIL: testFailingTest - Unexpected exception: RuntimeException: This test is designed to fail
Tearing down test...
Tearing down test class...

=== TEST RESULTS ===
Total: 4
Passed: 3
Failed: 1
Success rate: 75%
```

(Время в миллисекундах будет другим в реальном запуске.)

---

Если хочешь, могу:

* переписать `runTestWithTimeout` на `ExecutorService + Future` и показать пример;
* добавить `setAccessible(true)` в `runAnnotatedMethods` и обработку `static` методов более явно;
* сделать отдельный экземпляр `testInstance` для каждого теста (чтобы избежать совместного состояния);
* добавить обработку случая `total == 0`.

Скажи, какой из улучшений хочешь увидеть в коде — я поправлю прямо сейчас.
