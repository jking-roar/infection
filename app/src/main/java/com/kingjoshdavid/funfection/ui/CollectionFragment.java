package com.kingjoshdavid.funfection.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.data.UserProfileRepository;
import com.kingjoshdavid.funfection.data.VirusRepository;
import com.kingjoshdavid.funfection.engine.VirusFactory;
import com.kingjoshdavid.funfection.model.UserProfile;
import com.kingjoshdavid.funfection.model.Virus;

import java.util.ArrayList;
import java.util.List;

public class CollectionFragment extends Fragment {

    private ListView virusList;
    private TextView collectionSummary;
    private EditText userNameInput;
    private List<Virus> viruses = new ArrayList<>();
    private LabVirusListAdapter virusAdapter;
    private EditText pendingCreateSeedInput;
    private ActivityResultLauncher<ScanOptions> createSeedScanLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createSeedScanLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (!isAdded() || pendingCreateSeedInput == null) {
                return;
            }
            String contents = result.getContents();
            if (contents == null || contents.trim().isEmpty()) {
                Toast.makeText(requireContext(), R.string.lab_seed_scan_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            pendingCreateSeedInput.setText(contents.trim());
            pendingCreateSeedInput.setSelection(pendingCreateSeedInput.getText().length());
            Toast.makeText(requireContext(), R.string.lab_seed_scan_loaded, Toast.LENGTH_SHORT).show();
        });
    }

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
        Button createVirusButton = view.findViewById(R.id.createVirusButton);

        userNameInput.setText(UserProfileRepository.getCurrentUser().getUserName());
        virusAdapter = new LabVirusListAdapter(requireContext(), new LabVirusListAdapter.Callbacks() {
            @Override
            public void onViewDetails(Virus virus) {
                openVirusDetails(virus);
            }

            @Override
            public void onShareText(Virus virus) {
                shareVirusText(virus);
            }

            @Override
            public void onShareQr(Virus virus) {
                showVirusQr(virus);
            }

            @Override
            public void onPurge(Virus virus) {
                confirmPurge(virus);
            }

            @Override
            public void onCombine(Virus virus) {
                openCombine(virus);
            }
        });
        virusList.setAdapter(virusAdapter);

        saveUserNameButton.setOnClickListener(v -> {
            UserProfile updated = UserProfileRepository.updateUserName(
                    userNameInput.getText().toString());
            userNameInput.setText(updated.getUserName());
            refreshCollection();
            Toast.makeText(requireContext(),
                    getString(R.string.username_saved_toast, updated.getUserName()),
                    Toast.LENGTH_SHORT).show();
        });

        createVirusButton.setOnClickListener(v -> promptCreateVirus());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCollection();
    }

    private void refreshCollection() {
        viruses = VirusRepository.getViruses();
        if (virusAdapter != null) {
            virusAdapter.setViruses(viruses);
        }
        UserProfile userProfile = UserProfileRepository.getCurrentUser();
        collectionSummary.setText(getString(
                R.string.collection_summary_collected_viruses,
                userProfile.getUserName(),
                viruses.size()));
    }

    private void openVirusDetails(Virus virus) {
        Intent intent = new Intent(requireContext(), MyVirusActivity.class);
        intent.putExtra(MyVirusActivity.EXTRA_VIRUS_ID, virus.getId());
        startActivity(intent);
    }

    private void shareVirusText(Virus virus) {
        launchTextShare(buildInviteShareBody(virus.toShareCode()));
    }

    private void showVirusQr(Virus virus) {
        String invitePayload = virus.toShareCode();
        Bitmap qrBitmap;
        try {
            qrBitmap = new BarcodeEncoder().encodeBitmap(invitePayload, BarcodeFormat.QR_CODE, 768, 768);
        } catch (WriterException error) {
            Toast.makeText(requireContext(), R.string.infect_qr_generation_failed, Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_invite_qr, null, false);
        android.widget.ImageView qrImage = dialogView.findViewById(R.id.qrInviteImage);
        TextView qrSummary = dialogView.findViewById(R.id.qrInviteSummary);
        qrImage.setImageBitmap(qrBitmap);
        qrSummary.setText(getString(R.string.infect_qr_dialog_summary, 1));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.infect_qr_dialog_title)
                .setView(dialogView)
                .setNegativeButton(R.string.infect_qr_dialog_close, null)
                .setPositiveButton(R.string.infect_qr_dialog_share_text,
                        (dialog, which) -> launchTextShare(buildInviteShareBody(invitePayload)))
                .show();
    }

    private String buildInviteShareBody(String invitePayload) {
        return getString(R.string.infect_share_body, invitePayload);
    }

    private void launchTextShare(String body) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.infect_share_subject));
        intent.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(intent, getString(R.string.infect_share_chooser)));
    }

    private void confirmPurge(Virus virus) {
        VirusRepository.PurgeResult status = VirusRepository.getPurgeStatus(virus.getId());
        if (status == VirusRepository.PurgeResult.BLOCKED_LAST) {
            Toast.makeText(requireContext(), R.string.lab_purge_last_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        if (status == VirusRepository.PurgeResult.MISSING) {
            Toast.makeText(requireContext(), R.string.lab_purge_missing, Toast.LENGTH_SHORT).show();
            refreshCollection();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.lab_purge_title)
                .setMessage(getString(R.string.lab_purge_message, virus.getName()))
                .setNegativeButton(R.string.infection_preview_cancel, null)
                .setPositiveButton(R.string.lab_purge_confirm, (dialog, which) -> purgeVirus(virus))
                .show();
    }

    private void purgeVirus(Virus virus) {
        VirusRepository.PurgeResult result = VirusRepository.purgeVirusById(virus.getId());
        refreshCollection();
        if (result == VirusRepository.PurgeResult.BLOCKED_LAST) {
            Toast.makeText(requireContext(), R.string.lab_purge_last_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        if (result == VirusRepository.PurgeResult.MISSING) {
            Toast.makeText(requireContext(), R.string.lab_purge_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(requireContext(),
                getString(R.string.lab_purge_success, virus.getName()),
                Toast.LENGTH_SHORT).show();
    }

    private void openCombine(Virus virus) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, CombineFragment.newPinnedInstance(virus.getId()))
                .addToBackStack(null)
                .commit();
    }

    private void promptCreateVirus() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_virus, null, false);
        EditText seedInput = dialogView.findViewById(R.id.createVirusSeedInput);
        Button scanButton = dialogView.findViewById(R.id.createVirusScanButton);
        pendingCreateSeedInput = seedInput;

        scanButton.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
            options.setPrompt(getString(R.string.lab_seed_scan_prompt));
            options.setBeepEnabled(false);
            options.setOrientationLocked(true);
            createSeedScanLauncher.launch(options);
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.lab_seed_title)
                .setView(dialogView)
                .setNegativeButton(R.string.infection_preview_cancel, (d, w) -> pendingCreateSeedInput = null)
                .setPositiveButton(R.string.create_virus_button,
                        (dialog, which) -> {
                            pendingCreateSeedInput = null;
                            createFromLab(seedInput.getText().toString());
                        })
                .show();
    }

    private void createFromLab(String seed) {
        Virus createdVirus = VirusFactory.createLabVirus(seed);
        VirusRepository.addVirus(createdVirus);
        refreshCollection();
        openVirusDetails(createdVirus);
    }
}

