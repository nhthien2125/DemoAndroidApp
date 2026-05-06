package com.neith.subjectdemo.fn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DonutChartView extends View {

    private Paint paint;
    private RectF rectF;
    private List<Float> angles = new ArrayList<>();
    private List<Integer> colors = new ArrayList<>();

    public DonutChartView(Context context) {
        super(context);
        init();
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(80); // Độ dày của Donut
        rectF = new RectF();
    }

    public void setData(Map<String, Double> data) {
        angles.clear();
        colors.clear();

        int[] colorPalette = {
                Color.parseColor("#BD5E00"), // Tư vấn
                Color.parseColor("#3B82F6"), // Xây dựng
                Color.parseColor("#EC4899"), // Bảo trì
                Color.parseColor("#EAB308"), // Cải tạo
                Color.parseColor("#06B6D4"), // Vệ sinh
                Color.parseColor("#10B981")  // Bảo dưỡng
        };

        double total = 0;
        for (Double value : data.values()) {
            total += value;
        }

        int i = 0;
        for (Double value : data.values()) {
            angles.add((float) (value / total * 360));
            colors.add(colorPalette[i % colorPalette.length]);
            i++;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float padding = paint.getStrokeWidth() / 2 + 20;
        rectF.set(padding, padding, getWidth() - padding, getHeight() - padding);

        float startAngle = -90;
        for (int i = 0; i < angles.size(); i++) {
            paint.setColor(colors.get(i));
            canvas.drawArc(rectF, startAngle, angles.get(i), false, paint);
            startAngle += angles.get(i);
        }

        if (angles.isEmpty()) {
            paint.setColor(Color.DKGRAY);
            canvas.drawArc(rectF, 0, 360, false, paint);
        }
    }
}
