package team.creative.cmdcam.server;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import team.creative.cmdcam.common.scene.CamScene;

public class CMDCamServer {
    
    public static final CamCommandProcessorServer PROCESSOR = new CamCommandProcessorServer();
    
    public static CamScene get(Level level, String name) {
        CamSaveData data = ((ServerLevel) level).getDataStorage().get((x) -> new CamSaveData(x), CamSaveData.DATA_NAME);
        if (data != null)
            return data.get(name);
        return null;
    }
    
    public static void set(Level level, String name, CamScene scene) {
        CamSaveData data = ((ServerLevel) level).getDataStorage().get((x) -> new CamSaveData(x), CamSaveData.DATA_NAME);
        if (data == null) {
            data = new CamSaveData(new CompoundTag());
            ((ServerLevel) level).getDataStorage().set(CamSaveData.DATA_NAME, data);
        }
        data.set(name, scene);
    }
    
    public static void markDirty(Level level) {
        ((ServerLevel) level).getDataStorage().get((x) -> new CamSaveData(x), CamSaveData.DATA_NAME).setDirty();
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
