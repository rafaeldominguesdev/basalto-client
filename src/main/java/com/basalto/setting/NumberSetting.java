package com.basalto.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
	public final double min;
	public final double max;
	public final double step;
	private final String suffix;

	public NumberSetting(String name, double defaultValue, double min, double max, double step) {
		this(name, defaultValue, min, max, step, "");
	}

	public NumberSetting(String name, double defaultValue, double min, double max, double step, String suffix) {
		super(name, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
		this.suffix = suffix;
	}

	public double getD() {
		return value;
	}

	public float getF() {
		return value.floatValue();
	}

	public int getI() {
		return (int) Math.round(value);
	}

	@Override
	public void set(Double v) {
		double clamped = Math.max(min, Math.min(max, v));
		double snapped = Math.round(clamped / step) * step;
		this.value = Math.max(min, Math.min(max, snapped));
	}

	/** Define o valor a partir de uma fração 0..1 (posição no slider). */
	public void setFraction(double f) {
		set(min + (max - min) * Math.max(0, Math.min(1, f)));
	}

	public double fraction() {
		return (value - min) / (max - min);
	}

	@Override
	public String display() {
		String num = step >= 1 ? String.valueOf(getI()) : String.format(java.util.Locale.ROOT, step >= 0.1 ? "%.1f" : "%.2f", value);
		return name + ": " + num + suffix;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json != null && json.isJsonPrimitive()) set(json.getAsDouble());
	}
}
