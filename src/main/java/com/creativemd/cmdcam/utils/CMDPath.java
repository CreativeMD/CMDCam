package com.creativemd.cmdcam.utils;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.cmdcam.CMDCam;

public class CMDPath {
	
	public int loop;
	public long duration;
	public String path;
	public String movement;
	public Object target;
	public List<CamPoint> points;
	public double cameraFollowSpeed;
	
	public CMDPath() {
		this.loop = CMDCam.lastLoop;
		this.duration = CMDCam.lastDuration;
		this.path = CMDCam.lastPath;
		this.movement = CMDCam.lastMovement;
		this.target = CMDCam.target;
		this.points = new ArrayList<>(CMDCam.points);
		this.cameraFollowSpeed = CMDCam.cameraFollowSpeed;
	}
	
	public void load()
	{
		CMDCam.lastLoop = this.loop;
		CMDCam.lastDuration = this.duration;
		CMDCam.lastPath = this.path;
		CMDCam.lastMovement = this.movement;
		CMDCam.target = this.target;
		CMDCam.points = new ArrayList<>(this.points);
		CMDCam.cameraFollowSpeed = this.cameraFollowSpeed;
	}
}
