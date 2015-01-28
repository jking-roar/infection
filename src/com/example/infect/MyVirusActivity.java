package com.example.infect;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MyVirusActivity extends Activity {

    private TextView patientZero;
    private TextView infectedPopulation;
    private TextView infectionRate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_virus);

        patientZero = (TextView) findViewById(R.id.patientZero);
        infectedPopulation = (TextView) findViewById(R.id.infectionPopulation);
        infectionRate = (TextView) findViewById(R.id.infectionRate);

        Virus virus = new Virus();

        showVirusInformation(virus);
    }

    private void showVirusInformation(Virus virus) {
        patientZero.setText(virus.getPatientZero());
        infectedPopulation.setText(Integer.toString(virus.getInfectionCount()));
        infectionRate.setText(virus.getInfectionRate().toString());
    }

    public void onClickFixData(android.view.View button ) {

    }

    private class Virus {
        private String virusPatientZero;
        private int infectionCount;
        private InfectionRates infectionRate;

        public String getPatientZero() {
            return virusPatientZero;
        }

        public int getInfectionCount() {
            return infectionCount;
        }

        public InfectionRates getInfectionRate() {
            return infectionRate;
        }

        public Virus() {
            virusPatientZero = "Jerry";
            infectionCount = 120;
            infectionRate = InfectionRates.HIGH;
        }
    }
}