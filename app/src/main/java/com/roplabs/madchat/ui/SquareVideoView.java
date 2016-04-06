package com.roplabs.madchat.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

public class SquareVideoView extends VideoView {
    public SquareVideoView(Context context) {
        super(context);
    }

    public SquareVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // before it's 16:9, now we want to make it 4:3
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        int width = widthSpecSize;
        int height = width * 3 / 4;

        if (height > heightSpecSize) {
            // couldn't match aspect ratio within the constraints
            height = heightSpecSize;
        }

        setMeasuredDimension(width, height);
    }
}
