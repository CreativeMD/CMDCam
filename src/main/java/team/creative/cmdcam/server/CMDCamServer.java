package team.creative.cmdcam.server;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import team.creative.cmdcam.common.util.CamPath;

public class CMDCamServer {
    
    public static CamPath getPath(World world, String name) {
        
        CamSaveData data = ((ServerWorld) world).getDataStorage().get(() -> new CamSaveData(), CamSaveData.DATA_NAME);
        if (data != null)
            return data.get(name);
        return null;
    }
    
    public static void setPath(World world, String name, CamPath path) {
        CamSaveData data = ((ServerWorld) world).getDataStorage().get(() -> new CamSaveData(), CamSaveData.DATA_NAME);
        if (data == null) {
            data = new CamSaveData();
            ((ServerWorld) world).getDataStorage().set(data);
        }
        data.set(name, path);
    }
    
    public static boolean removePath(World world, String name) {
        CamSaveData data = ((ServerWorld) world).getDataStorage().get(() -> new CamSaveData(), CamSaveData.DATA_NAME);
        if (data != null)
            return data.remove(name);
        return false;
    }
    
    public static Collection<String> getSavedPaths(World world) {
        CamSaveData data = ((ServerWorld) world).getDataStorage().get(() -> new CamSaveData(), CamSaveData.DATA_NAME);
        if (data != null)
            return data.names();
        return new ArrayList<>();
    }
    
    public static void clearPaths(World world) {
        CamSaveData data = ((ServerWorld) world).getDataStorage().get(() -> new CamSaveData(), CamSaveData.DATA_NAME);
        if (data != null)
            data.clear();
    }
    
}
