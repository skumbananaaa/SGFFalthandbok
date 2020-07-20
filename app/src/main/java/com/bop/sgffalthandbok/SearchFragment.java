package com.bop.sgffalthandbok;

import android.content.Context;
import android.icu.text.StringSearch;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchFragment extends Fragment implements TextView.OnEditorActionListener, AdapterView.OnItemClickListener
{
    public static final int    MIN_SEARCH_STRING_LENGTH        = 2;

    private ArrayList<SearchResult>                             m_SearchResults;
    private ArrayList<String>                                   m_DocumentTextPages;
    private HashMap<Integer, ArrayList<SerializablePair<Integer, String>>>  m_PageNumberToHeadings;

    private SearchResultsAdapter        m_ListAdapter;

    private TextInputEditText           m_SearchField;
    private TextView                    m_ErrorTextView;
    private ListView                    m_SearchResultsView;

    private Pattern                     m_SearchSamplePattern;

    private OnSearchResultSelectedListener   m_Listener;

    private String  m_SearchString;

    public interface OnSearchResultSelectedListener
    {
        public void OnSearchResultSelected(String searchString, SearchResult searchResult);
    }

    @Override
    public void onAttach(@NonNull final Context context)
    {
        super.onAttach(context);
        try
        {
            m_Listener = (OnSearchResultSelectedListener)context;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(context.toString() + " must implement OnSearchResultSelectedListener");
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

        ResourceManager resourceManager = new ViewModelProvider(requireActivity()).get(ResourceManager.class);

        m_DocumentTextPages     = resourceManager.GetDocumentTextPages();
        m_PageNumberToHeadings  = resourceManager.GetPageNumberToHeadings();

        m_SearchString          = "";

        m_SearchField           = view.findViewById(R.id.searchField);
        m_ErrorTextView         = view.findViewById(R.id.searchErrorView);
        m_SearchResultsView     = view.findViewById(R.id.searchResults);

        m_SearchField.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        m_SearchField.setOnEditorActionListener(this);

        m_SearchResults = new ArrayList<>();
        m_ListAdapter = new SearchResultsAdapter(getContext(), m_SearchResults);
        m_SearchResultsView.setAdapter(m_ListAdapter);
        m_SearchResultsView.setOnItemClickListener(this);

        m_SearchSamplePattern = Pattern.compile("[\\s]");

        if (savedInstanceState != null)
        {
            m_SearchString = savedInstanceState.getString("m_SearchString");
            PerformSearch();
        }

        if (m_SearchString.length() > 0 && m_SearchString.length() < MIN_SEARCH_STRING_LENGTH)
        {
            m_ErrorTextView.setVisibility(View.VISIBLE);
        }
        else
        {
            m_ErrorTextView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState)
    {
        outState.putString("m_SearchString", m_SearchString);

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event)
    {
        if (actionId == EditorInfo.IME_ACTION_SEARCH)
        {
            m_SearchString = v.getText().toString().toLowerCase();
            PerformSearch();
        }

        return true;
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id)
    {
        SearchResult searchResult = (SearchResult) parent.getItemAtPosition(position);
        m_Listener.OnSearchResultSelected(m_SearchString, searchResult);
    }

    private void PerformSearch()
    {
        Locale swedishLocale = new Locale("sv", "SE");
        ArrayList<SerializablePair<Integer, Integer>> matchedCharactedIndices = new ArrayList<SerializablePair<Integer, Integer>>();
        m_SearchResults.clear();

        if (m_SearchString.length() > MIN_SEARCH_STRING_LENGTH)
        {
            m_ErrorTextView.setVisibility(View.INVISIBLE);

            for (int pageIndex = 0; pageIndex < m_DocumentTextPages.size(); pageIndex++)
            {
                String page = m_DocumentTextPages.get(pageIndex);

                if (page.length() > 0)
                {
                    //Todo: Store Lowercase Instead
                    StringCharacterIterator characterIterator = new StringCharacterIterator(page.toLowerCase(swedishLocale));
                    StringSearch stringSearch = new StringSearch(m_SearchString, characterIterator, swedishLocale);

                    int foundIndex = stringSearch.next();
                    int skipHits = 0;

                    while (foundIndex != StringSearch.DONE)
                    {
                        if (skipHits == 0)
                        {
                            String title = GetTitle(pageIndex, foundIndex);
                            String sampleText = GetSampleText(page, m_SearchString, foundIndex);

                            skipHits = DuplicatesInSampleTextCount(sampleText, m_SearchString);

                            m_SearchResults.add(new SearchResult(title, sampleText, pageIndex));
                            matchedCharactedIndices.add(new SerializablePair(pageIndex, foundIndex));
                        }
                        else
                        {
                            skipHits--;
                        }

                        foundIndex = stringSearch.next();
                    }
                }
            }
        }
        else
        {
            m_ErrorTextView.setVisibility(View.VISIBLE);
        }

        m_ListAdapter.SetSearchedText(m_SearchString);
        m_ListAdapter.notifyDataSetChanged();
    }

    private String GetTitle(int startPageIndex, int resultStart)
    {
        for (int pageIndex = startPageIndex; pageIndex >= 0; pageIndex--)
        {
            if (m_PageNumberToHeadings.containsKey(pageIndex))
            {
                final ArrayList<SerializablePair<Integer, String>> charIndexAndHeadings = m_PageNumberToHeadings.get(pageIndex);

                for (int headingIndex = charIndexAndHeadings.size() - 1; headingIndex >= 0; headingIndex--)
                {
                    final SerializablePair<Integer, String> charIndexAndHeading = charIndexAndHeadings.get(headingIndex);

                    if (charIndexAndHeading.first < resultStart)
                        return charIndexAndHeading.second;
                }
            }
        }

        return "Ingen Titel";
    }

    private String GetSampleText(String page, String searchedWord, int resultStart)
    {
        final int wordLength    = searchedWord.length();
        final int sampleRadius  = 60;
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

        String prefix       = "...";
        String suffix       = "...";

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

        String content   = page.substring(Math.max(resultStart + earliestBefore - sampleRadius, 0), Math.min(resultStart + lastAfter - sampleRadius, page.length()));

        return prefix + content + suffix;
    }

    private int DuplicatesInSampleTextCount(String sampleText, String searchString)
    {
        int searchStringLength  = searchString.length();
        int lastIndex           = 0;
        int count               = 0;

        while(true)
        {
            lastIndex = sampleText.indexOf(searchString, lastIndex);

            if(lastIndex != -1)
            {
                count++;
                lastIndex += searchStringLength;
            }
            else
            {
                break;
            }
        }

        return count - 1;
    }
}
