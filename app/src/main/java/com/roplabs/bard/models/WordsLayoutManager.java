package com.roplabs.bard.models;

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
    List<LineDefinition> lines = new ArrayList<LineDefinition>();
    List<ViewDefinition> views = new ArrayList<ViewDefinition>();

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
        super.onLayoutChildren(recycler, state);

        detachAndScrapAttachedViews(recycler);

        final int count = this.getItemCount();
        views.clear();
        lines.clear();
        for (int i = 0; i < count; i++) {
            View child = recycler.getViewForPosition(i);
            addView(child);
            measureChildWithMargins(child, 0, 0);

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            ViewDefinition view = new ViewDefinition(this.config, child);
            view.setWidth(child.getMeasuredWidth());
            view.setHeight(child.getMeasuredHeight());
            view.setNewLine(lp.isNewLine());
            view.setGravity(lp.getGravity());
            view.setWeight(lp.getWeight());
            view.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin);
            views.add(view);
        }

        this.config.setMaxWidth(this.getWidth() - this.getPaddingRight() - this.getPaddingLeft());
        this.config.setMaxHeight(this.getHeight() - this.getPaddingTop() - this.getPaddingBottom());
        this.config.setWidthMode(MeasureSpec.AT_MOST);
        this.config.setHeightMode(MeasureSpec.AT_MOST);
        this.config.setCheckCanFit(true);

        CommonLogic.fillLines(views, lines, config);
        CommonLogic.calculateLinesAndChildPosition(lines);

        int contentLength = 0;
        final int linesCount = lines.size();
        for (int i = 0; i < linesCount; i++) {
            LineDefinition l = lines.get(i);
            contentLength = Math.max(contentLength, l.getLineLength());
        }

        LineDefinition currentLine = lines.get(lines.size() - 1);
        int contentThickness = currentLine.getLineStartThickness() + currentLine.getLineThickness();
        int realControlLength = CommonLogic.findSize(this.config.getLengthMode(), this.config.getMaxLength(), contentLength);
        int realControlThickness = CommonLogic.findSize(this.config.getThicknessMode(), this.config.getMaxThickness(), contentThickness);

        CommonLogic.applyGravityToLines(lines, realControlLength, realControlThickness, config);

        for (int i = 0; i < linesCount; i++) {
            LineDefinition line = lines.get(i);
            applyPositionsToViews(line);
        }
    }

    private void applyPositionsToViews(LineDefinition line) {
        final List<ViewDefinition> childViews = line.getViews();
        final int childCount = childViews.size();
        for (int i = 0; i < childCount; i++) {
            final ViewDefinition child = childViews.get(i);
            final View view = child.getView();
//            measureChildWithMargins(view, child.getWidth(), child.getHeight());

            layoutDecorated(view, this.getPaddingLeft() + line.getLineStartLength() + child.getInlineStartLength(), this.getPaddingTop() + line.getLineStartThickness() + child.getInlineStartThickness(), this.getPaddingLeft() + line.getLineStartLength() + child.getInlineStartLength() + child.getWidth(), this.getPaddingTop() + line.getLineStartThickness() + child.getInlineStartThickness() + child.getHeight());
        }
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    static class LayoutState {

        final static String TAG = "LinearLayoutManager#LayoutState";

        final static int LAYOUT_START = -1;

        final static int LAYOUT_END = 1;

        final static int INVALID_LAYOUT = Integer.MIN_VALUE;

        final static int ITEM_DIRECTION_HEAD = -1;

        final static int ITEM_DIRECTION_TAIL = 1;

        final static int SCOLLING_OFFSET_NaN = Integer.MIN_VALUE;

        /**
         * We may not want to recycle children in some cases (e.g. layout)
         */
        boolean mRecycle = true;

        /**
         * Pixel offset where layout should start
         */
        int mOffset;

        /**
         * Number of pixels that we should fill, in the layout direction.
         */
        int mAvailable;

        /**
         * Current position on the adapter to get the next item.
         */
        int mCurrentPosition;

        /**
         * Defines the direction in which the data adapter is traversed.
         * Should be {@link #ITEM_DIRECTION_HEAD} or {@link #ITEM_DIRECTION_TAIL}
         */
        int mItemDirection;

        /**
         * Defines the direction in which the layout is filled.
         * Should be {@link #LAYOUT_START} or {@link #LAYOUT_END}
         */
        int mLayoutDirection;

        /**
         * Used when LayoutState is constructed in a scrolling state.
         * It should be set the amount of scrolling we can make without creating a new view.
         * Settings this is required for efficient view recycling.
         */
        int mScrollingOffset;

        /**
         * Used if you want to pre-layout items that are not yet visible.
         * The difference with {@link #mAvailable} is that, when recycling, distance laid out for
         * {@link #mExtra} is not considered to avoid recycling visible children.
         */
        int mExtra = 0;

        /**
         * Equal to {@link RecyclerView.State#isPreLayout()}. When consuming scrap, if this value
         * is set to true, we skip removed views since they should not be laid out in post layout
         * step.
         */
        boolean mIsPreLayout = false;

        /**
         * The most recent {@link #scrollBy(int, RecyclerView.Recycler, RecyclerView.State)} amount.
         */
        int mLastScrollDelta;

        /**
         * When LLM needs to layout particular views, it sets this list in which case, LayoutState
         * will only return views from this list and return null if it cannot find an item.
         */
        List<ViewHolder> mScrapList = null;

        /**
         * @return true if there are more items in the data adapter
         */
        boolean hasMore(RecyclerView.State state) {
            return mCurrentPosition >= 0 && mCurrentPosition < state.getItemCount();
        }

        /**
         * Gets the view for the next element that we should layout.
         * Also updates current item index to the next item, based on {@link #mItemDirection}
         *
         * @return The next element that we should layout.
         */
        View next(RecyclerView.Recycler recycler) {
            if (mScrapList != null) {
                return nextViewFromScrapList();
            }
            final View view = recycler.getViewForPosition(mCurrentPosition);
            mCurrentPosition += mItemDirection;
            return view;
        }

        /**
         * Returns the next item from the scrap list.
         * <p>
         * Upon finding a valid VH, sets current item position to VH.itemPosition + mItemDirection
         *
         * @return View if an item in the current position or direction exists if not null.
         */
        private View nextViewFromScrapList() {
            final int size = mScrapList.size();
            for (int i = 0; i < size; i++) {
                final View view = mScrapList.get(i).itemView;
                final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
                if (lp.isItemRemoved()) {
                    continue;
                }
                if (mCurrentPosition == lp.getViewLayoutPosition()) {
                    assignPositionFromScrapList(view);
                    return view;
                }
            }
            return null;
        }

        public void assignPositionFromScrapList() {
            assignPositionFromScrapList(null);
        }

        public void assignPositionFromScrapList(View ignore) {
            final View closest = nextViewInLimitedList(ignore);
            if (closest == null) {
                mCurrentPosition = NO_POSITION;
            } else {
                mCurrentPosition = ((RecyclerView.LayoutParams) closest.getLayoutParams()).getViewLayoutPosition();
            }
        }

        public View nextViewInLimitedList(View ignore) {
            int size = mScrapList.size();
            View closest = null;
            int closestDistance = Integer.MAX_VALUE;
            if (mIsPreLayout) {
                throw new IllegalStateException("Scrap list cannot be used in pre layout");
            }
            for (int i = 0; i < size; i++) {
                View view = mScrapList.get(i).itemView;
                final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
                if (view == ignore || lp.isItemRemoved()) {
                    continue;
                }
                final int distance = (lp.getViewLayoutPosition() - mCurrentPosition) * mItemDirection;
                if (distance < 0) {
                    continue; // item is not in current direction
                }
                if (distance < closestDistance) {
                    closest = view;
                    closestDistance = distance;
                    if (distance == 0) {
                        break;
                    }
                }
            }
            return closest;
        }

    }

    OrientationHelper mOrientationHelper;
    LayoutState       mLayoutStateLoc;
    int mOrientation = VERTICAL;

    void ensureLayoutStateLoc() {
        if (mLayoutStateLoc == null) {
            mLayoutStateLoc = createLayoutState();
        }
        if (mOrientationHelper == null) {
            mOrientationHelper = OrientationHelper.createOrientationHelper(this, mOrientation);
        }
    }

    /**
     * Test overrides this to plug some tracking and verification.
     *
     * @return A new LayoutState
     */
    LayoutState createLayoutState() {
        return new LayoutState();
    }

    /**
     * Recycles children between given indices.
     *
     * @param startIndex inclusive
     * @param endIndex   exclusive
     */
    private void recycleChildren(RecyclerView.Recycler recycler, int startIndex, int endIndex) {
        if (startIndex == endIndex) {
            return;
        }
        if (endIndex > startIndex) {
            for (int i = endIndex - 1; i >= startIndex; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
        } else {
            for (int i = startIndex; i > endIndex; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
        }
    }

    private void recycleViewsFromStart(RecyclerView.Recycler recycler, int dt) {
        if (dt < 0) {
            return;
        }
        // ignore padding, ViewGroup may not clip children.
        final int limit = dt;
        final int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (mOrientationHelper.getDecoratedEnd(child) > limit) {// stop here
                recycleChildren(recycler, 0, i);
                return;
            }
        }
    }

    /**
     * Recycles views that went out of bounds after scrolling towards the start of the layout.
     *
     * @param recycler Recycler instance of {@link android.support.v7.widget.RecyclerView}
     * @param dt       This can be used to add additional padding to the visible area. This is used
     *                 to detect children that will go out of bounds after scrolling, without
     *                 actually moving them.
     */
    private void recycleViewsFromEnd(RecyclerView.Recycler recycler, int dt) {
        final int childCount = getChildCount();
        if (dt < 0) {
            return;
        }
        final int limit = mOrientationHelper.getEnd() - dt;

        for (int i = childCount - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (mOrientationHelper.getDecoratedStart(child) < limit) {// stop here
                recycleChildren(recycler, childCount - 1, i);
                return;
            }
        }

    }

    /**
     * Helper method to call appropriate recycle method depending on current layout direction
     *
     * @param recycler    Current recycler that is attached to RecyclerView
     * @param layoutState Current layout state. Right now, this object does not change but
     *                    we may consider moving it out of this view so passing around as a
     *                    parameter for now, rather than accessing {@link #mLayoutStateLoc}
     * @see #recycleViewsFromStart(android.support.v7.widget.RecyclerView.Recycler, int)
     * @see #recycleViewsFromEnd(android.support.v7.widget.RecyclerView.Recycler, int)
     * @see android.support.v7.widget.LinearLayoutManager.LayoutState#mLayoutDirection
     */
    private void recycleByLayoutState(RecyclerView.Recycler recycler, LayoutState layoutState) {
        if (!layoutState.mRecycle) {
            return;
        }
        if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            recycleViewsFromEnd(recycler, layoutState.mScrollingOffset);
        } else {
            recycleViewsFromStart(recycler, layoutState.mScrollingOffset);
        }
    }

    protected static class LayoutChunkResult {
        public int mConsumed;
        public boolean mFinished;
        public boolean mIgnoreConsumed;
        public boolean mFocusable;

        void resetInternal() {
            mConsumed = 0;
            mFinished = false;
            mIgnoreConsumed = false;
            mFocusable = false;
        }
    }

    void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state,
                     LayoutState layoutState, LayoutChunkResult result) {
        View view = layoutState.next(recycler);
        if (view == null) {

            // if we are laying out views in scrap, this may return null which means there is
            // no more items to layout.
            result.mFinished = true;
            return;
        }
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (layoutState.mScrapList == null) {
            if (mShouldReverseLayout == (layoutState.mLayoutDirection
                    == LayoutState.LAYOUT_START)) {
                addView(view);
            } else {
                addView(view, 0);
            }
        } else {
            if (mShouldReverseLayout == (layoutState.mLayoutDirection
                    == LayoutState.LAYOUT_START)) {
                addDisappearingView(view);
            } else {
                addDisappearingView(view, 0);
            }
        }
        measureChildWithMargins(view, 0, 0);
        result.mConsumed = mOrientationHelper.getDecoratedMeasurement(view);
        int left, top, right, bottom;
        if (mOrientation == VERTICAL) {
            if (isLayoutRTL()) {
                right = getWidth() - getPaddingRight();
                left = right - mOrientationHelper.getDecoratedMeasurementInOther(view);
            } else {
                left = getPaddingLeft();
                right = left + mOrientationHelper.getDecoratedMeasurementInOther(view);
            }
            if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                bottom = layoutState.mOffset;
                top = layoutState.mOffset - result.mConsumed;
            } else {
                top = layoutState.mOffset;
                bottom = layoutState.mOffset + result.mConsumed;
            }
        } else {
            top = getPaddingTop();
            bottom = top + mOrientationHelper.getDecoratedMeasurementInOther(view);

            if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                right = layoutState.mOffset;
                left = layoutState.mOffset - result.mConsumed;
            } else {
                left = layoutState.mOffset;
                right = layoutState.mOffset + result.mConsumed;
            }
        }
        // We calculate everything with View's bounding box (which includes decor and margins)
        // To calculate correct layout position, we subtract margins.
        layoutDecorated(view, left + params.leftMargin, top + params.topMargin,
                right - params.rightMargin, bottom - params.bottomMargin);

        // Consume the available space if the view is not removed OR changed
        if (params.isItemRemoved() || params.isItemChanged()) {
            result.mIgnoreConsumed = true;
        }
        result.mFocusable = view.isFocusable();
    }

    int fill(RecyclerView.Recycler recycler, LayoutState layoutState, RecyclerView.State state, boolean stopOnFocusable) {
        // max offset we should set is mFastScroll + available
        final int start = layoutState.mAvailable;
        if (layoutState.mScrollingOffset != LayoutState.SCOLLING_OFFSET_NaN) {
            // TODO ugly bug fix. should not happen
            if (layoutState.mAvailable < 0) {
                layoutState.mScrollingOffset += layoutState.mAvailable;
            }
//            recycleByLayoutState(recycler, layoutState);
        }
        int remainingSpace = layoutState.mAvailable + layoutState.mExtra;
        LayoutChunkResult layoutChunkResult = new LayoutChunkResult();
        while (remainingSpace > 0 && layoutState.hasMore(state)) {
            layoutChunkResult.resetInternal();
            layoutChunk(recycler, state, layoutState, layoutChunkResult);
            if (layoutChunkResult.mFinished) {
                break;
            }
            layoutState.mOffset += layoutChunkResult.mConsumed * layoutState.mLayoutDirection;
            /**
             * Consume the available space if:
             * * layoutChunk did not request to be ignored
             * * OR we are laying out scrap children
             * * OR we are not doing pre-layout
             */
            if (!layoutChunkResult.mIgnoreConsumed || mLayoutStateLoc.mScrapList != null || !state.isPreLayout()) {
                layoutState.mAvailable -= layoutChunkResult.mConsumed;
                // we keep a separate remaining space because mAvailable is important for recycling
                remainingSpace -= layoutChunkResult.mConsumed;
            }

            if (layoutState.mScrollingOffset != LayoutState.SCOLLING_OFFSET_NaN) {
                layoutState.mScrollingOffset += layoutChunkResult.mConsumed;
                if (layoutState.mAvailable < 0) {
                    layoutState.mScrollingOffset += layoutState.mAvailable;
                }
                recycleByLayoutState(recycler, layoutState);
            }
            if (stopOnFocusable && layoutChunkResult.mFocusable) {
                break;
            }
        }
        return start - layoutState.mAvailable;
    }

    private View getChildClosestToStart() {
        return getChildAt(mShouldReverseLayout ? getChildCount() - 1 : 0);
    }

    /**
     * Convenience method to find the child closes to end. Caller should check it has enough
     * children.
     *
     * @return The child closes to end of the layout from user's perspective.
     */
    private View getChildClosestToEnd() {
        return getChildAt(mShouldReverseLayout ? 0 : getChildCount() - 1);
    }

    private void updateLayoutState(int layoutDirection, int requiredSpace,
                                   boolean canUseExistingSpace, RecyclerView.State state) {
        mLayoutStateLoc.mExtra = getExtraLayoutSpace(state);
        mLayoutStateLoc.mLayoutDirection = layoutDirection;
        int fastScrollSpace;
        if (layoutDirection == LayoutState.LAYOUT_END) {
            mLayoutStateLoc.mExtra += mOrientationHelper.getEndPadding();
            // get the first child in the direction we are going
            final View child = getChildClosestToEnd();
            // the direction in which we are traversing children
            mLayoutStateLoc.mItemDirection =  LayoutState.ITEM_DIRECTION_TAIL;
            mLayoutStateLoc.mCurrentPosition = getPosition(child) + mLayoutStateLoc.mItemDirection;
            mLayoutStateLoc.mOffset = mOrientationHelper.getDecoratedEnd(child);
            // calculate how much we can scroll without adding new children (independent of layout)
            fastScrollSpace = mOrientationHelper.getDecoratedEnd(child)
                    - mOrientationHelper.getEndAfterPadding();

        } else {
            final View child = getChildClosestToStart();
            mLayoutStateLoc.mExtra += mOrientationHelper.getStartAfterPadding();
            mLayoutStateLoc.mItemDirection =  LayoutState.ITEM_DIRECTION_HEAD;
            mLayoutStateLoc.mCurrentPosition = getPosition(child) + mLayoutStateLoc.mItemDirection;
            mLayoutStateLoc.mOffset = mOrientationHelper.getDecoratedStart(child);
            fastScrollSpace = -mOrientationHelper.getDecoratedStart(child)
                    + mOrientationHelper.getStartAfterPadding();
        }
        mLayoutStateLoc.mAvailable = requiredSpace;
        if (canUseExistingSpace) {
            mLayoutStateLoc.mAvailable -= fastScrollSpace;
        }
        mLayoutStateLoc.mScrollingOffset = fastScrollSpace;
    }

    @Override
    public int scrollVerticallyBy(int dy, Recycler recycler, State state) {
        if (getChildCount() == 0 || dy == 0) {
            return 0;
        }
        ensureLayoutStateLoc();

        mLayoutStateLoc.mRecycle = true;

        final int layoutDirection = dy > 0 ? LayoutState.LAYOUT_END : LayoutState.LAYOUT_START;
        final int absDy = Math.abs(dy);
        updateLayoutState(layoutDirection, absDy, true, state);
        final int freeScroll = mLayoutStateLoc.mScrollingOffset;
        final int consumed = freeScroll + fill(recycler, mLayoutStateLoc, state, false);
        if (consumed < 0) {
            return 0;
        }

        final int scrolled = absDy > consumed ? layoutDirection * consumed : dy;
        mOrientationHelper.offsetChildren(-scrolled);
        mLayoutStateLoc.mLastScrollDelta = scrolled;
        return scrolled;
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
        public static final int VERTICAL = 1;

        public static void calculateLinesAndChildPosition(List<LineDefinition> lines) {
            int prevLinesThickness = 0;
            final int linesCount = lines.size();
            for (int i = 0; i < linesCount; i++) {
                final LineDefinition line = lines.get(i);
                line.setLineStartThickness(prevLinesThickness);
                prevLinesThickness += line.getLineThickness();
                int prevChildThickness = 0;
                final List<ViewDefinition> childViews = line.getViews();
                final int childCount = childViews.size();
                for (int j = 0; j < childCount; j++) {
                    ViewDefinition child = childViews.get(j);
                    child.setInlineStartLength(prevChildThickness);
                    prevChildThickness += child.getLength() + child.getSpacingLength();
                }
            }
        }

        public static void applyGravityToLines(List<LineDefinition> lines, int realControlLength, int realControlThickness, ConfigDefinition config) {
            final int linesCount = lines.size();
            if (linesCount <= 0) {
                return;
            }

            final int totalWeight = linesCount;
            LineDefinition lastLine = lines.get(linesCount - 1);
            int excessThickness = realControlThickness - (lastLine.getLineThickness() + lastLine.getLineStartThickness());

            if (excessThickness < 0) {
                excessThickness = 0;
            }

            int excessOffset = 0;
            for (int i = 0; i < linesCount; i++) {
                final LineDefinition child = lines.get(i);
                int weight = 1;
                int gravity = getGravity(null, config);
                int extraThickness = Math.round(excessThickness * weight / totalWeight);

                final int childLength = child.getLineLength();
                final int childThickness = child.getLineThickness();

                Rect container = new Rect();
                container.top = excessOffset;
                container.left = 0;
                container.right = realControlLength;
                container.bottom = childThickness + extraThickness + excessOffset;

                Rect result = new Rect();
                Gravity.apply(gravity, childLength, childThickness, container, result);

                excessOffset += extraThickness;
                child.setLineStartLength(child.getLineStartLength() + result.left);
                child.setLineStartThickness(child.getLineStartThickness() + result.top);
                child.setLength(result.width());
                child.setThickness(result.height());

                applyGravityToLine(child, config);
            }
        }

        public static void applyGravityToLine(LineDefinition line, ConfigDefinition config) {
            final List<ViewDefinition> views = line.getViews();
            final int viewCount = views.size();
            if (viewCount <= 0) {
                return;
            }

            float totalWeight = 0;
            for (int i = 0; i < viewCount; i++) {
                final ViewDefinition child = views.get(i);
                totalWeight += getWeight(child, config);
            }

            ViewDefinition lastChild = views.get(viewCount - 1);
            int excessLength = line.getLineLength() - (lastChild.getLength() + lastChild.getSpacingLength() + lastChild.getInlineStartLength());
            int excessOffset = 0;
            for (int i = 0; i < viewCount; i++) {
                final ViewDefinition child = views.get(i);
                float weight = getWeight(child, config);
                int gravity = getGravity(child, config);
                int extraLength;
                if (totalWeight == 0) {
                    extraLength = excessLength / viewCount;
                } else {
                    extraLength = Math.round(excessLength * weight / totalWeight);
                }

                final int childLength = child.getLength() + child.getSpacingLength();
                final int childThickness = child.getThickness() + child.getSpacingThickness();

                Rect container = new Rect();
                container.top = 0;
                container.left = excessOffset;
                container.right = childLength + extraLength + excessOffset;
                container.bottom = line.getLineThickness();

                Rect result = new Rect();
                Gravity.apply(gravity, childLength, childThickness, container, result);

                excessOffset += extraLength;
                child.setInlineStartLength(result.left + child.getInlineStartLength());
                child.setInlineStartThickness(result.top);
                child.setLength(result.width() - child.getSpacingLength());
                child.setThickness(result.height() - child.getSpacingThickness());
            }
        }

        public static int findSize(int modeSize, int controlMaxSize, int contentSize) {
            int realControlSize;
            switch (modeSize) {
                case View.MeasureSpec.UNSPECIFIED:
                    realControlSize = contentSize;
                    break;
                case View.MeasureSpec.AT_MOST:
                    realControlSize = Math.min(contentSize, controlMaxSize);
                    break;
                case View.MeasureSpec.EXACTLY:
                    realControlSize = controlMaxSize;
                    break;
                default:
                    realControlSize = contentSize;
                    break;
            }
            return realControlSize;
        }

        private static float getWeight(ViewDefinition child, ConfigDefinition config) {
            return child.weightSpecified() ? child.getWeight() : config.getWeightDefault();
        }


        private static int getGravity(ViewDefinition child, ConfigDefinition config) {
            int parentGravity = config.getGravity();

            int childGravity;
            // get childGravity of child view (if exists)
            if (child != null && child.gravitySpecified()) {
                childGravity = child.getGravity();
            } else {
                childGravity = parentGravity;
            }

            childGravity = getGravityFromRelative(childGravity, config);
            parentGravity = getGravityFromRelative(parentGravity, config);

            // add parent gravity to child gravity if child gravity is not specified
            if ((childGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == 0) {
                childGravity |= parentGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
            }
            if ((childGravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
                childGravity |= parentGravity & Gravity.VERTICAL_GRAVITY_MASK;
            }

            // if childGravity is still not specified - set default top - left gravity
            if ((childGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == 0) {
                childGravity |= Gravity.LEFT;
            }
            if ((childGravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
                childGravity |= Gravity.TOP;
            }

            return childGravity;
        }


        public static int getGravityFromRelative(int childGravity, ConfigDefinition config) {
            // swap directions for vertical non relative view
            // if it is relative, then START is TOP, and we do not need to switch it here.
            // it will be switched later on onMeasure stage when calculations will be with length and thickness
            if (config.getOrientation() == CommonLogic.VERTICAL && (childGravity & Gravity.RELATIVE_LAYOUT_DIRECTION) == 0) {
                int horizontalGravity = childGravity;
                childGravity = 0;
                childGravity |= (horizontalGravity & Gravity.HORIZONTAL_GRAVITY_MASK) >> Gravity.AXIS_X_SHIFT << Gravity.AXIS_Y_SHIFT;
                childGravity |= (horizontalGravity & Gravity.VERTICAL_GRAVITY_MASK) >> Gravity.AXIS_Y_SHIFT << Gravity.AXIS_X_SHIFT;
            }

            // for relative layout and RTL direction swap left and right gravity
            if (config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL && (childGravity & Gravity.RELATIVE_LAYOUT_DIRECTION) != 0) {
                int ltrGravity = childGravity;
                childGravity = 0;
                childGravity |= (ltrGravity & Gravity.LEFT) == Gravity.LEFT ? Gravity.RIGHT : 0;
                childGravity |= (ltrGravity & Gravity.RIGHT) == Gravity.RIGHT ? Gravity.LEFT : 0;
            }

            return childGravity;
        }

        public static void fillLines(List<ViewDefinition> views, List<LineDefinition> lines, ConfigDefinition config) {
            LineDefinition currentLine = new LineDefinition(config);
            lines.add(currentLine);
            final int count = views.size();
            for (int i = 0; i < count; i++) {
                final ViewDefinition child = views.get(i);

                boolean newLine = child.isNewLine() || (config.isCheckCanFit() && !currentLine.canFit(child));
                if (newLine) {
                    currentLine = new LineDefinition(config);
                    if (config.getOrientation() == CommonLogic.VERTICAL && config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                        lines.add(0, currentLine);
                    } else {
                        lines.add(currentLine);
                    }
                }

                if (config.getOrientation() == CommonLogic.HORIZONTAL && config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                    currentLine.addView(0, child);
                } else {
                    currentLine.addView(child);
                }
            }
        }
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
        private int orientation = CommonLogic.HORIZONTAL;
        private float weightDefault = 0;
        private int gravity = Gravity.LEFT | Gravity.TOP;
        private int layoutDirection = View.LAYOUT_DIRECTION_LTR;
        private int maxWidth;
        private int maxHeight;
        private boolean checkCanFit;
        private int widthMode;
        private int heightMode;

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
        private int lineLength;
        private int lineThickness;
        private int lineStartThickness;
        private int lineStartLength;

        public LineDefinition(ConfigDefinition config) {
            this.config = config;
            this.lineStartThickness = 0;
            this.lineStartLength = 0;
        }

        public void addView(ViewDefinition child) {
            this.addView(this.views.size(), child);
        }

        public void addView(int i, ViewDefinition child) {
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
