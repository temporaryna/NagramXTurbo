package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.LongSparseArray;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

public class MemberRequestCell extends FrameLayout {

    private final AvatarDrawable avatarDrawable = new AvatarDrawable();
    private final BackupImageView avatarImageView = new BackupImageView(getContext());
    private final SimpleTextView nameTextView = new SimpleTextView(getContext());
    private final SimpleTextView statusTextView = new SimpleTextView(getContext());
    private final TextView addButton;
    private final TextView dismissButton;
    private final TextView banButton;

    private TLRPC.TL_chatInviteImporter importer;
    private boolean isNeedDivider;

    public MemberRequestCell(@NonNull Context context, OnClickListener clickListener, boolean isChannel) {
        super(context);

        avatarImageView.setRoundRadius(AndroidUtilities.dp(23));
        avatarImageView.setOnClickListener(v -> {
            if (clickListener != null && importer != null) {
                clickListener.onAvatarClicked(this);
            }
        });
        addView(avatarImageView, LayoutHelper.createFrame(46, 46, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 12, 8, 12, 0));

        nameTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        nameTextView.setMaxLines(1);
        ThemeBind.textColor(nameTextView, Theme.key_windowBackgroundWhiteBlackText);
        nameTextView.setTextSize(17);
        nameTextView.setTypeface(AndroidUtilities.bold());
        addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, LocaleController.isRTL ? 12 : 74, 12, LocaleController.isRTL ? 74 : 12, 0));

        statusTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        statusTextView.setMaxLines(1);
        ThemeBind.textColor(statusTextView, Theme.key_windowBackgroundWhiteGrayText);
        statusTextView.setTextSize(14);
        addView(statusTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, LocaleController.isRTL ? 12 : 74, 36, LocaleController.isRTL ? 74 : 12, 0));

        int btnPadding = AndroidUtilities.dp(17);
        addButton = new TextView(getContext());
        addButton.setBackground(Theme.AdaptiveRipple.filledRectByKey(Theme.key_featuredStickers_addButton, 16));
        addButton.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        addButton.setMaxLines(1);
        addButton.setPadding(btnPadding, 0, btnPadding, 0);
        addButton.setText(isChannel ? LocaleController.getString(R.string.AddToChannel) : LocaleController.getString(R.string.AddToGroup));
        ThemeBind.textColor(addButton, Theme.key_featuredStickers_buttonText);
        addButton.setTextSize(14);
        addButton.setTypeface(AndroidUtilities.bold());
        addButton.setOnClickListener(v -> {
            if (clickListener != null && importer != null) {
                clickListener.onAddClicked(importer);
            }
        });
        addView(addButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 32, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, LocaleController.isRTL ? 0 : 73, 62, LocaleController.isRTL ? 73 : 0, 0));

        float addButtonWidth = addButton.getPaint().measureText(addButton.getText().toString()) + btnPadding * 2;
        dismissButton = new TextView(getContext());
        dismissButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(16), Color.TRANSPARENT, Theme.getColor(Theme.key_listSelector), 0xff000000));
        dismissButton.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        dismissButton.setMaxLines(1);
        dismissButton.setPadding(btnPadding, 0, btnPadding, 0);
        dismissButton.setText(LocaleController.getString(R.string.Dismiss));
        ThemeBind.textColor(dismissButton, Theme.key_windowBackgroundWhiteBlueText);
        dismissButton.setTextSize(14);
        dismissButton.setTypeface(AndroidUtilities.bold());
        dismissButton.setOnClickListener(v -> {
            if (clickListener != null && importer != null) {
                clickListener.onDismissClicked(importer);
            }
        });
        FrameLayout.LayoutParams dismissLayoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, AndroidUtilities.dp(32), LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        dismissLayoutParams.topMargin = AndroidUtilities.dp(62);
        dismissLayoutParams.leftMargin = LocaleController.isRTL ? 0 : (int)(addButtonWidth + AndroidUtilities.dp(73 + 6));
        dismissLayoutParams.rightMargin = LocaleController.isRTL ? (int)(addButtonWidth + AndroidUtilities.dp(73 + 6)) : 0;
        addView(dismissButton, dismissLayoutParams);

        banButton = new TextView(getContext());
        banButton.setBackground(Theme.AdaptiveRipple.filledRectByKey(Theme.key_text_RedRegular, 16));
        banButton.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        banButton.setMaxLines(1);
        banButton.setPadding(btnPadding, 0, btnPadding, 0);
        banButton.setText(LocaleController.getString(R.string.Ban));
        banButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        banButton.setTextSize(14);
        banButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        banButton.setOnClickListener(v -> {
            if (clickListener != null && importer != null) {
                clickListener.onBanClicked(importer);
            }
        });
        FrameLayout.LayoutParams banLayoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, AndroidUtilities.dp(32), LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT);
        banLayoutParams.topMargin = AndroidUtilities.dp(62);
        banLayoutParams.leftMargin = LocaleController.isRTL ? AndroidUtilities.dp(24) : 0;
        banLayoutParams.rightMargin = LocaleController.isRTL ? 0 : AndroidUtilities.dp(24);
        addView(banButton, banLayoutParams);
    }

    public void setData(LongSparseArray<TLRPC.User> users, TLRPC.TL_chatInviteImporter importer, boolean isNeedDivider) {
        this.importer = importer;
        this.isNeedDivider = isNeedDivider;
        setWillNotDraw(!isNeedDivider);

        TLRPC.User user = users.get(importer.user_id);
        avatarDrawable.setInfo(user);
        avatarImageView.setForUserOrChat(user, avatarDrawable);
        nameTextView.setText(UserObject.getUserName(user));
        String dateText = LocaleController.formatDateAudio(importer.date, false);
        if (importer.via_chatlist) {
            statusTextView.setText(LocaleController.getString(R.string.JoinedViaFolder));
        } else if (importer.approved_by == 0) {
            statusTextView.setText(LocaleController.formatString("RequestedToJoinAt", R.string.RequestedToJoinAt, dateText));
        } else {
            TLRPC.User approvedByUser = users.get(importer.approved_by);
            if (approvedByUser != null) {
                statusTextView.setText(LocaleController.formatString("AddedBy", R.string.AddedBy, UserObject.getFirstName(approvedByUser), dateText));
            } else {
                statusTextView.setText("");
            }
        }
    }

    public TLRPC.TL_chatInviteImporter getImporter() {
        return importer;
    }

    public BackupImageView getAvatarImageView() {
        return avatarImageView;
    }

    public String getStatus() {
        return statusTextView.getText().toString();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final boolean rtl = LocaleController.isRTL;
        final int rowHeight = AndroidUtilities.dp(32);
        final int rowGap = AndroidUtilities.dp(6);
        final int buttonsTop = AndroidUtilities.dp(62);
        final int edgeMargin = AndroidUtilities.dp(24);

        final int loose = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        addButton.measure(loose, loose);
        dismissButton.measure(loose, loose);
        banButton.measure(loose, loose);
        final int addWidth = addButton.getMeasuredWidth();
        final int dismissWidth = dismissButton.getMeasuredWidth();
        final int banWidth = banButton.getMeasuredWidth();

        final int avail = width - AndroidUtilities.dp(73) - edgeMargin;
        final boolean singleRow = addWidth + dismissWidth + banWidth + rowGap * 2 <= avail;
        final boolean banFitsWithAdd = addWidth + rowGap + banWidth <= avail;
        final int rows = singleRow ? 1 : banFitsWithAdd ? 2 : 3;
        final int contentLeft = AndroidUtilities.dp(73);
        final int finalAddWidth;
        final int finalBanWidth;
        if (singleRow || !banFitsWithAdd) {
            finalAddWidth = banFitsWithAdd ? addWidth : avail;
            finalBanWidth = banFitsWithAdd ? banWidth : avail;
        } else {
            final int extra = avail - rowGap - addWidth - banWidth;
            finalAddWidth = addWidth + Math.round(extra * (float) addWidth / (addWidth + banWidth));
            finalBanWidth = avail - rowGap - finalAddWidth;
        }

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) addButton.getLayoutParams();
        lp.width = singleRow ? ViewGroup.LayoutParams.WRAP_CONTENT : finalAddWidth;
        lp.topMargin = buttonsTop;
        if (rtl) {
            lp.rightMargin = contentLeft;
            lp.leftMargin = singleRow || banFitsWithAdd ? 0 : edgeMargin;
        } else {
            lp.leftMargin = contentLeft;
            lp.rightMargin = singleRow || banFitsWithAdd ? 0 : edgeMargin;
        }

        lp = (FrameLayout.LayoutParams) dismissButton.getLayoutParams();
        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.topMargin = singleRow ? buttonsTop : buttonsTop + (rows - 1) * (rowHeight + rowGap);
        final int afterAdd = contentLeft + addWidth + rowGap;
        final int dismissCenter = contentLeft + (avail - dismissWidth) / 2;
        if (rtl) {
            lp.rightMargin = singleRow ? afterAdd : dismissCenter;
            lp.leftMargin = 0;
        } else {
            lp.leftMargin = singleRow ? afterAdd : dismissCenter;
            lp.rightMargin = 0;
        }

        lp = (FrameLayout.LayoutParams) banButton.getLayoutParams();
        lp.width = singleRow ? ViewGroup.LayoutParams.WRAP_CONTENT : finalBanWidth;
        lp.topMargin = singleRow || banFitsWithAdd ? buttonsTop : buttonsTop + rowHeight + rowGap;
        if (rtl) {
            lp.leftMargin = singleRow || !banFitsWithAdd ? edgeMargin : 0;
            lp.rightMargin = singleRow ? 0 : banFitsWithAdd ? contentLeft + finalAddWidth + rowGap : contentLeft;
        } else {
            lp.rightMargin = singleRow || !banFitsWithAdd ? edgeMargin : 0;
            lp.leftMargin = singleRow ? 0 : banFitsWithAdd ? contentLeft + finalAddWidth + rowGap : contentLeft;
        }

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(buttonsTop + rows * rowHeight + (rows - 1) * rowGap + AndroidUtilities.dp(13), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isNeedDivider) {
            canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(72), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(72) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
        }
    }


    public interface OnClickListener {

        void onAddClicked(TLRPC.TL_chatInviteImporter importer);

        void onDismissClicked(TLRPC.TL_chatInviteImporter importer);

        void onBanClicked(TLRPC.TL_chatInviteImporter importer);

        void onAvatarClicked(MemberRequestCell cell);
    }
}
