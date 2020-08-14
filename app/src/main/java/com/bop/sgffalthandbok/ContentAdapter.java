package com.bop.sgffalthandbok;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class ContentAdapter extends BaseExpandableListAdapter
{
    private Context                     m_Context;
    private ArrayList<ContentHeading>   m_TableOfContents;

    private static final int[] EMPTY_STATE_SET = {};
    private static final int[] GROUP_EXPANDED_STATE_SET =
            {android.R.attr.state_expanded};
    private static final int[][] GROUP_STATE_SETS = {
            EMPTY_STATE_SET, // 0
            GROUP_EXPANDED_STATE_SET // 1
    };

    public ContentAdapter(@NonNull Context context, ArrayList<ContentHeading> tableOfContents)
    {
        m_Context           = context;
        m_TableOfContents   = tableOfContents;
    }

    @Override
    public Object getChild(final int groupPosition, final int childPosition)
    {
        return m_TableOfContents.get(groupPosition).GetChild(childPosition);
    }

    @Override
    public long getChildId(final int groupPosition, final int childPosition)
    {
        return childPosition;
    }

    @Override
    public int getChildrenCount(final int groupPosition)
    {
        return m_TableOfContents.get(groupPosition).GetChildrenCount();
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild, View convertView, final ViewGroup parent)
    {
        final ContentSubHeading child = (ContentSubHeading)getChild(groupPosition, childPosition);

        switch (child.GetType())
        {
            case SUBHEADING:
            {
                convertView = LayoutInflater.from(m_Context).inflate(R.layout.subheading_item, parent,false);
                return CreateSubHeadingEntry(convertView, child.GetText());
            }

            case VIDEO:
            {
                convertView = LayoutInflater.from(m_Context).inflate(R.layout.video_item, parent,false);
                return CreateVideoEntry(convertView, child.GetText());
            }
        }

        return convertView;
    }

    @Override
    public Object getGroup(final int groupPosition)
    {
        return m_TableOfContents.get(groupPosition);
    }

    @Override
    public long getGroupId(final int groupPosition)
    {
        return groupPosition;
    }

    @Override
    public int getGroupCount()
    {
        return m_TableOfContents.size();
    }

    @Override
    public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView, final ViewGroup parent)
    {
        final ContentHeading heading = (ContentHeading)getGroup(groupPosition);

        if(convertView == null)
            convertView = LayoutInflater.from(m_Context).inflate(R.layout.heading_item, parent,false);

        View ind = convertView.findViewById(R.id.explist_indicator);
        if(ind != null)
        {
            ImageView indicator = (ImageView)ind;

            if( getChildrenCount( groupPosition ) == 0 )
            {
                indicator.setVisibility( View.INVISIBLE );
            }
            else
            {
                indicator.setVisibility( View.VISIBLE );
                int stateSetIndex = ( isExpanded ? 1 : 0) ;
                Drawable drawable = indicator.getDrawable();
                drawable.setState(GROUP_STATE_SETS[stateSetIndex]);
            }
        }

        return CreateHeadingEntry(convertView, heading.GetText());
    }

    @Override
    public boolean hasStableIds()
    {
        return false;
    }

    @Override
    public boolean isChildSelectable(final int groupPosition, final int childPosition)
    {
        return true;
    }

    private View CreateHeadingEntry(final View headingItem, final String headingText)
    {
        TextView headingTextView = (TextView)headingItem.findViewById(R.id.headingText);
        headingTextView.setText(headingText);

        return headingItem;
    }

    private View CreateSubHeadingEntry(final View subheadingItem, final String subHeadingText)
    {
        TextView subHeadingTextView = (TextView)subheadingItem.findViewById(R.id.subheadingText);
        subHeadingTextView.setText(subHeadingText);

        return subheadingItem;
    }

    private View CreateVideoEntry(final View videoItem, final String videoText)
    {
        TextView videoTextView = (TextView)videoItem.findViewById(R.id.videoText);
        videoTextView.setText(videoText);

        return videoItem;
    }
}
