package net.codebot.pdfviewer;

import android.graphics.*;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;

public class DrawAction extends Action {
    private Paint paint;
    private ArrayList<Action> Erasers = new ArrayList();
    protected float paintSize;
    DrawAction(float x, float y, Paint paint) {
        super(x, y);
        this.paint = paint;
    }

    private float calDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

//    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
//    @Override
//    void checkedIfErased(ArrayList<float[]> EraserPathPoints) {
//        for (int i = 0; i < EraserPathPoints.size(); i++) {
//            for (int j = 0; j < this.pathPoints.size(); j++) {
//                float[] EraserPoint = EraserPathPoints.get(i);
//                float[] PathPoint = this.pathPoints.get(j);
//                if (this.calDistance(
//                        EraserPoint[0],
//                        EraserPoint[1],
//                        PathPoint[0],
//                        PathPoint[1]) < this.paintSize * 2
//                ) {
//                    this.notErased = false;
//                    break;
//                }
//            }
//        }

//        Region region1 = new Region();
//        Region region2 = new Region();
//
//        Region clip = new Region();
//        region1.setPath(EraserPath, clip);
//        region2.setPath(this.path, clip);
//
//        if (region1.op(region2, Region.Op.INTERSECT)) {
//            this.notErased = false;
//        }

//        this.notErased = !this.path.op(EraserPath, Path.Op.INTERSECT);
//        if (this.path.op(EraserPath, Path.Op.INTERSECT)) {
//            this.notErased = false;
//        }
//    }

    @Override
    boolean hasValue() {
        return this.pathPoints.size() > 1;
    }

    @Override
    boolean isErased() {
        return this.Erasers.size() > 0;
    }


    @Override
    boolean checkedIfErased(float erasePointX, float erasePointY, Action Eraser) {
        for (int i = 0; i < this.pathPoints.size(); i++) {
            float[] PathPoint = this.pathPoints.get(i);
            if (this.calDistance(
                    erasePointX,
                    erasePointY,
                    PathPoint[0],
                    PathPoint[1]) < this.paintSize
            ) {
                this.Erasers.add(Eraser);
                return true;
            }
        }
        return false;
    }

    @Override
    void drawPath(Canvas canvas) {
        if (this.Erasers.size() == 0) {
            canvas.drawPath(this.path, this.paint);
        }
    }

    @Override
    void removeEraser(Action Eraser) {
        this.Erasers.remove(Eraser);
    }

    @Override
    void addEraser(Action Eraser) {
        this.Erasers.add(Eraser);
    }
}
