package com.basalto.setting;

import com.google.gson.JsonElement;

/** Uma opção configurável de um módulo (aparece no menu ao clicar com o botão direito). */
public abstract class Setting<T> {
	public final String name;
	protected T value;
	protected final T defaultValue;

	protected Setting(String name, T defaultValue) {
		this.name = name;
		this.value = defaultValue;
		this.defaultValue = defaultValue;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = value;
	}

	public void reset() {
		this.value = defaultValue;
	}

	public boolean isDefault() {
		return java.util.Objects.equals(value, defaultValue);
	}

	/** Texto mostrado no menu, ex.: "Alcance: 4.2". */
	public abstract String display();

	public abstract JsonElement toJson();

	public abstract void fromJson(JsonElement json);
}
