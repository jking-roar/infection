package com.kingjoshdavid.funfection.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.model.Virus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CombineSelectorAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<Virus> viruses = new ArrayList<>();
    private final Set<String> selectedPartnerIds;
    private String pinnedVirusId;

    public CombineSelectorAdapter(Context context, Set<String> selectedPartnerIds) {
        this.inflater = LayoutInflater.from(context);
        this.selectedPartnerIds = selectedPartnerIds;
    }

    public void setViruses(List<Virus> items, String pinnedId) {
        viruses.clear();
        if (items != null) {
            viruses.addAll(items);
        }
        pinnedVirusId = pinnedId;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return viruses.size();
    }

    @Override
    public Virus getItem(int position) {
        return viruses.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_combine_selector_virus, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Virus virus = getItem(position);
        boolean pinned = pinnedVirusId != null && pinnedVirusId.equals(virus.getId());
        boolean selected = pinned || selectedPartnerIds.contains(virus.getId());

        holder.name.setText(virus.getName());
        holder.meta.setText(virus.getFamily() + " | " + virus.getGenome());
        holder.check.setChecked(selected);
        if (pinned) {
            holder.badge.setText(R.string.combine_selector_badge_pinned);
        } else if (selected) {
            holder.badge.setText(R.string.combine_selector_badge_selected);
        } else {
            holder.badge.setText("");
        }

        return convertView;
    }

    private static final class ViewHolder {
        private final TextView name;
        private final TextView meta;
        private final TextView badge;
        private final CheckBox check;

        private ViewHolder(View itemView) {
            name = itemView.findViewById(R.id.combineSelectorName);
            meta = itemView.findViewById(R.id.combineSelectorMeta);
            badge = itemView.findViewById(R.id.combineSelectorBadge);
            check = itemView.findViewById(R.id.combineSelectorCheck);
        }
    }
}

