package com.example.sgffalthandbok;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.icu.text.RuleBasedCollator;
import android.icu.text.StringSearch;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.google.android.material.navigation.NavigationView;
import com.shockwave.pdfium.PdfDocument;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.IOException;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnLoadCompleteListener
{
    private AssetManager    m_AssetManager;

    private DrawerLayout    m_DrawerLayout;
    private Toolbar         m_Toolbar;
    private NavigationView  m_NavigationView;
    private Menu            m_NavigationMenu;
    private PDFView         m_PDFView;

   HashMap<CharSequence, Integer> m_SubHeadingsToPage;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_AssetManager      = getAssets();

        m_DrawerLayout      = findViewById(R.id.drawerLayout);
        m_Toolbar           = findViewById(R.id.toolbar);
        m_NavigationView    = findViewById(R.id.nav_view);
        m_NavigationMenu    = m_NavigationView.getMenu();
        m_PDFView           = findViewById(R.id.pdfView);
        m_SubHeadingsToPage = new HashMap<CharSequence, Integer>();

        String pdfDocumentName = "Geobok 181026 Bok.pdf";

        ArrayList<String> documentTextPages = new ArrayList<String>(200);

        //Load PDF in PDFBox to allow for text parsing
        {
            PDFBoxResourceLoader.init(getApplicationContext());

            String parsedText = null;
            PDDocument pdfDocument = null;
            try
            {
                pdfDocument = PDDocument.load(m_AssetManager.open(pdfDocumentName));
            }
            catch(IOException e)
            {
                Log.e("SGF Fälthandbok", "Exception thrown while loading document to strip...", e);
            }

            try
            {
                PDFTextStripper pdfStripper = new PDFTextStripper();

                for (int p = 0; p < pdfDocument.getNumberOfPages(); p++)
                {
                    pdfStripper.setStartPage(p);
                    pdfStripper.setEndPage(p);
                    documentTextPages.add(pdfStripper.getText(pdfDocument).toLowerCase());
                }
            }
            catch (IOException e)
            {
                Log.e("SGF Fälthandbok", "Exception thrown while stripping text...", e);
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
                }
            }
        }

        String searchString = "Skärborrkrona".toLowerCase();
        Locale swedishLocale = new Locale("sv", "SE");

        ArrayList<Pair<Integer, Integer>> matchedCharactedIndices = new ArrayList<Pair<Integer, Integer>>();

        for (int p = 0; p < documentTextPages.size(); p++)
        {
            String page = documentTextPages.get(p);

            if (page.length() > 0)
            {
                StringCharacterIterator characterIterator = new StringCharacterIterator(page);
                StringSearch stringSearch = new StringSearch(searchString, characterIterator, swedishLocale);

                int foundIndex = stringSearch.next();

                while (foundIndex != StringSearch.DONE)
                {
                    matchedCharactedIndices.add(new Pair(p, foundIndex));
                    foundIndex = stringSearch.next();
                }
            }
        }

        //Start PDF View
        {
            m_PDFView.fromAsset(pdfDocumentName)
                    //.pages(0, 2, 1, 3, 3, 3) // all pages are displayed by default
                    .enableSwipe(true) // allows to block changing pages using swipe
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    //.onDraw(onDrawListener) // allows to draw something on the current page, usually visible in the middle of the screen
                    //.onDrawAll(onDrawListener) // allows to draw something on all pages, separately for every page. Called only for visible pages
                    .onLoad(this) // called after document is loaded and starts to be rendered
                    //.onPageChange(onPageChangeListener)
                    //.onPageScroll(onPageScrollListener)
                    //.onError(onErrorListener)
                    //.onPageError(onPageErrorListener)
                    //.onRender(onRenderListener) // called after document is rendered for the first time
                    //.onTap(onTapListener) // called on single tap, return true if handled, false to toggle scroll handle visibility
                    //.onLongPress(onLongPressListener)
                    .enableAnnotationRendering(false) // render annotations (such as comments, colors or forms)
                    .enableAntialiasing(true) // improve rendering a little bit on low-res screens
                    .spacing(0)// spacing between pages in dp. To define spacing color, set view backgrounds
                    .autoSpacing(false) // add dynamic spacing to fit each page on its own on the screen
                    //.linkHandler(DefaultLinkHandler)
                    .pageFitPolicy(FitPolicy.WIDTH) // mode to fit pages in the view
                    .fitEachPage(false) // fit each page to the view, else smaller pages are scaled relative to largest page.
                    .pageSnap(true) // snap pages to screen boundaries
                    .pageFling(true) // make a fling change only a single page like ViewPager
                    .nightMode(false) // toggle night mode
                    .load();
        }

        //Create Navigation Drawer
        {
            setSupportActionBar(m_Toolbar);

            ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle
                    (
                            this,
                            m_DrawerLayout,
                            m_Toolbar,
                            R.string.openNavDrawer,
                            R.string.closeNavDrawer
                    );

            m_DrawerLayout.addDrawerListener(actionBarDrawerToggle);
            actionBarDrawerToggle.syncState();
            m_NavigationView.setNavigationItemSelectedListener(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.top_bar_menu, menu);

        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null)
        {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item)
    {
        final CharSequence title = item.getTitle();

        if (m_SubHeadingsToPage.containsKey(title))
        {
            int pageIndex = m_SubHeadingsToPage.get(title);
            m_PDFView.jumpTo(pageIndex, true);

            m_DrawerLayout.closeDrawer(GravityCompat.START);
        }

        return true;
    }



    @Override
    public void onPointerCaptureChanged(final boolean hasCapture)
    {

    }

    @Override
    public void loadComplete(final int nbPages)
    {
        m_PDFView.setBackgroundColor(Color.DKGRAY);

        final List<PdfDocument.Bookmark> tableOfContents = m_PDFView.getTableOfContents();

        //Loop through Chapter Headings
        for (PdfDocument.Bookmark heading : tableOfContents)
        {
            //Create new Menu Entry
            final SubMenu chapterMenu = m_NavigationMenu.addSubMenu(heading.getTitle());

            //Loop through Chapter Subheadings
            for (PdfDocument.Bookmark subHeading : heading.getChildren())
            {
                //Create new Subheading Hashmap Entry
                m_SubHeadingsToPage.put(subHeading.getTitle(), (int)subHeading.getPageIdx());

                //Create new Chapter Subheadings
                final MenuItem chapterSection = chapterMenu.add(subHeading.getTitle());
            }
        }
    }
}
