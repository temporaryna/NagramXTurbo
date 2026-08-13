package org.telegram.ui.Components.blur3;

import android.graphics.Canvas;
import android.graphics.RectF;

import org.telegram.ui.Components.blur3.capture.IBlur3Capture;
import org.telegram.ui.Components.blur3.capture.IBlur3Hash;

public class CompositeViewGroupPartRenderer implements IBlur3Capture {

    private final ViewGroupPartRenderer[] renderers;

    public CompositeViewGroupPartRenderer(ViewGroupPartRenderer... renderers) {
        this.renderers = renderers;
    }

    @Override
    public void capture(Canvas canvas, RectF position) {
        for (int i = 0; i < renderers.length; i++) {
            renderers[i].capture(canvas, position);
        }
    }

    @Override
    public void captureCalculateHash(IBlur3Hash builder, RectF position) {
        for (int i = 0; i < renderers.length; i++) {
            renderers[i].captureCalculateHash(builder, position);
        }
    }
}
