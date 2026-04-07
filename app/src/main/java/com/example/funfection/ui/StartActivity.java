package com.example.funfection.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
    private EditText friendCode;
    private ArrayAdapter<String> adapter;
    private List<Virus> viruses;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start);

        virusList = (ListView) findViewById(R.id.virusList);
        collectionSummary = (TextView) findViewById(R.id.collectionSummary);
        resultSummary = (TextView) findViewById(R.id.resultSummary);
        friendCode = (EditText) findViewById(R.id.friendCode);

        Button infectButton = (Button) findViewById(R.id.infectButton);
        Button shareButton = (Button) findViewById(R.id.shareButton);
        Button viewButton = (Button) findViewById(R.id.viewButton);

        infectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                infectFriend();
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareInvite();
            }
        });

        viewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSelectedVirus();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCollection();
    }

    private void refreshCollection() {
        viruses = VirusRepository.getViruses();
        List<String> labels = new ArrayList<String>();
        for (Virus virus : viruses) {
            labels.add(virus.getSummaryLine());
        }
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, labels);
        virusList.setAdapter(adapter);
        collectionSummary.setText("Collected viruses: " + viruses.size());
    }

    private void infectFriend() {
        InfectionPlan plan = prepareInfectionPlan();
        showInfectionPreview(plan);
    }

    private InfectionPlan prepareInfectionPlan() {
        List<Virus> selectedViruses = getSelectedViruses();
        boolean autoSelectedPrimary = false;
        if (selectedViruses.isEmpty() && !viruses.isEmpty()) {
            selectedViruses.add(viruses.get(0));
            virusList.setItemChecked(0, true);
            autoSelectedPrimary = true;
        }

        String inviteCodeText = friendCode.getText().toString();
        List<Virus> friendViruses = VirusFactory.parseInviteCode(inviteCodeText);
        boolean decodedInviteCode = !friendViruses.isEmpty();
        if (friendViruses.isEmpty()) {
            friendViruses.add(VirusFactory.createRandomFriendVirus());
        }

        Virus offspring = InfectionEngine.infect(selectedViruses, friendViruses);
        return new InfectionPlan(selectedViruses, friendViruses, offspring, autoSelectedPrimary, decodedInviteCode);
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
        Virus offspring = plan.offspring;
        VirusRepository.addVirus(offspring);
        refreshCollection();
        resultSummary.setText(buildOutbreakSummary(plan.selectedViruses, plan.friendViruses, offspring, plan.decodedInviteCode));
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
        appendVirusList(message, getString(R.string.infection_preview_friend_seeds), plan.friendViruses);
        message.append("\n");

        if (plan.decodedInviteCode) {
            message.append(getString(R.string.infection_preview_friend_source_invite, Integer.valueOf(plan.friendViruses.size())));
        } else {
            message.append(getString(R.string.infection_preview_friend_source_random, plan.friendViruses.get(0).getName()));
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
                Integer.valueOf(plan.offspring.getInfectivity().score()),
                Integer.valueOf(plan.offspring.getResilience().score()),
                Integer.valueOf(plan.offspring.getChaos().score()),
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
                    virus.getInfectionRate().toString()));
        }
    }

    private void shareInvite() {
        List<Virus> selectedViruses = getSelectedViruses();
        if (selectedViruses.isEmpty()) {
            Toast.makeText(this, "Select at least one virus to share.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder body = new StringBuilder();
        body.append("Swap strains with me in Funfection. Paste this invite code into the lab:\n\n");
        for (Virus virus : selectedViruses) {
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
        List<Virus> selectedViruses = new ArrayList<Virus>();
        SparseBooleanArray checkedItems = virusList.getCheckedItemPositions();
        for (int index = 0; index < virusList.getCount(); index++) {
            if (checkedItems.get(index)) {
                selectedViruses.add(viruses.get(index));
            }
        }
        return selectedViruses;
    }

    private String buildOutbreakSummary(List<Virus> selectedViruses,
                                        List<Virus> friendViruses,
                                        Virus offspring,
                                        boolean decodedInviteCode) {
        String hostLabel = selectedViruses.size() + " of yours";
        String friendLabel = friendViruses.size() + " from your friend";
        String mutationLabel = offspring.hasMutation() ? "Mutation detected." : "Stable transfer.";
        String inviteMode = decodedInviteCode ? "Invite code decoded." : "Random friend strain used.";
        return "Combined " + hostLabel + " with " + friendLabel + ". "
                + offspring.getName() + " emerged in the " + offspring.getFamily() + " family with genome "
                + offspring.getGenome() + ". " + mutationLabel + " " + inviteMode;
    }

    private static final class InfectionPlan {

        private final List<Virus> selectedViruses;
        private final List<Virus> friendViruses;
        private final Virus offspring;
        private final boolean autoSelectedPrimary;
        private final boolean decodedInviteCode;

        private InfectionPlan(List<Virus> selectedViruses,
                              List<Virus> friendViruses,
                              Virus offspring,
                              boolean autoSelectedPrimary,
                              boolean decodedInviteCode) {
            this.selectedViruses = selectedViruses;
            this.friendViruses = friendViruses;
            this.offspring = offspring;
            this.autoSelectedPrimary = autoSelectedPrimary;
            this.decodedInviteCode = decodedInviteCode;
        }
    }
}
