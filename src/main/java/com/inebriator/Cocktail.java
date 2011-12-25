package com.inebriator;

import java.util.Map;

public class Cocktail {

	private final String name;
	
	@Override
	public String toString() {
		return "Cocktail [name=" + name + ", ingredients=" + ingredients + "]";
	}

	private final Map<String, Integer> ingredients;
	
	public Cocktail(String name, Map<String, Integer> ingredients) {
		this.name = name;
		this.ingredients = ingredients;
	}
	
	public String getName() {
		return name;
	}
	
	public Map<String, Integer> getIngredients() {
		return ingredients;
	}
}
