package com.kingjoshdavid.funfection.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.model.Friend;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VectorListAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<Friend> vectors = new ArrayList<>();

    public VectorListAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void setVectors(List<Friend> updatedVectors) {
        vectors.clear();
        if (updatedVectors != null) {
            vectors.addAll(updatedVectors);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return vectors.size();
    }

    @Override
    public Friend getItem(int position) {
        return vectors.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_vector, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Friend vector = getItem(position);

        holder.handle.setText(vector.getEffectiveDisplayName());

        int historyCount = vector.getUsernameHistory().size();
        if (historyCount > 0) {
            holder.historySignal.setText(
                    holder.historySignal.getContext().getResources().getQuantityString(
                            R.plurals.vectors_history_signal, historyCount, historyCount));
            holder.historySignal.setVisibility(View.VISIBLE);
        } else {
            holder.historySignal.setVisibility(View.GONE);
        }

        String description = vector.getDescription();
        if (!description.isEmpty()) {
            holder.description.setText(description);
            holder.description.setVisibility(View.VISIBLE);
        } else {
            holder.description.setVisibility(View.GONE);
        }

        holder.lastInfection.setText(holder.lastInfection.getContext().getString(
                R.string.friend_last_infection_value,
                formatTimestamp(vector.getLastInfectionAt(), holder.lastInfection)));

        return convertView;
    }

    private String formatTimestamp(long timestamp, TextView view) {
        if (timestamp <= 0L) {
            return view.getContext().getString(R.string.friend_last_infection_unknown);
        }
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        return formatter.format(new Date(timestamp));
    }

    private static final class ViewHolder {
        private final TextView handle;
        private final TextView historySignal;
        private final TextView lastInfection;
        private final TextView description;

        private ViewHolder(View itemView) {
            handle = itemView.findViewById(R.id.vectorHandle);
            historySignal = itemView.findViewById(R.id.vectorHistorySignal);
            lastInfection = itemView.findViewById(R.id.vectorLastInfection);
            description = itemView.findViewById(R.id.vectorDescription);
        }
    }
}
