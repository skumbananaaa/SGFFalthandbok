package com.bop.sgffalthandbok;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class VideoFragment extends Fragment implements YouTubePlayer.OnInitializedListener
{
    private static String YOUYUBE_API_KEY = "AIzaSyAmJzQA4wHYuiONkLKF-W89ZZRDRF9nqlY";

    private YouTubePlayerView   m_VideoView;
    private YouTubePlayer       m_YouTubePlayer;

    public static VideoFragment newInstance()
    {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        m_VideoView = view.findViewById(R.id.videoView);
        m_VideoView.initialize(YOUYUBE_API_KEY, this);
    }

    @Override
    public void onInitializationSuccess(final YouTubePlayer.Provider provider, final YouTubePlayer youTubePlayer, final boolean b)
    {
        m_YouTubePlayer = youTubePlayer;

        ArrayList<String> videos = new ArrayList<String>();
        videos.add("yonwBxVlLDk");

        m_YouTubePlayer.loadVideo("yonwBxVlLDk");
        m_YouTubePlayer.play();
    }

    @Override
    public void onInitializationFailure(final YouTubePlayer.Provider provider, final YouTubeInitializationResult youTubeInitializationResult)
    {
        int dawdw = 1231;
    }
}
