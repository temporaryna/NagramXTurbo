package xyz.nextalone.nagram.helper;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.collection.SparseArrayCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;

import tw.nekomimi.nekogram.helpers.MessageHelper;
import xyz.nextalone.nagram.helper.CopyReferenceSender;

public class CopySendQueue implements NotificationCenter.NotificationCenterDelegate {

    private static final long COPY_SEND_CONFIRM_THRESHOLD_BYTES = 500L * 1024 * 1024;
    private static final long COPY_SEND_UNKNOWN_DOCUMENT_SIZE_BYTES = 100L * 1024 * 1024;
    private static final long COPY_SEND_UNKNOWN_PHOTO_SIZE_BYTES = 10L * 1024 * 1024;
    private static final long COPY_SEND_RETRY_DELAY_MS = 3000;
    private static final int COPY_SEND_MAX_RETRY_COUNT = 1;
    private static final int COPY_SEND_MAX_RECHECK_CYCLES = 1;
    private static final long COPY_SEND_NOTIFY_THROTTLE_MS = 200;
    private static final long COPY_SEND_IDLE_RECHECK_DELAY_MS = 4000;
    private static final String NOTIFICATION_TAG = "copy_send_queue";
    private static final int NOTIFICATION_ID = 0x43;

    private static final int STATE_DOWNLOADING = 0;
    private static final int STATE_SENDING = 2;

    public static final int RESULT_QUEUED = 0;
    public static final int RESULT_SENT_NOW = 1;
    public static final int RESULT_FAILED_NOW = 2;

    private static final SparseArrayCompat<CopySendQueue> instanceByAccount = new SparseArrayCompat<>();

    public static CopySendQueue getInstance(int account) {
        CopySendQueue instance = instanceByAccount.get(account);
        if (instance == null) {
            instance = new CopySendQueue(account);
            instanceByAccount.put(account, instance);
        }
        return instance;
    }

    private final int currentAccount;
    private final ArrayList<CopySendQueueJob> queuedJobs = new ArrayList<>();
    private CopySendQueueJob currentJob;
    private boolean isObserverRegistered;
    private long lastProgressNotifyTime;

    private CopySendQueue(int account) {
        currentAccount = account;
    }

    public static class CopySendQueueTarget {
        public final long dialogId;
        public final MessageObject replyTopMsg;
        public final long monoForumPeerId;

        public CopySendQueueTarget(long dialogId, MessageObject replyTopMsg, long monoForumPeerId) {
            this.dialogId = dialogId;
            this.replyTopMsg = replyTopMsg;
            this.monoForumPeerId = monoForumPeerId;
        }
    }

    private static class CopySendQueueJob {
        final ArrayList<MessageObject> messages;
        final int originalMessageCount;
        final ArrayList<CopySendQueueTarget> targets;
        final CharSequence comment;
        final ArrayList<TLRPC.MessageEntity> commentEntities;
        final MessageObject editableMessage;
        final String editedText;
        final ArrayList<TLRPC.MessageEntity> editedEntities;
        final boolean withSound;
        final int scheduleDate;
        final int scheduleRepeatPeriod;
        final LinkedHashMap<String, Long> pendingDocumentSizesByFileName = new LinkedHashMap<>();
        final HashMap<String, MessageObject> messageByDocumentFileName = new HashMap<>();
        final HashMap<MessageObject, String> photoFileNameByMessage = new HashMap<>();
        final HashMap<String, Integer> retryCountByFileName = new LinkedHashMap<>();
        final HashMap<String, Long> downloadedBytesByFileName = new HashMap<>();
        final ArrayList<String> initialMessageKeys = new ArrayList<>();
        boolean captionLost;
        long totalBytes;
        int recheckCycles;
        int state;

        CopySendQueueJob(ArrayList<MessageObject> messages, ArrayList<CopySendQueueTarget> targets,
                         CharSequence comment, ArrayList<TLRPC.MessageEntity> commentEntities,
                         MessageObject editableMessage, String editedText, ArrayList<TLRPC.MessageEntity> editedEntities,
                         boolean withSound, int scheduleDate, int scheduleRepeatPeriod) {
            this.messages = messages;
            this.originalMessageCount = messages.size();
            this.targets = targets;
            this.comment = comment;
            this.commentEntities = commentEntities;
            this.editableMessage = editableMessage;
            this.editedText = editedText;
            this.editedEntities = editedEntities;
            this.withSound = withSound;
            this.scheduleDate = scheduleDate;
            this.scheduleRepeatPeriod = scheduleRepeatPeriod;
            for (int i = 0; i < messages.size(); i++) {
                initialMessageKeys.add(messages.get(i).getDialogId() + ":" + messages.get(i).getId());
            }
        }
    }

    public int enqueue(Activity activity, ArrayList<MessageObject> sourceMessages, ArrayList<CopySendQueueTarget> targets,
                       CharSequence comment, ArrayList<TLRPC.MessageEntity> commentEntities,
                       MessageObject editableMessage, String editedText, ArrayList<TLRPC.MessageEntity> editedEntities,
                       boolean withSound, int scheduleDate, int scheduleRepeatPeriod) {
        if (sourceMessages == null || sourceMessages.isEmpty() || targets == null || targets.isEmpty()) {
            return RESULT_FAILED_NOW;
        }
        if (findDuplicateJob(sourceMessages, targets, editedText, comment, scheduleDate, withSound, scheduleRepeatPeriod) != null) {
            return RESULT_QUEUED;
        }
        ArrayList<MessageObject> messages = new ArrayList<>(sourceMessages);
        LinkedHashMap<String, Long> pendingDocuments = new LinkedHashMap<>();
        ArrayList<MessageObject> pendingPhotos = new ArrayList<>();
        long totalPendingBytes = collectPendingDownloads(messages, pendingDocuments, pendingPhotos);
        if (pendingDocuments.isEmpty() && pendingPhotos.isEmpty()) {
            return sendToTargets(messages, targets, comment, commentEntities, editableMessage, editedText, editedEntities, withSound, scheduleDate, scheduleRepeatPeriod, null);
        }
        if (totalPendingBytes > COPY_SEND_CONFIRM_THRESHOLD_BYTES) {
            showConfirmDialog(activity, messages, targets, comment, commentEntities, editableMessage, editedText, editedEntities, withSound, scheduleDate, scheduleRepeatPeriod, pendingDocuments, pendingPhotos, totalPendingBytes);
            return RESULT_QUEUED;
        }
        enqueueJob(messages, targets, comment, commentEntities, editableMessage, editedText, editedEntities, withSound, scheduleDate, scheduleRepeatPeriod, pendingDocuments, pendingPhotos);
        return RESULT_QUEUED;
    }

    private void showConfirmDialog(Activity activity, ArrayList<MessageObject> messages, ArrayList<CopySendQueueTarget> targets,
                                   CharSequence comment, ArrayList<TLRPC.MessageEntity> commentEntities,
                                   MessageObject editableMessage, String editedText, ArrayList<TLRPC.MessageEntity> editedEntities,
                                   boolean withSound, int scheduleDate, int scheduleRepeatPeriod,
                                   LinkedHashMap<String, Long> pendingDocuments, ArrayList<MessageObject> pendingPhotos, long totalPendingBytes) {
        if (activity == null) {
            enqueueJob(messages, targets, comment, commentEntities, editableMessage, editedText, editedEntities, withSound, scheduleDate, scheduleRepeatPeriod, pendingDocuments, pendingPhotos);
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(LocaleController.formatString(R.string.CopySendConfirmSize, AndroidUtilities.formatFileSize(totalPendingBytes)));
        builder.setPositiveButton(LocaleController.getString(R.string.CopySendConfirmAction), (dialog, which) ->
                enqueueJob(messages, targets, comment, commentEntities, editableMessage, editedText, editedEntities, withSound, scheduleDate, scheduleRepeatPeriod, pendingDocuments, pendingPhotos));
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private void enqueueJob(ArrayList<MessageObject> messages, ArrayList<CopySendQueueTarget> targets,
                            CharSequence comment, ArrayList<TLRPC.MessageEntity> commentEntities,
                            MessageObject editableMessage, String editedText, ArrayList<TLRPC.MessageEntity> editedEntities,
                            boolean withSound, int scheduleDate, int scheduleRepeatPeriod,
                            LinkedHashMap<String, Long> pendingDocuments, ArrayList<MessageObject> pendingPhotos) {
        CopySendQueueJob job = new CopySendQueueJob(messages, targets, comment, commentEntities, editableMessage, editedText, editedEntities, withSound, scheduleDate, scheduleRepeatPeriod);
        job.pendingDocumentSizesByFileName.putAll(pendingDocuments);
        for (int i = 0; i < pendingPhotos.size(); i++) {
            MessageObject messageObject = pendingPhotos.get(i);
            String photoFileName = resolvePhotoFileName(messageObject);
            if (photoFileName != null) {
                job.photoFileNameByMessage.put(messageObject, photoFileName);
            }
        }
        for (Long size : pendingDocuments.values()) {
            job.totalBytes += size == null ? 0 : size;
        }
        job.totalBytes += job.photoFileNameByMessage.size() * COPY_SEND_UNKNOWN_PHOTO_SIZE_BYTES;
        queuedJobs.add(job);
        if (currentJob == null) {
            startNextJob();
        }
    }

    @Nullable
    private CopySendQueueJob findDuplicateJob(ArrayList<MessageObject> messages, ArrayList<CopySendQueueTarget> targets, String editedText, CharSequence comment, int scheduleDate, boolean withSound, int scheduleRepeatPeriod) {
        ArrayList<CopySendQueueJob> allJobs = new ArrayList<>(queuedJobs);
        if (currentJob != null) {
            allJobs.add(currentJob);
        }
        for (CopySendQueueJob job : allJobs) {
            if (job.state == STATE_SENDING) {
                continue;
            }
            if (!Objects.equals(job.editedText, editedText)
                    || !Objects.equals(job.comment == null ? null : job.comment.toString(), comment == null ? null : comment.toString())
                    || job.scheduleDate != scheduleDate || job.withSound != withSound || job.scheduleRepeatPeriod != scheduleRepeatPeriod) {
                continue;
            }
            if (job.targets.size() != targets.size()) {
                continue;
            }
            boolean areSameMessages = job.initialMessageKeys.size() == messages.size();
            if (areSameMessages) {
                for (int i = 0; i < messages.size(); i++) {
                    if (!job.initialMessageKeys.get(i).equals(messages.get(i).getDialogId() + ":" + messages.get(i).getId())) {
                        areSameMessages = false;
                        break;
                    }
                }
            }
            if (!areSameMessages) {
                continue;
            }
            boolean areSameTargets = true;
            for (int i = 0; i < targets.size(); i++) {
                if (!resolveTargetKey(targets.get(i)).equals(resolveTargetKey(job.targets.get(i)))) {
                    areSameTargets = false;
                    break;
                }
            }
            if (areSameTargets) {
                return job;
            }
        }
        return null;
    }

    private String resolveTargetKey(CopySendQueueTarget target) {
        long topicId = target.replyTopMsg != null ? target.replyTopMsg.getId() : 0;
        return target.dialogId + ":" + topicId;
    }

    private long collectPendingDownloads(ArrayList<MessageObject> messages, LinkedHashMap<String, Long> pendingDocuments, ArrayList<MessageObject> pendingPhotos) {
        long totalBytes = 0;
        HashSet<Long> referenceCapableGroupIds = CopyReferenceSender.resolveFullyReferenceCapableGroupIds(messages);
        for (int i = 0; i < messages.size(); i++) {
            MessageObject messageObject = messages.get(i);
            if (!needsFileForCopy(messageObject) || CopyReferenceSender.isSentWithoutDownload(currentAccount, messageObject, referenceCapableGroupIds)) {
                continue;
            }
            TLRPC.Document document = messageObject.getDocument();
            if (document != null) {
                String fileName = FileLoader.getAttachFileName(document);
                if (!pendingDocuments.containsKey(fileName)) {
                    long size = document.size > 0 ? document.size : COPY_SEND_UNKNOWN_DOCUMENT_SIZE_BYTES;
                    pendingDocuments.put(fileName, size);
                    totalBytes += size;
                }
            } else if (!pendingPhotos.contains(messageObject)) {
                pendingPhotos.add(messageObject);
                totalBytes += COPY_SEND_UNKNOWN_PHOTO_SIZE_BYTES;
            }
        }
        return totalBytes;
    }

    private boolean needsFileForCopy(MessageObject messageObject) {
        return messageObject != null && messageObject.messageOwner != null
                && !messageObject.isSticker() && !messageObject.isAnimatedSticker() && !messageObject.isAnimatedEmoji()
                && (messageObject.isPhoto() || messageObject.isVideo() || messageObject.isRoundVideo() || messageObject.getDocument() != null);
    }

    @Nullable
    private String resolvePhotoFileName(MessageObject messageObject) {
        TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, AndroidUtilities.getPhotoSize());
        if (photoSize == null) {
            return null;
        }
        ImageLocation imageLocation = ImageLocation.getForObject(photoSize, messageObject.photoThumbsObject);
        if (imageLocation == null || imageLocation.location == null) {
            return null;
        }
        return imageLocation.location.volume_id + "_" + imageLocation.location.local_id + ".jpg";
    }

    private void startNextJob() {
        if (currentJob != null || queuedJobs.isEmpty()) {
            return;
        }
        currentJob = queuedJobs.remove(0);
        currentJob.state = STATE_DOWNLOADING;
        registerObserver();
        Context context = ApplicationLoader.applicationContext;
        Toast.makeText(context, LocaleController.getString(R.string.CopySendDownloadingToast), Toast.LENGTH_SHORT).show();
        startJobDownloads();
        showProgressNotification(0);
        CopySendQueueJob startedJob = currentJob;
        AndroidUtilities.runOnUIThread(() -> {
            if (currentJob == startedJob) {
                checkJobFilesReady();
            }
        }, COPY_SEND_IDLE_RECHECK_DELAY_MS);
        checkJobFilesReady();
    }

    private void startJobDownloads() {
        CopySendQueueJob job = currentJob;
        if (job == null) {
            return;
        }
        for (String fileName : job.pendingDocumentSizesByFileName.keySet()) {
            MessageObject ownerMessage = job.messageByDocumentFileName.get(fileName);
            if (ownerMessage == null) {
                ownerMessage = findMessageForDocumentFileName(job, fileName);
                if (ownerMessage != null) {
                    job.messageByDocumentFileName.put(fileName, ownerMessage);
                }
            }
            if (ownerMessage == null || FileLoader.getInstance(currentAccount).isLoadingFile(fileName)) {
                continue;
            }
            FileLoader.getInstance(currentAccount).loadFile(ownerMessage.getDocument(), ownerMessage, FileLoader.PRIORITY_NORMAL, 0);
        }
        for (int i = 0; i < job.messages.size(); i++) {
            MessageObject messageObject = job.messages.get(i);
            String photoFileName = job.photoFileNameByMessage.get(messageObject);
            if (photoFileName == null || FileLoader.getInstance(currentAccount).isLoadingFile(photoFileName)) {
                continue;
            }
            TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, AndroidUtilities.getPhotoSize());
            if (photoSize != null) {
                FileLoader.getInstance(currentAccount).loadFile(ImageLocation.getForObject(photoSize, messageObject.photoThumbsObject), messageObject, null, FileLoader.PRIORITY_NORMAL, messageObject.shouldEncryptPhotoOrVideo() ? 2 : 0);
            }
        }
    }

    @Nullable
    private MessageObject findMessageForDocumentFileName(CopySendQueueJob job, String fileName) {
        for (int i = 0; i < job.messages.size(); i++) {
            TLRPC.Document document = job.messages.get(i).getDocument();
            if (document != null && fileName.equals(FileLoader.getAttachFileName(document))) {
                return job.messages.get(i);
            }
        }
        return null;
    }

    public void cancelJob() {
        cancelProgressNotification();
        CopySendQueueJob job = currentJob;
        if (job == null || job.state != STATE_DOWNLOADING) {
            return;
        }
        for (String fileName : job.pendingDocumentSizesByFileName.keySet()) {
            FileLoader.getInstance(currentAccount).cancelLoadFile(fileName);
        }
        for (String photoFileName : job.photoFileNameByMessage.values()) {
            FileLoader.getInstance(currentAccount).cancelLoadFile(photoFileName);
        }
        finishJob();
        Context context = ApplicationLoader.applicationContext;
        notifyResultSafely(context, buildFinalNotification(context, LocaleController.getString(R.string.CopySendResultCancelled)));
        startNextJob();
    }

    private void cancelProgressNotification() {
        try {
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) ApplicationLoader.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(NOTIFICATION_TAG, NOTIFICATION_ID + currentAccount);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void finishJob() {
        currentJob = null;
        if (queuedJobs.isEmpty()) {
            unregisterObserver();
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (account != currentAccount || currentJob == null) {
            return;
        }
        if (id == NotificationCenter.fileLoaded) {
            checkJobFilesReady();
        } else if (id == NotificationCenter.fileLoadFailed) {
            onPendingFileFailed((String) args[0], args.length > 1 && args[1] instanceof Integer && (Integer) args[1] != 0);
        } else if (id == NotificationCenter.fileLoadProgressChanged) {
            onPendingFileProgress((String) args[0], (Long) args[1]);
        }
    }

    private void onPendingFileProgress(String location, long loadedSize) {
        CopySendQueueJob job = currentJob;
        if (job == null || (!job.pendingDocumentSizesByFileName.containsKey(location) && !isPendingPhotoFileName(job, location))) {
            return;
        }
        job.downloadedBytesByFileName.put(location, loadedSize);
        long downloadedBytes = 0;
        for (Long bytes : job.downloadedBytesByFileName.values()) {
            downloadedBytes += bytes == null ? 0 : bytes;
        }
        int progress = job.totalBytes <= 0 ? 0 : (int) Math.min(100, downloadedBytes * 100 / job.totalBytes);
        long now = System.currentTimeMillis();
        if (now - lastProgressNotifyTime < COPY_SEND_NOTIFY_THROTTLE_MS) {
            return;
        }
        lastProgressNotifyTime = now;
        Context context = ApplicationLoader.applicationContext;
        notifySafely(context, buildProgressNotification(context, progress));
    }

    private void onPendingFileFailed(String location, boolean wasCanceledByUser) {
        CopySendQueueJob job = currentJob;
        if (job == null || job.state == STATE_SENDING) {
            return;
        }
        if (!job.pendingDocumentSizesByFileName.containsKey(location) && !isPendingPhotoFileName(job, location)) {
            return;
        }
        Integer retryCount = job.retryCountByFileName.get(location);
        int retries = retryCount == null ? 0 : retryCount;
        if (wasCanceledByUser) {
            if (FileLoader.getInstance(currentAccount).isLoadingFile(location)) {
                return;
            }
            excludeFailedFileMessages(job, location);
            checkJobFilesReady();
            return;
        }
        if (retries < COPY_SEND_MAX_RETRY_COUNT) {
            job.retryCountByFileName.put(location, retries + 1);
            String fileName = location;
            AndroidUtilities.runOnUIThread(() -> restartJobFileLoad(fileName), COPY_SEND_RETRY_DELAY_MS);
        } else {
            excludeFailedFileMessages(job, location);
            checkJobFilesReady();
        }
    }

    private void restartJobFileLoad(String fileName) {
        CopySendQueueJob job = currentJob;
        if (job == null || job.state == STATE_SENDING) {
            return;
        }
        if (job.pendingDocumentSizesByFileName.containsKey(fileName)) {
            MessageObject ownerMessage = job.messageByDocumentFileName.containsKey(fileName) ? job.messageByDocumentFileName.get(fileName) : findMessageForDocumentFileName(job, fileName);
            if (ownerMessage != null) {
                FileLoader.getInstance(currentAccount).loadFile(ownerMessage.getDocument(), ownerMessage, FileLoader.PRIORITY_NORMAL, 0);
            }
            return;
        }
        for (int i = 0; i < job.messages.size(); i++) {
            MessageObject messageObject = job.messages.get(i);
            if (fileName.equals(job.photoFileNameByMessage.get(messageObject))) {
                TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, AndroidUtilities.getPhotoSize());
                if (photoSize != null) {
                    FileLoader.getInstance(currentAccount).loadFile(ImageLocation.getForObject(photoSize, messageObject.photoThumbsObject), messageObject, null, FileLoader.PRIORITY_NORMAL, messageObject.shouldEncryptPhotoOrVideo() ? 2 : 0);
                }
                return;
            }
        }
    }

    private boolean isPendingPhotoFileName(CopySendQueueJob job, String location) {
        for (String photoFileName : job.photoFileNameByMessage.values()) {
            if (photoFileName.equals(location)) {
                return true;
            }
        }
        return false;
    }

    private void excludeFailedFileMessages(CopySendQueueJob job, String location) {
        job.pendingDocumentSizesByFileName.remove(location);
        for (int i = job.messages.size() - 1; i >= 0; i--) {
            MessageObject messageObject = job.messages.get(i);
            TLRPC.Document document = messageObject.getDocument();
            if (document != null && location.equals(FileLoader.getAttachFileName(document))) {
                markCaptionLost(job, messageObject);
                job.messages.remove(i);
                continue;
            }
            if (location.equals(job.photoFileNameByMessage.get(messageObject))) {
                markCaptionLost(job, messageObject);
                job.photoFileNameByMessage.remove(messageObject);
                job.messages.remove(i);
            }
        }
    }

    private void markCaptionLost(CopySendQueueJob job, MessageObject messageObject) {
        if (messageObject.caption != null && messageObject.caption.length() > 0) {
            job.captionLost = true;
        }
    }

    private void checkJobFilesReady() {
        CopySendQueueJob job = currentJob;
        if (job == null || job.state == STATE_SENDING) {
            return;
        }
        LinkedHashMap<String, Long> stillPendingDocuments = new LinkedHashMap<>();
        ArrayList<MessageObject> stillPendingPhotos = new ArrayList<>();
        collectPendingDownloads(job.messages, stillPendingDocuments, stillPendingPhotos);
        if (!stillPendingDocuments.isEmpty() || !stillPendingPhotos.isEmpty()) {
            boolean hasIdlePendingFiles = false;
            for (String fileName : stillPendingDocuments.keySet()) {
                if (!FileLoader.getInstance(currentAccount).isLoadingFile(fileName)) {
                    hasIdlePendingFiles = true;
                    break;
                }
            }
            if (!hasIdlePendingFiles) {
                for (int i = 0; i < stillPendingPhotos.size(); i++) {
                    String photoFileName = job.photoFileNameByMessage.get(stillPendingPhotos.get(i));
                    if (photoFileName == null || !FileLoader.getInstance(currentAccount).isLoadingFile(photoFileName)) {
                        hasIdlePendingFiles = true;
                        break;
                    }
                }
            }
            if (!hasIdlePendingFiles) {
                return;
            }
            if (job.recheckCycles >= COPY_SEND_MAX_RECHECK_CYCLES) {
                job.state = STATE_SENDING;
                sendToTargets(job.messages, job.targets, job.comment, job.commentEntities, job.editableMessage, job.editedText, job.editedEntities, job.withSound, job.scheduleDate, job.scheduleRepeatPeriod, job);
                return;
            }
            job.recheckCycles++;
            job.downloadedBytesByFileName.clear();
            job.pendingDocumentSizesByFileName.clear();
            job.pendingDocumentSizesByFileName.putAll(stillPendingDocuments);
            job.photoFileNameByMessage.clear();
            for (int i = 0; i < stillPendingPhotos.size(); i++) {
                String photoFileName = resolvePhotoFileName(stillPendingPhotos.get(i));
                if (photoFileName != null) {
                    job.photoFileNameByMessage.put(stillPendingPhotos.get(i), photoFileName);
                }
            }
            job.totalBytes = 0;
            for (Long size : stillPendingDocuments.values()) {
                job.totalBytes += size == null ? 0 : size;
            }
            job.totalBytes += stillPendingPhotos.size() * COPY_SEND_UNKNOWN_PHOTO_SIZE_BYTES;
            startJobDownloads();
            return;
        }
        job.state = STATE_SENDING;
        sendToTargets(job.messages, job.targets, job.comment, job.commentEntities, job.editableMessage, job.editedText, job.editedEntities, job.withSound, job.scheduleDate, job.scheduleRepeatPeriod, job);
    }

    private ArrayList<MessageObject> filterReadyMessages(CopySendQueueJob job) {
        ArrayList<MessageObject> readyMessages = new ArrayList<>();
        HashSet<Long> referenceCapableGroupIds = CopyReferenceSender.resolveFullyReferenceCapableGroupIds(job.messages);
        for (int i = 0; i < job.messages.size(); i++) {
            MessageObject messageObject = job.messages.get(i);
            if (!needsFileForCopy(messageObject) || CopyReferenceSender.isSentWithoutDownload(currentAccount, messageObject, referenceCapableGroupIds)) {
                readyMessages.add(messageObject);
            }
        }
        return readyMessages;
    }

    private int sendToTargets(ArrayList<MessageObject> messages, ArrayList<CopySendQueueTarget> targets,
                               CharSequence comment, ArrayList<TLRPC.MessageEntity> commentEntities,
                               MessageObject editableMessage, String editedText, ArrayList<TLRPC.MessageEntity> editedEntities,
                               boolean withSound, int scheduleDate, int scheduleRepeatPeriod, @Nullable CopySendQueueJob job) {
        boolean hasComment = comment != null && comment.length() > 0;
        boolean captionLost = false;
        int excludedCount = 0;
        if (job != null) {
            ArrayList<MessageObject> readyMessages = filterReadyMessages(job);
            excludedCount = job.originalMessageCount - readyMessages.size();
            for (int i = 0; i < job.messages.size(); i++) {
                MessageObject messageObject = job.messages.get(i);
                if (!readyMessages.contains(messageObject)) {
                    markCaptionLost(job, messageObject);
                }
            }
            captionLost = job.captionLost;
            messages = readyMessages;
        }
        if (messages.isEmpty()) {
            if (job == null) {
                return RESULT_FAILED_NOW;
            }
            Context context = ApplicationLoader.applicationContext;
            cancelProgressNotification();
            notifyResultSafely(context, buildFinalNotification(context, LocaleController.getString(R.string.CopySendResultFailed)));
            finishJob();
            startNextJob();
            return RESULT_FAILED_NOW;
        }
        boolean dispatchAllByReference = CopyReferenceSender.canDispatchAllByReference(currentAccount, messages);
        int sentTargets = 0;
        int failedTargets = 0;
        StringBuilder failedTargetNames = null;
        for (int i = 0; i < targets.size(); i++) {
            CopySendQueueTarget target = targets.get(i);
            boolean sent = sendCopiesForTarget(messages, target, dispatchAllByReference, editedText, editedEntities, editableMessage, withSound, scheduleDate);
            if (sent) {
                sentTargets++;
                if (hasComment) {
                    sendComment(comment, commentEntities, target, withSound, scheduleDate, scheduleRepeatPeriod);
                }
            } else {
                failedTargets++;
                if (failedTargetNames == null) {
                    failedTargetNames = new StringBuilder();
                } else {
                    failedTargetNames.append(", ");
                }
                failedTargetNames.append(resolveTargetTitle(target.dialogId));
            }
        }
        if (job == null) {
            return sentTargets > 0 ? RESULT_SENT_NOW : RESULT_FAILED_NOW;
        }
        Context context = ApplicationLoader.applicationContext;
        cancelProgressNotification();
        int sentMessages = messages.size();
        String result;
        if (sentTargets > 0 && failedTargets == 0 && excludedCount == 0) {
            result = LocaleController.formatString(R.string.CopySendResultSent, sentMessages);
        } else if (sentTargets > 0) {
            result = LocaleController.formatString(R.string.CopySendResultPartial, sentMessages, job.originalMessageCount, excludedCount);
        } else {
            result = LocaleController.getString(R.string.CopySendResultFailed);
        }
        if (captionLost && sentTargets > 0) {
            result += " " + LocaleController.getString(R.string.CopySendResultCaptionLost);
        }
        NotificationCompat.Builder builder = buildFinalNotification(context, result);
        if (failedTargets > 0) {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(result + "\n" + LocaleController.formatString(R.string.CopySendResultTargetFailed, failedTargetNames.toString())));
        }
        notifyResultSafely(context, builder);
        finishJob();
        startNextJob();
        return RESULT_SENT_NOW;
    }

    private String resolveTargetTitle(long dialogId) {
        MessagesController messagesController = MessagesController.getInstance(currentAccount);
        if (dialogId < 0) {
            TLRPC.Chat chat = messagesController.getChat(-dialogId);
            if (chat != null) {
                return chat.title;
            }
        } else {
            TLRPC.User user = messagesController.getUser(dialogId);
            if (user != null) {
                return user.first_name;
            }
        }
        return "#" + dialogId;
    }

    private boolean sendCopiesForTarget(ArrayList<MessageObject> messages, CopySendQueueTarget target, boolean dispatchAllByReference,
                                        String editedText, ArrayList<TLRPC.MessageEntity> editedEntities, MessageObject editableMessage,
                                        boolean withSound, int scheduleDate) {
        if (dispatchAllByReference) {
            if (editedText == null || editableMessage == null) {
                return CopyReferenceSender.sendByReference(currentAccount, messages, target,
                        null, null, null, withSound, scheduleDate);
            }
            return ForwardTextEdit.withEditedText(editableMessage, editedText, editedEntities, () ->
                    CopyReferenceSender.sendByReference(currentAccount, messages, target,
                            editableMessage, editedText, editedEntities, withSound, scheduleDate));
        }
        if (editedText == null || editableMessage == null) {
            return MessageHelper.getInstance(currentAccount).sendMessagesAsCopy(messages, target.dialogId, null, target.replyTopMsg, null, withSound, scheduleDate, 0, null, 0, 0, target.monoForumPeerId, null);
        }
        return ForwardTextEdit.withEditedText(editableMessage, editedText, editedEntities, () ->
                MessageHelper.getInstance(currentAccount).sendMessagesAsCopy(messages, target.dialogId, null, target.replyTopMsg, null, withSound, scheduleDate, 0, null, 0, 0, target.monoForumPeerId, null));
    }

    private void sendComment(CharSequence comment, ArrayList<TLRPC.MessageEntity> commentEntities, CopySendQueueTarget target, boolean withSound, int scheduleDate, int scheduleRepeatPeriod) {
        SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(comment.toString(), target.dialogId, null, target.replyTopMsg, null, true, commentEntities, null, null, withSound, scheduleDate, scheduleRepeatPeriod, null, false);
        params.monoForumPeer = target.monoForumPeerId;
        SendMessagesHelper.getInstance(currentAccount).sendMessage(params);
    }

    private void registerObserver() {
        if (isObserverRegistered) {
            return;
        }
        NotificationCenter notificationCenter = NotificationCenter.getInstance(currentAccount);
        notificationCenter.addObserver(this, NotificationCenter.fileLoaded);
        notificationCenter.addObserver(this, NotificationCenter.fileLoadProgressChanged);
        notificationCenter.addObserver(this, NotificationCenter.fileLoadFailed);
        isObserverRegistered = true;
    }

    private void unregisterObserver() {
        if (!isObserverRegistered) {
            return;
        }
        NotificationCenter notificationCenter = NotificationCenter.getInstance(currentAccount);
        notificationCenter.removeObserver(this, NotificationCenter.fileLoaded);
        notificationCenter.removeObserver(this, NotificationCenter.fileLoadProgressChanged);
        notificationCenter.removeObserver(this, NotificationCenter.fileLoadFailed);
        isObserverRegistered = false;
    }

    private NotificationCompat.Builder buildFinalNotification(Context context, String text) {
        return new NotificationCompat.Builder(context, NotificationsController.OTHER_NOTIFICATIONS_CHANNEL)
                .setContentTitle(LocaleController.getString(R.string.CopySendNotificationTitle))
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true);
    }

    private NotificationCompat.Builder buildProgressNotification(Context context, int progress) {
        return new NotificationCompat.Builder(context, NotificationsController.OTHER_NOTIFICATIONS_CHANNEL)
                .setContentTitle(LocaleController.getString(R.string.CopySendNotificationTitle))
                .setContentText(LocaleController.getString(R.string.CopySendDownloadingToast))
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setOnlyAlertOnce(true)
                .setProgress(100, progress, false)
                .addAction(0, LocaleController.getString(R.string.CopySendNotificationCancel), buildCancelPendingIntent(context));
    }

    private void showProgressNotification(int progress) {
        Context context = ApplicationLoader.applicationContext;
        notifySafely(context, buildProgressNotification(context, progress));
    }

    private PendingIntent buildCancelPendingIntent(Context context) {
        Intent intent = new Intent(context, CopySendQueueReceiver.class);
        intent.setAction(CopySendQueueReceiver.ACTION_CANCEL);
        intent.putExtra("account", currentAccount);
        return PendingIntent.getBroadcast(context, 10 + currentAccount, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void notifySafely(Context context, NotificationCompat.Builder builder) {
        try {
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_TAG, NOTIFICATION_ID + currentAccount, builder.build());
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void notifyResultSafely(Context context, NotificationCompat.Builder builder) {
        try {
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_TAG + "_result", NOTIFICATION_ID + currentAccount, builder.build());
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
}
