package com.kingjoshdavid.funfection.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.data.FriendsRepository;
import com.kingjoshdavid.funfection.data.UserProfileRepository;
import com.kingjoshdavid.funfection.data.VirusRepository;
import com.kingjoshdavid.funfection.model.Virus;

import java.text.DateFormat;
import java.util.Date;

public class MyVirusActivity extends AppCompatActivity {

    public static final String EXTRA_VIRUS_ID = "com.kingjoshdavid.funfection.VIRUS_ID";

    private TextView virusName;
    private TextView patientZero;
    private TextView infectedPopulation;
    private TextView infectionCount;
    private TextView infectionRate;
    private TextView virusFamily;
    private TextView virusChaos;
    private TextView virusGenome;
    private TextView virusSeedSource;
    private TextView virusSeedTimestamp;
    private TextView virusOrigin;
    private Virus displayedVirus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserProfileRepository.initialize(getApplicationContext());
        VirusRepository.initialize(getApplicationContext());
        FriendsRepository.initialize(getApplicationContext());
        setContentView(R.layout.my_virus);

        virusName = findViewById(R.id.virusName);
        patientZero = findViewById(R.id.patientZero);
        infectedPopulation = findViewById(R.id.infectionPopulation);
        infectionCount = findViewById(R.id.infectionCount);
        infectionRate = findViewById(R.id.infectionRate);

        virusFamily = findViewById(R.id.virusFamily);
        virusChaos = findViewById(R.id.virusChaos);
        virusGenome = findViewById(R.id.virusGenome);
        virusSeedSource = findViewById(R.id.virusSeedSource);
        virusSeedTimestamp = findViewById(R.id.virusSeedTimestamp);
        virusOrigin = findViewById(R.id.virusOrigin);

        resolveVirus(getIntent(), virus -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (virus == null) {
                Toast.makeText(this, R.string.lab_purge_missing, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            displayedVirus = virus;
            showVirusInformation(displayedVirus);
        });

        VirusActionPanelBinder.bind(findViewById(R.id.virusActionPanel), false, true,
                new VirusActionPanelBinder.Callbacks() {
                    @Override
                    public void onViewDetails() {
                        // Not used on the details screen.
                    }

                    @Override
                    public void onShareText() {
                        shareVirusText();
                    }

                    @Override
                    public void onShareQr() {
                        showVirusQr();
                    }

                    @Override
                    public void onPurge() {
                        confirmPurge();
                    }

                    @Override
                    public void onCombine() {
                        openCombine();
                    }

                    @Override
                    public void onBackToLab() {
                        finish();
                    }
                });
    }

    private void showVirusInformation(Virus virus) {
        virusName.setText(virus.getName());
        patientZero.setText(virus.getCarrier());
        infectedPopulation.setText(Integer.toString(virus.getInfectionStrength()));
        infectionCount.setText(Integer.toString(virus.getGeneration()));
        infectionRate.setText(virus.getInfectionRate().toString());
        virusFamily.setText(virus.getFamily());
        virusChaos.setText(Integer.toString(virus.getChaos().score()));
        virusGenome.setText(virus.getGenome());
        bindSeedDetails(virus);
        String report = formatOriginReportWithProductionContext(virus,
                UserProfileRepository.getCurrentUser().getId());
        virusOrigin.setText(withItalicizedYou(report));
    }

    private void bindSeedDetails(Virus virus) {
        boolean isUserTextSeed = virus.getOriginInfo().isLabSeed();
        if (isUserTextSeed) {
            virusSeedSource.setText(R.string.virus_seed_source_user_text_tap);
            virusSeedSource.setOnClickListener(v -> showSeedTextDrilldown(virus));
        } else {
            virusSeedSource.setText(resolveSeedSourceLabelRes(virus));
            virusSeedSource.setOnClickListener(null);
        }

        virusSeedTimestamp.setText(R.string.virus_seed_timestamp_unknown);
        VirusRepository.getVirusCreatedAtAsync(virus.getId(), createdAt -> {
            if (isFinishing() || isDestroyed() || displayedVirus == null) {
                return;
            }
            if (!displayedVirus.getId().equals(virus.getId())) {
                return;
            }
            virusSeedTimestamp.setText(formatTimestamp(createdAt));
        });
    }

    private int resolveSeedSourceLabelRes(Virus virus) {
        if (virus.getOriginInfo().isWildQrSeed()) {
            return R.string.virus_seed_source_wild_qr;
        }
        if (virus.getOriginInfo().isWildBarcodeSeed()) {
            return R.string.virus_seed_source_wild_barcode;
        }
        if (virus.getOriginInfo().isLabSeed()) {
            return R.string.virus_seed_source_user_text;
        }
        return R.string.virus_seed_source_other;
    }

    private String formatTimestamp(long createdAtMillis) {
        if (createdAtMillis <= 0L) {
            return getString(R.string.virus_seed_timestamp_unknown);
        }
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
        return formatter.format(new Date(createdAtMillis));
    }

    private void showSeedTextDrilldown(Virus virus) {
        String rawSeed = virus.getRawSeed();
        String message = rawSeed == null || rawSeed.trim().isEmpty()
                ? getString(R.string.virus_seed_drilldown_empty)
                : rawSeed;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.virus_seed_drilldown_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private String formatOriginReportWithProductionContext(Virus virus, String viewerId) {
        String report = virus.getOriginReport(viewerId);
        String productionContext = virus.getProductionContext();
        if (productionContext == null || productionContext.trim().isEmpty()) {
            return report;
        }
        String contextLine = getString(R.string.virus_production_context_line, productionContext);
        if (report == null || report.trim().isEmpty()) {
            return contextLine;
        }
        return contextLine + "\n" + report;
    }

    private CharSequence withItalicizedYou(String text) {
        SpannableString styled = new SpannableString(text == null ? "" : text);
        String value = styled.toString();
        int start = 0;
        while (start >= 0 && start < value.length()) {
            int match = value.indexOf("you", start);
            if (match < 0) {
                break;
            }
            boolean leftBoundary = match == 0 || !Character.isLetterOrDigit(value.charAt(match - 1));
            int end = match + 3;
            boolean rightBoundary = end >= value.length() || !Character.isLetterOrDigit(value.charAt(end));
            if (leftBoundary && rightBoundary) {
                styled.setSpan(new StyleSpan(Typeface.ITALIC), match, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            start = end;
        }
        return styled;
    }

    private void resolveVirus(Intent intent, VirusRepository.ResultCallback<Virus> callback) {
        if (intent != null) {
            String virusId = intent.getStringExtra(EXTRA_VIRUS_ID);
            if (virusId != null) {
                VirusRepository.getVirusByIdAsync(virusId, existing -> {
                    if (existing != null) {
                        callback.onResult(existing);
                        return;
                    }
                    VirusRepository.getVirusesAsync(viruses -> callback.onResult(
                            viruses.isEmpty() ? null : viruses.get(0)));
                });
                return;
            }
        }
        VirusRepository.getVirusesAsync(viruses -> callback.onResult(
                viruses.isEmpty() ? null : viruses.get(0)));
    }

    private void shareVirusText() {
        if (displayedVirus == null) {
            return;
        }
        launchTextShare(buildInviteShareBody(displayedVirus.toShareCode()));
    }

    private void showVirusQr() {
        if (displayedVirus == null) {
            return;
        }
        String invitePayload = displayedVirus.toShareCode();
        Bitmap qrBitmap;
        try {
            qrBitmap = new BarcodeEncoder().encodeBitmap(invitePayload, BarcodeFormat.QR_CODE, 768, 768);
        } catch (WriterException error) {
            Toast.makeText(this, R.string.infect_qr_generation_failed, Toast.LENGTH_LONG).show();
            return;
        }

        android.view.View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_invite_qr, null, false);
        android.widget.ImageView qrImage = dialogView.findViewById(R.id.qrInviteImage);
        TextView qrSummary = dialogView.findViewById(R.id.qrInviteSummary);
        qrImage.setImageBitmap(qrBitmap);
        qrSummary.setText(getString(R.string.infect_qr_dialog_summary, 1));

        new MaterialAlertDialogBuilder(this)
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

    private void confirmPurge() {
        if (displayedVirus == null) {
            return;
        }
        VirusRepository.getPurgeStatusAsync(displayedVirus.getId(), status -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (status == VirusRepository.PurgeResult.BLOCKED_LAST) {
                Toast.makeText(this, R.string.lab_purge_last_blocked, Toast.LENGTH_SHORT).show();
                return;
            }
            if (status == VirusRepository.PurgeResult.MISSING) {
                Toast.makeText(this, R.string.lab_purge_missing, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.lab_purge_title)
                    .setMessage(getString(R.string.lab_purge_message, displayedVirus.getName()))
                    .setNegativeButton(R.string.infection_preview_cancel, null)
                    .setPositiveButton(R.string.lab_purge_confirm, (dialog, which) -> purgeVirus())
                    .show();
        });
    }

    private void purgeVirus() {
        if (displayedVirus == null) {
            return;
        }
        VirusRepository.purgeVirusByIdAsync(displayedVirus.getId(), result -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (result == VirusRepository.PurgeResult.BLOCKED_LAST) {
                Toast.makeText(this, R.string.lab_purge_last_blocked, Toast.LENGTH_SHORT).show();
                return;
            }
            if (result == VirusRepository.PurgeResult.MISSING) {
                Toast.makeText(this, R.string.lab_purge_missing, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this,
                    getString(R.string.lab_purge_success, displayedVirus.getName()),
                    Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void openCombine() {
        if (displayedVirus == null) {
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_COMBINE_VIRUS_ID, displayedVirus.getId());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}