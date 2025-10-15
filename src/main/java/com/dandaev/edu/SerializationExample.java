package com.dandaev.edu;

import com.dandaev.edu.entities.Product;
import com.dandaev.edu.json.serializer.JsonSerializer;

// Пример использования сериализации
public class SerializationExample {
	public static void main(String[] args) throws Exception {
		Product product = new Product(1L, "Laptop", 999.99, new java.util.Date());

		String json = JsonSerializer.serialize(product);

		System.out.println("Serialized JSON:");
		System.out.println(json);
	}
}
