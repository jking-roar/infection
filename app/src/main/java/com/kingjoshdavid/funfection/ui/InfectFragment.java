package com.kingjoshdavid.funfection.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.data.VirusRepository;
import com.kingjoshdavid.funfection.engine.InfectionEngine;
import com.kingjoshdavid.funfection.engine.VirusFactory;
import com.kingjoshdavid.funfection.model.Virus;

import java.util.ArrayList;
import java.util.List;

public class InfectFragment extends Fragment {

    private ListView virusList;
    private EditText friendCode;
    private TextView resultSummary;
    private List<Virus> viruses = new ArrayList<>();
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
        virusList = view.findViewById(R.id.virusList);
        friendCode = view.findViewById(R.id.friendCode);
        resultSummary = view.findViewById(R.id.resultSummary);
        Button infectButton = view.findViewById(R.id.infectButton);
        Button shareButton = view.findViewById(R.id.shareButton);
        Button shareTextButton = view.findViewById(R.id.shareTextButton);
        Button scanQrButton = view.findViewById(R.id.scanQrButton);
        infectButton.setOnClickListener(v -> prepareAndShowPreview());
        shareButton.setOnClickListener(v -> showInviteQr());
        shareTextButton.setOnClickListener(v -> shareInviteText());
        scanQrButton.setOnClickListener(v -> scanInviteQr());
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
                    v.getInfectionStrength(), v.getGeneration()));
        }
    }

    private void executeInfection(InfectionPlan plan) {
        VirusRepository.addVirus(plan.offspring);
        refreshList();

        String hostLabel = plan.myViruses.size() + " of yours";
        String friendLabel = plan.friendViruses.size() + " from your friend";
        String mutationLabel = plan.offspring.hasMutation() ? "Mutation detected." : "Stable transfer.";
        String inviteMode = plan.decodedInvite ? "Invite code decoded." : "Random friend strain used.";
        resultSummary.setText(getString(R.string.infect_result_summary,
                hostLabel,
                friendLabel,
                plan.offspring.getName(),
                plan.offspring.getFamily(),
                plan.offspring.getGenome(),
                plan.offspring.getInfectionStrength(),
                plan.offspring.getGeneration(),
                mutationLabel,
                inviteMode));
        Toast.makeText(requireContext(), getString(R.string.infect_new_virus_toast, plan.offspring.getName()), Toast.LENGTH_SHORT).show();
    }

    private void showInviteQr() {
        List<Virus> shared = prepareVirusesForSharing();
        if (shared == null) {
            return;
        }
        String invitePayload = buildInvitePayload(shared);
        Bitmap qrBitmap;
        try {
            qrBitmap = new BarcodeEncoder().encodeBitmap(invitePayload, BarcodeFormat.QR_CODE, 768, 768);
        } catch (WriterException error) {
            Toast.makeText(requireContext(), R.string.infect_qr_generation_failed, Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_invite_qr, null, false);
        ImageView qrImage = dialogView.findViewById(R.id.qrInviteImage);
        TextView qrSummary = dialogView.findViewById(R.id.qrInviteSummary);
        qrImage.setImageBitmap(qrBitmap);
        qrSummary.setText(getString(R.string.infect_qr_dialog_summary, shared.size()));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.infect_qr_dialog_title)
                .setView(dialogView)
                .setNegativeButton(R.string.infect_qr_dialog_close, null)
                .setPositiveButton(R.string.infect_qr_dialog_share_text,
                        (dialog, which) -> launchTextShare(buildInviteShareBody(invitePayload)))
                .show();
    }

    private void shareInviteText() {
        List<Virus> shared = prepareVirusesForSharing();
        if (shared == null) {
            return;
        }
        launchTextShare(buildInviteShareBody(buildInvitePayload(shared)));
    }

    private void scanInviteQr() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt(getString(R.string.infect_qr_scan_prompt));
        options.setBeepEnabled(false);
        options.setOrientationLocked(true);
        qrScanLauncher.launch(options);
    }

    @Nullable
    private List<Virus> prepareVirusesForSharing() {
        List<Virus> selected = getSelectedViruses();
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), R.string.infect_select_share_toast, Toast.LENGTH_SHORT).show();
            return null;
        }

        return selected;
    }

    private String buildInvitePayload(List<Virus> shared) {
        StringBuilder payload = new StringBuilder();
        for (Virus virus : shared) {
            if (!payload.isEmpty()) {
                payload.append("\n");
            }
            payload.append(virus.toShareCode());
        }
        return payload.toString();
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

