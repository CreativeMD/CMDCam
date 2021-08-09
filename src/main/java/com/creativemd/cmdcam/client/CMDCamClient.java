package com.creativemd.cmdcam.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.creativemd.cmdcam.CMDCamProxy;
import com.creativemd.cmdcam.common.utils.CamPath;
import com.creativemd.cmdcam.common.utils.CamPoint;
import com.creativemd.cmdcam.common.utils.CamTarget;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public class CMDCamClient extends CMDCamProxy {
    
    public static Minecraft mc = Minecraft.getMinecraft();
    
    public static int lastLoop = 0;
    
    public static long lastDuration = 10000;
    public static String lastMode = "default";
    public static String lastInterpolation = "hermite";
    public static CamTarget target = null;
    public static ArrayList<CamPoint> points = new ArrayList<>();
    
    public static double cameraFollowSpeed = 1D;
    
    public static HashMap<String, CamPath> savedPaths = new HashMap<>();
    
    public static boolean isInstalledOnSever = false;
    
    private static CamPath currentPath;
    
    @Override
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new CamEventHandlerClient());
        
        ClientCommandHandler.instance.registerCommand(new CamCommandClient());
        KeyHandler.initKeys();
    }
    
    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        
    }
    
    public static CamPath getCurrentPath() {
        return currentPath;
    }
    
    public static void startPath(CamPath path) throws PathParseException {
        currentPath = path;
        currentPath.start(mc.world);
    }
    
    public static void stopPath() {
        if (currentPath.serverPath)
            return;
        currentPath.finish(mc.world);
        currentPath = null;
    }
    
    public static void tickPath(World world, float renderTickTime) {
        currentPath.tick(world, renderTickTime);
        if (currentPath.hasFinished())
            currentPath = null;
    }
    
    public static CamPath createPathFromCurrentConfiguration() throws PathParseException {
        if (points.size() < 1)
            throw new PathParseException("You have to register at least 1 point!");
        
        List<CamPoint> newPoints = new ArrayList<>(points);
        if (newPoints.size() == 1)
            newPoints.add(newPoints.get(0));
        
        return new CamPath(lastLoop, lastDuration, lastMode, lastInterpolation, target, newPoints, cameraFollowSpeed);
    }
    
}
