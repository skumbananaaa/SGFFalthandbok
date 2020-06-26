package com.bop.sgffalthandbok;

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

    public String GetHeading()
    {
        return m_Heading;
    }

    public String GetSampleText()
    {
        return m_SampleText;
    }

    public int GetPageNumber()
    {
        return m_PageNumber;
    }
}
