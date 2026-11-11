package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.HiddenChatsAliasEditorDialog;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashMap;

import org.telegram.messenger.NotificationCenter;

import tw.nekomimi.nekogram.helpers.HiddenChatsHelper;

public class HiddenChatsListActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ListAdapter adapter;
    private RecyclerListView listView;
    private EmptyTextProgressView emptyView;
    private ActionBarMenuItem searchItem;
    private String searchQuery;
    private final ArrayList<Long> allDialogIds = new ArrayList<>();
    private final ArrayList<Long> visibleDialogIds = new ArrayList<>();

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.hiddenChatsLocked);
        loadDialogs();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.hiddenChatsLocked);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.dialogsNeedReload && account == currentAccount) {
            loadDialogs();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        } else if (id == NotificationCenter.hiddenChatsLocked && account == currentAccount && !HiddenChatsHelper.isUnlocked(account)) {
            finishFragment();
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.HiddenChatsList));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        searchItem = actionBar.createMenu().addItem(0, R.drawable.outline_header_search).setIsSearchField(true);
        searchItem.setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onTextChanged(EditText editText) {
                searchQuery = editText.getText().toString().trim().toLowerCase();
                filterDialogs();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        TextView bottomHint = new TextView(context);
        bottomHint.setText(LocaleController.getString(R.string.HiddenChatsManageHint));
        bottomHint.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        bottomHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        bottomHint.setGravity(Gravity.CENTER);
        bottomHint.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(10), AndroidUtilities.dp(24), AndroidUtilities.dp(14));
        bottomHint.setBackground(Theme.getThemedDrawable(context, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
        bottomHint.setLines(2);
        frameLayout.addView(bottomHint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));

        emptyView = new EmptyTextProgressView(context);
        emptyView.setText(LocaleController.getString(R.string.HiddenChatsManageEmpty));
        emptyView.showTextView();
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setEmptyView(emptyView);
        listView.setPadding(0, 0, 0, AndroidUtilities.dp(88));
        listView.setClipToPadding(false);
        listView.setAdapter(adapter = new ListAdapter(context));
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position < 0 || position >= visibleDialogIds.size()) {
                return;
            }
            if (view instanceof HiddenChatCell && ((HiddenChatCell) view).hitDeleteZone(x)) {
                return;
            }
            showAliasEditor(visibleDialogIds.get(position), HiddenChatsHelper.aliasMapFor(UserConfig.selectedAccount).get(visibleDialogIds.get(position)));
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        fragmentView = frameLayout;
        return fragmentView;
    }

    private void loadDialogs() {
        allDialogIds.clear();
        allDialogIds.addAll(HiddenChatsHelper.getHiddenIds(UserConfig.selectedAccount));
        MessagesController messagesController = MessagesController.getInstance(currentAccount);
        HashMap<Long, Long> dates = new HashMap<>();
        for (long dialogId : allDialogIds) {
            TLRPC.Dialog dialog = messagesController.dialogs_dict.get(dialogId);
            dates.put(dialogId, dialog == null ? 0L : dialog.last_message_date);
        }
        allDialogIds.sort((id1, id2) -> Long.compare(dates.get(id2), dates.get(id1)));
        filterDialogs();
    }

    private void filterDialogs() {
        visibleDialogIds.clear();
        for (long dialogId : allDialogIds) {
            if (TextUtils.isEmpty(searchQuery) || HiddenChatsHelper.getDialogTitle(dialogId).toLowerCase().contains(searchQuery)) {
                visibleDialogIds.add(dialogId);
            }
        }
    }

    private void showUnhidePopup(long dialogId) {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(HiddenChatsHelper.getDialogTitle(dialogId));
        builder.setMessage(LocaleController.getString(R.string.HiddenChatsUnhideConfirm));
        builder.setPositiveButton(LocaleController.getString(R.string.HiddenChatsUnhideChat), (alertDialog, which) -> {
            HiddenChatsHelper.setHidden(dialogId, false);
            HiddenChatsHelper.removeAliasFor(dialogId, UserConfig.selectedAccount);
            loadDialogs();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private void showAliasEditor(long dialogId, String preset) {
        HiddenChatsAliasEditorDialog.show(this, LocaleController.formatString(R.string.HiddenChatsEditAlias, HiddenChatsHelper.getDialogTitle(dialogId)), preset, value -> {
            if (value.isEmpty()) {
                HiddenChatsHelper.removeAliasFor(dialogId, UserConfig.selectedAccount);
            } else {
                HiddenChatsHelper.setAliasFor(dialogId, value, UserConfig.selectedAccount);
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
    }

    private class HiddenChatCell extends FrameLayout {

        private final BackupImageView avatarView = new BackupImageView(getContext());
        private final AvatarDrawable avatarDrawable = new AvatarDrawable();
        private final TextView titleView = new TextView(getContext());
        private final TextView subtitleView = new TextView(getContext());
        private final TextView deleteView = new TextView(getContext());
        private final View separator = new View(getContext());
        private long boundDialogId;

        HiddenChatCell(Context context) {
            super(context);
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            separator.setBackgroundColor(Theme.getColor(Theme.key_divider));
            addView(separator, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 1, Gravity.BOTTOM));

            avatarView.setRoundRadius(AndroidUtilities.dp(21));
            addView(avatarView, LayoutHelper.createFrame(42, 42, Gravity.CENTER_VERTICAL, 12, 0, 0, 0));

            LinearLayout texts = new LinearLayout(context);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setGravity(Gravity.CENTER_VERTICAL);

            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            titleView.setSingleLine(true);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            subtitleView.setSingleLine(true);
            subtitleView.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            addView(texts, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 68, 0, 60, 0));

            deleteView.setText("✕");
            deleteView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            deleteView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            deleteView.setGravity(Gravity.CENTER);
            deleteView.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_CIRCLE_TO_BOUND_EDGE));
            deleteView.setOnClickListener(v -> {
                if (boundDialogId != 0) {
                    showUnhidePopup(boundDialogId);
                }
            });
            addView(deleteView, LayoutHelper.createFrame(40, 40, Gravity.CENTER_VERTICAL | Gravity.RIGHT, 0, 0, 9, 0));
        }

        boolean hitDeleteZone(float x) {
            return x >= getWidth() - AndroidUtilities.dp(49);
        }

        void bind(long dialogId, boolean showSeparator) {
            boundDialogId = dialogId;
            String alias = HiddenChatsHelper.aliasMapFor(UserConfig.selectedAccount).get(dialogId);
            String title = HiddenChatsHelper.getDialogTitle(dialogId);

            TLRPC.User user = getMessagesController().getUser(dialogId);
            TLRPC.Chat chat = user == null && dialogId < 0 ? getMessagesController().getChat(-dialogId) : null;
            if (user != null) {
                avatarDrawable.setInfo(currentAccount, user);
                avatarView.setImage(ImageLocation.getForUserOrChat(currentAccount, user, ImageLocation.TYPE_SMALL), null, avatarDrawable, user);
            } else if (chat != null) {
                avatarDrawable.setInfo(currentAccount, chat);
                avatarView.setImage(ImageLocation.getForUserOrChat(currentAccount, chat, ImageLocation.TYPE_SMALL), null, avatarDrawable, chat);
            } else {
                avatarDrawable.setInfo(dialogId, title, null);
                avatarView.setImageDrawable(avatarDrawable);
            }

            if (alias != null && !alias.isEmpty()) {
                titleView.setText(alias);
                subtitleView.setVisibility(View.VISIBLE);
                subtitleView.setText(title);
            } else {
                titleView.setText(title);
                subtitleView.setVisibility(View.GONE);
            }
            separator.setVisibility(showSeparator ? View.VISIBLE : View.GONE);
        }
    }

    private class ListAdapter extends RecyclerView.Adapter {

        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            HiddenChatCell cell = new HiddenChatCell(context);
            cell.setLayoutParams(new RecyclerView.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(58)));
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            HiddenChatCell cell = (HiddenChatCell) holder.itemView;
            cell.bind(visibleDialogIds.get(position), position != visibleDialogIds.size() - 1);
        }

        @Override
        public int getItemCount() {
            return visibleDialogIds.size();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDialogs();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
