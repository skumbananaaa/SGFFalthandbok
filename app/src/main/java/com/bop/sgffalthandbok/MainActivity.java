package com.bop.sgffalthandbok;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.DragEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragmentX;

import java.util.Stack;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener, ContentFragment.OnHeadingSelectedListener, ContentFragment.OnVideoSelectedListener, CustomLinkHandler.OnVideoLinkSelectedListener, SearchFragment.OnSearchResultSelectedListener, YouTubePlayer.OnInitializedListener, OnToggleNavbarListener
{
    enum FragmentType
    {
        NONE,
        CONTENT,
        VIDEO,
        DOCUMENT,
        SEARCH
    };

    private static String YOUTUBE_API_KEY             = "QUl6YVN5QW1KelFBNHdIWXVpT05rTEtGLVc4OVpaUkRSRjlucWxZ";

    private FragmentManager         m_FragmentManager;
    private YouTubePlayer           m_YouTubePlayer;

    private boolean                 m_BottomNavigationVisible;
    private BottomNavigationView    m_BottomNavigationView;
    private ConstraintLayout        m_MainConstraintLayout;

    private FragmentType            m_NextFragmentType;
    private FragmentType            m_CurrentFragmentType;
    private boolean                 m_NextNavEventIsBack;
    private boolean                 m_VideoWasPlaying;

    private ContentFragment         m_ContentFragment;
    private YouTubePlayerFragmentX  m_VideoFragment;
    private DocumentFragment        m_DocumentFragment;
    private SearchFragment          m_SearchFragment;

    private ResourceManager         m_ResourceManager;

    private OnBackPressedCallback   m_BackButtonCallback;
    private Stack<SerializablePair<Integer, FragmentType>>     m_FragmentStack;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_ResourceManager = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(ResourceManager.class);

        m_FragmentManager   = getSupportFragmentManager();

        m_BottomNavigationView = findViewById(R.id.bottomNavigation);
        m_BottomNavigationView.setOnNavigationItemSelectedListener(this);

        m_MainConstraintLayout = findViewById(R.id.mainLayout);

        //Setup Back Button Functionality
        {
            m_BackButtonCallback = new OnBackPressedCallback(true)
            {
                @Override
                public void handleOnBackPressed()
                {
                    if (m_FragmentStack.size() > 0)
                    {
                        int itemId = m_FragmentStack.peek().first;
                        m_NextNavEventIsBack = true;
                        m_BottomNavigationView.setSelectedItemId(itemId);
                    }
                }
            };

            getOnBackPressedDispatcher().addCallback(this, m_BackButtonCallback);

            m_FragmentStack = new Stack<>();
        }

        if (savedInstanceState != null)
        {
            m_BottomNavigationVisible = savedInstanceState.getBoolean("m_BottomNavigationVisible");

            m_NextFragmentType      = (FragmentType)savedInstanceState.getSerializable("m_CurrentFragmentType");
            m_VideoWasPlaying       = savedInstanceState.getBoolean("m_VideoWasPlaying");

            m_ContentFragment   = (ContentFragment) m_FragmentManager.findFragmentByTag("Content");
            m_VideoFragment     = (YouTubePlayerFragmentX) m_FragmentManager.findFragmentByTag("Video");
            m_DocumentFragment  = (DocumentFragment) m_FragmentManager.findFragmentByTag("Document");
            m_SearchFragment    = (SearchFragment) m_FragmentManager.findFragmentByTag("Search");

            m_VideoFragment.initialize(new String(Base64.decode(YOUTUBE_API_KEY, Base64.DEFAULT)), this);

            m_BottomNavigationView.setSelectedItemId(savedInstanceState.getInt("SELECTED_NAV_ITEM"));
        }
        else
        {
            m_BottomNavigationVisible = true;

            m_NextNavEventIsBack = false;
            m_VideoWasPlaying = false;

            m_ContentFragment   = new ContentFragment();
            m_VideoFragment     = new YouTubePlayerFragmentX();
            m_DocumentFragment  = new DocumentFragment();
            m_SearchFragment    = new SearchFragment();

            m_VideoFragment.initialize(new String(Base64.decode(YOUTUBE_API_KEY, Base64.DEFAULT)), this);

            FragmentTransaction fragmentTransaction = m_FragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.frameLayoutFragment, m_ContentFragment, "Content");
            fragmentTransaction.add(R.id.frameLayoutFragment, m_VideoFragment, "Video");
            fragmentTransaction.add(R.id.frameLayoutFragment, m_DocumentFragment, "Document");
            fragmentTransaction.add(R.id.frameLayoutFragment, m_SearchFragment, "Search");
            fragmentTransaction.commit();

            m_NextFragmentType = FragmentType.DOCUMENT;
            m_BottomNavigationView.setSelectedItemId(R.id.documentNav);
        }

        m_DocumentFragment.SetToggleNavbarListener(this);
    }


    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putBoolean("m_BottomNavigationVisible", m_BottomNavigationVisible);

        if (m_YouTubePlayer != null)
        {
            outState.putBoolean("m_VideoWasPlaying", m_YouTubePlayer.isPlaying());
        }
        else
        {
            outState.putBoolean("m_VideoWasPlaying", false);
        }

        outState.putSerializable("m_CurrentFragmentType", m_CurrentFragmentType);
        outState.putInt("SELECTED_NAV_ITEM",  m_BottomNavigationView.getSelectedItemId());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item)
    {
        FragmentType nextFragmentType = null;

        if (m_NextNavEventIsBack)
        {
            m_NextNavEventIsBack = false;

            final SerializablePair<Integer, FragmentType> prevFragment = m_FragmentStack.pop();
            nextFragmentType    = prevFragment.second;
        }
        else
        {
            //m_NextFragmentType is only set manually, not by the navmenu
            if (m_NextFragmentType != FragmentType.NONE)
            {
                nextFragmentType    = m_NextFragmentType;
            }
            else if (m_BottomNavigationVisible)
            {
                switch (item.getItemId())
                {
                    case R.id.contentNav:   nextFragmentType = FragmentType.CONTENT;    break;
                    case R.id.documentNav:  nextFragmentType = FragmentType.DOCUMENT;   break;
                    case R.id.searchNav:    nextFragmentType = FragmentType.SEARCH;     break;
                    default:
                        return false;
                }
            }
            else
            {
                ToggleNavbar();
                m_NextFragmentType      = FragmentType.NONE;
                return false;
            }

            if (nextFragmentType == m_CurrentFragmentType)
            {
                m_NextFragmentType      = FragmentType.NONE;

                if (m_CurrentFragmentType == FragmentType.DOCUMENT)
                    m_DocumentFragment.Jump(0);

                return false;
            }

            if (m_CurrentFragmentType != null)
            {
                m_FragmentStack.push(new SerializablePair<Integer, FragmentType>(m_BottomNavigationView.getSelectedItemId(), m_CurrentFragmentType));
            }
        }

        if (m_CurrentFragmentType == FragmentType.VIDEO)
        {
            m_YouTubePlayer.pause();
        }

        m_NextFragmentType      = FragmentType.NONE;
        m_CurrentFragmentType   = nextFragmentType;

        switch (nextFragmentType)
        {
            case CONTENT:
            {
                FragmentTransaction fragmentTransaction = m_FragmentManager.beginTransaction();
                fragmentTransaction.show(m_ContentFragment);
                fragmentTransaction.hide(m_VideoFragment);
                fragmentTransaction.hide(m_DocumentFragment);
                fragmentTransaction.hide(m_SearchFragment);
                fragmentTransaction.commit();

                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(m_MainConstraintLayout);
                constraintSet.connect(R.id.frameLayoutFragment, ConstraintSet.BOTTOM, R.id.bottomNavigation, ConstraintSet.TOP);
                constraintSet.applyTo(m_MainConstraintLayout);

                if (!m_BottomNavigationVisible)
                {
                    ToggleNavbar();
                }

                return true;
            }
            case VIDEO:
            {
                FragmentTransaction fragmentTransaction = m_FragmentManager.beginTransaction();
                fragmentTransaction.hide(m_ContentFragment);
                fragmentTransaction.show(m_VideoFragment);
                fragmentTransaction.hide(m_DocumentFragment);
                fragmentTransaction.hide(m_SearchFragment);
                fragmentTransaction.commit();

                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(m_MainConstraintLayout);
                constraintSet.connect(R.id.frameLayoutFragment, ConstraintSet.BOTTOM, R.id.bottomNavigation, ConstraintSet.TOP);
                constraintSet.applyTo(m_MainConstraintLayout);

                if (!m_BottomNavigationVisible)
                {
                    ToggleNavbar();
                }

                return true;
            }
            case DOCUMENT:
            {
                FragmentTransaction fragmentTransaction = m_FragmentManager.beginTransaction();
                fragmentTransaction.hide(m_ContentFragment);
                fragmentTransaction.hide(m_VideoFragment);
                fragmentTransaction.show(m_DocumentFragment);
                fragmentTransaction.hide(m_SearchFragment);
                fragmentTransaction.commit();

                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(m_MainConstraintLayout);
                constraintSet.connect(R.id.frameLayoutFragment, ConstraintSet.BOTTOM, R.id.mainLayout, ConstraintSet.BOTTOM);
                constraintSet.applyTo(m_MainConstraintLayout);

                return true;
            }
            case SEARCH:
            {
                FragmentTransaction fragmentTransaction = m_FragmentManager.beginTransaction();
                fragmentTransaction.hide(m_ContentFragment);
                fragmentTransaction.hide(m_VideoFragment);
                fragmentTransaction.hide(m_DocumentFragment);
                fragmentTransaction.show(m_SearchFragment);
                fragmentTransaction.commit();

                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(m_MainConstraintLayout);
                constraintSet.connect(R.id.frameLayoutFragment, ConstraintSet.BOTTOM, R.id.bottomNavigation, ConstraintSet.TOP);
                constraintSet.applyTo(m_MainConstraintLayout);

                if (!m_BottomNavigationVisible)
                {
                    ToggleNavbar();
                }

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
    public void OnVideoLinkSelected(final String videoName)
    {
        String videoURI = m_ResourceManager.GetURIIfVideo(videoName);

        if (videoURI.length() > 0)
        {
            m_NextFragmentType = FragmentType.VIDEO;
            m_BottomNavigationView.setSelectedItemId(R.id.documentNav);

            m_YouTubePlayer.loadVideo(videoURI);
        }
    }

    @Override
    public void OnVideoSelected(final String uri)
    {
        m_NextFragmentType = FragmentType.VIDEO;
        m_BottomNavigationView.setSelectedItemId(R.id.documentNav);

        m_YouTubePlayer.loadVideo(uri);
    }

    @Override
    public void OnHeadingSelected(final String heading)
    {
        try
        {
            m_DocumentFragment.JumpFromTOC(heading);
            m_NextFragmentType = FragmentType.DOCUMENT;
            m_BottomNavigationView.setSelectedItemId(R.id.documentNav);
        }
        catch (NullPointerException e)
        {
            Log.e("SGF Fälthandbok", "Exception thrown while jumping to heading " + heading + "...", e);
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
            m_NextFragmentType = FragmentType.DOCUMENT;
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

    @Override
    public void ToggleNavbar()
    {
        if (m_BottomNavigationVisible)
        {
            m_BottomNavigationVisible = false;

            TranslateAnimation animation = new TranslateAnimation(0.0f, 0.0f, 0.0f, m_BottomNavigationView.getHeight());
            animation.setDuration(500);
            animation.setFillAfter(true);
            animation.setAnimationListener(new Animation.AnimationListener()
            {
                @Override
                public void onAnimationStart(final Animation animation)
                {

                }

                @Override
                public void onAnimationEnd(final Animation animation)
                {
                    m_BottomNavigationView.setElevation(-1.0f);
                }

                @Override
                public void onAnimationRepeat(final Animation animation)
                {

                }
            });

            m_BottomNavigationView.startAnimation(animation);
        }
        else
        {
            m_BottomNavigationVisible = true;

            TranslateAnimation animation = new TranslateAnimation(0.0f, 0.0f, m_BottomNavigationView.getHeight(), 0.0f);
            animation.setDuration(500);
            animation.setFillAfter(true);

            m_BottomNavigationView.startAnimation(animation);
            m_BottomNavigationView.setElevation(1.0f);
        }
    }
}
