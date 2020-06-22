package com.example.sgffalthandbok;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.shockwave.pdfium.PdfDocument;

import java.io.File;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DocumentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DocumentFragment extends Fragment
{
    private static final String ARG_DOCUMENT_BYTE_ARR = "documentByteArr";

    private byte[]      m_DocumentByteArr;
    private PDFView     m_PDFView;

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

    public void JumpToPage(int pageIndex)
    {
        m_JumpToPage = pageIndex;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            m_DocumentByteArr = getArguments().getByteArray(ARG_DOCUMENT_BYTE_ARR);
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
                    .defaultPage(m_JumpToPage)
                    //.onDraw(onDrawListener) // allows to draw something on the current page, usually visible in the middle of the screen
                    //.onDrawAll(onDrawListener) // allows to draw something on all pages, separately for every page. Called only for visible pages
                    //.onLoad(this) // called after document is loaded and starts to be rendered
                    //.onPageChange(onPageChangeListener)
                    //.onPageScroll(onPageScrollListener)
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
}
