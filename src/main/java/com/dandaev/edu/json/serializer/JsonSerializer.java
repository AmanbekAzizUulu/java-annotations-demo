package com.dandaev.edu.json.serializer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.dandaev.edu.annotations.jsonparser.JsonDate;
import com.dandaev.edu.annotations.jsonparser.JsonField;
import com.dandaev.edu.annotations.jsonparser.JsonSerializable;

/**
 * The {@code JsonSerializer} class provides functionality for serializing
 * Java objects into JSON strings using custom annotations.
 * <p>
 * Supported annotations:
 * <ul>
 * <li>{@link JsonSerializable} — marks a class as eligible for
 * serialization;</li>
 * <li>{@link JsonField} — specifies the JSON field name and whether to ignore
 * the field;</li>
 * <li>{@link JsonDate} — defines the date format for {@link java.util.Date}
 * fields.</li>
 * </ul>
 *
 * <p>
 * Supported data types:
 * <ul>
 * <li>Primitive types and wrappers (e.g., {@link Number},
 * {@link Boolean});</li>
 * <li>{@link String};</li>
 * <li>{@link java.util.Date};</li>
 * <li>Arrays and {@link java.util.Collection};</li>
 * <li>{@link java.util.Map};</li>
 * <li>Custom objects annotated with {@link JsonSerializable}.</li>
 * </ul>
 *
 * <p>
 * <b>Example usage:</b>
 * </p>
 *
 * <pre>
 * {
 * 	&#64;code
 * 	&#64;JsonSerializable
 * 	public class User {
 * 		&#64;JsonField(name = "user_name")
 * 		private String name;
 *
 * 		@JsonDate(pattern = "yyyy-MM-dd")
 * 		private Date birthDate;
 * 	}
 *
 * 	String json = JsonSerializer.serialize(new User("Alice", new Date()));
 * 	System.out.println(json);
 * }
 * </pre>
 *
 * @author Amanbek
 * @version 1.0
 */
public class JsonSerializer {

	/**
	 * Serializes a Java object into a JSON string.
	 * <p>
	 * Supports primitive types, strings, arrays, collections, maps,
	 * and objects annotated with {@link JsonSerializable}.
	 *
	 * @param obj the object to serialize
	 * @return a JSON representation of the object
	 * @throws IllegalAccessException   if field access is restricted
	 * @throws IllegalArgumentException if the class is not annotated with
	 *                                  {@link JsonSerializable}
	 */
	public static String serialize(Object obj) throws IllegalAccessException {
		if (obj == null)
			return "null";

		Class<?> clazz = obj.getClass();

		// Handle primitive types and strings before annotation check
		if (clazz.isPrimitive() || obj instanceof Number || obj instanceof Boolean) {
			return obj.toString();
		}

		if (obj instanceof String) {
			return "\"" + escapeJson(obj.toString()) + "\"";
		}

		if (obj instanceof java.util.Date) {
			return serializeDate((java.util.Date) obj, clazz);
		}

		// Arrays
		if (clazz.isArray()) {
			return serializeArray(obj);
		}

		// Collections
		if (obj instanceof Collection) {
			return serializeCollection((Collection<?>) obj);
		}

		// Maps
		if (obj instanceof Map) {
			return serializeMap((Map<?, ?>) obj);
		}

		// Only user-defined classes require annotation
		if (!clazz.isAnnotationPresent(JsonSerializable.class)) {
			throw new IllegalArgumentException("Class not annotated with @JsonSerializable: " + clazz.getName());
		}

		return serializeObject(obj);
	}

	/**
	 * Serializes a user-defined object into a JSON string.
	 * <p>
	 * Only fields annotated with {@link JsonField} are included.
	 * Fields marked as {@code ignore = true} are skipped.
	 * If a field is annotated with {@link JsonDate}, its value
	 * is formatted according to the specified date pattern.
	 *
	 * @param obj the object to serialize
	 * @return a JSON representation of the object
	 * @throws IllegalAccessException if access to a field is not allowed
	 */
	private static String serializeObject(Object obj) throws IllegalAccessException {
		Class<?> clazz = obj.getClass();
		StringBuilder json = new StringBuilder("{");
		Field[] fields = clazz.getDeclaredFields();
		boolean firstField = true;

		for (Field field : fields) {
			if (field.isAnnotationPresent(JsonField.class)) {
				JsonField annotation = field.getAnnotation(JsonField.class);

				if (annotation.ignore()) {
					continue;
				}

				field.setAccessible(true);
				Object value = field.get(obj);

				if (value == null) {
					continue; // Skip null values
				}

				if (!firstField) {
					json.append(",");
				}
				firstField = false;

				String fieldName = annotation.name().isEmpty() ? field.getName() : annotation.name();
				json.append("\"").append(fieldName).append("\":");

				if (field.isAnnotationPresent(JsonDate.class) && value instanceof java.util.Date) {
					JsonDate dateAnnotation = field.getAnnotation(JsonDate.class);
					json.append("\"").append(formatDate((java.util.Date) value, dateAnnotation.pattern())).append("\"");
				} else {
					json.append(serialize(value));
				}
			}
		}

		json.append("}");
		return json.toString();
	}

	/**
	 * Serializes a {@link Map} into a JSON object.
	 * Keys are converted to strings using {@code toString()}.
	 *
	 * @param map the map to serialize
	 * @return a JSON representation of the map
	 * @throws IllegalAccessException if access to a value is restricted
	 */
	private static String serializeMap(Map<?, ?> map) throws IllegalAccessException {
		StringBuilder json = new StringBuilder("{");
		boolean firstEntry = true;

		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (!firstEntry) {
				json.append(",");
			}
			firstEntry = false;

			String key = entry.getKey().toString();
			Object value = entry.getValue();

			json.append("\"").append(escapeJson(key)).append("\":");
			json.append(serialize(value));
		}

		json.append("}");
		return json.toString();
	}

	/**
	 * Serializes an array into a JSON array.
	 *
	 * @param array the array to serialize
	 * @return a JSON representation of the array
	 * @throws IllegalAccessException if access to an element is restricted
	 */
	private static String serializeArray(Object array) throws IllegalAccessException {
		int length = Array.getLength(array);
		StringBuilder json = new StringBuilder("[");

		for (int i = 0; i < length; i++) {
			if (i > 0)
				json.append(",");
			json.append(serialize(Array.get(array, i)));
		}

		json.append("]");
		return json.toString();
	}

	/**
	 * Serializes a {@link Collection} into a JSON array.
	 *
	 * @param collection the collection to serialize
	 * @return a JSON representation of the collection
	 * @throws IllegalAccessException if access to an element is restricted
	 */
	private static String serializeCollection(Collection<?> collection) throws IllegalAccessException {
		StringBuilder json = new StringBuilder("[");
		Iterator<?> iterator = collection.iterator();

		while (iterator.hasNext()) {
			json.append(serialize(iterator.next()));
			if (iterator.hasNext())
				json.append(",");
		}

		json.append("]");
		return json.toString();
	}

	/**
	 * Serializes a {@link java.util.Date} using the format defined by
	 * {@link JsonDate},
	 * if present on the declaring field or class.
	 *
	 * @param date  the date to serialize
	 * @param clazz the declaring class (used to find a date pattern)
	 * @return the formatted date string, quoted for JSON
	 */
	private static String serializeDate(java.util.Date date, Class<?> clazz) {
		if (clazz == java.util.Date.class) {
			return "\"" + formatDate(date, "yyyy-MM-dd HH:mm:ss") + "\"";
		}

		for (Field field : clazz.getDeclaredFields()) {
			if (field.isAnnotationPresent(JsonDate.class)) {
				JsonDate annotation = field.getAnnotation(JsonDate.class);
				return "\"" + formatDate(date, annotation.pattern()) + "\"";
			}
		}

		return "\"" + formatDate(date, "yyyy-MM-dd HH:mm:ss") + "\"";
	}

	/**
	 * Formats a date according to the given pattern.
	 *
	 * @param date    the date to format
	 * @param pattern the format pattern (e.g., "yyyy-MM-dd")
	 * @return the formatted date string
	 */
	private static String formatDate(java.util.Date date, String pattern) {
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(pattern);
		return sdf.format(date);
	}

	/**
	 * Escapes special characters in a string to ensure valid JSON output.
	 *
	 * @param str the original string
	 * @return a JSON-safe string with escaped characters
	 */
	private static String escapeJson(String str) {
		return str.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\b", "\\b")
				.replace("\f", "\\f")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}
}
