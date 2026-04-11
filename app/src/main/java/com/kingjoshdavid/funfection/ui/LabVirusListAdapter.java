package com.kingjoshdavid.funfection.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.model.Virus;

import java.util.ArrayList;
import java.util.List;

public class LabVirusListAdapter extends BaseAdapter {

    public interface Callbacks {
        void onViewDetails(Virus virus);
        void onShareText(Virus virus);
        void onShareQr(Virus virus);
        void onPurge(Virus virus);
        void onCombine(Virus virus);
    }

    private final LayoutInflater inflater;
    private final Callbacks callbacks;
    private final List<Virus> viruses = new ArrayList<>();
    private String expandedVirusId;

    public LabVirusListAdapter(Context context, Callbacks callbacks) {
        this.inflater = LayoutInflater.from(context);
        this.callbacks = callbacks;
    }

    public void setViruses(List<Virus> updatedViruses) {
        viruses.clear();
        if (updatedViruses != null) {
            viruses.addAll(updatedViruses);
        }
        if (expandedVirusId != null && findVirusById(expandedVirusId) == null) {
            expandedVirusId = null;
        }
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
            convertView = inflater.inflate(R.layout.item_lab_virus, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Virus virus = getItem(position);
        boolean expanded = virus.getId().equals(expandedVirusId);
        holder.name.setText(virus.getName());
        holder.summary.setText(virus.getSummaryLine());
        holder.actionPanel.setVisibility(expanded ? View.VISIBLE : View.GONE);
        holder.toggleButton.setText(expanded
                ? R.string.lab_action_hide_actions
                : R.string.lab_action_show_actions);

        View.OnClickListener toggleListener = v -> toggleExpanded(virus.getId());
        holder.header.setOnClickListener(toggleListener);
        holder.toggleButton.setOnClickListener(toggleListener);

        VirusActionPanelBinder.bind(holder.actionPanel, true, false,
                new VirusActionPanelBinder.Callbacks() {
                    @Override
                    public void onViewDetails() {
                        callbacks.onViewDetails(virus);
                    }

                    @Override
                    public void onShareText() {
                        callbacks.onShareText(virus);
                    }

                    @Override
                    public void onShareQr() {
                        callbacks.onShareQr(virus);
                    }

                    @Override
                    public void onPurge() {
                        callbacks.onPurge(virus);
                    }

                    @Override
                    public void onCombine() {
                        callbacks.onCombine(virus);
                    }

                    @Override
                    public void onBackToLab() {
                        // Not used in the Lab row action panel.
                    }
                });
        holder.actionPanel.setVisibility(expanded ? View.VISIBLE : View.GONE);

        return convertView;
    }

    private void toggleExpanded(String virusId) {
        if (virusId != null && virusId.equals(expandedVirusId)) {
            expandedVirusId = null;
        } else {
            expandedVirusId = virusId;
        }
        notifyDataSetChanged();
    }

    private Virus findVirusById(String virusId) {
        for (Virus virus : viruses) {
            if (virus.getId().equals(virusId)) {
                return virus;
            }
        }
        return null;
    }

    private static final class ViewHolder {
        private final View header;
        private final TextView name;
        private final TextView summary;
        private final Button toggleButton;
        private final View actionPanel;

        private ViewHolder(View itemView) {
            header = itemView.findViewById(R.id.labVirusHeader);
            name = itemView.findViewById(R.id.labVirusName);
            summary = itemView.findViewById(R.id.labVirusSummary);
            toggleButton = itemView.findViewById(R.id.labVirusToggleActions);
            actionPanel = itemView.findViewById(R.id.virusActionPanel);
        }
    }
}

