package com.dacer.androidcharts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by Dacer on 11/11/13.
 * Edited by mateuszbuda on 10 July 2014
 */
public class BarView extends View {
    private ArrayList<Float> values;
    private boolean displayValues = true;
    private ArrayList<Float> percentList;
    private ArrayList<Float> targetPercentList;
    private Paint textPaint;
    private Paint bgPaint;
    private Paint fgPaint;
    private Rect rect;
    private int barWidth;
    private int bottomTextDescent;
    private boolean autoSetWidth = true;
    private int topMargin;
    private int bottomTextHeight;
    private ArrayList<String> bottomTextList = new ArrayList<String>();
    private final int MINI_BAR_WIDTH;
    private final int BAR_SIDE_MARGIN;
    private final int TEXT_TOP_MARGIN;
    private int textColor = Color.parseColor("#9B9A9B");
    private int bgColor = Color.parseColor("#F6F6F6");
    private int fgColor = Color.parseColor("#FC496D");

    public void setTextColor(String textColor) {
        this.textColor = Color.parseColor(textColor);
        textPaint.setColor(this.textColor);
    }

    public void setBgColor(String bgColor) {
        this.bgColor = Color.parseColor(bgColor);
        bgPaint.setColor(this.bgColor);
    }

    public void setFgColor(String fgColor) {
        this.fgColor = Color.parseColor(fgColor);
        fgPaint.setColor(this.fgColor);
    }

    private Runnable animator = new Runnable() {
        @Override
        public void run() {
            boolean needNewFrame = false;
            for (int i = 0; i < targetPercentList.size(); i++) {
                if (percentList.get(i) < targetPercentList.get(i)) {
                    percentList.set(i, percentList.get(i) + 0.02f);
                    needNewFrame = true;
                } else if (percentList.get(i) > targetPercentList.get(i)) {
                    percentList.set(i, percentList.get(i) - 0.02f);
                    needNewFrame = true;
                }
                if (Math.abs(targetPercentList.get(i) - percentList.get(i)) < 0.02f) {
                    percentList.set(i, targetPercentList.get(i));
                }
            }
            if (needNewFrame) {
                postDelayed(this, 20);
            }
            invalidate();
        }
    };

    public BarView(Context context) {
        this(context, null);
    }

    public BarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        bgPaint = new Paint();
        bgPaint.setAntiAlias(true);
        bgPaint.setColor(bgColor);
        fgPaint = new Paint(bgPaint);
        fgPaint.setColor(fgColor);
        rect = new Rect();
        topMargin = MyUtils.dip2px(context, 8);
        int textSize = MyUtils.sp2px(context, 15);
        barWidth = MyUtils.dip2px(context, 22);
        MINI_BAR_WIDTH = MyUtils.dip2px(context, 22);
        BAR_SIDE_MARGIN = MyUtils.dip2px(context, 22);
        TEXT_TOP_MARGIN = MyUtils.dip2px(context, 12);
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);
        percentList = new ArrayList<Float>();
    }

    /**
     * dataList will be reset when called is method.
     *
     * @param bottomStringList The String ArrayList in the bottom.
     */
    public void setBottomTextList(ArrayList<String> bottomStringList) {
        this.bottomTextList = bottomStringList;
        Rect r = new Rect();
        bottomTextDescent = 0;
        barWidth = MINI_BAR_WIDTH;
        for (String s : bottomTextList) {
            textPaint.getTextBounds(s, 0, s.length(), r);
            if (bottomTextHeight < r.height() * 4 / 3) {
                bottomTextHeight = r.height() * 4 / 3;
            }
            if (autoSetWidth && (barWidth < r.width())) {
                barWidth = r.width();
            }
            if (bottomTextDescent < (Math.abs(r.bottom))) {
                bottomTextDescent = Math.abs(r.bottom);
            }
        }
        setMinimumWidth(2);
        postInvalidate();
    }

    /**
     * @param list The ArrayList of Integer with the range of [0-max].
     */
    public void setDataList(ArrayList<Float> list, float max) {
        targetPercentList = new ArrayList<Float>();
        if (max == 0) max = 1;

        for (Float value : list) {
            targetPercentList.add(1 - value / max);
        }

        selectedBar = -1;
        values = list;

        // Make sure percentList.size() == targetPercentList.size()
        if (percentList.isEmpty() || percentList.size() < targetPercentList.size()) {
            int temp = targetPercentList.size() - percentList.size();
            for (int i = 0; i < temp; i++) {
                percentList.add(1f);
            }
        } else if (percentList.size() > targetPercentList.size()) {
            int temp = percentList.size() - targetPercentList.size();
            for (int i = 0; i < temp; i++) {
                percentList.remove(percentList.size() - 1);
            }
        }
        setMinimumWidth(2);
        removeCallbacks(animator);
        post(animator);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int i = 1;
        if (percentList != null && !percentList.isEmpty()) {
            for (Float f : percentList) {
                rect.set(BAR_SIDE_MARGIN * i + barWidth * (i - 1),
                        topMargin,
                        (BAR_SIDE_MARGIN + barWidth) * i,
                        topMargin + getHeight() - bottomTextHeight - TEXT_TOP_MARGIN);
                canvas.drawRect(rect, bgPaint);
                rect.set(BAR_SIDE_MARGIN * i + barWidth * (i - 1),
                        (topMargin - 1) + (int) ((getHeight() - bottomTextHeight - TEXT_TOP_MARGIN) * percentList.get(i - 1)),
                        (BAR_SIDE_MARGIN + barWidth) * i,
                        topMargin + getHeight() - bottomTextHeight - TEXT_TOP_MARGIN);
                canvas.drawRect(rect, fgPaint);
                i++;
            }
        }

        if (bottomTextList != null && !bottomTextList.isEmpty()) {
            i = 1;
            for (String s : bottomTextList) {
                canvas.drawText(s, BAR_SIDE_MARGIN * i + barWidth * (i - 1) + barWidth / 2,
                        getHeight() - bottomTextDescent, textPaint);
                i++;
            }
        }

        if (displayValues && selectedBar >= 0)
            drawDataText(canvas);
    }

    private void drawDataText(Canvas canvas) {
        Rect textRect = new Rect();
        int padding = MyUtils.dip2px(getContext(), 4);
        String vStr = Float.toString(values.get(selectedBar));

        if (percentList.get(selectedBar) != targetPercentList.get(selectedBar))
            return;

        textPaint.getTextBounds(vStr, 0, vStr.length(), textRect);

        float x = BAR_SIDE_MARGIN * (selectedBar + 1) + barWidth * selectedBar + barWidth / 2 + textRect.height() / 2;
        float y;

        if (textRect.width() + 2 * padding <
                (getHeight() - bottomTextHeight - TEXT_TOP_MARGIN) * percentList.get(selectedBar)) {
            // drawing above the bar
            y = topMargin + (getHeight() - bottomTextHeight - TEXT_TOP_MARGIN) * percentList.get(selectedBar)
                    - padding - textRect.width() / 2;
        } else {
            // drawing on the bar itself
            y = topMargin + (getHeight() - bottomTextHeight - TEXT_TOP_MARGIN) * percentList.get(selectedBar)
                    + padding + textRect.width() / 2;
        }
        canvas.save();
        canvas.rotate(-90, x, y);
        canvas.drawText(vStr, x, y, textPaint);
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mViewWidth = measureWidth(widthMeasureSpec);
        int mViewHeight = measureHeight(heightMeasureSpec);
        setMeasuredDimension(mViewWidth, mViewHeight);
    }

    private int measureWidth(int measureSpec) {
        int preferred = 0;
        if (bottomTextList != null) {
            preferred = bottomTextList.size() * (barWidth + BAR_SIDE_MARGIN);
        }
        return getMeasurement(measureSpec, preferred);
    }

    private int measureHeight(int measureSpec) {
        int preferred = 222;
        return getMeasurement(measureSpec, preferred);
    }

    private int getMeasurement(int measureSpec, int preferred) {
        int specSize = MeasureSpec.getSize(measureSpec);
        int measurement;
        switch (MeasureSpec.getMode(measureSpec)) {
            case MeasureSpec.EXACTLY:
                measurement = specSize;
                break;
            case MeasureSpec.AT_MOST:
                measurement = Math.min(preferred, specSize);
                break;
            default:
                measurement = preferred;
                break;
        }
        return measurement;
    }

    int selectedBar = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        selectedBar = -1;
        final Region r = new Region();

        for (int i = 1; i <= percentList.size(); i++) {
            r.set(BAR_SIDE_MARGIN * i + barWidth * (i - 1),
                    topMargin,
                    (BAR_SIDE_MARGIN + barWidth) * i,
                    topMargin + getHeight() - bottomTextHeight - TEXT_TOP_MARGIN);
            if (r.contains((int) event.getX(), (int) event.getY())) {
                selectedBar = i - 1;
                break;
            }
        }

        invalidate();
        return true;
    }
}
