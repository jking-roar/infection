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

    public static final String ARG_FIXED_VIRUS_ID = "com.kingjoshdavid.funfection.FIXED_VIRUS_ID";

    private ListView virusList;
    private TextView instructionsView;
    private TextView resultSummary;
    private List<Virus> viruses = new ArrayList<>();
    private String fixedVirusId;

    public static CombineFragment newPinnedInstance(String virusId) {
        CombineFragment fragment = new CombineFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FIXED_VIRUS_ID, virusId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            fixedVirusId = args.getString(ARG_FIXED_VIRUS_ID);
        }
    }

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
        instructionsView = view.findViewById(R.id.combineInstructions);
        resultSummary = view.findViewById(R.id.resultSummary);
        Button combineButton = view.findViewById(R.id.combineButton);
        virusList.setOnItemClickListener((parent, itemView, position, id) -> {
            Virus fixedVirus = getFixedVirus();
            if (fixedVirus == null || position < 0 || position >= viruses.size()) {
                return;
            }
            Virus tappedVirus = viruses.get(position);
            if (fixedVirus.getId().equals(tappedVirus.getId()) && !virusList.isItemChecked(position)) {
                virusList.setItemChecked(position, true);
                Toast.makeText(requireContext(),
                        getString(R.string.combine_pinned_locked, fixedVirus.getName()),
                        Toast.LENGTH_SHORT).show();
            }
        });
        combineButton.setOnClickListener(v -> confirmCombine());

        if (savedInstanceState == null) {
            Virus fixedVirus = getFixedVirus();
            if (fixedVirus != null) {
                Toast.makeText(requireContext(),
                        getString(R.string.combine_pinned_opened_toast, fixedVirus.getName()),
                        Toast.LENGTH_SHORT).show();
            }
        }
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
        Virus fixedVirus = getFixedVirus();
        if (fixedVirus != null) {
            instructionsView.setText(getString(R.string.combine_pinned_selection_instructions, fixedVirus.getName()));
            int fixedIndex = findVirusIndex(fixedVirus.getId());
            if (fixedIndex >= 0) {
                virusList.setItemChecked(fixedIndex, true);
            }
        } else {
            instructionsView.setText(R.string.combine_selection_instructions);
        }
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
        Virus fixedVirus = getFixedVirus();
        if (fixedVirus != null) {
            selected.add(fixedVirus);
        }
        SparseBooleanArray checked = virusList.getCheckedItemPositions();
        for (int i = 0; i < virusList.getCount(); i++) {
            if (!checked.get(i)) {
                continue;
            }
            Virus virus = viruses.get(i);
            if (fixedVirus != null && fixedVirus.getId().equals(virus.getId())) {
                continue;
            }
            selected.add(virus);
        }
        return selected;
    }

    @Nullable
    private Virus getFixedVirus() {
        if (fixedVirusId == null || fixedVirusId.trim().isEmpty()) {
            return null;
        }
        Virus fixedVirus = VirusRepository.getVirusById(fixedVirusId);
        if (fixedVirus == null) {
            fixedVirusId = null;
        }
        return fixedVirus;
    }

    private int findVirusIndex(String virusId) {
        for (int index = 0; index < viruses.size(); index++) {
            if (viruses.get(index).getId().equals(virusId)) {
                return index;
            }
        }
        return -1;
    }
}

