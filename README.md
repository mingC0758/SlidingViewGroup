# 自定义控件之侧滑菜单SlidingViewGroup

本文主要介绍实现一个侧滑菜单的过程。利用ViewGroup来实现，并且具有强扩展性，用户可以自定义主要内容视图和侧滑菜单视图，并妥善处理了父View跟子View的滑动冲突问题。

## 灵感来源与需求

灵感主要来自TIM的聊天信息的滑动菜单：
![这里写图片描述](https://img-blog.csdn.net/20180824004645579?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L21pbmdDMDc1OA==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

需求：
1. ViewGroup允许用户放入两个视图，一个作为内容视图，一个作为菜单视图。
2. 滑动距离或速度大于阈值时滑出菜单
3. 在菜单视图中进行滑动时不响应菜单的点击事件


## 实现过程

常量定义：
```
/**
	 * 滑动距离占副View比值阈值,超过自动滑出
	 */
	private final float SCROLL_DISTANCE_SHREHOLD = 0.2f;

	/**
	 * 速度阈值，超过这个数自动滑出
	 */
	private final float SCROLL_SPEED_SHREHOLD = 500;
```

定义了一个滑动距离阈值和一个滑动速度阈值，任意一个超过了都进行滑动操作。

定义状态：
```
/**
	 * 状态
	 */
	private State mState = State.AT_FIRST;

	private enum State {
		AT_FIRST,  AT_SECOND
	}
```


AT_FIRST：在内容视图；

AT_SECOND：在菜单视图。

测量ViewGroup大小，如果ViewGroup指定了大小，则设置为指定大小，否则设置为子View中的最大值：
```
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
        //根据ViewGroup的要求来选择大小
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
```

布局，打横排列两个子View，再多就忽略：
```
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
```

添加几个滑动方法，利用Scroller实现：
```
    /**
	 * 自动滑动时的滑动时间，时间越小滑动越快
	 */
	private int mScrollDuration;
	
	//平滑滑动工具
	private Scroller scroller;

    //初始化一个Scroller
	private void init() {
		scroller = new Scroller(getContext());
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

```

重点，处理滑动事件：
```
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
			case MotionEvent.ACTION_DOWN:
				lastX = event.getX();
				isFirstTouch = false;
				break;
			case MotionEvent.ACTION_MOVE:
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
```

完成到这里之后，已经实现了侧滑功能：
![滑动冲突](https://img-blog.csdn.net/20180824004200731?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L21pbmdDMDc1OA==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

可以看到存在一个问题，就是在如果菜单视图设置了点击时间，由于在菜单上滑动事件被子View消费了，所以ViewGroup的onTouchEvent没有得到执行，因此也就不能根据我们的滑动手势进行滑动。

所以，下面我们用外部拦截法解决滑动冲突的问题：
```
private float downX = 0;

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
			case ACTION_DOWN:
			    //记录下按下的位置
				downX = ev.getX();
				break;
			case ACTION_MOVE:
				//在按钮上横向滑动超过20则拦截，不触发按钮的点击事件
				float delX = ev.getX() - downX;
				double distance = Math.sqrt(delX*delX);
				if (distance > 20) {
					return true;
				}
				break;
			case ACTION_UP:
				break;
		}
		return false;
	}
```

我们只根据需要对ACTION_MOVE进行拦截，当横向滑动超过20像素时拦截，拦截之后就会交给ViewGroup的onTouchEvent执行，而且子View接收不到MOVE事件。

我们不能拦截ACTION_DOWN，因为一旦拦截ACTION_DOWN，之后的一整个事件序列都会交给ViewGroup来执行；虽然我们拦截了MOVE事件，当事件序列结束时候，子View会收到一个CANCEL事件。

**最后，我们给出一些自定义属性，让用户可以在xml中指定这些属性**：
编辑模块中的attr.xml文件：
```
<?xml version="1.0" encoding="utf-8"?>
<resources>
	<declare-styleable name="SlidingViewGroup">
		<attr name="scroll_duration" format="integer"/>
		<attr name="menu_width_radio" format="float"/>
	</declare-styleable>
</resources>
```
代码设置：
```
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
		//从xml中读取属性
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
```

## 实现效果
![这里写图片描述](https://img-blog.csdn.net/20180824004609905?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L21pbmdDMDc1OA==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

```
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
	xmlns:android="http://schemas.android.com/apk/res/android"
	xmlns:tools="http://schemas.android.com/tools"
	android:layout_width="match_parent"
	android:layout_height="match_parent"
	xmlns:app="http://schemas.android.com/apk/res-auto"
	android:orientation="vertical"
	tools:context="com.slidingviewgroupdemo.MainActivity">


	<com.slidingviewgroup.SlidingViewGroup
		app:scroll_duration="2000"
		app:menu_width_radio="0.5"
		android:layout_width="match_parent"
		android:layout_height="150dp">

		<TextView
			android:gravity="center"
			android:textSize="18sp"
			android:text="主要内容"
			android:background="@color/colorAccent"
			android:layout_width="match_parent"
			android:layout_height="match_parent"/>

		<TextView
			android:gravity="center"
			android:textSize="18sp"
			android:text="侧滑菜单"
			android:background="@color/colorPrimary"
			android:layout_width="match_parent"
			android:layout_height="match_parent"/>
	</com.slidingviewgroup.SlidingViewGroup>

	<com.slidingviewgroup.SlidingViewGroup
		android:layout_marginTop="15dp"
		app:scroll_duration="500"
		app:menu_width_radio="0.3"
		android:layout_width="match_parent"
		android:layout_height="150dp">

		<TextView
			android:gravity="center"
			android:textSize="18sp"
			android:text="主要内容"
			android:background="@color/colorAccent"
			android:layout_width="match_parent"
			android:layout_height="match_parent"/>

		<LinearLayout
			android:orientation="horizontal"
			android:layout_width="match_parent"
			android:layout_height="match_parent">
			<TextView
				android:gravity="center"
				android:textSize="16sp"
				android:id="@+id/tv_set_top"
				android:text="置顶"
				android:background="@color/colorPrimary"
				android:layout_width="0dp"
				android:layout_weight="1"
				android:layout_height="match_parent"/>

			<TextView
				android:layout_marginLeft="1px"
				android:gravity="center"
				android:textSize="16sp"
				android:id="@+id/tv_del"
				android:text="删除"
				android:background="@color/colorPrimary"
				android:layout_width="0dp"
				android:layout_weight="1"
				android:layout_height="match_parent"/>
		</LinearLayout>

	</com.slidingviewgroup.SlidingViewGroup>
</LinearLayout>

```

## 接入方法
下载slidingviewgroup模块，导入到你的项目中：
![这里写图片描述](https://img-blog.csdn.net/20180824005335538?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L21pbmdDMDc1OA==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

添加模块依赖：
![这里写图片描述](https://img-blog.csdn.net/20180824005342359?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L21pbmdDMDc1OA==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

属性示例：
```
<com.slidingviewgroup.SlidingViewGroup
		android:layout_marginTop="15dp"
		app:scroll_duration="500"
		app:menu_width_radio="0.3"
		android:layout_width="match_parent"
		android:layout_height="150dp">
	<TextView
			android:text="内容视图"
			android:layout_width="match_parent"
			android:layout_height="match_parent"/>
	<Button
			android:text="菜单视图"
			android:layout_width="match_parent"
			android:layout_height="match_parent"/>
<ViewGroup>
```

属性 | 描述
---|---
scroll_duration | 自动滑动的时长，值越小越快
menu_width_radio | 菜单视图占容器的宽度比例，值为0.0~1.0

