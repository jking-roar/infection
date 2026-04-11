package com.kingjoshdavid.funfection.ui;

import android.view.View;
import android.widget.Button;
import com.kingjoshdavid.funfection.R;

public final class VirusActionPanelBinder {

    public interface Callbacks {
        void onViewDetails();
        void onShareText();
        void onShareQr();
        void onPurge();
        void onCombine();
        void onBackToLab();
    }

    private VirusActionPanelBinder() {
    }

    public static void bind(View root,
                            boolean showViewDetails,
                            boolean showBackToLab,
                            Callbacks callbacks) {
        if (root == null || callbacks == null) {
            return;
        }
        root.setVisibility(View.VISIBLE);
        configureButton(root.findViewById(R.id.virusActionViewDetails), showViewDetails,
                v -> callbacks.onViewDetails());
        configureButton(root.findViewById(R.id.virusActionShareText), true,
                v -> callbacks.onShareText());
        configureButton(root.findViewById(R.id.virusActionShareQr), true,
                v -> callbacks.onShareQr());
        configureButton(root.findViewById(R.id.virusActionPurge), true,
                v -> callbacks.onPurge());
        configureButton(root.findViewById(R.id.virusActionCombine), true,
                v -> callbacks.onCombine());
        configureButton(root.findViewById(R.id.virusActionBackToLab), showBackToLab,
                v -> callbacks.onBackToLab());
    }

    private static void configureButton(Button button,
                                        boolean visible,
                                        View.OnClickListener onClickListener) {
        if (button == null) {
            return;
        }
        button.setVisibility(visible ? View.VISIBLE : View.GONE);
        button.setOnClickListener(visible ? onClickListener : null);
    }
}

