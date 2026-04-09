package com.kingjoshdavid.funfection.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.data.VirusRepository;
import com.kingjoshdavid.funfection.engine.VirusFactory;
import com.kingjoshdavid.funfection.model.Virus;

public class CreateVirusFragment extends Fragment {

    private EditText labSeedInput;
    private TextView resultSummary;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_virus, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        labSeedInput = view.findViewById(R.id.labSeedInput);
        resultSummary = view.findViewById(R.id.resultSummary);
        Button createButton = view.findViewById(R.id.createButton);
        createButton.setOnClickListener(v -> createLabVirus());
    }

    private void createLabVirus() {
        String rawSeedInput = labSeedInput.getText().toString();
        String normalizedSeed = rawSeedInput.trim();
        Virus virus = VirusFactory.createLabVirus(rawSeedInput);

        VirusRepository.addVirus(virus);
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
        Toast.makeText(requireContext(),
                getString(R.string.create_virus_toast, virus.getName()),
                Toast.LENGTH_SHORT).show();
    }
}

