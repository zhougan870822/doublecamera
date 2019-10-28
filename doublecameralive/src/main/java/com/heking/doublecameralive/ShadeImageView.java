package com.heking.doublecameralive;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

/**
 * @Author HK-LJJ
 * @Date 2019/9/25
 * @Description TODO
 */
public class ShadeImageView extends android.support.v7.widget.AppCompatImageView {
    private Paint mPaint;
    public int width = 240;
    public int height = 320;

    private int left;
    private int top;
    private Rect rect=new Rect();

    public ShadeImageView(Context context) {
        super(context);
        initPaint();
    }

    public ShadeImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaint();
    }

    public ShadeImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint();
    }

    private void initPaint() {
        if (mPaint == null) {
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setStrokeWidth(2);
            mPaint.setColor(Color.parseColor("#55555555"));
        }

    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setColor(Color.parseColor("#55555555"));
        mPaint.setStyle(Paint.Style.FILL);
        rect.left=0;
        rect.top=0;
        rect.right=getMeasuredWidth();
        rect.bottom=top;
        canvas.drawRect(rect,mPaint );

        rect.left=0;
        rect.top=getMeasuredHeight()-top;
        rect.right=getMeasuredWidth();
        rect.bottom=getMeasuredHeight();
        canvas.drawRect(rect,mPaint );

        rect.left=0;
        rect.top=top;
        rect.right=left;
        rect.bottom=top+height;
        canvas.drawRect(rect,mPaint );

        rect.left=getMeasuredWidth()-left;
        rect.top=top;
        rect.right=getMeasuredWidth();
        rect.bottom=top+height;
        canvas.drawRect(rect,mPaint );


        mPaint.setStyle(Paint.Style.STROKE);
        rect.left=left;
        rect.top=top;
        rect.right=left+width;
        rect.bottom=top+height;
        mPaint.setColor(Color.BLUE);
        canvas.drawRect(rect,mPaint );
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int w=getMeasuredWidth();
        int h=getMeasuredHeight();
        width=w/3;
        height=h/3;
        int l = (getMeasuredWidth() - width) / 2;
        int t = (getMeasuredHeight() - height) / 2;
        left = l <= 0 ? 0 : l;
        top = t <= 0 ? 0 : t;

    }

    public int getRectLeft() {
        return left;
    }

    public int getRectTop() {
        return top;
    }
}
