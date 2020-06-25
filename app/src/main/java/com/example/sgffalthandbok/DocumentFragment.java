package com.example.sgffalthandbok;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnDrawListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DocumentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DocumentFragment extends Fragment implements OnLoadCompleteListener, OnPageChangeListener, OnDrawListener
{
    private static final String ARG_DOCUMENT_BYTE_ARR = "documentByteArr";

    private byte[]      m_DocumentByteArr;
    private PDFView     m_PDFView;
    private PDDocument  m_PDFDocument;

    private String                           m_CurrentSearch;
    private ArrayList<DocumentHighlights>    m_HighlightsPerPage;

    private int         m_JumpToPage;

    public DocumentFragment()
    {
        m_JumpToPage = 0;
    }

    public static DocumentFragment newInstance(byte[] documentByteArr)
    {
        DocumentFragment fragment = new DocumentFragment();
        Bundle args = new Bundle();
        args.putByteArray(ARG_DOCUMENT_BYTE_ARR, documentByteArr);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            m_DocumentByteArr = getArguments().getByteArray(ARG_DOCUMENT_BYTE_ARR);

            try
            {
                m_PDFDocument = PDDocument.load(m_DocumentByteArr);
            }
            catch(IOException e)
            {
                Log.e("SGF Fälthandbok", "Exception thrown while loading document to strip...", e);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_document, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        m_PDFView   = view.findViewById(R.id.pdfView);

        if (m_PDFView != null)
        {
            m_PDFView.fromBytes(m_DocumentByteArr)
                    //.pages(0, 2, 1, 3, 3, 3) // all pages are displayed by default
                    .enableSwipe(true) // allows to block changing pages using swipe
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    //.onDraw(this) // allows to draw something on the current page, usually visible in the middle of the screen
                    .onDrawAll(this) // allows to draw something on all pages, separately for every page. Called only for visible pages
                    .onLoad(this) // called after document is loaded and starts to be rendered
                    .onPageChange(this)
                    //.onPageScroll(this)
                    //.onError(onErrorListener)
                    //.onPageError(onPageErrorListener)
                    //.onRender(onRenderListener) // called after document is rendered for the first time
                    //.onTap(onTapListener) // called on single tap, return true if handled, false to toggle scroll handle visibility
                    //.onLongPress(onLongPressListener)
                    .enableAnnotationRendering(false) // render annotations (such as comments, colors or forms)
                    .enableAntialiasing(true) // improve rendering a little bit on low-res screens
                    .spacing(0)// spacing between pages in dp. To define spacing color, set view backgrounds
                    .autoSpacing(false) // add dynamic spacing to fit each page on its own on the screen
                    //.linkHandler(DefaultLinkHandler)
                    .pageFitPolicy(FitPolicy.WIDTH) // mode to fit pages in the view
                    .fitEachPage(false) // fit each page to the view, else smaller pages are scaled relative to largest page.
                    .pageSnap(false) // snap pages to screen boundaries
                    .pageFling(false) // make a fling change only a single page like ViewPager
                    .nightMode(false) // toggle night mode
                    .load();
        }


    }

    @Override
    public void loadComplete(final int nbPages)
    {
        m_HighlightsPerPage = new ArrayList<>(nbPages);

        for (int i = 0; i < nbPages; i++)
        {
            m_HighlightsPerPage.add(new DocumentHighlights(5));
        }
    }

    @Override
    public void onPageChanged(final int page, final int pageCount)
    {
        for (int pageIndex = Math.max(0, page - 1); pageIndex < Math.min(pageCount - 1, page + 1); pageIndex++)
        {
            final DocumentHighlights documentHighlights = m_HighlightsPerPage.get(pageIndex);

            if (documentHighlights.GetSearchString() != m_CurrentSearch) //Dirty
            {
                documentHighlights.SetSearchString(m_CurrentSearch);
                documentHighlights.ClearHighlights();

                AddPageHighlights(pageIndex);
            }
        }
    }

    @Override
    public void onLayerDrawn(final Canvas canvas, final float pageWidth, final float pageHeight, final int displayedPage)
    {
        final DocumentHighlights documentHighlights = m_HighlightsPerPage.get(displayedPage);

        for (RectF rect : documentHighlights.GetHighlights())
        {
            Paint paint = new Paint();
            paint.setARGB(70, 255, 0, 0);
            canvas.drawRect(rect, paint);
        }
    }

    public void SetCurrentSearch(final String searchString)
    {
        m_CurrentSearch = searchString;
    }

    public void JumpToPage(int pageIndex)
    {
        m_PDFView.jumpTo(pageIndex);
    }

    public void MoveRelativeTo(float x, float y)
    {
        m_PDFView.moveRelativeTo(x, y);
    }

    private void AddPageHighlights(int pageIndex)
    {
        try
        {
            PDFTextStripper pdfStripper = new PDFTextStripper()
            {
                @Override
                protected void writeString(final String text, final List<TextPosition> textPositions) throws IOException
                {
                    DocumentHighlights documentHighlights = m_HighlightsPerPage.get(getCurrentPageNo() - 1);

                    float posXInit      = 0;
                    float posXEnd       = 0;
                    float posYInit      = 0;
                    float posYEnd       = 0;
                    float width         = 0;
                    float height        = 0;
                    int searchStringLength = m_CurrentSearch.length();
                    String lowerCaseText = text.toLowerCase();

                    int searchIndex = lowerCaseText.indexOf(m_CurrentSearch);

                    while (searchIndex != -1)
                    {
                        int searchStringEndIndex = searchIndex + searchStringLength - 1;
                        TextPosition firstCharacter = textPositions.get(searchIndex);
                        TextPosition lastCharacter = textPositions.get(searchStringEndIndex);

                        posXInit = firstCharacter.getXDirAdj();
                        posXEnd  = lastCharacter.getXDirAdj();
                        posYInit = firstCharacter.getYDirAdj();
                        posYEnd  = lastCharacter.getYDirAdj();
                        width    = lastCharacter.getWidthDirAdj();
                        height   = firstCharacter.getHeightDir();

                        RectF rect = new RectF(posXInit, posYInit - height, posXEnd + width, posYEnd);
                        documentHighlights.AddHighlight(rect);

                        searchIndex = lowerCaseText.indexOf(m_CurrentSearch, searchIndex + searchStringLength);
                    }
                }
            };

            pdfStripper.setStartPage(pageIndex + 1);
            pdfStripper.setEndPage(pageIndex + 1);
            pdfStripper.getText(m_PDFDocument);
        }
        catch (IOException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while stripping text...", e);
            return;
        }
    }
}
