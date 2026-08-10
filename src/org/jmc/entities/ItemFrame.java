package org.jmc.entities;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jmc.BlockMaterial;
import org.jmc.FilledMapDat;
import org.jmc.Options;
import org.jmc.NBT.NBT_Tag;
import org.jmc.NBT.TAG_Byte;
import org.jmc.NBT.TAG_Compound;
import org.jmc.NBT.TAG_Double;
import org.jmc.NBT.TAG_List;
import org.jmc.NBT.TAG_Int;
import org.jmc.NBT.TAG_String;
import org.jmc.geom.BlockPos;
import org.jmc.geom.Transform;
import org.jmc.geom.Vertex;
import org.jmc.registry.NamespaceID;
import org.jmc.threading.ChunkProcessor;
import org.jmc.util.Log;
import org.jmc.util.MathHelper;


/**
 * Model for paintings.
 * TODO correctly render paintings larger than 1x1
 */
public class ItemFrame extends Entity
{
	
	public ItemFrame(String id) {
		super(id);
	}


	private static Set<NamespaceID> exportedMaps = new HashSet<>();

	
	@Override
	public void addEntity(ChunkProcessor obj, TAG_Compound entity)
	{
		
		BlockPos pos = getBlockPosition(entity);
		byte facing = ((TAG_Byte)entity.getElement("Facing")).value;
		
		TAG_Compound item = ((TAG_Compound)entity.getElement("Item"));
		
		String item_id = "";
		
		try {
			item_id = ((TAG_String)item.getElement("id")).value;
		}
		catch (Exception e) {
			// Log.info("Item Id of frame not found - that seams ok - it may be empty!");
		}
		
		
		Transform rotate = new Transform();
		Transform translate = new Transform();
		Transform rt;

		int frameRotation = 0;
		NBT_Tag itemRot = entity.getElement("ItemRotation");
		if (itemRot != null) {
			frameRotation = ((TAG_Byte)itemRot).value;
			frameRotation = frameRotation * 90; // doku says: 45 degrees - but thats wrong (at least for "filled_map")
		}

		int baseRotation = 0;
		int baseRotZ = 0;

		switch (facing)
		{
			case 3:
				baseRotation = 0;
				break;
			case 4:
				baseRotation = 90;
				break;
			case 2:
				baseRotation = 180;
				break;
			case 5:
				baseRotation = -90;
				break;
			case 1:  // UP
				baseRotZ = -90;
				frameRotation += 180;
				break;
		}

		rotate = Transform.rotation(baseRotZ, baseRotation, normalizeRot(frameRotation));

		translate = Transform.translation(pos.x, pos.y, pos.z);
		rt = translate.multiply(rotate);
		
		NamespaceID baseTexture = model.getMaterials().get(null, NamespaceID.NULL)[0];
		
		BlockMaterial materials=new BlockMaterial();
		
		switch (item_id)
		{
			case "minecraft:filled_map":
				if (processMap(item, materials)) break;
			default:
				// Log.info("Unsupported FrameItem: '" + item_id + "'");
				NamespaceID[] matname1={baseTexture};
				materials.put(matname1);
				break;
		}
		model.setMaterials(materials);
		model.addEntity(obj, rt);
	}

	private static int normalizeRot(int rot) {
		int normalized = rot % 360;
		if (normalized >= 180) {
			normalized -= 360;
		}
		if (normalized < -180) {
			normalized += 360;
		}
		return normalized;
	}
	
	private static boolean processMap(TAG_Compound item, BlockMaterial materials) {
		int map_id;
		TAG_Compound itemTag = (TAG_Compound) item.getElement("components");
		if (itemTag != null) {
			map_id = ((TAG_Int) itemTag.getElement("minecraft:map_id")).value;
		} else { // old tag format
			itemTag = (TAG_Compound) item.getElement("tag");
			map_id = ((TAG_Int) itemTag.getElement("map")).value;
		}
		// Log.info("Found map with id: '" + map_id+ "'");
		NamespaceID mapTexID = new NamespaceID("jmc2obj", "map/" + map_id);
		materials.put(new NamespaceID[]{mapTexID});
		
		
		FilledMapDat map_data = new FilledMapDat(Options.worldDir);
		if (!map_data.open(String.valueOf(map_id))) {
			Log.debugOnce("'map_" + map_id+ ".dat' not found");
			return false;
		} else {
			synchronized (exportedMaps) {
				// already exported material?
				if (!exportedMaps.contains(mapTexID)) {
					// Log.info("export map: "+mapName);
					try {
						map_data.writePngTexture(mapTexID);
						exportedMaps.add(mapTexID);
					} catch (IOException e) {
						Log.error("Cant write map", e, true);
					}
				}
			}
		}
		return true;
	}

	private BlockPos getBlockPosition(TAG_Compound entity) {
		final int x, y, z;

		TAG_List pos = (TAG_List) entity.getElement("Pos");
		if (pos != null) {
			x = MathHelper.floor(((TAG_Double) pos.elements[0]).value);
			y = MathHelper.floor(((TAG_Double) pos.elements[1]).value);
			z = MathHelper.floor(((TAG_Double) pos.elements[2]).value);
		} else {
			x = ((TAG_Int) entity.getElement("TileX")).value;
			y = ((TAG_Int) entity.getElement("TileY")).value;
			z = ((TAG_Int) entity.getElement("TileZ")).value;
		}
		return new BlockPos(x, y, z);
	}

	@Override
	public Vertex getPosition(TAG_Compound entity) {
		BlockPos pos = getBlockPosition(entity);
		return new Vertex(pos.x, pos.y, pos.z);
	}

	public static void clearExported() {
		synchronized (exportedMaps) {
			exportedMaps.clear();
		}
    }

}
