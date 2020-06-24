package com.example.sgffalthandbok;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDDocumentCatalog;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener, ContentFragment.OnHeadingSelectedListener, SearchFragment.OnSearchResultSelectedListener
{
    private static String   s_DocumentFilename = "Geobok 181026 Bok.pdf";

    private FragmentManager m_FragmentManager;

    private BottomNavigationView    m_BottomNavigationView;

    private ContentFragment     m_ContentFragment;
    private DocumentFragment    m_DocumentFragment;
    private SearchFragment      m_SearchFragment;

    private byte[]          m_DocumentByteArr;

    private ArrayList<String>                                   m_DocumentTextPages;                //Contains all Pages in Text Format
    private HashMap<String, Integer>                            m_HeadingsToPageNumber;             //Contains Key-Value Pairs of a Heading (with Separators removed) to a Page Number
    private HashMap<Integer, ArrayList<Pair<Integer, String>>>  m_PageNumberToHeadings;             //Contains Key-Value Pairs of a Page Number which maps to a Sorted Array of CharIndex-Heading Pairs
    private ArrayList<Pair<String, ArrayList<String>>>          m_TableOfContents;                  //Contains String-Array Pairs where the String is a Main Heading and the Array contains Subheadings

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_FragmentManager   = getSupportFragmentManager();

        m_BottomNavigationView = findViewById(R.id.bottomNavigation);
        m_BottomNavigationView.setOnNavigationItemSelectedListener(this);

        m_DocumentTextPages = new ArrayList<>(200);
        m_HeadingsToPageNumber = new HashMap<>();
        m_PageNumberToHeadings = new HashMap<>();

        if (!LoadDocumentAsByteArray())
        {
            Log.e("SGF Fälthandbok", "LoadDocumentAsByteArray Failed");
        }

        if (!LoadDocumentSearchablePages())
        {
            Log.e("SGF Fälthandbok", "LoadDocumentSearchablePages Failed");
        }

        m_ContentFragment   = ContentFragment.newInstance(m_TableOfContents);
        m_DocumentFragment  = DocumentFragment.newInstance(m_DocumentByteArr);
        m_SearchFragment    = SearchFragment.newInstance(m_DocumentTextPages, m_PageNumberToHeadings);

        FragmentTransaction fragmentTransaction = m_FragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.frameLayoutFragment, m_ContentFragment, "Content");
        fragmentTransaction.add(R.id.frameLayoutFragment, m_DocumentFragment, "Document");
        fragmentTransaction.add(R.id.frameLayoutFragment, m_SearchFragment, "Search");
        fragmentTransaction.commit();

        m_BottomNavigationView.setSelectedItemId(R.id.documentNav);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.contentNav:
            {
                FragmentTransaction fragmentTransaction = m_FragmentManager.beginTransaction();
                fragmentTransaction.show(m_ContentFragment);
                fragmentTransaction.hide(m_DocumentFragment);
                fragmentTransaction.hide(m_SearchFragment);
                fragmentTransaction.commit();
                return true;
            }
            case R.id.documentNav:
            {
                FragmentTransaction fragmentTransaction = m_FragmentManager.beginTransaction();
                fragmentTransaction.hide(m_ContentFragment);
                fragmentTransaction.show(m_DocumentFragment);
                fragmentTransaction.hide(m_SearchFragment);
                fragmentTransaction.commit();
                return true;
            }
            case R.id.searchNav:
            {
                FragmentTransaction fragmentTransaction = m_FragmentManager.beginTransaction();
                fragmentTransaction.hide(m_ContentFragment);
                fragmentTransaction.hide(m_DocumentFragment);
                fragmentTransaction.show(m_SearchFragment);
                fragmentTransaction.commit();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPointerCaptureChanged(final boolean hasCapture)
    {

    }

    boolean LoadDocumentAsByteArray()
    {
        InputStream inputStream = null;
        try
        {
            inputStream = getAssets().open(s_DocumentFilename);
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

        m_DocumentByteArr = output.toByteArray();
        return true;
    }

    boolean LoadDocumentSearchablePages()
    {
        PDFBoxResourceLoader.init(getApplicationContext());

        String parsedText = null;
        PDDocument pdfDocument = null;
        try
        {
            pdfDocument = PDDocument.load(m_DocumentByteArr);
        }
        catch(IOException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while loading document to strip...", e);
            return false;
        }

        try
        {
            PDFTextStripper pdfStripper = new PDFTextStripper();

            //Load Text
            for (int p = 0; p < pdfDocument.getNumberOfPages(); p++)
            {
                pdfStripper.setStartPage(p);
                pdfStripper.setEndPage(p);
                m_DocumentTextPages.add(pdfStripper.getText(pdfDocument).toLowerCase());
            }

            //Load ToC
            m_TableOfContents = new ArrayList<>(15);
            final PDDocumentCatalog documentCatalog = pdfDocument.getDocumentCatalog();
            final PDDocumentOutline tableOfContents = documentCatalog.getDocumentOutline();
            PDOutlineItem currentHeading = tableOfContents.getFirstChild();
            while (currentHeading != null)
            {
                //Create new Subheadings Entry
                ArrayList<String> subHeadings = new ArrayList<>();
                AddHeadingData(currentHeading, pdfDocument, documentCatalog);

                //Loop through Chapter Subheadings
                PDOutlineItem currentSubHeading = currentHeading.getFirstChild();
                while (currentSubHeading != null)
                {
                    AddHeadingData(currentSubHeading, pdfDocument, documentCatalog);

                    //Create new Chapter Subheadings
                    subHeadings.add(currentSubHeading.getTitle());

                    currentSubHeading = currentSubHeading.getNextSibling();
                }

                m_TableOfContents.add(new Pair<>(currentHeading.getTitle(), subHeadings));
                currentHeading = currentHeading.getNextSibling();
            }


        }
        catch (IOException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while stripping text...", e);
            return false;
        }
        finally
        {
            try
            {
                if (pdfDocument != null) pdfDocument.close();
            }
            catch (IOException e)
            {
                Log.e("SGF Fälthandbok", "Exception thrown while closing document...", e);
                return false;
            }
        }

        return true;
    }

    void AddHeadingData(PDOutlineItem currentHeading, PDDocument pdfDocument, PDDocumentCatalog documentCatalog) throws IOException
    {
        final String headingTitle       = currentHeading.getTitle();
        final PDPage currentPage        = currentHeading.findDestinationPage(pdfDocument);
        final int pageIndex             = documentCatalog.getPages().indexOf(currentPage);
        final String pageText           = m_DocumentTextPages.get(pageIndex);

        //Create new Headings to Page Number Entry
        m_HeadingsToPageNumber.put(headingTitle.replaceAll("\\s+", ""), pageIndex);

        //Create new Page Number to Headings Entry
        ArrayList<Pair<Integer, String>> headingsPerCharNumber;
        if (!m_PageNumberToHeadings.containsKey(pageIndex))
        {
            headingsPerCharNumber = new ArrayList<>();
            m_PageNumberToHeadings.put(pageIndex, headingsPerCharNumber);
        }
        else
        {
            headingsPerCharNumber = m_PageNumberToHeadings.get(pageIndex);
        }

        headingsPerCharNumber.add(new Pair(pageText.indexOf(headingTitle), headingTitle));
    }

    @Override
    public void OnHeadingSelected(final String heading)
    {
        try
        {
            int pageIndex = m_HeadingsToPageNumber.get(heading);
            m_DocumentFragment.JumpToPage(pageIndex);
            m_BottomNavigationView.setSelectedItemId(R.id.documentNav);
        }
        catch (NullPointerException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while jumping to heading " + heading + "...", e);
        }
    }

    @Override
    public void OnSearchResultSelected(final SearchResult searchResult)
    {
        int pageNumber = searchResult.GetPageNumber();

        try
        {
            InputMethodManager imm = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
            //Find the currently focused view, so we can grab the correct window token from it.
            View view = getCurrentFocus();
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null)
            {
                view = new View(this);
            }
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

            m_DocumentFragment.JumpToPage(searchResult.GetPageNumber());
            m_BottomNavigationView.setSelectedItemId(R.id.documentNav);
        }
        catch (NullPointerException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while jumping to page " + pageNumber + "...", e);
        }
    }
}
