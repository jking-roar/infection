package com.kingjoshdavid.funfection.ui;

import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.data.VirusRepository;
import com.kingjoshdavid.funfection.engine.InfectionEngine;
import com.kingjoshdavid.funfection.model.Virus;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class CombineFragment extends Fragment {

    private ListView virusList;
    private TextView resultSummary;
    private List<Virus> viruses = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_combine, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        virusList = view.findViewById(R.id.virusList);
        resultSummary = view.findViewById(R.id.resultSummary);
        Button combineButton = view.findViewById(R.id.combineButton);
        combineButton.setOnClickListener(v -> confirmCombine());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        viruses = VirusRepository.getViruses();
        List<String> labels = new ArrayList<>();
        for (Virus virus : viruses) {
            labels.add(virus.getSummaryLine());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_multiple_choice,
                labels);
        virusList.setAdapter(adapter);
    }

    private void confirmCombine() {
        List<Virus> selected = getSelectedViruses();
        if (selected.isEmpty() && !viruses.isEmpty()) {
            selected.add(viruses.get(0));
            virusList.setItemChecked(0, true);
        }
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), "No viruses in collection to combine.", Toast.LENGTH_SHORT).show();
            return;
        }

        final List<Virus> toMerge = selected;
        Virus preview = InfectionEngine.infectLocal(toMerge);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.infection_preview_title)
                .setMessage(buildPreviewMessage(toMerge, preview))
                .setNegativeButton(R.string.infection_preview_cancel, null)
                .setPositiveButton(R.string.infection_preview_proceed, (d, w) -> executeCombine(toMerge, preview))
                .show();
    }

    private String buildPreviewMessage(List<Virus> sources, Virus offspring) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.infection_preview_your_seeds)).append("\n");
        for (Virus v : sources) {
            sb.append(getString(R.string.infection_preview_seed_item,
                    v.getName(), v.getFamily(), v.getGenome(),
                    v.getInfectionRate().toString(),
                    v.getInfectionStrength(), v.getGeneration())).append("\n");
        }
        sb.append("\n").append(getString(R.string.infection_preview_local_only)).append("\n\n");
        sb.append(getString(R.string.infection_preview_possibilities_heading)).append("\n");
        sb.append(getString(R.string.infection_preview_lineage, offspring.getFamily(), offspring.getGenome())).append("\n");
        sb.append(getString(offspring.hasMutation()
                ? R.string.infection_preview_mutation_likely
                : R.string.infection_preview_mutation_stable));
        return sb.toString();
    }

    private void executeCombine(List<Virus> sources, Virus offspring) {
        VirusRepository.addVirus(offspring);
        refreshList();
        resultSummary.setText("Combined " + sources.size() + " strain(s). "
                + offspring.getName() + " emerged in the " + offspring.getFamily()
                + " family with genome " + offspring.getGenome()
                + ". Strength " + offspring.getInfectionStrength()
                + ", lineage generation " + offspring.getGeneration() + ".");
        Toast.makeText(requireContext(), "New virus: " + offspring.getName(), Toast.LENGTH_SHORT).show();
    }

    private List<Virus> getSelectedViruses() {
        List<Virus> selected = new ArrayList<>();
        SparseBooleanArray checked = virusList.getCheckedItemPositions();
        for (int i = 0; i < virusList.getCount(); i++) {
            if (checked.get(i)) selected.add(viruses.get(i));
        }
        return selected;
    }
}

