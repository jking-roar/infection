package com.kingjoshdavid.funfection.ui;

import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CombineFragment extends Fragment {

    public static final String ARG_FIXED_VIRUS_ID = "com.kingjoshdavid.funfection.FIXED_VIRUS_ID";
    private static final boolean ENABLE_ENHANCED_COMBINE_SELECTOR = true;

    private ListView virusList;
    private ListView selectorList;
    private TextView instructionsView;
    private TextView resultSummary;
    private TextView selectorSummary;
    private View selectorPanel;
    private Button openSelectorButton;
    private List<Virus> viruses = new ArrayList<>();
    private final Set<String> selectedPartnerIds = new HashSet<>();
    private CombineSelectorAdapter selectorAdapter;
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
        selectorList = view.findViewById(R.id.combineSelectorList);
        instructionsView = view.findViewById(R.id.combineInstructions);
        resultSummary = view.findViewById(R.id.resultSummary);
        selectorSummary = view.findViewById(R.id.combineSelectorSummary);
        selectorPanel = view.findViewById(R.id.combineSelectorPanel);
        openSelectorButton = view.findViewById(R.id.combineOpenSelectorButton);
        Button closeSelectorButton = view.findViewById(R.id.combineCloseSelectorButton);
        Button combineButton = view.findViewById(R.id.combineButton);
        Button backToLabButton = view.findViewById(R.id.combine_back_button);

        if (ENABLE_ENHANCED_COMBINE_SELECTOR) {
            selectorAdapter = new CombineSelectorAdapter(requireContext(), selectedPartnerIds);
            selectorList.setAdapter(selectorAdapter);
            selectorList.setOnItemClickListener((parent, itemView, position, id) -> onSelectorVirusTapped(position));
            openSelectorButton.setOnClickListener(v -> showSelectorPanel());
            closeSelectorButton.setOnClickListener(v -> hideSelectorPanel());
            virusList.setChoiceMode(ListView.CHOICE_MODE_NONE);
        } else {
            selectorPanel.setVisibility(View.GONE);
            openSelectorButton.setVisibility(View.GONE);
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
        }

        combineButton.setOnClickListener(v -> confirmCombine());
        backToLabButton.setOnClickListener(v -> {
            if (ENABLE_ENHANCED_COMBINE_SELECTOR && selectorPanel.getVisibility() == View.VISIBLE) {
                hideSelectorPanel();
                return;
            }
            getParentFragmentManager().popBackStack();
        });

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
        viruses = new ArrayList<>(VirusRepository.getViruses());
        movePinnedVirusToTop();
        List<String> labels = buildDisplayLabels();
        int rowLayout = ENABLE_ENHANCED_COMBINE_SELECTOR
                ? android.R.layout.simple_list_item_1
                : android.R.layout.simple_list_item_multiple_choice;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), rowLayout, labels);
        virusList.setAdapter(adapter);

        Virus fixedVirus = getFixedVirus();
        if (fixedVirus != null) {
            instructionsView.setText(getString(R.string.combine_pinned_selection_instructions, fixedVirus.getName()));
            if (ENABLE_ENHANCED_COMBINE_SELECTOR) {
                selectedPartnerIds.remove(fixedVirus.getId());
                selectorAdapter.setViruses(viruses, fixedVirus.getId());
                updateSelectorSummary();
            } else {
                int fixedIndex = findVirusIndex(fixedVirus.getId());
                if (fixedIndex >= 0) {
                    virusList.setItemChecked(fixedIndex, true);
                }
            }
        } else {
            instructionsView.setText(R.string.combine_selection_instructions);
            if (ENABLE_ENHANCED_COMBINE_SELECTOR) {
                selectorAdapter.setViruses(viruses, null);
                updateSelectorSummary();
            }
        }
    }

    private List<String> buildDisplayLabels() {
        List<String> labels = new ArrayList<>();
        Virus fixedVirus = getFixedVirus();
        for (Virus virus : viruses) {
            String prefix = "";
            if (fixedVirus != null && fixedVirus.getId().equals(virus.getId())) {
                prefix = "[Pinned] ";
            } else if (selectedPartnerIds.contains(virus.getId())) {
                prefix = "[Selected] ";
            }
            labels.add(prefix + virus.getSummaryLine());
        }
        return labels;
    }

    private void movePinnedVirusToTop() {
        Virus fixedVirus = getFixedVirus();
        if (fixedVirus == null) {
            return;
        }
        int fixedIndex = findVirusIndex(fixedVirus.getId());
        if (fixedIndex <= 0) {
            return;
        }
        Virus pinned = viruses.remove(fixedIndex);
        viruses.add(0, pinned);
    }

    private void confirmCombine() {
        List<Virus> selected = getSelectedViruses();
        if (selected.isEmpty() && !viruses.isEmpty()) {
            selected.add(viruses.get(0));
            if (ENABLE_ENHANCED_COMBINE_SELECTOR) {
                selectedPartnerIds.add(viruses.get(0).getId());
                updateSelectorSummary();
            } else {
                virusList.setItemChecked(0, true);
            }
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
        if (ENABLE_ENHANCED_COMBINE_SELECTOR) {
            for (Virus virus : viruses) {
                if (fixedVirus != null && fixedVirus.getId().equals(virus.getId())) {
                    continue;
                }
                if (selectedPartnerIds.contains(virus.getId())) {
                    selected.add(virus);
                }
            }
            return selected;
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

    private void onSelectorVirusTapped(int position) {
        if (position < 0 || position >= viruses.size()) {
            return;
        }
        Virus tappedVirus = viruses.get(position);
        Virus fixedVirus = getFixedVirus();
        if (fixedVirus != null && fixedVirus.getId().equals(tappedVirus.getId())) {
            Toast.makeText(requireContext(),
                    getString(R.string.combine_pinned_locked, fixedVirus.getName()),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedPartnerIds.contains(tappedVirus.getId())) {
            selectedPartnerIds.remove(tappedVirus.getId());
        } else {
            selectedPartnerIds.add(tappedVirus.getId());
        }
        selectorAdapter.notifyDataSetChanged();
        updateSelectorSummary();
        refreshList();
    }

    private void updateSelectorSummary() {
        if (selectorSummary == null) {
            return;
        }
        int count = selectedPartnerIds.size();
        if (count == 0) {
            selectorSummary.setText(R.string.combine_selector_none_selected);
            return;
        }
        selectorSummary.setText(getString(R.string.combine_selector_selected_count, count));
    }

    private void showSelectorPanel() {
        if (selectorPanel == null || selectorPanel.getVisibility() == View.VISIBLE) {
            return;
        }
        selectorPanel.setVisibility(View.VISIBLE);
        selectorPanel.post(() -> {
            selectorPanel.setTranslationX(selectorPanel.getWidth());
            selectorPanel.animate()
                    .translationX(0f)
                    .setDuration(220)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        });
    }

    private void hideSelectorPanel() {
        if (selectorPanel == null || selectorPanel.getVisibility() != View.VISIBLE) {
            return;
        }
        selectorPanel.animate()
                .translationX(selectorPanel.getWidth())
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    selectorPanel.setVisibility(View.GONE);
                    selectorPanel.setTranslationX(0f);
                })
                .start();
    }
}

