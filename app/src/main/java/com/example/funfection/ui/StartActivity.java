package com.example.funfection.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.funfection.R;
import com.example.funfection.data.VirusRepository;
import com.example.funfection.engine.InfectionEngine;
import com.example.funfection.engine.VirusFactory;
import com.example.funfection.model.Virus;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class StartActivity extends AppCompatActivity {

    private ListView virusList;
    private TextView collectionSummary;
    private TextView resultSummary;
    private EditText labSeedInput;
    private EditText friendCode;
    private List<Virus> viruses;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start);

        virusList = findViewById(R.id.virusList);
        collectionSummary = findViewById(R.id.collectionSummary);
        resultSummary = findViewById(R.id.resultSummary);
        labSeedInput = findViewById(R.id.labSeedInput);
        friendCode = findViewById(R.id.friendCode);

        Button createButton = findViewById(R.id.createButton);
        Button infectButton = findViewById(R.id.infectButton);
        Button shareButton = findViewById(R.id.shareButton);
        Button combineLocalButton = findViewById(R.id.combineLocalButton);
        Button viewButton = findViewById(R.id.viewButton);

        createButton.setOnClickListener(view -> createLabVirus());

        infectButton.setOnClickListener(view -> infectFriend());

        shareButton.setOnClickListener(view -> shareInvite());

        combineLocalButton.setOnClickListener(view -> combineLocalSelection());

        viewButton.setOnClickListener(view -> openSelectedVirus());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCollection();
    }

    private void refreshCollection() {
        viruses = VirusRepository.getViruses();
        List<String> labels = new ArrayList<>();
        for (Virus virus : viruses) {
            labels.add(virus.getSummaryLine());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, labels);
        virusList.setAdapter(adapter);
        collectionSummary.setText(getString(R.string.collection_summary_collected_viruses, viruses.size()));
    }

    private void infectFriend() {
        InfectionPlan plan = prepareInfectionPlan(false);
        showInfectionPreview(plan);
    }

    private void combineLocalSelection() {
        InfectionPlan plan = prepareInfectionPlan(true);
        showInfectionPreview(plan);
    }

    private void createLabVirus() {
        String rawSeedInput = labSeedInput.getText().toString();
        String normalizedSeed = rawSeedInput.trim();
        Virus virus = VirusFactory.createLabVirus(rawSeedInput);

        VirusRepository.addVirus(virus);
        refreshCollection();
        virusList.clearChoices();
        virusList.setItemChecked(0, true);
        virusList.smoothScrollToPosition(0);
        labSeedInput.setText("");

        String seedSummary = TextUtils.isEmpty(normalizedSeed)
                ? getString(R.string.create_virus_seed_random)
                : getString(R.string.create_virus_seed_manual, normalizedSeed);
        resultSummary.setText(getString(
                R.string.create_virus_summary,
                virus.getName(),
                virus.getFamily(),
                virus.getGenome(),
                seedSummary));
        Toast.makeText(this, getString(R.string.create_virus_toast, virus.getName()), Toast.LENGTH_SHORT).show();
    }

    private InfectionPlan prepareInfectionPlan(boolean localOnly) {
        List<Virus> selectedViruses = getSelectedViruses();
        boolean autoSelectedPrimary = false;
        if (selectedViruses.isEmpty() && !viruses.isEmpty()) {
            selectedViruses.add(viruses.get(0));
            virusList.setItemChecked(0, true);
            autoSelectedPrimary = true;
        }

        List<Virus> friendViruses = new ArrayList<>();
        boolean decodedInviteCode = false;
        Virus offspring;
        if (localOnly) {
            offspring = InfectionEngine.infectLocal(selectedViruses);
        } else {
            String inviteCodeText = friendCode.getText().toString();
            friendViruses = VirusFactory.parseInviteCode(inviteCodeText);
            decodedInviteCode = !friendViruses.isEmpty();
            if (friendViruses.isEmpty()) {
                friendViruses.add(VirusFactory.createRandomFriendVirus());
            }
            offspring = InfectionEngine.infect(selectedViruses, friendViruses);
        }
        return new InfectionPlan(selectedViruses, friendViruses, offspring, autoSelectedPrimary, decodedInviteCode, localOnly);
    }

    private void showInfectionPreview(final InfectionPlan plan) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.infection_preview_title)
                .setMessage(buildInfectionPreviewMessage(plan))
                .setNegativeButton(R.string.infection_preview_cancel, null)
                .setPositiveButton(R.string.infection_preview_proceed, (dialogInterface, which) -> executeInfection(plan))
                .show();
    }

    private void executeInfection(InfectionPlan plan) {
        VirusRepository.incrementInfectionCounts(getVirusIds(plan.selectedViruses));
        Virus offspring = plan.offspring;
        VirusRepository.addVirus(offspring);
        refreshCollection();
        resultSummary.setText(buildOutbreakSummary(plan.selectedViruses, plan.friendViruses, offspring,
                plan.decodedInviteCode, plan.localOnly));
        Toast.makeText(this, "New virus created: " + offspring.getName(), Toast.LENGTH_SHORT).show();
    }

    private String buildInfectionPreviewMessage(InfectionPlan plan) {
        StringBuilder message = new StringBuilder();
        message.append(getString(R.string.infection_preview_seeds_heading));
        message.append("\n");

        if (plan.autoSelectedPrimary && !plan.selectedViruses.isEmpty()) {
            message.append(getString(R.string.infection_preview_auto_selected, plan.selectedViruses.get(0).getName()));
            message.append("\n");
        } else if (plan.selectedViruses.isEmpty()) {
            message.append(getString(R.string.infection_preview_no_local_seed));
            message.append("\n");
        }

        appendVirusList(message, getString(R.string.infection_preview_your_seeds), plan.selectedViruses);
        message.append("\n");
        if (plan.localOnly) {
            message.append(getString(R.string.infection_preview_local_only));
        } else {
            appendVirusList(message, getString(R.string.infection_preview_friend_seeds), plan.friendViruses);
            message.append("\n");
            if (plan.decodedInviteCode) {
                message.append(getString(R.string.infection_preview_friend_source_invite, plan.friendViruses.size()));
            } else {
                message.append(getString(R.string.infection_preview_friend_source_random, plan.friendViruses.get(0).getName()));
            }
        }

        message.append("\n\n");
        message.append(getString(R.string.infection_preview_possibilities_heading));
        message.append("\n");
        message.append(getString(R.string.infection_preview_lineage, plan.offspring.getFamily(), plan.offspring.getGenome()));
        message.append("\n");
        message.append(getString(plan.offspring.hasMutation()
                ? R.string.infection_preview_mutation_likely
                : R.string.infection_preview_mutation_stable));
        message.append("\n");
        message.append(getString(
                R.string.infection_preview_stats,
                plan.offspring.getInfectivity().score(),
                plan.offspring.getResilience().score(),
                plan.offspring.getChaos().score(),
                plan.offspring.getInfectionRate().toString()));
        message.append("\n");
        message.append(getString(R.string.infection_preview_carrier_chain, plan.offspring.getCarrier()));
        return message.toString();
    }

    private void appendVirusList(StringBuilder message, String heading, List<Virus> source) {
        message.append(heading);
        if (source.isEmpty()) {
            message.append("\n");
            message.append(getString(R.string.infection_preview_no_seed_entries));
            return;
        }

        for (Virus virus : source) {
            message.append("\n");
            message.append(getString(
                    R.string.infection_preview_seed_item,
                    virus.getName(),
                    virus.getFamily(),
                    virus.getGenome(),
                    virus.getInfectionRate().toString(),
                    virus.getInfectionStrength(),
                    virus.getInfectionCount()));
        }
    }

    private void shareInvite() {
        List<Virus> selectedViruses = getSelectedViruses();
        if (selectedViruses.isEmpty()) {
            Toast.makeText(this, "Select at least one virus to share.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Virus> sharedViruses = VirusRepository.incrementInfectionCounts(getVirusIds(selectedViruses));
        if (sharedViruses.isEmpty()) {
            sharedViruses = selectedViruses;
        }
        refreshCollection();

        StringBuilder body = new StringBuilder();
        body.append("Swap strains with me in Funfection. Paste this invite code into the lab:\n\n");
        for (Virus virus : sharedViruses) {
            body.append(virus.toShareCode()).append("\n");
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Funfection invite");
        intent.putExtra(Intent.EXTRA_TEXT, body.toString());
        startActivity(Intent.createChooser(intent, "Share virus invite"));
    }

    private void openSelectedVirus() {
        List<Virus> selectedViruses = getSelectedViruses();
        Virus virus = selectedViruses.isEmpty() ? (viruses.isEmpty() ? null : viruses.get(0)) : selectedViruses.get(0);
        if (virus == null) {
            Toast.makeText(this, "No virus available to show.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, MyVirusActivity.class);
        intent.putExtra(MyVirusActivity.EXTRA_VIRUS_ID, virus.getId());
        startActivity(intent);
    }

    private List<Virus> getSelectedViruses() {
        List<Virus> selectedViruses = new ArrayList<>();
        SparseBooleanArray checkedItems = virusList.getCheckedItemPositions();
        for (int index = 0; index < virusList.getCount(); index++) {
            if (checkedItems.get(index)) {
                selectedViruses.add(viruses.get(index));
            }
        }
        return selectedViruses;
    }

    private List<String> getVirusIds(List<Virus> source) {
        List<String> ids = new ArrayList<>();
        for (Virus virus : source) {
            ids.add(virus.getId());
        }
        return ids;
    }

    private String buildOutbreakSummary(List<Virus> selectedViruses,
                                        List<Virus> friendViruses,
                                        Virus offspring,
                                        boolean decodedInviteCode,
                                        boolean localOnly) {
        if (localOnly) {
            return "Combined " + selectedViruses.size() + " of your strains locally. "
                    + offspring.getName() + " emerged in the " + offspring.getFamily() + " family with genome "
                    + offspring.getGenome() + ". Strength " + offspring.getInfectionStrength()
                    + ", lineage infections " + offspring.getInfectionCount() + ".";
        }
        String hostLabel = selectedViruses.size() + " of yours";
        String friendLabel = friendViruses.size() + " from your friend";
        String mutationLabel = offspring.hasMutation() ? "Mutation detected." : "Stable transfer.";
        String inviteMode = decodedInviteCode ? "Invite code decoded." : "Random friend strain used.";
        return "Combined " + hostLabel + " with " + friendLabel + ". "
                + offspring.getName() + " emerged in the " + offspring.getFamily() + " family with genome "
                + offspring.getGenome() + ". Strength " + offspring.getInfectionStrength()
                + ", lineage infections " + offspring.getInfectionCount() + ". " + mutationLabel + " " + inviteMode;
    }

    private static final class InfectionPlan {

        private final List<Virus> selectedViruses;
        private final List<Virus> friendViruses;
        private final Virus offspring;
        private final boolean autoSelectedPrimary;
        private final boolean decodedInviteCode;
        private final boolean localOnly;

        private InfectionPlan(List<Virus> selectedViruses,
                              List<Virus> friendViruses,
                              Virus offspring,
                              boolean autoSelectedPrimary,
                              boolean decodedInviteCode,
                              boolean localOnly) {
            this.selectedViruses = selectedViruses;
            this.friendViruses = friendViruses;
            this.offspring = offspring;
            this.autoSelectedPrimary = autoSelectedPrimary;
            this.decodedInviteCode = decodedInviteCode;
            this.localOnly = localOnly;
        }
    }
}
