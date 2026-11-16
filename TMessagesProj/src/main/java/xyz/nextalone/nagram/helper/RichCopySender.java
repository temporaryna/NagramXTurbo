package xyz.nextalone.nagram.helper;

import android.text.TextUtils;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.FileRefController;
import org.telegram.messenger.MessageSuggestionParams;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_iv;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.Components.Premium.PremiumFeatureBottomSheet;
import org.telegram.ui.PremiumPreviewFragment;
import org.telegram.ui.iv.RichEditor;

import java.util.ArrayList;

import android.app.Activity;

public class RichCopySender {

    private static final long REFRESH_TIMEOUT_MS = 30000;

    public static boolean interceptForwardForCopy(int currentAccount, ArrayList<MessageObject> messages, long peer,
                                                  boolean forwardFromMyName, boolean hideCaption, boolean notify,
                                                  int scheduleDate, int scheduleRepeatPeriod, MessageObject replyToTopMsg,
                                                  int videoTimestamp, long payStars, long monoForumPeerId,
                                                  MessageSuggestionParams suggestionParams) {
        if (BuildVars.TURBO_BASE || !forwardFromMyName || hideCaption || MessagesController.getInstance(currentAccount).richEditorAllowed()) {
            return false;
        }
        boolean hasRichPost = false;
        for (int a = 0; a < messages.size(); a++) {
            MessageObject m = messages.get(a);
            if (m != null && m.messageOwner != null && m.messageOwner.rich_message != null) {
                hasRichPost = true;
                break;
            }
        }
        Activity activity = LaunchActivity.instance;
        if (!hasRichPost || activity == null || activity.isFinishing()) {
            return false;
        }
        RichEditor.openConversionSheet(activity, () -> {
            ArrayList<MessageObject> plainMessages = new ArrayList<>();
            for (int a = 0; a < messages.size(); a++) {
                MessageObject m = messages.get(a);
                if (m != null && m.messageOwner != null && m.messageOwner.rich_message != null) {
                    sendRichAsText(currentAccount, m.messageOwner.rich_message, m, peer, notify, scheduleDate, monoForumPeerId);
                } else if (m != null) {
                    plainMessages.add(m);
                }
            }
            if (!plainMessages.isEmpty()) {
                SendMessagesHelper.getInstance(currentAccount).sendMessage(plainMessages, peer, forwardFromMyName, hideCaption, notify, scheduleDate, scheduleRepeatPeriod, replyToTopMsg, videoTimestamp, payStars, monoForumPeerId, suggestionParams);
            }
        }, () -> new PremiumFeatureBottomSheet(activity, PremiumPreviewFragment.PREMIUM_FEATURE_RICH_EDITOR, true, null).show(), null);
        return true;
    }

    public static boolean sendRichCopy(int currentAccount, MessageObject messageObject,
                                       long targetDialogId, MessageObject replyTopMsg,
                                       boolean withSound, int scheduleDate, long monoForumPeerId) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return false;
        }
        return sendRichCopy(currentAccount, messageObject.messageOwner.rich_message, messageObject, targetDialogId, replyTopMsg, withSound, scheduleDate, monoForumPeerId);
    }

    public static boolean sendRichCopy(int currentAccount, TL_iv.RichMessage richMessage, MessageObject refreshParent,
                                       long targetDialogId, MessageObject replyTopMsg,
                                       boolean withSound, int scheduleDate, long monoForumPeerId) {
        if (richMessage == null || refreshParent == null || refreshParent.messageOwner == null) {
            return false;
        }
        TLRPC.InputPeer peer = MessagesController.getInstance(currentAccount).getInputPeer(targetDialogId);
        if (peer == null) {
            return false;
        }
        TLRPC.TL_messages_sendMessage request = buildRequest(currentAccount, richMessage, refreshParent.messageOwner.invert_media, peer, replyTopMsg, withSound, scheduleDate, monoForumPeerId);
        if (request == null) {
            return false;
        }
        sendWithRefresh(currentAccount, refreshParent, richMessage, request, targetDialogId, withSound, scheduleDate, monoForumPeerId);
        return true;
    }

    private static TLRPC.TL_messages_sendMessage buildRequest(int currentAccount, TL_iv.RichMessage richMessage, boolean invertMedia,
                                                              TLRPC.InputPeer peer, MessageObject replyTopMsg,
                                                              boolean withSound, int scheduleDate, long monoForumPeerId) {
        try {
            TL_iv.TL_inputRichMessage inputRich = SendMessagesHelper.richMessageToInputRichMessage(richMessage, null);
            TLRPC.TL_messages_sendMessage request = new TLRPC.TL_messages_sendMessage();
            request.message = "";
            request.no_webpage = true;
            request.peer = peer;
            request.random_id = SendMessagesHelper.getInstance(currentAccount).getNextRandomId();
            request.silent = !withSound;
            request.rich_message = inputRich;
            request.invert_media = invertMedia;
            request.reply_to = CopyReferenceSender.buildInputReplyTo(currentAccount, replyTopMsg, monoForumPeerId);
            if (request.reply_to != null) {
                request.flags |= 1;
            }
            if (scheduleDate != 0) {
                request.schedule_date = scheduleDate;
                request.flags |= 1024;
            }
            return request;
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    private static void sendWithRefresh(int currentAccount, MessageObject messageObject, TL_iv.RichMessage richMessage, TLRPC.TL_messages_sendMessage request,
                                        long targetDialogId, boolean withSound, int scheduleDate, long monoForumPeerId) {
        boolean[] fallbackHandled = {false};
        boolean[] retryDispatched = {false};
        int[] pendingRefresh = {0};
        Runnable finalFallback = () -> AndroidUtilities.runOnUIThread(() -> {
            if (fallbackHandled[0]) {
                return;
            }
            fallbackHandled[0] = true;
            sendRichAsText(currentAccount, richMessage, messageObject, targetDialogId, withSound, scheduleDate, monoForumPeerId);
        });
        sendRequest(currentAccount, request, (response, error) -> {
            if (error == null || !FileRefController.isFileRefError(error.text)) {
                if (error != null) {
                    FileLog.d("rich copy send failed: " + error.text);
                    finalFallback.run();
                }
                return;
            }
            ArrayList<TLObject> inputs = new ArrayList<>();
            if (request.rich_message.photos != null) {
                inputs.addAll(request.rich_message.photos);
            }
            if (request.rich_message.documents != null) {
                inputs.addAll(request.rich_message.documents);
            }
            if (inputs.isEmpty()) {
                finalFallback.run();
                return;
            }
            pendingRefresh[0] = inputs.size();
            Runnable retryWithGuard = () -> {
                if (fallbackHandled[0] || retryDispatched[0]) {
                    return;
                }
                if (pendingRefresh[0] > 0 && --pendingRefresh[0] > 0) {
                    return;
                }
                retryDispatched[0] = true;
                AndroidUtilities.cancelRunOnUIThread(finalFallback);
                sendRequest(currentAccount, request, (response2, error2) -> {
                    if (error2 != null) {
                        FileLog.d("rich copy retry failed: " + error2.text);
                        finalFallback.run();
                    }
                });
            };
            for (int i = 0; i < inputs.size(); i++) {
                FileRefController.getInstance(currentAccount).requestReference(messageObject, inputs.get(i), (Runnable) retryWithGuard);
            }
            AndroidUtilities.runOnUIThread(finalFallback, REFRESH_TIMEOUT_MS);
        });
    }

    public static void sendRichAsText(int currentAccount, MessageObject messageObject, long dialogId, boolean withSound, int scheduleDate, long monoForumPeerId) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return;
        }
        sendRichAsText(currentAccount, messageObject.messageOwner.rich_message, messageObject, dialogId, withSound, scheduleDate, monoForumPeerId);
    }

    public static void sendRichAsText(int currentAccount, TL_iv.RichMessage richMessage, MessageObject mediaSource, long dialogId, boolean withSound, int scheduleDate, long monoForumPeerId) {
        CharSequence text = org.telegram.ui.iv.RichMessageConvert.toCharSequence(richMessage);
        if (TextUtils.isEmpty(text)) {
            sendRichMediaOnly(currentAccount, mediaSource, dialogId, null, withSound, scheduleDate, monoForumPeerId);
            return;
        }
        ArrayList<TLRPC.MessageEntity> entities = org.telegram.messenger.MediaDataController.getInstance(currentAccount).getEntities(new CharSequence[]{text}, true, false);
        SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(text.toString(), dialogId, null, null, null, false, entities, null, null, withSound, scheduleDate, 0, null, false);
        params.monoForumPeer = monoForumPeerId;
        SendMessagesHelper.getInstance(currentAccount).sendMessage(params);
    }

    public static boolean sendRichMediaOnly(int currentAccount, MessageObject messageObject, long targetDialogId, MessageObject replyTopMsg, boolean withSound, int scheduleDate, long monoForumPeerId) {
        if (messageObject == null || messageObject.messageOwner == null || messageObject.messageOwner.rich_message == null) {
            return false;
        }
        ArrayList<Object> media = collectRichMedia(messageObject.messageOwner.rich_message);
        if (media.isEmpty()) {
            return false;
        }
        TLRPC.InputPeer peer = MessagesController.getInstance(currentAccount).getInputPeer(targetDialogId);
        if (peer == null) {
            return false;
        }
        ArrayList<TLObject> refreshInputs = new ArrayList<>();
        TLObject request;
        if (media.size() == 1) {
            TLRPC.TL_messages_sendMedia singleRequest = new TLRPC.TL_messages_sendMedia();
            singleRequest.peer = peer;
            singleRequest.media = toInputMedia(media.get(0), refreshInputs);
            singleRequest.random_id = SendMessagesHelper.getInstance(currentAccount).getNextRandomId();
            singleRequest.silent = !withSound;
            singleRequest.message = "";
            singleRequest.reply_to = CopyReferenceSender.buildInputReplyTo(currentAccount, replyTopMsg, monoForumPeerId);
            if (singleRequest.reply_to != null) {
                singleRequest.flags |= 1;
            }
            if (scheduleDate != 0) {
                singleRequest.schedule_date = scheduleDate;
                singleRequest.flags |= 1024;
            }
            request = singleRequest;
        } else {
            TLRPC.TL_messages_sendMultiMedia multiRequest = new TLRPC.TL_messages_sendMultiMedia();
            multiRequest.peer = peer;
            multiRequest.silent = !withSound;
            for (int i = 0; i < media.size(); i++) {
                TLRPC.TL_inputSingleMedia singleMedia = new TLRPC.TL_inputSingleMedia();
                singleMedia.media = toInputMedia(media.get(i), refreshInputs);
                singleMedia.random_id = SendMessagesHelper.getInstance(currentAccount).getNextRandomId();
                singleMedia.message = "";
                multiRequest.multi_media.add(singleMedia);
            }
            multiRequest.reply_to = CopyReferenceSender.buildInputReplyTo(currentAccount, replyTopMsg, monoForumPeerId);
            if (multiRequest.reply_to != null) {
                multiRequest.flags |= 1;
            }
            if (scheduleDate != 0) {
                multiRequest.schedule_date = scheduleDate;
                multiRequest.flags |= 1024;
            }
            request = multiRequest;
        }
        sendMediaWithRefresh(currentAccount, messageObject, request, refreshInputs);
        return true;
    }

    public static ArrayList<Object> collectRichMedia(TL_iv.RichMessage richMessage) {
        ArrayList<Object> media = new ArrayList<>();
        if (richMessage != null) {
            collectBlocks(richMessage.blocks, richMessage, media);
        }
        return media;
    }

    private static void collectBlocks(ArrayList<TL_iv.PageBlock> blocks, TL_iv.RichMessage richMessage, ArrayList<Object> out) {
        if (blocks == null) {
            return;
        }
        for (int i = 0; i < blocks.size(); i++) {
            TL_iv.PageBlock block = blocks.get(i);
            boolean photo = false;
            long id = 0;
            if (block instanceof TL_iv.pageBlockVideo) {
                id = ((TL_iv.pageBlockVideo) block).video_id;
            } else if (block instanceof TL_iv.pageBlockPhoto) {
                id = ((TL_iv.pageBlockPhoto) block).photo_id;
                photo = true;
            } else if (block instanceof TL_iv.pageBlockDocument) {
                id = ((TL_iv.pageBlockDocument) block).document_id;
            } else if (block instanceof TL_iv.pageBlockAudio) {
                id = ((TL_iv.pageBlockAudio) block).audio_id;
            } else if (block instanceof TL_iv.pageBlockCollage) {
                collectBlocks(((TL_iv.pageBlockCollage) block).items, richMessage, out);
                continue;
            } else if (block instanceof TL_iv.pageBlockSlideshow) {
                collectBlocks(((TL_iv.pageBlockSlideshow) block).items, richMessage, out);
                continue;
            } else {
                continue;
            }
            if (id == 0) {
                continue;
            }
            Object resolved = resolveById(richMessage, id, photo);
            if (resolved != null) {
                out.add(resolved);
            }
        }
    }

    private static Object resolveById(TL_iv.RichMessage richMessage, long id, boolean photo) {
        if (photo && richMessage.photos != null) {
            for (int i = 0; i < richMessage.photos.size(); i++) {
                TLRPC.Photo photoObj = richMessage.photos.get(i);
                if (photoObj != null && photoObj.id == id) {
                    return photoObj;
                }
            }
            return null;
        }
        if (!photo && richMessage.documents != null) {
            for (int i = 0; i < richMessage.documents.size(); i++) {
                TLRPC.Document document = richMessage.documents.get(i);
                if (document != null && document.id == id) {
                    return document;
                }
            }
        }
        return null;
    }

    private static TLRPC.InputMedia toInputMedia(Object media, ArrayList<TLObject> refreshInputs) {
        if (media instanceof TLRPC.Photo) {
            TLRPC.Photo photo = (TLRPC.Photo) media;
            TLRPC.TL_inputPhoto inputPhoto = new TLRPC.TL_inputPhoto();
            inputPhoto.id = photo.id;
            inputPhoto.access_hash = photo.access_hash;
            inputPhoto.file_reference = photo.file_reference != null ? photo.file_reference : new byte[0];
            refreshInputs.add(inputPhoto);
            TLRPC.TL_inputMediaPhoto inputMediaPhoto = new TLRPC.TL_inputMediaPhoto();
            inputMediaPhoto.id = inputPhoto;
            return inputMediaPhoto;
        }
        TLRPC.Document document = (TLRPC.Document) media;
        TLRPC.TL_inputDocument inputDocument = new TLRPC.TL_inputDocument();
        inputDocument.id = document.id;
        inputDocument.access_hash = document.access_hash;
        inputDocument.file_reference = document.file_reference != null ? document.file_reference : new byte[0];
        refreshInputs.add(inputDocument);
        TLRPC.TL_inputMediaDocument inputMediaDocument = new TLRPC.TL_inputMediaDocument();
        inputMediaDocument.id = inputDocument;
        return inputMediaDocument;
    }

    private static void sendMediaWithRefresh(int currentAccount, MessageObject messageObject, TLObject request, ArrayList<TLObject> refreshInputs) {
        boolean[] fallbackHandled = {false};
        boolean[] retryDispatched = {false};
        int[] pendingRefresh = {0};
        Runnable finalFallback = () -> AndroidUtilities.runOnUIThread(() -> {
            if (fallbackHandled[0]) {
                return;
            }
            fallbackHandled[0] = true;
            FileLog.d("rich media-only send failed");
            if (LaunchActivity.instance != null) {
                Toast.makeText(LaunchActivity.instance, LocaleController.getString(org.telegram.messenger.R.string.CopySendResultFailed), Toast.LENGTH_SHORT).show();
            }
        });
        sendRequest(currentAccount, request, (response, error) -> {
            if (error == null || !FileRefController.isFileRefError(error.text)) {
                if (error != null) {
                    FileLog.d("rich media-only send failed: " + error.text);
                    finalFallback.run();
                }
                return;
            }
            if (refreshInputs.isEmpty()) {
                finalFallback.run();
                return;
            }
            pendingRefresh[0] = refreshInputs.size();
            Runnable retryWithGuard = () -> {
                if (fallbackHandled[0] || retryDispatched[0]) {
                    return;
                }
                if (pendingRefresh[0] > 0 && --pendingRefresh[0] > 0) {
                    return;
                }
                retryDispatched[0] = true;
                AndroidUtilities.cancelRunOnUIThread(finalFallback);
                sendRequest(currentAccount, request, (response2, error2) -> {
                    if (error2 != null) {
                        FileLog.d("rich media-only retry failed: " + error2.text);
                        finalFallback.run();
                    }
                });
            };
            for (int i = 0; i < refreshInputs.size(); i++) {
                FileRefController.getInstance(currentAccount).requestReference(messageObject, refreshInputs.get(i), (Runnable) retryWithGuard);
            }
            AndroidUtilities.runOnUIThread(finalFallback, REFRESH_TIMEOUT_MS);
        });
    }

    private static void sendRequest(int currentAccount, TLObject request, org.telegram.tgnet.RequestDelegate delegate) {
        org.telegram.tgnet.ConnectionsManager.getInstance(currentAccount).sendRequest(request, (response, error) -> {
            if (error == null) {
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        MessagesController.getInstance(currentAccount).processUpdates((TLRPC.Updates) response, false);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                });
                return;
            }
            delegate.run(response, error);
        });
    }
}
