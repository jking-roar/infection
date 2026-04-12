package com.kingjoshdavid.funfection.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "friend_virus_cross_ref",
        primaryKeys = {"friendId", "virusId"},
        foreignKeys = {
                @ForeignKey(entity = FriendEntity.class,
                        parentColumns = "id",
                        childColumns = "friendId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = VirusEntity.class,
                        parentColumns = "id",
                        childColumns = "virusId",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("friendId"), @Index("virusId")}
)
public class FriendVirusCrossRef {

    @NonNull
    public String friendId;

    @NonNull
    public String virusId;

    public long linkedAt;
}

