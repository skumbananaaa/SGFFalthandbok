package com.bop.sgffalthandbok;

import java.util.ArrayList;

public class ContentHeading
{
    enum Type
    {
        HEADING,
        ABOUT
    }

    private String                          m_Text;
    private ArrayList<ContentSubHeading>    m_Subheadings;
    private Type                            m_Type;

    public ContentHeading(final String text, final ArrayList<ContentSubHeading> subHeadings, final Type type)
    {
        m_Text          = text;
        m_Subheadings   = subHeadings;
        m_Type          = type;
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

    public Type GetType() { return m_Type; }
}
