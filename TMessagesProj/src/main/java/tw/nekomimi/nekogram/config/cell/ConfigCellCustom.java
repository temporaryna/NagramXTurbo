package tw.nekomimi.nekogram.config.cell;

import androidx.recyclerview.widget.RecyclerView;

public class ConfigCellCustom extends AbstractConfigCell implements WithKey {
    public static final int CUSTOM_ITEM_StickerSize = 998;
    public static final int CUSTOM_ITEM_CharBlurAlpha = 997;
    public static final int CUSTOM_ITEM_EmojiSet = 996;
    public static final int CUSTOM_ITEM_Temperature = 995;
    public static final int CUSTOM_ITEM_FontRegular = 993;
    public static final int CUSTOM_ITEM_FontBold = 992;
    public static final int CUSTOM_ITEM_FontItalic = 991;
    public static final int CUSTOM_ITEM_FontMono = 990;
    public static final int CUSTOM_ITEM_FontReset = 989;
    public static final int CUSTOM_ITEM_InputBarPreview = 988;

    public final int type;
    public boolean enabled;
    private final String key;

    public ConfigCellCustom(String key, int type, boolean enabled) {
        this.key = key;
        this.type = type;
        this.enabled = enabled;
    }

    public int getType() {
        return type;
    }

    public String getKey() {
        return this.key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void onBindViewHolder(RecyclerView.ViewHolder holder) {
        // Not Used
    }
}
