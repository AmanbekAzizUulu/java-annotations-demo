Разберём **по этапам и просто**, что происходит в твоём примере: от запуска `main()` до вывода результатов, и как здесь участвуют аннотации. Ниже — нумерованный пошаговый разбор + короткие замечания про аннотации (и исправленные определения).

---

## 1) Запуск программы

`main()` создаёт экземпляр фреймворка и регистрирует контроллер:

```java
WebFramework framework = new WebFramework();
framework.registerController(new UserController());
```

— `registerController` принимает объект контроллера (твой `UserController`) и кладёт его в карту `controllers`, если класс помечен `@RestController`.

---

## 2) Регистрация контроллера (`registerController`)

* Фреймворк через reflection: `controller.getClass()`.
* Проверяет: `clazz.isAnnotationPresent(RestController.class)`.
* Берёт значение `path()` из аннотации `@RestController` (например `"/api/users"`) и кладёт в `controllers.put(basePath, controller)`.

Итог: фреймворк знает, что есть контроллер, обслуживающий базовый путь `/api/users`.

---

## 3) Приходит имитация HTTP-запроса (`handleRequest`)

В `main()` мы симулируем запросы вызовами:

```java
framework.handleRequest("GET", "/api/users", new HashMap<>(), "");
framework.handleRequest("GET", "/api/users/1", new HashMap<>(), "");
framework.handleRequest("POST", "/api/users", params, "");
framework.handleRequest("GET", "/api/users/search", searchParams, "");
```

`handleRequest` получает: `httpMethod`, `path`, `params` (query/form), `body`.

---

## 4) Поиск подходящего метода в зарегистрированных контроллерах

Внутри `handleRequest`:

* Перебираются все контроллеры: `for (Object controller : controllers.values())`.
* Через reflection берутся все методы: `controller.getClass().getDeclaredMethods()`.
* Для каждого метода проверяется наличие `@RequestMapping`.
* Если есть — читается аннотация: `RequestMapping mapping = method.getAnnotation(RequestMapping.class)`.

Дальше проверяются два условия:

1. HTTP-метод совпадает: `mapping.method().equalsIgnoreCase(httpMethod)`.
2. Путь совпадает: `matchPath(mapping.path(), path)`.

Если оба — найден обработчик, вызывается `invokeMethod(controller, method, path, params, body)`.

---

## 5) Сопоставление пути (`matchPath`)

Твоя простая реализация:

```java
return mappingPath.equals(requestPath) ||
       mappingPath.equals(requestPath.split("\\?")[0]);
```

— это простое сравнение строк. Важно: оно НЕ поддерживает параметры в фигурных скобках `{id}` — их обработка выполняется отдельно в `extractPathParameter`, но `matchPath` у тебя требует точного соответствия (так что на практике `mapping.path()` должен совпасть по структуре с requestPath).

---

## 6) Подготовка аргументов и вызов метода (`invokeMethod`)

`invokeMethod` делает главное: формирует массив аргументов `args[]` для вызова метода, подставляя значения из URL/params/body.

Алгоритм для каждого параметра метода:

* Если параметр помечен `@RequestParam`:

  * Берём имя параметра из аннотации или из `param.getName()`.
  * Берём значение из `params.get(paramName)`.
  * Если `required=true` и значение отсутствует → бросаем исключение.
  * Конвертируем строку в нужный тип (`convertValue`) и кладём в `args[i]`.
* Иначе если параметр помечен `@PathVariable`:

  * Берём строку из `extractPathParameter(mappingPath, requestPath, paramName)`.
  * Конвертируем и кладём в `args[i]`.
* Иначе если параметр помечен `@RequestBody`:

  * Просто кладём `body` (у тебя нет JSON-парсинга).
* Иначе — `args[i] = null`.

После формирования аргументов вызывается `method.invoke(controller, args)` и возвращается `toString()` результата.

---

## 7) Преобразование типов (`convertValue`)

`convertValue` из строки переводит в `int`, `long`, `double`, `boolean` или возвращает `String`.
Если значение `null` — возвращает `null`. (Тут нужно быть аккуратным: примитивы `int`/`long` и `null` вызовут NPE при попытке проставить в примитивный параметр.)

---

## 8) Извлечение параметра пути (`extractPathParameter`)

Пример: `mappingPath = "/api/users/{id}"`, `requestPath = "/api/users/1"`.

* Разбивает по `/`, сравнивает части. Если в `mappingParts[i]` — `{id}`, то возвращает `requestParts[i]` (т.е. `"1"`).
* Возвращённая строка затем конвертируется в `Long` и передаётся в параметр метода.

---

## 9) Результат и вывод в `main()`

Метод контроллера выполняется и возвращает строку, например:

* `"All users: 2"`
* `"User: Alice"`
* `"User created: Bob"`
  Эти строки печатаются на консоль.

---

## 10) Дополнительные замечания и ограничения

* `matchPath` слишком прост — лучше поддерживать шаблоны с `{}` и wildcard.
* `controllers.put(basePath, controller)` использует `basePath` как ключ; если два контроллера имеют одинаковый basePath — перезапись.
* `param.getName()` возвращает имя параметра только если при компиляции включён `-parameters` (иначе Java возвращает сгенерированные имена вроде `arg0`).
* Примитивы + `null` → возможны ошибки. Можно использовать обёртки (`Integer`, `Long`) или дефолты.
* Нет JSON-парсинга для `@RequestBody` — сейчас `body` просто строка.
* Отсутствует валидация, типизация массивов/коллекций и обработка исключений с кодами HTTP — всё учебное.

---

## Про аннотации в конце — важное исправление

В твоём фрагменте аннотации аннотировали сами себя, например:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@RestController
@interface RestController { ... }
```

Это **неправильно** — аннотация не должна помечаться своей собственной аннотацией. Правильно — указать только мета-аннотации `@Retention` и `@Target`. Правильные определения:

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RestController {
    String path() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestMapping {
    String path() default "";
    String method() default "GET";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RequestParam {
    String value() default "";
    boolean required() default true;
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PathVariable {
    String value() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RequestBody {}
```

Также обязательно — `@Retention(RetentionPolicy.RUNTIME)`, чтобы отражение (reflection) могло читать аннотацию во время выполнения.

---

## Короткая шпаргалка (что происходит в логическом порядке)

1. `main()` → создаёт фреймворк и регистрирует контроллер.
2. `registerController` читает `@RestController` и сохраняет контроллер.
3. `handleRequest` получает запрос, перебирает методы контроллера.
4. Находит метод с `@RequestMapping`, сравнивает метод и путь.
5. `invokeMethod` формирует аргументы на основе `@RequestParam`, `@PathVariable`, `@RequestBody`.
6. Вызов метода `method.invoke(...)`, возвращает строку — печать в `main()`.

---

# Детальное объяснение работы WebFramework

Разберем **пошагово**, что происходит в этом коде, на конкретных примерах.

## Архитектура фреймворка

### Основные компоненты:
- **WebFramework** - ядро фреймворка
- **Аннотации** - маркеры для классов и методов
- **Контроллеры** - классы с бизнес-логикой
- **Маршрутизация** - связь URL → методы

## Этап 1: Регистрация контроллеров

```java
WebFramework framework = new WebFramework();
framework.registerController(new UserController());
```

### Что происходит в `registerController`:

```java
public void registerController(Object controller) {
    Class<?> clazz = controller.getClass();

    // 1. Проверяем аннотацию @RestController
    if (clazz.isAnnotationPresent(RestController.class)) {
        // 2. Извлекаем базовый путь из аннотации
        String basePath = clazz.getAnnotation(RestController.class).path(); // "/api/users"

        // 3. Сохраняем контроллер в Map
        controllers.put(basePath, controller);

        // ВАЖНО: controllers = { "/api/users" → UserController@1234 }
    }
}
```

**Результат:** Контроллер зарегистрирован и готов к обработке запросов.

## Этап 2: Обработка запросов - общая схема

```java
framework.handleRequest("GET", "/api/users", new HashMap<>(), "")
```

### Структура метода `handleRequest`:

```java
public String handleRequest(String httpMethod, String path,
                           Map<String, String> params, String body) {
    try {
        // 1. Перебираем все зарегистрированные контроллеры
        for (Object controller : controllers.values()) {
            // 2. Получаем все методы контроллера
            Method[] methods = controller.getClass().getDeclaredMethods();

            // 3. Ищем подходящий метод
            for (Method method : methods) {
                if (method.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping mapping = method.getAnnotation(RequestMapping.class);

                    // 4. Проверяем совпадение метода и пути
                    if (mapping.method().equalsIgnoreCase(httpMethod) &&
                            matchPath(mapping.path(), path)) {

                        // 5. Вызываем найденный метод
                        return invokeMethod(controller, method, path, params, body);
                    }
                }
            }
        }

        return "404 Not Found";
    } catch (Exception e) {
        return "500 Internal Server Error: " + e.getMessage();
    }
}
```

## Пример 1: `GET /api/users`

### Шаг 1: Поиск подходящего метода

**Запрос:** `GET /api/users`

**Перебор методов UserController:**
1. `getAllUsers()` - `@RequestMapping(path = "/api/users", method = "GET")`
   - `mapping.method()` = "GET" ✅ совпадает
   - `mapping.path()` = "/api/users"
   - `matchPath("/api/users", "/api/users")` = true ✅

**Найден метод:** `getAllUsers()`

### Шаг 2: Вызов метода через `invokeMethod`

```java
private String invokeMethod(Object controller, Method method,
                           String path, Map<String, String> params, String body) {

    // method = getAllUsers()
    // parameters = [] (нет параметров)
    // args = new Object[0] (пустой массив)

    Object[] args = new Object[parameters.length]; // args = []

    // Параметров нет → цикл не выполняется

    // Вызов метода: userController.getAllUsers()
    Object result = method.invoke(controller, args);
    return result.toString(); // "All users: 2"
}
```

**Результат:** `"All users: 2"`

## Пример 2: `GET /api/users/1`

### Шаг 1: Поиск метода

**Запрос:** `GET /api/users/1`

**Перебор методов:**
1. `getAllUsers()` - путь "/api/users" ≠ "/api/users/1" ❌
2. `getUserById()` - `@RequestMapping(path = "/api/users/{id}", method = "GET")`
   - Метод: "GET" ✅
   - Путь: `matchPath("/api/users/{id}", "/api/users/1")` ✅

**Найден метод:** `getUserById(@PathVariable("id") Long id)`

### Шаг 2: Подготовка аргументов

```java
Parameter[] parameters = method.getParameters(); // [Parameter: Long id]
Object[] args = new Object[1]; // Массив для одного аргумента

for (int i = 0; i < parameters.length; i++) {
    Parameter param = parameters[i]; // Parameter: Long id

    // Проверяем аннотации параметра
    if (param.isAnnotationPresent(PathVariable.class)) {
        PathVariable annotation = param.getAnnotation(PathVariable.class);

        // Извлекаем значение {id} из пути
        String value = extractPathParameter(
            "/api/users/{id}",  // mappingPath
            "/api/users/1",     // requestPath
            "id"                // paramName
        ); // value = "1"

        // Преобразуем "1" → 1L
        args[i] = convertValue("1", Long.class); // args[0] = 1L
    }
}
```

### Шаг 3: Извлечение PathVariable

```java
private String extractPathParameter(String mappingPath, String requestPath, String paramName) {
    // mappingPath = "/api/users/{id}" → ["", "api", "users", "{id}"]
    // requestPath = "/api/users/1"     → ["", "api", "users", "1"]

    String[] mappingParts = mappingPath.split("/");
    String[] requestParts = requestPath.split("/");

    for (int i = 0; i < mappingParts.length; i++) {
        // Находим часть с {id}
        if (mappingParts[i].startsWith("{") && mappingParts[i].endsWith("}")) {
            String currentParamName = mappingParts[i].substring(1, mappingParts[i].length() - 1);

            if (currentParamName.equals(paramName)) {
                return requestParts[i]; // "1"
            }
        }
    }
    return null;
}
```

### Шаг 4: Преобразование типа

```java
private Object convertValue(String value, Class<?> targetType) {
    if (targetType == Long.class || targetType == long.class)
        return Long.parseLong(value); // "1" → 1L
    // ...
}
```

### Шаг 5: Вызов метода
```java
// method.invoke(controller, [1L])
// → вызывает userController.getUserById(1L)
// → возвращает "User: Alice"
```

**Результат:** `"User: Alice"`

## Пример 3: `POST /api/users`

### Шаг 1: Поиск метода

**Запрос:** `POST /api/users` с параметрами:
- `name = "Bob"`
- `email = "bob@example.com"`
- `age = "35"`

**Найден метод:** `createUser()` с `@RequestMapping(path = "/api/users", method = "POST")`

### Шаг 2: Подготовка аргументов для @RequestParam

```java
// method: createUser(@RequestParam("name") String name,
//                    @RequestParam("email") String email,
//                    @RequestParam(value = "age", required = false) Integer age)

Parameter[] parameters = method.getParameters(); // [String, String, Integer]
Object[] args = new Object[3];

for (int i = 0; i < parameters.length; i++) {
    Parameter param = parameters[i];

    if (param.isAnnotationPresent(RequestParam.class)) {
        RequestParam annotation = param.getAnnotation(RequestParam.class);

        // Определяем имя параметра
        String paramName = annotation.value().isEmpty() ?
            param.getName() : annotation.value();

        // Получаем значение из Map params
        String value = params.get(paramName);

        // Проверка обязательных параметров
        if (value == null && annotation.required()) {
            throw new IllegalArgumentException(...);
        }

        // Преобразование типа
        args[i] = convertValue(value, param.getType());
    }
}
```

**Обработка каждого параметра:**

1. **Параметр 0:** `@RequestParam("name") String name`
   - `paramName = "name"`
   - `value = params.get("name") = "Bob"`
   - `args[0] = convertValue("Bob", String.class) = "Bob"`

2. **Параметр 1:** `@RequestParam("email") String email`
   - `paramName = "email"`
   - `value = "bob@example.com"`
   - `args[1] = "bob@example.com"`

3. **Параметр 2:** `@RequestParam(value = "age", required = false) Integer age`
   - `paramName = "age"`
   - `value = "35"`
   - `required = false` (может быть null)
   - `args[2] = convertValue("35", Integer.class) = 35`

**Итоговые аргументы:** `["Bob", "bob@example.com", 35]`

### Шаг 3: Вызов метода
```java
// method.invoke(controller, ["Bob", "bob@example.com", 35])
// → userController.createUser("Bob", "bob@example.com", 35)
// → создает пользователя и возвращает "User created: Bob"
```

**Результат:** `"User created: Bob"`

## Пример 4: `GET /api/users/search?q=java`

### Шаг 1: Поиск метода

**Запрос:** `GET /api/users/search` с параметром `q = "java"`

**Найден метод:** `searchUsers(@RequestParam("q") String query)`

### Шаг 2: Подготовка аргументов

```java
// method: searchUsers(@RequestParam("q") String query)
Parameter param = parameters[0]; // Parameter: String query
RequestParam annotation = param.getAnnotation(RequestParam.class);

String paramName = "q"; // из annotation.value()
String value = params.get("q"); // "java" из Map params

args[0] = convertValue("java", String.class); // "java"
```

### Шаг 3: Вызов метода
```java
// method.invoke(controller, ["java"])
// → userController.searchUsers("java")
// → возвращает "Search results for: java"
```

**Результат:** `"Search results for: java"`

## Важные детали реализации

### 1. Метод `matchPath`

```java
private boolean matchPath(String mappingPath, String requestPath) {
    return mappingPath.equals(requestPath) ||
           mappingPath.equals(requestPath.split("\\?")[0]);
}
```
**Назначение:** Игнорирует query-параметры при сравнении путей
- `"/api/users/search"` = `"/api/users/search?q=java"` ✅

### 2. Преобразование типов в `convertValue`

```java
private Object convertValue(String value, Class<?> targetType) {
    if (value == null) return null;

    if (targetType == String.class) return value;
    if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(value);
    if (targetType == Long.class || targetType == long.class) return Long.parseLong(value);
    if (targetType == Double.class || targetType == double.class) return Double.parseDouble(value);
    if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(value);

    return value; // fallback
}
```

### 3. Обработка ошибок

```java
try {
    // Вся логика обработки
} catch (Exception e) {
    return "500 Internal Server Error: " + e.getMessage();
}
```
**Гарантирует,** что фреймворк не "упадет" при ошибках

## Поток данных через фреймворк

```
HTTP Request
    ↓
handleRequest("GET", "/api/users/1", params, body)
    ↓
Поиск контроллера → UserController
    ↓
Поиск метода → getUserById(@PathVariable Long id)
    ↓
extractPathParameter() → "1"
    ↓
convertValue("1", Long) → 1L
    ↓
method.invoke(controller, 1L)
    ↓
"User: Alice" ← возвращается из метода
    ↓
Клиенту
```

## Что делает этот фреймворк особенным:

1. **Декларативная маршрутизация** - через аннотации, а не конфигурационные файлы
2. **Автоматическое преобразование типов** - String → Long, Integer и т.д.
3. **Извлечение данных из разных источников** - path, query params, body
4. **Обработка ошибок** - фреймворк не падает при исключениях
5. **Простота использования** - достаточно добавить аннотации к методам

Это **упрощенная версия** того, как работают реальные фреймворки типа Spring MVC, но она демонстрирует все ключевые принципы работы с аннотациями и reflection в Java.

---

# Подробное объяснение метода `handleRequest`

Разберём метод по строкам и логическим блокам — что он делает, зачем и какие тонкие места/подводные камни есть.

```java
public String handleRequest(String httpMethod, String path, Map<String, String> params, String body) {
    try {
        for (Object controller : controllers.values()) {
            Method[] methods = controller.getClass().getDeclaredMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping mapping = method.getAnnotation(RequestMapping.class);

                    // Проверка совпадения метода и пути
                    if (mapping.method().equalsIgnoreCase(httpMethod) && matchPath(mapping.path(), path)) {
                        return invokeMethod(controller, method, path, params, body);
                    }
                }
            }
        }

        return "404 Not Found";
    } catch (Exception e) {
        return "500 Internal Server Error: " + e.getMessage();
    }
}
```

---

## 1. Подпись метода и параметры

* `httpMethod` — HTTP-метод запроса, например `"GET"`, `"POST"`.
* `path` — полный путь запроса, например `"/api/users/1"` или `"/api/users/search"`.
* `params` — карта параметров запроса (query-params или form-params), `Map<String,String>`.
* `body` — тело запроса (строка), используется для `@RequestBody` в контроллере.
* Возвращает `String` — упрощённый ответ (в реальном фреймворке это был бы объект Response с кодом и телом).

---

## 2. Внешний `try` — обработка ошибок

Весь основной код обёрнут в `try { ... } catch (Exception e)`.
Если что-то идёт не так (Reflection бросил исключение, `invoke` — исключение и т.д.), метод возвращает строку:

```text
"500 Internal Server Error: " + e.getMessage()
```

> Примечание: в production логичнее возвращать HTTP-код и логировать стектрейс, а не выдавать `e.getMessage()` клиенту.

---

## 3. Перебор зарегистрированных контроллеров

```java
for (Object controller : controllers.values()) {
    Method[] methods = controller.getClass().getDeclaredMethods();
    ...
}
```

* `controllers` — `Map<String,Object>`; здесь мы используем `values()` (игнорируем ключи).
* Для каждого контроллера получаем все **объявленные** методы (`getDeclaredMethods()` возвращает и private, и public, в отличие от `getMethods()`).

---

## 4. Перебор методов и проверка аннотации

```java
for (Method method : methods) {
    if (method.isAnnotationPresent(RequestMapping.class)) {
        RequestMapping mapping = method.getAnnotation(RequestMapping.class);
        ...
    }
}
```

* Мы отфильтровываем только те методы, которые помечены `@RequestMapping`.
* Затем читаем саму аннотацию, чтобы узнать `path` и `method` для этого обработчика.

---

## 5. Сопоставление HTTP-метода и пути

```java
if (mapping.method().equalsIgnoreCase(httpMethod) && matchPath(mapping.path(), path)) {
    return invokeMethod(controller, method, path, params, body);
}
```

* Сравниваем HTTP-метод (чувствительность к регистру устраняем с `equalsIgnoreCase`).
* Вызываем `matchPath(mapping.path(), path)` — проверяет, совпадает ли объявленный путь (в аннотации) с текущим запросом.
* Если оба условия выполнены — вызываем `invokeMethod(...)`, который:

  * формирует аргументы метода на основе `@RequestParam`, `@PathVariable`, `@RequestBody`,
  * вызывает метод через reflection (`method.invoke(...)`) и возвращает результат.
* Важно: метод `invokeMethod` может бросить исключение — оно попадёт в верхний `catch`.

---

## 6. Поведение при отсутствии совпадений

Если после перебора ВСЕХ контроллеров и их методов не найден подходящий обработчик — возвращается:

```text
"404 Not Found"
```

— простая текстовая индикация отсутствия маршрута.

---

## 7. Тонкие места и возможные проблемы

1. **Порядок поиска**

   * Мы перебираем `controllers.values()` и методы в порядке, который не гарантирован, если ты используешь `HashMap`. Это может повлиять, если есть конфликтующие маршруты — какой обработчик найдётся первым.

2. **Совпадение пути (`matchPath`)**

   * Твоя текущая реализация `matchPath` — очень простая (строковое сравнение). Если в `mapping.path()` используются шаблоны с `{id}`, то `matchPath` может не сработать. В твоём проекте ты отдельно извлекаешь `PathVariable`, но `matchPath` всё равно должен уметь распознавать шаблоны.

3. **Performance**

   * Reflection + перебор всех методов при каждом запросе — нормально для учебного примера, но в реальном фреймворке лучше при регистрации контроллеров заранее строить карту маршрутов (path+method → handler), чтобы не сканировать методы на каждый запрос.

4. **Параметры и имена**

   * `param.getName()` возвращает реальные имена параметров только если проект компилируется с флагом `-parameters`. Иначе — `arg0`, `arg1`. Лучше явно указывать имя в аннотации (`@RequestParam("name")`).

5. **Null и примитивы**

   * `convertValue` может возвращать `null`. Если целевой тип — примитив `int`, `long` и т.п., попытка присвоить `null` вызовет NPE/IllegalArgumentException при `invoke`. Лучше использовать обёртки (`Integer`, `Long`) или обрабатывать отсутствие значения заранее.

6. **Обработка исключений из `method.invoke`**

   * `invoke` оборачивает исключения в `InvocationTargetException`. Твой catch ловит их, но полезнее логировать стектрейс и возвращать корректный HTTP-код.

7. **Потокобезопасность**

   * `controllers` — `HashMap` без синхронизации. Регистрация контроллеров во время обработки (редко в runtime) может привести к race conditions. В многопоточной среде либо инициализируй карту до запуска сервера, либо используй `ConcurrentHashMap`.

8. **Безопасность**

   * Возвращать `e.getMessage()` клиенту может раскрывать внутренние детали. Лучше стандартные ошибки + логи на сервере.

---

## 8. Возможные улучшения (практические рекомендации)

* При регистрации контроллера заранее строить `Map<RouteKey, Handler>` где `RouteKey` = (`method`, `normalizedPath`) — это снимет необходимость сканировать методы при каждом запросе.
* Расширить `matchPath` чтобы он поддерживал `{var}` шаблоны и wildcard (`*`).
* Добавить приоритет маршрутов (чтобы `/api/users/search` не конфликтовал с `/api/users/{id}`).
* Возвращать структурированный ответ (объект `Response` с кодом и телом) вместо текстовой строки.
* Разделять логирование и ответ пользователю: логировать стектрейсы, и клиенту отдавать безопасное сообщение.
* Подумать о кэшировании рефлексивной информации (Parameter[] и аннотации) при регистрации.

---

## 9. Пример логики на одном запросе (схема)

1. Пришёл запрос: `"GET", "/api/users/1"`.
2. Для каждого контроллера:

   * перебор методов,
   * если метод помечен `@RequestMapping`,
   * читаем `mapping.method()` и `mapping.path()`,
   * сравниваем метод и `matchPath`,
   * при совпадении формируем аргументы и вызываем `invokeMethod`.
3. `invokeMethod` извлекает `@PathVariable("id")` → получает `"1"`, конвертирует в `Long`, вызывает метод.
4. Возвращённое значение метода выводится пользователю.

---
