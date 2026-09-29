package xyz.nextalone.nagram.helper;

import android.app.Activity;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

public class ForwardEditedTextSender {

    public static void send(Activity activity, Theme.ResourcesProvider resourcesProvider, int currentAccount,
                            ArrayList<MessageObject> messages, ArrayList<CopySendQueue.CopySendQueueTarget> targets,
                            CharSequence comment, ArrayList<TLRPC.MessageEntity> commentEntities,
                            MessageObject editableMessage, CharSequence editedText, ArrayList<TLRPC.MessageEntity> editedEntities,
                            boolean notify, int scheduleDate, int scheduleRepeatPeriod) {
        if (activity == null) {
            return;
        }
        String editedTextString = editedText != null ? editedText.toString() : null;
        boolean hasComment = comment != null && comment.length() > 0;
        int enqueueResult = CopySendQueue.getInstance(currentAccount).enqueue(activity, messages, targets,
                hasComment ? comment : null, commentEntities, editableMessage, editedTextString, editedEntities,
                notify, scheduleDate, scheduleRepeatPeriod);
        if (enqueueResult == CopySendQueue.RESULT_FAILED_NOW) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity, resourcesProvider);
            builder.setMessage(LocaleController.getString(R.string.PleaseDownload));
            builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
            builder.show();
        }
    }
}
