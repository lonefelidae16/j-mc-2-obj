package org.jmc.entities;

import org.jmc.BlockMaterial;
import org.jmc.NBT.*;
import org.jmc.entities.models.Mesh;
import org.jmc.geom.Transform;
import org.jmc.geom.Vertex;
import org.jmc.registry.NamespaceID;
import org.jmc.threading.ChunkProcessor;
import org.jmc.util.Log;

public class Cushion extends Entity {
    public Cushion(String id) {
        super(id);
    }

    @Override
    public void addEntity(ChunkProcessor obj, TAG_Compound entity) {
        final Vertex pos = getPosition(entity);
        final Transform translate = Transform.translation(pos.x, pos.y, pos.z);

        final TAG_List rot = (TAG_List) entity.getElement("Rotation");
        final float yaw = ((TAG_Float) rot.getElement(0)).value;
        final float pitch = ((TAG_Float) rot.getElement(1)).value;

        final Transform rotate = Transform.rotation2(yaw + 90, pitch, 0);

        try {
            final TAG_String color = (TAG_String) entity.getElement("color");
            if (color == null) {
                throw new NullPointerException("NBT Tag 'color' does not exist in cushion entity!");
            }

            final Mesh modelToExtract = ((Mesh) model).clone();
            BlockMaterial materials = new BlockMaterial();
            materials.put(new NamespaceID[]{NamespaceID.fromString("minecraft:entity/cushion/" + color.value + "_cushion")});
            modelToExtract.setMaterials(materials);
            modelToExtract.addEntity(obj, translate.multiply(rotate));
        } catch (Exception ex) {
            Log.errorOnce("Failed to export cushion entity.", ex, false);
        }
    }

    @Override
    public Vertex getPosition(TAG_Compound entity) {
        TAG_List pos = (TAG_List) entity.getElement("Pos");
        double ex = ((TAG_Double) pos.getElement(0)).value - 0.5;
        double ey = ((TAG_Double) pos.getElement(1)).value;
        double ez = ((TAG_Double) pos.getElement(2)).value - 0.5;
        return new Vertex(ex, ey, ez);
    }
}
