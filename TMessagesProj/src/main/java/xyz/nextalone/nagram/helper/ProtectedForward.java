package xyz.nextalone.nagram.helper;

import android.content.Context;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.DialogsActivity;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.MessageHelper;
import xyz.nextalone.nagram.NaConfig;

public class ProtectedForward {

    public static final int FORWARD_PROTECTED_ASK = NaConfig.FORWARD_PROTECTED_ASK;
    public static final int FORWARD_PROTECTED_ALWAYS = NaConfig.FORWARD_PROTECTED_ALWAYS;
    public static final int FORWARD_PROTECTED_NEVER = NaConfig.FORWARD_PROTECTED_NEVER;

    public static int getMode() {
        return NaConfig.INSTANCE.getForwardProtectedMode().Int();
    }

    public static boolean containsProtected(ArrayList<MessageObject> messages) {
        if (messages == null) {
            return false;
        }
        for (int i = 0; i < messages.size(); i++) {
            MessageObject message = messages.get(i);
            if (message == null) {
                continue;
            }
            if (message.messageOwner != null && message.messageOwner.noforwards) {
                return true;
            }
            if (MessagesController.getInstance(message.currentAccount).isPeerNoForwards(message.getDialogId())) {
                return true;
            }
        }
        return false;
    }

    public static void handleProtectedForward(Context context, Theme.ResourcesProvider resourcesProvider, int messagesCount, Runnable onAllowed) {
        int mode = getMode();
        if (mode == FORWARD_PROTECTED_NEVER) {
            showDisabledDialog(context, resourcesProvider);
            return;
        }
        if (mode == FORWARD_PROTECTED_ALWAYS) {
            onAllowed.run();
            return;
        }
        showAskDialog(context, resourcesProvider, messagesCount, onAllowed);
    }

    public static boolean shouldBypassForwardRestriction(int account, ArrayList<MessageObject> selectedMessages) {
        return getMode() != FORWARD_PROTECTED_NEVER && MessageHelper.getInstance(account).canSendMessagesAsCopy(selectedMessages);
    }

    public static void forwardProtected(ChatActivity chatActivity, ArrayList<MessageObject> messages, ArrayList<MessagesStorage.TopicKey> dids, CharSequence comment, boolean notify, int scheduleDate, int scheduleRepeatPeriod, DialogsActivity fragment) {
        int mode = getMode();
        if (mode == FORWARD_PROTECTED_NEVER) {
            if (fragment != null) {
                fragment.finishFragment();
            }
            BulletinFactory.of(chatActivity).createSimpleBulletin(R.raw.info, LocaleController.getString(R.string.ForwardProtectedDisabled)).show();
            return;
        }
        if (mode == FORWARD_PROTECTED_ALWAYS) {
            sendCopies(chatActivity, messages, dids, comment, notify, scheduleDate, scheduleRepeatPeriod, fragment);
            return;
        }
        showAskDialog(chatActivity.getParentActivity(), chatActivity.getResourceProvider(), messages.size(), () -> sendCopies(chatActivity, messages, dids, comment, notify, scheduleDate, scheduleRepeatPeriod, fragment));
    }

    private static void showAskDialog(Context context, Theme.ResourcesProvider resourcesProvider, int messagesCount, Runnable onForward) {
        if (context == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString(messagesCount > 1 ? R.string.ForwardProtectedDialogTitleMany : R.string.ForwardProtectedDialogTitle));
        builder.setMessage(LocaleController.getString(messagesCount > 1 ? R.string.ForwardProtectedDialogTextMany : R.string.ForwardProtectedDialogText));
        builder.setPositiveButton(LocaleController.getString(R.string.ForwardProtectedOnce), (dialog, which) -> onForward.run());
        builder.setNegativeButton(LocaleController.getString(R.string.ForwardProtectedAlways), (dialog, which) -> {
            NaConfig.INSTANCE.getForwardProtectedMode().setConfigInt(FORWARD_PROTECTED_ALWAYS);
            onForward.run();
        });
        builder.setNeutralButton(LocaleController.getString(R.string.ForwardProtectedNever), (dialog, which) -> NaConfig.INSTANCE.getForwardProtectedMode().setConfigInt(FORWARD_PROTECTED_NEVER));
        builder.show();
    }

    private static void showDisabledDialog(Context context, Theme.ResourcesProvider resourcesProvider) {
        if (context == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setMessage(LocaleController.getString(R.string.ForwardProtectedDisabled));
        builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        builder.show();
    }

    private static void sendCopies(ChatActivity chatActivity, ArrayList<MessageObject> messages, ArrayList<MessagesStorage.TopicKey> dids, CharSequence comment, boolean notify, int scheduleDate, int scheduleRepeatPeriod, DialogsActivity fragment) {
        if (chatActivity.forwardingMessage != null) {
            chatActivity.forwardingMessage = null;
            chatActivity.forwardingMessageGroup = null;
        } else {
            for (int a = 1; a >= 0; a--) {
                chatActivity.selectedMessagesCanCopyIds[a].clear();
                chatActivity.selectedMessagesCanStarIds[a].clear();
                chatActivity.selectedMessagesIds[a].clear();
            }
            chatActivity.hideActionMode();
            chatActivity.updatePinnedMessageView(true);
            chatActivity.updateVisibleRows();
        }
        chatActivity.messagePreviewParams = null;
        chatActivity.hideFieldPanel(false);
        int account = chatActivity.getCurrentAccount();
        boolean hasSentAny = false;
        for (int a = 0; a < dids.size(); a++) {
            MessagesStorage.TopicKey topicKey = dids.get(a);
            long did = topicKey.dialogId;
            boolean isMonoForum = MessagesController.getInstance(account).isMonoForum(did);
            TLRPC.TL_forumTopic topic = topicKey.topicId != 0 ? MessagesController.getInstance(account).getTopicsController().findTopic(-did, topicKey.topicId) : null;
            long monoForumPeerId = topic != null && isMonoForum ? DialogObject.getPeerDialogId(topic.from_id) : 0;
            MessageObject replyTopMsg = topic != null && !isMonoForum ? new MessageObject(account, topic.topicStartMessage, false, false) : null;
            if (replyTopMsg != null) {
                replyTopMsg.isTopicMainMessage = true;
            }
            if (comment != null && !NekoConfig.sendCommentAfterForward.Bool()) {
                sendComment(chatActivity, comment, did, replyTopMsg, monoForumPeerId, notify, scheduleDate, scheduleRepeatPeriod);
            }
            if (MessageHelper.getInstance(account).sendMessagesAsCopy(messages, did, null, replyTopMsg, null, notify, scheduleDate, 0, null, 0, 0, monoForumPeerId, null)) {
                hasSentAny = true;
            }
            if (comment != null && NekoConfig.sendCommentAfterForward.Bool()) {
                sendComment(chatActivity, comment, did, replyTopMsg, monoForumPeerId, notify, scheduleDate, scheduleRepeatPeriod);
            }
        }
        if (!hasSentAny) {
            BulletinFactory.of(chatActivity).createErrorBulletin(LocaleController.getString(R.string.PleaseDownload), chatActivity.getResourceProvider()).show();
        }
        if (fragment != null) {
            fragment.finishFragment();
        }
        if (hasSentAny) {
            chatActivity.showForwardedFeedback(dids, messages.size());
        }
    }

    private static void sendComment(ChatActivity chatActivity, CharSequence comment, long did, MessageObject replyTopMsg, long monoForumPeerId, boolean notify, int scheduleDate, int scheduleRepeatPeriod) {
        SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(comment.toString(), did, null, replyTopMsg, null, true, null, null, null, notify, scheduleDate, scheduleRepeatPeriod, null, false);
        params.monoForumPeer = monoForumPeerId;
        SendMessagesHelper.getInstance(chatActivity.getCurrentAccount()).sendMessage(params);
    }
}
