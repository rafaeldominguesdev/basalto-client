package com.basalto.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.basalto.Basalto;
import com.basalto.module.Module;
import com.basalto.module.ModuleManager;
import com.basalto.setting.Setting;
import com.basalto.ui.Theme;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Salva tudo em .minecraft/config/basalto.json: preferências do client, cada
 * módulo (ligado, tecla, opções) e os perfis. Perfis exportados ficam em
 * .minecraft/config/basalto/perfis/.
 */
public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final int MAX_NAME = 24;

	private final Path file = FabricLoader.getInstance().getConfigDir().resolve("basalto.json");
	public final Path profilesDir = FabricLoader.getInstance().getConfigDir().resolve("basalto").resolve("perfis");
	public ClientSettings client = new ClientSettings();
	/** Perfil -> estado dos módulos naquele perfil. */
	public final Map<String, JsonObject> profiles = new LinkedHashMap<>();
	/** Perfil -> última alteração (epoch ms). */
	public final Map<String, Long> profileModified = new LinkedHashMap<>();
	public String activeProfile = "Padrão";
	/** Perfil carregado ao abrir o jogo; null = continua de onde parou. */
	public String defaultProfile;

	private boolean dirty;
	private int dirtyTicks;
	private boolean loading;
	private long lastSave;
	private int dirtyCheckFrame = -1;
	private boolean profileDirtyCache;

	public void markDirty() {
		if (!loading) dirty = true;
	}

	public boolean isSaving() {
		return dirty;
	}

	public long lastSave() {
		return lastSave;
	}

	/** Salva alguns ticks depois da última mudança, para não escrever no disco a cada clique. */
	public void tick() {
		if (!dirty) return;
		if (++dirtyTicks >= 30) save();
	}

	// ================================================================ perfis

	public void createDefaultProfiles(ModuleManager mm) {
		profiles.put("Padrão", presetProfile(mm, "Sprint", "Fullbright", "ESP"));
		profiles.put("PvP", presetProfile(mm, "KillAura", "Velocity", "AutoTotem", "Sprint", "ESP", "Reach"));
		profiles.put("Survival", presetProfile(mm, "AutoEat", "AutoTool", "Fullbright", "Sprint", "NoFall", "AutoRespawn"));
		long now = System.currentTimeMillis();
		for (String n : profiles.keySet()) profileModified.put(n, now);
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

	/** Grava o estado atual dos módulos no perfil (cria se não existir). */
	public void saveProfile(String name) {
		profiles.put(name, modulesToJson(Basalto.modules()));
		profileModified.put(name, System.currentTimeMillis());
		activeProfile = name;
		dirtyCheckFrame = -1;
		save();
	}

	public void loadProfile(String name) {
		JsonObject p = profiles.get(name);
		if (p == null) return;
		activeProfile = name;
		modulesFromJson(Basalto.modules(), p, false);
		dirtyCheckFrame = -1;
		save();
	}

	/** Motivo para o nome não servir, ou null se está ok. */
	public String validateName(String name, String ignore) {
		String n = name.trim();
		if (n.isEmpty()) return "Dê um nome ao perfil.";
		if (n.length() > MAX_NAME) return "Use até " + MAX_NAME + " caracteres.";
		if (n.matches(".*[\\\\/:*?\"<>|].*")) return "Não use \\ / : * ? \" < > |";
		for (String existing : profiles.keySet())
			if (existing.equalsIgnoreCase(n) && !existing.equals(ignore)) return "Já existe um perfil com esse nome.";
		return null;
	}

	public String uniqueName(String base) {
		String n = base.length() > MAX_NAME ? base.substring(0, MAX_NAME) : base;
		if (!profiles.containsKey(n)) return n;
		for (int i = 2; ; i++) {
			String suffix = " " + i;
			String cand = (n.length() + suffix.length() > MAX_NAME ? n.substring(0, MAX_NAME - suffix.length()) : n) + suffix;
			if (!profiles.containsKey(cand)) return cand;
		}
	}

	public void createProfile(String name) {
		saveProfile(name.trim());
	}

	public String duplicateProfile(String name) {
		JsonObject p = profiles.get(name);
		if (p == null) return null;
		String copy = uniqueName(name + " (cópia)");
		profiles.put(copy, p.deepCopy());
		profileModified.put(copy, System.currentTimeMillis());
		save();
		return copy;
	}

	public void renameProfile(String from, String to) {
		to = to.trim();
		if (!profiles.containsKey(from) || from.equals(to)) return;
		Map<String, JsonObject> copy = new LinkedHashMap<>(profiles);
		profiles.clear();
		for (Map.Entry<String, JsonObject> e : copy.entrySet()) profiles.put(e.getKey().equals(from) ? to : e.getKey(), e.getValue());
		Long mod = profileModified.remove(from);
		profileModified.put(to, mod == null ? System.currentTimeMillis() : mod);
		if (from.equals(activeProfile)) activeProfile = to;
		if (from.equals(defaultProfile)) defaultProfile = to;
		save();
	}

	public boolean deleteProfile(String name) {
		if (profiles.size() <= 1 || !profiles.containsKey(name)) return false;
		profiles.remove(name);
		profileModified.remove(name);
		if (name.equals(defaultProfile)) defaultProfile = null;
		if (name.equals(activeProfile)) activeProfile = profiles.keySet().iterator().next();
		dirtyCheckFrame = -1;
		save();
		return true;
	}

	public void setDefaultProfile(String name) {
		defaultProfile = name;
		save();
	}

	public int enabledIn(String name) {
		JsonObject p = profiles.get(name);
		if (p == null) return 0;
		int n = 0;
		for (Map.Entry<String, JsonElement> e : p.entrySet()) {
			JsonObject o = e.getValue().getAsJsonObject();
			if (o.has("enabled") && o.get("enabled").getAsBoolean()) n++;
		}
		return n;
	}

	/** O estado atual dos módulos é diferente do perfil ativo? (recalcula a cada poucos frames) */
	public boolean activeProfileChanged(int frame) {
		if (dirtyCheckFrame >= 0 && frame - dirtyCheckFrame < 10 && frame >= dirtyCheckFrame) return profileDirtyCache;
		dirtyCheckFrame = frame;
		JsonObject p = profiles.get(activeProfile);
		profileDirtyCache = p != null && !sameState(modulesToJson(Basalto.modules()), p);
		return profileDirtyCache;
	}

	private static boolean sameState(JsonObject a, JsonObject b) {
		for (Map.Entry<String, JsonElement> e : a.entrySet()) {
			if (!b.has(e.getKey())) return false;
			JsonObject x = e.getValue().getAsJsonObject(), y = b.getAsJsonObject(e.getKey());
			for (String k : new String[] {"enabled", "key", "hud", "hold"}) {
				JsonElement xa = x.get(k), ya = y.get(k);
				if (xa == null || ya == null) {
					if (k.equals("hold") && (xa == null || !xa.getAsBoolean()) && (ya == null || !ya.getAsBoolean())) continue;
					if (xa != ya) return false;
					continue;
				}
				if (xa.isJsonPrimitive() && xa.getAsJsonPrimitive().isNumber()) {
					if (xa.getAsDouble() != ya.getAsDouble()) return false;
				} else if (!xa.equals(ya)) return false;
			}
			JsonObject xs = x.getAsJsonObject("settings"), ys = y.getAsJsonObject("settings");
			if (xs == null || ys == null) continue;
			for (Map.Entry<String, JsonElement> s : xs.entrySet()) {
				JsonElement o = ys.get(s.getKey());
				if (o == null) return false;
				JsonElement v = s.getValue();
				if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber() && o.isJsonPrimitive()) {
					if (Math.abs(v.getAsDouble() - o.getAsDouble()) > 1e-9) return false;
				} else if (!v.equals(o)) return false;
			}
		}
		return true;
	}

	// ---------- exportar / importar

	public Path exportProfile(String name) throws IOException {
		JsonObject p = profiles.get(name);
		if (p == null) throw new IOException("Perfil não existe");
		Files.createDirectories(profilesDir);
		JsonObject out = new JsonObject();
		out.addProperty("basalto", "perfil");
		out.addProperty("name", name);
		out.addProperty("exported", System.currentTimeMillis());
		out.add("modules", p);
		Path f = profilesDir.resolve(name + ".json");
		Files.writeString(f, GSON.toJson(out), StandardCharsets.UTF_8);
		return f;
	}

	public List<Path> importable() {
		List<Path> out = new ArrayList<>();
		if (!Files.isDirectory(profilesDir)) return out;
		try (Stream<Path> s = Files.list(profilesDir)) {
			s.filter(f -> f.getFileName().toString().toLowerCase().endsWith(".json")).sorted().forEach(out::add);
		} catch (IOException e) {
			Basalto.LOGGER.warn("Não deu pra listar {}", profilesDir, e);
		}
		return out;
	}

	/** Importa um perfil exportado. Devolve o nome que ele ganhou. */
	public String importProfile(Path f) throws IOException {
		JsonObject root;
		try {
			root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception e) {
			throw new IOException("O arquivo não é um JSON válido.");
		}
		if (!root.has("modules") || !root.get("modules").isJsonObject()) throw new IOException("O arquivo não é um perfil do Basalto.");
		String base = root.has("name") ? root.get("name").getAsString() : f.getFileName().toString().replaceFirst("\\.json$", "");
		base = base.replaceAll("[\\\\/:*?\"<>|]", "").trim();
		if (base.isEmpty()) base = "Importado";
		String name = uniqueName(base);
		profiles.put(name, root.getAsJsonObject("modules"));
		profileModified.put(name, System.currentTimeMillis());
		save();
		return name;
	}

	// ================================================================ módulos <-> json

	private JsonObject modulesToJson(ModuleManager mm) {
		JsonObject mods = new JsonObject();
		for (Module m : mm.all()) {
			JsonObject o = new JsonObject();
			o.addProperty("enabled", m.isEnabled());
			o.addProperty("key", m.key);
			o.addProperty("hud", m.showInHud);
			if (m.holdKey) o.addProperty("hold", true);
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
			m.holdKey = o.has("hold") && o.get("hold").getAsBoolean();
			if (o.has("settings")) {
				JsonObject s = o.getAsJsonObject("settings");
				for (Setting<?> st : m.settings)
					if (s.has(st.name)) st.fromJson(s.get(st.name));
			}
			if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean(), notify);
		}
	}

	// ================================================================ disco

	public void load(ModuleManager mm) {
		createDefaultProfiles(mm);
		if (!Files.exists(file)) {
			loading = true;
			modulesFromJson(mm, profiles.get("Padrão"), false);
			setDefaultKeys(mm);
			profiles.put("Padrão", modulesToJson(mm));
			loading = false;
			save();
			return;
		}
		loading = true;
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
			if (root.has("client")) {
				JsonObject c = root.getAsJsonObject("client");
				try {
					ClientSettings read = GSON.fromJson(c, ClientSettings.class);
					if (read != null) client = read;
				} catch (Exception e) {
					Basalto.LOGGER.warn("Preferências do client inválidas, usando o padrão", e);
				}
				if (client.favorites == null) client.favorites = new java.util.LinkedHashSet<>();
				if (client.hud == null) client.hud = new ClientSettings().hud;
				if (client.uiSize == null) client.uiSize = ClientSettings.UiSize.MEDIO;
				if (client.motion == null) client.motion = com.basalto.ui.Anim.Motion.FULL;
				// Config da interface antiga: adota a cor e a opacidade novas.
				if (!c.has("uiVersion")) {
					client.accent = Theme.ACCENTS[0];
					client.opacity = 90;
					client.uiVersion = 2;
				}
			}
			if (root.has("profiles")) {
				profiles.clear();
				JsonObject p = root.getAsJsonObject("profiles");
				for (Map.Entry<String, JsonElement> e : p.entrySet())
					profiles.put(e.getKey(), e.getValue().getAsJsonObject());
			}
			if (root.has("profileModified")) {
				JsonObject pm = root.getAsJsonObject("profileModified");
				for (Map.Entry<String, JsonElement> e : pm.entrySet()) profileModified.put(e.getKey(), e.getValue().getAsLong());
			}
			if (root.has("activeProfile")) activeProfile = root.get("activeProfile").getAsString();
			if (root.has("defaultProfile")) defaultProfile = root.get("defaultProfile").getAsString();
			if (!profiles.containsKey(activeProfile) && !profiles.isEmpty()) activeProfile = profiles.keySet().iterator().next();
			if (defaultProfile != null && !profiles.containsKey(defaultProfile)) defaultProfile = null;
			if (root.has("modules")) modulesFromJson(mm, root.getAsJsonObject("modules"), false);
			if (defaultProfile != null) {
				activeProfile = defaultProfile;
				modulesFromJson(mm, profiles.get(defaultProfile), false);
			}
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

	/** Volta as preferências do client ao padrão (módulos e perfis ficam). */
	public void resetClientSettings() {
		ClientSettings fresh = new ClientSettings();
		fresh.favorites = client.favorites;
		client = fresh;
		save();
	}

	public Path file() {
		return file;
	}

	public void save() {
		dirty = false;
		dirtyTicks = 0;
		ModuleManager mm = Basalto.modules();
		if (mm == null) return;
		JsonObject root = new JsonObject();
		root.add("client", GSON.toJsonTree(client));
		root.add("modules", modulesToJson(mm));
		JsonObject p = new JsonObject();
		for (Map.Entry<String, JsonObject> e : profiles.entrySet()) p.add(e.getKey(), e.getValue());
		root.add("profiles", p);
		JsonObject pm = new JsonObject();
		for (Map.Entry<String, Long> e : profileModified.entrySet()) pm.addProperty(e.getKey(), e.getValue());
		root.add("profileModified", pm);
		root.addProperty("activeProfile", activeProfile);
		if (defaultProfile != null) root.addProperty("defaultProfile", defaultProfile);
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
			lastSave = System.currentTimeMillis();
		} catch (IOException e) {
			Basalto.LOGGER.error("Não deu pra salvar basalto.json", e);
		}
	}
}
