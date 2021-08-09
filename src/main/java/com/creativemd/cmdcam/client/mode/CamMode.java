package com.creativemd.cmdcam.client.mode;

import java.util.ArrayList;
import java.util.HashMap;

import com.creativemd.cmdcam.client.CamEventHandlerClient;
import com.creativemd.cmdcam.client.interpolation.CamInterpolation;
import com.creativemd.cmdcam.common.utils.CamPath;
import com.creativemd.cmdcam.common.utils.CamPoint;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class CamMode {
    
    public static HashMap<String, CamMode> modes = new HashMap<>();
    
    public static void registerPath(String id, CamMode path) {
        modes.put(id, path);
    }
    
    public static CamMode getMode(String id) {
        return modes.getOrDefault(id, defaultMode);
    }
    
    public static DefaultMode defaultMode = new DefaultMode(null);
    
    static {
        registerPath("default", defaultMode);
        registerPath("outside", new OutsideMode(null));
    }
    
    public final CamPath path;
    
    public double lastYaw;
    public double lastPitch;
    
    public CamMode(CamPath path) {
        this.path = path;
        
        if (path != null) {
            this.lastPitch = path.tempPoints.get(0).rotationPitch;
            this.lastYaw = path.tempPoints.get(0).rotationYaw;
        }
    }
    
    public abstract CamMode createMode(CamPath path);
    
    @SideOnly(Side.CLIENT)
    public CamPoint getCamPoint(CamPoint point1, CamPoint point2, double percent, double wholeProgress, float renderTickTime, boolean isFirstLoop, boolean isLastLoop) {
        CamPoint newPoint = path.cachedInterpolation.getPointInBetween(point1, point2, percent, wholeProgress, isFirstLoop, isLastLoop);
        if (path.target != null) {
            newPoint.rotationPitch = lastPitch;
            newPoint.rotationYaw = lastYaw;
            Minecraft mc = Minecraft.getMinecraft();
            
            Vec3d pos = path.target.getTargetVec(mc.world, mc.getRenderPartialTicks());
            
            if (pos != null) {
                long timeSinceLastRenderFrame = System.nanoTime() - CamEventHandlerClient.lastRenderTime;
                newPoint.faceEntity(pos, 0.00000001F, 0.00000001F, timeSinceLastRenderFrame / 600000000D * path.cameraFollowSpeed);
            }
            lastPitch = newPoint.rotationPitch;
            lastYaw = newPoint.rotationYaw;
        }
        return newPoint;
    }
    
    @SideOnly(Side.CLIENT)
    public void onPathStart() {
        
    }
    
    @SideOnly(Side.CLIENT)
    public void onPathFinish() {
        Minecraft.getMinecraft().gameSettings.fovSetting = CamEventHandlerClient.defaultfov;
        CamEventHandlerClient.roll = 0;
    }
    
    @SideOnly(Side.CLIENT)
    public EntityLivingBase getCamera() {
        return Minecraft.getMinecraft().player;
    }
    
    public abstract String getDescription();
    
    @SideOnly(Side.CLIENT)
    public void processPoint(CamPoint point) {
        CamEventHandlerClient.roll = (float) point.roll;
        Minecraft.getMinecraft().gameSettings.fovSetting = (float) point.zoom;
    }
    
    public static CamPoint getPoint(CamInterpolation movement, ArrayList<CamPoint> points, double percent, int currentLoop, int loops) {
        double lengthOfPoint = 1 / (points.size() - 1);
        int currentPoint = Math.min((int) (percent / lengthOfPoint), points.size() - 2);
        CamPoint point1 = points.get(currentPoint);
        CamPoint point2 = points.get(currentPoint + 1);
        double percentOfPoint = (percent % lengthOfPoint);
        return movement.getPointInBetween(point1, point2, percent, percent, currentLoop == 0, currentLoop == loops);
    }
    
}
