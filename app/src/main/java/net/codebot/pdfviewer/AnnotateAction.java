package net.codebot.pdfviewer;

import android.graphics.*;

public class AnnotateAction extends DrawAction{
    static Paint paint = new Paint();
    final int PAINTSIZE = 5;

    AnnotateAction(float x, float y) {
        super(x, y, AnnotateAction.paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(this.PAINTSIZE);
        paint.setColor(Color.BLUE);
        super.paintSize = this.PAINTSIZE;
    }
}
