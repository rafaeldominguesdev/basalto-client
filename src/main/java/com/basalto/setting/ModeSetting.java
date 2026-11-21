package com.basalto.setting;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ModeSetting extends Setting<String> {
	public final List<String> modes;

	public ModeSetting(String name, String defaultValue, String... modes) {
		super(name, defaultValue);
		this.modes = List.of(modes);
	}

	public boolean is(String mode) {
		return value.equals(mode);
	}

	public void cycle(boolean forward) {
		int i = modes.indexOf(value);
		int n = modes.size();
		i = forward ? (i + 1) % n : (i - 1 + n) % n;
		value = modes.get(i);
	}

	@Override
	public String display() {
		return name + ": " + value;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json != null && json.isJsonPrimitive() && modes.contains(json.getAsString())) value = json.getAsString();
	}
}
