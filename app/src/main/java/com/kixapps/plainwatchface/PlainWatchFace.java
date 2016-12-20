package com.kixapps.plainwatchface;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.BatteryManager;
import android.support.v4.content.ContextCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import java.util.Date;

public class PlainWatchFace extends CanvasWatchFaceService {

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    private static final String TIME_FORMAT = "h:mm";
    private static final String DOW_FORMAT = "EEEE";
    private static final String DATE_FORMAT = "MM-d-yyyy";
    private static final int BATTERY_LEVEL_UPDATE_FREQUENCY_SECONDS= 60;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private class Engine extends CanvasWatchFaceService.Engine {

        Calendar calendar;
        Paint timePaint;
        Paint datePaint;
        Paint batteryPaint;
        Paint bgPaint;
        float batteryPct = 0;
        Date lastBatteryUpdate = new Date();

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            calendar = Calendar.getInstance();

            setWatchFaceStyle(new WatchFaceStyle.Builder(PlainWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            bgPaint = new Paint();
            bgPaint.setColor(ContextCompat.getColor(PlainWatchFace.this, R.color.background));

            timePaint = new Paint();
            timePaint.setColor(ContextCompat.getColor(PlainWatchFace.this, R.color.time_text));
            timePaint.setTypeface(NORMAL_TYPEFACE);
            timePaint.setAntiAlias(true);
            timePaint.setTextSize(100);

            datePaint = new Paint();
            datePaint.setColor(ContextCompat.getColor(PlainWatchFace.this, R.color.date_text));
            datePaint.setAntiAlias(true);
            datePaint.setTextSize(20);

            batteryPaint = new Paint();
            batteryPaint.setColor(ContextCompat.getColor(PlainWatchFace.this, R.color.date_text));
            batteryPaint.setStrokeWidth(2);

            updateBatteryLevel();
        }

        @Override
        /* the time changed */
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            calendar.setTimeInMillis(System.currentTimeMillis());

            int oneTenth = bounds.height() / 10;

            canvas.drawRect(0, 0, bounds.width(), bounds.height(), bgPaint);

            SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);
            String timeText = timeFormat.format(calendar);

            Rect timeBounds = new Rect();
            timePaint.getTextBounds(timeText,0,timeText.length(),timeBounds);
            int middleOfText = timeBounds.height() / 4;
            canvas.drawText(timeText, centerTextX(timeText, timePaint, bounds), bounds.exactCenterY() + middleOfText, timePaint);

            SimpleDateFormat dowFormat = new SimpleDateFormat(DOW_FORMAT);
            String dowText = dowFormat.format(calendar);
            canvas.drawText(dowText, centerTextX(dowText, datePaint, bounds), bounds.exactCenterY() - (oneTenth * 4), datePaint);

            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            String dateText = dateFormat.format(calendar);
            canvas.drawText(dateText, centerTextX(dateText, datePaint, bounds), bounds.exactCenterY() + (oneTenth * 4), datePaint);

            if (shouldUpdateBattery()) {
                updateBatteryLevel();
            }
            float batteryLineLength = bounds.width() * batteryPct;
            canvas.drawLine(bounds.left, bounds.top, batteryLineLength, bounds.top, batteryPaint);
        }

        private float centerTextX(String text, Paint paint, Rect bounds) {
            float centerOfText = paint.measureText(text) / 2.0f;
            return bounds.exactCenterX() - centerOfText;
        }

        private boolean shouldUpdateBattery(){
            long millisecondsSinceLastUpdate = System.currentTimeMillis() - lastBatteryUpdate.getTime();
            long minutesSinceLastUpdate = millisecondsSinceLastUpdate / 1000;
            return minutesSinceLastUpdate >= BATTERY_LEVEL_UPDATE_FREQUENCY_SECONDS;
        }

        private void updateBatteryLevel(){
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = PlainWatchFace.this.registerReceiver(null, ifilter);
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryPct = level / (float) scale;
            lastBatteryUpdate.setTime(System.currentTimeMillis());
        }

    }
}
