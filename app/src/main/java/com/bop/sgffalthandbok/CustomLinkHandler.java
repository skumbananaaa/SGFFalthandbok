package com.bop.sgffalthandbok;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.link.LinkHandler;
import com.github.barteksc.pdfviewer.model.LinkTapEvent;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

public class CustomLinkHandler implements LinkHandler
{
    private static final String TAG = CustomLinkHandler.class.getSimpleName();

    private PDFView m_PDFView;
    private OnVideoLinkSelectedListener  m_Listener;

    public interface OnVideoLinkSelectedListener
    {
        public void OnVideoLinkSelected(String video);
    }

    public CustomLinkHandler(PDFView pdfView)
    {
        m_PDFView = pdfView;

        Context context = pdfView.getContext();

        try
        {
            m_Listener = (OnVideoLinkSelectedListener)context;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(context.toString() + " must implement OnVideoLinkSelected");
        }
    }

    @Override
    public void handleLinkEvent(LinkTapEvent event)
    {
        String uri = event.getLink().getUri();
        Integer page = event.getLink().getDestPageIdx();

        if (uri != null && !uri.isEmpty())
        {
            if (uri.contains("Video: "))
            {
                m_Listener.OnVideoLinkSelected(uri.substring(uri.indexOf('[') + 1, uri.indexOf(']')).replaceAll("\\s+", ""));
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