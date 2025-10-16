package com.dandaev.edu.web.framework.implementation;

import com.dandaev.edu.annotations.web.framework.PathVariable;
import com.dandaev.edu.annotations.web.framework.RequestBody;
import com.dandaev.edu.annotations.web.framework.RequestMapping;
import com.dandaev.edu.annotations.web.framework.RequestParam;
import com.dandaev.edu.annotations.web.framework.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * Простой учебный веб-фреймворк, реализующий базовую маршрутизацию
 * HTTP-запросов
 * с использованием аннотаций в стиле Spring MVC.
 *
 * <p>
 * Основные возможности:
 * <ul>
 * <li>Регистрация контроллеров, аннотированных {@code @RestController}</li>
 * <li>Обработка маршрутов, помеченных {@code @RequestMapping}</li>
 * <li>Поддержка параметров {@code @RequestParam}, {@code @PathVariable} и
 * {@code @RequestBody}</li>
 * </ul>
 *
 * <p>
 * Класс выполняет роль простого диспетчера HTTP-запросов,
 * распределяя их по методам контроллеров на основе аннотаций.
 */
public class WebFramework {

	/** Хранилище контроллеров по базовому пути */
	private Map<String, Object> controllers = new HashMap<>();

	/**
	 * Регистрирует контроллер в контейнере фреймворка.
	 * Контроллер должен быть помечен аннотацией {@code @RestController}.
	 *
	 * @param controller экземпляр контроллера, содержащий обработчики HTTP-запросов
	 */
	public void registerController(Object controller) {
		Class<?> clazz = controller.getClass();
		if (clazz.isAnnotationPresent(RestController.class)) {
			String basePath = clazz.getAnnotation(RestController.class).path();
			controllers.put(basePath, controller);
		}
	}

	/**
	 * Обрабатывает входящий HTTP-запрос.
	 *
	 * @param httpMethod HTTP-метод (например, "GET", "POST")
	 * @param path       полный путь запроса
	 * @param params     карта параметров запроса (например, query-параметры)
	 * @param body       тело запроса (используется для {@code @RequestBody})
	 * @return результат работы метода контроллера, либо сообщение об ошибке
	 */
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

	/**
	 * Проверяет, соответствует ли путь из аннотации пути запроса.
	 * Поддерживает простое сравнение без регулярных выражений.
	 *
	 * @param mappingPath путь из аннотации {@code @RequestMapping}
	 * @param requestPath путь из запроса
	 * @return {@code true}, если пути совпадают
	 */
	private boolean matchPath(String mappingPath, String requestPath) {
		return mappingPath.equals(requestPath) ||
				mappingPath.equals(requestPath.split("\\?")[0]);
	}

	/**
	 * Вызывает метод контроллера, сопоставленный маршруту.
	 * Автоматически подставляет значения параметров на основе аннотаций.
	 *
	 * @param controller контроллер, содержащий метод
	 * @param method     метод контроллера для вызова
	 * @param path       путь запроса
	 * @param params     параметры запроса
	 * @param body       тело запроса
	 * @return результат работы метода контроллера
	 * @throws Exception если при вызове метода возникла ошибка
	 */
	private String invokeMethod(Object controller, Method method, String path, Map<String, String> params, String body) throws Exception {
		Parameter[] parameters = method.getParameters();
		Object[] args = new Object[parameters.length];

		// Формирование аргументов метода
		for (int i = 0; i < parameters.length; i++) {
			Parameter param = parameters[i];

			if (param.isAnnotationPresent(RequestParam.class)) {
				// Query-параметры
				RequestParam annotation = param.getAnnotation(RequestParam.class);
				String paramName = annotation.value().isEmpty() ? param.getName() : annotation.value();
				String value = params.get(paramName);

				if (value == null && annotation.required()) {
					throw new IllegalArgumentException("Required parameter missing: " + paramName);
				}

				args[i] = convertValue(value, param.getType());

			} else if (param.isAnnotationPresent(PathVariable.class)) {
				// Параметры пути (например, /users/{id})
				PathVariable annotation = param.getAnnotation(PathVariable.class);
				String value = extractPathParameter(method.getAnnotation(RequestMapping.class).path(),
						path, annotation.value());
				args[i] = convertValue(value, param.getType());

			} else if (param.isAnnotationPresent(RequestBody.class)) {
				// Тело запроса
				args[i] = body; // Упрощённый вариант — без JSON-парсинга

			} else {
				args[i] = null;
			}
		}

		Object result = method.invoke(controller, args);
		return result != null ? result.toString() : "null";
	}

	/**
	 * Преобразует строковое значение параметра в нужный тип.
	 *
	 * @param value      строка из параметров
	 * @param targetType целевой тип параметра метода
	 * @return преобразованное значение (или {@code null}, если value == null)
	 */
	private Object convertValue(String value, Class<?> targetType) {
		if (value == null)
			return null;

		if (targetType == String.class)
			return value;
		if (targetType == Integer.class || targetType == int.class)
			return Integer.parseInt(value);
		if (targetType == Long.class || targetType == long.class)
			return Long.parseLong(value);
		if (targetType == Double.class || targetType == double.class)
			return Double.parseDouble(value);
		if (targetType == Boolean.class || targetType == boolean.class)
			return Boolean.parseBoolean(value);

		return value;
	}

	/**
	 * Извлекает значение параметра из пути запроса.
	 * Например: если маршрут {@code /api/users/{id}}, а запрос
	 * {@code /api/users/5},
	 * то метод вернёт {@code "5"} для параметра {@code id}.
	 *
	 * @param mappingPath маршрут, указанный в аннотации {@code @RequestMapping}
	 * @param requestPath фактический путь запроса
	 * @param paramName   имя параметра, указанное в {@code @PathVariable}
	 * @return значение параметра пути, либо {@code null}, если не найдено
	 */
	private String extractPathParameter(String mappingPath, String requestPath, String paramName) {
		String[] mappingParts = mappingPath.split("/");
		String[] requestParts = requestPath.split("/");

		for (int i = 0; i < Math.min(mappingParts.length, requestParts.length); i++) {
			if (mappingParts[i].startsWith("{") && mappingParts[i].endsWith("}")) {
				String currentParamName = mappingParts[i].substring(1, mappingParts[i].length() - 1);
				if (currentParamName.equals(paramName)) {
					return requestParts[i];
				}
			}
		}

		return null;
	}
}
