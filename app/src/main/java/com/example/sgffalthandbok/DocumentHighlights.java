package com.example.sgffalthandbok;

import android.graphics.RectF;

import java.util.ArrayList;

public class DocumentHighlights
{
    private String              m_SearchString;
    private ArrayList<RectF>    m_Highlights;

    public DocumentHighlights(final int initialCapacity)
    {
        m_Highlights = new ArrayList<>(initialCapacity);
    }

    public void SetSearchString(final String searchString)
    {
        this.m_SearchString = m_SearchString;
    }

    public String GetSearchString()
    {
        return m_SearchString;
    }

    public void AddHighlight(final RectF highlight)
    {
        m_Highlights.add(highlight);
    }

    public void ClearHighlights()
    {
        m_Highlights.clear();
    }

    public ArrayList<RectF> GetHighlights()
    {
        return m_Highlights;
    }
}
