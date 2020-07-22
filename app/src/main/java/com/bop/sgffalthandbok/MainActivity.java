package com.bop.sgffalthandbok;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener, ContentFragment.OnHeadingSelectedListener, SearchFragment.OnSearchResultSelectedListener, YouTubePlayer.OnInitializedListener
{
    private static String   YOUTUBE_API_KEY             = "AIzaSyAmJzQA4wHYuiONkLKF-W89ZZRDRF9nqlY";

    private FragmentManager         m_FragmentManager;
    private BottomNavigationView    m_BottomNavigationView;
    private YouTubePlayer           m_YouTubePlayer;

    private boolean                 m_DocumentIsCurrent;
    private boolean                 m_VideoWasPlaying;

    private ContentFragment         m_ContentFragment;
    private YouTubePlayerFragmentX  m_VideoFragment;
    private DocumentFragment        m_DocumentFragment;
    private SearchFragment          m_SearchFragment;

    private ResourceManager         m_ResourceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_ResourceManager = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(ResourceManager.class);

        m_FragmentManager   = getSupportFragmentManager();

        m_BottomNavigationView = findViewById(R.id.bottomNavigation);
        m_BottomNavigationView.setOnNavigationItemSelectedListener(this);

        if (savedInstanceState != null)
        {
            m_DocumentIsCurrent = savedInstanceState.getBoolean("m_DocumentIsCurrent");
            m_VideoWasPlaying = savedInstanceState.getBoolean("m_VideoWasPlaying");

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
            m_VideoWasPlaying = false;

            m_ContentFragment   = new ContentFragment();
            m_VideoFragment     = new YouTubePlayerFragmentX();
            m_DocumentFragment  = new DocumentFragment();
            m_SearchFragment    = new SearchFragment();

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
    protected void onSaveInstanceState(@NonNull final Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putBoolean("m_VideoWasPlaying", m_YouTubePlayer.isPlaying());
        outState.putBoolean("m_DocumentIsCurrent", m_DocumentIsCurrent);
        outState.putInt("SELECTED_NAV_ITEM",  m_BottomNavigationView.getSelectedItemId());
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

    @Override
    public void OnHeadingSelected(final String heading)
    {
        String videoURI = m_ResourceManager.GetURIIfVideo(heading);

        if (videoURI.length() > 0)
        {
            m_DocumentIsCurrent = false;
            m_BottomNavigationView.setSelectedItemId(R.id.documentNav);

            m_YouTubePlayer.loadVideo(videoURI);
        }
        else
        {
            try
            {
                m_DocumentFragment.JumpFromTOC(heading);
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

            m_DocumentFragment.JumpFromSearch(searchResult.GetPageNumber(), searchString);
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

        if (wasRestored)
        {
            if (m_VideoWasPlaying)
            {
                youTubePlayer.play();
                m_VideoWasPlaying = false;
            }
        }
    }

    @Override
    public void onInitializationFailure(final YouTubePlayer.Provider provider, final YouTubeInitializationResult youTubeInitializationResult)
    {

    }
}
