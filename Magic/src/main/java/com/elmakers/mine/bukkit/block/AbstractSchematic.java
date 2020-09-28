package com.elmakers.mine.bukkit.block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import org.bukkit.Art;
import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import com.elmakers.mine.bukkit.api.block.Schematic;
import com.elmakers.mine.bukkit.api.entity.EntityData;
import com.elmakers.mine.bukkit.utility.NMSUtils;

public abstract class AbstractSchematic implements Schematic {
    protected volatile boolean loaded = false;
    protected Vector size;
    protected Vector center;
    protected MaterialAndData[][][] blocks;
    protected Collection<EntityData> entities;
    protected Map<BlockVector, Object> tileEntities;

    protected void initialize(short width, short height, short length) {
        size = new Vector(width, height, length);
        center = new Vector(Math.floor(size.getBlockX() / 2), 0, Math.floor(size.getBlockZ() / 2));
        blocks = new MaterialAndData[width][height][length];
        entities = new ArrayList<>();
    }

    protected void loadTileEntities(Collection<Object> tileEntityData) {
        if (tileEntityData == null || tileEntityData.isEmpty()) return;
        tileEntities = new HashMap<>();
        for (Object tileEntity : tileEntityData)
        {
            try {
                Integer x = NMSUtils.getMetaInt(tileEntity, "x");
                Integer y = NMSUtils.getMetaInt(tileEntity, "y");
                Integer z = NMSUtils.getMetaInt(tileEntity, "z");

                if (x == null || y == null || z == null) continue;

                BlockVector location = new BlockVector(x, y, z);
                tileEntities.put(location, tileEntity);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    protected void addTileEntity(BlockVector blockLocation, MaterialAndData block) {
        if (tileEntities == null) return;
        Object tileEntity = tileEntities.get(blockLocation);
        if (tileEntity != null) {
            try {
                if (DefaultMaterials.isCommand(block.getMaterial())) {
                    String customName = NMSUtils.getMetaString(tileEntity, "CustomName");
                    if (!customName.isEmpty()) {
                        block.setCustomName(customName);
                    }
                    block.setCommandLine(NMSUtils.getMetaString(tileEntity, "Command"));
                } else {
                    block.setRawData(tileEntity);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    protected void loadEntities(Collection<Object> entityData, Vector origin) {
        if (entityData == null || entityData.isEmpty()) return;
        for (Object entity : entityData) {
            String type = NMSUtils.getMetaString(entity, "id");
            Vector position = NMSUtils.getPosition(entity, "Pos");
            if (position == null) continue;
            position = position.subtract(origin).subtract(center);

            if (type == null) continue;

            // Only doing paintings and item frames for now.
            if (type.equals("Painting")) {
                String motive = NMSUtils.getMetaString(entity, "Motive");
                motive = motive.toLowerCase();
                Art art = Art.ALBAN;
                for (Art test : Art.values()) {
                    if (test.name().toLowerCase().replace("_", "").equals(motive)) {
                        art = test;
                        break;
                    }
                }
                byte facing = NMSUtils.getMetaByte(entity, "Facing");
                EntityData painting = com.elmakers.mine.bukkit.entity.EntityData.loadPainting(position, art, getFacing(facing));
                entities.add(painting);
            } else if (type.equals("ItemFrame")) {
                byte facing = NMSUtils.getMetaByte(entity, "Facing");
                byte rotation = NMSUtils.getMetaByte(entity, "ItemRotation");
                Rotation rot = Rotation.NONE;
                if (rotation < Rotation.values().length) {
                    rot = Rotation.values()[rotation];
                }
                ItemStack item = NMSUtils.getItem(NMSUtils.getNode(entity, "Item"));
                EntityData itemFrame = com.elmakers.mine.bukkit.entity.EntityData.loadItemFrame(position, item, getFacing(facing), rot);
                entities.add(itemFrame);
            }
        }
    }

    @Override
    public boolean contains(Vector v) {
        int x = v.getBlockX() + center.getBlockX();
        int y = v.getBlockY() + center.getBlockY();
        int z = v.getBlockZ() + center.getBlockZ();

        return (x >= 0 && x <= size.getBlockX() && y >= 0 && y <= size.getBlockY() && z >= 0 && z <= size.getBlockZ());
    }

    @Nullable
    @Override
    public MaterialAndData getBlock(Vector v) {
        int x = v.getBlockX() + center.getBlockX();
        int y = v.getBlockY() + center.getBlockY();
        int z = v.getBlockZ() + center.getBlockZ();

        if (x < 0 || x >= blocks.length || y < 0 || y >= blocks[x].length || z < 0 || z >= blocks[x][y].length) {
            return null;
        }

        return blocks[x][y][z];
    }

    @Override
    public Collection<EntityData> getEntities(Location center) {
        List<EntityData> translated = new ArrayList<>();
        for (EntityData data : entities) {
            EntityData relative = data == null ? null : data.getRelativeTo(center);
            if (relative != null) {
                translated.add(relative);
            }
        }
        return translated;
    }

    @Override
    public Vector getSize() {
        return size;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    protected BlockFace getFacing(byte dir) {
        switch (dir) {
            case 0: return BlockFace.SOUTH;
            case 1: return BlockFace.WEST;
            case 2: return BlockFace.NORTH;
            case 3: return BlockFace.EAST;
        }
        return BlockFace.UP;
    }

}
