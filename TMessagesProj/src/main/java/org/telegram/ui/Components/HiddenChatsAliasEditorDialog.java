package org.telegram.ui.Components;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.messenger.R;

import java.util.function.Consumer;

import tw.nekomimi.nekogram.helpers.HiddenChatsHelper;

public final class HiddenChatsAliasEditorDialog {

    private HiddenChatsAliasEditorDialog() {
    }

    public static void show(BaseFragment fragment, String title, String preset, Consumer<String> onSave) {
        Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 20, 18, 20, 0));

        TextView messageView = new TextView(context);
        messageView.setText(LocaleController.getString(R.string.HiddenChatsAliasHint));
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        messageView.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        messageView.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(messageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 20, 6, 20, 0));

        EditTextBoldCursor input = new EditTextBoldCursor(context);
        input.setTextSize(16);
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        input.setHintText(LocaleController.getString(R.string.HiddenChatsAliasInputHint));
        input.setText(preset == null ? "" : preset);
        input.setSelection(input.length());
        content.addView(input, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 20, 10, 20, 0));

        TextView suggestButton = new TextView(context);
        suggestButton.setText(LocaleController.getString(R.string.HiddenChatsAliasSuggest));
        suggestButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        suggestButton.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
        suggestButton.setGravity(Gravity.CENTER_HORIZONTAL);
        suggestButton.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), Theme.RIPPLE_MASK_CIRCLE_TO_BOUND_EDGE));
        suggestButton.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        suggestButton.setOnClickListener(v -> {
            input.setText(HiddenChatsHelper.randomAlias());
            input.setSelection(input.length());
        });
        content.addView(suggestButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(content);
        AlertDialog dialog = builder.create();

        LinearLayout bottomRow = new LinearLayout(context);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView cancelButton = new TextView(context);
        cancelButton.setText(LocaleController.getString(R.string.Cancel));
        cancelButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        cancelButton.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        cancelButton.setGravity(Gravity.CENTER_HORIZONTAL);
        cancelButton.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), Theme.RIPPLE_MASK_CIRCLE_TO_BOUND_EDGE));
        cancelButton.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        bottomRow.addView(cancelButton, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        TextView saveButton = new TextView(context);
        saveButton.setText(LocaleController.getString(R.string.HiddenChatsAliasSave));
        saveButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        saveButton.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
        saveButton.setGravity(Gravity.CENTER_HORIZONTAL);
        saveButton.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), Theme.RIPPLE_MASK_CIRCLE_TO_BOUND_EDGE));
        saveButton.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        saveButton.setOnClickListener(v -> {
            onSave.accept(input.getText().toString().trim());
            dialog.dismiss();
        });
        bottomRow.addView(saveButton, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        content.addView(bottomRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 12));

        dialog.show();
        input.requestFocus();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }
}
