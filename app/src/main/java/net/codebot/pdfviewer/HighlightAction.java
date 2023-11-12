package net.codebot.pdfviewer;

import android.graphics.*;

public class HighlightAction extends DrawAction{
    static Paint paint = new Paint();
    final int PAINTSIZE = 38;

    HighlightAction(float x, float y) {
        super(x, y, HighlightAction.paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(this.PAINTSIZE);
        paint.setColor(Color.YELLOW);
        paint.setAlpha(100);
        super.paintSize = this.PAINTSIZE;
    }
}
