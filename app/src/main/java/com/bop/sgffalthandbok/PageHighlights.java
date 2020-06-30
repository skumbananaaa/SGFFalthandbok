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

public class PageHighlights implements Runnable
{
    private PDDocument          m_PDFDocument;
    private int                 m_PageIndex;
    private String              m_SearchString;
    private boolean             m_IsDirty;
    private boolean             m_Cancelled;
    private ArrayList<RectF>    m_Highlights;

    public PageHighlights(final PDDocument pdfDocument, final int pageIndex, final int initialCapacity)
    {
        m_PDFDocument   = ResourceManager.GetPDFDocument(pageIndex);
        m_PageIndex     = pageIndex;
        m_Highlights    = new ArrayList<>(initialCapacity);
        m_Cancelled     = false;
    }

    public boolean IsDirty()
    {
        synchronized (m_Highlights)
        {
            return m_IsDirty;
        }
    }

    public ArrayList<RectF> GetHighlights()
    {
        synchronized (m_Highlights)
        {
            return m_Highlights;
        }
    }

    public void Update(String newSearchString, boolean async)
    {
        synchronized (this)
        {
            boolean alreadyWasDirty = m_IsDirty;

            if (m_SearchString != newSearchString)
            {
                m_IsDirty       = true;
                m_SearchString  = newSearchString;
                m_Cancelled     = false;
            }

            //If it was dirty then an update is already scheduled -> updating m_SearchString is enough
            if (async && !alreadyWasDirty)
            {
                ThreadPool.Execute(this);
            }
            else
            {
                InternalUpdate();
            }
        }
    }

    public void CancelUpdate()
    {
        synchronized (this)
        {
            m_Cancelled = true;
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
            if (m_Cancelled)
            {
                m_Cancelled = false;
                return;
            }

            if (m_IsDirty)
            {
                synchronized (m_Highlights)
                {
                    m_Highlights.clear();
                }

                final ArrayList<RectF> newHighlights = new ArrayList<>();

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

                            final PDRectangle pageBBox  = getCurrentPage().getBBox();
                            final float pageWidth       = pageBBox.getWidth();
                            final float pageHeight      = pageBBox.getHeight();

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

                                RectF rect = new RectF(posXInit / pageWidth, (posYInit - height) / pageHeight, (posXEnd + width) / pageWidth, posYEnd / pageHeight);
                                newHighlights.add(rect);

                                searchIndex = lowerCaseText.indexOf(m_SearchString, searchIndex + searchStringLength);
                            }
                        }
                    };

//                    pdfStripper.setStartPage(m_PageIndex + 1);
//                    pdfStripper.setEndPage(m_PageIndex + 1);

                    pdfStripper.getText(m_PDFDocument);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    Log.e("SGF Fälthandbok", "Failed to Strip Text on page " + m_PageIndex + "...");
                }

                synchronized (m_Highlights)
                {
                    m_Highlights = newHighlights;
                    m_IsDirty = false;
                }
            }
        }
    }
}
