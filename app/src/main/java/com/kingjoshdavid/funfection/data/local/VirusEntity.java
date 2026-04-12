package com.kingjoshdavid.funfection.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "viruses",
        indices = {
                @Index("primary_patient_zero_id"),
                @Index("secondary_patient_zero_id"),
                @Index("combined_left_carrier_id"),
                @Index("combined_right_carrier_id")
        })
public class VirusEntity {

    @PrimaryKey
    @NonNull
    public String id;
    public String prefix;
    public String suffix;
    public String family;
    public String carrier;
    public int infectivity;
    public int resilience;
    public int chaos;
    public boolean mutation;
    public String genome;

    @ColumnInfo(name = "origin_summary")
    public String originSummary;

    @ColumnInfo(name = "origin_payload")
    public String originPayload;

    public int generation;

    @ColumnInfo(name = "production_context")
    public String productionContext;

    @ColumnInfo(name = "raw_seed")
    public String rawSeed;

    public long seed;

    @ColumnInfo(name = "primary_patient_zero_id")
    public String primaryPatientZeroId;

    @ColumnInfo(name = "secondary_patient_zero_id")
    public String secondaryPatientZeroId;

    @ColumnInfo(name = "combined_left_carrier_id")
    public String combinedLeftCarrierId;

    @ColumnInfo(name = "combined_right_carrier_id")
    public String combinedRightCarrierId;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
