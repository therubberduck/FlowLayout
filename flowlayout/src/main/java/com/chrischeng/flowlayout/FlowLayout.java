package com.chrischeng.flowlayout;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.LinkedHashSet;

public class FlowLayout extends ViewGroup {

    private float mHorizontalSpacing;
    private float mVerticalSpacing;
    private int mMaxSelectedNum;
    private LinkedHashSet<Integer> mSelectedPos;
    protected OnStateChangedListener mListener;
    private MotionEvent mMotionEvent;

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        initAttrs(attrs);
        initFields();
    }

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        this.mListener = listener;
    }

    public void setAdapter(FlowAdapter adapter) {
        removeAllViews();
        mSelectedPos.clear();

        for (int i = 0; i < adapter.getCount(); i++) {
            View v = adapter.getView(i, this);
            addView(v);
            if (v.isSelected())
                mSelectedPos.add(i);
        }
    }

    public void setHorizontalSpacing(int horizontalSpacing) {
        mHorizontalSpacing = horizontalSpacing;
        invalidate();
    }

    public void setVerticalSpacing(int verticalSpacing) {
        mVerticalSpacing = verticalSpacing;
        invalidate();
    }

    public void setMaxSelectedNum(int max) {
        mMaxSelectedNum = max;
    }

    public void setAllState(boolean isSelected) {
        int count = getChildCount();

        if (isSelected) {
            for (int i = 0; i < count; i++) {
                mSelectedPos.add(i);
                getChildAt(i).setSelected(true);
            }
        } else {
            mSelectedPos.clear();
            for (int i = 0; i < count; i++)
                getChildAt(i).setSelected(false);
        }
    }

    public LinkedHashSet<Integer> getSelectedPos() {
        return mSelectedPos;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP)
            mMotionEvent = MotionEvent.obtain(event);

        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        if (mMotionEvent == null)
            return super.performClick();

        View child = findChild((int) mMotionEvent.getX(), (int) mMotionEvent.getY());

        mMotionEvent = null;

        if (child != null) {
            int pos = findPosByView(child);

            if (pos != Constants.INVALID_RESULT) {
                boolean preState = child.isSelected();

                if (preState) {
                    mSelectedPos.remove(pos);
                    child.setSelected(false);
                    if (mListener != null)
                        mListener.onStateChanged(pos, false);
                } else {
                    if (mSelectedPos.size() != mMaxSelectedNum) {
                        mSelectedPos.add(pos);
                        child.setSelected(true);
                        if (mListener != null)
                            mListener.onStateChanged(pos, true);
                    } else if (mListener != null)
                        mListener.onMaxNumSelected();
                }
            }
        }

        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int selfWidth = resolveSize(0, widthMeasureSpec);
        int childCount = getChildCount();

        int childLeft = getPaddingLeft();
        int childTop = getPaddingTop();
        int lineHeight = 0;

        for (int i = 0; i < childCount; i++) {
            View v = getChildAt(i);
            LayoutParams params = v.getLayoutParams();
            v.measure(getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight(), params.width),
                    getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom(), params.height));
            int childWidth = v.getMeasuredWidth();
            int childHeight = v.getMeasuredHeight();
            lineHeight = Math.max(lineHeight, childHeight);

            if (childLeft + childWidth + getPaddingRight() > selfWidth) {
                childLeft = getPaddingLeft();
                childTop += mVerticalSpacing + lineHeight;
                lineHeight = childHeight;
            } else
                childLeft += mHorizontalSpacing + childWidth;
        }

        setMeasuredDimension(selfWidth, resolveSize(childTop + lineHeight + getPaddingBottom(), heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        int childCount = getChildCount();

        int childLeft = getPaddingLeft();
        int childTop = getPaddingTop();
        int lineHeight = 0;

        for (int i = 0; i < childCount; i++) {
            View v = getChildAt(i);

            if (v.getVisibility() == View.GONE)
                continue;

            int childWidth = v.getMeasuredWidth();
            int childHeight = v.getMeasuredHeight();
            lineHeight = Math.max(lineHeight, childHeight);

            if (childLeft + childWidth + getPaddingRight() > w) {
                childLeft = getPaddingLeft();
                childTop += mVerticalSpacing + lineHeight;
                lineHeight = childHeight;
            }

            v.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            childLeft += mHorizontalSpacing + childWidth;
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        int count = getChildCount();
        if (count <= 0)
            return super.onSaveInstanceState();

        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.BUNDLE_KEY_STATE, super.onSaveInstanceState());
        bundle.putSerializable(Constants.BUNDLE_KEY_SELECTS, mSelectedPos);
        return bundle;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mSelectedPos = (LinkedHashSet<Integer>) bundle.getSerializable(Constants.BUNDLE_KEY_SELECTS);
            if (mSelectedPos != null) {
                int size = mSelectedPos.size();
                if (size > 0) {
                    for (int i : mSelectedPos) {
                        View v = getChildAt(i);
                        if (v != null)
                            v.setSelected(true);
                    }
                }
            }

            super.onRestoreInstanceState(bundle.getParcelable(Constants.BUNDLE_KEY_STATE));
            return;
        }
        super.onRestoreInstanceState(state);
    }

    private void initAttrs(AttributeSet attrs) {
        Resources res = getResources();

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.FlowLayout);
        mHorizontalSpacing = a.getDimension(R.styleable.FlowLayout_fl_spacing_horizontal, res.getDimension(R.dimen.spacing));
        mVerticalSpacing = a.getDimension(R.styleable.FlowLayout_fl_spcaing_vertical, res.getDimension(R.dimen.spacing));
        mMaxSelectedNum = a.getInteger(R.styleable.FlowLayout_fl_max_selected, Constants.INVALID_RESULT);
        a.recycle();
    }

    private void initFields() {
        mSelectedPos = new LinkedHashSet<>();
    }

    private View findChild(int x, int y) {
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            if (v.getVisibility() == View.GONE)
                continue;

            Rect outRect = new Rect();
            v.getHitRect(outRect);
            if (outRect.contains(x, y)) {
                return v;
            }
        }
        return null;
    }

    private int findPosByView(View child) {
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            if (v == child)
                return i;
        }

        return Constants.INVALID_RESULT;
    }
}
