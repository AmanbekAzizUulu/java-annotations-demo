# 🧩 **Общее назначение**

Класс `DependencyInjectionContainer` — это **управляющий контейнер**, который:

* создаёт объекты нужных классов (так называемые *бины*);
* хранит их (реализуя *singleton-поведение*);
* автоматически внедряет зависимости в поля, помеченные аннотацией `@Autowired`.

По сути — это **мини-версия Spring IoC контейнера**.

---

# 🏗️ **Структура класса**

---

## 🔹 Поля

### 1. `private Map<String, Object> beans = new HashMap<>();`

* **Назначение:** хранит уже созданные объекты (бины).
* **Ключ:** имя класса (`String`, обычно `SimpleName`).
* **Значение:** экземпляр объекта (singleton).

📘 Пример:
После первого вызова `getBean(UserService.class)` контейнер создаёт объект `UserServiceImplementation` и сохраняет:

```java
beans.put("UserService", userServiceImplementationInstance);
```

При следующем вызове — отдаст уже готовый экземпляр.

---

### 2. `private Map<Class<?>, Class<?>> implementations = new HashMap<>();`

* **Назначение:** сопоставляет **интерфейсы** и **их реализации**.
* Это нужно, чтобы при запросе `UserService.class` контейнер знал, что нужно создать `UserServiceImplementation`.

📘 Пример:

```java
implementations.put(UserService.class, UserServiceImplementation.class);
implementations.put(UserRepository.class, UserRepositoryImplementation.class);
```

---

## 🔹 Методы

---

### 1. `public void register(Class<?> interfaceClass, Class<?> implementationClass)`

* Регистрирует пару *интерфейс → реализация* вручную.
* Нужно, если ты хочешь добавить новую реализацию без сканирования пакета.

📘 Пример:

```java
container.register(PaymentService.class, PaymentServiceImplementation.class);
```

---

### 2. `public void scanAndRegister(String basePackage)`

* Имитирует автоматическое **сканирование пакетов** (в реальном DI-контейнере ищут аннотации вроде `@Component`).
* Здесь просто вызывает `registerComponents()`, чтобы зарегистрировать заранее заданные классы.

📘 Пример вызова:

```java
container.scanAndRegister("com.dandaev.edu");
```

---

### 3. `private void registerComponents()`

* Это **вспомогательный метод**, который вручную добавляет базовые сервисы и репозитории.
* Используется вместо автоматического сканирования.

📘 Пример содержимого:

```java
internalRegister(UserService.class, UserServiceImplementation.class);
internalRegister(UserRepository.class, UserRepositoryImplementation.class);
```

---

### 4. `private void internalRegister(Class<?> interfaceClass, Class<?> implementationClass)`

* Низкоуровневая версия `register()`, используется только самим контейнером.
* Добавляет запись в `implementations`.

---

### 5. `public <T> T getBean(Class<T> clazz)`

* Главный метод контейнера.
* Отвечает за **получение экземпляра бина**:

  1. Проверяет, есть ли объект в `beans`.
  2. Если нет — ищет реализацию, создаёт экземпляр (`createInstance()`), внедряет зависимости (`injectDependencies()`), сохраняет.
  3. Возвращает готовый объект.

📘 Пример:

```java
UserService userService = container.getBean(UserService.class);
```

📊 Логика:

```
beans -> уже созданный объект?
       ↳ да → вернуть
       ↳ нет → создать → внедрить зависимости → сохранить → вернуть
```

---

### 6. `private <T> T createInstance(Class<?> clazz)`

* Создаёт **новый экземпляр** класса через **рефлексию**.
* Использует **конструктор без параметров** (`clazz.getDeclaredConstructor()`).
* После создания вызывает `injectDependencies(instance)`.

📘 Пример:

```java
UserServiceImplementation obj = (UserServiceImplementation) constructor.newInstance();
injectDependencies(obj);
```

---

### 7. `private void injectDependencies(Object instance)`

* Самое интересное место 💡
* Ищет все **поля**, помеченные аннотацией `@Autowired`.
* Для каждого такого поля:

  1. Определяет тип зависимости (`field.getType()`).
  2. Получает или создаёт бин нужного типа (`getBean(...)`).
  3. Внедряет его в поле через `field.set(instance, dependency)`.

📘 Пример:

```java
public class UserServiceImplementation {
    @Autowired
    private UserRepository userRepository;
}
```

При создании `UserServiceImplementation` контейнер:

* видит поле `userRepository` с `@Autowired`,
* создаёт/находит `UserRepositoryImplementation`,
* внедряет его в `userRepository`.

---

## 🔹 Аннотация `@Autowired`

Используется как **маркер**, чтобы контейнер знал:

> «Это поле нужно заполнить автоматически».

Если аннотация содержит `required = true`, а зависимость не найдена — выбрасывается исключение.

---

# 🔁 **Алгоритм работы контейнера (коротко)**

1. Контейнер создаётся:

   ```java
   DependencyInjectionContainer container = new DependencyInjectionContainer();
   ```
2. Регистрируются компоненты:

   ```java
   container.scanAndRegister("com.dandaev.edu");
   ```
3. При запросе бина:

   ```java
   UserService service = container.getBean(UserService.class);
   ```

   происходит:

   * поиск реализации в `implementations`,
   * создание объекта,
   * автоматическое внедрение `@Autowired` зависимостей,
   * сохранение в `beans`,
   * возврат готового экземпляра.

---

# 🧠 Итого

| Элемент                | Назначение                                                    |
| ---------------------- | ------------------------------------------------------------- |
| `beans`                | Хранилище готовых singleton-объектов                          |
| `implementations`      | Карта интерфейсов и их реализаций                             |
| `register()`           | Регистрация пары вручную                                      |
| `scanAndRegister()`    | Имитирует поиск и регистрацию компонентов                     |
| `internalRegister()`   | Внутренний метод регистрации                                  |
| `getBean()`            | Получает или создаёт бин                                      |
| `createInstance()`     | Создаёт объект с помощью рефлексии                            |
| `injectDependencies()` | Внедряет зависимости по `@Autowired`                          |
| `@Autowired`           | Указывает, что это зависимость для автоматической подстановки |


---

Разберём эти три метода максимально подробно: что они делают, почему так реализованы, какие у них слабые места и как можно улучшить. Я объясню шаг за шагом, добавлю пример выполнения и предложу практические рекомендации и улучшения кода.

## ○ getBean(Class<T> clazz)

## Что делает (пошагово)

1. `String beanName = clazz.getSimpleName();` — формирует имя бина по простому имени класса (без пакета).
2. `if (beans.containsKey(beanName)) { return (T) beans.get(beanName); }` — если экземпляр уже создан (singleton), возвращает его.
3. `Class<?> implementation = implementations.getOrDefault(clazz, clazz);` — если для запроса (обычно интерфейса) есть зарегистрированная реализация, берёт её; иначе предполагает, что `clazz` сам по себе — реализуемый класс.
4. `T instance = createInstance(implementation);` — создаёт экземпляр выбранного класса (через рефлексию).
5. `beans.put(beanName, instance);` — сохраняет созданный экземпляр в кэше (`beans`), чтобы при следующем запросе вернуть тот же объект.
6. `return instance;` — возвращает созданный объект.

## Ключевые замечания и ограничения

* **Ключ — `clazz.getSimpleName()`**:

  * Проблема: имена классов без пакета могут совпадать (например, `com.a.User` и `com.b.User`) → конфликт.
  * Рекомендация: использовать `Class<?>` как ключ или `clazz.getName()` (полное имя с пакетом).
* **Тип возвращаемого значения и unchecked cast**:

  * Используется `@SuppressWarnings("unchecked")`, потому что `beans` хранит `Object`. Это обычная компромиссная практика, но будьте аккуратны с безопасностью типов.
* **Singleton-поведение**: `getBean` всегда кеширует объект — это фиксированная политика singleton для всех классов (нет поддержки prototype scope).
* **Потокобезопасность**: `beans` — обычный `HashMap`. В многопоточной среде возможны гонки (создание нескольких экземпляров). Надо использовать `ConcurrentHashMap` либо синхронизацию.
* **Круговые зависимости**: реализация в текущем виде может застрять при круговой зависимости (A → B → A), потому что `getBean` при создании вызывает `createInstance`, который вызывает `injectDependencies`, который снова вызывает `getBean` и т.д. Нужна механика ранних ссылок/промежуточного создания или поддержка прокси.

---

## createInstance(Class<?> clazz)

### Что делает

1. `Constructor<?> constructor = clazz.getDeclaredConstructor();` — ищет **конструктор без параметров**. Если его нет — `NoSuchMethodException`.
2. `constructor.setAccessible(true);` — делает конструктор доступным, даже если он `private`.
3. `T instance = (T) constructor.newInstance();` — создаёт объект через вызов конструктора.
4. `injectDependencies(instance);` — после создания объекта внедряет все поля, помеченные `@Autowired`.
5. `return instance;` — возвращает созданный экземпляр.

### Замечания и ограничения

* **Требование no-arg конструктора**: текущая реализация работает только с классами, у которых есть публичный/доступный конструктор без параметов. Для классов с зависимостями через конструктор (constructor injection) это не подойдёт.
* **setAccessible(true)**: работает, но в некоторых средах с SecurityManager / модульной системой (JPMS) это может не сработать или быть запрещено.
* **Выбор конструктора**: лучше поддерживать выбор конструктора (например, помеченного `@Inject` или просто самый «подходящий»), и выполнять внедрение через конструктор (безопаснее и тестируемее).
* **Обработка исключений**: сейчас метод пробрасывает `Exception` наружу; можно использовать более узкие типы исключений или обёртки (например, `BeanCreationException`).

---

## injectDependencies(Object instance)

### Что делает

1. `Field[] fields = instance.getClass().getDeclaredFields();` — получает все поля класса (только текущий класс, не суперклассы).
2. Для каждого `field`:

   * проверяет `if (field.isAnnotationPresent(Autowired.class))`.
   * получает аннотацию `Autowired annotation = field.getAnnotation(Autowired.class);`
   * `Object dependency = getBean(field.getType());` — рекурсивно получает (или создаёт) бин нужного типа.
   * если `dependency == null && annotation.required()` — кидает `IllegalStateException`.
   * `field.setAccessible(true); field.set(instance, dependency);` — делает поле доступным и устанавливает значение.

### Замечания и ограничения

* **Инъекция только в поля текущего класса**: не проверяются поля суперклассов. Если требуется — надо итерировать по `getSuperclass()` рекурсивно.
* **Порядок полей**: Java не гарантирует порядок полей; это редко имеет значение, но иногда важно при сложных инициализациях.
* **Nullable зависимости**: если `@Autowired(required = false)`, код допускает `null` (но текущая логика проверяет и кидает при `required = true`).
* **Круговые зависимости и частично инициализированные объекты**: как выше — при круговой зависимости может возникнуть зацикливание. Один из подходов — сохранять «пустой»/частично созданный объект в кэше ранних ссылок перед вызовом `injectDependencies`, чтобы `getBean` мог вернуть ссылку и разорвать цикл.
* **Поле в суперклассе / приватные поля**: `setAccessible(true)` позволит писать в приватные поля, но в современных Java-модульных приложениях это может быть заблокировано.

---

## Пример выполнения (UserService → UserRepository)

1. Вызов: `container.getBean(UserService.class)`.
2. `beanName = "UserService"`, в `beans` — нет.
3. `implementations.getOrDefault(UserService.class, UserService.class)` → `UserServiceImplementation.class`.
4. `createInstance(UserServiceImplementation.class)`:

   * создаёт новый `UserServiceImplementation()` через no-arg конструктор.
   * вызывает `injectDependencies(instance)`.
5. В `injectDependencies`: находит поле `@Autowired private UserRepository userRepository;`

   * вызывает `getBean(UserRepository.class)` (рекурсивно создаёт `UserRepositoryImplementation`).
   * устанавливает `instance.userRepository = repositoryInstance`.
6. После успешного внедрения, `createInstance` возвращает `instance`.
7. `getBean` сохраняет `beans.put("UserService", instance)` и возвращает его.

---

## Рекомендации по улучшению (код + идеи)

Ниже — краткий список улучшений и примеры кода.

#### 1) Использовать `Class<?>` как ключ в `beans`

```java
private Map<Class<?>, Object> beans = new ConcurrentHashMap<>();
...
public <T> T getBean(Class<T> clazz) throws Exception {
    if (beans.containsKey(clazz)) {
        return (T) beans.get(clazz);
    }
    Class<?> implementation = implementations.getOrDefault(clazz, clazz);
    // double-checked locking / atomic computeIfAbsent
    return (T) beans.computeIfAbsent(clazz, key -> {
        try {
            return createInstance(implementation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });
}
```

#### 2) Поддержать многопоточность

* Используй `ConcurrentHashMap` и `computeIfAbsent` для атомарного создания бина.

#### 3) Обработка круговых зависимостей (простейшая стратегия: ранняя регистрация)

Перед `injectDependencies` сохранить «заготовку» в `beans`:

```java
// pseudo
beans.put(clazz, instance); // ранняя регистрация -> разрывает цикл
injectDependencies(instance);
```

Но это работает корректно только для полевой инъекции (field injection). Для конструкторной инъекции нужен другой подход.

#### 4) Поддержка constructor injection

* Найти подходящий конструктор (например помеченный `@Inject` или с максимальным количеством параметров, все типы которых можно получить из контейнера), собрать аргументы через `getBean` и вызвать конструктор с аргументами.

### 5) Бросать специализированные исключения

* `BeanCreationException`, `NoSuchBeanDefinitionException` вместо `Exception` / `IllegalStateException` — так легче отлавливать ошибки.

#### 6) Внедрение в поля суперклассов

* Рекурсивно обходить `Class<?> c = instance.getClass(); while (c != Object.class) { ... c = c.getSuperclass(); }`.

### 7) Логирование и диагностика

* Логи при создании бинов, при ошибках — полезно для отладки.

---

## Итог — коротко

* `getBean` — ответственный за получение/кеширование бина, выбор реализации и создание при необходимости.
* `createInstance` — создаёт объект через no-arg конструктор и сразу вызывает `injectDependencies`.
* `injectDependencies` — находит поля с `@Autowired` и рекурсивно запрашивает их бины, затем устанавливает значения через reflection.

Каждый метод работает, но текущая реализация простая и имеет ограничения: нет потокобезопасности, уязвимость к конфликтам имён, нет поддержки constructor injection, и возможны проблемы с круговыми зависимостями. Я привёл практические улучшения и краткие примеры кода — если хочешь, могу сделать одно из улучшений прямо сейчас (например, переписать `beans` на `Map<Class<?>, Object>` с `computeIfAbsent`, или добавить поддержку полей суперклассов / ранней регистрации для борьбы с круговыми зависимостями). Какой вариант интересует больше?
