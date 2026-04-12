package com.kingjoshdavid.funfection.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.data.FriendsRepository;
import com.kingjoshdavid.funfection.data.UserProfileRepository;
import com.kingjoshdavid.funfection.data.VirusRepository;
import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_COMBINE_VIRUS_ID =
            "com.kingjoshdavid.funfection.OPEN_COMBINE_VIRUS_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserProfileRepository.initialize(getApplicationContext());
        VirusRepository.initialize(getApplicationContext());
        FriendsRepository.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            showFragment(new CollectionFragment());
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = createFragmentForId(item.getItemId());
            showFragment(fragment);

            return true;
        });

        openPinnedCombineIfRequested(getIntent(), savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openPinnedCombineIfRequested(intent, null);
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();
    }

    @NotNull
    private Fragment createFragmentForId(int id) {
        if (id == R.id.tab_collection) return new CollectionFragment();
        if (id == R.id.tab_infect) return new InfectFragment();
        if (id == R.id.tab_friends) return new FriendsFragment();
        return new CollectionFragment();
    }

    private void openPinnedCombineIfRequested(Intent intent, Bundle savedInstanceState) {
        if (intent == null || savedInstanceState != null) {
            return;
        }
        String pinnedVirusId = intent.getStringExtra(EXTRA_OPEN_COMBINE_VIRUS_ID);
        if (pinnedVirusId == null || pinnedVirusId.trim().isEmpty()) {
            return;
        }
        // Show the collection view with the combine panel pre-opened for this virus.
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, CollectionFragment.newCombineInstance(pinnedVirusId))
                .commit();
        intent.removeExtra(EXTRA_OPEN_COMBINE_VIRUS_ID);
    }
}

