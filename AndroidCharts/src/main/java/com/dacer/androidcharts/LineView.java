package com.dacer.androidcharts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.NinePatchDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;

import java.util.ArrayList;
import java.util.Collections;


/**
 * Created by Dacer on 11/4/13.
 * Edited by Lee youngchan 21/1/14
 * Edited by dector 30-Jun-2014
 * Edited by mateuszbuda 10 June 2014
 */
public class LineView extends View
{
    private int mViewHeight;
    private boolean autoSetGridWidth = true;
    private int bottomTextHeight = 0;
    private ArrayList<String> bottomTextList = new ArrayList<String>();

    private ArrayList<ArrayList<Float>> dataLists;

    private ArrayList<Integer> xCoordinateList = new ArrayList<Integer>();
    private ArrayList<Integer> yCoordinateList = new ArrayList<Integer>();
    private ArrayList<Integer> reversedYCoordinateList;

    private ArrayList<ArrayList<Dot>> drawDotLists = new ArrayList<ArrayList<Dot>>();
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;

    private Paint bottomTextPaint = new Paint();
    private int bottomTextDescent;

    //popup
    private Paint popupTextPaint = new Paint();
    private final int bottomTriangleHeight = 15;
    public boolean showPopup = true;

    private Dot pointToSelect;
    private Dot selectedDot;
    private int horizontalLinesCount = 6;

    public void setHorizontalLinesCount(int count)
    {
        horizontalLinesCount = count > 1 ? count : 2;
    }

    private int topLineLength = MyUtils.dip2px(getContext(), 12);       // |  | ←this
    //-+--+-
    private int sideLineLength = MyUtils.dip2px(getContext(), 45) / 3 * 2;   //-+--+--+--+--+--+--
    //  ↑this
    private int backgroundGridWidth = MyUtils.dip2px(getContext(), 45);

    //Constants
    private final int popupTopPadding = MyUtils.dip2px(getContext(), 2);
    private final int popupBottomMargin = MyUtils.dip2px(getContext(), 5);
    private final int bottomTextTopMargin = MyUtils.sp2px(getContext(), 5);
    private final int bottomLineLength = MyUtils.sp2px(getContext(), 22);
    private final int DOT_INNER_CIR_RADIUS = MyUtils.dip2px(getContext(), 2);
    private final int DOT_OUTER_CIR_RADIUS = MyUtils.dip2px(getContext(), 5);
    private final int MIN_VERTICAL_GRID_NUM = 4;
    private final int MIN_HORIZONTAL_GRID_NUM = 1;
    private final int BACKGROUND_LINE_COLOR = Color.parseColor("#EEEEEE");
    private final int BOTTOM_TEXT_COLOR = Color.parseColor("#9B9A9B");
    private final static int ACCURACY = 10;

    public static final int SHOW_POPUPS_All = 1;
    public static final int SHOW_POPUPS_MAXMIN_ONLY = 2;
    public static final int SHOW_POPUPS_NONE = 3;

    private int showPopupType = SHOW_POPUPS_NONE;

    public void setShowPopup(int popupType)
    {
        this.showPopupType = popupType;
    }

    private Boolean drawDotLine = false;
    private String[] colorArray = {"#e74c3c", "#2980b9", "#1abc9c"};
    private int[] popupColorArray =
            {R.drawable.popup_red, R.drawable.popup_blue, R.drawable.popup_green};

    // onDraw optimisations
    private final Point tmpPoint = new Point();

    public void setDrawDotLine(Boolean drawDotLine)
    {
        this.drawDotLine = drawDotLine;
    }

    private Runnable animator = new Runnable()
    {
        @Override
        public void run()
        {
            boolean needNewFrame = false;
            for (ArrayList<Dot> data : drawDotLists)
            {
                for (Dot dot : data)
                {
                    dot.update();
                    if (!dot.isAtRest())
                    {
                        needNewFrame = true;
                    }
                }
            }
            if (needNewFrame)
            {
                postDelayed(this, 18);
            }
            invalidate();
        }
    };

    private ArrayList<Integer> currentDots = new ArrayList<Integer>();
    private Runnable drawer = new Runnable()
    {
        @Override
        public void run()
        {
            int x = 0;
            for (int k = 0; k < drawDotLists.size(); k++)
            {
                Dot dot = drawDotLists.get(k).get(currentDots.get(k));
                if (dot.visible < FUN_SPLIT)
                {
                    dot.visible += (FUN_SPLIT / 5);
                    x = dot.x;
                } else
                {
                    currentDots.set(k, currentDots.get(k) + 1);
                    Dot nextDot = drawDotLists.get(k).get(currentDots.get(k));
                    nextDot.visible += 1;
                    x = nextDot.x;
                }
            }

            for (int k = 0; k < drawDotLists.size(); k++)
                if (drawDotLists.get(k).get(drawDotLists.get(k).size() - 1).visible < 0)
                {
                    postDelayed(this, (int) ((float) 32 * (1 - ((float) x / (float) getWidth() / 2))));
//                    postDelayed(this, 32);
                    break;
                }

            invalidate(x - DOT_OUTER_CIR_RADIUS, 0,
                    x + (xCoordinateList.get(1) - xCoordinateList.get(0)) + DOT_OUTER_CIR_RADIUS,
                    getHeight() - bottomTextHeight);
        }
    };

    private boolean horizontalAnimation = false;

    public void enableHorizontalAnimation()
    {
        horizontalAnimation = true;
    }

    public void disableHorizontalAnimation()
    {
        horizontalAnimation = false;
    }

    private boolean interpolation = false;

    public void enableInterpolation()
    {
        interpolation = true;
    }

    public void disableInterpolation()
    {
        interpolation = false;
    }

    private final int FUN_SPLIT = 20;
    private ArrayList<UnivariateFunction> functions = new ArrayList<UnivariateFunction>();

    public LineView(Context context)
    {
        this(context, null);
    }

    public LineView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        popupTextPaint.setAntiAlias(true);
        popupTextPaint.setColor(Color.WHITE);
        popupTextPaint.setTextSize(MyUtils.sp2px(getContext(), 13));
        popupTextPaint.setStrokeWidth(5);
        popupTextPaint.setTextAlign(Paint.Align.CENTER);

        bottomTextPaint.setAntiAlias(true);
        bottomTextPaint.setTextSize(MyUtils.sp2px(getContext(), 12));
        bottomTextPaint.setTextAlign(Paint.Align.CENTER);
        bottomTextPaint.setStyle(Paint.Style.FILL);
        bottomTextPaint.setColor(BOTTOM_TEXT_COLOR);
    }

    /**
     * dataList will be reset when called is method.
     *
     * @param bottomTextList The String ArrayList in the bottom.
     */
    public void setBottomTextList(ArrayList<String> bottomTextList)
    {
        this.bottomTextList = bottomTextList;

        Rect r = new Rect();
        int longestWidth = 0;
        String longestStr = "";
        bottomTextDescent = 0;
        for (String s : bottomTextList)
        {
            bottomTextPaint.getTextBounds(s, 0, s.length(), r);
            if (bottomTextHeight < r.height())
            {
                bottomTextHeight = r.height();
            }
            if (autoSetGridWidth && (longestWidth < r.width()))
            {
                longestWidth = r.width();
                longestStr = s;
            }
            if (bottomTextDescent < (Math.abs(r.bottom)))
            {
                bottomTextDescent = Math.abs(r.bottom);
            }
        }

        if (autoSetGridWidth)
        {
            if (backgroundGridWidth < longestWidth)
            {
                backgroundGridWidth =
                        longestWidth + (int) bottomTextPaint.measureText(longestStr, 0, 1);
            }
            if (sideLineLength < longestWidth / 2)
            {
                sideLineLength = longestWidth / 2;
            }
        }

        refreshXCoordinateList(getHorizontalGridNum());
    }

    /**
     * @param dataLists The Integer ArrayLists for showing,
     *                  dataList.size() must < bottomTextList.size()
     */
    public void setDataList(ArrayList<ArrayList<Float>> dataLists)
    {
        selectedDot = null;
        this.dataLists = dataLists;
        for (ArrayList<Float> list : dataLists)
        {
            if (list.size() > bottomTextList.size())
            {
                throw new RuntimeException("dacer.LineView error:" +
                        " dataList.size() > bottomTextList.size() !!!");
            }
        }
        refreshAfterDataChanged();
        showPopup = true;
        setMinimumWidth(0); // It can help the LineView reset the Width,
        // I don't know the better way..
        postInvalidate();
    }

    private void refreshAfterDataChanged()
    {
        int verticalGridNum = getVerticalGridlNum();
        refreshTopLineLength(verticalGridNum);
        refreshYCoordinateList(verticalGridNum);
        refreshDrawDotList(verticalGridNum);
    }

    private int getVerticalGridlNum()
    {
        int tmp;
        min = Integer.MAX_VALUE;
        max = Integer.MIN_VALUE;
        for (ArrayList<Float> list : dataLists)
        {
            min = min > (tmp = (int) Math.floor(Collections.min(list))) ? tmp : min;
            max = max < (tmp = (int) Math.ceil(Collections.max(list))) ? tmp : max;
        }
        return (max - min) * ACCURACY;
    }

    private int getHorizontalGridNum()
    {
        int horizontalGridNum = bottomTextList.size() - 1;
        if (horizontalGridNum < MIN_HORIZONTAL_GRID_NUM)
        {
            horizontalGridNum = MIN_HORIZONTAL_GRID_NUM;
        }
        return horizontalGridNum;
    }

    private void refreshXCoordinateList(int horizontalGridNum)
    {
        xCoordinateList.clear();
        for (int i = 0; i < (horizontalGridNum + 1); i++)
        {
            xCoordinateList.add(sideLineLength + backgroundGridWidth * i);
        }

    }

    private void refreshYCoordinateList(int verticalGridNum)
    {
        yCoordinateList.clear();
        for (int i = 0; i < (verticalGridNum + 1); i++)
        {
            yCoordinateList.add(topLineLength +
                    ((mViewHeight - topLineLength - bottomTextHeight - bottomTextTopMargin -
                            bottomLineLength - bottomTextDescent) * i / (verticalGridNum)));
        }
        reversedYCoordinateList = (ArrayList) yCoordinateList.clone();
        Collections.reverse(reversedYCoordinateList);
    }

    private void refreshDrawDotList(int verticalGridNum)
    {
        if (dataLists != null && !dataLists.isEmpty())
        {
            if (drawDotLists.size() == 0)
            {
                for (int k = 0; k < dataLists.size(); k++)
                {
                    drawDotLists.add(new ArrayList<LineView.Dot>());
                }
            }
            for (int k = 0; k < dataLists.size(); k++)
            {
                int drawDotSize = drawDotLists.get(k).size();

                for (int i = 0; i < dataLists.get(k).size(); i++)
                {
                    int x = xCoordinateList.get(i);
                    int d = verticalGridNum - Math.round(dataLists.get(k).get(i) * ACCURACY) +
                            min * ACCURACY;
                    int y = yCoordinateList.get(d);
                    if (i > drawDotSize - 1)
                    {
                        drawDotLists.get(k).add(new Dot(x, 0, x, y, dataLists.get(k).get(i), k));
                    } else
                    {
                        drawDotLists.get(k).set(i, drawDotLists.get(k).get(i)
                                .setTargetData(x, y, dataLists.get(k).get(i), k));
                    }
                }

                int temp = drawDotLists.get(k).size() - dataLists.get(k).size();
                for (int i = 0; i < temp; i++)
                {
                    drawDotLists.get(k).remove(drawDotLists.get(k).size() - 1);
                }
            }
        }

        removeCallbacks(drawer);
        removeCallbacks(animator);

        if (horizontalAnimation)
        {
            currentDots.clear();
            for (int i = 0; i < drawDotLists.size(); i++)
            {
                currentDots.add(0);
                drawDotLists.get(i).get(0).visible = 0;
            }
            post(drawer);
        } else
            post(animator);

        functions.clear();
    }

    private void refreshTopLineLength(int verticalGridNum)
    {
        topLineLength = getPopupHeight() + popupBottomMargin + DOT_OUTER_CIR_RADIUS;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        drawBackgroundLines(canvas);
        drawLines(canvas);
        drawDots(canvas);

        for (int k = 0; k < drawDotLists.size(); k++)
        {
            float MaxValue = Collections.max(dataLists.get(k));
            float MinValue = Collections.min(dataLists.get(k));
            for (Dot d : drawDotLists.get(k))
            {
                if (showPopupType == SHOW_POPUPS_All)
                    drawPopup(canvas, String.valueOf(d.data), d.setupPoint(tmpPoint),
                            popupColorArray[k % 3]);
                else if (showPopupType == SHOW_POPUPS_MAXMIN_ONLY)
                {
                    if (d.data == MaxValue)
                        drawPopup(canvas, String.valueOf(d.data), d.setupPoint(tmpPoint),
                                popupColorArray[k % 3]);
                    if (d.data == MinValue)
                        drawPopup(canvas, String.valueOf(d.data), d.setupPoint(tmpPoint),
                                popupColorArray[k % 3]);
                }
            }
        }
        if (showPopup && selectedDot != null)
        {
            drawPopup(canvas,
                    String.valueOf(selectedDot.data),
                    selectedDot.setupPoint(tmpPoint), popupColorArray[selectedDot.linenumber % 3]);
        }
    }

    /**
     * @param canvas The canvas you need to draw on.
     * @param point  The Point consists of the x y coordinates from left bottom to right top.
     *               Like is
     *               <p/>
     *               3
     *               2
     *               1
     *               0 1 2 3 4 5
     */
    private void drawPopup(Canvas canvas, String num, Point point, int PopupColor)
    {
        boolean singularNum = (num.length() == 1);
        int sidePadding = MyUtils.dip2px(getContext(), singularNum ? 8 : 5);
        int x = point.x;
        int y = point.y - MyUtils.dip2px(getContext(), 5);
        Rect popupTextRect = new Rect();
        popupTextPaint.getTextBounds(num, 0, num.length(), popupTextRect);
        Rect r = new Rect(x - popupTextRect.width() / 2 - sidePadding,
                y - popupTextRect.height() - bottomTriangleHeight - popupTopPadding * 2 -
                        popupBottomMargin,
                x + popupTextRect.width() / 2 + sidePadding,
                y + popupTopPadding - popupBottomMargin
        );

        NinePatchDrawable popup = (NinePatchDrawable) getResources().getDrawable(PopupColor);
        popup.setBounds(r);
        popup.draw(canvas);
        canvas.drawText(num, x, y - bottomTriangleHeight - popupBottomMargin, popupTextPaint);
    }

    private int getPopupHeight()
    {
        Rect popupTextRect = new Rect();
        popupTextPaint.getTextBounds("9", 0, 1, popupTextRect);
        Rect r = new Rect(-popupTextRect.width() / 2,
                -popupTextRect.height() - bottomTriangleHeight - popupTopPadding * 2 -
                        popupBottomMargin,
                +popupTextRect.width() / 2,
                +popupTopPadding - popupBottomMargin
        );
        return r.height();
    }

    private void drawDots(Canvas canvas)
    {
        Paint bigCirPaint = new Paint();
        bigCirPaint.setAntiAlias(true);
        Paint smallCirPaint = new Paint(bigCirPaint);
        smallCirPaint.setColor(Color.parseColor("#FFFFFF"));
        if (drawDotLists != null && !drawDotLists.isEmpty())
        {
            for (int k = 0; k < drawDotLists.size(); k++)
            {
                bigCirPaint.setColor(Color.parseColor(colorArray[k % 3]));
                for (Dot dot : drawDotLists.get(k))
                {
                    if (horizontalAnimation && dot.visible < 0)
                        break;
                    canvas.drawCircle(dot.x, dot.y, DOT_OUTER_CIR_RADIUS, bigCirPaint);
                    canvas.drawCircle(dot.x, dot.y, DOT_INNER_CIR_RADIUS, smallCirPaint);
                }
            }
        }
    }

    private void drawLines(Canvas canvas)
    {
        Paint linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setStrokeWidth(MyUtils.dip2px(getContext(), 2));
        for (int k = 0; k < drawDotLists.size(); k++)
        {
            linePaint.setColor(Color.parseColor(colorArray[k % 3]));

            if (!interpolation)
            {
                for (int i = 0; i < drawDotLists.get(k).size() - 1; i++)
                {
                    canvas.drawLine(drawDotLists.get(k).get(i).x,
                            drawDotLists.get(k).get(i).y,
                            drawDotLists.get(k).get(i + 1).x,
                            drawDotLists.get(k).get(i + 1).y,
                            linePaint);
                }
            } else
            {
                if (functions.size() < k + 1)
                {
                    double[] x = new double[drawDotLists.get(k).size()];
                    double[] y = new double[drawDotLists.get(k).size()];

                    for (int i = 0; i < drawDotLists.get(k).size(); i++)
                    {
                        x[i] = drawDotLists.get(k).get(i).x;
                        y[i] = drawDotLists.get(k).get(i).y;
                    }

                    UnivariateInterpolator interpolator = new SplineInterpolator();
                    functions.add(interpolator.interpolate(x, y));
                }

                for (int i = 0; i < drawDotLists.get(k).size() - 1; i++)
                {
                    if (drawDotLists.get(k).get(i).visible < 0)
                        break;

                    double x1 = drawDotLists.get(k).get(i).x;
                    double x2 = drawDotLists.get(k).get(i + 1).x;
                    double step = (x2 - x1) / FUN_SPLIT;
                    for (int s = 0; s < FUN_SPLIT; s++)
                    {
                        if (drawDotLists.get(k).get(i).visible < s)
                            break;

                        canvas.drawLine((float) (x1 + s * step),
                                (float) functions.get(k).value(x1 + s * step),
                                (float) (x1 + (s + 1) * step),
                                (float) functions.get(k).value(x1 + (s + 1) * step),
                                linePaint);
                    }
                }
            }
        }
    }


    private void drawBackgroundLines(Canvas canvas)
    {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(MyUtils.dip2px(getContext(), 1f));
        paint.setColor(BACKGROUND_LINE_COLOR);
        PathEffect effects = new DashPathEffect(
                new float[]{10, 5, 10, 5}, 1);

        //draw vertical lines
        for (int i = 0; i < xCoordinateList.size(); i++)
        {
            canvas.drawLine(xCoordinateList.get(i),
                    getPopupHeight() / 2,
                    xCoordinateList.get(i),
                    mViewHeight - 2 * bottomTextTopMargin - bottomTextHeight - bottomTextDescent,
                    paint);
        }

        int n = yCoordinateList.size();
        int m = n / horizontalLinesCount;

        if (drawDotLine)
        {
            //draw dotted lines
            paint.setPathEffect(effects);
            Path dottedPath = new Path();
            for (int i = 0; i < n; i++)
            {
                if ((n < horizontalLinesCount) || (i % m == 0))
                {
                    dottedPath.moveTo(0, reversedYCoordinateList.get(i));
                    dottedPath.lineTo(getWidth(), reversedYCoordinateList.get(i));
                    canvas.drawPath(dottedPath, paint);
                }
            }
        } else
        {
            //draw solid lines
            for (int i = 0; i < n; i++)
            {
                if ((n < horizontalLinesCount) || (i % m == 0))
                {
                    canvas.drawLine(0, reversedYCoordinateList.get(i), getWidth(),
                            reversedYCoordinateList.get(i), paint);
                }
            }
        }

        //draw bottom text
        if (bottomTextList != null)
        {
            for (int i = 0; i < bottomTextList.size(); i++)
            {
                canvas.drawText(bottomTextList.get(i), sideLineLength + backgroundGridWidth * i,
                        mViewHeight - bottomTextDescent, bottomTextPaint);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int mViewWidth = measureWidth(widthMeasureSpec);
        mViewHeight = measureHeight(heightMeasureSpec);
        refreshAfterDataChanged();
        setMeasuredDimension(mViewWidth, mViewHeight);
    }

    private int measureWidth(int measureSpec)
    {
        int horizontalGridNum = getHorizontalGridNum();
        int preferred = backgroundGridWidth * horizontalGridNum + sideLineLength * 2;
        return getMeasurement(measureSpec, preferred);
    }

    private int measureHeight(int measureSpec)
    {
        int preferred = 0;
        return getMeasurement(measureSpec, preferred);
    }

    private int getMeasurement(int measureSpec, int preferred)
    {
        int specSize = MeasureSpec.getSize(measureSpec);
        int measurement;
        switch (MeasureSpec.getMode(measureSpec))
        {
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

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            pointToSelect = findPointAt((int) event.getX(), (int) event.getY());
        } else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            selectedDot = pointToSelect;
            pointToSelect = null;
            postInvalidate();
        }

        return true;
    }

    private Dot findPointAt(int x, int y)
    {
        if (drawDotLists.isEmpty())
        {
            return null;
        }

        ArrayList<Dot> candidates = new ArrayList<Dot>(dataLists.size());
        final int width = backgroundGridWidth / 2;
        final Region r = new Region();

        for (ArrayList<Dot> data : drawDotLists)
        {
            for (Dot dot : data)
            {
                final int pointX = dot.x;
                final int pointY = dot.y;

                r.set(pointX - width, pointY - width, pointX + width, pointY + width);
                if (r.contains(x, y))
                {
                    candidates.add(dot);
                }
            }
        }

        Dot nearest = null;
        double minDist = Double.MAX_VALUE, tmp;
        Point p = new Point(x, y);
        for (Dot dot : candidates)
            if (minDist > (tmp = distance(p, new Point(dot.x, dot.y))))
            {
                minDist = tmp;
                nearest = dot;
            }

        return nearest;
    }

    private double distance(Point p, Point q)
    {
        return (p.x - q.x) * (p.x - q.x) + (p.y - q.y) * (p.y - q.y);
    }


    class Dot
    {
        int x;
        int y;
        float data;
        int targetX;
        int targetY;
        int linenumber;
        int velocity = MyUtils.dip2px(getContext(), 18);
        int visible = -1;

        Dot(int x, int y, int targetX, int targetY, float data, int linenumber)
        {
            this.x = x;
            this.y = y;
            this.linenumber = linenumber;
            setTargetData(targetX, targetY, data, linenumber);
        }

        Point setupPoint(Point point)
        {
            point.set(x, y);
            return point;
        }

        Dot setTargetData(int targetX, int targetY, float data, int linenumber)
        {
            this.targetX = targetX;
            this.targetY = targetY;
            this.data = data;
            this.linenumber = linenumber;
            this.visible = -1;
            if (horizontalAnimation)
            {
                this.x = targetX;
                this.y = targetY;
            }
            return this;
        }

        boolean isAtRest()
        {
            return (x == targetX) && (y == targetY);
        }

        void update()
        {
            x = updateSelf(x, targetX, velocity);
            y = updateSelf(y, targetY, velocity);
        }

        private int updateSelf(int origin, int target, int velocity)
        {
            if (origin < target)
            {
                origin += velocity;
            } else if (origin > target)
            {
                origin -= velocity;
            }
            if (Math.abs(target - origin) < velocity)
            {
                origin = target;
            }
            return origin;
        }
    }
}
