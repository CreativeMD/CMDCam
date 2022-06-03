package team.creative.cmdcam.common.math.interpolation;

import java.util.List;

import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;
import team.creative.creativecore.common.util.math.interpolation.CubicInterpolation;
import team.creative.creativecore.common.util.math.interpolation.Interpolation;
import team.creative.creativecore.common.util.math.vec.VecNd;
import team.creative.creativecore.common.util.type.Color;

public class CubicCamInterpolation extends CamInterpolation {
    
    public CubicCamInterpolation() {
        super(new Color(255, 0, 0));
    }
    
    @Override
    public <T extends VecNd> Interpolation<T> create(double[] times, CamScene scene, T before, List<T> points, T after, CamAttribute<T> attribute) {
        return new CubicInterpolation<T>(times, before, points, after);
    }
    
}
