package team.creative.cmdcam.common.math.interpolation;

import java.util.List;

import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;
import team.creative.creativecore.common.util.math.interpolation.Interpolation;
import team.creative.creativecore.common.util.math.interpolation.LinearInterpolation;
import team.creative.creativecore.common.util.math.vec.VecNd;
import team.creative.creativecore.common.util.type.Color;

public class LinearCamInterpolation extends CamInterpolation {
    
    public LinearCamInterpolation() {
        super(new Color(0, 0, 255));
    }
    
    @Override
    public <T extends VecNd> Interpolation<T> create(CamScene scene, T before, List<T> points, T after, CamAttribute<T> attribute) {
        return new LinearInterpolation<T>(points);
    }
}
