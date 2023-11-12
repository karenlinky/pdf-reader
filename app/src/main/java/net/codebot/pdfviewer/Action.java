package net.codebot.pdfviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.util.ArrayList;

public class Action {
    protected Path path;
    protected ArrayList<float[]> pathPoints = new ArrayList();
    final protected int addNumPointsInBetween = 25;
    Action(float x, float y) {
        this.path = new Path();
        this.path.moveTo(x, y);
        this.pathPoints.add(new float[]{x, y});
    }

    void addPointToPath(float x, float y, ArrayList<Action> actions) {
        this.path.lineTo(x, y);
        float[] lastPoint = this.pathPoints.get(this.pathPoints.size() - 1);

        for (int i = 0; i < addNumPointsInBetween; i++) {
            float inBetweenX = lastPoint[0] + (x - lastPoint[0]) / addNumPointsInBetween * i;
            float inBetweenY = lastPoint[1] + (y - lastPoint[1]) / addNumPointsInBetween * i;
            this.pathPoints.add(new float[]{inBetweenX, inBetweenY});
        }
    }

    boolean hasValue() {
        return true;
    }

    boolean isErased() {
        return false;
    }
    boolean checkedIfErased(float erasePointX, float erasePointY, Action Eraser) {
        return false;
    }

    void drawPath(Canvas canvas) {
        return;
    }

//    void erase(ArrayList<Action> actions) {
//        return;
//    }

    void undoAction() {
        return;
    }

    void redoAction() {
        return;
    }

    void removeEraser(Action Eraser) {
        return;
    }

    void addEraser(Action Eraser) {
        return;
    }
}
