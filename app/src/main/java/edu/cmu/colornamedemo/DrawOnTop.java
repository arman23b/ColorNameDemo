package edu.cmu.colornamedemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.view.View;

public class DrawOnTop extends View {

    private int x;
    private int y;
    private int radius;

    public DrawOnTop(Context context) {
        super(context);
    }

    public void setValues(int x, int y, int radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        DashPathEffect dashPath = new DashPathEffect(new float[]{5,5}, (float)1.0);
        p.setPathEffect(dashPath);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(10);
        canvas.drawCircle(x, y, radius, p);
        invalidate();
        super.onDraw(canvas);
    }


}
