package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import xyz.nextalone.nagram.NaConfig;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        LauncherIcon chosen = getSavedLauncherIcon();
        if (chosen == null) {
            chosen = findMigratableIcon(pm, ctx);
            if (chosen == null) {
                chosen = LauncherIcon.TURBO;
            }
            saveLauncherIcon(chosen);
        }
        LauncherIcon enabledIcon = null;
        boolean anomaly = false;
        for (LauncherIcon icon : LauncherIcon.values()) {
            boolean keyEnabled = isEnabledState(pm, ctx, icon.getComponentName(ctx));
            boolean modernEnabled = icon.modernKey != null && isEnabledState(pm, ctx, component(ctx, icon.modernKey));
            if (keyEnabled && modernEnabled) {
                anomaly = true;
            }
            if (keyEnabled || modernEnabled) {
                if (enabledIcon == null) {
                    enabledIcon = icon;
                } else {
                    anomaly = true;
                }
            }
        }
        String expectedTarget = chosen.modernKey != null && NaConfig.INSTANCE.getModernClassicIcons().Bool() ? chosen.modernKey : chosen.key;
        if (enabledIcon == chosen && !isEnabledState(pm, ctx, component(ctx, expectedTarget))) {
            anomaly = true;
        }
        if (enabledIcon == null || anomaly || enabledIcon != chosen) {
            if (pm.getComponentEnabledSetting(component(ctx, expectedTarget)) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
                return;
            }
            setIcon(chosen);
        }
    }

    private static LauncherIcon findMigratableIcon(PackageManager pm, Context ctx) {
        LauncherIcon found = null;
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (icon == LauncherIcon.TURBO) {
                continue;
            }
            boolean enabled = isEnabledState(pm, ctx, icon.getComponentName(ctx))
                    || (icon.modernKey != null && isEnabledState(pm, ctx, component(ctx, icon.modernKey)));
            if (enabled) {
                if (found != null) {
                    return null;
                }
                found = icon;
            }
        }
        return found;
    }

    private static boolean isEnabledState(PackageManager pm, Context ctx, ComponentName cn) {
        return pm.getComponentEnabledSetting(cn) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        if (hasEnabledAlias(pm, ctx, icon)) {
            return true;
        }
        return pm.getComponentEnabledSetting(icon.getComponentName(ctx)) == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.TURBO;
    }

    private static boolean hasEnabledAlias(PackageManager pm, Context ctx, LauncherIcon icon) {
        if (pm.getComponentEnabledSetting(icon.getComponentName(ctx)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            return true;
        }
        return icon.modernKey != null && pm.getComponentEnabledSetting(component(ctx, icon.modernKey)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    public static void setIcon(LauncherIcon icon) {
        setIcon(icon, false, false);
    }

    public static void setIcon(LauncherIcon icon, boolean force, boolean killApp) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        String target = icon.modernKey != null && NaConfig.INSTANCE.getModernClassicIcons().Bool() ? icon.modernKey : icon.key;
        saveLauncherIcon(icon);
        for (LauncherIcon i : LauncherIcon.values()) {
            if (!i.key.equals(target)) {
                applyComponentState(pm, ctx, component(ctx, i.key), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, false, false);
            }
            if (i.modernKey != null && !i.modernKey.equals(target)) {
                applyComponentState(pm, ctx, component(ctx, i.modernKey), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, false, false);
            }
        }
        applyComponentState(pm, ctx, component(ctx, target), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, force, killApp);
    }

    private static LauncherIcon getSavedLauncherIcon() {
        String savedKey = NaConfig.INSTANCE.getLauncherIcon().String();
        if (TextUtils.isEmpty(savedKey)) {
            return null;
        }
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (icon.key.equals(savedKey)) {
                return icon;
            }
        }
        return null;
    }

    private static void saveLauncherIcon(LauncherIcon icon) {
        NaConfig.INSTANCE.getLauncherIcon().setConfigString(icon.key);
        NaConfig.INSTANCE.getPreferences().edit().putString(NaConfig.INSTANCE.getLauncherIcon().key, icon.key).commit();
    }

    public static LauncherIcon getActiveIcon() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return icon;
            }
        }
        return LauncherIcon.TURBO;
    }

    public static int resolveNotificationIconResId(int configValue) {
        if (configValue == 1 || NaConfig.INSTANCE.getNotificationIconAsAppIcon().Bool()) {
            return getActiveIcon().notification;
        }
        switch (configValue) {
            case 0:
                return R.drawable.notification;
            case 2:
                return R.drawable.neko_notification;
        }
        return R.drawable.ic_notification_turbo;
    }

    private static ComponentName component(Context ctx, String key) {
        return new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
    }

    private static void applyComponentState(PackageManager pm, Context ctx, ComponentName cn, int state) {
        applyComponentState(pm, ctx, cn, state, false, false);
    }

    private static void applyComponentState(PackageManager pm, Context ctx, ComponentName cn, int state, boolean force, boolean killApp) {
        int current = pm.getComponentEnabledSetting(cn);
        if (current == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
            return;
        }
        if (killApp) {
            pm.setComponentEnabledSetting(cn, state, 0);
            return;
        }
        if (!force && current == state) {
            return;
        }
        pm.setComponentEnabledSetting(cn, state, PackageManager.DONT_KILL_APP);
    }

    public enum LauncherIcon {
        TELEGRAM("TelegramIcon", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconTelegramOriginal, R.drawable.notification, "TelegramIconModern", R.drawable.ic_telegram_modern_background, R.drawable.ic_telegram_modern_foreground),
        VINTAGE("VintageIcon", R.drawable.icon_6_background_sa, R.mipmap.icon_6_foreground_sa, R.string.AppIconVintage, R.drawable.notification, "VintageIconModern", R.drawable.ic_vintage_modern_background, R.drawable.ic_vintage_modern_foreground),
        AQUA("AquaIcon", R.drawable.icon_4_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconAqua, R.drawable.notification, "AquaIconModern", R.drawable.ic_aqua_modern_background, R.drawable.ic_aqua_modern_foreground),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.mipmap.icon_3_foreground_sa, R.string.AppIconPremium, R.drawable.notification, "PremiumIconModern", R.drawable.ic_premium_modern_background, R.drawable.ic_premium_modern_foreground),
        CLASSIC("TurboIcon", R.drawable.icon_5_background_sa, R.mipmap.icon_5_foreground_sa, R.string.AppIconClassic, R.drawable.notification, "TurboIconModern", R.drawable.ic_classic_modern_background, R.drawable.ic_classic_modern_foreground),
        NOX("NoxIcon", R.mipmap.icon_2_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconNox, R.drawable.notification, "NoxIconModern", R.drawable.ic_nox_modern_background, R.drawable.ic_nox_modern_foreground),
        TURBO("TurboDefaultIcon", R.drawable.ic_turbo_background, R.drawable.ic_turbo_foreground, R.string.AppIconTurbo, R.drawable.ic_notification_turbo, null, 0, 0),
        SKY("SkyIcon", R.drawable.ic_sky_background, R.drawable.ic_sky_foreground, R.string.AppIconSky, R.drawable.ic_notification_sky, null, 0, 0),
        SUNSET("SunsetIcon", R.drawable.ic_sunset_background, R.drawable.ic_sunset_foreground, R.string.AppIconSunset, R.drawable.ic_notification_sunset, null, 0, 0),
        BLUE_NIGHT("BlueNightIcon", R.drawable.ic_blue_night_background, R.drawable.ic_blue_night_foreground, R.string.AppIconBlueNight, R.drawable.ic_notification_blue_night, null, 0, 0),
        HALLOWEEN("HalloweenIcon", R.drawable.ic_halloween_background, R.drawable.ic_halloween_foreground, R.string.AppIconHalloween, R.drawable.ic_notification_halloween, null, 0, 0),
        PAPER_BOX("PaperBoxIcon", R.drawable.ic_paper_box_background, R.drawable.ic_paper_box_foreground, R.string.AppIconPaperBox, R.drawable.ic_notification_paper_box, null, 0, 0),
        PAPER_FIRE("PaperFireIcon", R.drawable.ic_paper_fire_background, R.drawable.ic_paper_fire_foreground, R.string.AppIconPaperFire, R.drawable.ic_notification_paper_fire, null, 0, 0),
        CARBON("CarbonIcon", R.drawable.ic_carbon_background, R.drawable.ic_carbon_foreground, R.string.AppIconCarbon, R.drawable.ic_notification_carbon, null, 0, 0),
        GOLD("GoldIcon", R.drawable.ic_gold_background, R.drawable.ic_gold_foreground, R.string.AppIconGold, R.drawable.ic_notification_gold, null, 0, 0, true),
        MATRIX("MatrixIcon", R.drawable.ic_matrix_background, R.drawable.ic_matrix_foreground, R.string.AppIconMatrix, R.drawable.ic_notification_matrix, null, 0, 0, true),
        NEON("NeonIcon", R.drawable.ic_neon_background, R.drawable.ic_neon_foreground, R.string.AppIconNeon, R.drawable.ic_notification_neon, null, 0, 0, true),
        SPACE("SpaceIcon", R.drawable.ic_space_background, R.drawable.ic_space_foreground, R.string.AppIconSpace, R.drawable.ic_notification_space, null, 0, 0, true),
        HEXAGON("HexagonIcon", R.drawable.ic_hexagon_background, R.drawable.ic_hexagon_foreground, R.string.AppIconHexagon, R.drawable.ic_notification_hexagon, null, 0, 0, true),
        PIXEL("PixelIcon", R.drawable.ic_pixel_background, R.drawable.ic_pixel_foreground, R.string.AppIconPixel, R.drawable.ic_notification_pixel, null, 0, 0, true),
        GLITCH("GlitchIcon", R.drawable.ic_glitch_background, R.drawable.ic_glitch_foreground, R.string.AppIconGlitch, R.drawable.ic_notification_glitch, null, 0, 0, true);

        public final String key;
        public final String modernKey;
        public final int background;
        public final int foreground;
        public final int modernBackground;
        public final int modernForeground;
        public final int title;
        public final boolean premium;
        public final boolean isHidden;
        public final int notification;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title, int notification, String modernKey, int modernBackground, int modernForeground) {
            this(key, background, foreground, title, false, notification, modernKey, modernBackground, modernForeground, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, int notification, String modernKey, int modernBackground, int modernForeground, boolean isHidden) {
            this(key, background, foreground, title, false, notification, modernKey, modernBackground, modernForeground, isHidden);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium, int notification, String modernKey, int modernBackground, int modernForeground, boolean isHidden) {
            this.key = key;
            this.modernKey = modernKey;
            this.background = background;
            this.foreground = foreground;
            this.modernBackground = modernBackground;
            this.modernForeground = modernForeground;
            this.title = title;
            this.premium = premium;
            this.isHidden = isHidden;
            this.notification = notification;
        }
    }
}
