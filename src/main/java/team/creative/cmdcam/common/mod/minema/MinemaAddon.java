package team.creative.cmdcam.common.mod.minema;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import team.creative.cmdcam.CMDCam;
import team.creative.creativecore.CreativeCore;
import team.creative.creativecore.reflection.ReflectionHelper;

public class MinemaAddon {
    
    public static final String MODID = "minema";
    
    private static final boolean installed;
    private static final Field singletonField;
    private static final Method startCaptureMethod;
    private static final Method stopCaptureMethod;
    private static final Object configFrameLimit;
    private static final Method getInt;
    private static final Method setInt;
    private static int previousFrameLimit;
    
    static {
        if ((installed = CreativeCore.loader().isModLoaded(MODID))) {
            try {
                Class captureSessionClass = Class.forName("info.ata4.minecraft.minema.CaptureSession");
                singletonField = ReflectionHelper.findField(captureSessionClass, "singleton");
                startCaptureMethod = ReflectionHelper.findMethod(captureSessionClass, "startCapture");
                stopCaptureMethod = ReflectionHelper.findMethod(captureSessionClass, "stopCapture");
                Class modClass = Class.forName("info.ata4.minecraft.minema.Minema");
                Object config = ReflectionHelper.findField(modClass, "config").get(ReflectionHelper.findField(modClass, "instance").get(null));
                configFrameLimit = ReflectionHelper.findField(config.getClass(), "frameLimit").get(config);
                getInt = ReflectionHelper.findMethod(configFrameLimit.getClass(), "get");
                setInt = ReflectionHelper.findMethod(configFrameLimit.getClass(), "set", int.class);
            } catch (IllegalArgumentException | IllegalAccessException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            singletonField = null;
            startCaptureMethod = null;
            stopCaptureMethod = null;
            configFrameLimit = null;
            getInt = null;
            setInt = null;
        }
    }
    
    public static boolean installed() {
        return installed && CMDCam.CONFIG.syncMinema;
    }
    
    public static void startCapture() {
        try {
            previousFrameLimit = (int) getInt.invoke(configFrameLimit);
            setInt.invoke(configFrameLimit, -1);
            startCaptureMethod.invoke(singletonField.get(null));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {}
    }
    
    public static void stopCapture() {
        try {
            stopCaptureMethod.invoke(singletonField.get(null));
            setInt.invoke(configFrameLimit, previousFrameLimit);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {}
    }
    
}
