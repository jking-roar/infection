package com.example.funfection.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.funfection.R;
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
        setContentView(R.layout.my_virus);

        virusName = (TextView) findViewById(R.id.virusName);
        patientZero = (TextView) findViewById(R.id.patientZero);
        infectedPopulation = (TextView) findViewById(R.id.infectionPopulation);
        infectionCount = (TextView) findViewById(R.id.infectionCount);
        infectionRate = (TextView) findViewById(R.id.infectionRate);

        virusFamily = (TextView) findViewById(R.id.virusFamily);
        virusChaos = (TextView) findViewById(R.id.virusChaos);
        virusGenome = (TextView) findViewById(R.id.virusGenome);
        virusOrigin = (TextView) findViewById(R.id.virusOrigin);

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
        virusOrigin.setText(virus.getOriginReport());
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