package xyz.nextalone.nagram.helper;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.Components.ChatActivityEnterView;

public class ForwardTextEditMode {

    @Nullable
    private ChatActivityEnterView field;
    @Nullable
    private TextView copyNotice;
    @Nullable
    private MessageObject editableMessage;
    private CharSequence savedFieldText;

    public boolean isEntered() {
        return editableMessage != null && field != null;
    }

    @Nullable
    public MessageObject getEditableMessage() {
        return editableMessage;
    }

    public void enter(ChatActivityEnterView enterField, @Nullable TextView notice, MessageObject message) {
        if (isEntered() || message == null || enterField == null) {
            return;
        }
        field = enterField;
        copyNotice = notice;
        editableMessage = message;
        savedFieldText = enterField.getFieldText();
        CharSequence forwardText = ForwardTextEdit.getForwardText(message);
        enterField.setFieldText(forwardText != null ? forwardText : "");
        enterField.messageEditText.setHintText(LocaleController.getString(R.string.ForwardTextPlaceholder), false);
        if (copyNotice != null) {
            copyNotice.setVisibility(View.VISIBLE);
        }
        enterField.messageEditText.requestFocus();
        AndroidUtilities.showKeyboard(enterField.messageEditText);
        enterField.messageEditText.post(() -> {
            if (field != enterField) {
                return;
            }
            int length = enterField.messageEditText.getText().length();
            enterField.messageEditText.setSelection(length, length);
        });
    }

    public void exit() {
        if (!isEntered() || field == null) {
            return;
        }
        field.setFieldText(savedFieldText != null ? savedFieldText : "");
        resetModeState();
    }

    public void clear() {
        if (!isEntered() || field == null) {
            return;
        }
        field.setFieldText("");
        resetModeState();
    }

    public boolean isTextChanged() {
        if (!isEntered() || field == null) {
            return false;
        }
        CharSequence fieldText = field.getFieldText();
        return ForwardTextEdit.hasForwardTextChanged(fieldText != null ? fieldText.toString() : "", editableMessage);
    }

    @Nullable
    public CharSequence getEditedText() {
        return isEntered() && field != null ? field.getFieldText() : null;
    }

    @Nullable
    public CharSequence getSavedText() {
        return isEntered() ? savedFieldText : null;
    }

    public void flashCopyNotice() {
        if (copyNotice == null) {
            return;
        }
        copyNotice.animate().cancel();
        copyNotice.animate().alpha(0.2f).setDuration(120).withEndAction(() ->
                copyNotice.animate().alpha(1.0f).setDuration(180).start()).start();
    }

    private void resetModeState() {
        if (field != null) {
            field.updateFieldHint(false);
        }
        if (copyNotice != null) {
            copyNotice.setVisibility(View.GONE);
        }
        field = null;
        copyNotice = null;
        editableMessage = null;
        savedFieldText = null;
    }
}
