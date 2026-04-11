package com.kingjoshdavid.funfection.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.data.UserProfileRepository;
import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserProfileRepository.initialize(getApplicationContext());
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
}

