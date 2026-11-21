package com.basalto.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BoolSetting extends Setting<Boolean> {
	public BoolSetting(String name, boolean defaultValue) {
		super(name, defaultValue);
	}

	public boolean isOn() {
		return value;
	}

	public void toggle() {
		value = !value;
	}

	@Override
	public String display() {
		return name;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json != null && json.isJsonPrimitive()) value = json.getAsBoolean();
	}
}
