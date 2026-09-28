package xyz.nextalone.nagram.helper;

import android.app.Activity;

import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

import tw.nekomimi.nekogram.helpers.MessageHelper;

public class ForwardEditedTextSender {

    public static void send(Activity activity, Theme.ResourcesProvider resourcesProvider, int currentAccount,
                            ArrayList<MessageObject> messages, ArrayList<CopySendQueue.CopySendQueueTarget> targets,
                            CharSequence comment, ArrayList<TLRPC.MessageEntity> commentEntities,
                            MessageObject editableMessage, CharSequence editedText, ArrayList<TLRPC.MessageEntity> editedEntities,
                            boolean notify, int scheduleDate, int scheduleRepeatPeriod) {
        String editedTextString = editedText != null ? editedText.toString() : null;
        if (!BuildVars.TURBO_BASE && ProtectedForward.containsProtected(messages)
                && MessageHelper.getInstance(currentAccount).canSendMessagesAsCopy(messages)) {
            ProtectedForward.handleProtectedForward(activity, resourcesProvider, messages.size(), () ->
                    enqueue(activity, resourcesProvider, currentAccount, messages, targets, comment, commentEntities, editableMessage, editedTextString, editedEntities, notify, scheduleDate, scheduleRepeatPeriod));
        } else {
            enqueue(activity, resourcesProvider, currentAccount, messages, targets, comment, commentEntities, editableMessage, editedTextString, editedEntities, notify, scheduleDate, scheduleRepeatPeriod);
        }
    }

    private static void enqueue(Activity activity, Theme.ResourcesProvider resourcesProvider, int currentAccount,
                                ArrayList<MessageObject> messages, ArrayList<CopySendQueue.CopySendQueueTarget> targets,
                                CharSequence comment, ArrayList<TLRPC.MessageEntity> commentEntities,
                                MessageObject editableMessage, String editedText, ArrayList<TLRPC.MessageEntity> editedEntities,
                                boolean notify, int scheduleDate, int scheduleRepeatPeriod) {
        boolean hasComment = comment != null && comment.length() > 0;
        int enqueueResult = CopySendQueue.getInstance(currentAccount).enqueue(activity, messages, targets,
                hasComment ? comment : null, commentEntities, editableMessage, editedText, editedEntities,
                notify, scheduleDate, scheduleRepeatPeriod);
        if (enqueueResult == CopySendQueue.RESULT_FAILED_NOW) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity, resourcesProvider);
            builder.setMessage(LocaleController.getString(R.string.PleaseDownload));
            builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
            builder.show();
        }
    }
}
