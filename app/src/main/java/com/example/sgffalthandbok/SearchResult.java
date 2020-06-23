package com.example.sgffalthandbok;

public class SearchResult
{
    private String m_Heading;
    private String  m_SampleText;
    private int     m_PageNumber;

    public SearchResult(String headingName, String sampleText, int pageNumber)
    {
        m_Heading       = headingName;
        m_SampleText    = sampleText;
        m_PageNumber    = pageNumber;
    }

    public String getHeading()
    {
        return m_Heading;
    }

    public String getSampleText()
    {
        return m_SampleText;
    }

    public int getPageNumber()
    {
        return m_PageNumber;
    }
}
