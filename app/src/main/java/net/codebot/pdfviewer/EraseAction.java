package net.codebot.pdfviewer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.ArrayList;

public class EraseAction extends Action {
    ArrayList<Action> ErasedActions = new ArrayList();

    EraseAction(float x, float y) {
        super(x, y);
    }

    @Override
    void addPointToPath(float x, float y, ArrayList<Action> actions) {
        this.path.lineTo(x, y);
        float[] lastPoint = this.pathPoints.get(this.pathPoints.size() - 1);

        for (int i = 0; i < addNumPointsInBetween; i++) {
            float inBetweenX = lastPoint[0] + (x - lastPoint[0]) / addNumPointsInBetween * i;
            float inBetweenY = lastPoint[1] + (y - lastPoint[1]) / addNumPointsInBetween * i;
            this.pathPoints.add(new float[]{inBetweenX, inBetweenY});
            for (int j = 0; j < actions.size(); j++) {
                if (actions.get(j) == this) {
                    break;
                } else if (this.ErasedActions.contains(actions.get(j))) {
                    continue;
                } else if (actions.get(j).isErased()) {
                    continue;
                } else if (actions.get(j).checkedIfErased(inBetweenX, inBetweenY, this)) {
                    this.ErasedActions.add(actions.get(j));
                    continue;
                }
            }
        }
    }

    @Override
    boolean hasValue() {
        return this.ErasedActions.size() > 0;
    }

//    @Override
//    void erase(ArrayList<Action> actions) {
//        for (int i = 0; i < actions.size(); i++) {
//            if (actions.get(i) == this) {
//                break;
//            }
//            actions.get(i).checkedIfErased(this.pathPoints);
//        }
//    }
//
//    @Override
//    void drawPath(Canvas canvas) {
//        Paint paint = new Paint(Color.BLUE);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeWidth(10);
//        canvas.drawPath(this.path, paint);
//    }

    @Override
    void undoAction() {
        for (int i = 0; i < this.ErasedActions.size(); i++) {
            this.ErasedActions.get(i).removeEraser(this);
        }
    }

    @Override
    void redoAction() {
        for (int i = 0; i < this.ErasedActions.size(); i++) {
            this.ErasedActions.get(i).addEraser(this);
        }
    }
}