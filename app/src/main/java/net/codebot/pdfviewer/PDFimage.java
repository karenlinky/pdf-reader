package net.codebot.pdfviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.ArrayList;

@SuppressLint("AppCompatCustomView")
public class PDFimage extends ImageView {

    final String LOGNAME = "pdf_image";

    // drawing path
//    Path path = null;
    Action action = null;
    //    ArrayList<Path> paths = new ArrayList();
//    ArrayList<Action> actions = new ArrayList();
    ArrayList<ArrayList<Action>> allPagesActions = new ArrayList();
    ArrayList<ArrayList<Action>> allPagesRedoActions = new ArrayList();

    ImageButton UndoButton;
    ImageButton RedoButton;
    ImageButton ResetPositionButton;

    // image to display
    Bitmap bitmap;
    Paint paint = new Paint(Color.BLUE);

    // constructor
    public PDFimage(
            Context context,
            ImageButton UndoButton,
            ImageButton RedoButton,
            ImageButton ResetPositionButton
    ) {
        super(context);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        this.UndoButton = UndoButton;
        this.RedoButton = RedoButton;
        this.ResetPositionButton = ResetPositionButton;
    }

    void setTotalPageNumber(int totalPages) {
        for (int i = 0; i < totalPages; i++) {
            this.allPagesActions.add(new ArrayList());
            this.allPagesRedoActions.add(new ArrayList());
        }
    }

    // we save a lot of points because they need to be processed
    // during touch events e.g. ACTION_MOVE
    float x1, x2, y1, y2, old_x1, old_y1, old_x2, old_y2;
    float mid_x = -1f, mid_y = -1f, old_mid_x = -1f, old_mid_y = -1f;
    int p1_id, p1_index, p2_id, p2_index;
    float dx, dy;

    // store cumulative transformations
    // the inverse matrix is used to align points with the transformations - see below
    Matrix matrix = new Matrix();
    Matrix inverse = new Matrix();

    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getPointerCount()) {
            // 1 point is drawing or erasing
            case 1:
                p1_id = event.getPointerId(0);
                p1_index = event.findPointerIndex(p1_id);

                if (MainActivity.pen != MainActivity.Pen.HAND) {
                    // invert using the current matrix to account for pan/scale
                    // inverts in-place and returns boolean
                    inverse = new Matrix();
                    matrix.invert(inverse);
                }

                // mapPoints returns values in-place
                float[] inverted = new float[] { event.getX(p1_index), event.getY(p1_index) };
                inverse.mapPoints(inverted);
                old_x1 = x1;
                old_y1 = y1;
                x1 = inverted[0];
                y1 = inverted[1];

                if (MainActivity.pen == MainActivity.Pen.HAND) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_MOVE:
                            matrix.preTranslate(x1 - old_x1, y1 - old_y1);
                            this.enableDisableResetPositionButton(true);
                            break;
                    }
                    break;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(LOGNAME, "Action down");
                        if (MainActivity.pen == MainActivity.Pen.ANNOTATE) {
                            action = new AnnotateAction(x1, y1);
                        } else if (MainActivity.pen == MainActivity.Pen.HIGHLIGHT) {
                            action = new HighlightAction(x1, y1);
                        } else if (MainActivity.pen == MainActivity.Pen.ERASE) {
                            action = new EraseAction(x1, y1);
                        }
                        this.allPagesActions.get(MainActivity.currentPageNum).add(action);

                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.d(LOGNAME, "Action move");
                        action.addPointToPath(x1, y1, allPagesActions.get(MainActivity.currentPageNum));
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d(LOGNAME, "Action up");
                        if (!action.hasValue()) {
                            this.allPagesActions.get(MainActivity.currentPageNum).remove(action);
                        } else {
                            this.allPagesRedoActions.get(MainActivity.currentPageNum).clear();
                        }
                        this.enableDisableButtonsByPage(MainActivity.currentPageNum);
                        break;
                }
                break;
            // 2 points is zoom/pan
            case 2:
                // point 1
                p1_id = event.getPointerId(0);
                p1_index = event.findPointerIndex(p1_id);

                // mapPoints returns values in-place
                inverted = new float[] { event.getX(p1_index), event.getY(p1_index) };
                inverse.mapPoints(inverted);

                // first pass, initialize the old == current value
                if (old_x1 < 0 || old_y1 < 0) {
                    old_x1 = x1 = inverted[0];
                    old_y1 = y1 = inverted[1];
                } else {
                    old_x1 = x1;
                    old_y1 = y1;
                    x1 = inverted[0];
                    y1 = inverted[1];
                }

                // point 2
                p2_id = event.getPointerId(1);
                p2_index = event.findPointerIndex(p2_id);

                // mapPoints returns values in-place
                inverted = new float[] { event.getX(p2_index), event.getY(p2_index) };
                inverse.mapPoints(inverted);

                // first pass, initialize the old == current value
                if (old_x2 < 0 || old_y2 < 0) {
                    old_x2 = x2 = inverted[0];
                    old_y2 = y2 = inverted[1];
                } else {
                    old_x2 = x2;
                    old_y2 = y2;
                    x2 = inverted[0];
                    y2 = inverted[1];
                }

                // midpoint
                mid_x = (x1 + x2) / 2;
                mid_y = (y1 + y2) / 2;
                old_mid_x = (old_x1 + old_x2) / 2;
                old_mid_y = (old_y1 + old_y2) / 2;

                // distance
                float d_old = (float) Math.sqrt(Math.pow((old_x1 - old_x2), 2) + Math.pow((old_y1 - old_y2), 2));
                float d = (float) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));

                // pan and zoom during MOVE event
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    Log.d(LOGNAME, "Multitouch move");
                    // pan == translate of midpoint
                    dx = mid_x - old_mid_x;
                    dy = mid_y - old_mid_y;
                    matrix.preTranslate(dx, dy);
                    Log.d(LOGNAME, "translate: " + dx + "," + dy);

                    // zoom == change of spread between p1 and p2
                    float scale = d/d_old;
                    scale = Math.max(0, scale);
                    matrix.preScale(scale, scale, mid_x, mid_y);
                    Log.d(LOGNAME, "scale: " + scale);

                    this.enableDisableResetPositionButton(true);

                    // reset on up
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    old_x1 = -1f;
                    old_y1 = -1f;
                    old_x2 = -1f;
                    old_y2 = -1f;
                    old_mid_x = -1f;
                    old_mid_y = -1f;
                }
                break;
            // I have no idea what the user is doing for 3+ points
            default:
                break;
        }
        return true;
    }

    // set image as background
    public void setImage(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    // set brush characteristics
    // e.g. color, thickness, alpha
    public void setBrush(Paint paint) {
        this.paint = paint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // apply transformations from the event handler above
        canvas.concat(matrix);

        // draw background
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
        }

        for (Action action: allPagesActions.get(MainActivity.currentPageNum)) {
            action.drawPath(canvas);
        }

        super.onDraw(canvas);
    }

    private void enableDisableUndo(boolean enabled) {
        this.UndoButton.setEnabled(enabled);
        this.UndoButton.setClickable(enabled);
        if (enabled) {
            this.UndoButton.setImageResource(R.drawable.undo_ic_foreground);
        } else {
            this.UndoButton.setImageResource(R.drawable.undo_disable_ic_foreground);
        }
    }

    private void enableDisableRedo(boolean enabled) {
        this.RedoButton.setEnabled(enabled);
        this.RedoButton.setClickable(enabled);
        if (enabled) {
            this.RedoButton.setImageResource(R.drawable.redo_ic_foreground);
        } else {
            this.RedoButton.setImageResource(R.drawable.redo_disable_ic_foreground);
        }
    }

    public void enableDisableButtonsByPage(int pageIndex) {
        //undo
        if (this.allPagesActions.get(pageIndex).size() == 0) {
            this.enableDisableUndo(false);
        } else {
            this.enableDisableUndo(true);
        }

        //redo
        if (this.allPagesRedoActions.get(pageIndex).size() == 0) {
            this.enableDisableRedo(false);
        } else {
            this.enableDisableRedo(true);
        }
    }

    public void undo() {
        Action action = this.allPagesActions.get(MainActivity.currentPageNum).get(
                this.allPagesActions.get(MainActivity.currentPageNum).size() - 1
        );
        action.undoAction();

        this.allPagesActions.get(MainActivity.currentPageNum).remove(action);
        this.allPagesRedoActions.get(MainActivity.currentPageNum).add(action);

        this.enableDisableButtonsByPage(MainActivity.currentPageNum);
    }

    public void redo() {
        Action action = this.allPagesRedoActions.get(MainActivity.currentPageNum).get(
                this.allPagesRedoActions.get(MainActivity.currentPageNum).size() - 1
        );
        action.redoAction();

        this.allPagesRedoActions.get(MainActivity.currentPageNum).remove(action);
        this.allPagesActions.get(MainActivity.currentPageNum).add(action);

        this.enableDisableButtonsByPage(MainActivity.currentPageNum);
    }

    public void resetPosition() {
        matrix = new Matrix();
        this.enableDisableResetPositionButton(false);
    }

    public void enableDisableResetPositionButton(boolean enabled) {
        this.ResetPositionButton.setEnabled(enabled);
        this.ResetPositionButton.setClickable(enabled);
        if (enabled) {
            this.ResetPositionButton.setImageResource(R.drawable.center_ic_foreground);
        } else {
            this.ResetPositionButton.setImageResource(R.drawable.center_disable_ic_foreground);
        }
    }
}