package com.example.sgffalthandbok;

import android.graphics.RectF;
import android.util.Log;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class PageHighlights implements Runnable
{
    private PDDocument          m_PDFDocument;
    private int                 m_PageIndex;
    private String              m_SearchString;
    private boolean             m_IsDirty;
    private ArrayList<RectF>    m_Highlights;

    public PageHighlights(final PDDocument pdfDocument, final int pageIndex, final int initialCapacity)
    {
        m_PDFDocument   = pdfDocument;
        m_PageIndex     = pageIndex;
        m_Highlights    = new ArrayList<>(initialCapacity);
    }

    public ArrayList<RectF> GetHighlights()
    {
        synchronized (this)
        {
            return m_Highlights;
        }
    }

    public void Update(String newSearchString, boolean async)
    {
        if (m_SearchString != newSearchString)
        {
            m_IsDirty = true;
            m_SearchString = newSearchString;
        }

        if (async)
        {
            ThreadPool.Execute(this);
        }
        else
        {
            InternalUpdate();
        }
    }

    @Override
    public void run()
    {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        InternalUpdate();
    }

    private void InternalUpdate()
    {
        synchronized (this)
        {
            if (m_IsDirty)
            {
                m_Highlights.clear();
                m_IsDirty = false;

                try
                {
                    PDFTextStripper pdfStripper = new PDFTextStripper()
                    {
                        @Override
                        protected void writeString(final String text, final List<TextPosition> textPositions) throws IOException
                        {
                            float posXInit = 0;
                            float posXEnd = 0;
                            float posYInit = 0;
                            float posYEnd = 0;
                            float width = 0;
                            float height = 0;
                            int searchStringLength = m_SearchString.length();
                            String lowerCaseText = text.toLowerCase();

                            int searchIndex = lowerCaseText.indexOf(m_SearchString);

                            while (searchIndex != -1)
                            {
                                int searchStringEndIndex = searchIndex + searchStringLength - 1;
                                TextPosition firstCharacter = textPositions.get(searchIndex);
                                TextPosition lastCharacter = textPositions.get(searchStringEndIndex);

                                posXInit = firstCharacter.getXDirAdj();
                                posXEnd = lastCharacter.getXDirAdj();
                                posYInit = firstCharacter.getYDirAdj();
                                posYEnd = lastCharacter.getYDirAdj();
                                width = lastCharacter.getWidthDirAdj();
                                height = firstCharacter.getHeightDir();

                                RectF rect = new RectF(posXInit, posYInit - height, posXEnd + width, posYEnd);
                                m_Highlights.add(rect);

                                searchIndex = lowerCaseText.indexOf(m_SearchString, searchIndex + searchStringLength);
                            }
                        }
                    };

                    pdfStripper.setStartPage(m_PageIndex);
                    pdfStripper.setEndPage(m_PageIndex);

                    pdfStripper.getText(m_PDFDocument);
                } catch (IOException e)
                {
                    Log.e("SGF Fälthandbok", "Failed to Strip Text...");
                }
            }
        }
    }
}
