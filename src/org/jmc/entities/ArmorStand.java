package org.jmc.entities;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.jmc.OBJInputFile;
import org.jmc.OBJInputFile.OBJGroup;
import org.jmc.NBT.NBT_Tag;
import org.jmc.NBT.TAG_Compound;
import org.jmc.NBT.TAG_Double;
import org.jmc.NBT.TAG_Float;
import org.jmc.NBT.TAG_List;
import org.jmc.NBT.TAG_String;
import org.jmc.geom.Transform;
import org.jmc.geom.Vertex;
import org.jmc.models.Head;
import org.jmc.registry.NamespaceID;
import org.jmc.threading.ChunkProcessor;
import org.jmc.util.Filesystem.JmcConfFile;
import org.jmc.util.Log;
import org.jmc.util.StaticInit;

public class ArmorStand extends Entity {

	public ArmorStand(@Nonnull String id) {
		super(id);
	}

	private Transform getTranslate(TAG_Compound entity, float x, float y, float z, float scale) {
		Vertex vert = Vertex.add(getPosition(entity), new Vertex(x, y, z));
		Transform translate = Transform.translation(vert.x, vert.y, vert.z);

		Transform tScale = Transform.scale(scale, scale, scale);

		translate = translate.multiply(tScale);

		return translate;
	}

	private Transform getTranslate(TAG_Compound entity) {
		return getTranslate(entity, 0, 0, 0, 1);
	}


	private Transform getRotate(TAG_Compound entity, float offset) {
		TAG_List rot = (TAG_List) entity.getElement("Rotation");
		float yaw=((TAG_Float)rot.getElement(0)).value;
		float pitch=((TAG_Float)rot.getElement(1)).value;
		Transform rotate = Transform.rotation2(yaw+offset, pitch ,0);
		return rotate;
	}

	private static final Set<String> armor_parts = new HashSet<>(
			Arrays.asList("helmet", "chestplate", "leggings", "boots")
	);

	private static final Map<String, ArmorTransform> ARMOR_TRANSFORM = StaticInit.make(new HashMap<>(), map -> {
		ArmorTransform head = new ArmorTransform(0, 1.71, 0, 1, 180);
		map.put("head", head);
		map.put("helmet", head);
		map.put("chest", new ArmorTransform(0, 1.14, 0.002, 1, 180));
		map.put("legs", new ArmorTransform(0, 0.778, 0.01, 1, 0));
		map.put("feet", new ArmorTransform(0, 0.194, -0.03, 1, 0));
	});

	@Override
	public void addEntity(ChunkProcessor obj, TAG_Compound entity) {
		model.addEntity(obj, getTranslate(entity).multiply(getRotate(entity, 90)));

		// equipment
		// Log.info("ArmorStand found: "+entity.getElement("Equipment"));
		List<NBT_Tag> Equipment = ((TAG_Compound) entity.getElement("equipment")).elements;

		for(NBT_Tag equip : Equipment) {
			TAG_Compound armor = (TAG_Compound) equip;
			String modelName = armor.getName();
			TAG_String item_id_tag = (TAG_String)armor.getElement("id");
			if (item_id_tag == null) {
				Log.debug("Armour stand "+getPosition(entity).toString()+" " + modelName + " not armed");
				continue;
			}
			NamespaceID item_id = NamespaceID.fromString(item_id_tag.value);
			String[] item_name_parts = item_id.path.split("_");
			String armor_material = null;
			if (item_name_parts.length == 2 && armor_parts.contains(item_name_parts[1])) {
				armor_material = item_name_parts[0];
			}

			TAG_Compound components = ((TAG_Compound)armor.getElement("components"));
			boolean bEnchanted = components != null && components.getElement("minecraft:enchantments") instanceof TAG_Compound;

			// internal: offset x, y, z, rotate, initialscale
			// blender:  offset ?, z, -y, rotate, initialscale
			ArmorTransform armorTransform = ARMOR_TRANSFORM.get(modelName);

			if (item_id.equals(new NamespaceID("minecraft", "player_head"))) {
				TAG_Compound tag = ((TAG_Compound)armor.getElement("tag"));
				Transform t = getTranslate(entity, 0, 1.64f, 0, 1).multiply(getRotate(entity, 180));
				Head.addPlayerHead(obj, t, tag, null);
				continue;
			}

			if (armor_material != null) {
				if (armor_material.equals("golden")) armor_material = "gold";
				// base material
				addArmor("conf/models/armor_" + modelName + ".obj",
						NamespaceID.fromString("entity/equipment/humanoid/" + armor_material),
						obj, entity,
						armorTransform.x,
						armorTransform.y,
						armorTransform.z,
						1 * armorTransform.scale,
						armorTransform.rotation);

				// leather overlay
				if (armor_material.equals("leather")) {
					addArmor("conf/models/armor_" + modelName + ".obj",
							NamespaceID.fromString("entity/equipment/humanoid/" + armor_material + "_overlay"),
							obj, entity,
							armorTransform.x,
							armorTransform.y,
							armorTransform.z,
							1.04 * armorTransform.scale,
							armorTransform.rotation);
				}

				// is enchanted
				if (bEnchanted) {
					addArmor("conf/models/armor_" + modelName + ".obj",
							NamespaceID.fromString("misc/enchanted_glint_armor"),
							obj, entity,
							armorTransform.x,
							armorTransform.y,
							armorTransform.z,
							1.08 * armorTransform.scale,
							armorTransform.rotation);
				}
			}

		}
	}

	public void addArmor(String objFileName, NamespaceID material, ChunkProcessor obj, TAG_Compound entity, double x, double y, double z, double scale, double rotation) {

		OBJInputFile objFile = new OBJInputFile();

		try (JmcConfFile objFileStream = new JmcConfFile(objFileName)) {
			objFile.loadFile(objFileStream, material.getExportSafeString());
		} catch (IOException e) {
			Log.error("Cant read Armor_Stand Equipment obj", e, true);
			return;
		}

		OBJGroup myObjGroup = objFile.getDefaultObject();
		myObjGroup = objFile.overwriteMaterial(myObjGroup, material);
		// Log.info("myObjGroup: "+myObjGroup);
		Transform translate = getTranslate(entity, (float)x, (float)y, (float)z, (float)scale).multiply(getRotate(entity, (float) rotation));

		objFile.addObjectToOutput(myObjGroup, translate, obj, false);
	}

	static class ArmorTransform {
		private final double x;
		private final double y;
		private final double z;
		private final double scale;
		private final double rotation;

		ArmorTransform(double x, double y, double z, double scale, double rotation) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.scale = scale;
			this.rotation = rotation;
		}
	}
}
