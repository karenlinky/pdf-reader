package net.codebot.pdfviewer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

// PDF sample code from
// https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
// Issues about cache etc. are not at all obvious from documentation, so we should expect people to need this.
// We may wish to provide this code.

public class MainActivity extends AppCompatActivity  implements AdapterView.OnItemSelectedListener {

    final String LOGNAME = "pdf_viewer";
    final String FILENAME = "shannon1948.pdf";
    final int FILERESID = R.raw.shannon1948;

    // manage the pages of the PDF, see below
    PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer.Page currentPage;

    // custom ImageView class that captures strokes and draws them over the image
    PDFimage pageImage;

    public enum Pen {
        HAND,
        ANNOTATE,
        HIGHLIGHT,
        ERASE
    }
    static Pen pen = Pen.HAND;

    final String HAND = "Hand";
    final String ANNOTATE = "Annotate";
    final String HIGHLIGHT = "Highlight";
    final String ERASE = "Erase";
    private String[] penChoices = {HAND, ANNOTATE, HIGHLIGHT, ERASE};

    static int currentPageNum = 0;
    static int totalPages = 0;

    private void setUpPenSpinner() {
        Spinner PenSpinner = (Spinner) findViewById(R.id.PenSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, penChoices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        PenSpinner.setAdapter(adapter);
        PenSpinner.setSelection(0);
        PenSpinner.setOnItemSelectedListener(this);
    }

    public static void selectHand(){
        MainActivity.pen = Pen.HAND;
    }

    public static void selectAnnotate(){
        MainActivity.pen = Pen.ANNOTATE;
    }

    public static void selectHighlight(){
        MainActivity.pen = Pen.HIGHLIGHT;
    }

    public static void selectErase(){
        MainActivity.pen = Pen.ERASE;
    }

    private void setCurrentPageNum(int pageIndex) {
        MainActivity.currentPageNum = pageIndex;
        TextView CurrPageText = (TextView) findViewById(R.id.CurrPage);
        CurrPageText.setText(Integer.toString(MainActivity.currentPageNum + 1));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setUpPageNumber() {
//        this.setCurrentPageNum(0);

        MainActivity.totalPages = pdfRenderer.getPageCount();

        TextView TotalPageText = (TextView) findViewById(R.id.TotalPage);
        TotalPageText.setText(Integer.toString(MainActivity.totalPages));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setUp() {
        TextView TV = (TextView) findViewById(R.id.FileNameText);
        TV.setText(this.FILENAME);

        this.setUpPenSpinner();
        this.setUpPageNumber();
        pageImage.setTotalPageNumber(MainActivity.totalPages);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout layout = findViewById(R.id.pdfLayout);
        pageImage = new PDFimage(
                this,
                (ImageButton) findViewById(R.id.UndoButton),
                (ImageButton) findViewById(R.id.RedoButton),
                (ImageButton) findViewById(R.id.ResetPositionButton)
        );
        layout.addView(pageImage);
        layout.setEnabled(true);
        pageImage.setMinimumWidth(1000);
        pageImage.setMinimumHeight(2000);

        // open page 0 of the PDF
        // it will be displayed as an image in the pageImage (above)
        try {
            openRenderer(this);
            this.setUp();
            showPage(0);
//            closeRenderer();
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error opening PDF");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            closeRenderer();
        } catch (IOException ex) {
            Log.d(LOGNAME, "Unable to close PDF renderer");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            InputStream asset = this.getResources().openRawResource(FILERESID);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        }
    }

    // do this before you quit!
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeRenderer() throws IOException {
        if (null != currentPage) {
            currentPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    private void enableDisableButtonsByPage(int pageIndex) {
        ImageButton PrevButton = (ImageButton) findViewById(R.id.PrevButton);
        if (pageIndex == 0) {
            PrevButton.setEnabled(false);
            PrevButton.setClickable(false);
            PrevButton.setImageResource(R.drawable.prev_page_disable_ic_foreground);
        } else {
            PrevButton.setEnabled(true);
            PrevButton.setClickable(true);
            PrevButton.setImageResource(R.drawable.prev_page_ic_foreground);
        }

        ImageButton NextButton = (ImageButton) findViewById(R.id.NextButton);
        if (pageIndex + 1 == totalPages) {
            NextButton.setEnabled(false);
            NextButton.setClickable(false);
            NextButton.setImageResource(R.drawable.next_page_disable_ic_foreground);
        } else {
            NextButton.setEnabled(true);
            NextButton.setClickable(true);
            NextButton.setImageResource(R.drawable.next_page_ic_foreground);
        }

        // undo/redo buttons
        pageImage.enableDisableButtonsByPage(pageIndex);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }
        // Close the current page before opening another one.
        if (null != currentPage) {
            currentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);

        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // Display the page
        pageImage.setImage(bitmap);

        pageImage.resetPosition();
        this.setCurrentPageNum(index);
        this.enableDisableButtonsByPage(index);
    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        switch(penChoices[position]) {
            case ANNOTATE:
                MainActivity.selectAnnotate();
                break;
            case HIGHLIGHT:
                MainActivity.selectHighlight();
                break;
            case ERASE:
                MainActivity.selectErase();
                break;
            default:
                MainActivity.selectHand();
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        MainActivity.selectHand();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void prevPage(View view) {
        this.showPage(MainActivity.currentPageNum - 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void nextPage(View view) {
        this.showPage(MainActivity.currentPageNum + 1);
    }

    public void undo(View view) {
        pageImage.undo();
    }

    public void redo(View view) {
        pageImage.redo();
    }

    public void resetPosition(View view) {
        pageImage.resetPosition();
    }
}
