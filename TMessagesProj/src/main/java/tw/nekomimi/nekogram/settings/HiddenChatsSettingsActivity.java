package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.HiddenChatsAliasEditorDialog;
import org.telegram.ui.Components.HiddenChatsPasscodeDialog;
import org.telegram.ui.Components.RecyclerListView;

import static org.telegram.messenger.LocaleController.getString;

import tw.nekomimi.nekogram.config.CellGroup;
import tw.nekomimi.nekogram.config.cell.AbstractConfigCell;
import tw.nekomimi.nekogram.config.cell.ConfigCellCustom;
import tw.nekomimi.nekogram.config.cell.ConfigCellDivider;
import tw.nekomimi.nekogram.config.cell.ConfigCellHeader;
import org.telegram.ui.HiddenChatsListActivity;
import tw.nekomimi.nekogram.helpers.HiddenChatsHelper;

@SuppressLint("NotifyDataSetChanged")
public class HiddenChatsSettingsActivity extends BaseNekoXSettingsActivity implements NotificationCenter.NotificationCenterDelegate {

    @Override
    public boolean onFragmentCreate() {
        if (!super.onFragmentCreate()) {
            return false;
        }
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.hiddenChatsLocked);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.hiddenChatsLocked);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.hiddenChatsLocked && account == UserConfig.selectedAccount && !HiddenChatsHelper.isUnlocked(account)) {
            finishFragment();
        }
    }

    private final CellGroup cellGroup = new CellGroup(this);

    private final AbstractConfigCell headerProtection = cellGroup.appendCell(new ConfigCellHeader(getString(R.string.HiddenChatsSectionProtection)));
    private final AbstractConfigCell passwordRow = cellGroup.appendCell(new ConfigCellCustom("HiddenChatsPassword", CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true));
    private final AbstractConfigCell fingerprintRow = cellGroup.appendCell(new ConfigCellCustom("HiddenChatsFingerprintRow", CellGroup.ITEM_TYPE_TEXT_CHECK, true));
    private final AbstractConfigCell resetZoneRow = cellGroup.appendCell(new ConfigCellCustom("HiddenChatsResetZoneRow", CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true));
    private final AbstractConfigCell dividerProtection = cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell headerBehavior = cellGroup.appendCell(new ConfigCellHeader(getString(R.string.HiddenChatsBehaviorHeader)));
    private final AbstractConfigCell autoLockRow = cellGroup.appendCell(new ConfigCellCustom("HiddenChatsAutoLockRow", CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true));
    private final AbstractConfigCell notificationsRow = cellGroup.appendCell(new ConfigCellCustom("HiddenChatsNotificationsRow", CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true));
    private final AbstractConfigCell callsRow = cellGroup.appendCell(new ConfigCellCustom("HiddenChatsCallsRow", CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true));
    private final AbstractConfigCell stdAliasRow = cellGroup.appendCell(new ConfigCellCustom("HiddenChatsStdAliasRow", CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true));
    private final AbstractConfigCell holdToggleRow = cellGroup.appendCell(new ConfigCellCustom("HiddenChatsHoldToggleRow", CellGroup.ITEM_TYPE_TEXT_CHECK, true));
    private final AbstractConfigCell dividerBehavior = cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell headerHidden = cellGroup.appendCell(new ConfigCellHeader(getString(R.string.HiddenChatsManageHidden)));
    private final AbstractConfigCell hiddenChatsListRow = cellGroup.appendCell(new ConfigCellCustom("HiddenChatsListRow", CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true));
    private final AbstractConfigCell hintRow = cellGroup.appendCell(new ConfigCellCustom("HiddenChatsHint", CellGroup.ITEM_TYPE_TEXT, false));

    private ListAdapter listAdapter;

    private class ListAdapter extends BaseNekoXSettingsActivity.BaseListAdapter {
        ListAdapter(Context context) {
            super(context);
        }

        @Override
        protected void onBindCustomViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            bindCustomCell(holder, position);
        }
    }

    @Override
    protected RecyclerListView.SelectionAdapter getListAdapter() {
        if (listAdapter == null) {
            listAdapter = new ListAdapter(getContext());
        }
        return listAdapter;
    }

    @Override
    protected CellGroup getCellGroup() {
        return cellGroup;
    }

    @Override
    protected String getSettingsPrefix() {
        return "hidden";
    }

    @Override
    public String getTitle() {
        return getString(R.string.HiddenChatsSettings);
    }

    @Override
    public View createView(Context context) {
        View superView = super.createView(context);
        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);
        setupDefaultListeners();
        return superView;
    }

    private boolean pinSet() {
        return HiddenChatsHelper.hasPinForAccount(UserConfig.selectedAccount);
    }

    @Override
    protected void onCustomCellClick(View view, int position, float x, float y) {
        if (position == cellGroup.rows.indexOf(passwordRow)) {
            HiddenChatsHelper.requestUnlockWithPin(getParentActivity(), getString(R.string.HiddenChatsEnterCurrentPassword), () ->
                    new HiddenChatsPasscodeDialog(getParentActivity(), getString(R.string.HiddenChatsCreateNewPassword), getString(R.string.HiddenChatsConfirmNewPassword), (java.util.function.Consumer<String>) pin ->
                            listAdapter.notifyDataSetChanged()).show());
        } else if (position == cellGroup.rows.indexOf(fingerprintRow)) {
            if (pinSet()) {
                HiddenChatsHelper.setFingerprintEnabled(!HiddenChatsHelper.isFingerprintEnabled(UserConfig.selectedAccount), UserConfig.selectedAccount);
                listAdapter.notifyDataSetChanged();
            }
        } else if (position == cellGroup.rows.indexOf(autoLockRow)) {
            if (pinSet()) showAutoLockPicker();
        } else if (position == cellGroup.rows.indexOf(notificationsRow)) {
            if (pinSet()) showNotificationsPicker();
        } else if (position == cellGroup.rows.indexOf(callsRow)) {
            if (pinSet()) showCallsPicker();
        } else if (position == cellGroup.rows.indexOf(stdAliasRow)) {
            if (!pinSet()) return;
            HiddenChatsAliasEditorDialog.show(this, getString(R.string.HiddenChatsStandardAlias), HiddenChatsHelper.getStandardAlias(UserConfig.selectedAccount), alias -> {
                HiddenChatsHelper.setStandardAlias(alias, UserConfig.selectedAccount);
                listAdapter.notifyDataSetChanged();
            });
        } else if (position == cellGroup.rows.indexOf(holdToggleRow)) {
            if (pinSet()) {
                HiddenChatsHelper.setHoldToggleEnabled(!HiddenChatsHelper.isHoldToggleEnabled(UserConfig.selectedAccount), UserConfig.selectedAccount);
                listAdapter.notifyDataSetChanged();
            }
        } else if (position == cellGroup.rows.indexOf(hiddenChatsListRow)) {
            if (pinSet()) {
                presentFragment(new HiddenChatsListActivity());
            }
        } else if (position == cellGroup.rows.indexOf(resetZoneRow)) {
            if (pinSet()) {
                showResetZoneConfirm();
            }
        }
    }

    private void showResetZoneConfirm() {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.HiddenChatsResetZone));
        builder.setMessage(getString(R.string.HiddenChatsResetConfirm));
        builder.setPositiveButton(getString(R.string.HiddenChatsReset), (dialog, which) -> {
            HiddenChatsHelper.resetAllForAccount(UserConfig.selectedAccount);
            finishFragment();
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        builder.show();
    }

    private void bindCustomCell(@NonNull RecyclerView.ViewHolder holder, int position) {
        AbstractConfigCell a = cellGroup.rows.get(position);
        if (!(a instanceof ConfigCellCustom)) {
            return;
        }
        if (a == passwordRow) {
            TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
            textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            textCell.setText(getString(R.string.HiddenChatsPasswordChange), true);
        } else if (a == fingerprintRow) {
            TextCheckCell textCell = (TextCheckCell) holder.itemView;
            textCell.setEnabled(pinSet(), null);
            textCell.setTextAndCheck(getString(R.string.HiddenChatsFingerprint), pinSet() && HiddenChatsHelper.isFingerprintEnabled(UserConfig.selectedAccount), false);
        } else if (a == autoLockRow) {
            TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
            textCell.setEnabled(pinSet(), null);
            textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            textCell.setTextAndValue(getString(R.string.HiddenChatsAutoLock), autoLockLabel(HiddenChatsHelper.getAutoLockSeconds(UserConfig.selectedAccount)), true);
        } else if (a == notificationsRow) {
            TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
            textCell.setEnabled(pinSet(), null);
            textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            textCell.setTextAndValue(getString(R.string.HiddenChatsNotifications), notificationsValue(), true);
        } else if (a == callsRow) {
            TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
            textCell.setEnabled(pinSet(), null);
            textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            textCell.setTextAndValue(getString(R.string.HiddenChatsCalls), callsValue(), true);
        } else if (a == stdAliasRow) {
            TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
            textCell.setEnabled(pinSet(), null);
            textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            String alias = HiddenChatsHelper.getStandardAlias(UserConfig.selectedAccount);
            textCell.setTextAndValue(getString(R.string.HiddenChatsStandardAlias), alias.isEmpty() ? getString(R.string.HiddenChatsNeutralContact) : alias, true);
        } else if (a == holdToggleRow) {
            TextCheckCell textCell = (TextCheckCell) holder.itemView;
            textCell.setEnabled(pinSet(), null);
            textCell.setTextAndValueAndCheck(getString(R.string.HiddenChatsHoldToggle), getString(R.string.HiddenChatsHoldToggleDesc), pinSet() && HiddenChatsHelper.isHoldToggleEnabled(UserConfig.selectedAccount), true, true);
        } else if (a == hiddenChatsListRow) {
            TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
            textCell.setEnabled(pinSet(), null);
            textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            int count = pinSet() ? HiddenChatsHelper.getHiddenIds(UserConfig.selectedAccount).size() : 0;
            textCell.setTextAndValue(getString(R.string.HiddenChatsList), LocaleController.formatString(R.string.HiddenChatsChatsCount, count), true);
        } else if (a == resetZoneRow) {
            TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
            textCell.setEnabled(pinSet(), null);
            textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteRedText3));
            textCell.setText(getString(R.string.HiddenChatsResetZone), true);
        } else if (a == hintRow) {
            TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
            cell.setText(getString(R.string.HiddenChatsSetPasswordHint));
            cell.setBackground(Theme.getThemedDrawable(getContext(), R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
        }
    }

    private void showAutoLockPicker() {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        long[] values = {HiddenChatsHelper.AUTO_LOCK_IMMEDIATELY, 60, 300, 600, 900, 1800, 3600, 7200, 14400, 28800, 43200, 86400, HiddenChatsHelper.AUTO_LOCK_NEVER};
        String[] items = new String[values.length];
        for (int a = 0; a < values.length; a++) {
            items[a] = autoLockLabel(values[a]);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.HiddenChatsAutoLock));
        builder.setItems(items, (dialog, which) -> {
            HiddenChatsHelper.setAutoLockSeconds(values[which], UserConfig.selectedAccount);
            listAdapter.notifyDataSetChanged();
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        builder.show();
    }

    private void showNotificationsPicker() {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        String[] items = {
                getString(R.string.HiddenChatsNotifNormal),
                getString(R.string.HiddenChatsNotifNoContent),
                getString(R.string.HiddenChatsNotifAlias),
                getString(R.string.HiddenChatsNotifHide)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.HiddenChatsNotifications));
        builder.setItems(items, (dialog, which) -> {
            HiddenChatsHelper.setNotificationsMode(which, UserConfig.selectedAccount);
            listAdapter.notifyDataSetChanged();
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        builder.show();
    }

    private void showCallsPicker() {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        String[] items = {
                getString(R.string.HiddenChatsNotifNormal),
                getString(R.string.HiddenChatsNotifAlias),
                getString(R.string.HiddenChatsNotifHide)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.HiddenChatsCalls));
        builder.setItems(items, (dialog, which) -> {
            HiddenChatsHelper.setCallMode(which, UserConfig.selectedAccount);
            listAdapter.notifyDataSetChanged();
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        builder.show();
    }

    private String autoLockLabel(long seconds) {
        if (seconds == HiddenChatsHelper.AUTO_LOCK_IMMEDIATELY) {
            return getString(R.string.HiddenChatsAutoLockImmediately);
        }
        if (seconds == HiddenChatsHelper.AUTO_LOCK_NEVER) {
            return getString(R.string.HiddenChatsAutoLockNever);
        }
        if (seconds < 3600) {
            return LocaleController.formatString(R.string.HiddenChatsAutoLockMinutes, seconds / 60);
        }
        return LocaleController.formatString(R.string.HiddenChatsAutoLockHours, seconds / 3600);
    }

    private String notificationsValue() {
        switch (HiddenChatsHelper.getNotificationsMode(UserConfig.selectedAccount)) {
            case HiddenChatsHelper.NOTIF_MODE_NORMAL:
                return getString(R.string.HiddenChatsNotifNormal);
            case HiddenChatsHelper.NOTIF_MODE_NO_CONTENT:
                return getString(R.string.HiddenChatsNotifNoContent);
            case HiddenChatsHelper.NOTIF_MODE_ALIAS:
                return getString(R.string.HiddenChatsNotifAlias);
            default:
                return getString(R.string.HiddenChatsNotifHide);
        }
    }

    private String callsValue() {
        switch (HiddenChatsHelper.getCallMode(UserConfig.selectedAccount)) {
            case HiddenChatsHelper.CALL_MODE_NORMAL:
                return getString(R.string.HiddenChatsNotifNormal);
            case HiddenChatsHelper.CALL_MODE_ALIAS:
                return getString(R.string.HiddenChatsNotifAlias);
            default:
                return getString(R.string.HiddenChatsNotifHide);
        }
    }
}
