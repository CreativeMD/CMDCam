package team.creative.cmdcam.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.common.util.CamPath;

public class CamSaveData extends SavedData {
    
    public static final String DATA_NAME = CMDCam.MODID + "_Paths";
    
    private HashMap<String, CamPath> paths = new HashMap<>();
    
    public CamSaveData(CompoundTag nbt) {
        for (String key : nbt.getAllKeys())
            paths.put(key, new CamPath(nbt.getCompound(key)));
    }
    
    public CamPath get(String key) {
        return paths.get(key);
    }
    
    public void set(String key, CamPath path) {
        paths.put(key, path);
        setDirty();
    }
    
    public boolean remove(String key) {
        return paths.remove(key) != null;
    }
    
    public Collection<String> names() {
        return paths.keySet();
    }
    
    public void clear() {
        paths.clear();
        setDirty();
    }
    
    @Override
    public CompoundTag save(CompoundTag nbt) {
        for (Entry<String, CamPath> entry : paths.entrySet())
            nbt.put(entry.getKey(), entry.getValue().writeToNBT(new CompoundTag()));
        return nbt;
    }
    
}
