package com.bop.sgffalthandbok;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnDrawListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.tom_roush.pdfbox.pdmodel.PDDocument;

import java.util.ArrayList;
import java.util.HashMap;

import static com.bop.sgffalthandbok.SearchFragment.MIN_SEARCH_STRING_LENGTH;

public class DocumentFragment extends Fragment implements OnLoadCompleteListener, OnDrawListener, View.OnClickListener
{
    private byte[]                          m_DocumentByteArr;
    private PDFView                         m_PDFView;
    private PDDocument                      m_PDFDocument;
    private HashMap<String, Integer>        m_HeadingsToPageNumber;

    private ConstraintLayout                m_HighlightInfoContainerView;
    private TextView                        m_HighlightInfoView;
    private ImageButton                     m_ClearHighlightButton;

    private String                          m_CurrentSearch;
    private ArrayList<PageHighlights>       m_HighlightsPerPage;

    private Paint                           m_HighlightPaint;

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

        m_PDFDocument           = ResourceManager.GetPDFDocument();
        m_DocumentByteArr       = ResourceManager.GetDocumentByteArr();
        m_HeadingsToPageNumber  = ResourceManager.GetHeadingsToPageNumber();

        m_HighlightPaint = new Paint();
        m_HighlightPaint.setColor(getContext().getColor(R.color.colorAccentLight));
        m_HighlightPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

        final int defaultPage;

        if (savedInstanceState != null)
        {
            m_CurrentSearch     = savedInstanceState.getString("m_CurrentSearch");
            defaultPage         = savedInstanceState.getInt("CURRENT_PAGE");
        }
        else
        {
            m_CurrentSearch = "";
            defaultPage     = 0;
        }

        m_PDFView   = view.findViewById(R.id.pdfView);

        if (m_PDFView != null)
        {
            m_PDFView.fromBytes(m_DocumentByteArr)
                    //.pages(0, 2, 1, 3, 3, 3) // all pages are displayed by default
                    .enableSwipe(true) // allows to block changing pages using swipe
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(defaultPage)
                    //.onDraw(this) // allows to draw something on the current page, usually visible in the middle of the screen
                    .onDrawAll(this) // allows to draw something on all pages, separately for every page. Called only for visible pages
                    .onLoad(this) // called after document is loaded and starts to be rendered
                    //.onPageChange(this)
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

        m_HighlightInfoContainerView    = view.findViewById(R.id.documentHighlightInfoContainer);
        m_HighlightInfoView             = view.findViewById(R.id.documentHighlightInfo);
        m_ClearHighlightButton          = view.findViewById(R.id.clearHighlightButton);
        m_ClearHighlightButton.setOnClickListener(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState)
    {
        //m_HighlightsPerPage might be null if this is not current fragment -> onViewCreated not called ->  loadComplete not called -> m_HighlightsPerPage not initialized
        if (m_HighlightsPerPage != null)
        {
            for (PageHighlights pageHighlights : m_HighlightsPerPage)
            {
                pageHighlights.CancelUpdate();
            }
        }

        outState.putString("m_CurrentSearch", m_CurrentSearch);
        outState.putInt("CURRENT_PAGE", m_PDFView.getCurrentPage());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void loadComplete(final int nbPages)
    {
        m_HighlightsPerPage = new ArrayList<>(nbPages);

        for (int i = 0; i < nbPages; i++)
        {
            m_HighlightsPerPage.add(new PageHighlights(m_PDFDocument, i,5));
        }

        if (m_CurrentSearch.length() > MIN_SEARCH_STRING_LENGTH)
        {
            final int pageIndex = m_PDFView.getCurrentPage();

            ThreadPool.Execute(new Runnable()
            {
                @Override
                public void run()
                {
                    //Update pages around main page first
                    m_HighlightsPerPage.get(pageIndex).Update(m_CurrentSearch, true);

                    for (int p = Math.max(0, pageIndex - 1); p >= 0; p--)
                    {
                        m_HighlightsPerPage.get(p).Update(m_CurrentSearch, true);
                    }

                    for (int p = Math.min(m_PDFDocument.getNumberOfPages() - 1, pageIndex + 1); p < m_PDFDocument.getNumberOfPages(); p++)
                    {
                        m_HighlightsPerPage.get(p).Update(m_CurrentSearch, true);
                    }
                }
            });
        }
    }

    @Override
    public void onClick(final View v)
    {
        //Assume Clear Highlight Button
        m_CurrentSearch = "";
        m_HighlightInfoContainerView.setVisibility(View.GONE);
    }

    @Override
    public void onLayerDrawn(final Canvas canvas, final float pageWidth, final float pageHeight, final int displayedPage)
    {
        if (m_CurrentSearch.length() > 0)
        {
            final PageHighlights pageHighlights = m_HighlightsPerPage.get(displayedPage);

            if (!pageHighlights.IsDirty())
            {
                m_HighlightInfoView.setText(String.format("Visar Resultat för \"%s\"", m_CurrentSearch));
                m_HighlightInfoContainerView.setVisibility(View.VISIBLE);

                for (RectF rect : pageHighlights.GetHighlights())
                {
                    float relativeX = rect.left * pageWidth;
                    float relativeY = rect.top * pageHeight;
                    float relativeWidth = (rect.right - rect.left) * pageWidth;
                    float relativeHeight = (rect.bottom - rect.top) * pageHeight;

                    RectF relativeRect = new RectF(relativeX, relativeY, relativeX + relativeWidth, relativeY + relativeHeight);

                    canvas.drawRect(relativeRect, m_HighlightPaint);
                }
            }
            else
            {
                m_HighlightInfoView.setText(String.format("Laddar Resultat för \"%s\"...", m_CurrentSearch));
                m_HighlightInfoContainerView.setVisibility(View.VISIBLE);
            }
        }
    }

    public void JumpFromSearch(final int pageIndex, final String searchString)
    {
        if (searchString.length() > MIN_SEARCH_STRING_LENGTH)
        {
            m_CurrentSearch = searchString;
            m_PDFView.jumpTo(pageIndex);

            //Start Asynchronous Page Highlighting, we do not need to cancel old jobs since the new search string will be set
            ThreadPool.Execute(new Runnable()
            {
                @Override
                public void run()
                {
                    m_HighlightsPerPage.get(pageIndex).Update(m_CurrentSearch, true);

                    for (int p = Math.max(0, pageIndex - 1); p >= 0; p--)
                    {
                        m_HighlightsPerPage.get(p).Update(m_CurrentSearch, true);
                    }

                    for (int p = Math.min(m_PDFDocument.getNumberOfPages() - 1, pageIndex + 1); p < m_PDFDocument.getNumberOfPages(); p++)
                    {
                        m_HighlightsPerPage.get(p).Update(m_CurrentSearch, true);
                    }
                }
            });
        }
    }

    public void JumpFromTOC(final String heading)
    {
        m_PDFView.jumpTo(m_HeadingsToPageNumber.get(heading));
    }

    public void MoveRelativeTo(float x, float y)
    {
        m_PDFView.moveRelativeTo(x, y);
    }
}
