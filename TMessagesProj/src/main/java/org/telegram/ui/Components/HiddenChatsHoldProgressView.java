package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class HiddenChatsHoldProgressView extends View {

    private final RectF oval = new RectF();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress;

    public HiddenChatsHoldProgressView(Context context) {
        super(context);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(AndroidUtilities.dp(3));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Theme.getColor(Theme.key_chats_actionBackground));
    }

    public void setProgress(float value) {
        progress = value;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (progress <= 0) {
            return;
        }
        oval.set(getWidth() / 2f - paint.getStrokeWidth() * 3, getHeight() / 2f - paint.getStrokeWidth() * 3,
                getWidth() / 2f + paint.getStrokeWidth() * 3, getHeight() / 2f + paint.getStrokeWidth() * 3);
        canvas.drawArc(oval, -90, 360 * Math.min(1f, progress), false, paint);
    }
}
