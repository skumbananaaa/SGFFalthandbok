package com.example.sgffalthandbok;

import android.content.Context;
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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchFragment extends Fragment implements TextView.OnEditorActionListener, AdapterView.OnItemClickListener
{
    private static final String ARG_DOCUMENT_TEXT_PAGES         = "documentTextPages";
    private static final String ARG_PAGE_NUMBER_TO_HEADINGS     = "pageNumberToHeadings";

    private ArrayList<SearchResult>                             m_SearchResults;
    private ArrayList<String>                                   m_DocumentTextPages;
    private HashMap<Integer, ArrayList<Pair<Integer, String>>>  m_PageNumberToHeadings;

    private SearchResultsAdapter        m_ListAdapter;

    private TextInputEditText           m_SearchField;
    private ListView                    m_SearchResultsView;

    private Pattern                     m_SearchSamplePattern;

    private OnSearchResultSelectedListener   m_Listener;

    public interface OnSearchResultSelectedListener
    {
        public void OnSearchResultSelected(SearchResult searchResult);
    }

    public SearchFragment()
    {

    }

    public static SearchFragment newInstance(ArrayList<String> documentTextPages, HashMap<Integer, ArrayList<Pair<Integer, String>>> pageNumberToHeadings)
    {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DOCUMENT_TEXT_PAGES, documentTextPages);
        args.putSerializable(ARG_PAGE_NUMBER_TO_HEADINGS, pageNumberToHeadings);
        fragment.setArguments(args);
        return fragment;
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
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            m_DocumentTextPages     = (ArrayList<String>) getArguments().getSerializable(ARG_DOCUMENT_TEXT_PAGES);
            m_PageNumberToHeadings  = (HashMap<Integer, ArrayList<Pair<Integer, String>>>) getArguments().getSerializable(ARG_PAGE_NUMBER_TO_HEADINGS);
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
        m_SearchResultsView.setOnItemClickListener(this);

        m_SearchSamplePattern = Pattern.compile("[\\s]");
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

            for (int pageIndex = 0; pageIndex < m_DocumentTextPages.size(); pageIndex++)
            {
                String page = m_DocumentTextPages.get(pageIndex);

                if (page.length() > 0)
                {
                    //Todo: Store Lowercase Instead
                    StringCharacterIterator characterIterator = new StringCharacterIterator(page.toLowerCase(swedishLocale));
                    StringSearch stringSearch = new StringSearch(searchString, characterIterator, swedishLocale);

                    int foundIndex  = stringSearch.next();
                    int skipHits    = 0;

                    while (foundIndex != StringSearch.DONE)
                    {
                        if (skipHits == 0)
                        {
                            String title        = GetTitle(pageIndex, foundIndex);
                            String sampleText   = GetSampleText(page, searchString, foundIndex);

                            skipHits            = DuplicatesInSampleTextCount(sampleText, searchString);

                            m_SearchResults.add(new SearchResult(title, sampleText, pageIndex));
                            matchedCharactedIndices.add(new Pair(pageIndex, foundIndex));
                        }
                        else
                        {
                            skipHits--;
                        }

                        foundIndex = stringSearch.next();
                    }
                }
            }

            m_ListAdapter.SetSearchedText(searchString);
            m_ListAdapter.notifyDataSetChanged();

            return true;
        }

        return false;
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id)
    {
        SearchResult searchResult = (SearchResult) parent.getItemAtPosition(position);
        m_Listener.OnSearchResultSelected(searchResult);
    }

    String GetTitle(int startPageIndex, int resultStart)
    {
        for (int pageIndex = startPageIndex; pageIndex >= 0; pageIndex--)
        {
            if (m_PageNumberToHeadings.containsKey(pageIndex))
            {
                final ArrayList<Pair<Integer, String>> charIndexAndHeadings = m_PageNumberToHeadings.get(pageIndex);

                for (int headingIndex = charIndexAndHeadings.size() - 1; headingIndex >= 0; headingIndex--)
                {
                    final Pair<Integer, String> charIndexAndHeading = charIndexAndHeadings.get(headingIndex);

                    if (charIndexAndHeading.first < resultStart)
                        return charIndexAndHeading.second;
                }
            }
        }

        return "Ingen Titel";
    }

    String GetSampleText(String page, String searchedWord, int resultStart)
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

    int DuplicatesInSampleTextCount(String sampleText, String searchString)
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
