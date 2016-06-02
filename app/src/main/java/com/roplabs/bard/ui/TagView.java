package com.roplabs.bard.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.v4.widget.ViewDragHelper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TagView extends View {

    /** Border width*/
    private float mBorderWidth;

    /** Border radius*/
    private float mBorderRadius;

    /** Text size*/
    private float mTextSize;

    /** Horizontal padding for this view, include left & right padding(left & right padding are equal*/
    private int mHorizontalPadding;

    /** Vertical padding for this view, include top & bottom padding(top & bottom padding are equal)*/
    private int mVerticalPadding;

    /** TagView border color*/
    private int mBorderColor;

    /** TagView background color*/
    private int mBackgroundColor;

    /** TagView text color*/
    private int mTextColor;

    /** Whether this view clickable*/
    private boolean isViewClickable;

    /** The max length for this tag view*/
    private int mTagMaxLength;

    /** OnTagClickListener for click action*/
    private OnTagClickListener mOnTagClickListener;

    /** Move slop(default 20px)*/
    private int mMoveSlop = 20;

    /** Scroll slop threshold*/
    private int mSlopThreshold = 4;

    /** How long trigger long click callback(default 500ms)*/
    private int mLongPressTime = 500;

    /** Text direction(support:TEXT_DIRECTION_RTL & TEXT_DIRECTION_LTR, default TEXT_DIRECTION_LTR)*/
    private int mTextDirection = View.TEXT_DIRECTION_LTR;

    /** The distance between baseline and descent*/
    private float bdDistance;

    private Paint mPaint;

    private RectF mRectF;

    private String mAbstractText, mOriginText;

    private boolean isUp, isMoved, isExecLongClick;

    private float fontH, fontW;

    private Typeface mTypeface;

    public TagView(Context context) {
        super(context);
    }

    public TagView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TagView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init(String text){
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRectF = new RectF();
        mOriginText = text == null ? "" : text;
    }

    private void onDealText(){
        if(!TextUtils.isEmpty(mOriginText)) {
            mAbstractText = mOriginText.length() <= mTagMaxLength ? mOriginText
                    : mOriginText.substring(0, mTagMaxLength - 3) + "...";
        }else {
            mAbstractText = "";
        }
        mPaint.setTypeface(mTypeface);
        mPaint.setTextSize(mTextSize);
        final Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        fontH = fontMetrics.descent - fontMetrics.ascent;
        if (mTextDirection == View.TEXT_DIRECTION_RTL){
            fontW = 0;
            for (char c : mAbstractText.toCharArray()) {
                String sc = String.valueOf(c);
                fontW += mPaint.measureText(sc);
            }
        }else {
            fontW = mPaint.measureText(mAbstractText);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mHorizontalPadding * 2 + (int) fontW,
                mVerticalPadding * 2 + (int) fontH);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mRectF.set(mBorderWidth, mBorderWidth, w - mBorderWidth, h - mBorderWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mBackgroundColor);
        canvas.drawRoundRect(mRectF, mBorderRadius, mBorderRadius, mPaint);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mBorderWidth);
        mPaint.setColor(mBorderColor);
        canvas.drawRoundRect(mRectF, mBorderRadius, mBorderRadius, mPaint);

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mTextColor);

        if (mTextDirection == View.TEXT_DIRECTION_RTL){
            float tmpX = getWidth() / 2 + fontW / 2;
            for (char c : mAbstractText.toCharArray()) {
                String sc = String.valueOf(c);
                tmpX -= mPaint.measureText(sc);
                canvas.drawText(sc, tmpX, getHeight() / 2 + fontH / 2 - bdDistance, mPaint);
            }
        }else {
            canvas.drawText(mAbstractText, getWidth() / 2 - fontW / 2,
                    getHeight() / 2 + fontH / 2 - bdDistance, mPaint);
        }
    }

    public String getText(){
        return mOriginText;
    }

    public void setText(String text){
        mOriginText = text;
        onDealText();
    }

    public boolean getIsViewClickable(){
        return isViewClickable;
    }

    public void setTagMaxLength(int maxLength){
        this.mTagMaxLength = maxLength;
        onDealText();
    }

    public void setOnTagClickListener(OnTagClickListener listener){
        this.mOnTagClickListener = listener;
    }

    public void setTagBackgroundColor(int color){
        this.mBackgroundColor = color;
    }

    public void setTagBorderColor(int color){
        this.mBorderColor = color;
    }

    public void setTagTextColor(int color){
        this.mTextColor = color;
    }

    public void setBorderWidth(float width) {
        this.mBorderWidth = width;
    }

    public void setBorderRadius(float radius) {
        this.mBorderRadius = radius;
    }

    public void setTextSize(float size) {
        this.mTextSize = size;
        onDealText();
    }

    public void setHorizontalPadding(int padding) {
        this.mHorizontalPadding = padding;
    }

    public void setVerticalPadding(int padding) {
        this.mVerticalPadding = padding;
    }

    public void setIsViewClickable(boolean clickable) {
        this.isViewClickable = clickable;
    }

    public interface OnTagClickListener{
        void onTagClick(int position, String text);
        void onTagLongClick(int position, String text);
    }

    public int getTextDirection() {
        return mTextDirection;
    }

    public void setTextDirection(int textDirection) {
        this.mTextDirection = textDirection;
    }

    public void setTypeface(Typeface typeface) {
        this.mTypeface = typeface;
        onDealText();
    }

    public void setBdDistance(float bdDistance) {
        this.bdDistance = bdDistance;
    }
}
