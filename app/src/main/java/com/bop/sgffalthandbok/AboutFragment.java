package com.bop.sgffalthandbok;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class AboutFragment extends Fragment
{
    private Context m_Context;

    public AboutFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull final Context context)
    {
        super.onAttach(context);

        m_Context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        final View contactEmail = view.findViewById(R.id.aboutContactEmail);

        if (contactEmail != null)
        {
            contactEmail.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(final View v)
                {
                    ClipboardManager clipboardManager = (ClipboardManager)m_Context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Bucket o' Pixels Email", "bucketopixelscompany@gmail.com");
                    clipboardManager.setPrimaryClip(clip);
                }
            });
        }
    }
}
