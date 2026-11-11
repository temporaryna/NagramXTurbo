package org.telegram.ui.Components;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.Theme;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

import tw.nekomimi.nekogram.helpers.HiddenChatsHelper;

public class HiddenChatsPasscodeDialog extends Dialog {

    private static final int PIN_LENGTH = 4;
    private static final int MAX_TRIES = 5;
    private static final long RETRY_COOLDOWN_MS = 30_000;

    private final int account;
    private final String subtitle;
    private final String confirmSubtitle;
    private final boolean pinOnly;
    private final boolean setupMode;
    private final Runnable onSuccess;
    private final Consumer<String> onPinSet;
    private final TextView subtitleView;
    private final TextView errorView;
    private final View[] dots = new View[PIN_LENGTH];
    private final StringBuilder entered = new StringBuilder();
    private final LinearLayout contentView;
    private LinearLayout dotsRow;
    private String firstPin;
    private TextView fingerprintView;

    public HiddenChatsPasscodeDialog(@NonNull Context context, String subtitle, Runnable onSuccess) {
        this(context, subtitle, null, null, onSuccess, false);
    }

    public HiddenChatsPasscodeDialog(@NonNull Context context, String subtitle, Consumer<String> onPinSet) {
        this(context, subtitle, null, onPinSet, null, false);
    }

    public HiddenChatsPasscodeDialog(@NonNull Context context, String subtitle, String confirmSubtitle, Consumer<String> onPinSet) {
        this(context, subtitle, confirmSubtitle, onPinSet, null, false);
    }

    public HiddenChatsPasscodeDialog(@NonNull Context context, String subtitle, Runnable onSuccess, boolean pinOnly) {
        this(context, subtitle, null, null, onSuccess, pinOnly);
    }

    private HiddenChatsPasscodeDialog(@NonNull Context context, String subtitle, String confirmSubtitle, Consumer<String> onPinSet, Runnable onSuccess, boolean pinOnly) {
        super(context, R.style.TransparentDialog);
        this.account = UserConfig.selectedAccount;
        this.subtitle = subtitle;
        this.setupMode = onPinSet != null;
        this.pinOnly = pinOnly;
        this.confirmSubtitle = confirmSubtitle != null ? confirmSubtitle : LocaleController.getString(R.string.HiddenChatsConfirmPassword);
        this.onPinSet = onPinSet;
        this.onSuccess = onSuccess;

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        contentView = new LinearLayout(context);
        contentView.setOrientation(LinearLayout.VERTICAL);
        contentView.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView titleView = new TextView(context);
        titleView.setText(LocaleController.getString(R.string.HiddenChatsTitle));
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setGravity(Gravity.CENTER);
        contentView.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        subtitleView = new TextView(context);
        subtitleView.setText(subtitle);
        subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitleView.setGravity(Gravity.CENTER);
        contentView.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 24));

        dotsRow = new LinearLayout(context);
        dotsRow.setOrientation(LinearLayout.HORIZONTAL);
        dotsRow.setGravity(Gravity.CENTER);
        for (int a = 0; a < PIN_LENGTH; a++) {
            View dot = new View(context);
            dot.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)));
            dotsRow.addView(dot, LayoutHelper.createLinear(16, 16, Gravity.CENTER, 9, 0, 9, 0));
            dots[a] = dot;
        }
        contentView.addView(dotsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        errorView = new TextView(context);
        errorView.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        errorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        errorView.setGravity(Gravity.CENTER);
        errorView.setMinHeight(AndroidUtilities.dp(18));
        contentView.addView(errorView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        GridLayout numpad = new GridLayout(context);
        numpad.setColumnCount(3);
        for (int digit = 1; digit <= 9; digit++) {
            numpad.addView(createKey(numpad, String.valueOf(digit)));
        }
        numpad.addView(createEmptyKey());
        numpad.addView(createKey(numpad, "0"));
        numpad.addView(createKey(numpad, "⌫"));
        contentView.addView(numpad, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 8, 0, 0));

        if (!setupMode) {
            fingerprintView = new TextView(context);
            fingerprintView.setText(LocaleController.getString(R.string.HiddenChatsFingerprint));
            fingerprintView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            fingerprintView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            fingerprintView.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
            fingerprintView.setOnClickListener(v -> checkFingerprint());
            fingerprintView.setVisibility(View.GONE);
            contentView.addView(fingerprintView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 8, 0, 0));
            if (!pinOnly && HiddenChatsHelper.isFingerprintEnabled(account)) {
                try {
                    if (BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                        fingerprintView.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        }

        contentView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(48), AndroidUtilities.dp(24), AndroidUtilities.dp(24));
        root.addView(contentView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 0));
        setContentView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private TextView createKey(GridLayout numpad, String label) {
        TextView key = new TextView(numpad.getContext());
        key.setText(label);
        key.setGravity(Gravity.CENTER);
        key.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        key.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        Drawable background = Theme.createRoundRectDrawable(AndroidUtilities.dp(28), Theme.getColor(Theme.key_windowBackgroundGray));
        key.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(Theme.getColor(Theme.key_listSelector)),
                background,
                Theme.createRoundRectDrawable(AndroidUtilities.dp(28), 0xff000000)));
        key.setOnClickListener(v -> onKey(label));
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = AndroidUtilities.dp(72);
        params.height = AndroidUtilities.dp(56);
        params.setMargins(AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6));
        key.setLayoutParams(params);
        return key;
    }

    private View createEmptyKey() {
        View view = new View(getContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = AndroidUtilities.dp(72);
        params.height = AndroidUtilities.dp(56);
        params.setMargins(AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6));
        view.setLayoutParams(params);
        return view;
    }

    private void onKey(String label) {
        if ("⌫".equals(label)) {
            if (entered.length() > 0) {
                entered.deleteCharAt(entered.length() - 1);
                updateDots();
            }
            return;
        }
        if (lockedUntil() > System.currentTimeMillis()) {
            long secondsLeft = (lockedUntil() - System.currentTimeMillis() + 999) / 1000;
            errorView.setText(LocaleController.formatString(R.string.HiddenChatsLockedWait, (int) secondsLeft));
            return;
        }
        if (entered.length() >= PIN_LENGTH) {
            return;
        }
        entered.append(label);
        updateDots();
        if (entered.length() == PIN_LENGTH) {
            contentView.postDelayed(this::check, 120);
        }
    }

    private void check() {
        String pin = entered.toString();
        if (setupMode) {
            if (firstPin == null) {
                firstPin = pin;
                entered.setLength(0);
                updateDots();
                subtitleView.setText(confirmSubtitle);
                return;
            }
            if (firstPin.equals(pin)) {
                HiddenChatsHelper.setPinForAccount(pin, account);
                HiddenChatsHelper.markUnlocked(account);
                dismiss();
                if (onPinSet != null) {
                    onPinSet.accept(pin);
                }
                return;
            }
            firstPin = null;
            subtitleView.setText(subtitle);
            showWrongPassword();
            return;
        }
        if (HiddenChatsHelper.checkPin(pin, account)) {
            succeeded();
            return;
        }
        setFailedTries(failedTries() + 1);
        if (failedTries() >= MAX_TRIES) {
            HiddenChatsHelper.setLockedUntil(account, System.currentTimeMillis() + (RETRY_COOLDOWN_MS << Math.min(failedTries() - MAX_TRIES, 4)));
        }
        showWrongPassword();
    }

    private void showWrongPassword() {
        errorView.setText(LocaleController.getString(R.string.HiddenChatsWrongPassword));
        entered.setLength(0);
        updateDots();
        shakeDots();
    }

    private void succeeded() {
        setFailedTries(0);
        entered.setLength(0);
        dismiss();
        if (onSuccess != null) {
            onSuccess.run();
        }
    }

    private void updateDots() {
        for (int a = 0; a < PIN_LENGTH; a++) {
            boolean filled = a < entered.length();
            View dot = dots[a];
            if (dot.getTag() != null && (boolean) dot.getTag() == filled) {
                continue;
            }
            dot.setTag(filled);
            dot.animate().cancel();
            dot.getBackground().mutate().setAlpha(filled ? 255 : 90);
            if (filled) {
                dot.setScaleX(0.4f);
                dot.setScaleY(0.4f);
                dot.animate().scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(new android.view.animation.OvershootInterpolator(2f)).start();
            } else {
                dot.setScaleX(1f);
                dot.setScaleY(1f);
            }
        }
    }

    private void shakeDots() {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(dotsRow, View.TRANSLATION_X, 0, -AndroidUtilities.dp(12), AndroidUtilities.dp(12), -AndroidUtilities.dp(8), AndroidUtilities.dp(8), -AndroidUtilities.dp(4), AndroidUtilities.dp(4), 0)
        );
        set.setDuration(420);
        set.start();
    }

    private int failedTries() {
        return HiddenChatsHelper.getFailedTries(account);
    }

    private void setFailedTries(int value) {
        HiddenChatsHelper.setFailedTries(account, value);
    }

    private long lockedUntil() {
        return HiddenChatsHelper.getLockedUntil(account);
    }

    private void checkFingerprint() {
        try {
            Activity activity = AndroidUtilities.findActivity(getContext());
            if (activity == null) {
                return;
            }
            Executor executor = ContextCompat.getMainExecutor(getContext());
            androidx.fragment.app.FragmentActivity fragmentActivity = (androidx.fragment.app.FragmentActivity) activity;
            BiometricPrompt prompt = new BiometricPrompt(fragmentActivity, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errMsgId, @NonNull CharSequence errString) {
                    FileLog.d("HiddenChatsPasscodeDialog onAuthenticationError " + errMsgId);
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    succeeded();
                }

                @Override
                public void onAuthenticationFailed() {
                    FileLog.d("HiddenChatsPasscodeDialog onAuthenticationFailed");
                }
            });
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(LocaleController.getString(R.string.HiddenChatsTitle))
                    .setNegativeButtonText(LocaleController.getString(R.string.HiddenChatsUsePassword))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build();
            prompt.authenticate(promptInfo);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setWindowAnimations(R.style.DialogNoAnimation);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.FILL;
        params.dimAmount = 0;
        params.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        params.flags |= WindowManager.LayoutParams.FLAG_SECURE;
        params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        window.setAttributes(params);
        if (fingerprintView != null && fingerprintView.getVisibility() == View.VISIBLE) {
            contentView.postDelayed(this::checkFingerprint, 250);
        }
    }

    @Override
    public void onBackPressed() {
        dismiss();
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                onKey(String.valueOf(keyCode - KeyEvent.KEYCODE_0));
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                onKey("⌫");
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                onBackPressed();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
