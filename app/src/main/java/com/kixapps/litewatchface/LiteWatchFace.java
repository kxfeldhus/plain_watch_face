package com.kixapps.litewatchface;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.*;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.BatteryManager;
import android.support.v4.content.ContextCompat;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.util.Date;

public class LiteWatchFace extends CanvasWatchFaceService {

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    private static final String TIME_FORMAT = "h:mm";
    private static final String DOW_FORMAT = "EEEE";
    private static final String DATE_FORMAT = "MM-dd-yyyy";
    private static final int BATTERY_LEVEL_UPDATE_FREQUENCY_SECONDS= 60;

    private static final int LEFT_COMPLICATION_ID = 0;
    private static final int RIGHT_COMPLICATION_ID = 1;

    private static final int[] COMPLICATION_IDS = {LEFT_COMPLICATION_ID, RIGHT_COMPLICATION_ID};

    // Left and right dial supported types.
    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_LONG_TEXT,
                    ComplicationData.TYPE_LARGE_IMAGE
            },
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_LONG_TEXT,
                    ComplicationData.TYPE_LARGE_IMAGE
            }
    };

    private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;
    private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;

    static int getComplicationId(ConfigActivity.ComplicationLocation complicationLocation) {
        switch (complicationLocation) {
            case LEFT:
                return LEFT_COMPLICATION_ID;
            case RIGHT:
                return RIGHT_COMPLICATION_ID;
            default:
                return -1;
        }
    }

    static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    static int[] getSupportedComplicationTypes(
            ConfigActivity.ComplicationLocation complicationLocation) {
        switch (complicationLocation) {
            case LEFT:
                return COMPLICATION_SUPPORTED_TYPES[0];
            case RIGHT:
                return COMPLICATION_SUPPORTED_TYPES[1];
            default:
                return new int[] {};
        }
    }

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

            initializeComplications();

            setWatchFaceStyle(new WatchFaceStyle.Builder(LiteWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setStatusBarGravity(Gravity.LEFT | Gravity.TOP)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

            bgPaint = new Paint();
            bgPaint.setColor(ContextCompat.getColor(LiteWatchFace.this, R.color.background));

            timePaint = new Paint();
            timePaint.setColor(ContextCompat.getColor(LiteWatchFace.this, R.color.time_text));
            timePaint.setTypeface(NORMAL_TYPEFACE);
            timePaint.setAntiAlias(true);
            timePaint.setTextSize(100);

            datePaint = new Paint();
            datePaint.setColor(ContextCompat.getColor(LiteWatchFace.this, R.color.date_text));
            datePaint.setAntiAlias(true);
            datePaint.setTextSize(20);

            batteryPaint = new Paint();
            batteryPaint.setColor(ContextCompat.getColor(LiteWatchFace.this, R.color.date_text));
            batteryPaint.setAntiAlias(true);
            batteryPaint.setStrokeWidth(2);

            updateBatteryLevel();
        }

        @Override
        /* the time changed */
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        boolean mIsRound = true;

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            mIsRound = insets.isRound();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            calendar.setTimeInMillis(now);

            int oneTenth = bounds.height() / 10;

            canvas.drawRect(0, 0, bounds.width(), bounds.height(), bgPaint);

            SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);
            String timeText = timeFormat.format(calendar);

            Rect timeBounds = new Rect();
            timePaint.getTextBounds(timeText,0,timeText.length(),timeBounds);
            int middleOfText = timeBounds.height() / 4;
            canvas.drawText(timeText, centerTextX(timeText, timePaint, bounds), bounds.exactCenterY() - 30 + middleOfText, timePaint);

            SimpleDateFormat dowFormat = new SimpleDateFormat(DOW_FORMAT);
            String dowText = dowFormat.format(calendar);
            canvas.drawText(dowText, centerTextX(dowText, datePaint, bounds), bounds.exactCenterY() - 40 - (oneTenth * 2), datePaint);

            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            String dateText = dateFormat.format(calendar);
            canvas.drawText(dateText, centerTextX(dateText, datePaint, bounds), bounds.exactCenterY() - 10 + oneTenth, datePaint);

            if (shouldUpdateBattery()) {
                updateBatteryLevel();
            }
            if (mIsRound) {
                Path mPath = new Path();
                Float startingAngle = 225f;
                Float batteryLineMaxLength = 90f;
                Float batteryLineLength = batteryLineMaxLength * batteryPct;
                mPath.arcTo(bounds.left, bounds.top, bounds.right, bounds.bottom, startingAngle, batteryLineLength, false);
                batteryPaint.setStrokeCap(Paint.Cap.ROUND);
                batteryPaint.setStrokeJoin(Paint.Join.ROUND);
                batteryPaint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(mPath, batteryPaint);
            } else {
                float batteryLineLength = bounds.width() * batteryPct;
                canvas.drawLine(bounds.left, bounds.top, batteryLineLength, bounds.top, batteryPaint);
            }

            drawComplications(canvas, now);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            // For most Wear devices, width and height are the same, so we just chose one (width).
            // TODO: Step 2, calculating ComplicationDrawable locations
            int sizeOfComplication = width / 4;
            int midpointOfScreen = width / 2;

            int horizontalOffset = (midpointOfScreen - sizeOfComplication) / 2;
            int verticalOffset = midpointOfScreen - (sizeOfComplication / 2) + 100;

            Rect leftBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            horizontalOffset,
                            verticalOffset,
                            (horizontalOffset + sizeOfComplication),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable leftComplicationDrawable =
                    mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID);
            leftComplicationDrawable.setBounds(leftBounds);

            Rect rightBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (midpointOfScreen + horizontalOffset),
                            verticalOffset,
                            (midpointOfScreen + horizontalOffset + sizeOfComplication),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable rightComplicationDrawable =
                    mComplicationDrawableSparseArray.get(RIGHT_COMPLICATION_ID);
            rightComplicationDrawable.setBounds(rightBounds);
        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            // TODO: Step 4, drawComplications()
            int complicationId;
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationId = COMPLICATION_IDS[i];
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);

                complicationDrawable.draw(canvas, currentTimeMillis);
            }
        }

        @Override
        public void onComplicationDataUpdate(int complicationId, ComplicationData complicationData) {
            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);

            // Updates correct ComplicationDrawable with updated data.
            ComplicationDrawable complicationDrawable =
                    mComplicationDrawableSparseArray.get(complicationId);
            complicationDrawable.setComplicationData(complicationData);

            invalidate();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            // TODO: Step 5, OnTapCommand()
            switch (tapType) {
                case TAP_TYPE_TAP:
                    int tappedComplicationId = getTappedComplicationId(x, y);
                    if (tappedComplicationId != -1) {
                        onComplicationTap(tappedComplicationId);
                    }
                    break;
            }
        }

        private void onComplicationTap(int complicationId) {
            // TODO: Step 5, onComplicationTap()

            ComplicationData complicationData =
                    mActiveComplicationDataSparseArray.get(complicationId);

            if (complicationData != null) {

                if (complicationData.getTapAction() != null) {
                    try {
                        complicationData.getTapAction().send();
                    } catch (PendingIntent.CanceledException e) {
                    }

                } else if (complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {

                    // Watch face does not have permission to receive complication data, so launch
                    // permission request.
                    ComponentName componentName = new ComponentName(
                            getApplicationContext(),
                            LiteWatchFace.class);

                    Intent permissionRequestIntent =
                            ComplicationHelperActivity.createPermissionRequestHelperIntent(
                                    getApplicationContext(), componentName);

                    startActivity(permissionRequestIntent);
                }

            } else {
            }
        }

        /*
      * Determines if tap inside a complication area or returns -1.
      */
        private int getTappedComplicationId(int x, int y) {

            int complicationId;
            ComplicationData complicationData;
            ComplicationDrawable complicationDrawable;

            long currentTimeMillis = System.currentTimeMillis();

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationId = COMPLICATION_IDS[i];
                complicationData = mActiveComplicationDataSparseArray.get(complicationId);

                if ((complicationData != null)
                        && (complicationData.isActive(currentTimeMillis))
                        && (complicationData.getType() != ComplicationData.TYPE_NOT_CONFIGURED)
                        && (complicationData.getType() != ComplicationData.TYPE_EMPTY)) {

                    complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
                    Rect complicationBoundingRect = complicationDrawable.getBounds();

                    if (complicationBoundingRect.width() > 0) {
                        if (complicationBoundingRect.contains(x, y)) {
                            return complicationId;
                        }
                    } else {
                    }
                }
            }
            return -1;
        }

        private void initializeComplications() {
            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            ComplicationDrawable leftComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            leftComplicationDrawable.setContext(getApplicationContext());

            ComplicationDrawable rightComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            rightComplicationDrawable.setContext(getApplicationContext());

            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
            mComplicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable);
            mComplicationDrawableSparseArray.put(RIGHT_COMPLICATION_ID, rightComplicationDrawable);

            setActiveComplications(COMPLICATION_IDS);
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
            Intent batteryStatus = LiteWatchFace.this.registerReceiver(null, ifilter);
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryPct = level / (float) scale;
            lastBatteryUpdate.setTime(System.currentTimeMillis());
        }

    }
}
