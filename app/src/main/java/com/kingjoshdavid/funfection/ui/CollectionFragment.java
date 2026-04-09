package com.kingjoshdavid.funfection.ui;

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
import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.data.UserProfileRepository;
import com.kingjoshdavid.funfection.data.VirusRepository;
import com.kingjoshdavid.funfection.model.UserProfile;
import com.kingjoshdavid.funfection.model.Virus;

import java.util.ArrayList;
import java.util.List;

public class CollectionFragment extends Fragment {

    private ListView virusList;
    private TextView collectionSummary;
    private EditText userNameInput;
    private List<Virus> viruses = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_collection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        virusList = view.findViewById(R.id.virusList);
        collectionSummary = view.findViewById(R.id.collectionSummary);
        userNameInput = view.findViewById(R.id.userNameInput);
        Button saveUserNameButton = view.findViewById(R.id.saveUserNameButton);
        Button viewButton = view.findViewById(R.id.viewButton);

        userNameInput.setText(UserProfileRepository.getCurrentUser().getUserName());

        saveUserNameButton.setOnClickListener(v -> {
            UserProfile updated = UserProfileRepository.updateUserName(
                    userNameInput.getText().toString());
            userNameInput.setText(updated.getUserName());
            refreshCollection();
            Toast.makeText(requireContext(),
                    getString(R.string.username_saved_toast, updated.getUserName()),
                    Toast.LENGTH_SHORT).show();
        });

        viewButton.setOnClickListener(v -> openSelectedVirus());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCollection();
    }

    private void refreshCollection() {
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
        UserProfile userProfile = UserProfileRepository.getCurrentUser();
        collectionSummary.setText(getString(
                R.string.collection_summary_collected_viruses,
                userProfile.getUserName(),
                viruses.size()));
    }

    private void openSelectedVirus() {
        List<Virus> selected = getSelectedViruses();
        Virus virus = selected.isEmpty()
                ? (viruses.isEmpty() ? null : viruses.get(0))
                : selected.get(0);
        if (virus == null) {
            Toast.makeText(requireContext(), "No virus available to show.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(requireContext(), MyVirusActivity.class);
        intent.putExtra(MyVirusActivity.EXTRA_VIRUS_ID, virus.getId());
        startActivity(intent);
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

