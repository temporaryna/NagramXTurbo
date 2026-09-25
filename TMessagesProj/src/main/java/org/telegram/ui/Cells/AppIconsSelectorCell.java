package org.telegram.ui.Cells;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.Easings;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Premium.PremiumFeatureBottomSheet;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.LauncherIconController;
import org.telegram.ui.PremiumPreviewFragment;
import tw.nekomimi.nekogram.helpers.AppRestartHelper;
import xyz.nextalone.nagram.NaConfig;

import java.util.ArrayList;
import java.util.List;

public class AppIconsSelectorCell extends LinearLayout implements NotificationCenter.NotificationCenterDelegate {
    public final static float ICONS_ROUND_RADIUS = 18;
    public final static float SELECTION_STROKE_INSET = 2;

    private final BaseFragment fragment;
    private final int currentAccount;
    private final List<IconStripListView> strips = new ArrayList<>();

    private void applyIconSoftly(LauncherIconController.LauncherIcon icon, boolean isCurrentIcon) {
        LauncherIconController.setIcon(icon, isCurrentIcon, false);
        NotificationsController.rebuildAllAccounts();
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.showBulletin, Bulletin.TYPE_APP_ICON, icon);
    }

    private void showAppRestartPopup(LauncherIconController.LauncherIcon icon, boolean isCurrentIcon) {
        if (fragment.getParentActivity() == null) {
            applyIconSoftly(icon, isCurrentIcon);
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setMessage(LocaleController.getString(R.string.AppIconRestartMessage));
        builder.setPositiveButton(LocaleController.getString(R.string.RestartApp), (dialogInterface, i) -> {
            LauncherIconController.setIcon(icon, isCurrentIcon, true);
            if (fragment.getParentActivity() != null) {
                AppRestartHelper.triggerRebirth(fragment.getParentActivity(), new Intent(fragment.getParentActivity(), LaunchActivity.class));
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Later), (dialogInterface, i) -> applyIconSoftly(icon, isCurrentIcon));
        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(di -> applyIconSoftly(icon, isCurrentIcon));
        fragment.showDialog(dialog);
    }

    public AppIconsSelectorCell(Context context, BaseFragment fragment, int currentAccount) {
        super(context);
        this.fragment = fragment;
        this.currentAccount = currentAccount;

        setOrientation(VERTICAL);
        setFocusable(false);

        addStripSection(context, R.string.AppIconSectionStandard);
        addStripSection(context, R.string.AppIconSectionTurbo);
        addStripSection(context, R.string.AppIconSectionHidden);

        updateIconsVisibility();
    }

    private IconStripListView addStripSection(Context context, int titleRes) {
        TextView titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        titleView.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(8), AndroidUtilities.dp(18), 0);
        addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        titleView.setText(LocaleController.getString(titleRes));

        IconStripListView strip = new IconStripListView(context, fragment);
        strips.add(strip);
        addView(strip, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return strip;
    }

    public void scrollIconsToEnd() {
        for (int i = strips.size() - 1; i >= 0; i--) {
            IconStripListView strip = strips.get(i);
            if (strip.getVisibility() == VISIBLE && !strip.icons.isEmpty()) {
                strip.scrollToEnd();
                break;
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateIconsVisibility() {
        boolean easterEggUnlocked = NaConfig.INSTANCE.getEasterEggUnlocked().Bool();
        boolean isPremiumBlocked = MessagesController.getInstance(currentAccount).premiumFeaturesBlocked();

        List<LauncherIconController.LauncherIcon> standardIcons = new ArrayList<>();
        List<LauncherIconController.LauncherIcon> turboIcons = new ArrayList<>();
        List<LauncherIconController.LauncherIcon> hiddenIcons = new ArrayList<>();
        for (LauncherIconController.LauncherIcon icon : LauncherIconController.LauncherIcon.values()) {
            if (isPremiumBlocked && icon.premium) {
                continue;
            }
            if (icon.isHidden) {
                if (easterEggUnlocked) {
                    hiddenIcons.add(icon);
                }
            } else if (icon.modernKey != null) {
                standardIcons.add(icon);
            } else {
                turboIcons.add(icon);
            }
        }

        strips.get(0).setIcons(standardIcons);
        strips.get(1).setIcons(turboIcons);
        strips.get(2).setIcons(hiddenIcons);

        for (int i = 0; i < strips.size(); i++) {
            boolean hasIcons = !strips.get(i).icons.isEmpty();
            strips.get(i).setVisibility(hasIcons ? VISIBLE : GONE);
            View titleView = getChildAt(i * 2);
            titleView.setVisibility(hasIcons ? VISIBLE : GONE);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void notifyIconsChanged() {
        for (int i = 0; i < strips.size(); i++) {
            strips.get(i).getAdapter().notifyDataSetChanged();
        }
    }

    void onIconPicked() {
        notifyIconsChanged();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.premiumStatusChangedGlobal);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.premiumStatusChangedGlobal);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.premiumStatusChangedGlobal) {
            updateIconsVisibility();
        }
    }

    private class IconStripListView extends RecyclerListView {
        private final BaseFragment fragment;
        private final List<LauncherIconController.LauncherIcon> icons = new ArrayList<>();
        private LinearLayoutManager linearLayoutManager;

        public IconStripListView(Context context, BaseFragment fragment) {
            super(context);
            this.fragment = fragment;

            setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(12));
            setFocusable(false);
            setItemAnimator(null);
            setLayoutAnimation(null);
            setClipToPadding(false);

            setLayoutManager(linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            setAdapter(new Adapter() {

                @NonNull
                @Override
                public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    return new RecyclerListView.Holder(new IconHolderView(parent.getContext()));
                }

                @Override
                public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                    IconHolderView holderView = (IconHolderView) holder.itemView;
                    LauncherIconController.LauncherIcon icon = icons.get(position);
                    holderView.bind(icon);
                    holderView.iconView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(ICONS_ROUND_RADIUS), Color.TRANSPARENT, Theme.getColor(Theme.key_listSelector), Color.BLACK));
                    holderView.iconView.setForeground(icon.modernKey != null && NaConfig.INSTANCE.getModernClassicIcons().Bool() ? icon.modernForeground : icon.foreground);
                }

                @Override
                public int getItemCount() {
                    return icons.size();
                }
            });
            addItemDecoration(new ItemDecoration() {
                @Override
                public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull State state) {
                    int pos = parent.getChildViewHolder(view).getAdapterPosition();
                    if (pos == 0) {
                        outRect.left = AndroidUtilities.dp(18);
                    }
                    if (pos == getAdapter().getItemCount() - 1) {
                        outRect.right = AndroidUtilities.dp(18);
                    } else {
                        int itemCount = getAdapter().getItemCount();
                        if (itemCount == 4) {
                            outRect.right = (getWidth() - AndroidUtilities.dp(36) - AndroidUtilities.dp(58) * itemCount) / (itemCount - 1);
                        } else {
                            outRect.right = AndroidUtilities.dp(24);
                        }
                    }
                }
            });
            setOnItemClickListener((view, position) -> {
                IconHolderView holderView = (IconHolderView) view;
                LauncherIconController.LauncherIcon icon = icons.get(position);
                if (icon.premium && !UserConfig.hasPremiumOnAccounts()) {
                    fragment.showDialog(new PremiumFeatureBottomSheet(fragment, PremiumPreviewFragment.PREMIUM_FEATURE_APPLICATION_ICONS, true));
                    return;
                }

                boolean isCurrentIcon = LauncherIconController.isEnabled(icon);

                LinearSmoothScroller smoothScroller = new LinearSmoothScroller(context) {
                    @Override
                    public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                        return boxStart - viewStart + AndroidUtilities.dp(16);
                    }

                    @Override
                    protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                        return super.calculateSpeedPerPixel(displayMetrics) * 3f;
                    }
                };
                smoothScroller.setTargetPosition(position);
                linearLayoutManager.startSmoothScroll(smoothScroller);

                onIconPicked();
                holderView.setSelected(true, true);

                for (int i = 0; i < getChildCount(); i++) {
                    IconHolderView otherView = (IconHolderView) getChildAt(i);
                    if (otherView != holderView) {
                        otherView.setSelected(false, true);
                    }
                }

                showAppRestartPopup(icon, isCurrentIcon);
            });
        }

        @SuppressLint("NotifyDataSetChanged")
        void setIcons(List<LauncherIconController.LauncherIcon> newIcons) {
            icons.clear();
            icons.addAll(newIcons);
            getAdapter().notifyDataSetChanged();
            invalidateItemDecorations();

            for (int i = 0; i < icons.size(); i++) {
                LauncherIconController.LauncherIcon icon = icons.get(i);
                if (LauncherIconController.isEnabled(icon)) {
                    linearLayoutManager.scrollToPositionWithOffset(i, AndroidUtilities.dp(16));
                    break;
                }
            }
        }

        void scrollToEnd() {
            if (!icons.isEmpty()) {
                linearLayoutManager.scrollToPositionWithOffset(icons.size() - 1, AndroidUtilities.dp(16));
            }
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthSpec), MeasureSpec.EXACTLY), heightSpec);
        }
    }

    private final static class IconHolderView extends LinearLayout {
        private Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean isClassicMedallion;

        private AdaptiveIconImageView iconView;
        private TextView titleView;

        private float progress;

        private IconHolderView(@NonNull Context context) {
            super(context);

            setOrientation(VERTICAL);

            setWillNotDraw(false);
            iconView = new AdaptiveIconImageView(context);
            iconView.setBackgroundOuterPadding(AndroidUtilities.dp(29));
            addView(iconView, LayoutHelper.createLinear(58, 58, Gravity.CENTER_HORIZONTAL, 4, 4, 4, 0));

            titleView = new TextView(context);
            titleView.setSingleLine();
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));

            outlinePaint.setStyle(Paint.Style.STROKE);
            outlinePaint.setStrokeWidth(Math.max(2, AndroidUtilities.dp(0.5f)));

            fillPaint.setColor(Color.WHITE);
        }

        @Override
        public void draw(Canvas canvas) {
            float stroke = outlinePaint.getStrokeWidth();
            AndroidUtilities.rectTmp.set(iconView.getLeft(), iconView.getTop(), iconView.getRight(), iconView.getBottom());
            if (isClassicMedallion) {
                canvas.drawRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(ICONS_ROUND_RADIUS), AndroidUtilities.dp(ICONS_ROUND_RADIUS), fillPaint);
            }

            super.draw(canvas);

            float inset = AndroidUtilities.dp(SELECTION_STROKE_INSET);
            // stroke radius must grow by the inset to stay concentric with the clipped background
            float strokeRadius = AndroidUtilities.dp(ICONS_ROUND_RADIUS + SELECTION_STROKE_INSET);
            AndroidUtilities.rectTmp.set(iconView.getLeft() - inset, iconView.getTop() - inset, iconView.getRight() + inset, iconView.getBottom() + inset);
            canvas.drawRoundRect(AndroidUtilities.rectTmp, strokeRadius, strokeRadius, outlinePaint);
        }

        private void setProgress(float progress) {
            this.progress = progress;

            titleView.setTextColor(ColorUtils.blendARGB(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), Theme.getColor(Theme.key_windowBackgroundWhiteValueText), progress));
            outlinePaint.setColor(ColorUtils.blendARGB(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_switchTrack), 0x3F), Theme.getColor(Theme.key_windowBackgroundWhiteValueText), progress));
            outlinePaint.setStrokeWidth(Math.max(2, AndroidUtilities.lerp(1.5f, 3f, progress)));
            invalidate();
        }

        private void setSelected(boolean selected, boolean animate) {
            float to = selected ? 1 : 0;
            if (to == progress && animate) {
                return;
            }

            if (animate) {
                ValueAnimator animator = ValueAnimator.ofFloat(progress, to).setDuration(250);
                animator.setInterpolator(Easings.easeInOutQuad);
                animator.addUpdateListener(animation -> setProgress((Float) animation.getAnimatedValue()));
                animator.start();
            } else {
                setProgress(to);
            }
        }

        private void bind(LauncherIconController.LauncherIcon icon) {
            boolean modern = icon.modernKey != null && NaConfig.INSTANCE.getModernClassicIcons().Bool();
            iconView.setImageResource(modern ? icon.modernBackground : icon.background);
            boolean classicMedallion = icon.modernKey != null && !modern;
            if (classicMedallion != isClassicMedallion) {
                isClassicMedallion = classicMedallion;
                iconView.setCircleClip(classicMedallion);
            }

            MarginLayoutParams params = (MarginLayoutParams) titleView.getLayoutParams();
            if (icon.premium && !UserConfig.hasPremiumOnAccounts()) {
                SpannableString str = new SpannableString("d " + LocaleController.getString(icon.title));
                ColoredImageSpan span = new ColoredImageSpan(R.drawable.msg_mini_premiumlock);
                span.setTopOffset(1);
                span.setSize(AndroidUtilities.dp(13));
                str.setSpan(span, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                params.rightMargin = AndroidUtilities.dp(4);
                titleView.setText(str);
            } else {
                params.rightMargin = 0;
                titleView.setText(LocaleController.getString(icon.title));
            }
            setSelected(LauncherIconController.isEnabled(icon), false);
        }
    }

    public static class AdaptiveIconImageView extends ImageView {
        private Drawable foreground;
        private Path path = new Path();
        private boolean isCircleClip;
        private int backgroundOuterPadding = AndroidUtilities.dp(42);

        public AdaptiveIconImageView(Context context) {
            super(context);
        }

        public void setForeground(int res) {
            foreground = ContextCompat.getDrawable(getContext(), res);
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            updatePath();
        }

        public void setPadding(int padding) {
            setPadding(padding, padding, padding, padding);
        }

        public void setBackgroundOuterPadding(int backgroundOuterPadding) {
            this.backgroundOuterPadding = backgroundOuterPadding;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.save();
            canvas.clipPath(path);
            // one shared zoom into the launcher-visible zone for both layers, so the picker matches the homescreen
            float scale = 1f + backgroundOuterPadding / (float) Math.max(1, getWidth());
            canvas.scale(scale, scale, getWidth() / 2f, getHeight() / 2f);
            super.draw(canvas);
            if (foreground != null) {
                foreground.setBounds(0, 0, getWidth(), getHeight());
                foreground.draw(canvas);
            }
            canvas.restore();
        }

        public void setCircleClip(boolean circleClip) {
            isCircleClip = circleClip;
            updatePath();
            invalidate();
        }

        private void updatePath() {
            path.rewind();
            if (isCircleClip) {
                path.addCircle(getWidth() / 2f, getHeight() / 2f, Math.min(getWidth(), getHeight()) / 2f - AndroidUtilities.dp(8), Path.Direction.CW);
            } else {
                float radius = AndroidUtilities.dp(AppIconsSelectorCell.ICONS_ROUND_RADIUS);
                path.addRoundRect(0, 0, getWidth(), getHeight(), radius, radius, Path.Direction.CW);
            }
        }
    }
}
