package com.creativemd.cmdcam.common.utils;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.cmdcam.client.CMDCamClient;
import com.creativemd.cmdcam.client.PathParseException;
import com.creativemd.cmdcam.client.interpolation.CamInterpolation;
import com.creativemd.cmdcam.client.mode.CamMode;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class CamPath {
    
    public int loop;
    public long duration;
    public String mode;
    public String interpolation;
    public CamTarget target;
    public List<CamPoint> points;
    public double cameraFollowSpeed;
    public boolean serverPath = false;
    
    public CamPath(NBTTagCompound nbt) {
        this.loop = nbt.getInteger("loop");
        this.duration = nbt.getLong("duration");
        this.mode = nbt.getString("mode");
        this.interpolation = nbt.getString("interpolation");
        if (nbt.hasKey("target"))
            this.target = CamTarget.readFromNBT(nbt.getCompoundTag("target"));
        NBTTagList list = nbt.getTagList("points", 10);
        this.points = new ArrayList<>();
        for (NBTBase point : list) {
            points.add(new CamPoint((NBTTagCompound) point));
        }
        this.cameraFollowSpeed = nbt.getDouble("cameraFollowSpeed");
    }
    
    public CamPath(int loop, long duration, String mode, String interpolation, CamTarget target, List<CamPoint> points, double cameraFollowSpeed) {
        this.loop = loop;
        this.duration = duration;
        this.mode = mode;
        this.interpolation = interpolation;
        this.target = target;
        this.points = points;
        this.cameraFollowSpeed = cameraFollowSpeed;
    }
    
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("loop", loop);
        nbt.setLong("duration", duration);
        nbt.setString("mode", mode);
        nbt.setString("interpolation", interpolation);
        if (target != null)
            nbt.setTag("target", target.writeToNBT(new NBTTagCompound()));
        NBTTagList list = new NBTTagList();
        for (CamPoint point : points) {
            list.appendTag(point.writeToNBT(new NBTTagCompound()));
        }
        nbt.setTag("points", list);
        nbt.setDouble("cameraFollowSpeed", cameraFollowSpeed);
        return nbt;
    }
    
    @SideOnly(Side.CLIENT)
    public void overwriteClientConfig() {
        CMDCamClient.lastLoop = this.loop;
        CMDCamClient.lastDuration = this.duration;
        CMDCamClient.lastMode = this.mode;
        CMDCamClient.lastInterpolation = this.interpolation;
        CMDCamClient.target = this.target;
        CMDCamClient.points = new ArrayList<>(this.points);
        CMDCamClient.cameraFollowSpeed = this.cameraFollowSpeed;
    }
    
    @SideOnly(Side.CLIENT)
    private boolean hideGui;
    @SideOnly(Side.CLIENT)
    public CamInterpolation cachedInterpolation;
    @SideOnly(Side.CLIENT)
    public CamMode cachedMode;
    
    public long timeStarted = System.currentTimeMillis();
    public int currentLoop;
    private boolean finished;
    private boolean running;
    public List<CamPoint> tempPoints;
    
    public void start(World world) throws PathParseException {
        this.finished = false;
        this.running = true;
        
        this.timeStarted = System.currentTimeMillis();
        this.currentLoop = 0;
        this.tempPoints = new ArrayList<>(points);
        if (loop != 0)
            this.tempPoints.add(this.tempPoints.get(this.tempPoints.size() - 1).copy());
        
        if (world.isRemote) {
            CamMode parser = CamMode.getMode(mode);
            
            this.cachedMode = parser.createMode(this);
            this.cachedMode.onPathStart();
            
            this.cachedInterpolation = CamInterpolation.getInterpolationOrDefault(interpolation);
            this.cachedInterpolation.initMovement(tempPoints, loop, target);
            
            this.hideGui = Minecraft.getMinecraft().gameSettings.hideGUI;
        }
    }
    
    public void finish(World world) {
        this.finished = true;
        this.running = false;
        
        if (world.isRemote) {
            this.cachedMode.onPathFinish();
            this.tempPoints = null;
            
            this.cachedMode = null;
            this.cachedInterpolation = null;
            
            Minecraft.getMinecraft().gameSettings.hideGUI = hideGui;
        }
    }
    
    public void tick(World world, float renderTickTime) {
        long time = System.currentTimeMillis() - timeStarted;
        if (time >= duration) {
            if (currentLoop < loop || loop < 0) {
                timeStarted = System.currentTimeMillis();
                currentLoop++;
            } else
                finish(world);
        } else {
            if (world.isRemote)
                Minecraft.getMinecraft().gameSettings.hideGUI = true;
            
            long durationOfPoint = duration / (tempPoints.size() - 1);
            int currentPoint = Math.min((int) (time / durationOfPoint), tempPoints.size() - 2);
            CamPoint point1 = tempPoints.get(currentPoint);
            CamPoint point2 = tempPoints.get(currentPoint + 1);
            double percent = (time % durationOfPoint) / (double) durationOfPoint;
            CamPoint newPoint = cachedMode.getCamPoint(point1, point2, percent, (double) time / duration, renderTickTime, currentLoop == 0, currentLoop == loop);
            
            if (newPoint != null)
                cachedMode.processPoint(newPoint);
            
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public boolean hasFinished() {
        return finished;
    }
    
    public CamPath copy() {
        CamPath path = new CamPath(currentLoop, duration, mode, interpolation, target, new ArrayList<>(points), cameraFollowSpeed);
        path.serverPath = this.serverPath;
        return path;
    }
    
}
