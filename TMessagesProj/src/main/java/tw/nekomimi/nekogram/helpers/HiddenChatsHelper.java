package tw.nekomimi.nekogram.helpers;

import android.content.Context;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;
import android.util.LongSparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseArray;
import android.util.SparseLongArray;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.DialogObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.HiddenChatsPasscodeDialog;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class HiddenChatsHelper {

    public static final int NOTIF_MODE_NORMAL = 0;
    public static final int NOTIF_MODE_NO_CONTENT = 1;
    public static final int NOTIF_MODE_ALIAS = 2;
    public static final int NOTIF_MODE_HIDE = 3;

    public static final int CALL_MODE_NORMAL = 0;
    public static final int CALL_MODE_ALIAS = 1;
    public static final int CALL_MODE_HIDE = 2;

    public static final long AUTO_LOCK_IMMEDIATELY = 0;
    public static final long AUTO_LOCK_NEVER = -1;

    public static final long ZONE_HOLD_DURATION_MS = 3_000;

    public static final long HOLD_SHOW_DURATION_MS = 5_000;

    public static final long HOLD_HIDE_DURATION_MS = 2_000;

    private static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("turbohidden", Context.MODE_PRIVATE);

    private static final SparseBooleanArray unlocked = new SparseBooleanArray();
    private static final SparseLongArray lockedAt = new SparseLongArray();
    private static final SparseArray<Set<Long>> hiddenIdsCache = new SparseArray<>();
    private static final android.util.SparseIntArray failedTriesByAccount = new android.util.SparseIntArray();
    private static final android.util.SparseLongArray lockedUntilByAccount = new android.util.SparseLongArray();

    public static int getFailedTries(int account) {
        return failedTriesByAccount.get(account, 0);
    }

    public static void setFailedTries(int account, int value) {
        failedTriesByAccount.put(account, value);
    }

    public static long getLockedUntil(int account) {
        return lockedUntilByAccount.get(account, 0);
    }

    public static void setLockedUntil(int account, long value) {
        lockedUntilByAccount.put(account, value);
    }

    private static int currentAccount() {
        return UserConfig.selectedAccount;
    }

    private static String key(String name, int account) {
        return name + account;
    }

    public static boolean isFeatureAvailable(int account) {
        return hasPinForAccount(account);
    }

    public static boolean hasPinForAccount(int account) {
        return preferences.contains(key("pinHash", account)) && preferences.contains(key("pinSalt", account));
    }

    public static void setPinForAccount(String pin, int account) {
        try {
            byte[] salt = new byte[16];
            Utilities.random.nextBytes(salt);
            byte[] pinBytes = pin.getBytes(StandardCharsets.UTF_8);
            byte[] bytes = new byte[32 + pinBytes.length];
            System.arraycopy(salt, 0, bytes, 0, 16);
            System.arraycopy(pinBytes, 0, bytes, 16, pinBytes.length);
            System.arraycopy(salt, 0, bytes, pinBytes.length + 16, 16);
            preferences.edit()
                    .putString(key("pinHash", account), Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length)))
                    .putString(key("pinSalt", account), Base64.encodeToString(salt, Base64.DEFAULT))
                    .apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static boolean checkPin(String pin, int account) {
        try {
            String saltString = preferences.getString(key("pinSalt", account), "");
            byte[] salt = saltString.length() > 0 ? Base64.decode(saltString, Base64.DEFAULT) : new byte[0];
            byte[] pinBytes = pin.getBytes(StandardCharsets.UTF_8);
            byte[] bytes = new byte[32 + pinBytes.length];
            System.arraycopy(salt, 0, bytes, 0, 16);
            System.arraycopy(pinBytes, 0, bytes, 16, pinBytes.length);
            System.arraycopy(salt, 0, bytes, pinBytes.length + 16, 16);
            String hash = Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length));
            return hash.equals(preferences.getString(key("pinHash", account), ""));
        } catch (Exception e) {
            FileLog.e(e);
        }
        return false;
    }

    public static void removePinForAccount(int account) {
        hiddenIdsCache.remove(account);
        failedTriesByAccount.delete(account);
        lockedUntilByAccount.delete(account);
        preferences.edit()
                .remove(key("pinHash", account))
                .remove(key("pinSalt", account))
                .remove(key("hiddenIds", account))
                .remove(key("aliases", account))
                .remove(key("stdAlias", account))
                .remove(key("fp", account))
                .remove(key("notifMode", account))
                .remove(key("callMode", account))
                .remove(key("autoLock", account))
                .apply();
        markLocked(account, "pin-removed");
    }

    public static void resetAllForAccount(int account) {
        removePinForAccount(account);
        preferences.edit().remove(key("holdToggle", account)).apply();
    }

    public static Set<Long> getHiddenIds(int account) {
        Set<Long> cached = hiddenIdsCache.get(account);
        if (cached != null) {
            return cached;
        }
        Set<String> raw = preferences.getStringSet(key("hiddenIds", account), Collections.emptySet());
        Set<Long> ids = new HashSet<>();
        for (String value : raw) {
            try {
                ids.add(Long.parseLong(value));
            } catch (NumberFormatException ignore) {
            }
        }
        hiddenIdsCache.put(account, ids);
        return ids;
    }

    public static boolean isHidden(long dialogId) {
        return getHiddenIds(currentAccount()).contains(dialogId);
    }

    public static boolean isHidden(int account, long dialogId) {
        return getHiddenIds(account).contains(dialogId);
    }

    public static boolean shouldSuppressNotification(int account, long dialogId) {
        return isHidden(account, dialogId) && getNotificationsMode(account) == NOTIF_MODE_HIDE;
    }

    public static boolean shouldMaskNotificationContent(int account, long dialogId) {
        return isHidden(account, dialogId) && getNotificationsMode(account) == NOTIF_MODE_NO_CONTENT;
    }

    public static boolean shouldAliasNotification(int account, long dialogId) {
        return isHidden(account, dialogId) && getNotificationsMode(account) == NOTIF_MODE_ALIAS;
    }

    public static boolean shouldSuppressCall(int account, long dialogId) {
        return isHidden(account, dialogId) && getCallMode(account) == CALL_MODE_HIDE;
    }

    public static boolean shouldAliasCall(int account, long dialogId) {
        return isHidden(account, dialogId) && getCallMode(account) == CALL_MODE_ALIAS;
    }

    public static String getAliasFor(int account, long dialogId) {
        String alias = aliasMapFor(account).get(dialogId);
        if (alias == null || alias.isEmpty()) {
            alias = getStandardAlias(account);
        }
        if (alias == null || alias.isEmpty()) {
            alias = LocaleController.getString(R.string.HiddenChatsNeutralContact);
        }
        return alias;
    }

    public static void setHidden(long dialogId, boolean hidden) {
        int account = currentAccount();
        Set<String> raw = new HashSet<>(preferences.getStringSet(key("hiddenIds", account), Collections.emptySet()));
        String id = String.valueOf(dialogId);
        if (hidden) {
            raw.add(id);
        } else {
            raw.remove(id);
        }
        hiddenIdsCache.remove(account);
        preferences.edit().putStringSet(key("hiddenIds", account), raw).apply();
        MessagesController.getInstance(account).sortDialogs(null);
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload, true);
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.hiddenChatsLocked);
        MediaDataController.getInstance(account).buildShortcuts();
    }

    public static String getStandardAlias(int account) {
        return preferences.getString(key("stdAlias", account), "");
    }

    private static final String[] ALIAS_ADJECTIVES_RU = {"Оранжевый", "Тихий", "Синий", "Зимний", "Лунный", "Бархатный", "Северный", "Янтарный"};
    private static final String[] ALIAS_NOUNS_RU = {"дельфин", "маяк", "сад", "мост", "кит", "ветер", "квадрат", "берег"};
    private static final String[] ALIAS_ADJECTIVES_EN = {"Amber", "Silent", "Blue", "Winter", "Lunar", "Velvet", "Northern", "Sunny"};
    private static final String[] ALIAS_NOUNS_EN = {"dolphin", "beacon", "garden", "bridge", "whale", "wind", "square", "shore"};

    public static String randomAlias() {
        String language = LocaleController.getInstance().currentLocale.getLanguage();
        String[] adjectives = language.equals("ru") || language.equals("uk") ? ALIAS_ADJECTIVES_RU : ALIAS_ADJECTIVES_EN;
        String[] nouns = language.equals("ru") || language.equals("uk") ? ALIAS_NOUNS_RU : ALIAS_NOUNS_EN;
        return adjectives[Utilities.random.nextInt(adjectives.length)] + " " +
                nouns[Utilities.random.nextInt(nouns.length)];
    }

    public static void setStandardAlias(String alias, int account) {
        preferences.edit().putString(key("stdAlias", account), alias == null ? "" : alias).apply();
    }

    public static String getAliasFor(long dialogId) {
        int account = currentAccount();
        String alias = aliasMapFor(account).get(dialogId);
        if (alias == null || alias.isEmpty()) {
            alias = getStandardAlias(account);
        }
        if (alias == null || alias.isEmpty()) {
            alias = LocaleController.getString(R.string.HiddenChatsNeutralContact);
        }
        return alias;
    }

    public static void setAliasFor(long dialogId, String alias, int account) {
        LongSparseArray<String> map = aliasMapFor(account);
        if (alias == null || alias.isEmpty()) {
            map.remove(dialogId);
        } else {
            map.put(dialogId, alias);
        }
        preferences.edit().putString(key("aliases", account), serializeAliasMap(map)).apply();
    }

    public static void removeAliasFor(long dialogId, int account) {
        setAliasFor(dialogId, null, account);
    }

    public static LongSparseArray<String> aliasMapFor(int account) {
        LongSparseArray<String> map = new LongSparseArray<>();
        String raw = preferences.getString(key("aliases", account), "");
        if (raw.isEmpty()) {
            return map;
        }
        try {
            for (String entry : raw.split(";")) {
                int sep = entry.indexOf('=');
                if (sep <= 0) {
                    continue;
                }
                map.put(Long.parseLong(entry.substring(0, sep)), entry.substring(sep + 1));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return map;
    }

    private static String serializeAliasMap(LongSparseArray<String> map) {
        StringBuilder builder = new StringBuilder();
        for (int a = 0; a < map.size(); a++) {
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(map.keyAt(a)).append('=').append(map.valueAt(a));
        }
        return builder.toString();
    }

    public static boolean isFingerprintEnabled(int account) {
        return preferences.getBoolean(key("fp", account), false);
    }

    public static void setFingerprintEnabled(boolean enabled, int account) {
        preferences.edit().putBoolean(key("fp", account), enabled).apply();
    }

    public static boolean isHoldToggleEnabled(int account) {
        return preferences.getBoolean(key("holdToggle", account), false);
    }

    public static void setHoldToggleEnabled(boolean enabled, int account) {
        preferences.edit().putBoolean(key("holdToggle", account), enabled).apply();
    }

    public static int getNotificationsMode(int account) {
        return preferences.getInt(key("notifMode", account), NOTIF_MODE_HIDE);
    }

    public static void setNotificationsMode(int mode, int account) {
        preferences.edit().putInt(key("notifMode", account), mode).apply();
    }

    public static int getCallMode(int account) {
        return preferences.getInt(key("callMode", account), CALL_MODE_ALIAS);
    }

    public static void setCallMode(int mode, int account) {
        preferences.edit().putInt(key("callMode", account), mode).apply();
    }

    public static long getAutoLockSeconds(int account) {
        return preferences.getLong(key("autoLock", account), AUTO_LOCK_IMMEDIATELY);
    }

    public static void setAutoLockSeconds(long seconds, int account) {
        preferences.edit().putLong(key("autoLock", account), seconds).apply();
    }

    public static synchronized boolean isUnlocked(int account) {
        if (!hasPinForAccount(account) || !unlocked.get(account)) {
            return false;
        }
        long autoLock = getAutoLockSeconds(account);
        if (autoLock == AUTO_LOCK_NEVER || autoLock == AUTO_LOCK_IMMEDIATELY) {
            return true;
        }
        long inactiveFor = (System.currentTimeMillis() - lockedAt.get(account, System.currentTimeMillis())) / 1000;
        return inactiveFor < autoLock;
    }

    public static synchronized void markUnlocked(int account) {
        unlocked.put(account, true);
        lockedAt.put(account, System.currentTimeMillis());
        reloadDialogs(account);
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.hiddenChatsLocked);
        MediaDataController.getInstance(account).buildShortcuts();
    }

    public static synchronized void markLocked(int account, String reason) {
        unlocked.put(account, false);
        lockedAt.put(account, System.currentTimeMillis());
        reloadDialogs(account);
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.hiddenChatsLocked);
        MediaDataController.getInstance(account).buildShortcuts();
    }

    private static void reloadDialogs(int account) {
        MessagesController.getInstance(account).sortDialogs(null);
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload, true);
    }


    public static synchronized void onAppPause(int account) {
        if (getAutoLockSeconds(account) == AUTO_LOCK_IMMEDIATELY && unlocked.get(account)) {
            markLocked(account, "app-pause");
        }
    }

    public static synchronized void noteUserActivity() {
        long now = System.currentTimeMillis();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (unlocked.get(a)) {
                lockedAt.put(a, now);
            }
        }
    }

    public static synchronized void onAppPauseAll() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            onAppPause(a);
        }
    }

    public static synchronized void onAppResumeAll() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            onAppResume(a);
        }
    }

    public static synchronized void onAppResume(int account) {
        long autoLock = getAutoLockSeconds(account);
        if (autoLock == AUTO_LOCK_NEVER || autoLock == AUTO_LOCK_IMMEDIATELY || !unlocked.get(account)) {
            return;
        }
        if ((System.currentTimeMillis() - lockedAt.get(account, System.currentTimeMillis())) / 1000 >= autoLock) {
            markLocked(account, "auto-lock-timeout");
        }
    }

    public static boolean isSecureWindowRequired(int account) {
        return isUnlocked(account);
    }

    public static boolean isEntryLocked(long dialogId) {
        return isEntryLocked(currentAccount(), dialogId);
    }

    public static boolean interceptChatPresent(BaseFragment fragment, ActionBarLayout layout, ActionBarLayout.NavigationParams params) {
        if (!(fragment instanceof ChatActivity) || !isEntryLocked(currentAccount(), ((ChatActivity) fragment).getDialogId())) {
            return false;
        }
        long lockedDialogId = ((ChatActivity) fragment).getDialogId();
        Bundle lockedArgs = fragment.getArguments();
        requestUnlock(layout.getContext(), getOpenSubtitle(lockedDialogId), () -> {
            ChatActivity replayFragment = new ChatActivity(lockedArgs);
            ActionBarLayout.NavigationParams replay = new ActionBarLayout.NavigationParams(replayFragment);
            replay.removeLast = params.removeLast;
            replay.noAnimation = params.noAnimation;
            layout.presentFragment(replay);
        });
        return true;
    }

    public static boolean isEntryLocked(int account, long dialogId) {
        return isInvisibleDialog(account, dialogId);
    }

    public static boolean isInvisibleDialog(int account, long dialogId) {
        return isHidden(account, dialogId) && !isUnlocked(account);
    }

    public static boolean isStrikedName(int account, long dialogId) {
        return isHidden(account, dialogId) && isUnlocked(account);
    }

    public static void requestUnlock(Context context, String subtitle, Runnable onSuccess) {
        int account = currentAccount();
        new HiddenChatsPasscodeDialog(context, subtitle, () -> {
            markUnlocked(account);
            if (onSuccess != null) {
                onSuccess.run();
            }
        }).show();
    }

    public static void requestUnlockWithPin(Context context, String subtitle, Runnable onSuccess) {
        int account = currentAccount();
        new HiddenChatsPasscodeDialog(context, subtitle, () -> {
            markUnlocked(account);
            if (onSuccess != null) {
                onSuccess.run();
            }
        }, true).show();
    }

    public static String getOpenSubtitle(long dialogId) {
        return LocaleController.formatString(R.string.HiddenChatsEnterPasswordToOpen, getDialogTitle(dialogId));
    }

    public static String getDialogTitle(long dialogId) {
        try {
            MessagesController messagesController = MessagesController.getInstance(currentAccount());
            if (DialogObject.isUserDialog(dialogId)) {
                TLRPC.User user = messagesController.getUser(dialogId);
                if (user != null) {
                    return UserObject.getUserName(user);
                }
            } else if (DialogObject.isChatDialog(dialogId)) {
                TLRPC.Chat chat = messagesController.getChat(-dialogId);
                if (chat != null && !TextUtils.isEmpty(chat.title)) {
                    return chat.title;
                }
            } else if (DialogObject.isEncryptedDialog(dialogId)) {
                TLRPC.EncryptedChat encryptedChat = messagesController.getEncryptedChat(DialogObject.getEncryptedChatId(dialogId));
                if (encryptedChat != null) {
                    TLRPC.User user = messagesController.getUser(encryptedChat.user_id);
                    if (user != null) {
                        return UserObject.getUserName(user);
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return LocaleController.getString(R.string.HiddenChatsNeutralContact);
    }

    public static void cleanupForAccount(int account) {
        resetAllForAccount(account);
    }
}
