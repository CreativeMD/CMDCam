package team.creative.cmdcam.common.math.interpolation;

public enum CamPitchMode {
    
    NO_FIX,
    FIX,
    FIX_KEEP_DIRECTION;
    
    public static final String[] NAMES;
    
    static {
        NAMES = new String[values().length];
        for (int i = 0; i < NAMES.length; i++)
            NAMES[i] = values()[i].name().toLowerCase();
    }
    
}
