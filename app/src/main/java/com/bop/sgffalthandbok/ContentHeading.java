package com.bop.sgffalthandbok;

import java.util.ArrayList;

public class ContentHeading
{
    private String                          m_Text;
    private ArrayList<ContentSubHeading>    m_Subheadings;

    public ContentHeading(final String text, final ArrayList<ContentSubHeading> subHeadings)
    {
        m_Text          = text;
        m_Subheadings   = subHeadings;
    }

    public String GetText()
    {
        return m_Text;
    }

    public ContentSubHeading GetChild(int index)
    {
        return m_Subheadings.get(index);
    }

    public int GetChildrenCount()
    {
        return m_Subheadings.size();
    }
}
