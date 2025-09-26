package team.creative.cmdcam.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.creativecore.common.util.registry.exception.RegistryException;

public class CamSaveData extends SavedData {
    
    public static final String DATA_NAME = CMDCam.MODID + "_Scenes";
    public static final Codec<CamSaveData> CODEC = CompoundTag.CODEC.flatXmap(tag -> {
        var data = new CamSaveData(null);
        data.load(tag);
        return DataResult.success(data);
    }, data -> DataResult.success(data.save(new CompoundTag())));
    
    private HashMap<String, CamScene> scenes = new HashMap<>();
    
    public CamSaveData(SavedData.Context context) {}
    
    public CamScene get(String key) {
        return scenes.get(key);
    }
    
    public void set(String key, CamScene path) {
        scenes.put(key, path);
        setDirty();
    }
    
    public boolean remove(String key) {
        return scenes.remove(key) != null;
    }
    
    public Collection<String> names() {
        return scenes.keySet();
    }
    
    public void clear() {
        scenes.clear();
        setDirty();
    }
    
    public void load(CompoundTag nbt) {
        for (String key : nbt.keySet())
            try {
                scenes.put(key, new CamScene(nbt.getCompoundOrEmpty(key)));
            } catch (RegistryException e) {
                e.printStackTrace();
            }
    }
    
    public CompoundTag save(CompoundTag nbt) {
        for (Entry<String, CamScene> entry : scenes.entrySet())
            nbt.put(entry.getKey(), entry.getValue().save(new CompoundTag()));
        return nbt;
    }
    
}
