# 自定义控件之侧滑菜单SlidingViewGroup

## 效果图
![功能菜单效果图](http://ww1.sinaimg.cn/large/b1b3510fgy1fuky8jrvb3g20a20fl46b.gif)

## 功能介绍
1. ViewGroup允许用户放入两个视图，一个作为内容视图，一个作为菜单视图。
2. 滑动距离或速度大于阈值时滑出菜单
3. 在菜单视图中进行滑动时不响应菜单的点击事件

## 使用说明
下载slidingviewgroup模块，导入到你的项目中：
![模块图](http://ww1.sinaimg.cn/large/b1b3510fgy1fuky9ufbzxj20d108k74z.jpg)

添加模块依赖：
![模块依赖](http://ww1.sinaimg.cn/large/b1b3510fgy1fukya6hh99j20cl08fwf0.jpg)

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

