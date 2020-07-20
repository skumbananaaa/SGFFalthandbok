package com.bop.sgffalthandbok;

import android.graphics.RectF;
import android.util.Log;

import com.tom_roush.pdfbox.cos.COSDictionary;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.TextPosition;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class PageHighlights
{
    private boolean             m_IsDirty;
    private ArrayList<RectF>    m_Highlights;

    public PageHighlights()
    {
        m_IsDirty     = true;
    }

    public void SetIsDirty(final boolean isDirty)
    {
        m_IsDirty = isDirty;
    }

    public void SetHighlights(final ArrayList<RectF> highlights)
    {
        m_Highlights = highlights;
    }

    public boolean IsDirty()
    {
        return m_IsDirty;
    }

    public ArrayList<RectF> GetHighlights()
    {
        return m_Highlights;
    }
}
