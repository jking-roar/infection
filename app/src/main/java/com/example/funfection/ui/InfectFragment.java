package com.example.funfection.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.funfection.R;
import com.example.funfection.data.VirusRepository;
import com.example.funfection.engine.InfectionEngine;
import com.example.funfection.engine.VirusFactory;
import com.example.funfection.model.Virus;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class InfectFragment extends Fragment {

    private ListView virusList;
    private EditText friendCode;
    private TextView resultSummary;
    private List<Virus> viruses = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_infect, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        virusList = view.findViewById(R.id.virusList);
        friendCode = view.findViewById(R.id.friendCode);
        resultSummary = view.findViewById(R.id.resultSummary);
        Button infectButton = view.findViewById(R.id.infectButton);
        Button shareButton = view.findViewById(R.id.shareButton);
        infectButton.setOnClickListener(v -> prepareAndShowPreview());
        shareButton.setOnClickListener(v -> shareInvite());
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

    private void prepareAndShowPreview() {
        List<Virus> selected = getSelectedViruses();
        boolean autoSelected = false;
        if (selected.isEmpty() && !viruses.isEmpty()) {
            selected.add(viruses.get(0));
            virusList.setItemChecked(0, true);
            autoSelected = true;
        }

        String inviteCodeText = friendCode.getText().toString();
        List<Virus> friendViruses = VirusFactory.parseInviteCode(inviteCodeText);
        boolean decodedInvite = !friendViruses.isEmpty();
        if (friendViruses.isEmpty()) {
            friendViruses.add(VirusFactory.createRandomFriendVirus());
        }

        Virus offspring = InfectionEngine.infect(selected, friendViruses);
        InfectionPlan plan = new InfectionPlan(selected, friendViruses, offspring, autoSelected, decodedInvite);
        showPreview(plan);
    }

    private void showPreview(InfectionPlan plan) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.infection_preview_title)
                .setMessage(buildPreviewMessage(plan))
                .setNegativeButton(R.string.infection_preview_cancel, null)
                .setPositiveButton(R.string.infection_preview_proceed, (d, w) -> executeInfection(plan))
                .show();
    }

    private String buildPreviewMessage(InfectionPlan plan) {
        StringBuilder msg = new StringBuilder();
        msg.append(getString(R.string.infection_preview_seeds_heading)).append("\n");

        if (plan.autoSelected && !plan.myViruses.isEmpty()) {
            msg.append(getString(R.string.infection_preview_auto_selected, plan.myViruses.get(0).getName())).append("\n");
        } else if (plan.myViruses.isEmpty()) {
            msg.append(getString(R.string.infection_preview_no_local_seed)).append("\n");
        }

        appendVirusList(msg, getString(R.string.infection_preview_your_seeds), plan.myViruses);
        msg.append("\n");
        appendVirusList(msg, getString(R.string.infection_preview_friend_seeds), plan.friendViruses);
        msg.append("\n");
        if (plan.decodedInvite) {
            msg.append(getString(R.string.infection_preview_friend_source_invite, plan.friendViruses.size()));
        } else {
            msg.append(getString(R.string.infection_preview_friend_source_random, plan.friendViruses.get(0).getName()));
        }

        msg.append("\n\n").append(getString(R.string.infection_preview_possibilities_heading)).append("\n");
        msg.append(getString(R.string.infection_preview_lineage, plan.offspring.getFamily(), plan.offspring.getGenome())).append("\n");
        msg.append(getString(plan.offspring.hasMutation()
                ? R.string.infection_preview_mutation_likely
                : R.string.infection_preview_mutation_stable)).append("\n");
        msg.append(getString(R.string.infection_preview_stats,
                plan.offspring.getInfectivity().score(),
                plan.offspring.getResilience().score(),
                plan.offspring.getChaos().score(),
                plan.offspring.getInfectionRate().toString())).append("\n");
        msg.append(getString(R.string.infection_preview_carrier_chain, plan.offspring.getCarrier()));
        return msg.toString();
    }

    private void appendVirusList(StringBuilder msg, String heading, List<Virus> source) {
        msg.append(heading);
        if (source.isEmpty()) {
            msg.append("\n").append(getString(R.string.infection_preview_no_seed_entries));
            return;
        }
        for (Virus v : source) {
            msg.append("\n").append(getString(R.string.infection_preview_seed_item,
                    v.getName(), v.getFamily(), v.getGenome(),
                    v.getInfectionRate().toString(),
                    v.getInfectionStrength(), v.getInfectionCount()));
        }
    }

    private void executeInfection(InfectionPlan plan) {
        List<String> ids = new ArrayList<>();
        for (Virus v : plan.myViruses) ids.add(v.getId());
        VirusRepository.incrementInfectionCounts(ids);
        VirusRepository.addVirus(plan.offspring);
        refreshList();

        String hostLabel = plan.myViruses.size() + " of yours";
        String friendLabel = plan.friendViruses.size() + " from your friend";
        String mutationLabel = plan.offspring.hasMutation() ? "Mutation detected." : "Stable transfer.";
        String inviteMode = plan.decodedInvite ? "Invite code decoded." : "Random friend strain used.";
        resultSummary.setText("Combined " + hostLabel + " with " + friendLabel + ". "
                + plan.offspring.getName() + " emerged in the " + plan.offspring.getFamily()
                + " family with genome " + plan.offspring.getGenome()
                + ". Strength " + plan.offspring.getInfectionStrength()
                + ", lineage infections " + plan.offspring.getInfectionCount()
                + ". " + mutationLabel + " " + inviteMode);
        Toast.makeText(requireContext(), "New virus: " + plan.offspring.getName(), Toast.LENGTH_SHORT).show();
    }

    private void shareInvite() {
        List<Virus> selected = getSelectedViruses();
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), "Select at least one virus to share.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> ids = new ArrayList<>();
        for (Virus v : selected) ids.add(v.getId());
        List<Virus> shared = VirusRepository.incrementInfectionCounts(ids);
        if (shared.isEmpty()) shared = selected;
        refreshList();

        StringBuilder body = new StringBuilder();
        body.append("Swap strains with me in Funfection. Paste this invite code into the lab:\n\n");
        for (Virus v : shared) {
            body.append(v.toShareCode()).append("\n");
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Funfection invite");
        intent.putExtra(Intent.EXTRA_TEXT, body.toString());
        startActivity(Intent.createChooser(intent, "Share virus invite"));
    }

    private List<Virus> getSelectedViruses() {
        List<Virus> selected = new ArrayList<>();
        SparseBooleanArray checked = virusList.getCheckedItemPositions();
        for (int i = 0; i < virusList.getCount(); i++) {
            if (checked.get(i)) selected.add(viruses.get(i));
        }
        return selected;
    }

    private static final class InfectionPlan {
        final List<Virus> myViruses;
        final List<Virus> friendViruses;
        final Virus offspring;
        final boolean autoSelected;
        final boolean decodedInvite;

        InfectionPlan(List<Virus> myViruses, List<Virus> friendViruses, Virus offspring,
                      boolean autoSelected, boolean decodedInvite) {
            this.myViruses = myViruses;
            this.friendViruses = friendViruses;
            this.offspring = offspring;
            this.autoSelected = autoSelected;
            this.decodedInvite = decodedInvite;
        }
    }
}

