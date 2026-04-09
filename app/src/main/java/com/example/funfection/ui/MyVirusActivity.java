package com.example.funfection.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.funfection.R;
import com.example.funfection.data.UserProfileRepository;
import com.example.funfection.data.VirusRepository;
import com.example.funfection.model.Virus;

public class MyVirusActivity extends AppCompatActivity {

    public static final String EXTRA_VIRUS_ID = "com.example.funfection.VIRUS_ID";

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
        infectionCount.setText(Integer.toString(virus.getInfectionCount()));
        infectionRate.setText(virus.getInfectionRate().toString());
        virusFamily.setText(virus.getFamily());
        virusChaos.setText(Integer.toString(virus.getChaos().score()));
        virusGenome.setText(virus.getGenome());
        String report = virus.getOriginReport(UserProfileRepository.getCurrentUser().getId());
        virusOrigin.setText(withItalicizedYou(report));
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