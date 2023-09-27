package team.creative.cmdcam.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.creativecore.common.util.registry.exception.RegistryException;

public class CamSaveData extends SavedData {
    
    public static final String DATA_NAME = CMDCam.MODID + "_Scenes";
    
    private HashMap<String, CamScene> scenes = new HashMap<>();
    
    public CamSaveData() {}
    
    public CamSaveData(CompoundTag nbt) {
        for (String key : nbt.getAllKeys())
            try {
                scenes.put(key, new CamScene(nbt.getCompound(key)));
            } catch (RegistryException e) {
                e.printStackTrace();
            }
    }
    
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
    
    @Override
    public CompoundTag save(CompoundTag nbt) {
        for (Entry<String, CamScene> entry : scenes.entrySet())
            nbt.put(entry.getKey(), entry.getValue().save(new CompoundTag()));
        return nbt;
    }
    
}
