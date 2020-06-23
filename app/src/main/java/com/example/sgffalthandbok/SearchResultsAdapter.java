package com.example.sgffalthandbok;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SearchResultsAdapter extends ArrayAdapter<SearchResult>
{
    private Context                 m_Context;
    private List<SearchResult>      m_SearchResultsList;

    public SearchResultsAdapter(@NonNull Context context, ArrayList<SearchResult> list)
    {
        super(context, 0 , list);
        m_Context           = context;
        m_SearchResultsList = list;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable final View convertView, @NonNull final ViewGroup parent)
    {
        View listItem = convertView;
        if(listItem == null)
            listItem = LayoutInflater.from(m_Context).inflate(R.layout.search_list_item, parent,false);

        SearchResult currentSearchResult = m_SearchResultsList.get(position);

        TextView headingTextView = (TextView) listItem.findViewById(R.id.searchHeading);
        headingTextView.setText(currentSearchResult.getHeading());

        TextView sampleTextTextView = (TextView) listItem.findViewById(R.id.searchSampleText);
        sampleTextTextView.setText(currentSearchResult.getSampleText());

        TextView pageNumberTextView = (TextView) listItem.findViewById(R.id.searchPageNumber);
        pageNumberTextView.setText("s." + String.valueOf(currentSearchResult.getPageNumber()));

        return listItem;
    }
}
