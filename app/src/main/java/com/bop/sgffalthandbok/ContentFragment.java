package com.bop.sgffalthandbok;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;

public class ContentFragment extends Fragment
{
    private ArrayList<ContentHeading>   m_TableOfContents;

    private ExpandableListView          m_ListView;
    private ContentAdapter              m_ContentAdapter;

    private OnHeadingSelectedListener   m_OnHeadingSelectedListener;
    private OnVideoSelectedListener     m_OnVideoSelectedListener;

    public interface OnHeadingSelectedListener
    {
        public void OnHeadingSelected(String heading);
    }

    public interface OnVideoSelectedListener
    {
        public void OnVideoSelected(String uri);
    }

    @Override
    public void onAttach(@NonNull final Context context)
    {
        super.onAttach(context);
        try
        {
            m_OnHeadingSelectedListener = (OnHeadingSelectedListener)context;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(context.toString() + " must implement OnHeadingSelectedListener");
        }

        try
        {
            m_OnVideoSelectedListener = (OnVideoSelectedListener)context;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(context.toString() + " must implement OnVideoSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        ResourceManager resourceManager = new ViewModelProvider(requireActivity()).get(ResourceManager.class);

        m_TableOfContents = resourceManager.GetTableOfContents();

        m_ContentAdapter = new ContentAdapter(getContext(), m_TableOfContents);
        m_ListView = view.findViewById(R.id.contentList);
        m_ListView.setAdapter(m_ContentAdapter);

        m_ListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener()
        {
            @Override
            public boolean onChildClick(final ExpandableListView parent, final View v, final int groupPosition, final int childPosition, final long id)
            {
                final ContentSubHeading clickedChild = m_TableOfContents.get(groupPosition).GetChild(childPosition);

                switch (clickedChild.GetType())
                {
                    case SUBHEADING:
                    {
                        m_OnHeadingSelectedListener.OnHeadingSelected(clickedChild.GetText().replaceAll("\\s+", ""));
                        break;
                    }

                    case VIDEO:
                    {
                        m_OnVideoSelectedListener.OnVideoSelected(clickedChild.GetMetadata());
                        break;
                    }
                }

                return false;
            }
        });

    }
}
