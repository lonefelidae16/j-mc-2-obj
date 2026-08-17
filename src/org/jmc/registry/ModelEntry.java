package org.jmc.registry;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import com.google.gson.*;
import org.jmc.geom.Vertex;

public class ModelEntry extends RegistryEntry {
	public RegistryModel model;
	private RegistryModel generatedModel;
	
	protected ModelEntry(NamespaceID id) {
		super(id);
	}

	public static ModelEntry parseJson(NamespaceID id, JsonObject json) {
		ModelEntry entry = new ModelEntry(id);
		entry.model = RegistryModel.CONVERTER.fromJson(json, RegistryModel.class);
		if (entry.model.parent != null) {
			entry.model.parentEntry = Registries.getModel(entry.model.parent);
		}
		return entry;
	}
	
	public RegistryModel generateModel() {
		//fast path
		if (generatedModel != null) {
			return generatedModel;
		}
		synchronized (this) {
			if (generatedModel != null) {
				return generatedModel;
			}
			RegistryModel newModel = new Gson().fromJson(new Gson().toJson(model), model.getClass());// clone via serialisation
			newModel.parentEntry = model.parentEntry;
			if (newModel.parentEntry != null) {
				newModel.parentEntry.propagateToChild(newModel);
			}
			generatedModel = newModel;
			return generatedModel;
		}
	}
	
	private void propagateToChild(RegistryModel childModel) {
		if (childModel.elements == null) {
			childModel.elements = model.elements;
		}
		for (Entry<String, String> textureEntry : model.textures.entrySet()) {
			if (textureEntry.getValue().startsWith("#")) {
				String refName = textureEntry.getValue().substring(1);
				String refTex = childModel.textures.get(refName);
				assert refTex != null;
				childModel.textures.putIfAbsent(textureEntry.getKey(), refTex);
			} else {
				childModel.textures.putIfAbsent(textureEntry.getKey(), textureEntry.getValue());
			}
		}
		if (model.parentEntry != null) {
			model.parentEntry.propagateToChild(childModel);
		}
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
	public static class RegistryModel {
		private static final Gson CONVERTER = new GsonBuilder().registerTypeAdapter(RegistryModel.class, new RegistryModelConverter()).create();
		private transient ModelEntry parentEntry;
		private NamespaceID parent;
		@Nonnull
		public Map<String, String> textures = new HashMap<>();
		public List<ModelElement> elements;
		
		private RegistryModel() {
		}
		
		@Override
		public String toString() {
			return new Gson().toJson(this);
		}
		
		public static class ModelElement {
			public Vertex from;
			public Vertex to;
			public ElementRotation rotation;
			@Nonnull
			public Map<String, ElementFace> faces = new HashMap<>();
			
			@Override
			public String toString() {
				return new Gson().toJson(this);
			}
			
			public static class ElementRotation {
				public Vertex origin;
				public String axis;
				public float angle = 0;
				public boolean rescale = false;
				@Override
				public String toString() {
					return new Gson().toJson(this);
				}
			}
			
			public static class ElementFace {
				public float[] uv;
				public String texture;
				public String cullface;
				public int rotation;
				public float tintindex;
				@Override
				public String toString() {
					return new Gson().toJson(this);
				}
			}
		}
	}

	static class RegistryModelConverter implements JsonDeserializer<RegistryModel> {
		@Override
		public RegistryModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject object = json.getAsJsonObject();
			RegistryModel result = new RegistryModel();
			if (object.has("parent")) {
				result.parent = NamespaceID.fromString(object.get("parent").getAsString());
			}
			if (object.has("textures")) {
				for (Entry<String, JsonElement> entry : object.getAsJsonObject("textures").entrySet()) {
					JsonElement elem = entry.getValue();
					if (elem.isJsonObject()) {
						JsonObject texObj = elem.getAsJsonObject();
						if (texObj.has("sprite")) {
							result.textures.put(entry.getKey(), texObj.get("sprite").getAsString());
						}
					} else {
						result.textures.put(entry.getKey(), elem.getAsString());
					}
				}
			}
			if (object.has("elements")) {
				result.elements = new ArrayList<>();
				for (JsonElement element : object.get("elements").getAsJsonArray()) {
					result.elements.add(context.deserialize(element, RegistryModel.ModelElement.class));
				}
			}
			return result;
		}
	}
	
}
