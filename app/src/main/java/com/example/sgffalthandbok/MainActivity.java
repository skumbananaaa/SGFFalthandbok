package com.example.sgffalthandbok;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener, ContentFragment.OnHeadingSelectedListener
{
    private static String   s_DocumentFilename = "Geobok 181026 Bok.pdf";

    private FragmentManager m_FragmentManager;

    private BottomNavigationView    m_BottomNavigationView;

    private ContentFragment     m_ContentFragment;
    private DocumentFragment    m_DocumentFragment;
    private Fragment        m_SearchFragment;

    private byte[]          m_DocumentByteArr;

    private ArrayList<String>                           m_DocumentTextPages;
    private HashMap<CharSequence, Integer>              m_HeadingsToPageNumber;
    private ArrayList<Pair<String, ArrayList<String>>>  m_TableOfContents;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_FragmentManager   = getSupportFragmentManager();

        m_BottomNavigationView = findViewById(R.id.bottomNavigation);
        m_BottomNavigationView.setOnNavigationItemSelectedListener(this);

        m_DocumentTextPages = new ArrayList<String>(200);
        m_HeadingsToPageNumber = new HashMap<CharSequence, Integer>();

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
        m_SearchFragment    = SearchFragment.newInstance("Ja", "Fim");

        m_BottomNavigationView.setSelectedItemId(R.id.documentNav);

//        String searchString = "Skärborrkrona".toLowerCase();
//        Locale swedishLocale = new Locale("sv", "SE");
//
//        ArrayList<Pair<Integer, Integer>> matchedCharactedIndices = new ArrayList<Pair<Integer, Integer>>();
//
//        for (int p = 0; p < m_DocumentTextPages.size(); p++)
//        {
//            String page = m_DocumentTextPages.get(p);
//
//            if (page.length() > 0)
//            {
//                StringCharacterIterator characterIterator = new StringCharacterIterator(page);
//                StringSearch stringSearch = new StringSearch(searchString, characterIterator, swedishLocale);
//
//                int foundIndex = stringSearch.next();
//
//                while (foundIndex != StringSearch.DONE)
//                {
//                    matchedCharactedIndices.add(new Pair(p, foundIndex));
//                    foundIndex = stringSearch.next();
//                }
//            }
//        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.contentNav:
            {
                FragmentTransaction fragmentTransaction = m_FragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.frameLayoutFragment, m_ContentFragment);
                fragmentTransaction.commit();
                return true;
            }
            case R.id.documentNav:
            {
                FragmentTransaction fragmentTransaction = m_FragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.frameLayoutFragment, m_DocumentFragment);
                fragmentTransaction.commit();
                return true;
            }
            case R.id.searchNav:
            {
                FragmentTransaction fragmentTransaction = m_FragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.frameLayoutFragment, m_SearchFragment);
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
            PDPage currentPage = null;
            while (currentHeading != null)
            {
                //Create new Subheadings Entry
                ArrayList<String> subHeadings = new ArrayList<>();

                String headingTitle = currentHeading.getTitle();

                //Create new Heading Hashmap Entry
                currentPage = currentHeading.findDestinationPage(pdfDocument);
                m_HeadingsToPageNumber.put(headingTitle.replaceAll("\\s+", ""), documentCatalog.getPages().indexOf(currentPage));

                //Loop through Chapter Subheadings
                PDOutlineItem currentSubHeading = currentHeading.getFirstChild();
                while (currentSubHeading != null)
                {
                    //Create new Subheading Hashmap Entry
                    currentPage = currentSubHeading.findDestinationPage(pdfDocument);
                    String subHeadingTitle = currentSubHeading.getTitle();
                    m_HeadingsToPageNumber.put(subHeadingTitle.replaceAll("\\s+", ""), documentCatalog.getPages().indexOf(currentPage));

                    //Create new Chapter Subheadings
                    subHeadings.add(subHeadingTitle);

                    currentSubHeading = currentSubHeading.getNextSibling();
                }

                m_TableOfContents.add(new Pair<>(headingTitle, subHeadings));
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
}
