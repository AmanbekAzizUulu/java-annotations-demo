package com.dandaev.edu.dependency.injection.container;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.dandaev.edu.annotations.dependency.injection.Autowired;
import com.dandaev.edu.repository.UserRepository;
import com.dandaev.edu.repository.UserRepositoryImplementation;
import com.dandaev.edu.service.UserService;
import com.dandaev.edu.service.UserServiceImplementation;

/**
 * Контейнер для внедрения зависимостей (Dependency Injection Container).
 *
 * Этот класс управляет жизненным циклом бинов и их зависимостями.
 * Он поддерживает регистрацию интерфейсов и их реализаций,
 * а также автоматическое внедрение зависимостей, помеченных аннотацией
 * {@link Autowired}.
 *
 * <p>
 * Контейнер хранит созданные объекты (singletons) в памяти и повторно
 * использует их при запросе.
 * </p>
 *
 * Пример использования:
 *
 * <pre>
 * DependencyInjectionContainer container = new DependencyInjectionContainer();
 * container.scanAndRegister("com.dandaev.edu");
 * UserService userService = container.getBean(UserService.class);
 * </pre>
 *
 * @author
 * @version 1.0
 */
public class DependencyInjectionContainer {

	/**
	 * Карта зарегистрированных бинов (singleton-объектов), где ключ — имя класса.
	 */
	private Map<String, Object> beans = new HashMap<>();

	/** Карта зарегистрированных интерфейсов и их реализаций. */
	private Map<Class<?>, Class<?>> implementations = new HashMap<>();

	/**
	 * Регистрирует реализацию интерфейса вручную.
	 *
	 * @param interfaceClass      интерфейс, для которого регистрируется реализация
	 * @param implementationClass класс, реализующий данный интерфейс
	 */
	public void register(Class<?> interfaceClass, Class<?> implementationClass) {
		implementations.put(interfaceClass, implementationClass);
	}

	/**
	 * Имитирует сканирование пакета и регистрирует компоненты.
	 *
	 * <p>
	 * В реальной реализации здесь должно происходить динамическое
	 * сканирование пакетов для поиска классов с аннотациями вроде
	 * {@code @Component}.
	 * </p>
	 *
	 * @param basePackage базовый пакет для поиска компонентов
	 * @throws Exception если при регистрации произошла ошибка
	 */
	public void scanAndRegister(String basePackage) throws Exception {
		// В реальной реализации здесь было бы сканирование пакетов.
		// Для демонстрации регистрируем вручную.
		registerComponents();
	}

	/**
	 * Внутренний метод для регистрации базовых компонентов.
	 * <p>
	 * Добавляет тестовые сервисы и репозитории в контейнер.
	 * </p>
	 */
	private void registerComponents() {
		internalRegister(UserService.class, UserServiceImplementation.class);
		internalRegister(UserRepository.class, UserRepositoryImplementation.class);
	}

	/**
	 * Внутренний метод регистрации пары интерфейс–реализация.
	 *
	 * @param interfaceClass      интерфейс
	 * @param implementationClass реализация интерфейса
	 */
	private void internalRegister(Class<?> interfaceClass, Class<?> implementationClass) {
		implementations.put(interfaceClass, implementationClass);
	}

	/**
	 * Возвращает экземпляр бина указанного типа.
	 *
	 * <p>
	 * Если бин уже создан, возвращается существующий экземпляр (singleton).
	 * Если нет — создаётся новый экземпляр, и все его зависимости внедряются
	 * автоматически.
	 * </p>
	 *
	 * @param <T>   тип возвращаемого объекта
	 * @param clazz класс или интерфейс, бин которого требуется получить
	 * @return экземпляр бина указанного типа
	 * @throws Exception если невозможно создать экземпляр или внедрить зависимости
	 */
	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> clazz) throws Exception {
		String beanName = clazz.getSimpleName();

		if (beans.containsKey(beanName)) {
			return (T) beans.get(beanName);
		}

		// Создание экземпляра класса
		Class<?> implementation = implementations.getOrDefault(clazz, clazz);
		T instance = createInstance(implementation);
		beans.put(beanName, instance);

		return instance;
	}

	/**
	 * Создаёт экземпляр указанного класса с помощью рефлексии.
	 *
	 * <p>
	 * После создания объекта вызывается метод {@link #injectDependencies(Object)}
	 * для автоматического внедрения зависимостей.
	 * </p>
	 *
	 * @param <T>   тип создаваемого объекта
	 * @param clazz класс, экземпляр которого требуется создать
	 * @return созданный объект
	 * @throws Exception если невозможно создать экземпляр
	 */
	@SuppressWarnings("unchecked")
	private <T> T createInstance(Class<?> clazz) throws Exception {
		Constructor<?> constructor = clazz.getDeclaredConstructor();
		constructor.setAccessible(true);
		T instance = (T) constructor.newInstance();

		// Внедрение зависимостей
		injectDependencies(instance);

		return instance;
	}

	/**
	 * Выполняет внедрение зависимостей в поля, помеченные аннотацией
	 * {@link Autowired}.
	 *
	 * @param instance объект, в поля которого нужно внедрить зависимости
	 * @throws Exception если требуемая зависимость не найдена
	 */
	private void injectDependencies(Object instance) throws Exception {
		Field[] fields = instance.getClass().getDeclaredFields();

		for (Field field : fields) {
			if (field.isAnnotationPresent(Autowired.class)) {
				Autowired annotation = field.getAnnotation(Autowired.class);
				Object dependency = getBean(field.getType());

				if (dependency == null && annotation.required()) {
					throw new IllegalStateException("Required dependency not found: " + field.getType());
				}

				field.setAccessible(true);
				field.set(instance, dependency);
			}
		}
	}
}
