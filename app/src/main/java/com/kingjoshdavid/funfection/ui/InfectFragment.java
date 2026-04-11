package com.kingjoshdavid.funfection.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.data.VirusRepository;
import com.kingjoshdavid.funfection.engine.InfectionEngine;
import com.kingjoshdavid.funfection.engine.VirusFactory;
import com.kingjoshdavid.funfection.model.Virus;

import java.util.List;

public class InfectFragment extends Fragment {

    private EditText friendCode;
    private TextView resultSummary;
    private ActivityResultLauncher<ScanOptions> qrScanLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        qrScanLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (!isAdded()) {
                return;
            }
            String contents = result.getContents();
            if (contents == null || contents.trim().isEmpty()) {
                Toast.makeText(requireContext(), R.string.infect_qr_scan_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            friendCode.setText(contents.trim());
            friendCode.setSelection(friendCode.getText().length());
            Toast.makeText(requireContext(), R.string.infect_qr_scan_success, Toast.LENGTH_SHORT).show();
        });
    }

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
        friendCode = view.findViewById(R.id.friendCode);
        resultSummary = view.findViewById(R.id.resultSummary);
        Button importButton = view.findViewById(R.id.infectButton);
        Button scanQrButton = view.findViewById(R.id.scanQrButton);
        importButton.setOnClickListener(v -> prepareAndShowPreview());
        scanQrButton.setOnClickListener(v -> scanInviteQr());
    }

    private void prepareAndShowPreview() {
        String inviteCodeText = friendCode.getText().toString().trim();
        List<Virus> friendViruses = VirusFactory.parseInviteCode(inviteCodeText);
        boolean decodedInvite = !friendViruses.isEmpty();
        if (friendViruses.isEmpty()) {
            friendViruses.add(VirusFactory.createRandomFriendVirus());
        }

        List<Virus> myViruses = VirusRepository.getViruses();
        List<Virus> localSeeds = myViruses.isEmpty()
                ? java.util.Collections.emptyList()
                : java.util.Collections.singletonList(myViruses.get(0));

        Virus offspring = InfectionEngine.infect(localSeeds, friendViruses);

        String sourceLabel = decodedInvite
                ? getString(R.string.import_preview_source_code, friendViruses.size())
                : getString(R.string.import_preview_source_random);
        String previewMessage = getString(R.string.import_preview_message,
                offspring.getFamily(),
                offspring.getGenome(),
                getString(offspring.hasMutation()
                        ? R.string.infection_preview_mutation_likely
                        : R.string.infection_preview_mutation_stable),
                sourceLabel);

        final List<Virus> finalFriendViruses = friendViruses;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.import_preview_title)
                .setMessage(previewMessage)
                .setNegativeButton(R.string.infection_preview_cancel, null)
                .setPositiveButton(R.string.import_button_confirm,
                        (d, w) -> executeImport(localSeeds, finalFriendViruses, offspring, decodedInvite))
                .show();
    }

    private void executeImport(List<Virus> localSeeds, List<Virus> friendViruses,
                               Virus offspring, boolean decodedInvite) {
        VirusRepository.addVirus(offspring);
        String inviteMode = decodedInvite
                ? getString(R.string.import_result_code_decoded, friendViruses.size())
                : getString(R.string.import_result_random_used);
        resultSummary.setText(getString(R.string.import_result_summary,
                offspring.getName(),
                offspring.getFamily(),
                offspring.getGenome(),
                offspring.getInfectionStrength(),
                offspring.getGeneration(),
                inviteMode));
        Toast.makeText(requireContext(),
                getString(R.string.infect_new_virus_toast, offspring.getName()),
                Toast.LENGTH_SHORT).show();
    }

    private void scanInviteQr() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt(getString(R.string.infect_qr_scan_prompt));
        options.setBeepEnabled(false);
        options.setOrientationLocked(true);
        qrScanLauncher.launch(options);
    }
}
