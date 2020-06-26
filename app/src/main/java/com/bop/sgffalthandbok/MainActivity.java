package com.bop.sgffalthandbok;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragmentX;
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

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener, ContentFragment.OnHeadingSelectedListener, SearchFragment.OnSearchResultSelectedListener, YouTubePlayer.OnInitializedListener
{
    private static String   YOUTUBE_API_KEY     = "AIzaSyAmJzQA4wHYuiONkLKF-W89ZZRDRF9nqlY";
    private static String   DOCUMENT_FILENAME   = "Geobok 181026 Bok.pdf";

    private FragmentManager         m_FragmentManager;
    private BottomNavigationView    m_BottomNavigationView;
    private YouTubePlayer           m_YouTubePlayer;

    private boolean                 m_DocumentIsCurrent;

    private ContentFragment         m_ContentFragment;
    private YouTubePlayerFragmentX  m_VideoFragment;
    private DocumentFragment        m_DocumentFragment;
    private SearchFragment          m_SearchFragment;

    private byte[]                  m_DocumentByteArr;

    private ArrayList<String>                                               m_DocumentTextPages;                //Contains all Pages in Text Format
    private HashMap<String, Integer>                                        m_HeadingsToPageNumber;             //Contains Key-Value Pairs of a Heading (with Separators removed) to a Page Number
    private HashMap<Integer, ArrayList<SerialiablePair<Integer, String>>>   m_PageNumberToHeadings;             //Contains Key-Value Pairs of a Page Number which maps to a Sorted Array of CharIndex-Heading Pairs
    private ArrayList<SerialiablePair<String, ArrayList<String>>>           m_TableOfContents;                  //Contains String-Array Pairs where the String is a Main Heading and the Array contains Subheadings

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ThreadPool.Init();

        m_FragmentManager   = getSupportFragmentManager();

        m_BottomNavigationView = findViewById(R.id.bottomNavigation);
        m_BottomNavigationView.setOnNavigationItemSelectedListener(this);

        if (!LoadDocumentAsByteArray())
        {
            Log.e("SGF Fälthandbok", "LoadDocumentAsByteArray Failed");
        }

        if (savedInstanceState != null)
        {
            m_DocumentIsCurrent = savedInstanceState.getBoolean("m_DocumentIsCurrent");

            m_DocumentTextPages         = (ArrayList<String>) savedInstanceState.getSerializable("m_DocumentTextPages");
            m_HeadingsToPageNumber      = (HashMap<String, Integer>) savedInstanceState.getSerializable("m_HeadingsToPageNumber");
            m_PageNumberToHeadings      = (HashMap<Integer, ArrayList<SerialiablePair<Integer, String>>>) savedInstanceState.getSerializable("m_PageNumberToHeadings");
            m_TableOfContents           = (ArrayList<SerialiablePair<String, ArrayList<String>>>) savedInstanceState.getSerializable("m_TableOfContents");

            m_ContentFragment   = (ContentFragment) m_FragmentManager.findFragmentByTag("Content");
            m_VideoFragment     = (YouTubePlayerFragmentX) m_FragmentManager.findFragmentByTag("Video");
            m_DocumentFragment  = (DocumentFragment) m_FragmentManager.findFragmentByTag("Document");
            m_SearchFragment    = (SearchFragment) m_FragmentManager.findFragmentByTag("Search");

            m_VideoFragment.initialize(YOUTUBE_API_KEY, this);

            m_BottomNavigationView.setSelectedItemId(savedInstanceState.getInt("SELECTED_NAV_ITEM"));
        }
        else
        {
            m_DocumentIsCurrent = true;

            m_DocumentTextPages = new ArrayList<>(200);
            m_HeadingsToPageNumber = new HashMap<>();
            m_PageNumberToHeadings = new HashMap<>();

            if (!LoadDocumentSearchablePages())
            {
                Log.e("SGF Fälthandbok", "LoadDocumentSearchablePages Failed");
            }

            m_ContentFragment   = ContentFragment.newInstance(m_TableOfContents);
            m_VideoFragment     = YouTubePlayerFragmentX.newInstance();
            m_DocumentFragment  = DocumentFragment.newInstance(m_DocumentByteArr);
            m_SearchFragment    = SearchFragment.newInstance(m_DocumentTextPages, m_PageNumberToHeadings);

            m_VideoFragment.initialize(YOUTUBE_API_KEY, this);

            FragmentTransaction fragmentTransaction = m_FragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.frameLayoutFragment, m_ContentFragment, "Content");
            fragmentTransaction.add(R.id.frameLayoutFragment, m_VideoFragment, "Video");
            fragmentTransaction.add(R.id.frameLayoutFragment, m_DocumentFragment, "Document");
            fragmentTransaction.add(R.id.frameLayoutFragment, m_SearchFragment, "Search");
            fragmentTransaction.commit();

            m_BottomNavigationView.setSelectedItemId(R.id.documentNav);
        }
    }

    @Override
    protected void onPostResume()
    {
        ThreadPool.Init();
        super.onPostResume();
    }

    @Override
    protected void onPause()
    {
        ThreadPool.Shutdown();
        super.onPause();
    }

    @Override
    protected void onStop()
    {
        ThreadPool.Shutdown();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState)
    {
        outState.putBoolean("m_DocumentIsCurrent", m_DocumentIsCurrent);

        outState.putSerializable("m_DocumentTextPages", m_DocumentTextPages);
        outState.putSerializable("m_HeadingsToPageNumber", m_HeadingsToPageNumber);
        outState.putSerializable("m_PageNumberToHeadings", m_PageNumberToHeadings);
        outState.putSerializable("m_TableOfContents", m_TableOfContents);

        outState.putInt("SELECTED_NAV_ITEM",  m_BottomNavigationView.getSelectedItemId());

        super.onSaveInstanceState(outState);
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
                fragmentTransaction.hide(m_VideoFragment);
                fragmentTransaction.hide(m_DocumentFragment);
                fragmentTransaction.hide(m_SearchFragment);
                fragmentTransaction.commit();
                return true;
            }
            case R.id.documentNav:
            {
                FragmentTransaction fragmentTransaction = m_FragmentManager.beginTransaction();
                fragmentTransaction.hide(m_ContentFragment);
                fragmentTransaction.hide(m_SearchFragment);

                if (m_DocumentIsCurrent)
                {
                    fragmentTransaction.hide(m_VideoFragment);
                    fragmentTransaction.show(m_DocumentFragment);
                }
                else
                {
                    fragmentTransaction.show(m_VideoFragment);
                    fragmentTransaction.hide(m_DocumentFragment);
                }

                fragmentTransaction.commit();
                return true;
            }
            case R.id.searchNav:
            {
                FragmentTransaction fragmentTransaction = m_FragmentManager.beginTransaction();
                fragmentTransaction.hide(m_ContentFragment);
                fragmentTransaction.hide(m_VideoFragment);
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
            inputStream = getAssets().open(DOCUMENT_FILENAME);
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
        final PDDocument pdfDocument;

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
            //Load Text
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

                m_TableOfContents.add(new SerialiablePair<>(currentHeading.getTitle(), subHeadings));
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
        ArrayList<SerialiablePair<Integer, String>> headingsPerCharNumber;
        if (!m_PageNumberToHeadings.containsKey(pageIndex))
        {
            headingsPerCharNumber = new ArrayList<>();
            m_PageNumberToHeadings.put(pageIndex, headingsPerCharNumber);
        }
        else
        {
            headingsPerCharNumber = m_PageNumberToHeadings.get(pageIndex);
        }

        headingsPerCharNumber.add(new SerialiablePair(pageText.indexOf(headingTitle), headingTitle));
    }

    @Override
    public void OnHeadingSelected(final String heading)
    {
        if (heading.equals("TestVideo"))
        {
            m_DocumentIsCurrent = false;
            m_BottomNavigationView.setSelectedItemId(R.id.documentNav);
        }
        else
        {
            try
            {
                int pageIndex = m_HeadingsToPageNumber.get(heading);
                m_DocumentFragment.JumpFromTOC(pageIndex);
                m_DocumentIsCurrent = true;
                m_BottomNavigationView.setSelectedItemId(R.id.documentNav);
            }
            catch (NullPointerException e)
            {
                Log.e("SGF Fälthandbok", "Exception thrown while jumping to heading " + heading + "...", e);
            }
        }
    }

    @Override
    public void OnSearchResultSelected(final String searchString, final SearchResult searchResult)
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

            m_DocumentFragment.JumpFromSearch(searchResult.GetPageNumber() - 1, searchString);
            m_DocumentIsCurrent = true;
            m_BottomNavigationView.setSelectedItemId(R.id.documentNav);
        }
        catch (NullPointerException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while jumping to page " + pageNumber + "...", e);
        }
    }

    @Override
    public void onInitializationSuccess(final YouTubePlayer.Provider provider, final YouTubePlayer youTubePlayer, final boolean wasRestored)
    {
        m_YouTubePlayer = youTubePlayer;
        m_YouTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);

        if (!wasRestored)
        {
            m_YouTubePlayer.loadVideo("yonwBxVlLDk");
        }

        m_YouTubePlayer.play();
    }

    @Override
    public void onInitializationFailure(final YouTubePlayer.Provider provider, final YouTubeInitializationResult youTubeInitializationResult)
    {

    }
}
