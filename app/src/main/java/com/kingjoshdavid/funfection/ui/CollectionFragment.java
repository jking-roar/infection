package com.kingjoshdavid.funfection.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
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
import com.kingjoshdavid.funfection.engine.InfectionEngine;
import com.kingjoshdavid.funfection.engine.VirusFactory;
import com.kingjoshdavid.funfection.model.UserProfile;
import com.kingjoshdavid.funfection.model.Virus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionFragment extends Fragment {

    public static final String ARG_PENDING_COMBINE_VIRUS_ID =
            "com.kingjoshdavid.funfection.PENDING_COMBINE_VIRUS_ID";

    /** Creates a CollectionFragment that will immediately open the combine panel for the given virus. */
    public static CollectionFragment newCombineInstance(String virusId) {
        CollectionFragment f = new CollectionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PENDING_COMBINE_VIRUS_ID, virusId);
        f.setArguments(args);
        return f;
    }

    // Collection views
    private ListView virusList;
    private TextView collectionSummary;
    private TextView collectionWelcome;
    private List<Virus> viruses = new ArrayList<>();
    private LabVirusListAdapter virusAdapter;
    private EditText pendingCreateSeedInput;
    private ActivityResultLauncher<ScanOptions> createSeedScanLauncher;

    // Combine panel
    private DrawerLayout combineDrawerLayout;
    private CombineSelectorAdapter combineSelectorAdapter;
    private final Set<String> selectedPartnerIds = new HashSet<>();
    private String pinnedCombineVirusId;
    private TextView pinnedVirusNameView;
    private TextView pinnedVirusMetaView;
    private TextView combineSelectorSummaryView;
    private String pendingOpenCombineVirusId;

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

        // --- Collection views ---
        virusList = view.findViewById(R.id.virusList);
        collectionSummary = view.findViewById(R.id.collectionSummary);
        collectionWelcome = view.findViewById(R.id.collectionWelcome);
        Button createVirusButton = view.findViewById(R.id.createVirusButton);

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

        createVirusButton.setOnClickListener(v -> promptCreateVirus());

        // --- Combine panel (DrawerLayout) ---
        combineDrawerLayout = view.findViewById(R.id.combineDrawerLayout);
        pinnedVirusNameView = view.findViewById(R.id.pinnedVirusName);
        pinnedVirusMetaView = view.findViewById(R.id.pinnedVirusMeta);
        combineSelectorSummaryView = view.findViewById(R.id.combineSelectorSummary);
        ListView selectorList = view.findViewById(R.id.combineSelectorList);
        Button combineActionButton = view.findViewById(R.id.combineActionButton);

        // Prevent swipe-to-open; only allow programmatic opening.
        combineDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);

        combineSelectorAdapter = new CombineSelectorAdapter(requireContext(), selectedPartnerIds);
        selectorList.setAdapter(combineSelectorAdapter);
        selectorList.setOnItemClickListener((parent, itemView, position, id) -> onPartnerVirusTapped(position));

        combineActionButton.setOnClickListener(v -> executeCombine());

        combineDrawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                // Re-lock so swipe cannot reopen it.
                combineDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
                pinnedCombineVirusId = null;
                selectedPartnerIds.clear();
                updateCombineSelectorSummary();
            }
        });

        // Intercept the back button: close the drawer first if it is open.
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (combineDrawerLayout != null
                                && combineDrawerLayout.isDrawerOpen(GravityCompat.END)) {
                            combineDrawerLayout.closeDrawer(GravityCompat.END);
                        } else {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                            setEnabled(true);
                        }
                    }
                });

        // Consume any pending combine virus ID passed via arguments (from MyVirusActivity flow).
        Bundle args = getArguments();
        if (args != null) {
            pendingOpenCombineVirusId = args.getString(ARG_PENDING_COMBINE_VIRUS_ID);
            if (pendingOpenCombineVirusId != null) {
                args.remove(ARG_PENDING_COMBINE_VIRUS_ID);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCollection();

        // Open the combine panel if we have a pending virus ID (e.g. from MyVirusActivity).
        if (pendingOpenCombineVirusId != null) {
            final String virusId = pendingOpenCombineVirusId;
            pendingOpenCombineVirusId = null;
            VirusRepository.getVirusByIdAsync(virusId, pendingVirus -> {
                if (!isAdded() || pendingVirus == null || combineDrawerLayout == null) {
                    return;
                }
                combineDrawerLayout.post(() -> openCombine(pendingVirus));
            });
        }
    }

    // -------------------------------------------------------------------------
    // Collection helpers
    // -------------------------------------------------------------------------

    private void refreshCollection() {
        VirusRepository.getVirusesAsync(loadedViruses -> {
            if (!isAdded()) {
                return;
            }
            viruses = loadedViruses;
            if (virusAdapter != null) {
                virusAdapter.setViruses(viruses);
            }
            UserProfile userProfile = UserProfileRepository.getCurrentUser();
            if (collectionWelcome != null) {
                collectionWelcome.setText(getString(
                        R.string.collection_welcome,
                        userProfile.getUserName()));
            }
            collectionSummary.setText(getString(
                    R.string.collection_summary_collected_viruses,
                    userProfile.getUserName(),
                    viruses.size()));
        });
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
        VirusRepository.getPurgeStatusAsync(virus.getId(), status -> {
            if (!isAdded()) {
                return;
            }
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
        });
    }

    private void purgeVirus(Virus virus) {
        VirusRepository.purgeVirusByIdAsync(virus.getId(), result -> {
            if (!isAdded()) {
                return;
            }
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
        });
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
        VirusRepository.addVirusAsync(createdVirus, () -> {
            if (!isAdded()) {
                return;
            }
            refreshCollection();
            openVirusDetails(createdVirus);
        });
    }

    // -------------------------------------------------------------------------
    // Combine panel
    // -------------------------------------------------------------------------

    /** Opens the combine drawer with the given virus pinned as the left-hand strain. */
    private void openCombine(Virus virus) {
        if (virus == null || combineDrawerLayout == null) {
            return;
        }
        pinnedCombineVirusId = virus.getId();
        selectedPartnerIds.clear();

        pinnedVirusNameView.setText(virus.getName());
        pinnedVirusMetaView.setText(virus.getFamily() + " | " + virus.getGenome());

        refreshCombinePartnerList();

        // Unlock so the user can swipe it closed after it opens.
        combineDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
        combineDrawerLayout.openDrawer(GravityCompat.END);
    }

    /** Rebuilds the partner-virus list in the selector (excludes the pinned virus). */
    private void refreshCombinePartnerList() {
        VirusRepository.getVirusesAsync(allViruses -> {
            if (!isAdded()) {
                return;
            }
            List<Virus> partnerViruses = new ArrayList<>();
            for (Virus v : allViruses) {
                if (!v.getId().equals(pinnedCombineVirusId)) {
                    partnerViruses.add(v);
                }
            }
            // No pinned ID in the adapter – pinned virus is shown in its own dedicated row above.
            combineSelectorAdapter.setViruses(partnerViruses, null);
            updateCombineSelectorSummary();
        });
    }

    private void onPartnerVirusTapped(int position) {
        if (position < 0 || position >= combineSelectorAdapter.getCount()) {
            return;
        }
        Virus tapped = combineSelectorAdapter.getItem(position);
        if (selectedPartnerIds.contains(tapped.getId())) {
            selectedPartnerIds.remove(tapped.getId());
        } else {
            selectedPartnerIds.add(tapped.getId());
        }
        combineSelectorAdapter.notifyDataSetChanged();
        updateCombineSelectorSummary();
    }

    private void updateCombineSelectorSummary() {
        if (combineSelectorSummaryView == null) {
            return;
        }
        int count = selectedPartnerIds.size();
        if (count == 0) {
            combineSelectorSummaryView.setText(R.string.combine_selector_none_selected);
        } else {
            combineSelectorSummaryView.setText(getString(R.string.combine_selector_selected_count, count));
        }
    }

    /**
     * Immediately combines the pinned virus with any selected partners,
     * adds the result to the repository, closes the drawer, and opens
     * the new virus in {@link MyVirusActivity}.
     */
    private void executeCombine() {
        if (pinnedCombineVirusId == null) {
            return;
        }
        VirusRepository.getVirusByIdAsync(pinnedCombineVirusId, pinned -> {
            if (!isAdded() || pinned == null) {
                return;
            }
            VirusRepository.getVirusesAsync(allViruses -> {
                if (!isAdded()) {
                    return;
                }
                List<Virus> toMerge = new ArrayList<>();
                toMerge.add(pinned);
                for (Virus v : allViruses) {
                    if (!v.getId().equals(pinnedCombineVirusId) && selectedPartnerIds.contains(v.getId())) {
                        toMerge.add(v);
                    }
                }

                if (toMerge.size() < 2) {
                    Toast.makeText(requireContext(), R.string.combine_need_partner, Toast.LENGTH_SHORT).show();
                    return;
                }

                Virus offspring = InfectionEngine.infectLocal(toMerge);
                VirusRepository.addVirusAsync(offspring, () -> {
                    if (!isAdded()) {
                        return;
                    }
                    // Reset state, close drawer, then open the new strain.
                    pinnedCombineVirusId = null;
                    selectedPartnerIds.clear();
                    combineDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
                    combineDrawerLayout.closeDrawer(GravityCompat.END);
                    openVirusDetails(offspring);
                });
            });
        });
    }
}
