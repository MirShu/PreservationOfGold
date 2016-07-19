package main.litigationpreservation.com.preservationofgold;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.util.AttributeSet;
import android.view.View;


/**
 * Created by Shulin on 2016/7/16.
 * 折线自定义View类
 */
public class LineView extends View {
    private int mViewHeight;
    private boolean autoSetDataOfGird = true;
    private boolean autoSetGridWidth = true;
    private int dataOfAGird = 10;
    private int bottomTextHeight = 0;
    private ArrayList<String> bottomTextList;
    private ArrayList<Integer> dataList;
    private ArrayList<Integer> xCoordinateList = new ArrayList<Integer>();
    private ArrayList<Integer> yCoordinateList = new ArrayList<Integer>();
    private ArrayList<Dot> drawDotList = new ArrayList<Dot>();
    ;
    private Paint bottomTextPaint = new Paint();
    private Paint ycoordTextPaint = new Paint();
    private int bottomTextDescent;

    private Paint popupTextPaint = new Paint();
    private final int bottomTriangleHeight = 12;
    private boolean mShowYCoordinate = true;

    private int topLineLength = MyUtils.dip2px(getContext(), 12);
    private int sideLineLength = MyUtils.dip2px(getContext(), 45) / 3 * 2;
    private int backgroundGridWidth = MyUtils.dip2px(getContext(), 45);
    private final int popupTopPadding = MyUtils.dip2px(getContext(), 2);
    private final int popupBottomMargin = MyUtils.dip2px(getContext(), 5);
    private final int bottomTextTopMargin = MyUtils.sp2px(getContext(), 5);
    private final int bottomLineLength = MyUtils.sp2px(getContext(), 22);
    private final int DOT_INNER_CIR_RADIUS = MyUtils.dip2px(getContext(), 2);
    private final int DOT_OUTER_CIR_RADIUS = MyUtils.dip2px(getContext(), 5);
    private final int MIN_TOP_LINE_LENGTH = MyUtils.dip2px(getContext(), 12);
    private final int MIN_VERTICAL_GRID_NUM = 4;
    private final int MIN_HORIZONTAL_GRID_NUM = 1;
    private final int BACKGROUND_LINE_COLOR = Color.parseColor("#f5f4f2");  //背景方格子线条颜色值，设置白色则为隐藏不显示状态。
    private final int BOTTOM_TEXT_COLOR = Color.parseColor("#9B9A9B");//左边金额数值TextView颜色值
    private final int YCOORD_TEXT_LEFT_MARGIN = MyUtils.dip2px(getContext(), 10);

    class YCoordData {
        private int y;
        private int data;

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getData() {
            return data;
        }

        public void setData(int data) {
            this.data = data;
        }
    }

    private Runnable animator = new Runnable() {
        @Override
        public void run() {
            boolean needNewFrame = false;
            for (Dot dot : drawDotList) {
                dot.update();
                if (!dot.isAtRest()) {
                    needNewFrame = true;
                }
            }
            if (needNewFrame) {
                postDelayed(this, 0);
            }
            invalidate();
        }
    };

    public LineView(Context context) {
        this(context, null);
    }

    public LineView(Context context, AttributeSet attrs) {
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

        ycoordTextPaint.setAntiAlias(true);
        ycoordTextPaint.setTextSize(MyUtils.sp2px(getContext(), 12));
        ycoordTextPaint.setTextAlign(Paint.Align.LEFT);
        ycoordTextPaint.setStyle(Paint.Style.FILL);
        ycoordTextPaint.setColor(BOTTOM_TEXT_COLOR);
    }

    public void setBottomTextList(ArrayList<String> bottomTextList) {
        this.dataList = null;
        this.bottomTextList = bottomTextList;

        Rect r = new Rect();
        int longestWidth = 0;
        String longestStr = "";
        bottomTextDescent = 0;
        for (String s : bottomTextList) {
            bottomTextPaint.getTextBounds(s, 0, s.length(), r);
            if (bottomTextHeight < r.height()) {
                bottomTextHeight = r.height();
            }
            if (autoSetGridWidth && (longestWidth < r.width())) {
                longestWidth = r.width();
                longestStr = s;
            }
            if (bottomTextDescent < (Math.abs(r.bottom))) {
                bottomTextDescent = Math.abs(r.bottom);
            }
        }

        if (autoSetGridWidth) {
            if (backgroundGridWidth < longestWidth) {
                backgroundGridWidth = longestWidth + (int) bottomTextPaint.measureText(longestStr, 0, 1);
            }
            if (sideLineLength < longestWidth / 2) {
                sideLineLength = longestWidth / 2;
            }
        }

        refreshXCoordinateList(getHorizontalGridNum());
    }


    public void setDataList(ArrayList<Integer> dataList) {
        this.dataList = dataList;
        if (dataList.size() > bottomTextList.size()) {
            throw new RuntimeException("dacer.LineView error:" +
                    " dataList.size() > bottomTextList.size() !!!");
        }
        if (autoSetDataOfGird) {
            int biggestData = 0;
            for (Integer i : dataList) {
                if (biggestData < i) {
                    biggestData = i;
                }
            }
            dataOfAGird = 1;
            while (biggestData / 10 > dataOfAGird) {
                dataOfAGird *= 10;
            }
        }
        refreshAfterDataChanged();
        setMinimumWidth(0);
        postInvalidate();
    }

    public void setShowYCoordinate(boolean showYCoordinate) {
        mShowYCoordinate = showYCoordinate;
    }

    private void refreshAfterDataChanged() {
        int verticalGridNum = getVerticalGridlNum();
        refreshTopLineLength(verticalGridNum);
        refreshYCoordinateList(verticalGridNum);
        refreshDrawDotList(verticalGridNum);
    }

    private int getVerticalGridlNum() {
        int verticalGridNum = MIN_VERTICAL_GRID_NUM;
        if (dataList != null && !dataList.isEmpty()) {
            for (Integer integer : dataList) {
                if (verticalGridNum < (integer + 1)) {
                    verticalGridNum = integer + 1;
                }
            }
        }
        return verticalGridNum;
    }

    private int getHorizontalGridNum() {
        int horizontalGridNum = bottomTextList.size() - 1;
        if (horizontalGridNum < MIN_HORIZONTAL_GRID_NUM) {
            horizontalGridNum = MIN_HORIZONTAL_GRID_NUM;
        }
        return horizontalGridNum;
    }

    private void refreshXCoordinateList(int horizontalGridNum) {
        xCoordinateList.clear();
        for (int i = 0; i < (horizontalGridNum + 1); i++) {
            xCoordinateList.add(sideLineLength + backgroundGridWidth * i);
        }

    }

    private void refreshYCoordinateList(int verticalGridNum) {
        yCoordinateList.clear();
        for (int i = 0; i < (verticalGridNum + 1); i++) {
            yCoordinateList.add(topLineLength +
                    ((mViewHeight - topLineLength - bottomTextHeight - bottomTextTopMargin -
                            bottomLineLength - bottomTextDescent) * i / (verticalGridNum)));
        }
    }

    private void refreshDrawDotList(int verticalGridNum) {
        if (dataList != null && !dataList.isEmpty()) {
            int drawDotSize = drawDotList.isEmpty() ? 0 : drawDotList.size();
            for (int i = 0; i < dataList.size(); i++) {
                int x = xCoordinateList.get(i);
                int y = yCoordinateList.get(verticalGridNum - dataList.get(i));
                if (i > drawDotSize - 1) {
                    drawDotList.add(new Dot(x, 0, x, y, dataList.get(i)));
                } else {
                    drawDotList.set(i, drawDotList.get(i).setTargetData(x, y, dataList.get(i)));
                }
            }
            int temp = drawDotList.size() - dataList.size();
            for (int i = 0; i < temp; i++) {
                drawDotList.remove(drawDotList.size() - 1);
            }
        }
        removeCallbacks(animator);
        post(animator);
    }

    private void refreshTopLineLength(int verticalGridNum) {
        if ((mViewHeight - topLineLength - bottomTextHeight - bottomTextTopMargin) /
                (verticalGridNum + 2) < getPopupHeight()) {
            topLineLength = getPopupHeight() + DOT_OUTER_CIR_RADIUS + DOT_INNER_CIR_RADIUS + 2;
        } else {
            topLineLength = MIN_TOP_LINE_LENGTH;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackgroundLines(canvas);
        drawLines(canvas);
        drawDots(canvas);
        for (Dot dot : drawDotList) {
            drawPopup(canvas,
                    String.valueOf(dot.data),
                    dot.getPoint());
        }
    }

    private void drawPopup(Canvas canvas, String num, Point point) {
        boolean singularNum = (num.length() == 1);
        int sidePadding = MyUtils.dip2px(getContext(), singularNum ? 8 : 5);
        int x = point.x;
        if (mShowYCoordinate == true) x += YCOORD_TEXT_LEFT_MARGIN;
        int y = point.y - MyUtils.dip2px(getContext(), 5);
        Rect popupTextRect = new Rect();
        popupTextPaint.getTextBounds(num, 0, num.length(), popupTextRect);
        Rect r = new Rect(x - popupTextRect.width() / 2 - sidePadding,
                y - popupTextRect.height() - bottomTriangleHeight - popupTopPadding * 2 - popupBottomMargin,
                x + popupTextRect.width() / 2 + sidePadding,
                y + popupTopPadding - popupBottomMargin);

        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.popup_red);
        byte chunk[] = bmp.getNinePatchChunk();
        NinePatchDrawable popup = new NinePatchDrawable(bmp, chunk, new Rect(), null);
        popup.setBounds(r);
        popup.draw(canvas);
        canvas.drawText(num, x, y - bottomTriangleHeight - popupBottomMargin, popupTextPaint);
    }

    private int getPopupHeight() {
        Rect popupTextRect = new Rect();
        popupTextPaint.getTextBounds("9", 0, 1, popupTextRect);
        Rect r = new Rect(-popupTextRect.width() / 2,
                -popupTextRect.height() - bottomTriangleHeight - popupTopPadding * 2 - popupBottomMargin,
                +popupTextRect.width() / 2,
                +popupTopPadding - popupBottomMargin);
        return r.height();
    }

    private void drawDots(Canvas canvas) {
        Paint bigCirPaint = new Paint();
        bigCirPaint.setAntiAlias(true);
        bigCirPaint.setColor(Color.parseColor("#FF0033"));
        Paint smallCirPaint = new Paint(bigCirPaint);
        smallCirPaint.setColor(Color.parseColor("#FFFFFF"));
        if (drawDotList != null && !drawDotList.isEmpty()) {
            for (Dot dot : drawDotList) {
                int x = dot.x;
                if (mShowYCoordinate == true) x += YCOORD_TEXT_LEFT_MARGIN;
                canvas.drawCircle(x, dot.y, DOT_OUTER_CIR_RADIUS, bigCirPaint);
                canvas.drawCircle(x, dot.y, DOT_INNER_CIR_RADIUS, smallCirPaint);
            }
        }
    }

    private void drawLines(Canvas canvas) {
        Paint linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setColor(Color.parseColor("#FF0033"));
        linePaint.setStrokeWidth(MyUtils.dip2px(getContext(), 2));
        for (int i = 0; i < drawDotList.size() - 1; i++) {
            int x1 = drawDotList.get(i).x;
            int x2 = drawDotList.get(i + 1).x;
            if (mShowYCoordinate == true) {
                x1 += YCOORD_TEXT_LEFT_MARGIN;
                x2 += YCOORD_TEXT_LEFT_MARGIN;
            }
            canvas.drawLine(x1,
                    drawDotList.get(i).y,
                    x2,
                    drawDotList.get(i + 1).y,
                    linePaint);
        }
    }

    private void drawBackgroundLines(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(MyUtils.dip2px(getContext(), 1f));
        paint.setColor(BACKGROUND_LINE_COLOR);

        for (int i = 0; i < xCoordinateList.size(); i++) {
            int x = xCoordinateList.get(i);
            if (mShowYCoordinate == true) x += YCOORD_TEXT_LEFT_MARGIN;
            canvas.drawLine(x, 0, x,
                    mViewHeight - bottomTextTopMargin - bottomTextHeight - bottomTextDescent,
                    paint);
        }

        for (int i = 0; i < yCoordinateList.size(); i++) {
            if ((yCoordinateList.size() - 1 - i) % dataOfAGird == 0) {
                int y = yCoordinateList.get(i);
                canvas.drawLine(0, y, getWidth(), y, paint);

                if (mShowYCoordinate == true)
                    canvas.drawText(String.valueOf(yCoordinateList.size() - i - 1), 0, y, ycoordTextPaint);
            }
        }

        if (bottomTextList != null) {
            for (int i = 0; i < bottomTextList.size(); i++) {
                int x = sideLineLength + backgroundGridWidth * i;
                if (mShowYCoordinate == true) x += YCOORD_TEXT_LEFT_MARGIN;
                canvas.drawText(bottomTextList.get(i), x,
                        mViewHeight - bottomTextDescent, bottomTextPaint);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mViewWidth = measureWidth(widthMeasureSpec);
        mViewHeight = measureHeight(heightMeasureSpec);
        refreshAfterDataChanged();
        setMeasuredDimension(mViewWidth, mViewHeight);
    }

    private int measureWidth(int measureSpec) {
        int horizontalGridNum = getHorizontalGridNum();
        int preferred = backgroundGridWidth * horizontalGridNum + sideLineLength * 2;
        return getMeasurement(measureSpec, preferred);
    }

    private int measureHeight(int measureSpec) {
        int preferred = 0;
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

    private int updateSelf(int origin, int target, int velocity) {
        if (origin < target) {
            origin += velocity;
        } else if (origin > target) {
            origin -= velocity;
        }
        if (Math.abs(target - origin) < velocity) {
            origin = target;
        }
        return origin;
    }

    class Dot {
        int x;
        int y;
        int data;
        int targetX;
        int targetY;
        int velocity = MyUtils.dip2px(getContext(), 20);

        Dot(int x, int y, int targetX, int targetY, Integer data) {
            this.x = x;
            this.y = y;
            setTargetData(targetX, targetY, data);
        }

        Point getPoint() {
            return new Point(x, y);
        }

        Dot setTargetData(int targetX, int targetY, Integer data) {
            this.targetX = targetX;
            this.targetY = targetY;
            this.data = data;
            return this;
        }

        boolean isAtRest() {
            return (x == targetX) && (y == targetY);
        }

        void update() {
            x = updateSelf(x, targetX, velocity);
            y = updateSelf(y, targetY, velocity);
        }
    }
}

