package com.kingjoshdavid.funfection.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "friend_username_history",
        foreignKeys = @ForeignKey(
                entity = FriendEntity.class,
                parentColumns = "id",
                childColumns = "friendId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("friendId")}
)
public class FriendUsernameHistoryEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String friendId;

    @NonNull
    public String username;

    @ColumnInfo(name = "added_at")
    public long addedAt;
}
