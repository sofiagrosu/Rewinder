package com.example.myapplication1.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.myapplication1.R;

/**
 * Simple bar chart: one bar per value, a label under each bar and the value above it.
 * Used for "Întrebări pe zi" on the Stats screen.
 *
 *     chart.setData(new int[]{12, 17, 9}, new String[]{"L", "M", "M"});
 */
public class BarChartView extends View {

    private int[] values = new int[0];
    private String[] labels = new String[0];

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public BarChartView(Context context) {
        super(context);
        init(context);
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        barPaint.setColor(ContextCompat.getColor(context, R.color.primary));
        stubPaint.setColor(ContextCompat.getColor(context, R.color.track));

        labelPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(sp(13));

        valuePaint.setColor(ContextCompat.getColor(context, R.color.text_primary));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTextSize(sp(12));
        valuePaint.setFakeBoldText(true);
    }

    public void setData(int[] values, String[] labels) {
        this.values = values;
        this.labels = labels;

        // for screen readers: "L: 12, M: 17, ..."
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < values.length && i < labels.length; i++) {
            if (i > 0) description.append(", ");
            description.append(labels[i]).append(": ").append(values[i]);
        }
        setContentDescription(description.toString());
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = values.length;
        if (n == 0) return;

        float width = getWidth();
        float height = getHeight();
        float valueAreaHeight = dp(18);
        float labelAreaHeight = dp(26);
        float chartTop = valueAreaHeight;
        float chartBottom = height - labelAreaHeight;
        float chartHeight = chartBottom - chartTop;

        int max = 1;
        for (int v : values) max = Math.max(max, v);

        float slot = width / n;
        float barWidth = slot * 0.62f;

        for (int i = 0; i < n; i++) {
            float centerX = slot * i + slot / 2f;
            float left = centerX - barWidth / 2f;
            float right = centerX + barWidth / 2f;

            if (values[i] > 0) {
                float barHeight = chartHeight * values[i] / max;
                float radius = Math.min(dp(8), Math.min(barHeight / 2f, barWidth / 2f));
                rect.set(left, chartBottom - barHeight, right, chartBottom);
                canvas.drawRoundRect(rect, radius, radius, barPaint);
                canvas.drawText(String.valueOf(values[i]), centerX,
                        chartBottom - barHeight - dp(4), valuePaint);
            } else {
                // no answers that day: a small grey stub so the day is still visible
                rect.set(left, chartBottom - dp(4), right, chartBottom);
                canvas.drawRoundRect(rect, dp(2), dp(2), stubPaint);
            }

            if (i < labels.length) {
                canvas.drawText(labels[i], centerX, height - dp(6), labelPaint);
            }
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }
}

