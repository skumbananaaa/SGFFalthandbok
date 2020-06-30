package com.bop.sgffalthandbok;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDDocumentCatalog;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
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

public class ResourceManager
{
    private static int      NUMBER_OF_PAGES             = 34;
    private static String   PAGE_FILENAME               = "Geobok 181026 Pages/Geobok 181026 Pages_";
    private static String   DOCUMENT_FILENAME           = "Geobok 181026 Bok.pdf";
    private static String   DOCUMENT_JSON_FILENAME      = "Geobok 181026 Bok.json";

    private static boolean s_Initialized                = false;

    private static ArrayList<byte[]>                                               s_DocumentPagesByteArr;              //The entire PDF as a byte arrays per page
    private static byte[]                                                          s_DocumentByteArr;                   //The entire PDF as a byte array
    private static ArrayList<PDDocument>                                           s_PDFDocumentPerPage;
    private static PDDocument                                                      s_PDFDocument;
    private static ArrayList<String>                                               s_DocumentTextPages;                 //Contains all Pages in Text Format
    private static HashMap<String, Integer>                                        s_HeadingsToPageNumber;              //Contains Key-Value Pairs of a Heading (with Separators removed) to a Page Number
    private static HashMap<Integer, ArrayList<SerializablePair<Integer, String>>>  s_PageNumberToHeadings;              //Contains Key-Value Pairs of a Page Number which maps to a Sorted Array of CharIndex-Heading Pairs
    private static ArrayList<SerializablePair<String, ArrayList<String>>>          s_TableOfContents;                   //Contains String-Array Pairs where the String is a Main Heading and the Array contains Subheadings


    public static boolean Initialize(@Nullable final Bundle savedInstanceState, final Context applicationContext, final AssetManager assetManager)
    {
        if (!s_Initialized)
        {
            s_Initialized = true;

            PDFBoxResourceLoader.init(applicationContext);

            s_DocumentPagesByteArr = new ArrayList<>();
            s_PDFDocumentPerPage = new ArrayList<>();

            if (!LoadDocumentsAsByteArrays(assetManager))
                return false;

            if (!LoadPDDocuments())
                return false;

            s_DocumentTextPages = new ArrayList<>(200);

            if (!LoadDocumentTextPages(assetManager))
            return false;

            if (savedInstanceState == null)
            {
                //First Init
                s_HeadingsToPageNumber = new HashMap<>();
                s_PageNumberToHeadings = new HashMap<>();
                s_TableOfContents = new ArrayList<>(15);

                if (!LoadDocumentContentDescriptions())
                    return false;
            }
            else
            {
                //Restore State

                //These casts are safe, see the implementation of Destroy and SerializablePair
                s_HeadingsToPageNumber      = (HashMap<String, Integer>) savedInstanceState.getSerializable("s_HeadingsToPageNumber");
                s_PageNumberToHeadings      = (HashMap<Integer, ArrayList<SerializablePair<Integer, String>>>) savedInstanceState.getSerializable("s_PageNumberToHeadings");
                s_TableOfContents           = (ArrayList<SerializablePair<String, ArrayList<String>>>) savedInstanceState.getSerializable("s_TableOfContents");
            }
        }

        return true;
    }

    public static void Destroy(@NonNull final Bundle outState)
    {
        if (s_Initialized)
        {
            s_Initialized = false;

            ClosePDDocuments();

            outState.putSerializable("s_HeadingsToPageNumber", s_HeadingsToPageNumber);
            outState.putSerializable("s_PageNumberToHeadings", s_PageNumberToHeadings);
            outState.putSerializable("s_TableOfContents", s_TableOfContents);
        }
    }

    private static boolean LoadDocumentsAsByteArrays(final AssetManager assetManager)
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

            s_DocumentByteArr = output.toByteArray();
        }

        for (int pageIndex = 0; pageIndex < NUMBER_OF_PAGES; pageIndex++)
        {
            InputStream inputStream = null;
            try
            {
                inputStream = assetManager.open(PAGE_FILENAME + String.format("%02d", pageIndex) + ".pdf");
            }
            catch (IOException e)
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
                }
                catch (IOException e)
                {
                    Log.e("SGF Fälthandbok", "Exception thrown while converting document to byte array...", e);
                    return false;
                }
            }

            try
            {
                inputStream.close();
            }
            catch (IOException e)
            {
                Log.e("SGF Fälthandbok", "Exception thrown while closing document input stream...", e);
            }

            s_DocumentPagesByteArr.add(output.toByteArray());
        }

        return true;
    }

    private static boolean LoadPDDocuments()
    {
        try
        {
            s_PDFDocument = PDDocument.load(s_DocumentByteArr);
        }
        catch(IOException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while loading document to strip...", e);
            return false;
        }

        try
        {
            for (int pageIndex = 0; pageIndex < 34; pageIndex++)
            {
                s_PDFDocumentPerPage.add(PDDocument.load(s_DocumentPagesByteArr.get(pageIndex)));
            }
        }
        catch(IOException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while loading document to strip...", e);
            return false;
        }

        return true;
    }

    private static void ClosePDDocuments()
    {
        try
        {
            if (s_PDFDocument != null) s_PDFDocument.close();
        }
        catch (IOException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while closing document...", e);
        }

        try
        {
            for (int pageIndex = 0; pageIndex < s_PDFDocumentPerPage.size(); pageIndex++)
            {
                s_PDFDocumentPerPage.get(pageIndex).close();
            }
        }
        catch(IOException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while loading document to strip...", e);
        }
    }

    private static boolean LoadDocumentTextPages(final AssetManager assetManager)
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
                s_DocumentTextPages.add(pages.getString(p).toLowerCase());
            }
        }
        catch (JSONException | UnsupportedEncodingException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while parsing json file...", e);
            return false;
        }

        return true;
    }

    private static boolean LoadDocumentContentDescriptions()
    {
        String parsedText = null;

        try
        {
            //Load ToC
            final PDDocumentCatalog documentCatalog = s_PDFDocument.getDocumentCatalog();
            final PDDocumentOutline tableOfContents = documentCatalog.getDocumentOutline();
            PDOutlineItem currentHeading = tableOfContents.getFirstChild();
            while (currentHeading != null)
            {
                //Create new Subheadings Entry
                ArrayList<String> subHeadings = new ArrayList<>();
                AddHeadingData(currentHeading, s_PDFDocument, documentCatalog);

                //Loop through Chapter Subheadings
                PDOutlineItem currentSubHeading = currentHeading.getFirstChild();
                while (currentSubHeading != null)
                {
                    AddHeadingData(currentSubHeading, s_PDFDocument, documentCatalog);

                    //Create new Chapter Subheadings
                    subHeadings.add(currentSubHeading.getTitle());

                    currentSubHeading = currentSubHeading.getNextSibling();
                }

                s_TableOfContents.add(new SerializablePair<>(currentHeading.getTitle(), subHeadings));
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

    private static void AddHeadingData(PDOutlineItem currentHeading, PDDocument pdfDocument, PDDocumentCatalog documentCatalog) throws IOException
    {
        final String headingTitle       = currentHeading.getTitle();
        final PDPage currentPage        = currentHeading.findDestinationPage(pdfDocument);
        final int pageIndex             = documentCatalog.getPages().indexOf(currentPage);
        final String pageText           = s_DocumentTextPages.get(pageIndex);

        //Create new Headings to Page Number Entry
        s_HeadingsToPageNumber.put(headingTitle.replaceAll("\\s+", ""), pageIndex);

        //Create new Page Number to Headings Entry
        ArrayList<SerializablePair<Integer, String>> headingsPerCharNumber;
        if (!s_PageNumberToHeadings.containsKey(pageIndex))
        {
            headingsPerCharNumber = new ArrayList<>();
            s_PageNumberToHeadings.put(pageIndex, headingsPerCharNumber);
        }
        else
        {
            headingsPerCharNumber = s_PageNumberToHeadings.get(pageIndex);
        }

        headingsPerCharNumber.add(new SerializablePair<>(pageText.indexOf(headingTitle), headingTitle));
    }

    public static byte[] GetDocumentByteArr()
    {
        return s_DocumentByteArr;
    }

    public static PDDocument GetPDFDocument(int pageIndex)
    {
        return s_PDFDocumentPerPage.get(pageIndex);
    }

    public static PDDocument GetPDFDocument()
    {
        return s_PDFDocument;
    }

    public static ArrayList<String> GetDocumentTextPages()
    {
        return s_DocumentTextPages;
    }

    public static HashMap<String, Integer> GetHeadingsToPageNumber()
    {
        return s_HeadingsToPageNumber;
    }

    public static HashMap<Integer, ArrayList<SerializablePair<Integer, String>>> GetPageNumberToHeadings()
    {
        return s_PageNumberToHeadings;
    }

    public static ArrayList<SerializablePair<String, ArrayList<String>>> GetTableOfContents()
    {
        return s_TableOfContents;
    }
}
