package com.slidingviewgroup;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.getMode;

/**
 * 侧滑容器，只能包括两个子View。第一个子View的平时显示的，第二个是侧滑时才显示的。
 * 侧滑的View会被拦截距离大于20的横向滑动事件
 *
 * @author mingC
 * @date 2018/6/11
 */
public class SlidingViewGroup extends ViewGroup {

	/**
	 * 滑动距离占副View比值阈值,超过自动滑出
	 */
	private final float SCROLL_DISTANCE_SHREHOLD = 0.2f;

	/**
	 * 速度阈值，超过这个数自动滑出
	 */
	private final float SCROLL_SPEED_SHREHOLD = 500;

	/**
	 * 状态
	 */
	private State mState = State.AT_FIRST;

	private enum State {
		AT_FIRST,  AT_SECOND
	}

	/**
	 * 自动滑动时的滑动时间，时间越小滑动越快
	 */
	private int mScrollDuration;

	/**
	 * 侧滑栏占容器的宽度比例
	 */
	private float mMenuWidthRadio = 0.2f;

	public SlidingViewGroup(Context context) {
		super(context);
		init();
	}

	public SlidingViewGroup(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SlidingViewGroup);
		mScrollDuration = array.getInt(R.styleable.SlidingViewGroup_scroll_duration, 300);
		mMenuWidthRadio = array.getFloat(R.styleable.SlidingViewGroup_menu_width_radio, 0.2f);
		array.recycle();
		init();
	}

	public SlidingViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	//平滑滑动工具
	private Scroller scroller;

	private void init() {
		scroller = new Scroller(getContext());
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		return super.dispatchTouchEvent(ev);
	}

	private float downX = 0;

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
			case ACTION_DOWN:
				Log.d("MyViewGroup", "down");
				downX = ev.getX();
				break;
			case ACTION_MOVE:
				Log.d("MyViewGroup", "move");
				//在按钮上横向滑动超过20则拦截，不触发按钮的点击事件
				float delX = ev.getX() - downX;
				double distance = Math.sqrt(delX*delX);
				Log.d("MyViewGroup", "distance:" + distance);
				if (distance > 20) {
					return true;
				}
				break;
			case ACTION_UP:
				Log.d("MyViewGroup", "up");
				break;
		}
		return false;
	}

	//速度追踪器
	private VelocityTracker velocityTracker;

	//当前滑动距离
	private int slideDistance = 0;

	//上一次滑动的X坐标
	private float lastX;

	//第一次Touch
	private boolean isFirstTouch = true;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (velocityTracker == null) {
			velocityTracker = VelocityTracker.obtain();
		}
		velocityTracker.addMovement(event);
		switch (event.getAction()) {
			case ACTION_DOWN:
				lastX = event.getX();
				isFirstTouch = false;
				break;
			case ACTION_MOVE:
				if (isFirstTouch) {
					lastX = event.getX();
					isFirstTouch = false;
					break;
				}
				int delta = (int) (lastX - event.getX());
				//滑动
				int scrollX = getScrollX() + delta;
				if (scrollX >= 0 && scrollX <= getSecondChildWidth()) {
					scrollTo(scrollX, 0);
					slideDistance += delta;
				} else {
					//到边界了
					slideDistance = 0;
					if (scrollX < 0) {
						scrollTo(0, 0);
					} else {
						scrollTo(getSecondChildWidth(), 0);
					}
				}
				lastX = event.getX();
				break;
			case ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				isFirstTouch = true;
				//判断是否超过滑动阈值
				velocityTracker.computeCurrentVelocity(1000);
				int speed = (int) velocityTracker.getXVelocity();
				Log.d("MyViewGroup", "滑动距离:" + slideDistance);
				Log.d("MyViewGroup", "滑动速度" + speed);
				if (mState == State.AT_FIRST) {
					if (slideDistance > getSecondChildWidth() * SCROLL_DISTANCE_SHREHOLD || -speed > SCROLL_SPEED_SHREHOLD) {
						scrollToSecondView();
					} else {
						scrollToFirstView();
					}
				} else if (mState == State.AT_SECOND) {
					if (-slideDistance > getSecondChildWidth() * SCROLL_DISTANCE_SHREHOLD || speed > SCROLL_SPEED_SHREHOLD) {
						scrollToFirstView();
					} else {
						scrollToSecondView();
					}
				}
				//重置滑动距离
				slideDistance = 0;
				lastX = 0;
				//回收速度追踪器
				velocityTracker.recycle();
				velocityTracker = null;
				break;
		}
		return true;
	}

	/**
	 * 平滑移动到主view
	 */
	public void scrollToFirstView() {
		smoothScrollTo(0, 0);
		mState = State.AT_FIRST;
	}

	/**
	 * 平滑移动到副view
	 */
	public void scrollToSecondView() {
		smoothScrollTo(getSecondChildWidth(), 0);
		mState = State.AT_SECOND;
	}

	/**
	 * 平滑滑动到(destX,destY)，滑动时间为mScrollDuration
	 * @param destX 相对x坐标
	 * @param destY 相对y坐标
	 */
	private void smoothScrollTo(int destX, int destY) {
		int scrollX = getScrollX();
		int scrollY = getScrollY();
		int deltaX = destX - scrollX;
		int deltaY = destY - scrollY;
		scroller.startScroll(scrollX, scrollY, deltaX, deltaY, mScrollDuration);
		invalidate();
	}

	@Override
	public void computeScroll() {
		if (scroller.computeScrollOffset()) {
			scrollTo(scroller.getCurrX(), scroller.getCurrY());
			postInvalidate();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}

	private int getFirstChildWidth() {
		if (getChildCount() > 0) {
			return getChildAt(0).getMeasuredWidth();
		}
		return 0;
	}

	private int getSecondChildWidth() {
		if (getChildCount() > 1) {
			return getChildAt(1).getMeasuredWidth();
		}
		return 0;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int childCount = getChildCount();
		int left = 0;
		int top = 0;
		int right;
		int bottom;
		//打横排列
		for (int i = 0; i < childCount && i < 2; i++) {
			View child = getChildAt(i);
			right = left + child.getMeasuredWidth();
			bottom = top + child.getMeasuredHeight();
			child.layout(left, top, right, bottom);
			left = right;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		int widthMode = getMode(widthMeasureSpec);
		int heightMode = getMode(heightMeasureSpec);

		//只测量前两个子View，其他的忽视
		int childCount = getChildCount();
		if (childCount > 0) {
			//测量子View1
			measureChild(getChildAt(0), widthMeasureSpec, heightMeasureSpec);
		}

		if (childCount > 1) {
			//测量子View2，宽度限定为本容器的特定比例
			int slidingViewMS = MeasureSpec.makeMeasureSpec((int) (widthSize * mMenuWidthRadio),
					widthMode);
			measureChild(getChildAt(1), slidingViewMS, heightMeasureSpec);
		}

		//计算子View中的最高和最宽
		int maxH = 0;
		int maxW = 0;

		if (childCount > 1) {
			int h1 = getChildAt(0).getMeasuredHeight();
			int h2 = getChildAt(1).getMeasuredHeight();
			maxH = h1 > h2 ? h1 : h2;
			int w1 = getChildAt(0).getMeasuredWidth();
			int w2 = getChildAt(1).getMeasuredWidth();
			maxW = w1 > w2 ? w1 : w2;
		} else if (childCount > 0) {
			maxW = getChildAt(0).getMeasuredWidth();
			maxH = getChildAt(0).getMeasuredHeight();
		}

		if (widthMode == AT_MOST && heightMode == AT_MOST) {
			setMeasuredDimension(maxW < widthSize ? maxW : widthSize,
					maxH < heightSize ? maxH : heightSize);
		} else if (widthMode == AT_MOST) {
			setMeasuredDimension(maxW < widthSize ? maxW : widthSize, heightMeasureSpec);
		} else if (heightMode == AT_MOST) {
			setMeasuredDimension(widthSize, maxH < heightSize ? maxH : heightSize);
		} else {
			setMeasuredDimension(widthSize, heightSize);
		}
	}
}
