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
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

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

import static com.bop.sgffalthandbok.ResourceManager.DOCUMENT_START_PAGE_INDEX;
import static com.bop.sgffalthandbok.SearchFragment.MIN_SEARCH_STRING_LENGTH;

public class DocumentFragment extends Fragment implements OnLoadCompleteListener, OnDrawListener, View.OnClickListener, CustomLinkHandler.OnLinkConfirmedListener
{
    private ResourceManager                 m_ResourceManager;

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

    private OnToggleNavbarListener          m_ToggleNavbarListener;

    private boolean                         m_NextClickEventOnPDFIsLink;
    private boolean                         m_PageHighlightsAreLoaded;

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

        m_ResourceManager = new ViewModelProvider(requireActivity()).get(ResourceManager.class);

        m_PDFDocument           = m_ResourceManager.GetPDFDocument();
        m_DocumentByteArr       = m_ResourceManager.GetDocumentByteArr();
        m_HeadingsToPageNumber  = m_ResourceManager.GetHeadingsToPageNumber();

        m_HighlightPaint = new Paint();
        m_HighlightPaint.setColor(Color.YELLOW);
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
            int numberOfActualPages = m_PDFDocument.getNumberOfPages() - DOCUMENT_START_PAGE_INDEX;
            int[] pages = new int[numberOfActualPages];

            for (int i = 0; i < numberOfActualPages; i++)
                pages[i] = DOCUMENT_START_PAGE_INDEX + i;

            m_PDFView.fromBytes(m_DocumentByteArr)
                    .pages(pages) // all pages are displayed by default
                    .enableSwipe(true) // allows to block changing pages using swipe
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(defaultPage)
                    .onDraw(new OnDrawListener()
                    {
                        @Override
                        public void onLayerDrawn(final Canvas canvas, final float pageWidth, final float pageHeight, final int displayedPage)
                        {
                            if (m_CurrentSearch.length() > 0)
                            {
                                if (m_PageHighlightsAreLoaded)
                                {
                                    m_HighlightInfoView.setText(String.format("Visar resultat för \"%s\".", m_CurrentSearch));
                                    m_HighlightInfoContainerView.setVisibility(View.VISIBLE);
                                }
                                else
                                {
                                    m_HighlightInfoView.setText(String.format("Laddar resultat för \"%s\"...", m_CurrentSearch));
                                    m_HighlightInfoContainerView.setVisibility(View.VISIBLE);

                                    m_PageHighlightsAreLoaded = true;
                                }
                            }
                        }
                    }) // allows to draw something on the current page, usually visible in the middle of the screen
                    .onDrawAll(this) // allows to draw something on all pages, separately for every page. Called only for visible pages
                    .onLoad(this) // called after document is loaded and starts to be rendered
                    //.onPageChange(this)
                    //.onPageScroll(this)
                    //.onError(onErrorListener)
                    //.onPageError(onPageErrorListener)
                    //.onRender(onRenderListener) // called after document is rendered for the first time
                    //.onTap(this) // called on single tap, return true if handled, false to toggle scroll handle visibility
                    //.onLongPress(onLongPressListener)
                    .enableAnnotationRendering(false) // render annotations (such as comments, colors or forms)
                    .enableAntialiasing(true) // improve rendering a little bit on low-res screens
                    .spacing(0)// spacing between pages in dp. To define spacing color, set view backgrounds
                    .autoSpacing(false) // add dynamic spacing to fit each page on its own on the screen
                    .linkHandler(new CustomLinkHandler(m_PDFView, this))
                    .pageFitPolicy(FitPolicy.WIDTH) // mode to fit pages in the view
                    .fitEachPage(false) // fit each page to the view, else smaller pages are scaled relative to largest page.
                    .pageSnap(false) // snap pages to screen boundaries
                    .pageFling(false) // make a fling change only a single page like ViewPager
                    .nightMode(false) // toggle night mode
                    .load();
        }

        m_PDFView.setOnClickListener(this);

        m_HighlightInfoContainerView    = view.findViewById(R.id.documentHighlightInfoContainer);
        m_HighlightInfoView             = view.findViewById(R.id.documentHighlightInfo);
        m_ClearHighlightButton          = view.findViewById(R.id.clearHighlightButton);
        m_ClearHighlightButton.setOnClickListener(this);

        m_NextClickEventOnPDFIsLink = false;
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState)
    {
        outState.putString("m_CurrentSearch", m_CurrentSearch);
        outState.putInt("CURRENT_PAGE", m_PDFView.getCurrentPage());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void loadComplete(final int nbPages)
    {
        m_HighlightsPerPage = new ArrayList<>(nbPages);

        for (int p = 0; p < nbPages; p++)
        {
            m_HighlightsPerPage.add(new PageHighlights());

            m_ResourceManager.SetPageHighlightsObserver(this, new Observer<Pair<Integer, ArrayList<RectF>>>()
            {
                @Override
                public void onChanged(final Pair<Integer, ArrayList<RectF>> integerArrayListPair)
                {
                    if (integerArrayListPair.first >= DOCUMENT_START_PAGE_INDEX)
                    {
                        PageHighlights pageHighlights = m_HighlightsPerPage.get(integerArrayListPair.first - DOCUMENT_START_PAGE_INDEX);

                        pageHighlights.SetIsDirty(false);
                        pageHighlights.SetHighlights(new ArrayList<RectF>(integerArrayListPair.second));
                    }
                }
            }, p);
        }

        if (m_CurrentSearch.length() > MIN_SEARCH_STRING_LENGTH)
        {
            final int pageIndex = m_PDFView.getCurrentPage();

            for (int p = 0; p < nbPages; p++)
            {
                PageHighlights pageHighlights =  m_HighlightsPerPage.get(p);
                pageHighlights.SetIsDirty(true);
            }

            m_ResourceManager.UpdateBookHighlights(m_CurrentSearch, pageIndex);
        }
    }

    public void SetToggleNavbarListener(final OnToggleNavbarListener toggleNavbarListener)
    {
        m_ToggleNavbarListener = toggleNavbarListener;
    }

    @Override
    public void OnLinkConfirmed()
    {
        m_NextClickEventOnPDFIsLink = true;
    }

    @Override
    public void onClick(final View v)
    {
        if (v == m_PDFView)
        {
            if (!m_NextClickEventOnPDFIsLink)
            {
                if (m_ToggleNavbarListener != null)
                {
                    m_ToggleNavbarListener.ToggleNavbar();
                }
            }

            m_NextClickEventOnPDFIsLink = false;
        }
        else if (v == m_ClearHighlightButton)
        {
            m_CurrentSearch = "";
            m_HighlightInfoContainerView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLayerDrawn(final Canvas canvas, final float pageWidth, final float pageHeight, final int displayedPage)
    {
        //For some reason PDFViewer always tries to draw page 0, we check for this here
        final int currentPage = m_PDFView.getCurrentPage();
        if (Math.abs(displayedPage - currentPage) > 1)
            return;

        if (m_CurrentSearch.length() > 0)
        {
            final PageHighlights pageHighlights = m_HighlightsPerPage.get(displayedPage);

            if (!pageHighlights.IsDirty())
            {
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
                m_PageHighlightsAreLoaded = false;
            }
        }
    }

    public void JumpFromSearch(final int pageIndex, final String searchString)
    {
        if (searchString.length() > MIN_SEARCH_STRING_LENGTH)
        {
            m_CurrentSearch = searchString;
            m_PDFView.jumpTo(pageIndex);

            final int numberOfPages = m_PDFView.getPageCount();
            for (int p = 0; p < numberOfPages; p++)
            {
                PageHighlights pageHighlights =  m_HighlightsPerPage.get(p);
                pageHighlights.SetIsDirty(true);
            }

            m_ResourceManager.UpdateBookHighlights(m_CurrentSearch, DOCUMENT_START_PAGE_INDEX + pageIndex);
        }
    }

    public void JumpFromTOC(final String heading)
    {
        m_PDFView.jumpTo(m_HeadingsToPageNumber.get(heading) - DOCUMENT_START_PAGE_INDEX);
    }

    public void Jump(final int pageIndex)
    {
        m_PDFView.jumpTo(pageIndex);
    }
}
