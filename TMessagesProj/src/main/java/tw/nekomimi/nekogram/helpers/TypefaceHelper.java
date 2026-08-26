package tw.nekomimi.nekogram.helpers;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.TypefaceSpan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import tw.nekomimi.nekogram.NekoConfig;
import xyz.nextalone.nagram.NaConfig;

public class TypefaceHelper {

    public static final String FONT_CATEGORY_REGULAR = "regular";
    public static final String FONT_CATEGORY_BOLD = "bold";
    public static final String FONT_CATEGORY_ITALIC = "italic";
    public static final String FONT_CATEGORY_MONO = "mono";

    private static final HashMap<String, Typeface> fontCache = new HashMap<>();

    private static final String TEST_TEXT;
    private static final int CANVAS_SIZE = 40;
    private static final Paint PAINT = new Paint() {{
        setTextSize(20);
        setAntiAlias(false);
        setSubpixelText(false);
        setFakeBoldText(false);
    }};

    private static Boolean mediumWeightSupported = null;
    private static Boolean italicSupported = null;

    static {
        var lang = LocaleController.getInstance().getCurrentLocale().getLanguage();
        if (List.of("zh", "ja", "ko").contains(lang)) {
            TEST_TEXT = "你好";
        } else if (List.of("ar", "fa").contains(lang)) {
            TEST_TEXT = "مرحبا";
        } else if ("iw".equals(lang)) {
            TEST_TEXT = "שלום";
        } else if ("th".equals(lang)) {
            TEST_TEXT = "สวัสดี";
        } else if ("hi".equals(lang)) {
            TEST_TEXT = "नमस्ते";
        } else if (List.of("ru", "uk", "ky", "be", "sr").contains(lang)) {
            TEST_TEXT = "Привет";
        } else {
            TEST_TEXT = "R";
        }
    }

    // --- Font category management ---

    public static Typeface getCustomFontForCategory(String category) {
        tw.nekomimi.nekogram.config.ConfigItem config = switch (category) {
            case FONT_CATEGORY_REGULAR -> tw.nekomimi.nekogram.NekoConfig.customFontRegular;
            case FONT_CATEGORY_BOLD -> tw.nekomimi.nekogram.NekoConfig.customFontBold;
            case FONT_CATEGORY_ITALIC -> tw.nekomimi.nekogram.NekoConfig.customFontItalic;
            case FONT_CATEGORY_MONO -> tw.nekomimi.nekogram.NekoConfig.customFontMono;
            default -> null;
        };
        if (config == null) return null;
        String path = config.String();
        if (path == null || path.isEmpty()) return null;
        Typeface cached = fontCache.get(path);
        if (cached != null) return cached;
        try {
            Typeface tf;
            if (path.startsWith("__builtin__")) {
                String fontName = path.substring("__builtin__".length());
                tf = Typeface.create(fontName, Typeface.NORMAL);
            } else {
                tf = Typeface.createFromFile(path);
            }
            if (tf != null) {
                fontCache.put(path, tf);
            }
            return tf;
        } catch (Exception e) {
            FileLog.e("Failed to load custom font: " + path, e);
            return null;
        }
    }

    public static boolean hasCustomFontForCategory(String category) {
        tw.nekomimi.nekogram.config.ConfigItem config = switch (category) {
            case FONT_CATEGORY_REGULAR -> tw.nekomimi.nekogram.NekoConfig.customFontRegular;
            case FONT_CATEGORY_BOLD -> tw.nekomimi.nekogram.NekoConfig.customFontBold;
            case FONT_CATEGORY_ITALIC -> tw.nekomimi.nekogram.NekoConfig.customFontItalic;
            case FONT_CATEGORY_MONO -> tw.nekomimi.nekogram.NekoConfig.customFontMono;
            default -> null;
        };
        if (config == null) return false;
        String path = config.String();
        return path != null && !path.isEmpty();
    }

    public static boolean hasAnyCustomFont() {
        return hasCustomFontForCategory(FONT_CATEGORY_REGULAR)
                || hasCustomFontForCategory(FONT_CATEGORY_BOLD)
                || hasCustomFontForCategory(FONT_CATEGORY_ITALIC)
                || hasCustomFontForCategory(FONT_CATEGORY_MONO);
    }

    public static String getCustomFontName(String category) {
        tw.nekomimi.nekogram.config.ConfigItem config = switch (category) {
            case FONT_CATEGORY_REGULAR -> tw.nekomimi.nekogram.NekoConfig.customFontRegular;
            case FONT_CATEGORY_BOLD -> tw.nekomimi.nekogram.NekoConfig.customFontBold;
            case FONT_CATEGORY_ITALIC -> tw.nekomimi.nekogram.NekoConfig.customFontItalic;
            case FONT_CATEGORY_MONO -> tw.nekomimi.nekogram.NekoConfig.customFontMono;
            default -> null;
        };
        if (config == null) return "";
        String path = config.String();
        if (path == null || path.isEmpty()) return "";
        if (path.startsWith("__builtin__")) {
            return path.substring("__builtin__".length());
        }
        int lastSlash = path.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    public static void setFont(String category, String fontPath) {
        tw.nekomimi.nekogram.config.ConfigItem config = switch (category) {
            case FONT_CATEGORY_REGULAR -> tw.nekomimi.nekogram.NekoConfig.customFontRegular;
            case FONT_CATEGORY_BOLD -> tw.nekomimi.nekogram.NekoConfig.customFontBold;
            case FONT_CATEGORY_ITALIC -> tw.nekomimi.nekogram.NekoConfig.customFontItalic;
            case FONT_CATEGORY_MONO -> tw.nekomimi.nekogram.NekoConfig.customFontMono;
            default -> null;
        };
        if (config != null) {
            config.setConfigString(fontPath != null ? fontPath : "");
        }
    }

    public static void resetFont(String category) {
        setFont(category, null);
    }

    public static void resetAllFonts() {
        resetFont(FONT_CATEGORY_REGULAR);
        resetFont(FONT_CATEGORY_BOLD);
        resetFont(FONT_CATEGORY_ITALIC);
        resetFont(FONT_CATEGORY_MONO);
    }

    public static String importFontFile(String sourcePath, String fileName) {
        File fontsDir = ApplicationLoader.getFilesDirFixed("custom_fonts");
        File destFile = new File(fontsDir, fileName);
        // Handle name collisions
        if (destFile.exists()) {
            return destFile.getAbsolutePath();
        }
        try (FileInputStream fis = new FileInputStream(sourcePath);
             FileOutputStream fos = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            return destFile.getAbsolutePath();
        } catch (IOException e) {
            FileLog.e("Failed to import font file", e);
            return null;
        }
    }

    public static boolean isFontFile(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".ttf") || lower.endsWith(".otf");
    }

    // --- Asset path to category mapping ---

    public static String getAssetPathCategory(String assetPath) {
        if (assetPath == null) return null;
        return switch (assetPath) {
            case AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM,
                 AndroidUtilities.TYPEFACE_ROBOTO_EXTRA_BOLD,
                 AndroidUtilities.TYPEFACE_RCONDENSED_BOLD,
                 AndroidUtilities.TYPEFACE_MERRIWEATHER_BOLD -> FONT_CATEGORY_BOLD;
            case AndroidUtilities.TYPEFACE_RITALIC,
                 AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM_ITALIC -> FONT_CATEGORY_ITALIC;
            case AndroidUtilities.TYPEFACE_ROBOTO_MONO -> FONT_CATEGORY_MONO;
            default -> null;
        };
    }

    // --- Style resolution for TextStyleSpan ---

    public static Typeface getTypefaceForStyle(boolean bold, boolean italic, boolean mono) {
        if (mono) return getCustomFontForCategory(FONT_CATEGORY_MONO);
        if (bold && italic) {
            Typeface boldTf = getCustomFontForCategory(FONT_CATEGORY_BOLD);
            if (boldTf != null) return boldTf;
            Typeface italicTf = getCustomFontForCategory(FONT_CATEGORY_ITALIC);
            return italicTf;
        }
        if (bold) return getCustomFontForCategory(FONT_CATEGORY_BOLD);
        if (italic) return getCustomFontForCategory(FONT_CATEGORY_ITALIC);
        return null;
    }

    // --- Cache management ---

    public static void clearAllFontCaches() {
        fontCache.clear();
        AndroidUtilities.clearTypefaceCache();
        Theme.resetThemePaintsFonts();
    }

    // --- Existing methods (preserved) ---

    public static Typeface createTypeface(String assetPath) {
        return switch (assetPath) {
            case AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM -> {
                if (NekoConfig.forceFontWeightFallback.Bool()) {
                    yield createTypeface(700, false);
                }
                yield isMediumWeightSupported() ? Typeface.create("sans-serif-medium", Typeface.NORMAL) : Typeface.create("sans-serif", Typeface.BOLD);
            }
            case AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM_ITALIC -> {
                if (NekoConfig.forceFontWeightFallback.Bool()) {
                    yield createTypeface(700, true);
                }
                yield isMediumWeightSupported() ? Typeface.create("sans-serif-medium", Typeface.ITALIC) : Typeface.create("sans-serif", Typeface.BOLD_ITALIC);
            }
            case AndroidUtilities.TYPEFACE_RCONDENSED_BOLD ->
                    Typeface.create("sans-serif-condensed", Typeface.BOLD);
            case AndroidUtilities.TYPEFACE_ROBOTO_EXTRA_BOLD ->
                    createTypeface(800, false);
            case AndroidUtilities.TYPEFACE_RITALIC ->
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? Typeface.create(Typeface.SANS_SERIF, 400, true) : Typeface.create("sans-serif", Typeface.ITALIC);
            case AndroidUtilities.TYPEFACE_ROBOTO_MONO ->
                    Typeface.MONOSPACE;
            default -> createTypefaceFromAsset(assetPath);
        };
    }

    public static Typeface createTypefaceFromAsset(String assetPath) {
        Typeface.Builder builder = new Typeface.Builder(ApplicationLoader.applicationContext.getAssets(), assetPath);
        if (assetPath.contains("rextrabold")) {
            builder.setWeight(800);
        }
        if (assetPath.contains("medium") || assetPath.contains("rbold")) {
            builder.setWeight(700);
        }
        if (assetPath.contains("italic")) {
            builder.setItalic(true);
        }
        return builder.build();
    }

    public static boolean isMediumWeightSupported() {
        if (mediumWeightSupported == null) {
            mediumWeightSupported = testTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            FileLog.d("mediumWeightSupported = " + mediumWeightSupported);
        }
        return mediumWeightSupported;
    }

    public static boolean isItalicSupported() {
        if (italicSupported == null) {
            italicSupported = testTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
            FileLog.d("italicSupported = " + italicSupported);
        }
        return italicSupported;
    }

    private static boolean testTypeface(Typeface typeface) {
        Canvas canvas = new Canvas();

        Bitmap bitmap1 = Bitmap.createBitmap(CANVAS_SIZE * 2, CANVAS_SIZE, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap1);
        PAINT.setTypeface(null);
        canvas.drawText(TEST_TEXT, 0, CANVAS_SIZE, PAINT);

        Bitmap bitmap2 = Bitmap.createBitmap(CANVAS_SIZE * 2, CANVAS_SIZE, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap2);
        PAINT.setTypeface(typeface);
        canvas.drawText(TEST_TEXT, 0, CANVAS_SIZE, PAINT);

        boolean supported = !bitmap1.sameAs(bitmap2);
        AndroidUtilities.recycleBitmaps(List.of(bitmap1, bitmap2));
        return supported;
    }

    public static Typeface createTypeface(int weight, boolean italic) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Typeface.create(null, weight, italic);
        }
        if (weight == 700) {
            return Typeface.create("sans-serif", italic ? Typeface.BOLD_ITALIC : Typeface.BOLD);
        }
        var family = switch (weight) {
            case 800 -> "sans-serif-black";
            case 500 -> "sans-serif-medium";
            default -> "sans-serif";
        };
        return Typeface.create(family, italic ? Typeface.ITALIC : Typeface.NORMAL);
    }

    public static SpannableStringBuilder getTitleText(int currentAccount) {
        String title = NaConfig.INSTANCE.getCustomTitle().String();
        if (NaConfig.INSTANCE.getCustomTitleUserName().Bool()) {
            TLRPC.User self = UserConfig.getInstance(currentAccount).getCurrentUser();
            if (self != null && self.first_name != null) {
                title = self.first_name;
            }
        }
        var builder = new SpannableStringBuilder(title);
        builder.setSpan(new LeadingMarginSpan.Standard(dp(2), 0), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        Typeface titleTypeface = NekoConfig.typeface.Bool() && NekoConfig.forceFontWeightFallback.Bool() ? createTypeface(700, false) : createTypeface(600, false);
        builder.setSpan(new TypefaceSpan(titleTypeface, 0, Theme.key_telegram_color_dialogsLogo, null), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

}
