/*
 * Copyright (C) 2012 EaseTheWorld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * https://github.com/EaseTheWorld/CalendarView
 */

package com.easetheworld.calendarviewtest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

// this is from android.widget.CalendarView source

/**
 * This class is a calendar widget for displaying and selecting dates. The range
 * of dates supported by this calendar is configurable. A user can select a date
 * by taping on it and can scroll and fling the calendar to a desired date.
 */
public class CalendarView extends FrameLayout {

    /**
     * Tag for logging.
     */
    private static final String LOG_TAG = CalendarView.class.getSimpleName();

    /**
     * Default value whether to show week number.
     */
    private static final boolean DEFAULT_SHOW_WEEK_NUMBER = true;

    /**
     * The number of milliseconds in a day.e
     */
    private static final long MILLIS_IN_DAY = 86400000L;

    /**
     * The number of day in a week.
     */
    private static final int DAYS_PER_WEEK = 7;

    /**
     * The number of milliseconds in a week.
     */
    private static final long MILLIS_IN_WEEK = DAYS_PER_WEEK * MILLIS_IN_DAY;

    /**
     * String for parsing dates.
     */
    private static final String DATE_FORMAT = "MM/dd/yyyy";

    /**
     * The default minimal date.
     */
    private static final String DEFAULT_MIN_DATE = "01/01/1900";

    /**
     * The default maximal date.
     */
    private static final String DEFAULT_MAX_DATE = "01/01/2100";

    private static final int DEFAULT_SHOWN_WEEK_COUNT = 6;

    private static final int DEFAULT_DATE_TEXT_SIZE = 14;

    private static final int UNSCALED_WEEK_SEPARATOR_LINE_WIDTH = 1;

    private static final int DEFAULT_WEEK_DAY_TEXT_APPEARANCE_RES_ID = -1;
    

    private final int mWeekSeperatorLineWidth;

    private int mDateTextSize;
    
    private int mSelectedMonthDateColor;

    private int mWeekSeparatorLineColor;

    private int mWeekNumberColor;

    private int mWeekDayTextAppearanceResId;

    private Paint mYearPaint;
    private Paint mMonthPaint;
    private Paint mWeekdayPaint;
    private Paint mSaturdayPaint;
    private Paint mSundayPaint;

    /**
     * The number of shown weeks.
     */
    private int mShownWeekCount;

    /**
     * Flag whether to show the week number.
     */
    private boolean mShowWeekNumber;

    /**
     * The number of day per week to be shown.
     */
    private int mDaysPerWeek = 7;

    /**
     * The friction of the week list while flinging.
     */
    private float mFriction = .05f;

    /**
     * Scale for adjusting velocity of the week list while flinging.
     */
    private float mVelocityScale = 0.333f;
    
    private LinearLayout mContentView;

    /**
     * The adapter for the weeks list.
     */
    private WeeksAdapter mAdapter;

    /**
     * The weeks list.
     */
    private WeeksListView mListView;

    /**
     * The header with week day names.
     */
    private ViewGroup mDayNamesHeader;

    /**
     * Cached labels for the week names header.
     */
    private String[] mDayLabels;

    /**
     * The first day of the week.
     */
    private int mFirstDayOfWeek;

    /**
     * Listener for changes in the selected day.
     */
    private OnDateChangeListener mOnDateChangeListener;
    
    /**
     * Command for adjusting the position after a scroll/fling.
     */
    private AdjustScrollRunnable mAdjustScrollRunnable = new AdjustScrollRunnable();

    /**
     * Temporary instance to avoid multiple instantiations.
     */
    private Calendar mTempDate;

    /**
     * The start date of the range supported by this picker.
     */
    private Calendar mMinDate;

    /**
     * The end date of the range supported by this picker.
     */
    private Calendar mMaxDate;

    /**
     * Date format for parsing dates.
     */
    private final java.text.DateFormat mDateFormat = new SimpleDateFormat(DATE_FORMAT);

    /**
     * The current locale.
     */
    private Locale mCurrentLocale;

    /**
     * The callback used to indicate the user changes the date.
     */
    public interface OnDateChangeListener {

        /**
         * Called upon change of the selected day.
         *
         * @param view The view associated with this listener.
         * @param year The year that was set.
         * @param month The month that was set [0-11].
         * @param dayOfMonth The day of the month that was set.
         */
        public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth);
    }

    public CalendarView(Context context) {
        this(context, null);
    }

    public CalendarView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.calendarViewStyle);
    }

    public CalendarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // initialization based on locale
        setCurrentLocale(Locale.getDefault());

        TypedArray attributesArray = context.obtainStyledAttributes(attrs, R.styleable.CalendarView, defStyle, 0);
        mShowWeekNumber = attributesArray.getBoolean(R.styleable.CalendarView_showWeekNumber,
                DEFAULT_SHOW_WEEK_NUMBER);
        mFirstDayOfWeek = attributesArray.getInt(R.styleable.CalendarView_firstDayOfWeek, mTempDate.getFirstDayOfWeek());//LocaleData.get(Locale.getDefault()).firstDayOfWeek);
        String minDate = attributesArray.getString(R.styleable.CalendarView_minDate);
        if (TextUtils.isEmpty(minDate) || !parseDate(minDate, mMinDate)) {
            parseDate(DEFAULT_MIN_DATE, mMinDate);
        }
        String maxDate = attributesArray.getString(R.styleable.CalendarView_maxDate);
        if (TextUtils.isEmpty(maxDate) || !parseDate(maxDate, mMaxDate)) {
            parseDate(DEFAULT_MAX_DATE, mMaxDate);
        }
        if (mMaxDate.before(mMinDate)) {
            throw new IllegalArgumentException("Max date cannot be before min date.");
        }
        mShownWeekCount = attributesArray.getInt(R.styleable.CalendarView_shownWeekCount, DEFAULT_SHOWN_WEEK_COUNT);
        
        mSelectedMonthDateColor = attributesArray.getColor(R.styleable.CalendarView_selectedMonthDateColor, 0);
        
        int weekdayColor = attributesArray.getColor(R.styleable.CalendarView_dateColor, Color.BLACK);
        
        // easetheworld : Saturday, Sunday highlight
        int saturdayColor = attributesArray.getColor(R.styleable.CalendarView_dateSaturdayColor, Color.BLUE);
        int sundayColor = attributesArray.getColor(R.styleable.CalendarView_dateSundayColor, Color.RED);
        
        int yearColor = attributesArray.getColor(R.styleable.CalendarView_yearColor, 0xff80ff80);
        int monthColor = attributesArray.getColor(R.styleable.CalendarView_monthColor, Color.BLACK);
        
        // easetheworld : set date text size
        mDateTextSize = attributesArray.getDimensionPixelSize(R.styleable.CalendarView_dateTextSize, DEFAULT_DATE_TEXT_SIZE);
       
    	// easetheworld : set paints for year, month, day
        mYearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mYearPaint.setTextAlign(Paint.Align.CENTER);
		mYearPaint.setColor(yearColor);
		
        mMonthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMonthPaint.setTextAlign(Align.CENTER);
		mMonthPaint.setColor(monthColor);
		
        mWeekdayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWeekdayPaint.setTextAlign(Align.CENTER);
        mWeekdayPaint.setFakeBoldText(true);
        mWeekdayPaint.setStyle(Style.FILL);
        mWeekdayPaint.setTextSize(mDateTextSize);
		mWeekdayPaint.setColor(weekdayColor);
		
		mSaturdayPaint = new Paint(mWeekdayPaint);
		mSaturdayPaint.setColor(saturdayColor);
		
		mSundayPaint = new Paint(mWeekdayPaint);
		mSundayPaint.setColor(sundayColor);
        
        mWeekSeparatorLineColor = attributesArray.getColor(
                R.styleable.CalendarView_weekSeparatorLineColor, 0);
        mWeekNumberColor = attributesArray.getColor(R.styleable.CalendarView_weekNumberColor, 0);

        mWeekDayTextAppearanceResId = attributesArray.getResourceId(
                R.styleable.CalendarView_weekDayTextAppearance,
                DEFAULT_WEEK_DAY_TEXT_APPEARANCE_RES_ID);
        
        attributesArray.recycle();

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        mWeekSeperatorLineWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                UNSCALED_WEEK_SEPARATOR_LINE_WIDTH, displayMetrics);

        LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        mContentView = (LinearLayout)layoutInflater.inflate(R.layout.calendar_view, null, false);
        addView(mContentView);

        setUpHeader();
        setUpListView();
        setUpAdapter();

        // go to today or whichever is close to today min or max date
        mTempDate.setTimeInMillis(System.currentTimeMillis());
        if (mTempDate.before(mMinDate)) {
            goTo(mMinDate, false, true, true);
        } else if (mMaxDate.before(mTempDate)) {
            goTo(mMaxDate, false, true, true);
        } else {
            goTo(mTempDate, false, true, true);
        }

        invalidate();
    }

    /**
     * Sets the number of weeks to be shown.
     *
     * @param count The shown week count.
     *
     * @attr ref android.R.styleable#CalendarView_shownWeekCount
     */
    public void setShownWeekCount(int count) {
        if (mShownWeekCount != count) {
            mShownWeekCount = count;
            invalidate();
        }
    }

    /**
     * Gets the number of weeks to be shown.
     *
     * @return The shown week count.
     *
     * @attr ref android.R.styleable#CalendarView_shownWeekCount
     */
    public int getShownWeekCount() {
        return mShownWeekCount;
    }

    /**
     * Sets the color for the week numbers.
     *
     * @param color The week number color.
     *
     * @attr ref android.R.styleable#CalendarView_weekNumberColor
     */
    public void setWeekNumberColor(int color) {
        if (mWeekNumberColor != color) {
            mWeekNumberColor = color;
            if (mShowWeekNumber) {
                invalidateAllWeekViews();
            }
        }
    }

    /**
     * Gets the color for the week numbers.
     *
     * @return The week number color.
     *
     * @attr ref android.R.styleable#CalendarView_weekNumberColor
     */
    public int getWeekNumberColor() {
        return mWeekNumberColor;
    }

    /**
     * Sets the color for the separator line between weeks.
     *
     * @param color The week separator color.
     *
     * @attr ref android.R.styleable#CalendarView_weekSeparatorLineColor
     */
    public void setWeekSeparatorLineColor(int color) {
        if (mWeekSeparatorLineColor != color) {
            mWeekSeparatorLineColor = color;
            invalidateAllWeekViews();
        }
    }

    /**
     * Gets the color for the separator line between weeks.
     *
     * @return The week separator color.
     *
     * @attr ref android.R.styleable#CalendarView_weekSeparatorLineColor
     */
    public int getWeekSeparatorLineColor() {
        return mWeekSeparatorLineColor;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mListView.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return mListView.isEnabled();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setCurrentLocale(newConfig.locale);
    }

    @Override
    @TargetApi(14)
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(CalendarView.class.getName());
    }

    @Override
    @TargetApi(14)
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(CalendarView.class.getName());
    }

    /**
     * Gets the minimal date supported by this {@link CalendarView} in milliseconds
     * since January 1, 1970 00:00:00 in {@link TimeZone#getDefault()} time
     * zone.
     * <p>
     * Note: The default minimal date is 01/01/1900.
     * <p>
     *
     * @return The minimal supported date.
     *
     * @attr ref android.R.styleable#CalendarView_minDate
     */
    public long getMinDate() {
        return mMinDate.getTimeInMillis();
    }

    /**
     * Sets the minimal date supported by this {@link CalendarView} in milliseconds
     * since January 1, 1970 00:00:00 in {@link TimeZone#getDefault()} time
     * zone.
     *
     * @param minDate The minimal supported date.
     *
     * @attr ref android.R.styleable#CalendarView_minDate
     */
    public void setMinDate(long minDate) {
        mTempDate.setTimeInMillis(minDate);
        if (isSameDate(mTempDate, mMinDate)) {
            return;
        }
        mMinDate.setTimeInMillis(minDate);
        // make sure the current date is not earlier than
        // the new min date since the latter is used for
        // calculating the indices in the adapter thus
        // avoiding out of bounds error
        Calendar date = mAdapter.mSelectedDate;
        if (date.before(mMinDate)) {
            mAdapter.setSelectedDay(mMinDate);
        }
        // reinitialize the adapter since its range depends on min date
        mAdapter.init();
        if (date.before(mMinDate)) {
            setDate(mTempDate.getTimeInMillis());
        } else {
            // we go to the current date to force the ListView to query its
            // adapter for the shown views since we have changed the adapter
            // range and the base from which the later calculates item indices
            // note that calling setDate will not work since the date is the same
            goTo(date, false, true, false);
        }
    }

    /**
     * Gets the maximal date supported by this {@link CalendarView} in milliseconds
     * since January 1, 1970 00:00:00 in {@link TimeZone#getDefault()} time
     * zone.
     * <p>
     * Note: The default maximal date is 01/01/2100.
     * <p>
     *
     * @return The maximal supported date.
     *
     * @attr ref android.R.styleable#CalendarView_maxDate
     */
    public long getMaxDate() {
        return mMaxDate.getTimeInMillis();
    }

    /**
     * Sets the maximal date supported by this {@link CalendarView} in milliseconds
     * since January 1, 1970 00:00:00 in {@link TimeZone#getDefault()} time
     * zone.
     *
     * @param maxDate The maximal supported date.
     *
     * @attr ref android.R.styleable#CalendarView_maxDate
     */
    public void setMaxDate(long maxDate) {
        mTempDate.setTimeInMillis(maxDate);
        if (isSameDate(mTempDate, mMaxDate)) {
            return;
        }
        mMaxDate.setTimeInMillis(maxDate);
        // reinitialize the adapter since its range depends on max date
        mAdapter.init();
        Calendar date = mAdapter.mSelectedDate;
        if (date.after(mMaxDate)) {
            setDate(mMaxDate.getTimeInMillis());
        } else {
            // we go to the current date to force the ListView to query its
            // adapter for the shown views since we have changed the adapter
            // range and the base from which the later calculates item indices
            // note that calling setDate will not work since the date is the same
            goTo(date, false, true, false);
        }
    }

    /**
     * Sets whether to show the week number.
     *
     * @param showWeekNumber True to show the week number.
     *
     * @attr ref android.R.styleable#CalendarView_showWeekNumber
     */
    public void setShowWeekNumber(boolean showWeekNumber) {
        if (mShowWeekNumber == showWeekNumber) {
            return;
        }
        mShowWeekNumber = showWeekNumber;
        mAdapter.notifyDataSetChanged();
        setUpHeader();
    }

    /**
     * Gets whether to show the week number.
     *
     * @return True if showing the week number.
     *
     * @attr ref android.R.styleable#CalendarView_showWeekNumber
     */
    public boolean getShowWeekNumber() {
        return mShowWeekNumber;
    }

    /**
     * Gets the first day of week.
     *
     * @return The first day of the week conforming to the {@link CalendarView}
     *         APIs.
     * @see Calendar#MONDAY
     * @see Calendar#TUESDAY
     * @see Calendar#WEDNESDAY
     * @see Calendar#THURSDAY
     * @see Calendar#FRIDAY
     * @see Calendar#SATURDAY
     * @see Calendar#SUNDAY
     *
     * @attr ref android.R.styleable#CalendarView_firstDayOfWeek
     */
    public int getFirstDayOfWeek() {
        return mFirstDayOfWeek;
    }

    /**
     * Sets the first day of week.
     *
     * @param firstDayOfWeek The first day of the week conforming to the
     *            {@link CalendarView} APIs.
     * @see Calendar#MONDAY
     * @see Calendar#TUESDAY
     * @see Calendar#WEDNESDAY
     * @see Calendar#THURSDAY
     * @see Calendar#FRIDAY
     * @see Calendar#SATURDAY
     * @see Calendar#SUNDAY
     *
     * @attr ref android.R.styleable#CalendarView_firstDayOfWeek
     */
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        if (mFirstDayOfWeek == firstDayOfWeek) {
            return;
        }
        mFirstDayOfWeek = firstDayOfWeek;
        mAdapter.init();
        mAdapter.notifyDataSetChanged();
        setUpHeader();
    }

    /**
     * Sets the listener to be notified upon selected date change.
     *
     * @param listener The listener to be notified.
     */
    public void setOnDateChangeListener(OnDateChangeListener listener) {
        mOnDateChangeListener = listener;
    }

    /**
     * Gets the selected date in milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     *
     * @return The selected date.
     */
    public long getDate() {
        return mAdapter.mSelectedDate.getTimeInMillis();
    }

    /**
     * Sets the selected date in milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     *
     * @param date The selected date.
     *
     * @throws IllegalArgumentException of the provided date is before the
     *        minimal or after the maximal date.
     *
     * @see #setDate(long, boolean, boolean)
     * @see #setMinDate(long)
     * @see #setMaxDate(long)
     */
    public void setDate(long date) {
        setDate(date, false, false);
    }

    /**
     * Sets the selected date in milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     *
     * @param date The date.
     * @param animate Whether to animate the scroll to the current date.
     * @param center Whether to center the current date even if it is already visible.
     *
     * @throws IllegalArgumentException of the provided date is before the
     *        minimal or after the maximal date.
     *
     * @see #setMinDate(long)
     * @see #setMaxDate(long)
     */
    public void setDate(long date, boolean animate, boolean center) {
        mTempDate.setTimeInMillis(date);
        if (isSameDate(mTempDate, mAdapter.mSelectedDate)) {
            return;
        }
        goTo(mTempDate, animate, true, center);
    }
    
    public void addDate(int value, boolean animate, boolean center) {
    	add(Calendar.DATE, value, animate, center);
    }
    
    public void addWeek(int value, boolean animate, boolean center) {
    	add(Calendar.WEEK_OF_YEAR, value, animate, center);
    }
    
    public void addMonth(int value, boolean animate, boolean center) {
    	add(Calendar.MONTH, value, animate, center);
    }
    
    public void addYear(int value, boolean animate, boolean center) {
    	add(Calendar.YEAR, value, animate, center);
    }
    
    private void add(int field, int value, boolean animate, boolean center) {
    	mTempDate.setTimeInMillis(mAdapter.mSelectedDate.getTimeInMillis());
    	mTempDate.add(field, value);
        goTo(mTempDate, animate, true, center);
    }

    /**
     * Invalidates all week views.
     */
    private void invalidateAllWeekViews() {
        final int childCount = mListView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = mListView.getChildAt(i);
            view.invalidate();
        }
    }

    /**
     * Sets the current locale.
     *
     * @param locale The current locale.
     */
    private void setCurrentLocale(Locale locale) {
        if (locale.equals(mCurrentLocale)) {
            return;
        }

        mCurrentLocale = locale;

        mTempDate = getCalendarForLocale(mTempDate, locale);
        mMinDate = getCalendarForLocale(mMinDate, locale);
        mMaxDate = getCalendarForLocale(mMaxDate, locale);
    }

    /**
     * Gets a calendar for locale bootstrapped with the value of a given calendar.
     *
     * @param oldCalendar The old calendar.
     * @param locale The locale.
     */
    private Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
        if (oldCalendar == null) {
            return Calendar.getInstance(locale);
        } else {
            final long currentTimeMillis = oldCalendar.getTimeInMillis();
            Calendar newCalendar = Calendar.getInstance(locale);
            newCalendar.setTimeInMillis(currentTimeMillis);
            return newCalendar;
        }
    }

    /**
     * @return True if the <code>firstDate</code> is the same as the <code>
     * secondDate</code>.
     */
    private boolean isSameDate(Calendar firstDate, Calendar secondDate) {
        return (firstDate.get(Calendar.DAY_OF_YEAR) == secondDate.get(Calendar.DAY_OF_YEAR)
                && firstDate.get(Calendar.YEAR) == secondDate.get(Calendar.YEAR));
    }

    /**
     * Creates a new adapter if necessary and sets up its parameters.
     */
    private void setUpAdapter() {
        if (mAdapter == null) {
            mAdapter = new WeeksAdapter(getContext());
            mAdapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    if (mOnDateChangeListener != null) {
                        Calendar selectedDay = mAdapter.getSelectedDay();
                        mOnDateChangeListener.onSelectedDayChange(CalendarView.this,
                                selectedDay.get(Calendar.YEAR),
                                selectedDay.get(Calendar.MONTH),
                                selectedDay.get(Calendar.DAY_OF_MONTH));
                    }
                }
            });
            mListView.setAdapter(mAdapter);
        }

        // refresh the view with the new parameters
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Sets up the strings to be used by the header.
     */
    private void setUpHeader() {
        mDayNamesHeader = (ViewGroup)mContentView.findViewById(R.id.day_names);
        mDayLabels = new String[mDaysPerWeek];
        for (int i = mFirstDayOfWeek, count = mFirstDayOfWeek + mDaysPerWeek; i < count; i++) {
            int calendarDay = (i > Calendar.SATURDAY) ? i - Calendar.SATURDAY : i;
            mDayLabels[i - mFirstDayOfWeek] = DateUtils.getDayOfWeekString(calendarDay,
                    DateUtils.LENGTH_SHORTEST);
        }

        TextView label = (TextView) mDayNamesHeader.getChildAt(0);
        if (mShowWeekNumber) {
            label.setVisibility(View.VISIBLE);
        } else {
            label.setVisibility(View.GONE);
        }
        for (int i = 1, count = mDayNamesHeader.getChildCount(); i < count; i++) {
            label = (TextView) mDayNamesHeader.getChildAt(i);
            if (mWeekDayTextAppearanceResId > -1) {
                label.setTextAppearance(getContext(), mWeekDayTextAppearanceResId);
            }
            if (i < mDaysPerWeek + 1) {
                label.setText(mDayLabels[i - 1]);
                label.setVisibility(View.VISIBLE);
            } else {
                label.setVisibility(View.GONE);
            }
        }
        mDayNamesHeader.invalidate();
    }

    /**
     * Sets all the required fields for the list view.
     */
    @TargetApi(11)
    private void setUpListView() {
        mListView = new WeeksListView(getContext());
        mListView.setId(android.R.id.list);
        mContentView.addView(mListView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
		
        // Configure the listview
        mListView.setDivider(null);
        mListView.setItemsCanFocus(true);
        mListView.setVerticalScrollBarEnabled(false);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { // API 11
	        // Make the scrolling behavior nicer
	        mListView.setFriction(mFriction);
	        mListView.setVelocityScale(mVelocityScale);
        }
    }

    /**
     * This moves to the specified time in the view. If the time is not already
     * in range it will move the list so that the first of the month containing
     * the time is at the top of the view. If the new time is already in view
     * the list will not be scrolled unless forceScroll is true. This time may
     * optionally be highlighted as selected as well.
     *
     * @param date The time to move to.
     * @param animate Whether to scroll to the given time or just redraw at the
     *            new location.
     * @param setSelected Whether to set the given time as selected.
     *
     * @throws IllegalArgumentException of the provided date is before the
     *        range start of after the range end.
     */
    private void goTo(Calendar date, boolean animate, boolean setSelected, boolean center) {
        if (date.before(mMinDate) || date.after(mMaxDate)) {
            throw new IllegalArgumentException("Time not between " + mMinDate.getTime()
                    + " and " + mMaxDate.getTime());
        }
        
        if (setSelected) {
            mAdapter.setSelectedDay(date);
        }
        // Get the week we're going to
        int position = getWeeksSinceMinDate(date);

        mListView.scrollToPosition(position, animate, center);
    }

    /**
     * Parses the given <code>date</code> and in case of success sets
     * the result to the <code>outDate</code>.
     *
     * @return True if the date was parsed.
     */
    private boolean parseDate(String date, Calendar outDate) {
        try {
            outDate.setTime(mDateFormat.parse(date));
            return true;
        } catch (ParseException e) {
            Log.w(LOG_TAG, "Date: " + date + " not in format: " + DATE_FORMAT);
            return false;
        }
    }

    /**
     * @return Returns the number of weeks between the current <code>date</code>
     *         and the <code>mMinDate</code>.
     */
    private int getWeeksSinceMinDate(Calendar date) {
        if (date.before(mMinDate)) {
            throw new IllegalArgumentException("fromDate: " + mMinDate.getTime()
                    + " does not precede toDate: " + date.getTime());
        }
        long endTimeMillis = date.getTimeInMillis()
                + date.getTimeZone().getOffset(date.getTimeInMillis());
        long startTimeMillis = mMinDate.getTimeInMillis()
                + mMinDate.getTimeZone().getOffset(mMinDate.getTimeInMillis());
        long dayOffsetMillis = (mMinDate.get(Calendar.DAY_OF_WEEK) - mFirstDayOfWeek)
                * MILLIS_IN_DAY;
        return (int) ((endTimeMillis - startTimeMillis + dayOffsetMillis) / MILLIS_IN_WEEK);
    }
    
    private class AdjustScrollRunnable implements Runnable {
    
	    /**
	     * The duration of the adjustment upon a user scroll in milliseconds.
	     */
	    private static final int ADJUSTMENT_SCROLL_DURATION = 500;
	
	    /**
	     * How long to wait after receiving an onScrollStateChanged notification
	     * before acting on it.
	     */
	    private static final int SCROLL_CHANGE_DELAY = 40;
	    
        public void execute() {
            removeCallbacks(this);
            postDelayed(this, SCROLL_CHANGE_DELAY);
        }

        public void run() {
            View child = mListView.getChildAt(0);
            if (child != null) {
            	int top = child.getTop();
            	int bottom = child.getBottom();
            	if (top + bottom > 0)
                    mListView.smoothScrollBy(top, ADJUSTMENT_SCROLL_DURATION);
            	else
                    mListView.smoothScrollBy(bottom, ADJUSTMENT_SCROLL_DURATION);
            }
        }
    }

    /**
     * <p>
     * This is a specialized adapter for creating a list of weeks with
     * selectable days. It can be configured to display the week number, start
     * the week on a given day, show a reduced number of days, or display an
     * arbitrary number of weeks at a time.
     * </p>
     */
    private class WeeksAdapter extends BaseAdapter implements OnTouchListener {

        private int mSelectedWeek;

        private GestureDetector mGestureDetector;

        private int mFocusedMonth;

        private final Calendar mSelectedDate = Calendar.getInstance();

        private int mTotalWeekCount;

        public WeeksAdapter(Context context) {
            mGestureDetector = new GestureDetector(context, new CalendarGestureListener());
            init();
        }

        /**
         * Set up the gesture detector and selected time
         */
        private void init() {
            mSelectedWeek = getWeeksSinceMinDate(mSelectedDate);
            mTotalWeekCount = getWeeksSinceMinDate(mMaxDate);
            if (mMinDate.get(Calendar.DAY_OF_WEEK) != mFirstDayOfWeek
                || mMaxDate.get(Calendar.DAY_OF_WEEK) != mFirstDayOfWeek) {
                mTotalWeekCount++;
            }
        }

        /**
         * Updates the selected day and related parameters.
         *
         * @param selectedDay The time to highlight
         */
        public void setSelectedDay(Calendar selectedDay) {
            if (selectedDay.get(Calendar.DAY_OF_YEAR) == mSelectedDate.get(Calendar.DAY_OF_YEAR)
                    && selectedDay.get(Calendar.YEAR) == mSelectedDate.get(Calendar.YEAR)) {
                return;
            }
            mSelectedDate.setTimeInMillis(selectedDay.getTimeInMillis());
            mSelectedWeek = getWeeksSinceMinDate(mSelectedDate);
            mFocusedMonth = mSelectedDate.get(Calendar.MONTH);
            notifyDataSetChanged();
        }

        /**
         * @return The selected day of month.
         */
        public Calendar getSelectedDay() {
            return mSelectedDate;
        }

        @Override
        public int getCount() {
            return mTotalWeekCount;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            WeekView weekView = null;
            if (convertView != null) {
                weekView = (WeekView) convertView;
            } else {
                weekView = new WeekView(getContext());
                android.widget.AbsListView.LayoutParams params =
                    new android.widget.AbsListView.LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT);
                weekView.setLayoutParams(params);
                weekView.setClickable(true);
                weekView.setOnTouchListener(this);
            }

            int selectedWeekDay = (mSelectedWeek == position) ? mSelectedDate.get(
                    Calendar.DAY_OF_WEEK) : -1;
            weekView.init(position, selectedWeekDay, mFocusedMonth);

            return weekView;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mListView.isEnabled() && mGestureDetector.onTouchEvent(event)) {
                WeekView weekView = (WeekView) v;
                // if we cannot find a day for the given location we are done
                if (!weekView.getDayFromLocation(event.getX(), mTempDate)) {
                    return true;
                }
                // it is possible that the touched day is outside the valid range
                // we draw whole weeks but range end can fall not on the week end
                if (mTempDate.before(mMinDate) || mTempDate.after(mMaxDate)) {
                    return true;
                }
                onDateTapped(mTempDate);
                return true;
            }
            return false;
        }

        /**
         * Maintains the same hour/min/sec but moves the day to the tapped day.
         *
         * @param day The day that was tapped
         */
        private void onDateTapped(Calendar day) {
            setSelectedDay(day);
        }

        /**
         * This is here so we can identify single tap events and set the
         * selected day correctly
         */
        class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        }
    }

    /**
     * <p>
     * This is a dynamic view for drawing a single week. It can be configured to
     * display the week number, start the week on a given day, or show a reduced
     * number of days. It is intended for use as a single view within a
     * ListView. See {@link WeeksAdapter} for usage.
     * </p>
     */
    private class WeekView extends View {
    	
    	private final Rect mTempRect = new Rect();

        private final Paint mDrawPaint = new Paint();
        
        private final Paint mSelectedDateDrawPaint = new Paint();

        // Cache the number strings so we don't have to recompute them each time
        private String[] mDayNumbers;

        // The first day displayed by this item
        private Calendar mFirstDay;

        // The month of the first day in this week
        private int mMonthOfFirstWeekDay = -1;

        // The month of the last day in this week
        private int mMonthOfLastWeekDay = -1;
        
        // easetheworld : draw year, month background
        private int mYearOfFirstWeekDay = -1;
        private int mWeekOfFirstWeekDay = -1;
        private int mMaxWeekOfFirstWeekDay = -1;
        private int mWeekOfLastWeekDay = -1;
        private int mMaxWeekOfLastWeekDay = -1;

        // The position of this week, equivalent to weeks since the week of Jan
        // 1st, 1900
        private int mWeek = -1;

        // Quick reference to the width of this view, matches parent
        private int mWidth;

        // The height this view should draw at in pixels, set by height param
        private int mHeight;

        // If this view contains the selected day
        private boolean mHasSelectedDay = false;

        // Which day is selected [0-6] or -1 if no day is selected
        private int mSelectedDay = -1;

        // The number of days + a spot for week number if it is displayed
        private int mNumCells;

        // The left edge of the selected day
        private int mSelectedLeft = -1;

        // The right edge of the selected day
        private int mSelectedRight = -1;

        public WeekView(Context context) {
            super(context);

            // Sets up any standard paints that will be used
            initilaizePaints();
			
        }

        /**
         * Initializes this week view.
         *
         * @param weekNumber The number of the week this view represents. The
         *            week number is a zero based index of the weeks since
         *            {@link CalendarView#getMinDate()}.
         * @param selectedWeekDay The selected day of the week from 0 to 6, -1 if no
         *            selected day.
         * @param focusedMonth The month that is currently in focus i.e.
         *            highlighted.
         */
        public void init(int weekNumber, int selectedWeekDay, int focusedMonth) {
            mSelectedDay = selectedWeekDay;
            mHasSelectedDay = mSelectedDay != -1;
            mNumCells = mShowWeekNumber ? mDaysPerWeek + 1 : mDaysPerWeek;
            mWeek = weekNumber;
            mTempDate.setTimeInMillis(mMinDate.getTimeInMillis());

            mTempDate.add(Calendar.WEEK_OF_YEAR, mWeek);
            mTempDate.setFirstDayOfWeek(mFirstDayOfWeek);

            // Allocate space for caching the day numbers and focus values
            mDayNumbers = new String[mNumCells];

            // If we're showing the week number calculate it based on Monday
            int i = 0;
            if (mShowWeekNumber) {
                mDayNumbers[0] = Integer.toString(mTempDate.get(Calendar.WEEK_OF_YEAR));
                i++;
            }

            // Now adjust our starting day based on the start day of the week
            int diff = mFirstDayOfWeek - mTempDate.get(Calendar.DAY_OF_WEEK);
            mTempDate.add(Calendar.DAY_OF_MONTH, diff);

            mFirstDay = (Calendar) mTempDate.clone();
            mMonthOfFirstWeekDay = mTempDate.get(Calendar.MONTH);
            
	        // easetheworld : draw year, month background
            mYearOfFirstWeekDay = mTempDate.get(Calendar.YEAR);
            mWeekOfFirstWeekDay = mTempDate.get(Calendar.WEEK_OF_MONTH);
            mMaxWeekOfFirstWeekDay = mTempDate.getActualMaximum(Calendar.WEEK_OF_MONTH);

            for (; i < mNumCells; i++) {
                // do not draw dates outside the valid range to avoid user confusion
                if (mTempDate.before(mMinDate) || mTempDate.after(mMaxDate)) {
                    mDayNumbers[i] = "";
                } else {
                    mDayNumbers[i] = Integer.toString(mTempDate.get(Calendar.DAY_OF_MONTH));
                }
		        // easetheworld : draw year, month background
                if (i == mNumCells - 1) {
		            mWeekOfLastWeekDay = mTempDate.get(Calendar.WEEK_OF_MONTH);
		            mMaxWeekOfLastWeekDay = mTempDate.getActualMaximum(Calendar.WEEK_OF_MONTH);
                }
                mTempDate.add(Calendar.DAY_OF_MONTH, 1);
            }
            // We do one extra add at the end of the loop, if that pushed us to
            // new month undo it
            if (mTempDate.get(Calendar.DAY_OF_MONTH) == 1) {
                mTempDate.add(Calendar.DAY_OF_MONTH, -1);
            }
            mMonthOfLastWeekDay = mTempDate.get(Calendar.MONTH);

            updateSelectionPositions();
        }

        /**
         * Initialize the paint instances.
         */
        private void initilaizePaints() {
            mDrawPaint.setFakeBoldText(false);
            mDrawPaint.setAntiAlias(true);
            mDrawPaint.setStyle(Style.FILL);
            mDrawPaint.setTextAlign(Align.CENTER);
            
            mSelectedDateDrawPaint.setAntiAlias(true);
            mSelectedDateDrawPaint.setColor(mSelectedMonthDateColor);
            mSelectedDateDrawPaint.setStyle(Style.FILL);
        }

        public int getYearOfFirstWeekDay() {
            return mYearOfFirstWeekDay;
        }

        /**
         * Calculates the day that the given x position is in, accounting for
         * week number.
         *
         * @param x The x position of the touch event.
         * @return True if a day was found for the given location.
         */
        public boolean getDayFromLocation(float x, Calendar outCalendar) {
            int dayStart = mShowWeekNumber ? mWidth / mNumCells : 0;
            if (x < dayStart || x > mWidth) {
                outCalendar.clear();
                return false;
            }
            // Selection is (x - start) / (pixels/day) == (x -s) * day / pixels
            int dayPosition = (int) ((x - dayStart) * mDaysPerWeek
                    / (mWidth - dayStart));
            outCalendar.setTimeInMillis(mFirstDay.getTimeInMillis());
            outCalendar.add(Calendar.DAY_OF_MONTH, dayPosition);
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
        	drawMonth(canvas);
        	drawSelectedDateBackground(canvas);
        	drawWeekNumbersAndDates(canvas);
        	drawWeekSeparators(canvas);
        }
        
        // easetheworld : draw year, month background
        private static final int YEAR_MONTH_BACKGROUND_SPAN_ROW = 4;
        
        private void drawMonth(Canvas canvas) {
        	// month of first day
        	drawMultirowBackgroundText(canvas, Integer.toString(mMonthOfFirstWeekDay+1), mMonthPaint, mMaxWeekOfFirstWeekDay, YEAR_MONTH_BACKGROUND_SPAN_ROW, mWeekOfFirstWeekDay-1, getWidth() / 2);
        	
        	if (mMonthOfLastWeekDay == mMonthOfFirstWeekDay)
        		return;
        	
        	// month of last day
        	drawMultirowBackgroundText(canvas, Integer.toString(mMonthOfLastWeekDay+1), mMonthPaint, mMaxWeekOfLastWeekDay, YEAR_MONTH_BACKGROUND_SPAN_ROW, mWeekOfLastWeekDay-1, getWidth() / 2);
        }
		
        /**
         * draw text background which is laid across spanRows rows.
         * the background is drawn through drawRows rows.
         * to set margin between backgrounds set spanRows bigger than drawRows.
         * currentRow is the zero-based index in spanRows.
         * 
         * y will be calculated from the input.
         * you can set x.
         */
        final private void drawMultirowBackgroundText(Canvas canvas, String text, Paint paint, int spanRows, int drawRows, int currentRow, float x) {
        	float y = (spanRows - drawRows) * getHeight() / 2 + (drawRows - currentRow) * getHeight();
        	paint.setTextSize(drawRows * getHeight());
			canvas.drawText(text, x, y - paint.descent() / 2, paint);
        }

        /**
         * Draws the week and month day numbers for this week.
         *
         * @param canvas The canvas to draw on
         */
        private void drawWeekNumbersAndDates(Canvas canvas) {
            mDrawPaint.setTextSize(mDateTextSize);
            
            float textHeight = mDrawPaint.getTextSize();
            int y = (int) ((mHeight + textHeight) / 2) - mWeekSeperatorLineWidth;
            int nDays = mNumCells;

            int i = 0;
            int divisor = 2 * nDays;
            if (mShowWeekNumber) {
                mDrawPaint.setColor(mWeekNumberColor);
                int x = mWidth / divisor;
                canvas.drawText(mDayNumbers[0], x, y, mDrawPaint);
                i++;
            }
            int i0 = mFirstDayOfWeek - i;
            Paint dayPaint;
            for (; i < nDays; i++) {
            	// easetheworld : Saturday, Sunday highlight
            	int ii = i + i0;
            	if (ii > Calendar.SATURDAY)
            		ii -= DAYS_PER_WEEK;
        		if (ii == Calendar.SATURDAY) // Saturday
        			dayPaint = mSaturdayPaint;
        		else if (ii == Calendar.SUNDAY) // Sunday
        			dayPaint = mSundayPaint;
        		else
        			dayPaint = mWeekdayPaint;
                int x = (2 * i + 1) * mWidth / divisor;
                canvas.drawText(mDayNumbers[i], x, y, dayPaint);
            }
        }

        /**
         * Draws a horizontal line for separating the weeks.
         *
         * @param canvas The canvas to draw on.
         */
        private void drawWeekSeparators(Canvas canvas) {
            mDrawPaint.setColor(mWeekSeparatorLineColor);
            mDrawPaint.setStrokeWidth(mWeekSeperatorLineWidth);
            float x = mShowWeekNumber ? mWidth / mNumCells : 0;
            canvas.drawLine(x, 0, mWidth, 0, mDrawPaint);
        }

        /**
         * Draws the selected date bars if this week has a selected day.
         *
         * @param canvas The canvas to draw on
         */
        private void drawSelectedDateBackground(Canvas canvas) {
            if (!mHasSelectedDay) {
                return;
            }
            mTempRect.set(mSelectedLeft, mWeekSeperatorLineWidth, mSelectedRight, mHeight);
            canvas.drawRect(mTempRect, mSelectedDateDrawPaint);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            mWidth = w;
            updateSelectionPositions();
        }

        /**
         * This calculates the positions for the selected day lines.
         */
        private void updateSelectionPositions() {
            if (mHasSelectedDay) {
                int selectedPosition = mSelectedDay - mFirstDayOfWeek;
                if (selectedPosition < 0) {
                    selectedPosition += 7;
                }
                if (mShowWeekNumber) {
                    selectedPosition++;
                }
                mSelectedLeft = selectedPosition * mWidth / mNumCells;
                mSelectedRight = (selectedPosition + 1) * mWidth / mNumCells;
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            mHeight = (mListView.getHeight() - mListView.getPaddingTop() - mListView
                    .getPaddingBottom()) / mShownWeekCount;
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mHeight);
        }
    }
    
    private class WeeksListView extends SmoothListView {

		public WeeksListView(Context context) {
			super(context);
			setCacheColorHint(Color.TRANSPARENT);
			setFastScrollEnabled(false);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			drawYearText(canvas);
		}
		
		private void drawYearText(Canvas canvas) {
			WeekView middleChild = (WeekView)getChildAt(getChildCount() / 2);
			int year = middleChild.getYearOfFirstWeekDay();
        	mYearPaint.setTextSize(middleChild.getHeight());
			canvas.drawText(Integer.toString(year), getWidth() / 2, getHeight() / 2, mYearPaint);
		}
		
		public void scrollToPosition(int position, boolean animate, boolean center) {
			mCenterScrollRunnable.execute(position, animate, center);
		}
		
		private CenterScrollRunnable mCenterScrollRunnable = new CenterScrollRunnable();
    
	    private class CenterScrollRunnable implements Runnable {
	    	
	    	private int position;
	    	private boolean animate;
	    	private boolean center;
	    	
	    	private void execute(int position, boolean animate, boolean center) {
	    		removeCallbacks(this);
	    		if (center) {
	    			View firstChild = getChildAt(0);
	    			if (firstChild != null && firstChild.getHeight() > 0) {
	    				int offset = (getHeight() - firstChild.getHeight()) / 2;
	    				if (animate) {
	    					smoothScrollToPositionFromTop(position, offset);
	    				} else {
	    					setSelectionFromTop(position, offset);
	    				}
	    			} else { // child is not made yet. ex) called in constructor
	    				this.position = position;
	    				this.animate = animate;
	    				this.center = center;
	    				post(this);
	    			}
	    		} else if (position < getFirstVisiblePosition() || position > getLastVisiblePosition()) {
	    			if (animate)
	        			smoothScrollToPosition(position);
	    			else
	    				setSelection(position);
	    		}
	    	}
	
			@Override
			public void run() {
				execute(this.position, this.animate, this.center);
			}
	    }
	}
}