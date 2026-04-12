package com.kingjoshdavid.funfection.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.data.AppSettingsRepository;
import com.kingjoshdavid.funfection.data.UserProfileRepository;
import com.kingjoshdavid.funfection.model.UserProfile;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText userNameInput = view.findViewById(R.id.settingsUserNameInput);
        Button saveUserNameButton = view.findViewById(R.id.settingsSaveUserNameButton);
        RadioGroup nightModeGroup = view.findViewById(R.id.settingsNightModeGroup);

        // Populate username
        userNameInput.setText(UserProfileRepository.getCurrentUser().getUserName());

        saveUserNameButton.setOnClickListener(v -> {
            UserProfile updated = UserProfileRepository.updateUserName(
                    userNameInput.getText().toString());
            userNameInput.setText(updated.getUserName());
            Toast.makeText(requireContext(),
                    getString(R.string.username_saved_toast, updated.getUserName()),
                    Toast.LENGTH_SHORT).show();
        });

        // Set current night mode selection
        switch (AppSettingsRepository.getNightMode()) {
            case LIGHT:
                nightModeGroup.check(R.id.settingsNightModeLight);
                break;
            case NIGHT:
                nightModeGroup.check(R.id.settingsNightModeNight);
                break;
            default:
                nightModeGroup.check(R.id.settingsNightModeSystem);
                break;
        }

        nightModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            AppSettingsRepository.NightMode selected;
            if (checkedId == R.id.settingsNightModeLight) {
                selected = AppSettingsRepository.NightMode.LIGHT;
            } else if (checkedId == R.id.settingsNightModeNight) {
                selected = AppSettingsRepository.NightMode.NIGHT;
            } else {
                selected = AppSettingsRepository.NightMode.SYSTEM;
            }
            AppSettingsRepository.setNightMode(selected);
            AppCompatDelegate.setDefaultNightMode(
                    AppSettingsRepository.toAppCompatNightMode(selected));
        });
    }
}
