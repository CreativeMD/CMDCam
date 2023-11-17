package team.creative.cmdcam.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

public class ComputeCameraAnglesCallback {
    public static final Event<ComputeCameraAngles> EVENT = EventFactory.createArrayBacked(ComputeCameraAngles.class, events -> callback -> {
        for (ComputeCameraAngles event : events) {
            event.onComputeCameraAngles(callback);
        }
    });

    public interface ComputeCameraAngles {
        void onComputeCameraAngles(ComputeCameraAnglesCallback event);
    }

    private final GameRenderer renderer;
    private final Camera camera;
    private final double partialTick;

    public GameRenderer getRenderer() {
        return renderer;
    }

    public Camera getCamera() {
        return camera;
    }

    public double getPartialTick() {
        return partialTick;
    }

    private float yaw;
    private float pitch;
    private float roll;

    public ComputeCameraAnglesCallback(GameRenderer renderer, Camera camera, double partialTick, float yaw, float pitch, float roll) {
        this.renderer = renderer;
        this.camera = camera;
        this.partialTick = partialTick;
        this.setYaw(yaw);
        this.setPitch(pitch);
        this.setRoll(roll);
    }

    /**
     * {@return the yaw of the player's camera}
     */
    public float getYaw()
    {
        return yaw;
    }

    /**
     * Sets the yaw of the player's camera.
     *
     * @param yaw the new yaw
     */
    public void setYaw(float yaw)
    {
        this.yaw = yaw;
    }

    /**
     * {@return the pitch of the player's camera}
     */
    public float getPitch()
    {
        return pitch;
    }

    /**
     * Sets the pitch of the player's camera.
     *
     * @param pitch the new pitch
     */
    public void setPitch(float pitch)
    {
        this.pitch = pitch;
    }

    /**
     * {@return the roll of the player's camera}
     */
    public float getRoll()
    {
        return roll;
    }

    /**
     * Sets the roll of the player's camera.
     *
     * @param roll the new roll
     */
    public void setRoll(float roll)
    {
        this.roll = roll;
    }
}