package com.kingjoshdavid.funfection.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kingjoshdavid.funfection.R;
import com.kingjoshdavid.funfection.data.FriendsRepository;
import com.kingjoshdavid.funfection.model.Friend;

import java.util.List;

public class FriendsFragment extends Fragment {

    private ListView vectorsList;
    private TextView emptyText;
    private TextView loadingText;
    private VectorListAdapter vectorAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vectorsList = view.findViewById(R.id.vectorsList);
        emptyText = view.findViewById(R.id.vectorsEmptyText);
        loadingText = view.findViewById(R.id.vectorsLoadingText);

        vectorAdapter = new VectorListAdapter(requireContext());
        vectorsList.setAdapter(vectorAdapter);
        vectorsList.setOnItemClickListener((parent, itemView, position, id) -> {
            Friend friend = vectorAdapter.getItem(position);
            Intent intent = new Intent(requireContext(), FriendDetailsActivity.class);
            intent.putExtra(FriendDetailsActivity.EXTRA_FRIEND_ID, friend.getId());
            startActivity(intent);
        });

        loadVectors();
    }

    private void loadVectors() {
        showLoading();
        FriendsRepository.getFriendsAsync(this::onVectorsLoaded);
    }

    private void onVectorsLoaded(List<Friend> vectors) {
        vectorAdapter.setVectors(vectors);
        if (vectors == null || vectors.isEmpty()) {
            showEmpty();
        } else {
            showList();
        }
    }

    private void showLoading() {
        loadingText.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);
        vectorsList.setVisibility(View.GONE);
    }

    private void showEmpty() {
        loadingText.setVisibility(View.GONE);
        emptyText.setVisibility(View.VISIBLE);
        vectorsList.setVisibility(View.GONE);
    }

    private void showList() {
        loadingText.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);
        vectorsList.setVisibility(View.VISIBLE);
    }
}

