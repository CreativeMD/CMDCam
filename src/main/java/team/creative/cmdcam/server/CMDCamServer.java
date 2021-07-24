package team.creative.cmdcam.server;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import team.creative.cmdcam.common.util.CamPath;

public class CMDCamServer {
    
    public static CamPath getPath(Level level, String name) {
        
        CamSaveData data = ((ServerLevel) level).getDataStorage().get((x) -> new CamSaveData(x), CamSaveData.DATA_NAME);
        if (data != null)
            return data.get(name);
        return null;
    }
    
    public static void setPath(Level level, String name, CamPath path) {
        CamSaveData data = ((ServerLevel) level).getDataStorage().get((x) -> new CamSaveData(x), CamSaveData.DATA_NAME);
        if (data == null) {
            data = new CamSaveData(new CompoundTag());
            ((ServerLevel) level).getDataStorage().set(CamSaveData.DATA_NAME, data);
        }
        data.set(name, path);
    }
    
    public static boolean removePath(Level level, String name) {
        CamSaveData data = ((ServerLevel) level).getDataStorage().get((x) -> new CamSaveData(x), CamSaveData.DATA_NAME);
        if (data != null)
            return data.remove(name);
        return false;
    }
    
    public static Collection<String> getSavedPaths(Level level) {
        CamSaveData data = ((ServerLevel) level).getDataStorage().get((x) -> new CamSaveData(x), CamSaveData.DATA_NAME);
        if (data != null)
            return data.names();
        return new ArrayList<>();
    }
    
    public static void clearPaths(Level level) {
        CamSaveData data = ((ServerLevel) level).getDataStorage().get((x) -> new CamSaveData(x), CamSaveData.DATA_NAME);
        if (data != null)
            data.clear();
    }
    
}
