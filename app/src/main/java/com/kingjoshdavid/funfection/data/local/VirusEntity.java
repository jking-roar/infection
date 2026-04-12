package com.kingjoshdavid.funfection.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "viruses")
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

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
