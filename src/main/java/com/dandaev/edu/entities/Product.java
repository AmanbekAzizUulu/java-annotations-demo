package com.dandaev.edu.entities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dandaev.edu.annotations.jsonparser.JsonDate;
import com.dandaev.edu.annotations.jsonparser.JsonField;
import com.dandaev.edu.annotations.jsonparser.JsonSerializable;

@JsonSerializable
public class Product {
	@JsonField(name = "product_id")
	private Long id;

	@JsonField(name = "product_name")
	private String name;

	@JsonField
	private Double price;

	@JsonField(ignore = true)
	private String internalCode;

	@JsonField
	@JsonDate(pattern = "yyyy-MM-dd")
	private java.util.Date createdAt;

	@JsonField(name = "tags")
	private List<String> tags;

	@JsonField(name = "metadata")
	private Map<String, Object> metadata;

	public Product(Long id, String name, Double price, java.util.Date createdAt) {
		this.id = id;
		this.name = name;
		this.price = price;
		this.internalCode = "SECRET123";
		this.createdAt = createdAt;
		this.tags = Arrays.asList("electronics", "tech");
		this.metadata = new HashMap<>();
		metadata.put("weight", 1.5);
		metadata.put("dimensions", new int[] { 10, 20, 30 });
	}

	// Геттеры
	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Double getPrice() {
		return price;
	}

	public String getInternalCode() {
		return internalCode;
	}

	public java.util.Date getCreatedAt() {
		return createdAt;
	}

	public List<String> getTags() {
		return tags;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}
}
