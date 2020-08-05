package com.bop.sgffalthandbok;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.link.LinkHandler;
import com.github.barteksc.pdfviewer.model.LinkTapEvent;

public class CustomLinkHandler implements LinkHandler
{
    private static final String TAG = CustomLinkHandler.class.getSimpleName();

    private PDFView                     m_PDFView;
    private OnVideoLinkSelectedListener m_OnVideoLinkSelectedListener;
    private OnLinkConfirmedListener     m_OnLinkConfirmedListener;

    public interface OnVideoLinkSelectedListener
    {
        void OnVideoLinkSelected(String videoName);
    }

    public interface OnLinkConfirmedListener
    {
        void OnLinkConfirmed();
    }

    public CustomLinkHandler(PDFView pdfView, OnLinkConfirmedListener onLinkConfirmedListener)
    {
        m_PDFView = pdfView;

        Context context = pdfView.getContext();

        try
        {
            m_OnVideoLinkSelectedListener = (OnVideoLinkSelectedListener)context;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(context.toString() + " must implement OnVideoLinkSelected");
        }

        m_OnLinkConfirmedListener = onLinkConfirmedListener;
    }

    @Override
    public void handleLinkEvent(LinkTapEvent event)
    {
        m_OnLinkConfirmedListener.OnLinkConfirmed();

        String uri = event.getLink().getUri();
        Integer page = event.getLink().getDestPageIdx();

        if (uri != null && !uri.isEmpty())
        {
            if (uri.contains("Video: "))
            {
                m_OnVideoLinkSelectedListener.OnVideoLinkSelected(uri.substring(uri.indexOf('[') + 1, uri.indexOf(']')).replaceAll("\\s+", ""));
            }
            else
            {
                handleUri(uri);
            }
        }
        else if (page != null)
        {
            handlePage(page);
        }
    }

    private void handleUri(String uri)
    {
        Uri parsedUri = Uri.parse(uri);
        Intent intent = new Intent(Intent.ACTION_VIEW, parsedUri);
        Context context = m_PDFView.getContext();
        if (intent.resolveActivity(context.getPackageManager()) != null)
        {
            context.startActivity(intent);
        }
        else
        {
            Log.w(TAG, "No activity found for URI: " + uri);
        }
    }

    private void handlePage(int page)
    {
        m_PDFView.jumpTo(page);
    }
}