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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResourceManager extends AndroidViewModel
{
    public static int       DOCUMENT_START_PAGE_INDEX   = 5;
    public static int       DOCUMENT_CONTENT_OFFSET     = 4; //Describes the number of pages from DOCUMENT_START_PAGE_INDEX where the actual content starts

    private static String   DOCUMENT_FILENAME           = "Geobok 181026 Bok.pdf";
    private static String   DOCUMENT_JSON_FILENAME      = "Geobok 181026 Bok.json";

    private byte[]                                                           m_DocumentByteArr;                   //The entire PDF as a byte array
    private PDDocument                                                       m_PDFDocument;
    private ArrayList<String>                                                m_DocumentTextPages;                 //Contains all Pages in Text Format
    private HashMap<String, Integer>                                         m_HeadingsToPageNumber;              //Contains Key-Value Pairs of a Heading (with Separators removed) to a Page Number
    private HashMap<Integer, ArrayList<SerializablePair<Integer, String>>>   m_PageNumberToHeadings;              //Contains Key-Value Pairs of a Page Number which maps to a Sorted Array of CharIndex-Heading Pairs
    private ArrayList<ContentHeading>                                        m_TableOfContents;                   //Contains String-Array Pairs where the String is a Main Heading and the Array contains Subheadings
    private ArrayList<VideoData>                                             m_Videos;

    private ArrayList<MutableLiveData<Pair<Integer, ArrayList<RectF>>>>      m_PageHighlights;
    private ArrayList<ArrayList<RectF>>                                      m_PageHighlightsWorkspace;
    private ExecutorService                                                  m_PageHighlightLoaderService;

    private class VideoData
    {
        public String Prefix;
        public String VideoName;
        public String URI;

        VideoData(String prefix, String videoName, String URI)
        {
            this.Prefix             = prefix;
            this.VideoName          = videoName;
            this.URI                = URI;
        }
    }

    public ResourceManager(@NonNull final Application application)
    {
        super(application);

        PDFBoxResourceLoader.init(getApplication());

        m_DocumentTextPages = new ArrayList<>(200);
        m_HeadingsToPageNumber = new HashMap<>();
        m_PageNumberToHeadings = new HashMap<>();
        m_TableOfContents = new ArrayList<>(15);

        m_Videos = new ArrayList<>(10);

        //Kap 3
        m_Videos.add(new VideoData("Film", "SGFs Geofysikfilm", "5e7yiEa5CK4"));

        //Kap 7
        m_Videos.add(new VideoData("Instruktionsvideo", "Spetstrycksondering", "NbhLCYSIojk"));
        m_Videos.add(new VideoData("Instruktionsvideo", "Jord-Bergsondering", "tbrlP-vvgT4"));
        m_Videos.add(new VideoData("Instruktionsvideo", "Hejarsondering", "oqUgIU6Qn9c"));
        m_Videos.add(new VideoData("Instruktionsvideo", "Viktsondering", "6_CEWBkyiD8"));
        m_Videos.add(new VideoData("Instruktionsvideo", "Trycksondering", "eqso_AoABnM"));

        //Kap 8
        m_Videos.add(new VideoData("Instruktionsvideo", "Kolvprovtagning (I)", "3bUoIlOarsw"));
        m_Videos.add(new VideoData("Instruktionsvideo", "Kolvprovtagning (II)", "Xa-gp1XGfDw"));
        m_Videos.add(new VideoData("Instruktionsvideo", "Skruvprovtagning", "yonwBxVlLDk"));

        //Kap 9
        m_Videos.add(new VideoData("Instruktionsvideo", "Vingförsök", "aoTHVICJHTQ"));

        if (!LoadDocumentsAsByteArray(getApplication().getAssets()))
            return;

        if (!LoadPDDocuments())
            return;

        if (!LoadDocumentTextPages(getApplication().getAssets()))
            return;

        if (!LoadDocumentContentDescriptions())
            return;
    }

    @Override
    protected void onCleared()
    {
        ClosePDDocuments();
    }

    private boolean LoadDocumentsAsByteArray(final AssetManager assetManager)
    {
        {
            InputStream inputStream;
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
        InputStream inputStream;
        byte[] buffer;

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
            JSONObject jsonObject = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
            final JSONArray pages = jsonObject.getJSONArray("pages");

            for (int p = 0; p < pages.length(); p++)
            {
                m_DocumentTextPages.add(pages.getString(p).toLowerCase());
            }
        }
        catch (JSONException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while parsing json file...", e);
            return false;
        }

        return true;
    }

    private boolean LoadDocumentContentDescriptions()
    {
        try
        {
            //Load ToC
            final PDDocumentCatalog documentCatalog = m_PDFDocument.getDocumentCatalog();
            final PDDocumentOutline tableOfContents = documentCatalog.getDocumentOutline();
            PDOutlineItem currentHeading = tableOfContents.getFirstChild();
            while (currentHeading != null)
            {
                //Create new Subheadings Entry
                ArrayList<ContentSubHeading> subHeadings = new ArrayList<>();
                AddHeadingData(currentHeading, m_PDFDocument, documentCatalog);

                //Loop through Chapter Subheadings
                PDOutlineItem currentSubHeading = currentHeading.getFirstChild();
                while (currentSubHeading != null)
                {
                    AddHeadingData(currentSubHeading, m_PDFDocument, documentCatalog);

                    //Create new Chapter Subheadings
                    String subHeadingTitle = currentSubHeading.getTitle();
                    subHeadings.add(new ContentSubHeading(subHeadingTitle, ContentSubHeading.Type.SUBHEADING, null));

                    if (subHeadingTitle.equals("3.2 Geofysiska metoder"))
                    {
                        VideoData videoData = m_Videos.get(0);
                        subHeadings.add(new ContentSubHeading(videoData.Prefix + ": " + videoData.VideoName, ContentSubHeading.Type.VIDEO, videoData.URI));
                    }
                    else if (subHeadingTitle.equals("7.2 Spetstrycksondering, CPT och CPTU"))
                    {
                        VideoData videoData = m_Videos.get(1);
                        subHeadings.add(new ContentSubHeading(videoData.Prefix + ": " + videoData.VideoName, ContentSubHeading.Type.VIDEO, videoData.URI));
                    }
                    else if (subHeadingTitle.equals("7.3 Jord-Bergsondering"))
                    {
                        VideoData videoData = m_Videos.get(2);
                        subHeadings.add(new ContentSubHeading(videoData.Prefix + ": " + videoData.VideoName, ContentSubHeading.Type.VIDEO, videoData.URI));
                    }
                    else if (subHeadingTitle.equals("7.4 Hejarsondering"))
                    {
                        VideoData videoData = m_Videos.get(3);
                        subHeadings.add(new ContentSubHeading(videoData.Prefix + ": " + videoData.VideoName, ContentSubHeading.Type.VIDEO, videoData.URI));
                    }
                    else if (subHeadingTitle.equals("7.5 Viktsondering"))
                    {
                        VideoData videoData = m_Videos.get(4);
                        subHeadings.add(new ContentSubHeading(videoData.Prefix + ": " + videoData.VideoName, ContentSubHeading.Type.VIDEO, videoData.URI));
                    }
                    else if (subHeadingTitle.equals("7.6 Mekanisk trycksondering"))
                    {
                        VideoData videoData = m_Videos.get(5);
                        subHeadings.add(new ContentSubHeading(videoData.Prefix + ": " + videoData.VideoName, ContentSubHeading.Type.VIDEO, videoData.URI));
                    }
                    else if (subHeadingTitle.equals("8.3 Ostörd provtagning"))
                    {
                        //St I och St II
                        VideoData videoData0 = m_Videos.get(6);
                        subHeadings.add(new ContentSubHeading(videoData0.Prefix + ": " + videoData0.VideoName, ContentSubHeading.Type.VIDEO, videoData0.URI));
                        VideoData videoData1 = m_Videos.get(7);
                        subHeadings.add(new ContentSubHeading(videoData1.Prefix + ": " + videoData1.VideoName, ContentSubHeading.Type.VIDEO, videoData1.URI));
                    }
                    else if (subHeadingTitle.equals("8.4 Störd provtagning"))
                    {
                        VideoData videoData = m_Videos.get(8);
                        subHeadings.add(new ContentSubHeading(videoData.Prefix + ": " + videoData.VideoName, ContentSubHeading.Type.VIDEO, videoData.URI));
                    }
                    else if (subHeadingTitle.equals("9.6 Fältvingförsök"))
                    {
                        VideoData videoData = m_Videos.get(9);
                        subHeadings.add(new ContentSubHeading(videoData.Prefix + ": " + videoData.VideoName, ContentSubHeading.Type.VIDEO, videoData.URI));
                    }

                        currentSubHeading = currentSubHeading.getNextSibling();
                }

                String headingTitle = currentHeading.getTitle();

                m_TableOfContents.add(new ContentHeading(headingTitle, subHeadings, ContentHeading.Type.HEADING));
                currentHeading = currentHeading.getNextSibling();
            }

            m_TableOfContents.add(new ContentHeading("Om appen", new ArrayList<ContentSubHeading>(), ContentHeading.Type.ABOUT));
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

    public ArrayList<ContentHeading> GetTableOfContents()
    {
        return m_TableOfContents;
    }

    public String GetURIIfVideo(String heading)
    {
        int videoNameStart = heading.indexOf(": ");
        String videoName = heading.substring(videoNameStart + 1);

        for (VideoData videodata : m_Videos)
        {
            //Replace whitespace characters with nothing to match returned value by ContentFragment
            String currentVideoName = videodata.VideoName;
            currentVideoName = currentVideoName.replaceAll("\\s+", "");

            if (videoName.equals(currentVideoName))
                return videodata.URI;
        }

        return "";
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

        m_PageHighlights.get(pageIndex).observe(owner, observer);

        if (m_PageHighlightsWorkspace == null)
        {
            m_PageHighlightsWorkspace = new ArrayList<>(m_PDFDocument.getNumberOfPages());

            for (int p = 0; p < numberOfPages; p++)
            {
                m_PageHighlightsWorkspace.add(new ArrayList<RectF>());
            }
        }

        m_PageHighlights.get(pageIndex).postValue(new Pair<Integer, ArrayList<RectF>>(pageIndex, m_PageHighlightsWorkspace.get(pageIndex)));

        if (m_PageHighlightLoaderService == null)
        {
            m_PageHighlightLoaderService = Executors.newSingleThreadExecutor();
        }
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
                            protected void writeString(final String text, final List<TextPosition> textPositions)
                            {
                                float posXInit;
                                float posXEnd;
                                float posYInit;
                                float posYEnd;
                                float width;
                                float height;
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
                    currentPageHighlightsLiveData.postValue(new Pair<Integer, ArrayList<RectF>>(pageIndex, currentPageHighlights));
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
                                protected void writeString(final String text, final List<TextPosition> textPositions)
                                {
                                    float posXInit;
                                    float posXEnd;
                                    float posYInit;
                                    float posYEnd;
                                    float width;
                                    float height;
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
                        currentPageHighlightsLiveData.postValue(new Pair<Integer, ArrayList<RectF>>(pageIndex, currentPageHighlights));
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
                                protected void writeString(final String text, final List<TextPosition> textPositions)
                                {
                                    float posXInit;
                                    float posXEnd;
                                    float posYInit;
                                    float posYEnd;
                                    float width;
                                    float height;
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
                        currentPageHighlightsLiveData.postValue(new Pair<Integer, ArrayList<RectF>>(pageIndex, currentPageHighlights));
                    }
                }
            }
        });
    }
}
