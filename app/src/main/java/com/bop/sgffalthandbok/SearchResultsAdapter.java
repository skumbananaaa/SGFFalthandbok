package com.bop.sgffalthandbok;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SearchResultsAdapter extends ArrayAdapter<SearchResult>
{
    private Context                 m_Context;
    private List<SearchResult>      m_SearchResultsList;
    private String                  m_SearchedText;
    private int                     m_HighlightColor;

    public SearchResultsAdapter(@NonNull Context context, ArrayList<SearchResult> list)
    {
        super(context, 0 , list);
        m_Context           = context;
        m_SearchResultsList = list;
        m_HighlightColor    = Color.YELLOW;
    }

    void SetSearchedText(String text)
    {
        m_SearchedText = text;
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
        headingTextView.setText(currentSearchResult.GetHeading());

        TextView sampleTextTextView = (TextView) listItem.findViewById(R.id.searchSampleText);
        sampleTextTextView.setText(currentSearchResult.GetSampleText());
        SetHighLightedText(sampleTextTextView);

        TextView pageNumberTextView = (TextView) listItem.findViewById(R.id.searchPageNumber);
        pageNumberTextView.setText("s." + String.valueOf(currentSearchResult.GetPageNumber()));

        return listItem;
    }

    private void SetHighLightedText(TextView tv)
    {
        String tvt = tv.getText().toString();
        int ofe = tvt.indexOf(m_SearchedText, 0);
        Spannable wordToSpan = new SpannableString(tv.getText());
        for (int ofs = 0; ofs < tvt.length() && ofe != -1; ofs = ofe + 1)
        {
            ofe = tvt.indexOf(m_SearchedText, ofs);
            if (ofe == -1)
                break;
            else
            {
                wordToSpan.setSpan(new BackgroundColorSpan(m_HighlightColor), ofe, ofe + m_SearchedText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                tv.setText(wordToSpan, TextView.BufferType.SPANNABLE);
            }
        }
    }
}
