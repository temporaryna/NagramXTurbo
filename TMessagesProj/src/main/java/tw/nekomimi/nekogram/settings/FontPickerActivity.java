package tw.nekomimi.nekogram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import tw.nekomimi.nekogram.helpers.TypefaceHelper;

@SuppressLint("RtlHardcoded")
public class FontPickerActivity extends BaseNekoSettingsActivity {

    private final String category;

    private int defaultRow;
    private int importRow;
    private int builtinHeaderRow;
    private int builtinStartRow;
    private int builtinEndRow;
    private int systemHeaderRow;
    private int systemStartRow;
    private int systemEndRow;
    private int infoRow;

    private static final String[] BUILTIN_FONTS = {
            "sans-serif",
            "sans-serif-medium",
            "serif",
            "monospace"
    };

    private final List<String> systemFontNames = new ArrayList<>();

    private static final int REQUEST_CODE_IMPORT_FONT = 20001;

    public FontPickerActivity(String category) {
        this.category = category;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                java.util.Set<android.graphics.fonts.Font> fonts = android.graphics.fonts.SystemFonts.getAvailableFonts();
                for (android.graphics.fonts.Font font : fonts) {
                    String name = font.getFile().getName().replace(".ttf", "").replace(".otf", "");
                    if (!name.startsWith(".") && !systemFontNames.contains(name)) {
                        systemFontNames.add(name);
                    }
                }
            } catch (Exception e) {
                FileLog.e("Failed to load system fonts", e);
            }
        }
    }

    @Override
    protected void updateRows() {
        super.updateRows();
        defaultRow = addRow("default");
        importRow = addRow("import");
        builtinHeaderRow = addRow("builtin_header");
        builtinStartRow = addRow("builtin_start");
        for (int i = 1; i < BUILTIN_FONTS.length; i++) {
            addRow("builtin_" + i);
        }
        builtinEndRow = rowCount;
        systemHeaderRow = addRow("system_header");
        systemStartRow = addRow("system_start");
        for (int i = 1; i < systemFontNames.size(); i++) {
            addRow("system_" + i);
        }
        systemEndRow = rowCount;
        infoRow = addRow("info");
    }

    @Override
    protected String getActionBarTitle() {
        String suffix = switch (category) {
            case TypefaceHelper.FONT_CATEGORY_REGULAR -> getString(R.string.FontCategoryRegular);
            case TypefaceHelper.FONT_CATEGORY_BOLD -> getString(R.string.FontCategoryBold);
            case TypefaceHelper.FONT_CATEGORY_ITALIC -> getString(R.string.FontCategoryItalic);
            case TypefaceHelper.FONT_CATEGORY_MONO -> getString(R.string.FontCategoryMono);
            default -> "";
        };
        return getString(R.string.FontsSettings) + " — " + suffix;
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == defaultRow) {
            TypefaceHelper.resetFont(category);
            applyAndFinish();
        } else if (position == importRow) {
            openFontFilePicker();
        } else if (position >= builtinStartRow && position < builtinEndRow) {
            int idx = position - builtinStartRow;
            String fontName = BUILTIN_FONTS[idx];
            String path = "__builtin__" + fontName;
            TypefaceHelper.setFont(category, path);
            applyAndFinish();
        } else if (position >= systemStartRow && position < systemEndRow) {
            int idx = position - systemStartRow;
            String fontName = systemFontNames.get(idx);
            try {
                File fontFile = new File("/system/fonts/", fontName + ".ttf");
                if (!fontFile.exists()) {
                    fontFile = new File("/system/fonts/", fontName + ".otf");
                }
                if (fontFile.exists()) {
                    String importedPath = TypefaceHelper.importFontFile(fontFile.getAbsolutePath(), fontFile.getName());
                    if (importedPath != null) {
                        TypefaceHelper.setFont(category, importedPath);
                        applyAndFinish();
                    }
                }
            } catch (Exception e) {
                FileLog.e("Failed to apply system font", e);
            }
        }
    }

    private void applyAndFinish() {
        TypefaceHelper.clearAllFontCaches();
        finishFragment();
    }

    private void openFontFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf"});
            startActivityForResult(intent, REQUEST_CODE_IMPORT_FONT);
        } catch (Exception e) {
            FileLog.e("Failed to open font file picker", e);
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_IMPORT_FONT && data != null && data.getData() != null) {
            importFontFromUri(data.getData());
        }
    }

    private void importFontFromUri(Uri uri) {
        try {
            String fileName = "font";
            if (uri.getLastPathSegment() != null) {
                String last = uri.getLastPathSegment();
                int slash = last.lastIndexOf('/');
                if (slash >= 0) last = last.substring(slash + 1);
                fileName = last;
            }
            if (!TypefaceHelper.isFontFile(fileName)) {
                fileName += ".ttf";
            }

            File tempFile = new File(ApplicationLoader.getFilesDirFixed("custom_fonts"), "temp_" + fileName);
            try (java.io.InputStream is = ApplicationLoader.applicationContext.getContentResolver().openInputStream(uri);
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while (is != null && (len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            }

            try {
                Typeface tf = Typeface.createFromFile(tempFile);
                if (tf == null) {
                    showFontError();
                    tempFile.delete();
                    return;
                }
            } catch (Exception e) {
                showFontError();
                tempFile.delete();
                return;
            }

            String importedPath = TypefaceHelper.importFontFile(tempFile.getAbsolutePath(), fileName);
            tempFile.delete();

            if (importedPath != null) {
                TypefaceHelper.setFont(category, importedPath);
                applyAndFinish();
            }
        } catch (Exception e) {
            FileLog.e("Failed to import font from URI", e);
            showFontError();
        }
    }

    private void showFontError() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.ErrorOccurred));
        builder.setMessage("Failed to load font file. Make sure it's a valid .ttf or .otf file.");
        builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        showDialog(builder.create());
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == importRow) {
                return TYPE_TEXT;
            } else if (position == builtinHeaderRow || position == systemHeaderRow) {
                return TYPE_HEADER;
            } else if (position == infoRow) {
                return TYPE_INFO_PRIVACY;
            } else {
                return TYPE_SETTINGS;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case TYPE_TEXT:
                    view = new TextCell(mContext);
                    break;
                case TYPE_HEADER:
                    view = new tw.nekomimi.nekogram.ui.cells.HeaderCell(mContext);
                    break;
                case TYPE_INFO_PRIVACY:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
                default:
                    view = new FontCell(mContext);
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (position == defaultRow) {
                FontCell cell = (FontCell) holder.itemView;
                cell.set(category, getString(R.string.FontDefault), null, false);
            } else if (position == importRow) {
                TextCell cell = (TextCell) holder.itemView;
                cell.setTextAndIcon(getString(R.string.FontImportFromFile), R.drawable.msg_gallery, false);
            } else if (position == builtinHeaderRow) {
                ((tw.nekomimi.nekogram.ui.cells.HeaderCell) holder.itemView).setText(getString(R.string.FontBuiltinFonts));
            } else if (position >= builtinStartRow && position < builtinEndRow) {
                FontCell cell = (FontCell) holder.itemView;
                int idx = position - builtinStartRow;
                String name = BUILTIN_FONTS[idx];
                Typeface tf = Typeface.create(name, Typeface.NORMAL);
                cell.set(category, name, tf, position < builtinEndRow - 1);
            } else if (position == systemHeaderRow) {
                ((tw.nekomimi.nekogram.ui.cells.HeaderCell) holder.itemView).setText(getString(R.string.FontSystemFonts));
            } else if (position >= systemStartRow && position < systemEndRow) {
                FontCell cell = (FontCell) holder.itemView;
                int idx = position - systemStartRow;
                String name = systemFontNames.get(idx);
                Typeface tf = null;
                try {
                    File f = new File("/system/fonts/", name + ".ttf");
                    if (!f.exists()) f = new File("/system/fonts/", name + ".otf");
                    if (f.exists()) tf = Typeface.createFromFile(f);
                } catch (Exception ignored) {}
                cell.set(category, name, tf, position < systemEndRow - 1);
            } else if (position == infoRow) {
                ((TextInfoPrivacyCell) holder.itemView).setText(getString(R.string.FontPreviewText));
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int pos = holder.getAdapterPosition();
            return pos != builtinHeaderRow && pos != systemHeaderRow && pos != infoRow;
        }
    }

    private static class FontCell extends FrameLayout {

        private final SimpleTextView titleTextView;
        private final SimpleTextView previewTextView;

        public FontCell(Context context) {
            super(context);
            setWillNotDraw(false);

            titleTextView = new SimpleTextView(context);
            titleTextView.setTextSize(16);
            titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleTextView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            addView(titleTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 20, 10, 20, 0));

            previewTextView = new SimpleTextView(context);
            previewTextView.setTextSize(14);
            previewTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            previewTextView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            addView(previewTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 20, 32, 20, 10));
        }

        public void set(String category, String name, Typeface typeface, boolean divider) {
            titleTextView.setText(name);
            if (typeface != null) {
                titleTextView.setTypeface(typeface);
                previewTextView.setTypeface(typeface);
            } else {
                titleTextView.setTypeface(null);
                previewTextView.setTypeface(null);
            }
            previewTextView.setText(getString(R.string.FontPreviewText));

            String currentName = TypefaceHelper.getCustomFontName(category);
            boolean isSelected = !currentName.isEmpty() && currentName.equals(name);
            if (isSelected) {
                titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
            } else {
                titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            }

            setWillNotDraw(!divider);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (getAlpha() == 1f) {
                canvas.drawLine(dp(20), getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }
    }
}
