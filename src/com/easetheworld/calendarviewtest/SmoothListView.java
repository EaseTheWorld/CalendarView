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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ListView;

// implement smoothScrollToPositionFromTop for pre-Honeycomb
public class SmoothListView extends ListView {
	
	/**
     * Handles scrolling between positions within the list.
     */
    private PositionScroller mPositionScroller;	        

	public SmoothListView(Context context) {
		super(context);
	}
	
	public SmoothListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
    public boolean onTouchEvent(MotionEvent ev) {
		if (mPositionScroller != null)
			mPositionScroller.stop();
		return super.onTouchEvent(ev);
	}
	
	@Override
	@TargetApi(11)
	public void smoothScrollToPositionFromTop(int position, int offset) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			super.smoothScrollToPositionFromTop(position, offset); // has a bug
		} else {
			if (mPositionScroller == null)
				mPositionScroller = new PositionScroller();
			mPositionScroller.startWithOffset(position, offset);
		}
	}
	
	private class PositionScroller implements Runnable {
        private static final int SCROLL_DURATION = 200;

        private static final int MOVE_DOWN_POS = 1;
        private static final int MOVE_UP_POS = 2;
        private static final int MOVE_OFFSET = 3;

        private int mMode;
        private int mTargetPos;
        private int mLastSeenPos;
        private int mDirection;
        private int mScrollDuration;
        private final int mExtraScroll;

        private int mOffsetFromTop;

        PositionScroller() {
            mExtraScroll = ViewConfiguration.get(getContext()).getScaledFadingEdgeLength();
        }
        
        void start(final int position) {
        	start(position, SCROLL_DURATION);
        }
        
        void start(final int position, int duration) {
        	stop();
            final int firstPos = getFirstVisiblePosition();
            final int lastPos = getLastVisiblePosition();
            
            int viewTravelCount = 0;
            if (position <= firstPos) {                
                viewTravelCount = firstPos - position + 1;
                mMode = MOVE_UP_POS;
            } else if (position >= lastPos) {
                viewTravelCount = position - lastPos + 1;
                mMode = MOVE_DOWN_POS;
            } else {
                // Already on screen, nothing to do
                return;
            }
            
            if (viewTravelCount > 0) {
                mScrollDuration = duration / viewTravelCount;
            } else {
                mScrollDuration = duration;
            }
            mTargetPos = position;
            mLastSeenPos = INVALID_POSITION;
            
            post(this);
        }
        
        void startWithOffset(int position, int offset) {
            startWithOffset(position, offset, SCROLL_DURATION);
        }

        void startWithOffset(final int position, int offset, final int duration) {
            stop();

            final int childCount = getChildCount();
            if (childCount == 0) {
                // Can't scroll without children.
                return;
            }

            offset += getPaddingTop();

            mTargetPos = Math.max(0, Math.min(getCount() - 1, position));
            mOffsetFromTop = offset;
            mLastSeenPos = INVALID_POSITION;
            mMode = MOVE_OFFSET;

            final int firstPos = getFirstVisiblePosition();
            final int lastPos = firstPos + childCount - 1;

            int viewTravelCount;
            if (mTargetPos < firstPos) {
            	mDirection = MOVE_UP_POS;
                viewTravelCount = firstPos - mTargetPos;
            } else if (mTargetPos > lastPos) {
            	mDirection = MOVE_DOWN_POS;
                viewTravelCount = mTargetPos - lastPos;
            } else {
                // On-screen, just scroll.
                final int targetTop = getChildAt(mTargetPos - firstPos).getTop();
                smoothScrollBy(targetTop - offset, duration);
                return;
            }

            // Estimate how many screens we should travel
            final float screenTravelCount = (float) viewTravelCount / childCount;
            mScrollDuration = screenTravelCount < 1 ? duration : (int) (duration / screenTravelCount);
            mLastSeenPos = INVALID_POSITION;

            post(this);
        }
        

        void stop() {
            removeCallbacks(this);
        }

        public void run() {
            final int listHeight = getHeight();
            final int firstPos = getFirstVisiblePosition();
            final int lastPos = getLastVisiblePosition();

            switch (mMode) {
            case MOVE_DOWN_POS: {
                final int lastViewIndex = getChildCount() - 1;

                if (lastViewIndex < 0) {
                    return;
                }

                if (lastPos == mLastSeenPos) {
                    // No new views, let things keep going.
                    post(this);
                    return;
                }

                final View lastView = getChildAt(lastViewIndex);
                final int lastViewHeight = lastView.getHeight();
                final int lastViewTop = lastView.getTop();
                final int lastViewPixelsShowing = listHeight - lastViewTop;
                final int extraScroll = lastPos < getCount() - 1 ? Math.max(getListPaddingBottom(), mExtraScroll) : getListPaddingBottom();

                final int scrollBy = lastViewHeight - lastViewPixelsShowing + extraScroll;
                smoothScrollBy(scrollBy, mScrollDuration);

                mLastSeenPos = lastPos;
                if (lastPos < mTargetPos) {
                    post(this);
                }
                break;
            }

            case MOVE_UP_POS: {
                if (firstPos == mLastSeenPos) {
                    // No new views, let things keep going.
                    post(this);
                    return;
                }

                final View firstView = getChildAt(0);
                if (firstView == null) {
                    return;
                }
                final int firstViewTop = firstView.getTop();
                final int extraScroll = firstPos > 0 ? Math.max(mExtraScroll, getListPaddingTop()) : getListPaddingTop();

                smoothScrollBy(firstViewTop - extraScroll, mScrollDuration);

                mLastSeenPos = firstPos;

                if (firstPos > mTargetPos) {
                    post(this);
                }
                break;
            }

            case MOVE_OFFSET: {
            	if (mDirection == MOVE_UP_POS) {
            		if (mLastSeenPos == firstPos) {
            			// No new views, let things keep going.
        				post(this);
            			return;
            		}
	                mLastSeenPos = firstPos;
            	} else {
            		if (mLastSeenPos == lastPos) {
            			// No new views, let things keep going.
        				post(this);
            			return;
            		}
	                mLastSeenPos = lastPos;
            	}

                final int childCount = getChildCount();
                final int position = mTargetPos;

                int viewTravelCount = 0;
                if (position < firstPos) {
                    viewTravelCount = firstPos - position + 1;
                } else if (position > lastPos) {
                    viewTravelCount = position - lastPos;
                }

                // Estimate how many screens we should travel
                final float screenTravelCount = (float) viewTravelCount / childCount;

                final float modifier = Math.min(screenTravelCount, 1.f);
                if (position < firstPos) {
                    final int distance = (int) (-getHeight() * modifier);
                    final int duration = (int) (mScrollDuration * modifier);
                    
                	// distance is sometimes not big enough to change mLastSeenPos. so check first view top.
	                final View firstView = getChildAt(0);
	                final int scrollBy = firstView.getTop() - 1; // -1 is important to go to next view
	                
                    smoothScrollBy(Math.min(distance, scrollBy), duration);
                    post(this);
                } else if (position > lastPos) {
                    final int distance = (int) (getHeight() * modifier);
                    final int duration = (int) (mScrollDuration * modifier);
                    
                	// distance is sometimes not big enough to change mLastSeenPos. so check last view top.
	                final View lastView = getChildAt(getChildCount() - 1);
	                final int scrollBy = lastView.getHeight() - (listHeight - lastView.getTop()) + 1; // +1 is important to go to next view
	                
                    smoothScrollBy(Math.max(distance, scrollBy), duration);
                    post(this);
                } else {
                    // On-screen, just scroll.
                    final int targetTop = getChildAt(position - firstPos).getTop();
                    final int distance = targetTop - mOffsetFromTop;
                    final int duration = (int) (mScrollDuration * ((float) Math.abs(distance) / getHeight()));
                    smoothScrollBy(distance, duration);
                }
                break;
            }

            default:
                break;
            }
        }
    }
}