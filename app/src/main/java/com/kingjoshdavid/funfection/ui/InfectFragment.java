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
import com.kingjoshdavid.funfection.engine.VirusFactory;
import com.kingjoshdavid.funfection.model.Virus;

import java.util.ArrayList;
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
        List<Virus> importedViruses = VirusFactory.parseInviteCode(inviteCodeText);
        if (importedViruses.isEmpty()) {
            Toast.makeText(requireContext(), R.string.import_preview_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        Virus first = importedViruses.get(0);
        String previewMessage = getString(R.string.import_preview_message,
                importedViruses.size(),
                first.getName(),
                first.getFamily(),
                first.getGeneration());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.import_preview_title)
                .setMessage(previewMessage)
                .setNegativeButton(R.string.infection_preview_cancel, null)
                .setPositiveButton(R.string.import_button_confirm,
                        (d, w) -> executeImport(new ArrayList<>(importedViruses)))
                .show();
    }

    private void executeImport(List<Virus> importedViruses) {
        saveImportedVirus(importedViruses, 0, () -> {
            if (!isAdded()) {
                return;
            }
            Virus newest = importedViruses.get(importedViruses.size() - 1);
            resultSummary.setText(getString(R.string.import_result_summary,
                    importedViruses.size(),
                    newest.getName(),
                    newest.getFamily(),
                    newest.getGeneration()));
            Toast.makeText(requireContext(),
                    getString(R.string.infect_new_virus_toast, newest.getName()),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void saveImportedVirus(List<Virus> viruses, int index, Runnable onComplete) {
        if (index >= viruses.size()) {
            onComplete.run();
            return;
        }
        VirusRepository.addVirusAsync(viruses.get(index),
                () -> saveImportedVirus(viruses, index + 1, onComplete));
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
