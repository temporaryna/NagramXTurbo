package tw.nekomimi.nekogram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.database.AyuData;
import com.radolyn.ayugram.messages.AyuMessagesController;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.Components.CheckBoxSquare;
import org.telegram.ui.Cells.AppIconsSelectorCell;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.Components.BlurredRecyclerView;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProviderThemed;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSource;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceColor;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceWrapped;
import org.telegram.ui.Components.chat.WallpaperBitmapProvider;
import org.telegram.ui.Components.ActionButtonStyle;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.LauncherIconController;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.config.ConfigItem;
import tw.nekomimi.nekogram.config.CellGroup;
import tw.nekomimi.nekogram.config.cell.AbstractConfigCell;
import tw.nekomimi.nekogram.config.cell.ConfigCellCustom;
import tw.nekomimi.nekogram.config.cell.ConfigCellDivider;
import tw.nekomimi.nekogram.config.cell.ConfigCellHeader;
import tw.nekomimi.nekogram.config.cell.ConfigCellSelectBox;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextCheck;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextCheckIcon;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextInput;
import tw.nekomimi.nekogram.utils.ShareUtil;
import tw.nekomimi.nekogram.helpers.TypefaceHelper;
import tw.nekomimi.nekogram.helpers.ChatsHelper;
import xyz.nextalone.nagram.NaConfig;
import xyz.nextalone.nagram.helper.ProtectedForward;

@SuppressLint("RtlHardcoded")
@SuppressWarnings("unused")
public class TurboSettingsActivity extends BaseNekoXSettingsActivity implements NotificationCenter.NotificationCenterDelegate {

    @Override
    protected RecyclerListView.SelectionAdapter getListAdapter() {
        return listAdapter;
    }

    @Override
    protected CellGroup getCellGroup() {
        return cellGroup;
    }

    @Override
    protected String getSettingsPrefix() {
        return "turbo";
    }

    private final CellGroup cellGroup = new CellGroup(this);

    private final AbstractConfigCell headerAppIcon = cellGroup.appendCell(new ConfigCellHeader(getString(R.string.AppIcon)));
    private final AbstractConfigCell appIconPickerRow = cellGroup.appendCell(new ConfigCellCustom("AppIconPicker", ConfigCellCustom.CUSTOM_ITEM_AppIconPicker, true));
    private final AbstractConfigCell modernClassicIconsRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getModernClassicIcons(), getString(R.string.ModernClassicIconsAbout)));
    private final AbstractConfigCell notificationPreviewRow = cellGroup.appendCell(new ConfigCellCustom("NotificationPreview", ConfigCellCustom.CUSTOM_ITEM_NotificationPreview, true));
    private final AbstractConfigCell notificationMarksRow = cellGroup.appendCell(new ConfigCellCustom("NotificationMarksPicker", ConfigCellCustom.CUSTOM_ITEM_NotificationMarksPicker, true));
    private final AbstractConfigCell dividerAppIcon = cellGroup.appendCell(new ConfigCellDivider());
    private AppIconsSelectorCell appIconsSelectorCell;
    private NotificationPreviewCell notificationPreviewCell;
    private NotificationMarksCell notificationMarksCell;

    private final AbstractConfigCell headerInputBar = cellGroup.appendCell(new ConfigCellHeader(getString(R.string.InputBar)));
    private final AbstractConfigCell inputBarPreviewRow = cellGroup.appendCell(new ConfigCellCustom("InputBarPreview", ConfigCellCustom.CUSTOM_ITEM_InputBarPreview, false));
    private final AbstractConfigCell inputBarTextSizeRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getInputBarTextSizeSameAsChat()));
    private final AbstractConfigCell inputBarTextSizeSliderRow = cellGroup.appendCell(new ConfigCellCustom("InputBarTextSizeSlider", ConfigCellCustom.CUSTOM_ITEM_InputBarTextSizeSlider, false));
    private final AbstractConfigCell iosButtonPlacementRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getIosButtonPlacement()));
    private final AbstractConfigCell iosInputAppearanceRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getIosInputAppearance()));
    private final AbstractConfigCell compactInputSizeRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getCompactInputSize()));
    private final AbstractConfigCell actionButtonStyleRow = cellGroup.appendCell(new ConfigCellSelectBox(null, NaConfig.INSTANCE.getActionButtonStyle(), new String[]{
            getString(R.string.ActionButtonStyleAccent),
            getString(R.string.ActionButtonStyleNeutral),
            getString(R.string.ActionButtonStyleWhite)
    }, new int[]{
            ActionButtonStyle.ACCENT,
            ActionButtonStyle.NEUTRAL,
            ActionButtonStyle.WHITE
    }, null));
    private final AbstractConfigCell buttonOutlineRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getButtonOutlineEnabled()));
    private final AbstractConfigCell dividerInputBar = cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell headerMedia = cellGroup.appendCell(new ConfigCellHeader(getString(R.string.MediaSettings)));
    private final AbstractConfigCell swipeAllMediaRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getSwipeAllMedia(), getString(R.string.SwipeAllMediaAbout)));
    private final AbstractConfigCell seamlessVideoHandoffRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getSeamlessVideoHandoff(), getString(R.string.SeamlessVideoHandoffAbout)));
    private final AbstractConfigCell mediaAutoRotateModeRow = cellGroup.appendCell(new ConfigCellSelectBox(null, NaConfig.INSTANCE.getMediaAutoRotateMode(), new String[]{
            getString(R.string.MediaAutoRotateOff),
            getString(R.string.MediaAutoRotateFill),
            getString(R.string.MediaAutoRotateGyro),
    }, null, new int[]{
            R.raw.media_rotate_off,
            R.raw.media_rotate_fill,
            R.raw.media_rotate_gyro,
    }, null));
    private final AbstractConfigCell showMediaRotateButtonRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getShowMediaRotateButton()));
    private final AbstractConfigCell scrollToCurrentPhotoRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getScrollToCurrentPhoto(), getString(R.string.ScrollToCurrentPhotoAbout)));
    private final AbstractConfigCell dateFormatInBubbleRow = cellGroup.appendCell(new ConfigCellSelectBox("ShowDateInBubble", NaConfig.INSTANCE.getDateFormatInBubble(), buildDateFormatLabels(), new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8}, null));
    private final AbstractConfigCell photoViewerHdrRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.photoViewerHdr, getString(R.string.PhotoViewerHdrAbout)));
    private final AbstractConfigCell dividerMedia = cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell headerForwarding = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellHeader(getString(R.string.ForwardingSettings)));
    private final AbstractConfigCell forwardProtectedModeRow = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellSelectBox(null, NaConfig.INSTANCE.getForwardProtectedMode(), new String[]{
            getString(R.string.ForwardProtectedModeAsk),
            getString(R.string.ForwardProtectedModeAlways),
            getString(R.string.ForwardProtectedModeNever)
    }, new int[]{
            ProtectedForward.FORWARD_PROTECTED_ASK,
            ProtectedForward.FORWARD_PROTECTED_ALWAYS,
            ProtectedForward.FORWARD_PROTECTED_NEVER
    }, null));
        private final AbstractConfigCell leftActionButtonRow = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellSelectBox(null, NaConfig.INSTANCE.getLeftBottomButton(), new String[]{
                getString(R.string.Reply),
                getString(R.string.AddToSavedMessages),
                getString(R.string.FastForward),
                getString(R.string.SelectBetween),
                getString(R.string.NoCaptionForward),
                getString(R.string.NoQuoteForward),
        }, new int[]{
                ChatsHelper.LEFT_BUTTON_REPLY,
                ChatsHelper.LEFT_BUTTON_SAVE_MESSAGE,
                ChatsHelper.LEFT_BUTTON_DIRECT_SHARE,
                ChatsHelper.LEFT_BUTTON_SELECT_BETWEEN,
                ChatsHelper.LEFT_BUTTON_NOCAPTION,
                ChatsHelper.LEFT_BUTTON_NOQUOTE,
        }, null));
        private final AbstractConfigCell rightActionButtonRow = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellSelectBox(null, NaConfig.INSTANCE.getRightBottomButton(), new String[]{
                getString(R.string.Forward),
                getString(R.string.FastForward),
                getString(R.string.NoQuoteForward),
        }, new int[]{
                ChatsHelper.RIGHT_BUTTON_FORWARD,
                ChatsHelper.RIGHT_BUTTON_FAST_FORWARD,
                ChatsHelper.RIGHT_BUTTON_NOQUOTE,
        }, null));
    private final AbstractConfigCell dividerForwarding = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell headerFonts = cellGroup.appendCell(new ConfigCellHeader(getString(R.string.FontsSettings)));
    private final AbstractConfigCell typefaceRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.typeface));
    private final AbstractConfigCell fontRegularRow = cellGroup.appendCell(new ConfigCellCustom("FontRegular", ConfigCellCustom.CUSTOM_ITEM_FontRegular, true));
    private final AbstractConfigCell fontBoldRow = cellGroup.appendCell(new ConfigCellCustom("FontBold", ConfigCellCustom.CUSTOM_ITEM_FontBold, true));
    private final AbstractConfigCell fontItalicRow = cellGroup.appendCell(new ConfigCellCustom("FontItalic", ConfigCellCustom.CUSTOM_ITEM_FontItalic, true));
    private final AbstractConfigCell fontMonoRow = cellGroup.appendCell(new ConfigCellCustom("FontMono", ConfigCellCustom.CUSTOM_ITEM_FontMono, true));
    private final AbstractConfigCell fontResetRow = cellGroup.appendCell(new ConfigCellCustom("FontReset", ConfigCellCustom.CUSTOM_ITEM_FontReset, true));
    private final AbstractConfigCell dividerFonts = cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell headerSavedDeletedMessages = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellHeader(getString(R.string.DeletedMessages)));
    private final AbstractConfigCell enableSaveDeletedMessagesRow = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getEnableSaveDeletedMessages()));
    private final AbstractConfigCell messageSavingSaveMediaRow = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getMessageSavingSaveMedia()));
    private final AbstractConfigCell saveDeletedCategoriesRow = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellCustom("SaveDeletedCategories", ConfigCellCustom.CUSTOM_ITEM_SaveDeletedCategories, true));
    private final AbstractConfigCell saveDeletedMessageForBotsUserRow = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getSaveDeletedMessageForBotUser()));
    private final AbstractConfigCell saveDeletedMessageInBotChatRow = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getSaveDeletedMessageForBot()));
    private final AbstractConfigCell translucentDeletedMessagesRow = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getTranslucentDeletedMessages()));
    private final AbstractConfigCell useDeletedIconRow = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getUseDeletedIcon()));
    private final AbstractConfigCell customDeletedMarkRow = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellTextInput(null, NaConfig.INSTANCE.getCustomDeletedMark(), "", null));
    private final AbstractConfigCell enableSaveEditsHistoryRow = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getEnableSaveEditsHistory()));
    private final AbstractConfigCell clearMessageDatabaseRow = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellTextCheckIcon(null, "ClearMessageDatabase", null, AyuData.totalSize > 0 ? AndroidUtilities.formatFileSize(AyuData.totalSize) : "...", R.drawable.msg_clear, false, () -> new AlertDialog.Builder(getContext(), getResourceProvider())
            .setTitle(getString(R.string.ClearMessageDatabase))
            .setMessage(getString(R.string.AreYouSure))
            .setPositiveButton(getString(R.string.Clear), (dialog, which) -> {
                AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
                progressDialog.setCanCancel(false);
                progressDialog.show();
                Utilities.globalQueue.postRunnable(() -> {
                    AyuMessagesController.getInstance().clean();
                    AndroidUtilities.runOnUIThread(() -> {
                        progressDialog.dismiss();
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.done, getString(R.string.ClearMessageDatabaseNotification)).show();
                    });
                    AyuData.loadSizes(this::refreshAyuDataSize);
                });
            })
            .setNegativeButton(getString(R.string.Cancel), (d, w) -> d.dismiss())
            .makeRed(AlertDialog.BUTTON_POSITIVE)
            .show()));
    private final AbstractConfigCell dividerClear = BuildVars.TURBO_BASE ? null : cellGroup.appendCell(new ConfigCellDivider());

    private ListAdapter listAdapter;
    private InputBarPreviewCell inputBarPreviewCell;
    private SaveDeletedCategoriesCell saveDeletedCategoriesCell;

    public TurboSettingsActivity() {
        if (!NaConfig.INSTANCE.getIosInputAppearance().Bool()) {
            cellGroup.rows.remove(compactInputSizeRow);
        }
        if (NaConfig.INSTANCE.getInputBarTextSizeSameAsChat().Bool()) {
            cellGroup.rows.remove(inputBarTextSizeSliderRow);
        }
        if (ActionButtonStyle.getCurrentStyle() != ActionButtonStyle.NEUTRAL || NaConfig.INSTANCE.getIosInputAppearance().Bool()) {
            cellGroup.rows.remove(buttonOutlineRow);
        }
        if (NaConfig.INSTANCE.getUseDeletedIcon().Bool()) {
            cellGroup.rows.remove(customDeletedMarkRow);
        }
        if (!NaConfig.INSTANCE.getSaveDeletedMessageForBotUser().Bool()) {
            cellGroup.rows.remove(saveDeletedMessageInBotChatRow);
        }
        checkFontsRows();
        if (!BuildVars.TURBO_BASE) {
            checkUseDeletedIconRows();
            checkSaveBotMsgRows();
            checkSaveDeletedRows();
        }
        addRowsToMap(cellGroup);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        if (!BuildVars.TURBO_BASE) {
            AyuData.loadSizes(this::refreshAyuDataSize);
        }
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.showBulletin);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.showBulletin);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.showBulletin && args.length > 0 && args[0] instanceof Integer bulletinType && bulletinType == Bulletin.TYPE_APP_ICON) {
            AndroidUtilities.runOnUIThread(this::updateNotificationPreview);
        }
    }

    @Override
    protected BlurredRecyclerView createListView(Context context) {
        return new BlurredRecyclerView(context) {
            @Override
            public Integer getSelectorColor(int position) {
                if (position == cellGroup.rows.indexOf(clearMessageDatabaseRow)) {
                    return Theme.multAlpha(getThemedColor(Theme.key_text_RedRegular), .1f);
                }
                return getThemedColor(Theme.key_listSelector);
            }
        };
    }

    private static final int EASTER_EGG_TAPS_TO_UNLOCK = 5;
    private static final long EASTER_EGG_TAP_RESET_MS = 1500;
    private static final int TEST_NOTIFICATION_ID = 0x7055;

    private int easterEggTapCounter = 0;
    private Runnable easterEggResetRunnable = () -> easterEggTapCounter = 0;

    @Override
    public View createView(Context context) {
        View superView = super.createView(context);

        listAdapter = new ListAdapter(context);

        listView.setAdapter(listAdapter);

        setupDefaultListeners();

        if (actionBar.getTitleTextView() != null) {
            actionBar.getTitleTextView().setOnClickListener(v -> registerEasterEggTap());
        }

        cellGroup.callBackSettingsChanged = (key, newValue) -> {
            if (key.equals(NaConfig.INSTANCE.getModernClassicIcons().getKey())) {
                LauncherIconController.setIcon(LauncherIconController.getActiveIcon(), true, false);
                if (appIconsSelectorCell != null) {
                    appIconsSelectorCell.notifyIconsChanged();
                }
            }
            if (key.equals(NaConfig.INSTANCE.getIosButtonPlacement().getKey())
                    || key.equals(NaConfig.INSTANCE.getIosInputAppearance().getKey())
                    || key.equals(NaConfig.INSTANCE.getCompactInputSize().getKey())
                    || key.equals(NaConfig.INSTANCE.getActionButtonStyle().getKey())
                    || key.equals(NaConfig.INSTANCE.getButtonOutlineEnabled().getKey())) {
                if (inputBarPreviewCell != null) {
                    inputBarPreviewCell.updateInputBarState();
                }
            }
            if (key.equals(NaConfig.INSTANCE.getActionButtonStyle().getKey())
                    || key.equals(NaConfig.INSTANCE.getIosInputAppearance().getKey())) {
                boolean outlineRowVisible = ActionButtonStyle.getCurrentStyle() == ActionButtonStyle.NEUTRAL
                        && !NaConfig.INSTANCE.getIosInputAppearance().Bool();
                if (outlineRowVisible) {
                    if (!cellGroup.rows.contains(buttonOutlineRow)) {
                        cellGroup.rows.add(cellGroup.rows.indexOf(actionButtonStyleRow) + 1, buttonOutlineRow);
                    }
                } else {
                    cellGroup.rows.remove(buttonOutlineRow);
                }
                listAdapter.notifyDataSetChanged();
            }
            if (key.equals(NaConfig.INSTANCE.getDateFormatInBubble().getKey())) {
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface);
            }
            if (key.equals(NaConfig.INSTANCE.getInputBarTextSizeSameAsChat().getKey())) {
                if (!NaConfig.INSTANCE.getInputBarTextSizeSameAsChat().Bool()) {
                    if (!cellGroup.rows.contains(inputBarTextSizeSliderRow)) {
                        cellGroup.rows.add(cellGroup.rows.indexOf(inputBarTextSizeRow) + 1, inputBarTextSizeSliderRow);
                    }
                } else {
                    cellGroup.rows.remove(inputBarTextSizeSliderRow);
                }
                listAdapter.notifyDataSetChanged();
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.inputBarTextSizeChanged);
            }
            if (key.equals(NaConfig.INSTANCE.getIosInputAppearance().getKey())) {
                if (NaConfig.INSTANCE.getIosInputAppearance().Bool()) {
                    if (!cellGroup.rows.contains(compactInputSizeRow)) {
                        cellGroup.rows.add(cellGroup.rows.indexOf(iosInputAppearanceRow) + 1, compactInputSizeRow);
                    }
                } else {
                    cellGroup.rows.remove(compactInputSizeRow);
                }
                listAdapter.notifyDataSetChanged();
            }
            if (!BuildVars.TURBO_BASE && key.equals(NaConfig.INSTANCE.getEnableSaveDeletedMessages().getKey())) {
                checkSaveDeletedRows();
                if (saveDeletedCategoriesCell != null) {
                    saveDeletedCategoriesCell.bindStates();
                }
            } else if (!BuildVars.TURBO_BASE && (key.equals(NaConfig.INSTANCE.getMessageSavingSaveMedia().getKey())
                    || key.startsWith("SaveDeletedIn") || key.startsWith("SaveMediaIn"))) {
                if (saveDeletedCategoriesCell != null) {
                    saveDeletedCategoriesCell.bindStates();
                }
            } else if (!BuildVars.TURBO_BASE && key.equals(NaConfig.INSTANCE.getUseDeletedIcon().getKey())) {
                checkUseDeletedIconRows();
            } else if (!BuildVars.TURBO_BASE && key.equals(NaConfig.INSTANCE.getSaveDeletedMessageForBotUser().getKey())) {
                checkSaveBotMsgRows();
            } else if (key.equals(NekoConfig.typeface.getKey())) {
                tooltip.showWithAction(0, UndoView.ACTION_NEED_RESTART, null, null);
                checkFontsRows();
            } else if (key.startsWith("CustomFont")) {
                TypefaceHelper.clearAllFontCaches();
                AndroidUtilities.runOnUIThread(() -> {
                    if (LaunchActivity.instance != null) {
                        LaunchActivity.instance.recreate();
                    }
                }, 100);
            }
        };

        return superView;
    }

    @Override
    protected void handleCellClick(View view, int position, float x, float y) {
        if (position < 0 || position >= cellGroup.rows.size()) {
            return;
        }
        super.handleCellClick(view, position, x, y);
    }

    @Override
    protected boolean onItemLongClick(View view, int position, float x, float y) {
        AbstractConfigCell a = cellGroup.rows.get(position);
        if (!BuildVars.TURBO_BASE && a == clearMessageDatabaseRow) {
            ItemOptions options = makeLongClickOptions(view);
            options.add(R.drawable.msg_instant_link_solar, getString(R.string.ExportAyuDB), this::exportAyuDB);
            addDefaultLongClickOptions(options, "turbo", position);
            showLongClickOptions(view, options);
            return true;
        }
        return false;
    }

    @Override
    protected void onCustomCellClick(View view, int position, float x, float y) {
        if (position == cellGroup.rows.indexOf(fontRegularRow)) {
            presentFragment(new FontPickerActivity(TypefaceHelper.FONT_CATEGORY_REGULAR));
        } else if (position == cellGroup.rows.indexOf(fontBoldRow)) {
            presentFragment(new FontPickerActivity(TypefaceHelper.FONT_CATEGORY_BOLD));
        } else if (position == cellGroup.rows.indexOf(fontItalicRow)) {
            presentFragment(new FontPickerActivity(TypefaceHelper.FONT_CATEGORY_ITALIC));
        } else if (position == cellGroup.rows.indexOf(fontMonoRow)) {
            presentFragment(new FontPickerActivity(TypefaceHelper.FONT_CATEGORY_MONO));
        } else if (position == cellGroup.rows.indexOf(fontResetRow)) {
            if (getParentActivity() == null) return;
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(getString(R.string.FontReset));
            builder.setMessage(getString(R.string.FontResetConfirm));
            builder.setPositiveButton(getString(R.string.FontApplyButton), (dialog, which) -> {
                TypefaceHelper.resetAllFonts();
                TypefaceHelper.clearAllFontCaches();
                AndroidUtilities.runOnUIThread(() -> {
                    if (LaunchActivity.instance != null) {
                        LaunchActivity.instance.recreate();
                    }
                }, 100);
            });
            builder.setNegativeButton(getString(R.string.Cancel), null);
            showDialog(builder.create());
        }
    }

    @Override
    public int getBaseGuid() {
        return 14000;
    }

    @Override
    public int getDrawable() {
        return R.drawable.msg_rocket;
    }

    @Override
    public String getTitle() {
        return getString(R.string.NagramXTurbo);
    }

    private void checkFontsRows() {
        boolean hasCustom = TypefaceHelper.hasAnyCustomFont();
        if (hasCustom && cellGroup.rows.contains(typefaceRow)) {
            cellGroup.rows.remove(typefaceRow);
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        } else if (!hasCustom && !cellGroup.rows.contains(typefaceRow)) {
            int idx = cellGroup.rows.indexOf(fontRegularRow);
            if (idx > 0) {
                cellGroup.rows.add(idx, typefaceRow);
                if (listAdapter != null) listAdapter.notifyDataSetChanged();
            }
        }
    }

    public void refreshAyuDataSize() {
        if (listAdapter != null) {
            ((ConfigCellTextCheckIcon) clearMessageDatabaseRow).setValue(AyuData.totalSize > 0 ? AndroidUtilities.formatFileSize(AyuData.totalSize) : "...");
            listAdapter.notifyItemChanged(cellGroup.rows.indexOf(clearMessageDatabaseRow));
        }
    }

    private void exportAyuDB() {
        if (getParentActivity() == null) return;
        AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.setCanCancel(false);
        progressDialog.show();
        Utilities.globalQueue.postRunnable(() -> {
            try {
                File dbFile = ApplicationLoader.applicationContext.getDatabasePath(AyuConstants.AYU_DATABASE);
                File exportFile = new File(AndroidUtilities.getCacheDir(), AyuConstants.AYU_DATABASE_EXPORT);
                AyuData.checkpointDatabase();
                if (!AndroidUtilities.copyFile(dbFile, exportFile)) {
                    if (!exportFile.delete()) exportFile.deleteOnExit();
                    throw new IOException("Failed to copy Ayu database");
                }
                AndroidUtilities.runOnUIThread(() -> {
                    Context parentActivity = getParentActivity();
                    progressDialog.dismiss();
                    if (parentActivity != null) {
                        ShareUtil.shareFile(parentActivity, exportFile);
                    }
                });
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> {
                    progressDialog.dismiss();
                    if (getParentActivity() != null) {
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.error, getString(R.string.ErrorOccurred)).show();
                    }
                });
            }
        });
    }

    private String[] buildDateFormatLabels() {
        String timeSample = LocaleController.stringForMessageListDate(System.currentTimeMillis() / 1000);
        String[] labels = new String[9];
        labels[0] = timeSample;
        for (int i = 1; i < labels.length; i++) {
            labels[i] = ChatMessageCell.getBubbleDatePatternSample(i) + " · " + timeSample;
        }
        return labels;
    }

    private void checkSaveDeletedRows() {
        final boolean isSaveEnabled = NaConfig.INSTANCE.getEnableSaveDeletedMessages().Bool();
        final List<AbstractConfigCell> managedRows = Arrays.asList(
                messageSavingSaveMediaRow,
                saveDeletedCategoriesRow,
                saveDeletedMessageForBotsUserRow,
                saveDeletedMessageInBotChatRow,
                translucentDeletedMessagesRow,
                useDeletedIconRow,
                customDeletedMarkRow
        );
        if (listAdapter == null) {
            if (!isSaveEnabled) {
                cellGroup.rows.removeAll(managedRows);
            }
            return;
        }
        for (int i = managedRows.size() - 1; i >= 0; i--) {
            final int rowIndex = cellGroup.rows.indexOf(managedRows.get(i));
            if (rowIndex != -1) {
                cellGroup.rows.remove(rowIndex);
                listAdapter.notifyItemRemoved(rowIndex);
            }
        }
        if (isSaveEnabled) {
            final List<AbstractConfigCell> rowsToAdd = new ArrayList<>();
            for (AbstractConfigCell row : managedRows) {
                if (row == saveDeletedMessageInBotChatRow && !NaConfig.INSTANCE.getSaveDeletedMessageForBotUser().Bool()) {
                    continue;
                }
                if (row == customDeletedMarkRow && NaConfig.INSTANCE.getUseDeletedIcon().Bool()) {
                    continue;
                }
                rowsToAdd.add(row);
            }
            final int insertIndex = cellGroup.rows.indexOf(enableSaveDeletedMessagesRow) + 1;
            cellGroup.rows.addAll(insertIndex, rowsToAdd);
            listAdapter.notifyItemRangeInserted(insertIndex, rowsToAdd.size());
        }
        addRowsToMap(cellGroup);
    }

    private void checkSaveBotMsgRows() {
        boolean enabled = NaConfig.INSTANCE.getSaveDeletedMessageForBotUser().Bool();
        if (listAdapter == null) {
            if (!enabled) {
                cellGroup.rows.remove(saveDeletedMessageInBotChatRow);
            }
            return;
        }
        if (enabled) {
            final int index = cellGroup.rows.indexOf(saveDeletedMessageForBotsUserRow);
            if (!cellGroup.rows.contains(saveDeletedMessageInBotChatRow)) {
                cellGroup.rows.add(index + 1, saveDeletedMessageInBotChatRow);
                listAdapter.notifyItemInserted(index + 1);
            }
        } else {
            final int index = cellGroup.rows.indexOf(saveDeletedMessageInBotChatRow);
            if (index != -1) {
                cellGroup.rows.remove(saveDeletedMessageInBotChatRow);
                listAdapter.notifyItemRemoved(index);
            }
        }
        addRowsToMap(cellGroup);
    }

    private void checkUseDeletedIconRows() {
        boolean enabled = NaConfig.INSTANCE.getUseDeletedIcon().Bool();
        if (listAdapter == null) {
            if (enabled) {
                cellGroup.rows.remove(customDeletedMarkRow);
            }
            return;
        }
        if (!enabled) {
            final int index = cellGroup.rows.indexOf(useDeletedIconRow);
            if (!cellGroup.rows.contains(customDeletedMarkRow)) {
                cellGroup.rows.add(index + 1, customDeletedMarkRow);
                listAdapter.notifyItemInserted(index + 1);
            }
        } else {
            final int index = cellGroup.rows.indexOf(customDeletedMarkRow);
            if (index != -1) {
                cellGroup.rows.remove(customDeletedMarkRow);
                listAdapter.notifyItemRemoved(index);
            }
        }
        addRowsToMap(cellGroup);
    }

    private class SaveDeletedCategoriesCell extends LinearLayout {

        private static final int CATEGORY_COUNT = 5;
        private static final int CHECKBOX_ZONE_DP = 44;
        private static final int CHECKBOX_RIPPLE_RADIUS_DP = 16;
        private static final int CHECKBOX_SIZE_DP = 18;
        private static final int CATEGORY_ROW_START_DP = 35;
        private static final int CATEGORY_NAME_INDENT_DP = 14;
        private static final int CATEGORY_ROW_HEIGHT_DP = 50;

        private final int[] categoryNameKeys = {
                R.string.SaveDeletedCategoryPrivateChats,
                R.string.SaveDeletedCategoryPublicChannels,
                R.string.SaveDeletedCategoryPrivateChannels,
                R.string.SaveDeletedCategoryPublicGroups,
                R.string.SaveDeletedCategoryPrivateGroups
        };
        private final ConfigItem[] textConfigItems = {
                NaConfig.INSTANCE.getSaveDeletedInPrivateChats(),
                NaConfig.INSTANCE.getSaveDeletedInPublicChannels(),
                NaConfig.INSTANCE.getSaveDeletedInPrivateChannels(),
                NaConfig.INSTANCE.getSaveDeletedInPublicGroups(),
                NaConfig.INSTANCE.getSaveDeletedInPrivateGroups()
        };
        private final ConfigItem[] mediaConfigItems = {
                NaConfig.INSTANCE.getSaveMediaInPrivateChats(),
                NaConfig.INSTANCE.getSaveMediaInPublicChannels(),
                NaConfig.INSTANCE.getSaveMediaInPrivateChannels(),
                NaConfig.INSTANCE.getSaveMediaInPublicGroups(),
                NaConfig.INSTANCE.getSaveMediaInPrivateGroups()
        };
        private final View[] mediaZones = new View[CATEGORY_COUNT];
        private final CheckBoxSquare[] textCheckBoxes = new CheckBoxSquare[CATEGORY_COUNT];
        private final CheckBoxSquare[] mediaCheckBoxes = new CheckBoxSquare[CATEGORY_COUNT];

        SaveDeletedCategoriesCell(Context context) {
            super(context);
            setOrientation(VERTICAL);
            addView(buildCaptionsRow(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            for (int index = 0; index < CATEGORY_COUNT; index++) {
                addView(buildCategoryRow(context, index), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, CATEGORY_ROW_HEIGHT_DP));
            }
        }

        private LinearLayout buildCaptionsRow(Context context) {
            LinearLayout captionsRow = new LinearLayout(context);
            captionsRow.setClickable(true);
            captionsRow.setOrientation(HORIZONTAL);
            captionsRow.setPaddingRelative(AndroidUtilities.dp(21), AndroidUtilities.dp(6), AndroidUtilities.dp(21), 0);
            View spacer = new View(context);
            captionsRow.addView(spacer, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
            captionsRow.addView(buildCaptionText(context, R.string.SaveDeletedColumnText), LayoutHelper.createLinear(CHECKBOX_ZONE_DP, LayoutHelper.WRAP_CONTENT));
            captionsRow.addView(buildCaptionText(context, R.string.SaveDeletedColumnMedia), LayoutHelper.createLinear(CHECKBOX_ZONE_DP, LayoutHelper.WRAP_CONTENT));
            return captionsRow;
        }

        private TextView buildCaptionText(Context context, int stringKey) {
            TextView caption = new TextView(context);
            caption.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            caption.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3));
            caption.setGravity(Gravity.CENTER);
            caption.setSingleLine(true);
            caption.setEllipsize(TextUtils.TruncateAt.END);
            caption.setText(getString(stringKey));
            return caption;
        }

        private LinearLayout buildCategoryRow(Context context, int index) {
            LinearLayout categoryRow = new LinearLayout(context) {
                @Override
                protected void onDraw(Canvas canvas) {
                    canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(CATEGORY_ROW_START_DP), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(CATEGORY_ROW_START_DP) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
                }
            };
            categoryRow.setWillNotDraw(false);
            categoryRow.setClickable(true);
            categoryRow.setOrientation(HORIZONTAL);
            categoryRow.setGravity(Gravity.CENTER_VERTICAL);
            categoryRow.setPaddingRelative(AndroidUtilities.dp(21), 0, AndroidUtilities.dp(21), 0);
            TextView name = new TextView(context);
            name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            name.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            name.setText(getString(categoryNameKeys[index]));
            LinearLayout.LayoutParams nameParams = LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f);
            nameParams.setMarginStart(AndroidUtilities.dp(CATEGORY_NAME_INDENT_DP));
            categoryRow.addView(name, nameParams);
            View textZone = buildCheckBoxZone(context, index, true);
            mediaZones[index] = buildCheckBoxZone(context, index, false);
            categoryRow.addView(textZone, LayoutHelper.createLinear(CHECKBOX_ZONE_DP, LayoutHelper.MATCH_PARENT));
            categoryRow.addView(mediaZones[index], LayoutHelper.createLinear(CHECKBOX_ZONE_DP, LayoutHelper.MATCH_PARENT));
            return categoryRow;
        }

        private View buildCheckBoxZone(Context context, int index, boolean isTextColumn) {
            FrameLayout zone = new FrameLayout(context);
            zone.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_CIRCLE_20DP, AndroidUtilities.dp(CHECKBOX_RIPPLE_RADIUS_DP)));
            String columnLabel = getString(isTextColumn ? R.string.SaveDeletedColumnText : R.string.SaveDeletedColumnMedia);
            zone.setContentDescription(columnLabel + ": " + getString(categoryNameKeys[index]));
            zone.setFocusable(true);
            CheckBoxSquare checkBox = new CheckBoxSquare(context, false, getResourceProvider());
            checkBox.setDuplicateParentStateEnabled(false);
            checkBox.setFocusable(false);
            checkBox.setClickable(false);
            zone.addView(checkBox, LayoutHelper.createFrame(CHECKBOX_SIZE_DP, CHECKBOX_SIZE_DP, Gravity.CENTER));
            if (isTextColumn) {
                textCheckBoxes[index] = checkBox;
                zone.setOnClickListener(v -> {
                    if (!v.isEnabled()) return;
                    onTextZoneClicked(index);
                });
            } else {
                mediaCheckBoxes[index] = checkBox;
                zone.setOnClickListener(v -> {
                    if (!v.isEnabled()) return;
                    onMediaZoneClicked(index);
                });
            }
            return zone;
        }

        private void onTextZoneClicked(int index) {
            ConfigItem textConfigItem = textConfigItems[index];
            ConfigItem mediaConfigItem = mediaConfigItems[index];
            if (textConfigItem.Bool()) {
                mediaConfigItem.setConfigBool(false);
                textConfigItem.setConfigBool(false);
            } else {
                textConfigItem.setConfigBool(true);
            }
            bindStates();
            cellGroup.runCallback(textConfigItem.getKey(), textConfigItem.Bool());
        }

        private void onMediaZoneClicked(int index) {
            ConfigItem textConfigItem = textConfigItems[index];
            ConfigItem mediaConfigItem = mediaConfigItems[index];
            if (!textConfigItem.Bool()) {
                textConfigItem.setConfigBool(true);
                mediaConfigItem.setConfigBool(true);
            } else {
                mediaConfigItem.toggleConfigBool();
            }
            bindStates();
            cellGroup.runCallback(mediaConfigItem.getKey(), mediaConfigItem.Bool());
        }

        public void bindStates() {
            boolean isSaveEnabled = NaConfig.INSTANCE.getEnableSaveDeletedMessages().Bool();
            boolean isMediaColumnEnabled = NaConfig.INSTANCE.getMessageSavingSaveMedia().Bool();
            setAlpha(isSaveEnabled ? 1f : 0.5f);
            for (int index = 0; index < CATEGORY_COUNT; index++) {
                boolean isTextChecked = textConfigItems[index].Bool();
                textCheckBoxes[index].setChecked(isTextChecked, false);
                mediaCheckBoxes[index].setChecked(mediaConfigItems[index].Bool() && isTextChecked, false);
                mediaZones[index].setEnabled(isSaveEnabled && isMediaColumnEnabled);
                mediaZones[index].setAlpha(!isSaveEnabled || isMediaColumnEnabled ? 1f : 0.5f);
            }
        }
    }

    private abstract class TextSizeSliderCell extends FrameLayout {

        private final SeekBarView sizeBar;
        private final TextPaint digitPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final int minSizeDp = ChatActivityEnterView.INPUT_BAR_TEXT_SIZE_MIN_DP;
        private final int maxSizeDp = ChatActivityEnterView.INPUT_BAR_TEXT_SIZE_MAX_DP;

        protected abstract int getCurrentSizeDp();

        protected abstract void onSizeSelected(int sizeDp, boolean isStopped);

        public TextSizeSliderCell(Context context) {
            super(context);
            setWillNotDraw(false);
            digitPaint.setTextSize(AndroidUtilities.dp(16));
            sizeBar = new SeekBarView(context);
            sizeBar.setReportChanges(true);
            sizeBar.setSeparatorsCount(maxSizeDp - minSizeDp + 1);
            sizeBar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
                @Override
                public void onSeekBarDrag(boolean stop, float progress) {
                    int sizeDp = minSizeDp + Math.round((maxSizeDp - minSizeDp) * progress);
                    onSizeSelected(sizeDp, stop);
                    invalidate();
                }

                @Override
                public CharSequence getContentDescription() {
                    return String.valueOf(minSizeDp + Math.round((maxSizeDp - minSizeDp) * sizeBar.getProgress()));
                }

                @Override
                public int getStepsCount() {
                    return maxSizeDp - minSizeDp;
                }
            });
            addView(sizeBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.LEFT | Gravity.TOP, 9, 5, 43, 11));
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            sizeBar.getSeekBarAccessibilityDelegate().onInitializeAccessibilityNodeInfoInternal(this, info);
        }

        @Override
        public boolean performAccessibilityAction(int action, Bundle arguments) {
            return super.performAccessibilityAction(action, arguments) || sizeBar.getSeekBarAccessibilityDelegate().performAccessibilityActionInternal(this, action, arguments);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            digitPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText, getResourceProvider()));
            canvas.drawText(String.valueOf(getCurrentSizeDp()), getMeasuredWidth() - AndroidUtilities.dp(39), AndroidUtilities.dp(28), digitPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(54), MeasureSpec.EXACTLY));
            sizeBar.setProgress((getCurrentSizeDp() - minSizeDp) / (float) (maxSizeDp - minSizeDp));
        }
    }

    private class InputBarPreviewCell extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

        private static final int BUBBLE_RADIUS_DP = 22;
        private static final int BAR_HEIGHT_DP = 44;
        private static final int GAP_NORMAL_DP = 8;
        private static final int GAP_COMPACT_DP = 2;
        private static final int CELL_VERTICAL_PADDING_DP = 12;
        private static final int CELL_BOTTOM_PADDING_DP = 20;

        private final Theme.ResourcesProvider resourcesProvider;
        private final WallpaperBitmapProvider wallpaperBitmapProvider = new WallpaperBitmapProvider();

        private BlurredBackgroundDrawableViewFactory glassFactory;
        private BlurredBackgroundColorProviderThemed colorProvider;
        private BlurredBackgroundColorProviderThemed whiteColorProvider;
        private BlurredBackgroundColorProviderThemed accentColorProvider;
        private final Paint sendCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint previewGreetingPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private BlurredBackgroundDrawable oneBlockDrawable;
        private BlurredBackgroundDrawable capsuleDrawable;
        private BlurredBackgroundDrawable leftBubbleDrawable;
        private BlurredBackgroundDrawable rightBubbleDrawable;

        private final ImageView attachIconView;
        private final ImageView emojiIconView;
        private final ImageView sendIconView;

        private boolean isPlacementEnabled;
        private boolean isAppearanceEnabled;
        private boolean isCompactEnabled;
        private int gapPx;

        private Drawable lastWallpaper;
        private boolean lastIsBlurEnabled;
        private boolean lastIsLiquidGlassEnabled;
        private Drawable lastDrawnWallpaper;

        public InputBarPreviewCell(Context context, Theme.ResourcesProvider provider) {
            super(context);
            resourcesProvider = provider;
            setWillNotDraw(false);
            setClipChildren(false);
            setPadding(0, AndroidUtilities.dp(CELL_VERTICAL_PADDING_DP), 0, AndroidUtilities.dp(CELL_BOTTOM_PADDING_DP));

            int iconColor = Theme.getColor(Theme.key_glass_defaultIcon, provider);
            int sendColor = Theme.getColor(Theme.key_chat_messagePanelSend, provider);

            attachIconView = createIconView(context, R.drawable.msg_input_attach2, iconColor);
            emojiIconView = createIconView(context, R.drawable.smiles_tab_smiles, iconColor);
            sendIconView = createIconView(context, R.drawable.send_plane_24, sendColor);

            buildGlassFactory();
            updateInputBarState();
        }

        private ImageView createIconView(Context context, int resId, int color) {
            ImageView view = new ImageView(context);
            view.setImageResource(resId);
            view.setScaleType(ImageView.ScaleType.CENTER);
            view.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            addView(view);
            return view;
        }

        private void buildGlassFactory() {
            Drawable wallpaper = Theme.getCachedWallpaperNonBlocking();
            boolean isBlurEnabled = SharedConfig.chatBlurEnabled() && LiteMode.isEnabled(LiteMode.FLAG_CHAT_BLUR);
            boolean isLiquidGlassEnabled = LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS);
            if (glassFactory != null && wallpaper == lastWallpaper && isBlurEnabled == lastIsBlurEnabled && isLiquidGlassEnabled == lastIsLiquidGlassEnabled) {
                return;
            }
            lastWallpaper = wallpaper;
            lastIsBlurEnabled = isBlurEnabled;
            lastIsLiquidGlassEnabled = isLiquidGlassEnabled;

            BlurredBackgroundSource source;
            if (isBlurEnabled && wallpaper != null) {
                source = wallpaperBitmapProvider.updateSourceFromBackgroundViewDrawable(wallpaper);
            } else {
                source = new BlurredBackgroundSourceColor();
            }
            BlurredBackgroundSourceWrapped wrappedSource = new BlurredBackgroundSourceWrapped();
            wrappedSource.setSource(source);
            glassFactory = new BlurredBackgroundDrawableViewFactory(wrappedSource);
            colorProvider = new BlurredBackgroundColorProviderThemed(resourcesProvider, Theme.key_chat_messagePanelBackground);
            final boolean previewBlurEnabled = isBlurEnabled;
            final boolean previewLiquidGlass = isLiquidGlassEnabled;
            whiteColorProvider = new BlurredBackgroundColorProviderThemed(resourcesProvider, Theme.key_windowBackgroundWhite) {
                @Override
                public int getBackgroundColor() {
                    if (!previewBlurEnabled) return 0xFFFFFFFF;
                    return previewLiquidGlass ? 0xD9FFFFFF : 0xC2FFFFFF;
                }
            };
            accentColorProvider = new BlurredBackgroundColorProviderThemed(resourcesProvider, Theme.key_chat_messagePanelSend);
            if (!isBlurEnabled) {
                colorProvider.setAlpha(1.0f);
            }
            glassFactory.setLiquidGlassEffectAllowed(isLiquidGlassEnabled);
            oneBlockDrawable = createBubble();
            capsuleDrawable = createBubble();
            leftBubbleDrawable = createBubble();
            rightBubbleDrawable = createBubble();
        }

        private BlurredBackgroundDrawable createBubble() {
            BlurredBackgroundDrawable drawable = glassFactory.create(this, colorProvider);
            drawable.setRadius(AndroidUtilities.dp(BUBBLE_RADIUS_DP));
            return drawable;
        }

        public void updateInputBarState() {
            isPlacementEnabled = NaConfig.INSTANCE.getIosButtonPlacement().Bool();
            isAppearanceEnabled = NaConfig.INSTANCE.getIosInputAppearance().Bool();
            isCompactEnabled = NaConfig.INSTANCE.getCompactInputSize().Bool() && isAppearanceEnabled;
            int gapDp = isCompactEnabled ? GAP_COMPACT_DP : GAP_NORMAL_DP;
            gapPx = AndroidUtilities.dp(gapDp);

            ImageView leftIcon = isPlacementEnabled ? attachIconView : emojiIconView;
            ImageView rightIcon = isPlacementEnabled ? emojiIconView : attachIconView;
            int edgeInsetDp = isAppearanceEnabled ? gapDp : CELL_VERTICAL_PADDING_DP;
            leftIcon.setLayoutParams(LayoutHelper.createFrame(BAR_HEIGHT_DP, BAR_HEIGHT_DP, Gravity.BOTTOM | Gravity.LEFT, edgeInsetDp, 0, 0, 0));
            rightIcon.setLayoutParams(LayoutHelper.createFrame(BAR_HEIGHT_DP, BAR_HEIGHT_DP, Gravity.BOTTOM | Gravity.RIGHT, 0, 0, BAR_HEIGHT_DP + edgeInsetDp + gapDp, 0));
            sendIconView.setLayoutParams(LayoutHelper.createFrame(BAR_HEIGHT_DP, BAR_HEIGHT_DP, Gravity.BOTTOM | Gravity.RIGHT, 0, 0, edgeInsetDp, 0));

            if (rightBubbleDrawable != null) {
                rightBubbleDrawable.setColorProvider(ActionButtonStyle.resolveBubbleColorProvider(whiteColorProvider, colorProvider, accentColorProvider));
            }
            sendIconView.setColorFilter(new PorterDuffColorFilter(
                    ActionButtonStyle.resolveIconColor(resourcesProvider),
                    PorterDuff.Mode.SRC_IN));

            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int height = AndroidUtilities.dp(BAR_HEIGHT_DP + CELL_VERTICAL_PADDING_DP + CELL_BOTTOM_PADDING_DP);
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            Drawable wallpaper = Theme.getCachedWallpaperNonBlocking();
            if (wallpaper != null) {
                if (wallpaper != lastDrawnWallpaper) {
                    lastDrawnWallpaper = wallpaper;
                    invalidate();
                }
                wallpaper.setBounds(0, 0, getWidth(), getHeight());
                wallpaper.draw(canvas);
            }
            int padding = AndroidUtilities.dp(CELL_VERTICAL_PADDING_DP);
            int fieldTop = attachIconView.getTop();
            int fieldBottom = attachIconView.getBottom();
            if (isAppearanceEnabled) {
                ImageView leftIcon = isPlacementEnabled ? attachIconView : emojiIconView;
                int pillLeft = leftIcon.getRight() + gapPx;
                int pillRight = sendIconView.getLeft() - gapPx;
                capsuleDrawable.setBounds(pillLeft, fieldTop, pillRight, fieldBottom);
                capsuleDrawable.draw(canvas);
                leftBubbleDrawable.setBounds(leftIcon.getLeft(), leftIcon.getTop(), leftIcon.getRight(), leftIcon.getBottom());
                leftBubbleDrawable.draw(canvas);
                rightBubbleDrawable.setBounds(sendIconView.getLeft(), sendIconView.getTop(), sendIconView.getRight(), sendIconView.getBottom());
                rightBubbleDrawable.draw(canvas);
            } else {
                oneBlockDrawable.setBounds(padding, fieldTop, getWidth() - padding, fieldBottom);
                oneBlockDrawable.draw(canvas);
                sendCirclePaint.setColor(ActionButtonStyle.resolveBackgroundColor(resourcesProvider));
                float sendCx = (sendIconView.getLeft() + sendIconView.getRight()) / 2f;
                float sendCy = (sendIconView.getTop() + sendIconView.getBottom()) / 2f;
                float sendCircleRadius = (sendIconView.getRight() - sendIconView.getLeft()) / 2f - AndroidUtilities.dp(3);
                canvas.drawCircle(sendCx, sendCy, sendCircleRadius, sendCirclePaint);
                if (ActionButtonStyle.getCurrentStyle() != ActionButtonStyle.ACCENT) {
                    int sendCircleFillColor = sendCirclePaint.getColor();
                    sendCirclePaint.setStyle(Paint.Style.STROKE);
                    sendCirclePaint.setStrokeWidth(AndroidUtilities.dp(1));
                    sendCirclePaint.setColor(ActionButtonStyle.resolveStrokeColor(resourcesProvider));
                    canvas.drawCircle(sendCx, sendCy, sendCircleRadius - AndroidUtilities.dp(0.5f), sendCirclePaint);
                    sendCirclePaint.setStyle(Paint.Style.FILL);
                    sendCirclePaint.setColor(sendCircleFillColor);
                }
            }
            ImageView greetingLeftIcon = isPlacementEnabled ? attachIconView : emojiIconView;
            int greetingLeft = greetingLeftIcon.getRight() + (isAppearanceEnabled ? gapPx : 0) + AndroidUtilities.dp(12);
            int greetingRight = sendIconView.getLeft() - AndroidUtilities.dp(12);
            previewGreetingPaint.setColor(Theme.getColor(Theme.key_chat_messagePanelText, resourcesProvider));
            previewGreetingPaint.setTextSize(AndroidUtilities.dp(ChatActivityEnterView.resolveInputBarTextSize()));
            Typeface inputTypeface = tw.nekomimi.nekogram.helpers.TypefaceHelper.getCustomFontForCategory(tw.nekomimi.nekogram.helpers.TypefaceHelper.FONT_CATEGORY_REGULAR);
            previewGreetingPaint.setTypeface(inputTypeface != null ? inputTypeface : Typeface.DEFAULT);
            Paint.FontMetrics greetingFontMetrics = previewGreetingPaint.getFontMetrics();
            float greetingBaseline = (fieldTop + fieldBottom) / 2f - (greetingFontMetrics.ascent + greetingFontMetrics.descent) / 2f;
            canvas.save();
            canvas.clipRect(greetingLeft, fieldTop, greetingRight, fieldBottom);
            canvas.drawText(getString(R.string.InputBarTextSizePreviewText), greetingLeft, greetingBaseline, previewGreetingPaint);
            canvas.restore();
            super.dispatchDraw(canvas);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.didSetNewWallpapper);
            NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.inputBarTextSizeChanged);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didSetNewWallpapper);
            NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.inputBarTextSizeChanged);
        }

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.didSetNewWallpapper) {
                buildGlassFactory();
                invalidate();
            } else if (id == NotificationCenter.inputBarTextSizeChanged) {
                invalidate();
            }
        }
    }

    private void updateNotificationPreview() {
        if (notificationPreviewCell != null) {
            notificationPreviewCell.updateSilhouette();
        }
        if (notificationMarksCell != null) {
            notificationMarksCell.updateSelection();
        }
    }

    private class NotificationPreviewCell extends FrameLayout {
        private final ImageView silhouetteView;
        private final TextView labelView;

        public NotificationPreviewCell(Context context) {
            super(context);
            setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));

            FrameLayout statusBar = new FrameLayout(context);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFF101014);
            bg.setCornerRadius(AndroidUtilities.dp(9));
            statusBar.setBackground(bg);
            addView(statusBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 44, Gravity.CENTER_VERTICAL));

            silhouetteView = new ImageView(context);
            statusBar.addView(silhouetteView, LayoutHelper.createFrame(18, 18, Gravity.CENTER_VERTICAL | Gravity.LEFT, 10, 0, 0, 0));

            TextView time = new TextView(context);
            time.setText("12:47");
            time.setTextColor(0xFFFFFFFF);
            time.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            statusBar.addView(time, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.RIGHT, 0, 0, 10, 0));

            labelView = new TextView(context);
            labelView.setText(LocaleController.getString(R.string.NotificationBarIconCaption));
            labelView.setTextColor(0xB3FFFFFF);
            labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            statusBar.addView(labelView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

            setOnLongClickListener(v -> {
                postTestNotification();
                return true;
            });
            updateSilhouette();
        }

        void updateSilhouette() {
            silhouetteView.setImageResource(LauncherIconController.resolveNotificationIconResId(NaConfig.INSTANCE.getNotificationIcon().Int()));
        }
    }

    private void postTestNotification() {
        Context context = ApplicationLoader.applicationContext;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("turbo_icon_test", "Turbo icon test", NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(channel);
        LauncherIconController.LauncherIcon previewIcon = LauncherIconController.getActiveIcon();
        boolean followAppIcon = NaConfig.INSTANCE.getNotificationIconAsAppIcon().Bool();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "turbo_icon_test")
                .setSmallIcon(followAppIcon ? previewIcon.notification : LauncherIconController.resolveNotificationIconResId(NaConfig.INSTANCE.getNotificationIcon().Int()))
                .setContentTitle(LocaleController.getString(R.string.AppName))
                .setContentText("Turbo")
                .setAutoCancel(true);
        nm.notify(TEST_NOTIFICATION_ID, builder.build());
    }

    private void registerEasterEggTap() {
        boolean unlocked = NaConfig.INSTANCE.getEasterEggUnlocked().Bool();
        AndroidUtilities.cancelRunOnUIThread(easterEggResetRunnable);
        easterEggTapCounter++;
        if (easterEggTapCounter >= EASTER_EGG_TAPS_TO_UNLOCK) {
            easterEggTapCounter = 0;
            if (unlocked) {
                lockEasterEgg();
            } else {
                unlockEasterEgg();
            }
            return;
        }
        if (!unlocked) {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, LocaleController.formatPluralString("EasterEggTapsLeft", EASTER_EGG_TAPS_TO_UNLOCK - easterEggTapCounter)).show();
        }
        AndroidUtilities.runOnUIThread(easterEggResetRunnable, EASTER_EGG_TAP_RESET_MS);
    }

    private void unlockEasterEgg() {
        NaConfig.INSTANCE.getEasterEggUnlocked().setConfigBool(true);
        if (appIconsSelectorCell != null) {
            appIconsSelectorCell.updateIconsVisibility();
            appIconsSelectorCell.scrollIconsToEnd();
        }
        BulletinFactory.of(this).createSimpleBulletin(R.raw.done, getString(R.string.EasterEggUnlockedToast)).show();
    }

    private void lockEasterEgg() {
        NaConfig.INSTANCE.getEasterEggUnlocked().setConfigBool(false);
        if (LauncherIconController.getActiveIcon().isHidden) {
            LauncherIconController.setIcon(LauncherIconController.LauncherIcon.TURBO);
            NotificationsController.rebuildAllAccounts();
        }
        if (appIconsSelectorCell != null) {
            appIconsSelectorCell.updateIconsVisibility();
        }
        BulletinFactory.of(this).createSimpleBulletin(R.raw.done, getString(R.string.EasterEggLockedToast)).show();
    }

    private class NotificationMarksCell extends FrameLayout {
        private final LinearLayout marksLayout;
        private final List<Integer> markValues = new ArrayList<>();
        private final List<View> markViews = new ArrayList<>();
        private final List<TextView> markNames = new ArrayList<>();
        private ImageView likeAppIcon;

        public NotificationMarksCell(Context context) {
            super(context);
            setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(12));

            HorizontalScrollView scrollView = new HorizontalScrollView(context);
            scrollView.setHorizontalScrollBarEnabled(false);
            addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            marksLayout = new LinearLayout(context);
            marksLayout.setOrientation(LinearLayout.HORIZONTAL);
            scrollView.addView(marksLayout, new FrameLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            likeAppIcon = addMark(context, 0, LocaleController.getString(R.string.NotificationIconLikeApp), 1);
            addMark(context, R.drawable.notification, LocaleController.getString(R.string.MapPreviewProviderTelegram), 0);
            addMark(context, R.drawable.neko_notification, LocaleController.getString(R.string.NekoX), 2);
            updateSelection();
        }

        private ImageView addMark(Context context, int iconRes, String name, int value) {
            LinearLayout chip = new LinearLayout(context);
            chip.setOrientation(LinearLayout.VERTICAL);
            chip.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
            chipParams.rightMargin = AndroidUtilities.dp(14);
            chip.setLayoutParams(chipParams);

            FrameLayout plate = new FrameLayout(context);
            GradientDrawable plateBg = new GradientDrawable();
            plateBg.setColor(0xFF101014);
            plateBg.setCornerRadius(AndroidUtilities.dp(12));
            plate.setBackground(plateBg);
            ImageView icon = new ImageView(context);
            if (iconRes != 0) {
                icon.setImageResource(iconRes);
            }
            plate.addView(icon, LayoutHelper.createFrame(22, 22, Gravity.CENTER));
            chip.addView(plate, new LinearLayout.LayoutParams(AndroidUtilities.dp(46), AndroidUtilities.dp(46)));

            TextView nameView = new TextView(context);
            nameView.setText(name);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
            nameView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            chip.addView(nameView, new LinearLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            plate.setOnClickListener(v -> {
                NaConfig.INSTANCE.getNotificationIcon().setConfigInt(value);
                NaConfig.INSTANCE.getNotificationIconAsAppIcon().setConfigBool(value == 1);
                NotificationsController.rebuildAllAccounts();
                updateSelection();
                updateNotificationPreview();
            });
            markValues.add(value);
            markViews.add(plate);
            markNames.add(nameView);
            marksLayout.addView(chip);
            return icon;
        }

        void updateSelection() {
            int selected = NaConfig.INSTANCE.getNotificationIconAsAppIcon().Bool() ? 1 : NaConfig.INSTANCE.getNotificationIcon().Int();
            LauncherIconController.LauncherIcon previewIcon = LauncherIconController.getActiveIcon();
            likeAppIcon.setImageResource(previewIcon.notification);
            for (int i = 0; i < markViews.size(); i++) {
                boolean isSelected = markValues.get(i) == selected;
                markViews.get(i).setScaleX(isSelected ? 1.1f : 1f);
                markViews.get(i).setScaleY(isSelected ? 1.1f : 1f);
                markViews.get(i).setAlpha(isSelected ? 1f : 0.55f);
                markNames.get(i).setTextColor(getThemedColor(isSelected ? Theme.key_windowBackgroundWhiteBlueText : Theme.key_windowBackgroundWhiteGrayText));
            }
        }
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        protected void onBindDefaultViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            CellGroup cellGroup = getCellGroup();
            if (cellGroup == null) return;
            AbstractConfigCell a = cellGroup.rows.get(position);
            if (a instanceof ConfigCellTextCheckIcon) {
                if (holder.itemView instanceof TextCell textCell) {
                    if (position == cellGroup.rows.indexOf(clearMessageDatabaseRow)) {
                        textCell.setColors(Theme.key_text_RedRegular, Theme.key_text_RedRegular);
                    }
                }
            }
        }

        @Override
        protected View onCreateCustomViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = null;
            if (viewType == ConfigCellCustom.CUSTOM_ITEM_AppIconPicker) {
                view = appIconsSelectorCell = new AppIconsSelectorCell(mContext, TurboSettingsActivity.this, UserConfig.selectedAccount);
            } else if (viewType == ConfigCellCustom.CUSTOM_ITEM_NotificationPreview) {
                view = notificationPreviewCell = new NotificationPreviewCell(mContext);
            } else if (viewType == ConfigCellCustom.CUSTOM_ITEM_NotificationMarksPicker) {
                view = notificationMarksCell = new NotificationMarksCell(mContext);
            } else if (viewType == ConfigCellCustom.CUSTOM_ITEM_InputBarPreview) {
                view = inputBarPreviewCell = new InputBarPreviewCell(mContext, getResourceProvider());
            } else if (viewType == ConfigCellCustom.CUSTOM_ITEM_InputBarTextSizeSlider) {
                view = new TextSizeSliderCell(mContext) {
                    @Override
                    protected int getCurrentSizeDp() {
                        int value = NaConfig.INSTANCE.getInputBarTextSizeValue().Int();
                        if (value <= 0) {
                            return Math.max(ChatActivityEnterView.INPUT_BAR_TEXT_SIZE_MIN_DP, Math.min(ChatActivityEnterView.INPUT_BAR_TEXT_SIZE_MAX_DP, SharedConfig.fontSize));
                        }
                        return Math.max(ChatActivityEnterView.INPUT_BAR_TEXT_SIZE_MIN_DP, Math.min(ChatActivityEnterView.INPUT_BAR_TEXT_SIZE_MAX_DP, value));
                    }

                    @Override
                    protected void onSizeSelected(int sizeDp, boolean isStopped) {
                        NaConfig.INSTANCE.getInputBarTextSizeValue().setConfigInt(sizeDp);
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.inputBarTextSizeChanged);
                    }
                };
            } else if (viewType == ConfigCellCustom.CUSTOM_ITEM_SaveDeletedCategories) {
                view = saveDeletedCategoriesCell = new SaveDeletedCategoriesCell(mContext);
            } else if (viewType == ConfigCellCustom.CUSTOM_ITEM_FontRegular ||
                    viewType == ConfigCellCustom.CUSTOM_ITEM_FontBold ||
                    viewType == ConfigCellCustom.CUSTOM_ITEM_FontItalic ||
                    viewType == ConfigCellCustom.CUSTOM_ITEM_FontMono) {
                view = new TextSettingsCell(mContext);
            } else if (viewType == ConfigCellCustom.CUSTOM_ITEM_FontReset) {
                view = new TextCell(mContext);
            }
            return view;
        }

        @Override
        protected void onBindCustomViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.itemView instanceof SaveDeletedCategoriesCell saveDeletedCell) {
                saveDeletedCell.bindStates();
            } else if (holder.itemView instanceof TextSettingsCell textCell) {
                String category = null;
                String title = null;
                if (position == cellGroup.rows.indexOf(fontRegularRow)) {
                    category = TypefaceHelper.FONT_CATEGORY_REGULAR;
                    title = getString(R.string.FontCategoryRegular);
                } else if (position == cellGroup.rows.indexOf(fontBoldRow)) {
                    category = TypefaceHelper.FONT_CATEGORY_BOLD;
                    title = getString(R.string.FontCategoryBold);
                } else if (position == cellGroup.rows.indexOf(fontItalicRow)) {
                    category = TypefaceHelper.FONT_CATEGORY_ITALIC;
                    title = getString(R.string.FontCategoryItalic);
                } else if (position == cellGroup.rows.indexOf(fontMonoRow)) {
                    category = TypefaceHelper.FONT_CATEGORY_MONO;
                    title = getString(R.string.FontCategoryMono);
                }
                if (category != null) {
                    String fontName = TypefaceHelper.getCustomFontName(category);
                    textCell.setTextAndValue(title, fontName.isEmpty() ? getString(R.string.FontDefault) : fontName, true);
                }
            } else if (holder.itemView instanceof TextCell textCell) {
                if (position == cellGroup.rows.indexOf(fontResetRow)) {
                    textCell.setText(getString(R.string.FontReset), true);
                    textCell.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                }
            }
        }
    }
}
