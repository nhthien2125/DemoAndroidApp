package com.neith.subjectdemo.fn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CombinedChartView extends View {

    private Paint barPaint, linePaint, axisPaint, textPaint;
    private Map<String, Double> monthlyRevenue;
    private List<Double> marketIndices;

    public CombinedChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#3B82F6")); // Blue bar

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#EAB308")); // Yellow line
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5f);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.GRAY);
        axisPaint.setStrokeWidth(2f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.GRAY);
        textPaint.setTextSize(24f);
    }

    public void setData(Map<String, Double> revenue, List<Double> market) {
        this.monthlyRevenue = revenue;
        this.marketIndices = market;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (monthlyRevenue == null || monthlyRevenue.isEmpty()) return;

        float width = getWidth();
        float height = getHeight() - 60; // Trừa chỗ cho label X
        float padding = 80;
        float chartWidth = width - padding * 2;
        float chartHeight = height - padding;

        List<Double> revValues = new ArrayList<>(monthlyRevenue.values());
        List<String> months = new ArrayList<>(monthlyRevenue.keySet());
        
        double maxRev = 50.0; // Tỉ lệ theo ảnh (50000M)
        for (Double v : revValues) if (v > maxRev) maxRev = v;
        
        float stepX = chartWidth / (months.size() - 1);
        float barWidth = 30f;

        // Vẽ cột (Revenue)
        for (int i = 0; i < revValues.size(); i++) {
            float x = padding + i * stepX;
            float barH = (float) (revValues.get(i) / maxRev * chartHeight);
            canvas.drawRect(x - barWidth/2, height - barH, x + barWidth/2, height, barPaint);
            
            // Label tháng
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(months.get(i), x, height + 40, textPaint);
        }

        // Vẽ đường (Market Index)
        if (marketIndices != null && !marketIndices.isEmpty()) {
            Path path = new Path();
            float stepLine = chartWidth / (marketIndices.size() - 1);
            for (int i = 0; i < marketIndices.size(); i++) {
                float x = padding + i * stepLine;
                float y = (float) (height - (marketIndices.get(i) / 2.0 * chartHeight)); // Giả định max index = 2.0
                if (i == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
                canvas.drawCircle(x, y, 8f, linePaint);
            }
            canvas.drawPath(path, linePaint);
        }
    }
}
