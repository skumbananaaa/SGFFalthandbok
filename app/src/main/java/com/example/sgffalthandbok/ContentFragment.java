package com.example.sgffalthandbok;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ContentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContentFragment extends Fragment implements AdapterView.OnItemClickListener
{
    private static final String ARG_TABLE_OF_CONTENTS = "tableOfContents";

    private ArrayList<Pair<String, ArrayList<String>>>  m_TableOfContents;

    private ListView                m_ListView;
    private ArrayAdapter<String>    m_ListAdapter;
    private ArrayList<String>       m_Headings;

    private OnHeadingSelectedListener   m_Listener;

    public interface OnHeadingSelectedListener
    {
        public void OnHeadingSelected(String heading);
    }

    public static ContentFragment newInstance(ArrayList<Pair<String, ArrayList<String>>>  tableOfContents)
    {
        ContentFragment fragment = new ContentFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TABLE_OF_CONTENTS, tableOfContents);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull final Context context)
    {
        super.onAttach(context);
        try
        {
            m_Listener = (OnHeadingSelectedListener)context;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(context.toString() + " must implement OnArticleSelectedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            m_TableOfContents = (ArrayList<Pair<String, ArrayList<String>>>)getArguments().getSerializable(ARG_TABLE_OF_CONTENTS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        m_Headings = new ArrayList<>();

        m_ListView = view.findViewById(R.id.contentList);
        m_ListAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, m_Headings);
        m_ListView.setAdapter(m_ListAdapter);

        for (Pair<String, ArrayList<String>> chapter : m_TableOfContents)
        {
            m_ListAdapter.add(chapter.first);

            for (String subHeading : chapter.second)
            {
                m_ListAdapter.add("\t\t" + subHeading);
            }
        }

        m_ListView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id)
    {
        MaterialTextView clickedView = (MaterialTextView)view;
        String clickedText = clickedView.getText().toString();
        m_Listener.OnHeadingSelected(clickedText.replaceAll("\\s+", ""));
    }
}
