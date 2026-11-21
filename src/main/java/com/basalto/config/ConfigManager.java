package com.basalto.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.basalto.Basalto;
import com.basalto.module.Module;
import com.basalto.module.ModuleManager;
import com.basalto.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Salva tudo em .minecraft/config/basalto.json: aparência, HUD, cada módulo
 * (ligado, tecla, opções) e os perfis de config (Padrão, PvP, Survival...).
 */
public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path file = FabricLoader.getInstance().getConfigDir().resolve("basalto.json");
	public final ClientSettings client = new ClientSettings();
	/** Perfil -> estado dos módulos naquele perfil. */
	public final Map<String, JsonObject> profiles = new LinkedHashMap<>();
	public String activeProfile = "Padrão";
	/** Posição de cada painel do menu (nome -> x,y). */
	public final Map<String, int[]> panelPos = new LinkedHashMap<>();

	private boolean dirty;
	private int dirtyTicks;
	private boolean loading;

	public void markDirty() {
		if (!loading) dirty = true;
	}

	/** Salva alguns ticks depois da última mudança, para não escrever no disco a cada clique. */
	public void tick() {
		if (!dirty) return;
		if (++dirtyTicks >= 40) save();
	}

	// ---------- perfis ----------

	public void createDefaultProfiles(ModuleManager mm) {
		profiles.put("Padrão", presetProfile(mm, "Sprint", "Fullbright", "ESP"));
		profiles.put("PvP", presetProfile(mm, "KillAura", "Velocity", "AutoTotem", "Sprint", "ESP", "Reach"));
		profiles.put("Survival", presetProfile(mm, "AutoEat", "AutoTool", "Fullbright", "Sprint", "NoFall", "AutoRespawn"));
	}

	private JsonObject presetProfile(ModuleManager mm, String... on) {
		JsonObject mods = modulesToJson(mm);
		for (Map.Entry<String, JsonElement> e : mods.entrySet()) {
			boolean enable = false;
			for (String n : on) if (n.equalsIgnoreCase(e.getKey())) enable = true;
			e.getValue().getAsJsonObject().addProperty("enabled", enable);
		}
		return mods;
	}

	public void saveProfile(String name) {
		profiles.put(name, modulesToJson(Basalto.modules()));
		activeProfile = name;
		save();
	}

	public void loadProfile(String name) {
		JsonObject p = profiles.get(name);
		if (p == null) return;
		activeProfile = name;
		modulesFromJson(Basalto.modules(), p, true);
		save();
	}

	// ---------- módulos <-> json ----------

	private JsonObject modulesToJson(ModuleManager mm) {
		JsonObject mods = new JsonObject();
		for (Module m : mm.all()) {
			JsonObject o = new JsonObject();
			o.addProperty("enabled", m.isEnabled());
			o.addProperty("key", m.key);
			o.addProperty("hud", m.showInHud);
			JsonObject s = new JsonObject();
			for (Setting<?> st : m.settings) s.add(st.name, st.toJson());
			o.add("settings", s);
			mods.add(m.name, o);
		}
		return mods;
	}

	private void modulesFromJson(ModuleManager mm, JsonObject mods, boolean notify) {
		for (Module m : mm.all()) {
			if (!mods.has(m.name)) continue;
			JsonObject o = mods.getAsJsonObject(m.name);
			if (o.has("key")) m.key = o.get("key").getAsInt();
			if (o.has("hud")) m.showInHud = o.get("hud").getAsBoolean();
			if (o.has("settings")) {
				JsonObject s = o.getAsJsonObject("settings");
				for (Setting<?> st : m.settings)
					if (s.has(st.name)) st.fromJson(s.get(st.name));
			}
			if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean(), notify);
		}
	}

	// ---------- disco ----------

	public void load(ModuleManager mm) {
		createDefaultProfiles(mm);
		if (!Files.exists(file)) {
			loading = true;
			modulesFromJson(mm, profiles.get("Padrão"), false);
			setDefaultKeys(mm);
			loading = false;
			save();
			return;
		}
		loading = true;
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
			if (root.has("client")) {
				JsonObject c = root.getAsJsonObject("client");
				if (c.has("accent")) client.accent = c.get("accent").getAsInt();
				if (c.has("opacity")) client.opacity = c.get("opacity").getAsInt();
				if (c.has("blur")) client.blur = c.get("blur").getAsBoolean();
				if (c.has("menuKey")) client.menuKey = c.get("menuKey").getAsInt();
				if (c.has("hud")) {
					JsonObject h = c.getAsJsonObject("hud");
					for (String id : ClientSettings.HUD_LABELS.keySet())
						if (h.has(id)) client.hud.put(id, h.get(id).getAsBoolean());
				}
			}
			if (root.has("profiles")) {
				JsonObject p = root.getAsJsonObject("profiles");
				for (Map.Entry<String, JsonElement> e : p.entrySet())
					profiles.put(e.getKey(), e.getValue().getAsJsonObject());
			}
			if (root.has("activeProfile")) activeProfile = root.get("activeProfile").getAsString();
			if (root.has("panels")) {
				JsonObject p = root.getAsJsonObject("panels");
				for (Map.Entry<String, JsonElement> e : p.entrySet()) {
					var arr = e.getValue().getAsJsonArray();
					panelPos.put(e.getKey(), new int[] {arr.get(0).getAsInt(), arr.get(1).getAsInt()});
				}
			}
			if (root.has("modules")) modulesFromJson(mm, root.getAsJsonObject("modules"), false);
		} catch (Exception e) {
			Basalto.LOGGER.error("Não deu pra ler basalto.json, usando o padrão", e);
		} finally {
			loading = false;
		}
	}

	private void setDefaultKeys(ModuleManager mm) {
		bind(mm, "KillAura", org.lwjgl.glfw.GLFW.GLFW_KEY_R);
		bind(mm, "Fly", org.lwjgl.glfw.GLFW.GLFW_KEY_F);
		bind(mm, "ESP", org.lwjgl.glfw.GLFW.GLFW_KEY_G);
		bind(mm, "Fullbright", org.lwjgl.glfw.GLFW.GLFW_KEY_B);
		bind(mm, "XRay", org.lwjgl.glfw.GLFW.GLFW_KEY_X);
		bind(mm, "Zoom", org.lwjgl.glfw.GLFW.GLFW_KEY_C);
	}

	private void bind(ModuleManager mm, String name, int key) {
		Module m = mm.find(name);
		if (m != null) m.key = key;
	}

	public void save() {
		dirty = false;
		dirtyTicks = 0;
		ModuleManager mm = Basalto.modules();
		if (mm == null) return;
		JsonObject root = new JsonObject();
		JsonObject c = new JsonObject();
		c.addProperty("accent", client.accent);
		c.addProperty("opacity", client.opacity);
		c.addProperty("blur", client.blur);
		c.addProperty("menuKey", client.menuKey);
		JsonObject h = new JsonObject();
		for (Map.Entry<String, Boolean> e : client.hud.entrySet()) h.addProperty(e.getKey(), e.getValue());
		c.add("hud", h);
		root.add("client", c);
		root.add("modules", modulesToJson(mm));
		JsonObject p = new JsonObject();
		for (Map.Entry<String, JsonObject> e : profiles.entrySet()) p.add(e.getKey(), e.getValue());
		root.add("profiles", p);
		root.addProperty("activeProfile", activeProfile);
		JsonObject panels = new JsonObject();
		for (Map.Entry<String, int[]> e : panelPos.entrySet()) {
			var arr = new com.google.gson.JsonArray();
			arr.add(e.getValue()[0]);
			arr.add(e.getValue()[1]);
			panels.add(e.getKey(), arr);
		}
		root.add("panels", panels);
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException e) {
			Basalto.LOGGER.error("Não deu pra salvar basalto.json", e);
		}
	}
}
