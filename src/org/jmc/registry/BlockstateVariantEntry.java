package org.jmc.registry;

import java.util.*;
import java.util.Map.Entry;

import org.jmc.Blockstate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jmc.geom.Direction;
import org.jmc.util.Log;

public class BlockstateVariantEntry extends BlockstateEntry {
	private ArrayList<HashMap<Blockstate, ModelListWeighted>> modelsList = new ArrayList<>();
	
	public BlockstateVariantEntry(NamespaceID name) {
		super(name);
	}

	public Optional<Blockstate> getDefaultState() {
		Set<Blockstate> keys = modelsList.get(0).keySet();
		if (id.path.equals("deepslate")) {
			return keys.stream().filter(state -> state.get("axis").equalsIgnoreCase("Y")).findFirst();
		}
		return keys.stream().findFirst();
	}

	@Override
	protected void parseJson(JsonElement variantsElem) {
		HashMap<Blockstate, ModelListWeighted> models = new HashMap<>();
		JsonObject variantsObj = variantsElem.getAsJsonObject();
		for (String variantKey : variantsObj.keySet()) {
			Blockstate blockstate = new Blockstate();
			blockstate.putAll(parseStateString(variantKey));
			ModelListWeighted stateModels = new ModelListWeighted();
			models.put(blockstate, stateModels);
			
			JsonElement variantElem = variantsObj.get(variantKey);
			if (variantElem.isJsonArray()) {
				JsonArray variantArr = variantElem.getAsJsonArray();
				for (JsonElement modelElem : variantArr) {
					ModelInfo model = new Gson().fromJson(modelElem, ModelInfo.class);
					stateModels.addModel(model);
				}
			} else {
				ModelInfo model = new Gson().fromJson(variantElem, ModelInfo.class);
				stateModels.addModel(model);
			}
		}
		modelsList.add(models);
	}
	
	public void addStates(JsonObject json) {
		JsonElement variantsElem = json.get("variants");
		if (variantsElem != null) {
			parseJson(variantsElem);
		}
	}
	
	@Override
	public List<ModelListWeighted> getModelsFor(Blockstate state) {
		List<ModelListWeighted> stateModels = new ArrayList<>();
		for (HashMap<Blockstate, ModelListWeighted> models : modelsList) {
			for (Entry<Blockstate, ModelListWeighted> modelEntry : models.entrySet()) {
				if (modelEntry.getKey().maskMatches(state)) {
					stateModels.add(modelEntry.getValue());
					return stateModels;// Only 1 matching state for variant type
				}
			}
		}
		return stateModels;
	}
	
	@Override
	public String toString() {
		return String.format("%s:%s", super.toString(), modelsList.toString());
	}
	
}
