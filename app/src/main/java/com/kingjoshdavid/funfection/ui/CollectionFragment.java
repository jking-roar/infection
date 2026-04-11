package com.kingjoshdavid.funfection.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputType;
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

    private static final int ACTION_VIEW_DETAILS = 0;
    private static final int ACTION_SHARE_TEXT = 1;
    private static final int ACTION_SHARE_QR = 2;
    private static final int ACTION_PURGE = 3;
    private static final int ACTION_COMBINE = 4;

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
        Button createVirusButton = view.findViewById(R.id.createVirusButton);

        userNameInput.setText(UserProfileRepository.getCurrentUser().getUserName());
        virusList.setOnItemClickListener((parent, itemView, position, id) -> {
            if (position >= 0 && position < viruses.size()) {
                showVirusActions(viruses.get(position));
            }
        });

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
        List<String> labels = new ArrayList<>();
        for (Virus virus : viruses) {
            labels.add(virus.getSummaryLine());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                labels);
        virusList.setAdapter(adapter);
        UserProfile userProfile = UserProfileRepository.getCurrentUser();
        collectionSummary.setText(getString(
                R.string.collection_summary_collected_viruses,
                userProfile.getUserName(),
                viruses.size()));
    }

    private void showVirusActions(Virus virus) {
        String[] actions = new String[] {
                getString(R.string.lab_action_view_details),
                getString(R.string.lab_action_share_text),
                getString(R.string.lab_action_share_qr),
                getString(R.string.lab_action_purge),
                getString(R.string.lab_action_combine)
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.lab_action_menu_title, virus.getName()))
                .setItems(actions, (dialog, which) -> handleVirusAction(which, virus))
                .show();
    }

    private void handleVirusAction(int actionIndex, Virus virus) {
        if (actionIndex == ACTION_VIEW_DETAILS) {
            openVirusDetails(virus);
            return;
        }
        if (actionIndex == ACTION_SHARE_TEXT) {
            shareVirusText(virus);
            return;
        }
        if (actionIndex == ACTION_SHARE_QR) {
            showVirusQr(virus);
            return;
        }
        if (actionIndex == ACTION_PURGE) {
            confirmPurge(virus);
            return;
        }
        if (actionIndex == ACTION_COMBINE) {
            openCombine(virus);
        }
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
        if (VirusRepository.getViruses().size() <= 1) {
            Toast.makeText(requireContext(), R.string.lab_purge_last_blocked, Toast.LENGTH_SHORT).show();
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
        if (VirusRepository.getViruses().size() <= 1) {
            Toast.makeText(requireContext(), R.string.lab_purge_last_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        boolean removed = VirusRepository.removeVirusById(virus.getId());
        refreshCollection();
        if (!removed) {
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
        EditText seedInput = new EditText(requireContext());
        seedInput.setHint(R.string.lab_seed_hint);
        seedInput.setInputType(InputType.TYPE_CLASS_TEXT);
        seedInput.setSingleLine(true);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.lab_seed_title)
                .setView(seedInput)
                .setNegativeButton(R.string.infection_preview_cancel, null)
                .setPositiveButton(R.string.create_virus_button,
                        (dialog, which) -> createFromLab(seedInput.getText().toString()))
                .show();
    }

    private void createFromLab(String seed) {
        Virus createdVirus = VirusFactory.createLabVirus(seed);
        VirusRepository.addVirus(createdVirus);
        refreshCollection();
        openVirusDetails(createdVirus);
    }
}

