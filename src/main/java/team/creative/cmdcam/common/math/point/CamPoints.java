package team.creative.cmdcam.common.math.point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.minecraft.util.Mth;
import team.creative.cmdcam.common.math.interpolation.CamPitchMode;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;
import team.creative.creativecore.common.util.math.interpolation.Interpolation;
import team.creative.creativecore.common.util.math.vec.VecNd;
import team.creative.creativecore.common.util.type.itr.ConsecutiveIterator;
import team.creative.creativecore.common.util.type.itr.SingleIterator;

public class CamPoints implements Iterable<CamPoint> {
    
    private CamPoint before;
    private List<CamPoint> content;
    private CamPoint after;
    
    public CamPoints(Collection<CamPoint> collection) {
        this.content = new ArrayList<>(collection);
    }
    
    public CamPoints() {
        content = new ArrayList<>();
    }
    
    public boolean add(CamPoint point) {
        return content.add(point);
    }
    
    public boolean addAll(Collection<CamPoint> points) {
        return content.addAll(points);
    }
    
    public CamPoint before() {
        return this.before;
    }
    
    public void before(CamPoint point) {
        this.before = point;
    }
    
    public CamPoint after() {
        return this.after;
    }
    
    public void after(CamPoint point) {
        this.after = point;
    }
    
    public void fixSpinning(CamPitchMode mode) {
        if (mode == CamPitchMode.FIX)
            fixSpinning();
        else if (mode == CamPitchMode.FIX_KEEP_DIRECTION)
            fixSpinningKeepDirection();
    }
    
    public void fixSpinning() {
        double wrappedYaw = 180;
        double lastYaw = 0;
        for (Iterator<CamPoint> iterator = iteratorAll(); iterator.hasNext();) {
            CamPoint point = iterator.next();
            double wrappedYawCurrent = Mth.wrapDegrees(point.rotationYaw) + 180;
            
            double rightDistance;
            double leftDistance;
            if (wrappedYaw > wrappedYawCurrent) {
                rightDistance = 360 - wrappedYaw + wrappedYawCurrent;
                leftDistance = wrappedYaw - wrappedYawCurrent;
            } else {
                rightDistance = wrappedYawCurrent - wrappedYaw;
                leftDistance = 360 - wrappedYawCurrent + wrappedYaw;
            }
            
            if (rightDistance < leftDistance)
                lastYaw += rightDistance;
            else
                lastYaw -= leftDistance;
            
            wrappedYaw = wrappedYawCurrent;
            point.rotationYaw = lastYaw;
        }
    }
    
    public void fixSpinningKeepDirection() {
        double lastYaw = 0;
        double originalYaw = 0;
        for (Iterator<CamPoint> iterator = iteratorAll(); iterator.hasNext();) {
            CamPoint point = iterator.next();
            lastYaw += (point.rotationYaw - originalYaw) % 360;
            originalYaw = point.rotationYaw;
            point.rotationYaw = lastYaw;
        }
    }
    
    @Override
    public Iterator<CamPoint> iterator() {
        return content.iterator();
    }
    
    public Iterator<CamPoint> iteratorAll() {
        return new ConsecutiveIterator<CamPoint>(new SingleIterator<CamPoint>(before), content.iterator(), new SingleIterator<CamPoint>(after));
    }
    
    public double estimateLength() {
        double distance = 0;
        for (int i = 0; i < content.size() - 1; i++)
            distance += content.get(i).distance(content.get(i + 1));
        return distance;
    }
    
    public <T extends VecNd> Interpolation<T> interpolate(CamScene scene, CamAttribute<T> attribute) {
        List vecs = new ArrayList(content.size());
        for (CamPoint point : content)
            vecs.add(attribute.get(point));
        Interpolation<T> inter = scene.interpolation.create(scene, before != null ? attribute.get(before) : null, vecs, after != null ? attribute.get(after) : null, attribute);
        if (scene.distanceBasedTiming)
            inter.makeTimingDistanceBased();
        return inter;
    }
    
    public int size() {
        return content.size();
    }
    
}
