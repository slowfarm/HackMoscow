package ru.eva.hackmoscow.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.here.android.mpa.venues3d.Level;

import java.util.List;

public class VenueFloorAdapter extends BaseAdapter {

    Level[] m_levels;
    private final int m_floorItem;
    private final int m_floorName;
    private final int m_floorGroundSep;
    private LayoutInflater m_inflater = null;

    public VenueFloorAdapter(Context context, List<Level> levels, int floorItemId,
                             int floorNameId, int floorGroundSepId) {
        this.m_levels = new Level[levels.size()];
        m_floorItem = floorItemId;
        m_floorName = floorNameId;
        m_floorGroundSep = floorGroundSepId;
        for (int i = 0; i < levels.size(); i++) {
            this.m_levels[levels.size() - i - 1] = levels.get(i);
        }
        m_inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return m_levels.length;
    }

    @Override
    public Object getItem(int position) {
        return m_levels[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int getLevelIndex(Level level) {
        for (int i = 0; i < m_levels.length; i++) {
            if (m_levels[i].getFloorNumber() == level.getFloorNumber()) {
                return i;
            }
        }

        return -1;
    }

    public void updateFont(TextView text) {
        int size = text.getText().length();
        switch (size) {
            case 1:
                text.setTextSize(24);
                break;
            case 2:
                text.setTextSize(21);
                break;
            case 3:
                text.setTextSize(28);
                break;
            case 4:
                text.setTextSize(15);
                break;
            default:
                text.setTextSize(16);
                break;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (vi == null)
            vi = m_inflater.inflate(m_floorItem, null);
        TextView text = vi.findViewById(m_floorName);
        text.setText(m_levels[position].getFloorSynonym());
        updateFont(text);
        int showSep = m_levels[position].getFloorNumber() == 0
                && position != m_levels.length - 1 ? View.VISIBLE : View.INVISIBLE;
        View separator = vi.findViewById(m_floorGroundSep);
        separator.setVisibility(showSep);
        return vi;
    }
}
