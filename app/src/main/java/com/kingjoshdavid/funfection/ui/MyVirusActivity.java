package com.kingjoshdavid.funfection.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.data.UserProfileRepository;
import com.kingjoshdavid.funfection.data.VirusRepository;
import com.kingjoshdavid.funfection.model.Virus;

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
    private TextView virusOrigin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserProfileRepository.initialize(getApplicationContext());
        setContentView(R.layout.my_virus);

        virusName = findViewById(R.id.virusName);
        patientZero = findViewById(R.id.patientZero);
        infectedPopulation = findViewById(R.id.infectionPopulation);
        infectionCount = findViewById(R.id.infectionCount);
        infectionRate = findViewById(R.id.infectionRate);

        virusFamily = findViewById(R.id.virusFamily);
        virusChaos = findViewById(R.id.virusChaos);
        virusGenome = findViewById(R.id.virusGenome);
        virusOrigin = findViewById(R.id.virusOrigin);

        Virus virus = resolveVirus(getIntent());

        showVirusInformation(virus);
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
        String report = formatOriginReportWithProductionContext(virus,
                UserProfileRepository.getCurrentUser().getId());
        virusOrigin.setText(withItalicizedYou(report));
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

    private Virus resolveVirus(Intent intent) {
        if (intent != null) {
            String virusId = intent.getStringExtra(EXTRA_VIRUS_ID);
            if (virusId != null) {
                Virus existing = VirusRepository.getVirusById(virusId);
                if (existing != null) {
                    return existing;
                }
            }
        }
        return VirusRepository.getViruses().get(0);
    }

    public void onClose(android.view.View button) {
        finish();
    }
}