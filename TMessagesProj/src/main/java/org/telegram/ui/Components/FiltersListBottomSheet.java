package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.FilterCreateActivity;

import java.util.ArrayList;

import tw.nekomimi.nekogram.folder.FolderIconHelper;

public class FiltersListBottomSheet extends BottomSheet implements NotificationCenter.NotificationCenterDelegate {

    private RecyclerListView listView;
    private ListAdapter adapter;
    private TextView titleTextView;
    private AnimatorSet shadowAnimation;
    private View shadow;

    private int scrollOffsetY;
    private boolean ignoreLayout;

    private FiltersListBottomSheetDelegate delegate;

    private ArrayList<MessagesController.DialogFilter> dialogFilters;

    private static final int SNAPSHOT_NEITHER = 0;
    private static final int SNAPSHOT_ALWAYS = 1;
    private static final int SNAPSHOT_NEVER = 2;

    private final ArrayList<byte[]> membershipSnapshot;
    private TextView resetButton;

    public interface FiltersListBottomSheetDelegate {
        void didSelectFilter(MessagesController.DialogFilter filter, boolean checked);
    }

    private final ArrayList<Long> selectedDialogs;
    private final BaseFragment fragment;

    public FiltersListBottomSheet(BaseFragment baseFragment, ArrayList<Long> selectedDialogs) {
        super(baseFragment.getParentActivity(), false);
        fixNavigationBar();
        this.selectedDialogs = selectedDialogs;
        this.fragment = baseFragment;
        dialogFilters = new ArrayList<>(baseFragment.getMessagesController().dialogFilters);
        for (int i = 0; i < dialogFilters.size(); ++i) {
            if (dialogFilters.get(i).isDefault()) {
                dialogFilters.remove(i);
                i--;
            }
        }
        membershipSnapshot = new ArrayList<>(dialogFilters.size());
        for (int i = 0; i < dialogFilters.size(); i++) {
            MessagesController.DialogFilter filter = dialogFilters.get(i);
            byte[] states = new byte[selectedDialogs.size()];
            for (int d = 0; d < selectedDialogs.size(); d++) {
                states[d] = snapshotState(filter, selectedDialogs.get(d));
            }
            membershipSnapshot.add(states);
        }
        Context context = baseFragment.getParentActivity();

        containerView = new FrameLayout(context) {

            private RectF rect = new RectF();
            private boolean fullHeight;

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                if (ev.getAction() == MotionEvent.ACTION_DOWN && scrollOffsetY != 0 && ev.getY() < scrollOffsetY) {
                    dismiss();
                    return true;
                }
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent e) {
                return !isDismissed() && super.onTouchEvent(e);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int height = MeasureSpec.getSize(heightMeasureSpec);
                ignoreLayout = true;
                setPadding(backgroundPaddingLeft, AndroidUtilities.statusBarHeight, backgroundPaddingLeft, 0);
                ignoreLayout = false;
                int contentSize = dp(48) + dp(48) * adapter.getItemCount() + backgroundPaddingTop + AndroidUtilities.statusBarHeight;
                int padding = contentSize < (height / 5 * 3.2) ? 0 : (height / 5 * 2);
                if (padding != 0 && contentSize < height) {
                    padding -= (height - contentSize);
                }
                if (padding == 0) {
                    padding = backgroundPaddingTop;
                }
                if (listView.getPaddingTop() != padding) {
                    ignoreLayout = true;
                    listView.setPadding(dp(10), padding, dp(10), 0);
                    ignoreLayout = false;
                }
                fullHeight = contentSize >= height;
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(Math.min(contentSize, height), MeasureSpec.EXACTLY));
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                updateLayout();
            }

            @Override
            public void requestLayout() {
                if (ignoreLayout) {
                    return;
                }
                super.requestLayout();
            }

            @Override
            protected void onDraw(Canvas canvas) {
                int top = scrollOffsetY - backgroundPaddingTop - dp(8);
                int height = getMeasuredHeight() + dp(36) + backgroundPaddingTop;
                int statusBarHeight = 0;
                float radProgress = 1.0f;
                top += AndroidUtilities.statusBarHeight;
                height -= AndroidUtilities.statusBarHeight;

                if (fullHeight) {
                    if (top + backgroundPaddingTop < AndroidUtilities.statusBarHeight * 2) {
                        int diff = Math.min(AndroidUtilities.statusBarHeight, AndroidUtilities.statusBarHeight * 2 - top - backgroundPaddingTop);
                        top -= diff;
                        height += diff;
                        radProgress = 1.0f - Math.min(1.0f, (diff * 2) / (float) AndroidUtilities.statusBarHeight);
                    }
                    if (top + backgroundPaddingTop < AndroidUtilities.statusBarHeight) {
                        statusBarHeight = Math.min(AndroidUtilities.statusBarHeight, AndroidUtilities.statusBarHeight - top - backgroundPaddingTop);
                    }
                }

                // On some devices(Pixel 9 ~) the status bar inset can be larger than the extra space we add for the
                // shadow drawable. In that case the drawable ends before the bottom of the sheet and the
                // last rows appear as if their background is "cut".
                if (height < getMeasuredHeight()) {
                    height = getMeasuredHeight();
                }

                shadowDrawable.setBounds(0, top, getMeasuredWidth(), getMeasuredHeight());
                shadowDrawable.draw(canvas);

                if (radProgress != 1.0f) {
                    Theme.dialogs_onlineCirclePaint.setColor(Theme.getColor(Theme.key_dialogBackground));
                    rect.set(backgroundPaddingLeft, backgroundPaddingTop + top, getMeasuredWidth() - backgroundPaddingLeft, backgroundPaddingTop + top + dp(24));
                    canvas.drawRoundRect(rect, dp(12) * radProgress, dp(12) * radProgress, Theme.dialogs_onlineCirclePaint);
                }

                if (statusBarHeight > 0) {
                    Theme.dialogs_onlineCirclePaint.setColor(Theme.getColor(Theme.key_dialogBackground));
                    canvas.drawRect(backgroundPaddingLeft, AndroidUtilities.statusBarHeight - statusBarHeight, getMeasuredWidth() - backgroundPaddingLeft, AndroidUtilities.statusBarHeight, Theme.dialogs_onlineCirclePaint);
                }
                updateLightStatusBar(statusBarHeight > AndroidUtilities.statusBarHeight / 2);
            }

            private Boolean statusBarOpen;
            private void updateLightStatusBar(boolean open) {
                if (statusBarOpen != null && statusBarOpen == open) {
                    return;
                }
                boolean openBgLight = AndroidUtilities.computePerceivedBrightness(getThemedColor(Theme.key_dialogBackground)) > .721f;
                boolean closedBgLight = AndroidUtilities.computePerceivedBrightness(Theme.blendOver(getThemedColor(Theme.key_actionBarDefault), 0x33000000)) > .721f;
                boolean isLight = (statusBarOpen = open) ? openBgLight : closedBgLight;
                AndroidUtilities.setLightStatusBar(getWindow(), isLight);
            }
        };
        containerView.setWillNotDraw(false);
        containerView.setPadding(backgroundPaddingLeft, 0, backgroundPaddingLeft, 0);

        FrameLayout.LayoutParams frameLayoutParams = new FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, AndroidUtilities.getShadowHeight(), Gravity.TOP | Gravity.LEFT);
        frameLayoutParams.topMargin = dp(48);
        shadow = new View(context);
        shadow.setBackgroundColor(Theme.getColor(Theme.key_dialogShadowLine));
        shadow.setAlpha(0.0f);
        shadow.setVisibility(View.INVISIBLE);
        shadow.setTag(1);
        containerView.addView(shadow, frameLayoutParams);

        listView = new RecyclerListView(context) {
            @Override
            public void requestLayout() {
                if (ignoreLayout) {
                    return;
                }
                super.requestLayout();
            }
        };
        listView.setTag(14);
        listView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(adapter = new ListAdapter(context));
        listView.setVerticalScrollBarEnabled(false);
        listView.setPadding(dp(10), 0, dp(10), 0);
        listView.setClipToPadding(false);
        listView.setGlowColor(Theme.getColor(Theme.key_dialogScrollGlow));
        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateLayout();
            }
        });
        listView.setOnItemClickListener((view, position) -> {
            delegate.didSelectFilter(adapter.getItem(position), view instanceof BottomSheet.BottomSheetCell ? ((BottomSheet.BottomSheetCell) view).isChecked() : false);
            if (position < dialogFilters.size()) {
                adapter.notifyItemChanged(position);
                resetButton.setVisibility(isDirty() ? View.VISIBLE : View.GONE);
            } else {
                dismiss();
            }
        });
        containerView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 0, 48, 0, 0));

        titleTextView = new TextView(context);
        titleTextView.setLines(1);
        titleTextView.setSingleLine(true);
        titleTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleTextView.setLinkTextColor(Theme.getColor(Theme.key_dialogTextLink));
        titleTextView.setHighlightColor(Theme.getColor(Theme.key_dialogLinkSelection));
        titleTextView.setEllipsize(TextUtils.TruncateAt.END);
        titleTextView.setPadding(dp(24), 0, dp(24), 0);
        titleTextView.setGravity(Gravity.CENTER_VERTICAL);
        titleTextView.setText(LocaleController.getString(R.string.FilterChoose));
        titleTextView.setTypeface(AndroidUtilities.bold());
        containerView.addView(titleTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 50, Gravity.LEFT | Gravity.TOP, 0, 0, 40, 0));

        resetButton = new TextView(context);
        resetButton.setLines(1);
        resetButton.setSingleLine(true);
        resetButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        resetButton.setTypeface(AndroidUtilities.bold());
        resetButton.setTextColor(Theme.getColor(Theme.key_dialogTextLink));
        resetButton.setPadding(dp(17), 0, dp(17), 0);
        resetButton.setGravity(Gravity.CENTER_VERTICAL);
        resetButton.setText(LocaleController.getString(R.string.FoldersReset));
        resetButton.setVisibility(View.GONE);
        resetButton.setOnClickListener(v -> revertChanges());
        containerView.addView(resetButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 50, Gravity.RIGHT | Gravity.TOP, 0, 0, 0, 0));

        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.emojiLoaded);
    }

    @Override
    protected boolean canDismissWithSwipe() {
        return false;
    }

    private void updateLayout() {
        if (listView.getChildCount() <= 0) {
            listView.setTopGlowOffset(scrollOffsetY = listView.getPaddingTop());
            titleTextView.setTranslationY(scrollOffsetY);
            shadow.setTranslationY(scrollOffsetY);
            containerView.invalidate();
            return;
        }
        View child = listView.getChildAt(0);
        RecyclerListView.Holder holder = (RecyclerListView.Holder) listView.findContainingViewHolder(child);
        int top = child.getTop();
        int newOffset = 0;
        if (top >= 0 && holder != null && holder.getAdapterPosition() == 0) {
            newOffset = top;
            runShadowAnimation(false);
        } else {
            runShadowAnimation(true);
        }
        if (scrollOffsetY != newOffset) {
            listView.setTopGlowOffset(scrollOffsetY = newOffset);
            titleTextView.setTranslationY(scrollOffsetY);
            shadow.setTranslationY(scrollOffsetY);
            containerView.invalidate();
        }
    }

    private void runShadowAnimation(final boolean show) {
        if (show && shadow.getTag() != null || !show && shadow.getTag() == null) {
            shadow.setTag(show ? null : 1);
            if (show) {
                shadow.setVisibility(View.VISIBLE);
            }
            if (shadowAnimation != null) {
                shadowAnimation.cancel();
            }
            shadowAnimation = new AnimatorSet();
            shadowAnimation.playTogether(ObjectAnimator.ofFloat(shadow, View.ALPHA, show ? 1.0f : 0.0f));
            shadowAnimation.setDuration(150);
            shadowAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (shadowAnimation != null && shadowAnimation.equals(animation)) {
                        if (!show) {
                            shadow.setVisibility(View.INVISIBLE);
                        }
                        shadowAnimation = null;
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    if (shadowAnimation != null && shadowAnimation.equals(animation)) {
                        shadowAnimation = null;
                    }
                }
            });
            shadowAnimation.start();
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.emojiLoaded);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.emojiLoaded) {
            AndroidUtilities.forEachViews(listView, view -> {
                if (view instanceof BottomSheetCell) {
                    ((BottomSheetCell) view).getTextView().invalidate();
                } else {
                    view.invalidate();
                }
            });
        }
    }

    public void setDelegate(FiltersListBottomSheetDelegate filtersListBottomSheetDelegate) {
        delegate = filtersListBottomSheetDelegate;
    }

    public static ArrayList<MessagesController.DialogFilter> getCanAddDialogFilters(BaseFragment fragment, Long dialogId) {
        var arrays = new ArrayList<Long>(1);
        arrays.add(dialogId);
        return getCanAddDialogFilters(fragment, arrays);
    }

    public static ArrayList<MessagesController.DialogFilter> getCanAddDialogFilters(BaseFragment fragment, ArrayList<Long> selectedDialogs) {
        ArrayList<MessagesController.DialogFilter> result = new ArrayList<>();
        ArrayList<MessagesController.DialogFilter> filters = fragment.getMessagesController().dialogFilters;
        for (int a = 0, N = filters.size(); a < N; a++) {
            MessagesController.DialogFilter filter = filters.get(a);
            if (!getDialogsCount(fragment, filter, selectedDialogs, true, true).isEmpty() && !filter.isDefault()) {
                result.add(filter);
            }
        }
        return result;
    }

    public static ArrayList<Long> getDialogsCount(BaseFragment fragment, MessagesController.DialogFilter filter, ArrayList<Long> selectedDialogs, boolean always, boolean check) {
        ArrayList<Long> dids = new ArrayList<>();
        for (int b = 0, N2 = selectedDialogs.size(); b < N2; b++) {
            long did = selectedDialogs.get(b);
            if (DialogObject.isEncryptedDialog(did)) {
                TLRPC.EncryptedChat encryptedChat = fragment.getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(did));
                if (encryptedChat != null) {
                    did = encryptedChat.user_id;
                    if (dids.contains(did)) {
                        continue;
                    }
                } else {
                    continue;
                }
            }
            if (filter != null && (always && filter.alwaysShow.contains(did) || !always && filter.neverShow.contains(did))) {
                continue;
            }
            dids.add(did);
            if (check) {
                break;
            }
        }
        return dids;
    }

    private long resolveDialogId(long rawDid) {
        if (DialogObject.isEncryptedDialog(rawDid)) {
            TLRPC.EncryptedChat encryptedChat = fragment.getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(rawDid));
            if (encryptedChat != null) {
                return encryptedChat.user_id;
            }
            return 0;
        }
        return rawDid;
    }

    private byte snapshotState(MessagesController.DialogFilter filter, long rawDid) {
        long dialogId = resolveDialogId(rawDid);
        if (filter.alwaysShow.contains(dialogId)) {
            return SNAPSHOT_ALWAYS;
        }
        if (filter.neverShow.contains(rawDid)) {
            return SNAPSHOT_NEVER;
        }
        return SNAPSHOT_NEITHER;
    }

    private boolean isDirty() {
        for (int i = 0; i < dialogFilters.size(); i++) {
            MessagesController.DialogFilter filter = dialogFilters.get(i);
            byte[] snapshot = membershipSnapshot.get(i);
            for (int d = 0; d < selectedDialogs.size(); d++) {
                if (snapshotState(filter, selectedDialogs.get(d)) != snapshot[d]) {
                    return true;
                }
            }
        }
        return false;
    }

    private void revertChanges() {
        boolean anyChanged = false;
        for (int i = 0; i < dialogFilters.size(); i++) {
            MessagesController.DialogFilter filter = dialogFilters.get(i);
            if (restoreFilterMembership(filter, membershipSnapshot.get(i))) {
                FilterCreateActivity.saveFilterToServer(filter, filter.flags, filter.emoticon, filter.name, filter.entities, filter.title_noanimate, filter.color, filter.alwaysShow, filter.neverShow, filter.pinnedDialogs, false, false, true, true, false, fragment, null);
                anyChanged = true;
            }
        }
        resetButton.setVisibility(isDirty() ? View.VISIBLE : View.GONE);
        if (anyChanged) {
            adapter.notifyDataSetChanged();
        }
    }

    private boolean restoreFilterMembership(MessagesController.DialogFilter filter, byte[] snapshot) {
        boolean changed = false;
        for (int d = 0; d < selectedDialogs.size(); d++) {
            long rawDid = selectedDialogs.get(d);
            long dialogId = resolveDialogId(rawDid);
            byte target = snapshot[d];
            boolean isInAlwaysShow = filter.alwaysShow.contains(dialogId);
            boolean isInNeverShow = filter.neverShow.contains(rawDid);
            if (target == SNAPSHOT_ALWAYS) {
                if (isInNeverShow) { filter.neverShow.remove(rawDid); changed = true; }
                if (!isInAlwaysShow) { filter.alwaysShow.add(dialogId); changed = true; }
            } else if (target == SNAPSHOT_NEVER) {
                if (isInAlwaysShow) { filter.alwaysShow.remove(dialogId); changed = true; }
                if (!isInNeverShow) { filter.neverShow.add(rawDid); changed = true; }
            } else {
                if (isInAlwaysShow) { filter.alwaysShow.remove(dialogId); changed = true; }
                if (isInNeverShow) { filter.neverShow.remove(rawDid); changed = true; }
            }
        }
        return changed;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context context;

        public ListAdapter(Context context) {
            this.context = context;
        }

        public MessagesController.DialogFilter getItem(int position) {
            if (position < dialogFilters.size()) {
                return dialogFilters.get(position);
            }
            return null;
        }

        @Override
        public int getItemCount() {
            int count = dialogFilters.size();
            if (count < 10) {
                count++;
            }
            return count;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            BottomSheet.BottomSheetCell cell = new BottomSheet.BottomSheetCell(context, 0);
            cell.setBackground(null);
            cell.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            BottomSheet.BottomSheetCell cell = (BottomSheet.BottomSheetCell) holder.itemView;
            if (position < dialogFilters.size()) {
                cell.getImageView().setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogIcon), PorterDuff.Mode.MULTIPLY));
                MessagesController.DialogFilter filter = dialogFilters.get(position);
                cell.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                CharSequence title = filter.name;
                title = Emoji.replaceEmoji(title, cell.getTextView().getPaint().getFontMetricsInt(), false);
                title = MessageObject.replaceAnimatedEmoji(title, filter.entities, cell.getTextView().getPaint().getFontMetricsInt());
                cell.setTextAndIcon(title, 0, new FolderDrawable(getContext(), FolderIconHelper.getTabIcon(filter.emoticon), filter.color), false);
                cell.getTextView().setEmojiColor(Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider));
                int inFolderCount = 0;
                for (int i = 0; i < selectedDialogs.size(); ++i) {
                    long did = selectedDialogs.get(i);
                    if (filter.includesDialog(AccountInstance.getInstance(currentAccount), did)) {
                        inFolderCount++;
                    }
                }
                boolean areAllInFolder = inFolderCount == selectedDialogs.size();
                cell.setIndeterminate(inFolderCount > 0 && !areAllInFolder);
                cell.setChecked(areAllInFolder);
            } else {
                cell.setIndeterminate(false);
                cell.setChecked(false);
                cell.getImageView().setColorFilter(null);
                Drawable drawable1 = context.getResources().getDrawable(R.drawable.poll_add_circle);
                Drawable drawable2 = context.getResources().getDrawable(R.drawable.poll_add_plus);
                drawable1.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_switchTrackChecked), PorterDuff.Mode.MULTIPLY));
                drawable2.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_checkboxCheck), PorterDuff.Mode.MULTIPLY));
                CombinedDrawable combinedDrawable = new CombinedDrawable(drawable1, drawable2);
                cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
                cell.setTextAndIcon(LocaleController.getString(R.string.CreateNewFilter), combinedDrawable);
            }
        }
    }
}
