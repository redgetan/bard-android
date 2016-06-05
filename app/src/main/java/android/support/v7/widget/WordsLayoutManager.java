package android.support.v7.widget;

import android.content.Context;
import android.graphics.Rect;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewDebug;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

public class WordsLayoutManager extends LinearLayoutManager {

    private ConfigDefinition config;

    public WordsLayoutManager(Context context) {
        super(context);
        this.config = new ConfigDefinition();
    }

    boolean mShouldReverseLayout = false;

    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return super.checkLayoutParams(lp);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return null;
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        this.config.setMaxWidth(this.getWidth() - this.getPaddingRight() - this.getPaddingLeft());
        this.config.setMaxHeight(this.getHeight() - this.getPaddingTop() - this.getPaddingBottom());
        this.config.setWidthMode(MeasureSpec.AT_MOST);
        this.config.setHeightMode(MeasureSpec.AT_MOST);
        this.config.setCheckCanFit(true);

        super.onLayoutChildren(recycler, state);
    }

    @Override
    void layoutChunk(Recycler recycler, State state, LayoutState layoutState, LinearLayoutManager.LayoutChunkResult result) {
//        super.layoutChunk(recycler, state, layoutState, result);

        boolean lineCompleted = false;
        int count = 0;
        LineDefinition currentLine = new LineDefinition(config);
        boolean scrollToStart = layoutState.mLayoutDirection == LayoutState.LAYOUT_START;

        while (!lineCompleted && layoutState.hasMore(state)) {
            View childView = layoutState.next(recycler);
            if (childView == null) {
                break;
            }
            count++;

            measureChildWithMargins(childView, 0, 0);

            final LayoutParams lp = (LayoutParams) childView.getLayoutParams();

            ViewDefinition view = new ViewDefinition(this.config, childView);
            view.setWidth(childView.getMeasuredWidth());
            view.setHeight(childView.getMeasuredHeight());
            view.setNewLine(lp.isNewLine());
            view.setGravity(lp.getGravity());
            view.setWeight(lp.getWeight());
            view.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin);

            if (currentLine.canFit(view)) {
                if (mShouldReverseLayout == scrollToStart) {
                    addView(childView);
                    currentLine.addView(view);
                } else {
                    addView(childView, 0);
                    currentLine.addView(view, 0);
                }

            } else {
                if (mShouldReverseLayout == scrollToStart) {
                    layoutState.mCurrentPosition--;
                } else {
                    layoutState.mCurrentPosition++;
                }

                lineCompleted = true;
            }
        }

        if (count == 0) {
            result.mFinished = true;
            return;
        }

        result.mConsumed = mOrientationHelper.getDecoratedMeasurement(currentLine.getViews().get(0).getView());

        int left, top, right, bottom;
        if (scrollToStart) {
            bottom = layoutState.mOffset;
            top = layoutState.mOffset - result.mConsumed;
        } else {
            top = layoutState.mOffset;
            bottom = layoutState.mOffset + result.mConsumed;
        }
        left = getPaddingLeft();
        right = left + currentLine.getLineStartLength();

        currentLine.setLineStartThickness(layoutState.mOffset);
        int prevChildThickness = 0;
        final List<ViewDefinition> childViews = currentLine.getViews();
        final int childCount = childViews.size();
        for (int j = 0; j < childCount; j++) {
            ViewDefinition child = childViews.get(j);
            child.setInlineStartLength(prevChildThickness);
            prevChildThickness += child.getLength() + child.getSpacingLength();

            final View view = child.getView();
            layoutDecorated(view, left + child.getInlineStartLength(), top + child.getTopMargin(),
                    right + child.getInlineStartLength() + child.getWidth(), bottom - child.getBottomMargin());
        }
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, Recycler recycler, State state) {
        return super.scrollVerticallyBy(dy, recycler, state);
    }

    public static class LayoutParams extends RecyclerView.LayoutParams {
        @ViewDebug.ExportedProperty(mapping = {@ViewDebug.IntToString(from = Gravity.NO_GRAVITY, to = "NONE"), @ViewDebug.IntToString(from = Gravity.TOP, to = "TOP"), @ViewDebug.IntToString(from = Gravity.BOTTOM, to = "BOTTOM"), @ViewDebug.IntToString(from = Gravity.LEFT, to = "LEFT"), @ViewDebug.IntToString(from = Gravity.RIGHT, to = "RIGHT"), @ViewDebug.IntToString(from = Gravity.CENTER_VERTICAL, to = "CENTER_VERTICAL"), @ViewDebug.IntToString(from = Gravity.FILL_VERTICAL, to = "FILL_VERTICAL"), @ViewDebug.IntToString(from = Gravity.CENTER_HORIZONTAL, to = "CENTER_HORIZONTAL"), @ViewDebug.IntToString(from = Gravity.FILL_HORIZONTAL, to = "FILL_HORIZONTAL"), @ViewDebug.IntToString(from = Gravity.CENTER, to = "CENTER"), @ViewDebug.IntToString(from = Gravity.FILL, to = "FILL")})

        private boolean newLine = false;
        private int     gravity = Gravity.NO_GRAVITY;
        private float   weight  = -1.0f;

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
        }

        public int getGravity() {
            return gravity;
        }

        public void setGravity(int gravity) {
            this.gravity = gravity;
        }

        public float getWeight() {
            return weight;
        }

        public void setWeight(float weight) {
            this.weight = weight;
        }

        public boolean isNewLine() {
            return newLine;
        }

        public void setNewLine(boolean newLine) {
            this.newLine = newLine;
        }
    }

    public static class CommonLogic {
        public static final int HORIZONTAL = 0;
        public static final int VERTICAL   = 1;
    }

    public class ViewDefinition {
        private final ConfigDefinition config;
        private final View             view;
        private       int              inlineStartLength;
        private       float            weight;
        private       int              gravity;
        private       boolean          newLine;
        private       int              inlineStartThickness;
        private       int              width;
        private       int              height;
        private       int              leftMargin;
        private       int              topMargin;
        private       int              rightMargin;
        private       int              bottomMargin;

        public ViewDefinition(ConfigDefinition config, View child) {
            this.config = config;
            this.view = child;
        }

        public int getLength() {
            return this.config.getOrientation() == CommonLogic.HORIZONTAL ? width : height;
        }

        public void setLength(int length) {
            if (this.config.getOrientation() == CommonLogic.HORIZONTAL) {
                width = length;
            } else {
                height = length;
            }
        }

        public int getSpacingLength() {
            return this.config.getOrientation() == CommonLogic.HORIZONTAL ? this.leftMargin + this.rightMargin : this.topMargin + this.bottomMargin;
        }

        public int getThickness() {
            return this.config.getOrientation() == CommonLogic.HORIZONTAL ? height : width;
        }

        public void setThickness(int thickness) {
            if (this.config.getOrientation() == CommonLogic.HORIZONTAL) {
                height = thickness;
            } else {
                width = thickness;
            }
        }

        public int getSpacingThickness() {
            return this.config.getOrientation() == CommonLogic.HORIZONTAL ? this.topMargin + this.bottomMargin : this.leftMargin + this.rightMargin;
        }

        public float getWeight() {
            return weight;
        }

        public void setWeight(float weight) {
            this.weight = weight;
        }

        public boolean weightSpecified() {
            return this.weight >= 0;
        }

        public int getInlineStartLength() {
            return inlineStartLength;
        }

        public void setInlineStartLength(int inlineStartLength) {
            this.inlineStartLength = inlineStartLength;
        }

        public boolean gravitySpecified() {
            return gravity != Gravity.NO_GRAVITY;
        }

        public int getGravity() {
            return gravity;
        }

        public void setGravity(int gravity) {
            this.gravity = gravity;
        }

        public boolean isNewLine() {
            return newLine;
        }

        public void setNewLine(boolean newLine) {
            this.newLine = newLine;
        }

        public View getView() {
            return view;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getTopMargin() {
            return topMargin;
        }

        public int getBottomMargin() {
            return bottomMargin;
        }

        public int getInlineStartThickness() {
            return inlineStartThickness;
        }

        public void setInlineStartThickness(int inlineStartThickness) {
            this.inlineStartThickness = inlineStartThickness;
        }

        public void setMargins(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
            this.leftMargin = leftMargin;
            this.topMargin = topMargin;
            this.rightMargin = rightMargin;
            this.bottomMargin = bottomMargin;
        }

        public int getInlineX() {
            return this.config.getOrientation() == CommonLogic.HORIZONTAL ? this.inlineStartLength : this.inlineStartThickness;
        }

        public int getInlineY() {
            return this.config.getOrientation() == CommonLogic.HORIZONTAL ? this.inlineStartThickness : this.inlineStartLength;
        }
    }

    public class ConfigDefinition {
        private int   orientation     = CommonLogic.HORIZONTAL;
        private float weightDefault   = 0;
        private int   gravity         = Gravity.LEFT | Gravity.TOP;
        private int   layoutDirection = View.LAYOUT_DIRECTION_LTR;
        private int     maxWidth;
        private int     maxHeight;
        private boolean checkCanFit;
        private int     widthMode;
        private int     heightMode;

        public ConfigDefinition() {
            this.setOrientation(CommonLogic.HORIZONTAL);
            this.setWeightDefault(1.0f);
            this.setGravity(Gravity.LEFT | Gravity.TOP);
            this.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            this.setCheckCanFit(false);
        }

        public int getOrientation() {
            return this.orientation;
        }

        public void setOrientation(int orientation) {
            if (orientation == CommonLogic.VERTICAL) {
                this.orientation = orientation;
            } else {
                this.orientation = CommonLogic.HORIZONTAL;
            }
        }

        public float getWeightDefault() {
            return this.weightDefault;
        }

        public void setWeightDefault(float weightDefault) {
            this.weightDefault = Math.max(0, weightDefault);
        }

        public int getGravity() {
            return this.gravity;
        }

        public void setGravity(int gravity) {
            this.gravity = gravity;
        }

        public int getLayoutDirection() {
            return layoutDirection;
        }

        public void setLayoutDirection(int layoutDirection) {
            if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                this.layoutDirection = layoutDirection;
            } else {
                this.layoutDirection = View.LAYOUT_DIRECTION_LTR;
            }
        }

        public void setMaxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
        }

        public void setMaxHeight(int maxHeight) {
            this.maxHeight = maxHeight;
        }

        public int getMaxLength() {
            return this.orientation == CommonLogic.HORIZONTAL ? this.maxWidth : this.maxHeight;
        }

        public int getMaxThickness() {
            return this.orientation == CommonLogic.HORIZONTAL ? this.maxHeight : this.maxWidth;
        }

        public void setCheckCanFit(boolean checkCanFit) {
            this.checkCanFit = checkCanFit;
        }

        public boolean isCheckCanFit() {
            return checkCanFit;
        }

        public void setWidthMode(int widthMode) {
            this.widthMode = widthMode;
        }

        public void setHeightMode(int heightMode) {
            this.heightMode = heightMode;
        }

        public int getLengthMode() {
            return this.orientation == CommonLogic.HORIZONTAL ? this.widthMode : this.heightMode;
        }

        public int getThicknessMode() {
            return this.orientation == CommonLogic.HORIZONTAL ? this.heightMode : this.widthMode;
        }
    }

    public static class LineDefinition {
        private final List<ViewDefinition> views = new ArrayList<ViewDefinition>();
        private final ConfigDefinition config;
        private       int              lineLength;
        private       int              lineThickness;
        private       int              lineStartThickness;
        private       int              lineStartLength;

        public LineDefinition(ConfigDefinition config) {
            this.config = config;
            this.lineStartThickness = 0;
            this.lineStartLength = 0;
        }

        public void addView(ViewDefinition child) {
            this.addView(child, this.views.size());
        }

        public void addView(ViewDefinition child, int i) {
            this.views.add(i, child);

            this.lineLength = this.lineLength + child.getLength() + child.getSpacingLength();
            this.lineThickness = Math.max(this.lineThickness, child.getThickness() + child.getSpacingThickness());
        }

        public boolean canFit(ViewDefinition child) {
            return lineLength + child.getLength() + child.getSpacingLength() <= config.getMaxLength();
        }

        public int getLineStartThickness() {
            return lineStartThickness;
        }

        public void setLineStartThickness(int lineStartThickness) {
            this.lineStartThickness = lineStartThickness;
        }

        public int getLineThickness() {
            return lineThickness;
        }

        public int getLineLength() {
            return lineLength;
        }

        public int getLineStartLength() {
            return lineStartLength;
        }

        public void setLineStartLength(int lineStartLength) {
            this.lineStartLength = lineStartLength;
        }

        public List<ViewDefinition> getViews() {
            return views;
        }

        public void setThickness(int thickness) {
            this.lineThickness = thickness;
        }

        public void setLength(int length) {
            this.lineLength = length;
        }

        public int getX() {
            return this.config.getOrientation() == CommonLogic.HORIZONTAL ? this.lineStartLength : this.lineStartThickness;
        }

        public int getY() {
            return this.config.getOrientation() == CommonLogic.HORIZONTAL ? this.lineStartThickness : this.lineStartLength;
        }
    }
}
