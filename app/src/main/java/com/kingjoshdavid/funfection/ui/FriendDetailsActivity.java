package com.kingjoshdavid.funfection.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.data.FriendsRepository;
import com.kingjoshdavid.funfection.data.VirusRepository;
import com.kingjoshdavid.funfection.model.Friend;
import com.kingjoshdavid.funfection.model.UsernameHistoryEntry;
import com.kingjoshdavid.funfection.model.Virus;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FriendDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_FRIEND_ID = "com.kingjoshdavid.funfection.FRIEND_ID";

    private TextView nameView;
    private TextView sourceHandleView;
    private TextView lastInfectionView;
    private TextView descriptionView;
    private TextView nicknameTextView;
    private TextView notesTextView;
    private TextView historyTextView;
    private ScrollView historyContainer;
    private Button toggleHistoryButton;
    private View notesActions;
    private View notesEditor;
    private EditText notesInput;
    private EditText nicknameInput;
    private Button editNotesButton;
    private Button clearNicknameButton;
    private Button clearNotesButton;
    private Button cancelNotesButton;
    private Button saveNotesButton;
    private ListView virusListView;
    private Friend friend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_details);

        nameView = findViewById(R.id.friendDetailsName);
        sourceHandleView = findViewById(R.id.friendDetailsSourceHandle);
        lastInfectionView = findViewById(R.id.friendDetailsLastInfection);
        descriptionView = findViewById(R.id.friendDetailsDescription);
        nicknameTextView = findViewById(R.id.friendDetailsNicknameText);
        notesTextView = findViewById(R.id.friendDetailsNotesText);
        historyTextView = findViewById(R.id.friendDetailsHistoryText);
        historyContainer = findViewById(R.id.friendDetailsHistoryContainer);
        toggleHistoryButton = findViewById(R.id.friendDetailsToggleHistory);
        notesActions = findViewById(R.id.friendDetailsNotesActions);
        notesEditor = findViewById(R.id.friendDetailsNotesEditor);
        notesInput = findViewById(R.id.friendDetailsNotesInput);
        nicknameInput = findViewById(R.id.friendDetailsNicknameInput);
        editNotesButton = findViewById(R.id.friendDetailsEditNotesButton);
        clearNicknameButton = findViewById(R.id.friendDetailsClearNicknameButton);
        clearNotesButton = findViewById(R.id.friendDetailsClearNotesButton);
        cancelNotesButton = findViewById(R.id.friendDetailsCancelNotesButton);
        saveNotesButton = findViewById(R.id.friendDetailsSaveNotesButton);
        virusListView = findViewById(R.id.friendDetailsVirusList);

        findViewById(R.id.friendDetailsBackButton).setOnClickListener(v -> finish());
        toggleHistoryButton.setOnClickListener(v -> toggleHistory());
        editNotesButton.setOnClickListener(v -> enterEditMode());
        cancelNotesButton.setOnClickListener(v -> exitEditMode());
        clearNicknameButton.setOnClickListener(v -> saveFriendDetails("", notesInput.getText().toString()));
        clearNotesButton.setOnClickListener(v -> saveFriendDetails(nicknameInput.getText().toString(), ""));
        saveNotesButton.setOnClickListener(v -> saveFriendDetails(
                nicknameInput.getText().toString(),
                notesInput.getText().toString()));

        String friendId = getIntent().getStringExtra(EXTRA_FRIEND_ID);
        if (friendId == null || friendId.trim().isEmpty()) {
            Toast.makeText(this, R.string.vectors_empty, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadFriend(friendId);
    }

    private void loadFriend(String friendId) {
        FriendsRepository.getFriendByIdAsync(friendId, loaded -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (loaded == null) {
                Toast.makeText(this, R.string.friend_detail_missing, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            friend = loaded;
            bindFriend();
            loadAssociatedViruses();
        });
    }

    private void bindFriend() {
        nameView.setText(friend.getEffectiveDisplayName());
        sourceHandleView.setText(getString(R.string.friend_source_handle_value, friend.getDisplayName()));
        lastInfectionView.setText(getString(R.string.friend_last_infection_value,
                formatTimestamp(friend.getLastInfectionAt())));

        String nickname = friend.getDisplayNameOverride();
        nicknameTextView.setText(nickname.isEmpty()
                ? getString(R.string.friend_nickname_empty)
                : nickname);
        nicknameInput.setText(nickname);

        if (friend.getDescription().isEmpty()) {
            descriptionView.setVisibility(View.GONE);
        } else {
            descriptionView.setVisibility(View.VISIBLE);
            descriptionView.setText(friend.getDescription());
        }

        notesTextView.setText(friend.getNotes().isEmpty()
                ? getString(R.string.friend_notes_empty)
                : friend.getNotes());
        notesInput.setText(friend.getNotes());

        boolean editableNotes = !friend.isProtectedProfile();
        notesActions.setVisibility(editableNotes ? View.VISIBLE : View.GONE);
        notesEditor.setVisibility(View.GONE);
        nicknameInput.setEnabled(editableNotes);
        notesInput.setEnabled(editableNotes);
        if (!editableNotes) {
            nicknameTextView.setText(getString(R.string.friend_nickname_locked));
            notesTextView.setText(getString(R.string.friend_notes_locked));
        }

        List<UsernameHistoryEntry> history = friend.getUsernameHistory();
        if (history.isEmpty()) {
            toggleHistoryButton.setVisibility(View.GONE);
            historyContainer.setVisibility(View.GONE);
        } else {
            toggleHistoryButton.setVisibility(View.VISIBLE);
            toggleHistoryButton.setText(R.string.friend_history_show);
            historyContainer.setVisibility(View.GONE);
            historyTextView.setText(buildHistoryText(history));
        }
    }

    private void loadAssociatedViruses() {
        VirusRepository.getVirusesByFriendIdAsync(friend.getId(), viruses -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            List<String> rows = new ArrayList<>();
            for (Virus virus : viruses) {
                rows.add(virus.getSummaryLine());
            }
            if (rows.isEmpty()) {
                rows.add(getString(R.string.friend_associated_viruses_empty));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1,
                    rows);
            virusListView.setAdapter(adapter);
            virusListView.setOnItemClickListener((parent, view, position, id) -> {
                if (position < 0 || position >= viruses.size()) {
                    return;
                }
                Intent intent = new Intent(this, MyVirusActivity.class);
                intent.putExtra(MyVirusActivity.EXTRA_VIRUS_ID, viruses.get(position).getId());
                startActivity(intent);
            });
        });
    }

    private void toggleHistory() {
        if (historyContainer.getVisibility() == View.VISIBLE) {
            historyContainer.setVisibility(View.GONE);
            toggleHistoryButton.setText(R.string.friend_history_show);
        } else {
            historyContainer.setVisibility(View.VISIBLE);
            toggleHistoryButton.setText(R.string.friend_history_hide);
        }
    }

    private void enterEditMode() {
        notesActions.setVisibility(View.GONE);
        notesEditor.setVisibility(View.VISIBLE);
        notesInput.requestFocus();
    }

    private void exitEditMode() {
        notesEditor.setVisibility(View.GONE);
        notesActions.setVisibility(View.VISIBLE);
        nicknameInput.setText(friend.getDisplayNameOverride());
        notesInput.setText(friend.getNotes());
    }

    private void saveFriendDetails(String nicknameOverride, String notes) {
        if (friend == null || friend.isProtectedProfile()) {
            return;
        }
        Friend updated = new Friend(friend.getId(),
                friend.getDisplayName(),
                nicknameOverride,
                notes,
                friend.getDescription(),
                false,
                friend.getUsernameHistory(),
                friend.getLastInfectionAt());
        FriendsRepository.saveFriendAsync(updated, () -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            friend = updated;
            bindFriend();
            Toast.makeText(this, R.string.friend_notes_saved, Toast.LENGTH_SHORT).show();
        });
    }

    private String buildHistoryText(List<UsernameHistoryEntry> history) {
        StringBuilder text = new StringBuilder();
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        for (int i = history.size() - 1; i >= 0; i--) {
            UsernameHistoryEntry entry = history.get(i);
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(entry.getUsername()).append(" - ")
                    .append(formatter.format(new Date(entry.getAddedAt())));
        }
        return text.toString();
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp <= 0L) {
            return getString(R.string.friend_last_infection_unknown);
        }
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
        return formatter.format(new Date(timestamp));
    }
}

