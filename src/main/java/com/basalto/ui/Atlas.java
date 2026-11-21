package com.basalto.ui;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.basalto.Basalto;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * O atlas da interface (textures/ui/atlas.png): formas arredondadas 1:1 em
 * pixels, a sombra e os ícones em três resoluções. Gerado por tools/ui-assets.
 */
public final class Atlas {
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Basalto.MOD_ID, "textures/ui/atlas.png");
	private static final Identifier META = Identifier.fromNamespaceAndPath(Basalto.MOD_ID, "textures/ui/atlas.json");
	public static final int[] ICON_SIZES = {24, 40, 64};

	/** Posição de um sprite no atlas, em texels. */
	public record Sprite(int x, int y, int w, int h) {}

	private static Map<String, Sprite> sprites;
	private static int texW = 2048, texH = 1024;
	public static int maxRadius = 48;
	public static int density = 4;
	public static int shadowRadius = 8, shadowBlur = 16;

	private Atlas() {}

	private static void load() {
		sprites = new HashMap<>();
		try {
			var res = Minecraft.getInstance().getResourceManager().getResource(META);
			if (res.isEmpty()) {
				Basalto.LOGGER.error("atlas.json não encontrado");
				return;
			}
			try (Reader r = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
				JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
				JsonArray size = o.getAsJsonArray("size");
				texW = size.get(0).getAsInt();
				texH = size.get(1).getAsInt();
				density = o.get("density").getAsInt();
				maxRadius = o.get("maxRadius").getAsInt();
				JsonObject sh = o.getAsJsonObject("shadow");
				shadowRadius = sh.get("radius").getAsInt();
				shadowBlur = sh.get("blur").getAsInt();
				for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("sprites").entrySet()) {
					JsonArray a = e.getValue().getAsJsonArray();
					sprites.put(e.getKey(), new Sprite(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(), a.get(3).getAsInt()));
				}
			}
		} catch (Exception e) {
			Basalto.LOGGER.error("Não deu pra ler o atlas da interface", e);
		}
	}

	public static Sprite get(String name) {
		if (sprites == null) load();
		return sprites.get(name);
	}

	/** Ícone na menor resolução que cobre o tamanho pedido em pixels. */
	public static Sprite icon(String name, float sizePx) {
		for (int s : ICON_SIZES) {
			if (s >= sizePx * 0.95f || s == ICON_SIZES[ICON_SIZES.length - 1]) {
				Sprite sp = get("icon/" + name + "@" + s);
				if (sp != null) return sp;
			}
		}
		return null;
	}

	public static float u(float texel) {
		return texel / texW;
	}

	public static float v(float texel) {
		return texel / texH;
	}

	public static GpuTextureView view() {
		return Minecraft.getInstance().getTextureManager().getTexture(TEXTURE).getTextureView();
	}

	public static GpuSampler sampler() {
		return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
	}
}
