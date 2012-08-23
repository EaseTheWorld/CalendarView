package com.easetheworld.calendarviewtest;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private CalendarView mCalendarView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mCalendarView = (CalendarView)findViewById(R.id.calendarView);
        mCalendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
			@Override
			public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
				Toast.makeText(MainActivity.this, year+"/"+(month+1)+"/"+dayOfMonth, Toast.LENGTH_SHORT).show();
			}
        });
    }
    
    public void clickHandler(View v) {
    	boolean animate = true;
    	boolean center = true;
    	switch(v.getId()) {
    	case R.id.btnDateDec:
    		mCalendarView.addDate(-1, animate, center);
    		break;
    	case R.id.btnDateInc:
    		mCalendarView.addDate(1, animate, center);
    		break;
    	case R.id.btnWeekDec:
    		mCalendarView.addWeek(-1, animate, center);
    		break;
    	case R.id.btnWeekInc:
    		mCalendarView.addWeek(1, animate, center);
    		break;
    	case R.id.btnMonthDec:
    		mCalendarView.addMonth(-1, animate, center);
    		break;
    	case R.id.btnMonthInc:
    		mCalendarView.addMonth(1, animate, center);
    		break;
    	case R.id.btnYearDec:
    		mCalendarView.addYear(-1, animate, center);
    		break;
    	case R.id.btnYearInc:
    		mCalendarView.addYear(1, animate, center);
    		break;
    	}
    }
}
