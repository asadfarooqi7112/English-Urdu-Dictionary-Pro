package com.hfad.personaldictionary.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class GraphicOverlay extends View {

    private List<Rect> boundingBoxes = new ArrayList<>();
    private Paint boxPaint;

    // Camera image dimensions
    private int imageWidth = 0;
    private int imageHeight = 0;

    // Set this to true if using front camera
    private boolean isImageFlipped = false;

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        boxPaint = new Paint();
        boxPaint.setColor(0xFF00FF00); // Bright green
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8.0f);
    }

    public void setBoundingBoxes(List<Rect> boxes) {
        boundingBoxes = boxes;
        invalidate(); // Triggers onDraw()
    }

    public void setImageSourceInfo(int width, int height) {
        imageWidth = width;
        imageHeight = height;
    }

    public void setImageFlipped(boolean flipped) {
        isImageFlipped = flipped;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (imageWidth == 0 || imageHeight == 0) {
            return;
        }

        float scaleX = getWidth() * 1.0f / imageWidth;
        float scaleY = getHeight() * 1.0f / imageHeight;

        for (Rect rect : boundingBoxes) {
            float left = rect.left * scaleX;
            float top = rect.top * scaleY;
            float right = rect.right * scaleX;
            float bottom = rect.bottom * scaleY;

            if (isImageFlipped) {
                float flippedLeft = getWidth() - right;
                float flippedRight = getWidth() - left;
                left = flippedLeft;
                right = flippedRight;
            }

            canvas.drawRect(left, top, right, bottom, boxPaint);
        }
    }
}
