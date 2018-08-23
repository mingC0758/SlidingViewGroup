package com.slidingviewgroupdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author mingC
 * @date 2018/6/11
 */
public class MyView extends View {
	public MyView(Context context) {
		super(context);
	}

	public MyView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		//滑动过多的时候不允许产生点击事件
		this.setOnTouchListener(new OnTouchListener() {
			float lastX;
			float lastY;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					lastX = event.getX();
					lastY = event.getY();
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					float x = event.getX();
					float y = event.getY();
					double distance = Math.sqrt((x-lastX)*(x-lastX) + (y-lastY)*(y-lastY));
					Log.d("MyView", "按钮内部滑动距离:" + distance);
					if (distance > 30) {
						return true;
					}
				}
				return false;
			}
		});
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
	}

	Paint paint = new Paint();

	@Override
	protected void onDraw(Canvas canvas) {
		Log.d("MyView", "isHardwareAccelerated():" + isHardwareAccelerated());
		canvas.drawColor(Color.GRAY);
		paint.setColor(Color.BLACK);
		paint.setStrokeWidth(5);
		canvas.drawLine(0, 0, 300, 300, paint);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(200, 200);
		Log.d("MyView", MeasureSpec.toString(widthMeasureSpec));
		Log.d("MyView", MeasureSpec.toString(heightMeasureSpec));
	}
}
