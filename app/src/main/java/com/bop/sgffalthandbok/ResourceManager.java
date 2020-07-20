package com.bop.sgffalthandbok;

import android.app.Application;
import android.content.res.AssetManager;
import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDDocumentCatalog;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.TextPosition;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResourceManager extends AndroidViewModel
{
    private static String   DOCUMENT_FILENAME           = "Geobok 181026 Bok.pdf";
    private static String   DOCUMENT_JSON_FILENAME      = "Geobok 181026 Bok.json";

    private byte[]                                                           m_DocumentByteArr;                   //The entire PDF as a byte array
    private PDDocument                                                       m_PDFDocument;
    private ArrayList<String>                                                m_DocumentTextPages;                 //Contains all Pages in Text Format
    private HashMap<String, Integer>                                         m_HeadingsToPageNumber;              //Contains Key-Value Pairs of a Heading (with Separators removed) to a Page Number
    private HashMap<Integer, ArrayList<SerializablePair<Integer, String>>>   m_PageNumberToHeadings;              //Contains Key-Value Pairs of a Page Number which maps to a Sorted Array of CharIndex-Heading Pairs
    private ArrayList<SerializablePair<String, ArrayList<String>>>           m_TableOfContents;                   //Contains String-Array Pairs where the String is a Main Heading and the Array contains Subheadings

    private ArrayList<MutableLiveData<Pair<Integer, ArrayList<RectF>>>>      m_PageHighlights;
    private ArrayList<ArrayList<RectF>>                                      m_PageHighlightsWorkspace;
        private ExecutorService                                              m_PageHighlightLoaderService;

    public ResourceManager(@NonNull final Application application)
    {
        super(application);

        PDFBoxResourceLoader.init(getApplication());

        m_DocumentTextPages = new ArrayList<>(200);
        m_HeadingsToPageNumber = new HashMap<>();
        m_PageNumberToHeadings = new HashMap<>();
        m_TableOfContents = new ArrayList<>(15);

        if (!LoadDocumentsAsByteArray(getApplication().getAssets()))
            return;

        if (!LoadPDDocuments())
            return;

        if (!LoadDocumentTextPages(getApplication().getAssets()))
            return;

        if (!LoadDocumentContentDescriptions())
            return;

//        if (savedInstanceState == null)
//        {
//
//        }
//        else
//        {
//            //Restore State
//
//            //These casts are safe, see the implementation of Destroy and SerializablePair
//            m_HeadingsToPageNumber = (HashMap<String, Integer>) savedInstanceState.getSerializable("s_HeadingsToPageNumber");
//            m_PageNumberToHeadings = (HashMap<Integer, ArrayList<SerializablePair<Integer, String>>>) savedInstanceState.getSerializable("s_PageNumberToHeadings");
//            m_TableOfContents = (ArrayList<SerializablePair<String, ArrayList<String>>>) savedInstanceState.getSerializable("s_TableOfContents");
//        }
    }

    @Override
    protected void onCleared()
    {
        //outState.putSerializable("s_HeadingsToPageNumber", m_HeadingsToPageNumber);
        //outState.putSerializable("s_PageNumberToHeadings", m_PageNumberToHeadings);
        //outState.putSerializable("s_TableOfContents", m_TableOfContents);

        ClosePDDocuments();
    }

    private boolean LoadDocumentsAsByteArray(final AssetManager assetManager)
    {
        {
            InputStream inputStream = null;
            try
            {
                inputStream = assetManager.open(DOCUMENT_FILENAME);
            } catch (IOException e)
            {
                Log.e("SGF Fälthandbok", "Exception thrown while opening document...", e);
                return false;
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while (true)
            {
                try
                {
                    if (((bytesRead = inputStream.read(buffer)) == -1)) break;
                    output.write(buffer, 0, bytesRead);
                } catch (IOException e)
                {
                    Log.e("SGF Fälthandbok", "Exception thrown while converting document to byte array...", e);
                    return false;
                }
            }

            try
            {
                inputStream.close();
            } catch (IOException e)
            {
                Log.e("SGF Fälthandbok", "Exception thrown while closing document input stream...", e);
            }

            m_DocumentByteArr = output.toByteArray();
        }

        return true;
    }

    private boolean LoadPDDocuments()
    {
        try
        {
            m_PDFDocument = PDDocument.load(m_DocumentByteArr);
        }
        catch(IOException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while loading document to strip...", e);
            return false;
        }

        return true;
    }

    private void ClosePDDocuments()
    {
        try
        {
            if (m_PDFDocument != null) m_PDFDocument.close();
        }
        catch (IOException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while closing document...", e);
        }
    }

    private boolean LoadDocumentTextPages(final AssetManager assetManager)
    {
        InputStream inputStream = null;
        byte[] buffer = null;

        try
        {
            inputStream = assetManager.open(DOCUMENT_JSON_FILENAME);
            int size = inputStream.available();
            buffer = new byte[size];

            inputStream.read(buffer);
            inputStream.close();
        }
        catch (IOException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while opening json file...", e);
            return false;
        }

        try
        {
            JSONObject jsonObject = new JSONObject(new String(buffer, "UTF-8"));
            final JSONArray pages = jsonObject.getJSONArray("pages");

            for (int p = 0; p < pages.length(); p++)
            {
                m_DocumentTextPages.add(pages.getString(p).toLowerCase());
            }
        }
        catch (JSONException | UnsupportedEncodingException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while parsing json file...", e);
            return false;
        }

        return true;
    }

    private boolean LoadDocumentContentDescriptions()
    {
        String parsedText = null;

        try
        {
            //Load ToC
            final PDDocumentCatalog documentCatalog = m_PDFDocument.getDocumentCatalog();
            final PDDocumentOutline tableOfContents = documentCatalog.getDocumentOutline();
            PDOutlineItem currentHeading = tableOfContents.getFirstChild();
            while (currentHeading != null)
            {
                //Create new Subheadings Entry
                ArrayList<String> subHeadings = new ArrayList<>();
                AddHeadingData(currentHeading, m_PDFDocument, documentCatalog);

                //Loop through Chapter Subheadings
                PDOutlineItem currentSubHeading = currentHeading.getFirstChild();
                while (currentSubHeading != null)
                {
                    AddHeadingData(currentSubHeading, m_PDFDocument, documentCatalog);

                    //Create new Chapter Subheadings
                    subHeadings.add(currentSubHeading.getTitle());

                    currentSubHeading = currentSubHeading.getNextSibling();
                }

                m_TableOfContents.add(new SerializablePair<>(currentHeading.getTitle(), subHeadings));
                currentHeading = currentHeading.getNextSibling();
            }
        }
        catch (IOException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while stripping text...", e);
            return false;
        }

        return true;
    }

    private void AddHeadingData(PDOutlineItem currentHeading, PDDocument pdfDocument, PDDocumentCatalog documentCatalog) throws IOException
    {
        final String headingTitle       = currentHeading.getTitle();
        final PDPage currentPage        = currentHeading.findDestinationPage(pdfDocument);
        final int pageIndex             = documentCatalog.getPages().indexOf(currentPage);
        final String pageText           = m_DocumentTextPages.get(pageIndex);

        //Create new Headings to Page Number Entry
        m_HeadingsToPageNumber.put(headingTitle.replaceAll("\\s+", ""), pageIndex);

        //Create new Page Number to Headings Entry
        ArrayList<SerializablePair<Integer, String>> headingsPerCharNumber;
        if (!m_PageNumberToHeadings.containsKey(pageIndex))
        {
            headingsPerCharNumber = new ArrayList<>();
            m_PageNumberToHeadings.put(pageIndex, headingsPerCharNumber);
        }
        else
        {
            headingsPerCharNumber = m_PageNumberToHeadings.get(pageIndex);
        }

        headingsPerCharNumber.add(new SerializablePair<>(pageText.indexOf(headingTitle), headingTitle));
    }

    public byte[] GetDocumentByteArr()
    {
        return m_DocumentByteArr;
    }

    public PDDocument GetPDFDocument()
    {
        return m_PDFDocument;
    }

    public ArrayList<String> GetDocumentTextPages()
    {
        return m_DocumentTextPages;
    }

    public HashMap<String, Integer> GetHeadingsToPageNumber()
    {
        return m_HeadingsToPageNumber;
    }

    public HashMap<Integer, ArrayList<SerializablePair<Integer, String>>> GetPageNumberToHeadings()
    {
        return m_PageNumberToHeadings;
    }

    public ArrayList<SerializablePair<String, ArrayList<String>>> GetTableOfContents()
    {
        return m_TableOfContents;
    }

    public void SetPageHighlightsObserver(final LifecycleOwner owner, final Observer<Pair<Integer, ArrayList<RectF>>> observer, final int pageIndex)
    {
        final int numberOfPages = m_PDFDocument.getNumberOfPages();

        if (m_PageHighlights == null)
        {
            m_PageHighlights = new ArrayList<>();

            for (int p = 0; p < numberOfPages; p++)
            {
                m_PageHighlights.add(new MutableLiveData<Pair<Integer, ArrayList<RectF>>>());
            }
        }

        if (m_PageHighlightsWorkspace == null)
        {
            m_PageHighlightsWorkspace = new ArrayList<>(m_PDFDocument.getNumberOfPages());

            for (int p = 0; p < numberOfPages; p++)
            {
                m_PageHighlightsWorkspace.add(new ArrayList<RectF>());
            }
        }

        if (m_PageHighlightLoaderService == null)
        {
            m_PageHighlightLoaderService = Executors.newSingleThreadExecutor();
        }

        m_PageHighlights.get(pageIndex).observe(owner, observer);
    }

    public void UpdateBookHighlights(final String highlightString, final int currentPage)
    {
        final int numberOfPages = m_PDFDocument.getNumberOfPages();

        if (m_PageHighlights == null)
        {
            m_PageHighlights = new ArrayList<>();

            for (int p = 0; p < numberOfPages; p++)
            {
                m_PageHighlights.add(new MutableLiveData<Pair<Integer, ArrayList<RectF>>>());
            }
        }

        if (m_PageHighlightsWorkspace == null)
        {
            m_PageHighlightsWorkspace = new ArrayList<>(m_PDFDocument.getNumberOfPages());

            for (int p = 0; p < numberOfPages; p++)
            {
                m_PageHighlightsWorkspace.add(new ArrayList<RectF>());
            }
        }

        if (m_PageHighlightLoaderService != null)
        {
            m_PageHighlightLoaderService.shutdownNow();
        }

        m_PageHighlightLoaderService = Executors.newSingleThreadExecutor();
        UpdatePageHighlights(highlightString, currentPage);
    }

    private void UpdatePageHighlights(final String highlightString, final int currentPageIndex)
    {
        m_PageHighlightLoaderService.submit(new Runnable()
        {
            @Override
            public void run()
            {
                final int numPages          = m_PDFDocument.getNumberOfPages();
                final int prevCount         = currentPageIndex;
                final int afterCount        = numPages - currentPageIndex;
                final int pingPongRadius    = Math.min(prevCount, afterCount);
                final int pingPongCount     = 2 * pingPongRadius + 1;

                for (int pingPongIndex = 0; pingPongIndex <= pingPongCount; pingPongIndex++)
                {
                    if (Thread.interrupted())
                        return;

                    final int addedIndex = pingPongIndex / 2;
                    final int pageIndex = currentPageIndex + (pingPongIndex % 2 == 0 ? addedIndex : -addedIndex);

                    final ArrayList<RectF> currentPageHighlights = m_PageHighlightsWorkspace.get(pageIndex);
                    currentPageHighlights.clear();

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
                                int searchStringLength = highlightString.length();
                                String lowerCaseText = text.toLowerCase();

                                final PDRectangle pageBBox = getCurrentPage().getBBox();
                                final float pageWidth = pageBBox.getWidth();
                                final float pageHeight = pageBBox.getHeight();

                                int searchIndex = lowerCaseText.indexOf(highlightString);

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
                                    currentPageHighlights.add(rect);

                                    searchIndex = lowerCaseText.indexOf(highlightString, searchIndex + searchStringLength);
                                }
                            }
                        };

                        pdfStripper.setStartPage(pageIndex + 1);
                        pdfStripper.setEndPage(pageIndex + 1);

                        pdfStripper.getText(m_PDFDocument);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        Log.e("SGF Fälthandbok", "Failed to Strip Text on page index" + pageIndex + "...");
                    }

                    final MutableLiveData<Pair<Integer, ArrayList<RectF>>> currentPageHighlightsLiveData = m_PageHighlights.get(pageIndex);
                    currentPageHighlightsLiveData.postValue(new Pair(pageIndex, currentPageHighlights));
                }

                if (prevCount > afterCount)
                {
                    for (int pageIndex = Math.max(0, currentPageIndex - pingPongRadius - 1); pageIndex >= 0; pageIndex--)
                    {
                        if (Thread.interrupted())
                            return;

                        final ArrayList<RectF> currentPageHighlights = m_PageHighlightsWorkspace.get(pageIndex);
                        currentPageHighlights.clear();

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
                                    int searchStringLength = highlightString.length();
                                    String lowerCaseText = text.toLowerCase();

                                    final PDRectangle pageBBox = getCurrentPage().getBBox();
                                    final float pageWidth = pageBBox.getWidth();
                                    final float pageHeight = pageBBox.getHeight();

                                    int searchIndex = lowerCaseText.indexOf(highlightString);

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
                                        currentPageHighlights.add(rect);

                                        searchIndex = lowerCaseText.indexOf(highlightString, searchIndex + searchStringLength);
                                    }
                                }
                            };

                            pdfStripper.setStartPage(pageIndex + 1);
                            pdfStripper.setEndPage(pageIndex + 1);

                            pdfStripper.getText(m_PDFDocument);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            Log.e("SGF Fälthandbok", "Failed to Strip Text on page index" + pageIndex + "...");
                        }

                        final MutableLiveData<Pair<Integer, ArrayList<RectF>>> currentPageHighlightsLiveData = m_PageHighlights.get(pageIndex);
                        currentPageHighlightsLiveData.postValue(new Pair(pageIndex, currentPageHighlights));
                    }
                }
                else if (afterCount > prevCount)
                {
                    for (int pageIndex = Math.min(numPages - 1, currentPageIndex + pingPongRadius + 1); pageIndex < numPages; pageIndex++)
                    {
                        if (Thread.interrupted())
                            return;

                        final ArrayList<RectF> currentPageHighlights = m_PageHighlightsWorkspace.get(pageIndex);
                        currentPageHighlights.clear();

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
                                    int searchStringLength = highlightString.length();
                                    String lowerCaseText = text.toLowerCase();

                                    final PDRectangle pageBBox = getCurrentPage().getBBox();
                                    final float pageWidth = pageBBox.getWidth();
                                    final float pageHeight = pageBBox.getHeight();

                                    int searchIndex = lowerCaseText.indexOf(highlightString);

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
                                        currentPageHighlights.add(rect);

                                        searchIndex = lowerCaseText.indexOf(highlightString, searchIndex + searchStringLength);
                                    }
                                }
                            };

                            pdfStripper.setStartPage(pageIndex + 1);
                            pdfStripper.setEndPage(pageIndex + 1);

                            pdfStripper.getText(m_PDFDocument);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            Log.e("SGF Fälthandbok", "Failed to Strip Text on page index" + pageIndex + "...");
                        }

                        final MutableLiveData<Pair<Integer, ArrayList<RectF>>> currentPageHighlightsLiveData = m_PageHighlights.get(pageIndex);
                        currentPageHighlightsLiveData.postValue(new Pair(pageIndex, currentPageHighlights));
                    }
                }
            }
        });
    }
}
