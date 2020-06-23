package com.example.sgffalthandbok;

import android.icu.text.StringSearch;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchFragment extends Fragment implements TextView.OnEditorActionListener
{
    private static final String ARG_DOCUMENT_TEXT_PAGES = "documentTextPages";

    private ArrayList<SearchResult>     m_SearchResults;
    private ArrayList<String>           m_DocumentTextPages;

    private SearchResultsAdapter        m_ListAdapter;

    private TextInputEditText           m_SearchField;
    private ListView                    m_SearchResultsView;

    private Pattern                     m_SearchSamplePattern;

    public SearchFragment()
    {

    }

    public static SearchFragment newInstance(ArrayList<String> documentTextPages)
    {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DOCUMENT_TEXT_PAGES, documentTextPages);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            m_DocumentTextPages = (ArrayList<String>)getArguments().getSerializable(ARG_DOCUMENT_TEXT_PAGES);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        m_SearchField           = view.findViewById(R.id.searchField);
        m_SearchResultsView     = view.findViewById(R.id.searchResults);

        m_SearchField.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        m_SearchField.setOnEditorActionListener(this);

        m_SearchResults = new ArrayList<>();
        m_ListAdapter = new SearchResultsAdapter(getContext(), m_SearchResults);
        m_SearchResultsView.setAdapter(m_ListAdapter);

        m_SearchSamplePattern = Pattern.compile("[\\t\\.]");
    }

    @Override
    public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event)
    {
        if (actionId == EditorInfo.IME_ACTION_SEARCH)
        {
            String searchString = v.getText().toString().toLowerCase();

            Locale swedishLocale = new Locale("sv", "SE");
            ArrayList<Pair<Integer, Integer>> matchedCharactedIndices = new ArrayList<Pair<Integer, Integer>>();
            m_SearchResults.clear();

            for (int p = 0; p < m_DocumentTextPages.size(); p++)
            {
                String page = m_DocumentTextPages.get(p);

                if (page.length() > 0)
                {
                    //Todo: Store Lowercase Instead
                    StringCharacterIterator characterIterator = new StringCharacterIterator(page.toLowerCase(swedishLocale));
                    StringSearch stringSearch = new StringSearch(searchString, characterIterator, swedishLocale);

                    int foundIndex = stringSearch.next();

                    while (foundIndex != StringSearch.DONE)
                    {
                        m_SearchResults.add(new SearchResult("Titel", getSampleText(page, foundIndex, searchString.length()), p));
                        matchedCharactedIndices.add(new Pair(p, foundIndex));
                        foundIndex = stringSearch.next();
                    }
                }
            }

            m_ListAdapter.notifyDataSetChanged();

            return true;
        }

        return false;
    }

    String getSampleText(String page, int resultStart, int wordLength)
    {
        final int sampleRadius = 60;
        String targetString = page.substring(Math.max(resultStart - sampleRadius, 0), Math.min(resultStart + wordLength + sampleRadius, page.length()));
        Matcher matcher = m_SearchSamplePattern.matcher(targetString);

        int earliestBefore  = -1;
        int lastAfter       = -1;

        while (matcher.find())
        {
            int foundIndex = matcher.start();

            if (foundIndex < earliestBefore || (earliestBefore == -1 && foundIndex < sampleRadius))
            {
                earliestBefore = foundIndex;
            }
            else if (foundIndex > lastAfter || (lastAfter == -1 && foundIndex > sampleRadius + wordLength))
            {
                lastAfter = lastAfter;
            }
        }

        String prefix = "...";
        String content = "";
        String suffix = "...";

        if (earliestBefore == -1)
        {
            prefix += "-";
            earliestBefore = 0;
        }
        else
        {
            earliestBefore++;
        }

        if (lastAfter == -1)
        {
            suffix = "-...";
            lastAfter = 2 * sampleRadius + wordLength;
        }
        else
        {
            lastAfter--;
        }

        content = page.substring(Math.max(resultStart + earliestBefore - sampleRadius, 0),  Math.min(resultStart + lastAfter - sampleRadius, page.length()));

        return prefix + content + suffix;
    }
}
