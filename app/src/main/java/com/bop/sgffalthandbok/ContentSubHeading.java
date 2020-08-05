package com.bop.sgffalthandbok;

public class ContentSubHeading
{
    enum Type
    {
        SUBHEADING,
        VIDEO
    }

    private String  m_Text;
    private Type    m_Type;
    private String  m_Metadata;

    public ContentSubHeading(final String text, final Type type, final String metadata)
    {
        m_Text      = text;
        m_Type      = type;
        m_Metadata  = metadata;
    }

    public String GetText()
    {
        return m_Text;
    }

    public Type GetType()
    {
        return m_Type;
    }

    public String GetMetadata()
    {
        return m_Metadata;
    }
}
