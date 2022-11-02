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
    
    public static CamPitchMode of(String name) {
        for (int i = 0; i < NAMES.length; i++)
            if (name.equals(NAMES[i]))
                return values()[i];
        throw new IllegalArgumentException(name);
    }
    
}
