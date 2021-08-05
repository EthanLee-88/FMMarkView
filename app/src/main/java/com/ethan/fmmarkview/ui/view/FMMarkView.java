package com.ethan.fmmarkview.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Scroller;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;

/**
 * FM刻度尺
 *
 * EthanLee
 */
public class FMMarkView extends View {
    private static final String TAG = "FMMarkView";
    // 画刻度线
    private Paint linePaint;
    // 画中间指示线
    private Paint guideLinePaint;
    // 画刻度值
    private Paint numberPaint;
    // 每刻度间隔
    private static int defaultMark = 6;
    // 总刻度数
    private static int markCount = 210;
    // 短刻度线长度
    private static int shortLineLength = 16;
    // 长刻度线长度
    private static int longLineLength = 32;
    // 所有刻度总长度（+2，前后各留一个间隙）
    private static int contentTotalLength;
    // 刻度值保留一位小数
    private DecimalFormat numFormat = new DecimalFormat("0.0");
    // 上一次滑动事件x值
    private float lastX;
    // 内容滑动的左边界
    private int leftBorder = 0;
    // 内容滑动的右边界
    private int rightBorder = 0;
    // 中心三角形指针
    private Path guidePath;
    // 刻度线及刻度值颜色
    private int markLineColor = Color.parseColor("#FF000000");
    // 三角形指针颜色
    private int guideLineColor = Color.parseColor("#FFF10404");

    private OnFMChangeListener mOnFMChangeListener;

    private VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    private Scroller mScroller = new Scroller(getContext());

    public FMMarkView(Context context) {
        this(context, null);
    }

    public FMMarkView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FMMarkView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        linePaint = new Paint();
        linePaint.setColor(markLineColor);
        linePaint.setAntiAlias(true);
        linePaint.setDither(true);
        linePaint.setStrokeWidth(3);

        guideLinePaint = new Paint();
        guideLinePaint.setColor(guideLineColor);
        guideLinePaint.setAntiAlias(true);
        guideLinePaint.setDither(true);
        guideLinePaint.setStrokeWidth(3);

        numberPaint = new Paint();
        numberPaint.setColor(markLineColor);
        numberPaint.setAntiAlias(true);
        numberPaint.setDither(true);
        numberPaint.setTextSize(dipToPx(22));
        numberPaint.setStrokeWidth(1);

        contentTotalLength = (markCount + 2) * defaultMark;
    }

    /**
     * 刻度及刻度值颜色
     *
     * @param color
     */
    public void setMarkLineColor(@ColorRes int color) {
        this.markLineColor = getResources().getColor(color);
        if (linePaint != null)
            linePaint.setColor(markLineColor);
    }

    /**
     * 指针及当前刻度值颜色
     *
     * @param color
     */
    public void setGuideLineColor(@ColorRes int color) {
        this.guideLineColor = getResources().getColor(color);
        if (guideLinePaint != null)
            guideLinePaint.setColor(guideLineColor);
    }

    /**
     * 设置频段
     *
     * @param mHZ 频段
     */
    public void setFM(float mHZ) {
        if ((mHZ < 87) || (mHZ > 108)) return;
        double destMarks = (mHZ - 87) * 10;
        int currentMaks = calculateCurrentMarks(null);
        Log.d(TAG, "destMarks = " + destMarks + "--currentMaks = " + currentMaks);
        scrollBy((int) ((destMarks - currentMaks) * dipToPx(defaultMark)), 0);
    }

    /**
     * 获取当前频段
     *
     * @return 当前频段
     */
    public double getFM() {
        int currentMaks = calculateCurrentMarks(null);
        double currentFM = 87 + currentMaks * 1.0 / 10;
        Log.d(TAG, "currentFM = " + currentFM);
        return currentFM;
    }

    public interface OnFMChangeListener {
        void onChang(double currentFM);
    }

    public void setOnFMChangeListener(OnFMChangeListener onFMChangeListener) {
        this.mOnFMChangeListener = onFMChangeListener;
    }

    /**
     * 回调当前FM
     */
    private void onFMChange() {
        if (this.mOnFMChangeListener == null) return;
        this.mOnFMChangeListener.onChang(getFM());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (heightMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSize, (int) dipToPx(120));
            return;
        }
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // 初始化内容滑动的左右边界
        getLeftBorder();
        getRightBorder();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 画底部直线
        float baseStartX = dipToPx(defaultMark);
        float baseStartY = getHeight() - dipToPx(defaultMark);
        float baseEndX = dipToPx(contentTotalLength - defaultMark);
        canvas.drawLine(baseStartX, baseStartY, baseEndX, baseStartY, linePaint);
        // 画所有刻度线
        for (int i = 0; i <= markCount; i++) {
            float markStartX = dipToPx(defaultMark * (i + 1));
            float markStarY = getHeight() - dipToPx(defaultMark);
            float markEndX = dipToPx(defaultMark * (i + 1));
            float markEndY;
            if (i % 10 == 0) {
                markEndY = getHeight() - dipToPx(defaultMark + longLineLength);// 长刻度
                drawNumbers(canvas, i);
            } else {
                markEndY = getHeight() - dipToPx(defaultMark + shortLineLength);
            }
            canvas.drawLine(markStartX, markStarY, markEndX, markEndY, linePaint);
        }
        // 画中心三角形指针
        Path guidePath = getGuidePath();
        canvas.drawPath(guidePath, guideLinePaint);
        // 画当前刻度值
        drawCurrentNumber(canvas);
    }

    /**
     * @return 三角形指针
     */
    private Path getGuidePath() {
        if (guidePath == null) guidePath = new Path();
        guidePath.reset();
        float centerX = getWidth() / 2 + getScrollX();
        float bottomY = getHeight() - dipToPx(defaultMark);
        float topY = getHeight() - dipToPx(defaultMark + longLineLength * 2);

        guidePath.moveTo(centerX, bottomY);
        guidePath.lineTo(centerX - dipToPx(3), topY);
        guidePath.lineTo(centerX + dipToPx(3), topY);
        guidePath.close();
        return guidePath;
    }

    /**
     * 画当前刻度值
     *
     * @param canvas
     */
    private void drawCurrentNumber(Canvas canvas) {
        float centerX = getWidth() / 2 + getScrollX();
        float guideLineTopY = getHeight() - dipToPx(defaultMark + longLineLength * 2);

        int currentMaks = calculateCurrentMarks(null);
        // 从 87MHZ开始，0.1每刻度
        double contentNum = 87 + currentMaks * 1.0 / 10;
        String currentNumber = numFormat.format(contentNum);

        Log.d(TAG, "currentNumber = " + currentNumber + "--" + currentMaks + "--contentNum = " + contentNum);
        Rect textRect = getTextRect(numberPaint, currentNumber);
        float textWidth = textRect.width();
        float textHeight = textRect.height();

        int x = (int) (centerX - textWidth / 2);
        int baseY = (int) (guideLineTopY - textHeight * 3 / 4);

        numberPaint.setFakeBoldText(true);
        numberPaint.setTextSize(dipToPx(25));
        numberPaint.setColor(guideLineColor);
        canvas.drawText(currentNumber, x, baseY, numberPaint);

        numberPaint.setTextSize(dipToPx(12));
        int unitX = (int) (centerX + textWidth * 3 / 4);
        canvas.drawText("MHZ", unitX, baseY, numberPaint);
        // 回调当前FM
        onFMChange();
    }

    /**
     * 画刻度值
     *
     * @param canvas
     * @param number 长刻度线的位置
     */
    private void drawNumbers(Canvas canvas, int number) {
        String text = String.valueOf(number / 10 + 87);
        Rect textRect = getTextRect(numberPaint, text);
        float textWidth = textRect.width();
        float textHeight = textRect.height();
        numberPaint.setFakeBoldText(false);
        numberPaint.setTextSize(dipToPx(22));
        numberPaint.setColor(markLineColor);
        canvas.drawText(text, dipToPx(defaultMark * (number + 1)) - textWidth / 2,
                getHeight() - dipToPx(defaultMark + longLineLength) - textHeight / 2, numberPaint);
    }

    private Rect getTextRect(Paint textPaint, String text) {
        Rect rect = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), rect);
        return rect;
    }

    private int getLeftBorder() {
        leftBorder = (int) (dipToPx(defaultMark) - getWidth() / 2);
        return leftBorder;
    }

    private int getRightBorder() {
        rightBorder = (int) (dipToPx((markCount + 1) * defaultMark) - getWidth() / 2);
        return rightBorder;
    }

    /**
     * 开始处理滑动事件
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mVelocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = (int) (lastX - event.getX());
                Log.d(TAG, "deltaX = " + deltaX + "--getScrollX() = " + getScrollX());
                // 限制左边界
                if ((getScrollX() <= leftBorder) && (deltaX < 0)) {
                    scrollTo(leftBorder, 0);
                    break;
                }
                // 限制有边界
                if ((getScrollX() >= rightBorder) && (deltaX > 0)) {
                    scrollTo(rightBorder, 0);
                    break;
                }
                // 界内滑动
                scrollBy(deltaX, 0);
                lastX = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                // 计算滑动速度
                computeVelocity();
                break;
        }
        calculateCurrentMarks(event);
        return true;
    }

    /**
     * 计算滑动速度
     */
    private void computeVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000);
        float velocityX = mVelocityTracker.getXVelocity();
        Log.d(TAG, "velocityX = " + velocityX);
        // 初始化 Scroller
        setFling((int) velocityX);
    }

    private void setFling(int vx) {
        fling(getScrollX(), 0, -vx, 0, leftBorder, rightBorder, 0, 0);
    }

    /**
     * @param startX    起始 X
     * @param startY    起始 Y
     * @param velocityX X 方向速度
     * @param velocityY Y 方向速度
     * @param minX      左边界
     * @param maxX      右边界
     * @param minY      上边界
     * @param maxY      下边界
     */
    private void fling(int startX, int startY, int velocityX, int velocityY,
                       int minX, int maxX, int minY, int maxY) {
        if (mScroller == null) return;
        mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
    }

    /**
     * Scroll 回调
     */
    @Override
    public void computeScroll() {
        Log.d(TAG, "computeScroll = " + mScroller.isFinished());
        if (mScroller == null) return;
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), 0);
        } else {
            MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0, 0, 0);
            calculateCurrentMarks(event);
        }
    }

    /**
     * 计算当前刻度
     *
     * @param event
     * @return
     */
    private int calculateCurrentMarks(MotionEvent event) {
        float guideLineX = getWidth() / 2;
        float contentX = getScrollX() + guideLineX - dipToPx(defaultMark);
        int marks = (int) (contentX / dipToPx(defaultMark));
        if (contentX % dipToPx(defaultMark) > dipToPx(defaultMark / 2)) {
            marks += 1;
            if ((event != null) && (event.getAction() == MotionEvent.ACTION_UP)) {
                scrollBy((int) (dipToPx(defaultMark) - (contentX % dipToPx(defaultMark))), 0); //五入,ACTION_UP时跳到刻度线
            }
        } else {
            if ((event != null) && (event.getAction() == MotionEvent.ACTION_UP)) {
                scrollBy((int) (-contentX % dipToPx(defaultMark)), 0); // 四舍,ACTION_UP时跳到刻度线
            }
        }
        Log.d(TAG, "marks = " + marks);
        return marks;
    }

    private float dipToPx(int dip) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
    }
}
