Android CalendarView
===========================

Android supports CalendarView since API 11.
So I backport it and changed ui and method for my taste.
![screenshot0](https://raw.github.com/EaseTheWorld/CalendarView/master/screenshot_calendarview.png)

Differences
-----------
- Year and month are shown in background.
- Saturday and Sunday has different colors.
- Support `addDate()`, `addWeek()`, `addMonth()`, `addYear()` to easily change selected date.
- Some setXXXColor(), getXXXColor() are removed. I just thought they are too much(Nothing technical). If you need them, you are welcome to add them.
- `android.widget.ListView.smoothScrollToPositionFromTop()` has some bugs so I fixed it in `SmoothListView`.

Release Notes
-------------
- v0.1.0 : Initial Release

Source
------
https://github.com/EaseTheWorld/CalendarView

Made by **EaseTheWorld**