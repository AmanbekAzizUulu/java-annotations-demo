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
 * Класс {@code JsonSerializer} предоставляет функциональность сериализации
 * объектов Java в строки формата JSON с использованием пользовательских аннотаций.
 *
 * <p>Поддерживаются следующие аннотации:
 * <ul>
 *   <li>{@link JsonSerializable} - помечает класс, допустимый для сериализации</li>
 *   <li>{@link JsonField} - указывает имя поля в JSON и возможность игнорирования</li>
 *   <li>{@link JsonDate} - задаёт формат сериализации для полей типа {@link java.util.Date}</li>
 * </ul>
 *
 * <p>Поддерживаемые типы данных:
 * <ul>
 *   <li>Примитивные типы и их обёртки ({@link Number}, {@link Boolean})</li>
 *   <li>{@link String}</li>
 *   <li>{@link java.util.Date}</li>
 *   <li>Массивы и {@link Collection}</li>
 *   <li>{@link Map}</li>
 *   <li>Пользовательские объекты, аннотированные {@link JsonSerializable}</li>
 * </ul>
 *
 * <p><b>Пример использования:</b>
 * <pre>{@code
 * @JsonSerializable
 * public class User {
 *     @JsonField(name = "user_name")
 *     private String name;
 *
 *     @JsonDate(pattern = "yyyy-MM-dd")
 *     private Date birthDate;
 * }
 *
 * String json = JsonSerializer.serialize(new User("Alice", new Date()));
 * System.out.println(json);
 * }</pre>
 *
 * @author Amanbek
 * @version 1.0
 */
public class JsonSerializer {

    /**
     * Сериализует объект Java в строку JSON.
     *
     * <p>Поддерживает примитивные типы, строки, массивы, коллекции, отображения
     * и объекты, аннотированные {@link JsonSerializable}.
     *
     * @param obj объект для сериализации
     * @return JSON-представление объекта
     * @throws IllegalAccessException если доступ к полю ограничен
     * @throws IllegalArgumentException если класс не аннотирован {@link JsonSerializable}
     */
    public static String serialize(Object obj) throws IllegalAccessException {
        if (obj == null)
            return "null";

        Class<?> clazz = obj.getClass();

        // Обработка примитивных типов и строк до проверки аннотации
        if (clazz.isPrimitive() || obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }

        if (obj instanceof String) {
            return "\"" + escapeJson(obj.toString()) + "\"";
        }

        if (obj instanceof java.util.Date) {
            return serializeDate((java.util.Date) obj, clazz);
        }

        // Массивы
        if (clazz.isArray()) {
            return serializeArray(obj);
        }

        // Коллекции
        if (obj instanceof Collection) {
            return serializeCollection((Collection<?>) obj);
        }

        // Отображения
        if (obj instanceof Map) {
            return serializeMap((Map<?, ?>) obj);
        }

        // Только пользовательские классы требуют аннотации
        if (!clazz.isAnnotationPresent(JsonSerializable.class)) {
            throw new IllegalArgumentException("Class not annotated with @JsonSerializable: " + clazz.getName());
        }

        return serializeObject(obj);
    }

    /**
     * Сериализует пользовательский объект в строку JSON.
     *
     * <p>Включаются только поля, аннотированные {@link JsonField}.
     * Поля, помеченные как {@code ignore = true}, пропускаются.
     * Если поле аннотировано {@link JsonDate}, его значение
     * форматируется согласно указанному шаблону даты.
     *
     * @param obj объект для сериализации
     * @return JSON-представление объекта
     * @throws IllegalAccessException если доступ к полю запрещён
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
                    continue; // Пропустить null значения
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
     * Сериализует {@link Map} в JSON-объект.
     * Ключи преобразуются в строки с помощью {@code toString()}.
     *
     * @param map отображение для сериализации
     * @return JSON-представление отображения
     * @throws IllegalAccessException если доступ к значению ограничен
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
     * Сериализует массив в JSON-массив.
     *
     * @param array массив для сериализации
     * @return JSON-представление массива
     * @throws IllegalAccessException если доступ к элементу ограничен
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
     * Сериализует {@link Collection} в JSON-массив.
     *
     * @param collection коллекция для сериализации
     * @return JSON-представление коллекции
     * @throws IllegalAccessException если доступ к элементу ограничен
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
     * Сериализует {@link java.util.Date} с использованием формата,
     * определённого в {@link JsonDate}, если аннотация присутствует
     * в объявляющем поле или классе.
     *
     * @param date дата для сериализации
     * @param clazz объявляющий класс (используется для поиска шаблона даты)
     * @return отформатированная строка даты в кавычках для JSON
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
     * Форматирует дату согласно заданному шаблону.
     *
     * @param date дата для форматирования
     * @param pattern шаблон формата (например, "yyyy-MM-dd")
     * @return отформатированная строка даты
     */
    private static String formatDate(java.util.Date date, String pattern) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    /**
     * Экранирует специальные символы в строке для обеспечения корректного вывода JSON.
     *
     * @param str исходная строка
     * @return безопасная для JSON строка с экранированными символами
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
