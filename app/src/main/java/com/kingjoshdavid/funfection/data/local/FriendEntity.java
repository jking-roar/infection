package com.kingjoshdavid.funfection.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "friends")
public class FriendEntity {

    @PrimaryKey
    @NonNull
    public String id;
    public String displayName;
    public String inviteCode;
    public String displayNameOverride;
    public String notes;
    public String description;
    public boolean protectedProfile;

    public long lastInfectionAt;

    public long createdAt;
}
