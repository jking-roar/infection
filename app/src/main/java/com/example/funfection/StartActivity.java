package com.example.funfection;

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
        List<Virus> selectedViruses = getSelectedViruses();
        if (selectedViruses.isEmpty() && !viruses.isEmpty()) {
            selectedViruses.add(viruses.get(0));
            virusList.setItemChecked(0, true);
        }

        List<Virus> friendViruses = VirusFactory.parseInviteCode(friendCode.getText().toString());
        if (friendViruses.isEmpty()) {
            friendViruses.add(VirusFactory.createRandomFriendVirus());
        }

        Virus offspring = InfectionEngine.infect(selectedViruses, friendViruses);
        VirusRepository.addVirus(offspring);
        refreshCollection();
        resultSummary.setText(buildOutbreakSummary(selectedViruses, friendViruses, offspring));
        Toast.makeText(this, "New virus created: " + offspring.getName(), Toast.LENGTH_SHORT).show();
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

    private String buildOutbreakSummary(List<Virus> selectedViruses, List<Virus> friendViruses, Virus offspring) {
        String hostLabel = selectedViruses.size() + " of yours";
        String friendLabel = friendViruses.size() + " from your friend";
        String mutationLabel = offspring.hasMutation() ? "Mutation detected." : "Stable transfer.";
        String inviteMode = TextUtils.isEmpty(friendCode.getText().toString().trim())
                ? "Random friend strain used."
                : "Invite code decoded."
                ;
        return "Combined " + hostLabel + " with " + friendLabel + ". "
                + offspring.getName() + " emerged in the " + offspring.getFamily() + " family with genome "
                + offspring.getGenome() + ". " + mutationLabel + " " + inviteMode;
    }
}
