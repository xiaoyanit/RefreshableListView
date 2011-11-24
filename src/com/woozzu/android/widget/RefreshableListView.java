package com.woozzu.android.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.woozzu.android.R;

public class RefreshableListView extends ListView {
	
	private View mHeaderContainer = null;
	private View mHeaderView = null;
	private ImageView mArrow = null;
	private ProgressBar mProgress = null;
	private TextView mText = null;
	private float mY = 0;
	private float mHistoricalY = 0;
	private int mHistoricalTop = 0;
	private boolean mFlag = false;
	private boolean mArrowUp = false;
	private int mHeaderHeight = 0;
	private OnRefreshListener mListener = null;
	
	private static final int REFRESH = 0;
	private static final int NORMAL = 1;
	private static final int HEADER_HEIGHT_PX = 62;

	public RefreshableListView(Context context) {
		super(context);
		initialize();
	}

	public RefreshableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	public RefreshableListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}
	
	public void initialize() {
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mHeaderContainer = inflater.inflate(R.layout.refreshable_list_header, null);
		mHeaderView = mHeaderContainer.findViewById(R.id.refreshable_list_header);
		mArrow = (ImageView) mHeaderContainer.findViewById(R.id.refreshable_list_arrow);
		mProgress = (ProgressBar) mHeaderContainer.findViewById(R.id.refreshable_list_progress);
		mText = (TextView) mHeaderContainer.findViewById(R.id.refreshable_list_text);
		addHeaderView(mHeaderContainer);
		
		mHeaderHeight = (int) (HEADER_HEIGHT_PX * getContext().getResources().getDisplayMetrics().density);
		setHeaderHeight(0);
	}
	
	public void setOnRefreshListener(OnRefreshListener l) {
		mListener = l;
	}
	
	public void completeRefreshing() {
		mProgress.setVisibility(View.INVISIBLE);
		mArrow.setVisibility(View.VISIBLE);
		mHandler.sendMessage(mHandler.obtainMessage(NORMAL, mHeaderHeight, 0));
		invalidateViews();
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mY = mHistoricalY = ev.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			mHistoricalTop = getChildAt(0).getTop();
			break;
		case MotionEvent.ACTION_UP:
			if (mArrowUp) {
				startRefreshing();
				mHandler.sendMessage(mHandler.obtainMessage(REFRESH, (int) (ev.getY() - mY) / 2, 0));
			} else {
				if (getChildAt(0).getTop() == 0)
					mHandler.sendMessage(mHandler.obtainMessage(NORMAL, (int) (ev.getY() - mY) / 2, 0));
			}
			mFlag = false;
			break;
		}
		return super.onTouchEvent(ev);
	}

	@Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_MOVE && getFirstVisiblePosition() == 0) {
        	float direction = ev.getY() - mHistoricalY;
        	
        	// Scrolling downward
    		if (direction > 0) {
    			// Refresh bar is extended if top pixel of the first item is visible
    			if (getChildAt(0).getTop() == 0) {
    				if (mHistoricalTop < 0) {
    					mY = ev.getY();
    					mHistoricalTop = 0;
    				}
    				
    				// Extends refresh bar
    				setHeaderHeight((int) (ev.getY() - mY) / 2);
    				
    				// Stop list scroll to prevent the list from overscrolling
    				ev.setAction(MotionEvent.ACTION_CANCEL);
    				mFlag = false;
            	}
    		} else if (direction < 0) {
    			// Scrolling upward
    			
    			// Refresh bar is shortened if top pixel of the first item is visible
    			if (getChildAt(0).getTop() == 0) {
    				setHeaderHeight((int) (ev.getY() - mY) / 2);
    				
    				// If scroll reaches top of the list, list scroll is enabled
    				if (getChildAt(1).getTop() == 1 && !mFlag) {
    	        		ev.setAction(MotionEvent.ACTION_DOWN);
    	        		mFlag = true;
            		}
            	}
        	}
    		
        	mHistoricalY = ev.getY();
        }
        
        return super.dispatchTouchEvent(ev);
    }
	
	private void setHeaderHeight(int height) {
		// Extends refresh bar
		LayoutParams lp = (LayoutParams) mHeaderContainer.getLayoutParams();
		if (lp == null)
			lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		lp.height = height;
		mHeaderContainer.setLayoutParams(lp);
		
		// Refresh bar shows up from bottom to top
		LinearLayout.LayoutParams headerLp = (LinearLayout.LayoutParams) mHeaderView.getLayoutParams();
		if (headerLp == null)
			headerLp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		headerLp.topMargin = -mHeaderHeight + height;
		mHeaderView.setLayoutParams(headerLp);
		
		// If scroll reaches the trigger line, start refreshing
		if (height > mHeaderHeight && !mArrowUp) {
			mArrow.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.rotate));
			mText.setText("Release to update");
			rotateArrow();
			mArrowUp = true;
		} else if (height < mHeaderHeight && mArrowUp) {
			mArrow.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.rotate));
			mText.setText("Pull down to update");
			rotateArrow();
			mArrowUp = false;
		}
	}
	
	private void rotateArrow() {
		Drawable drawable = mArrow.getDrawable();
		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.save();
		canvas.rotate(180.0f, canvas.getWidth() / 2.0f, canvas.getHeight() / 2.0f);
		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
		drawable.draw(canvas);
		canvas.restore();
		mArrow.setImageBitmap(bitmap);
	}
	
	private void startRefreshing() {
		mArrow.setVisibility(View.INVISIBLE);
		mProgress.setVisibility(View.VISIBLE);
		mText.setText("Loading...");
		
		if (mListener != null)
			mListener.onRefresh();
	}
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			int limit = 0;
			switch (msg.what) {
			case REFRESH:
				limit = mHeaderHeight;
				break;
			case NORMAL:
				limit = 0;
				break;
			}
			
			// Elastic scrolling
			if (msg.arg1 >= limit) {
				setHeaderHeight(msg.arg1);
				int displacement = (msg.arg1 - limit) / 10;
				if (displacement == 0)
					mHandler.sendMessage(mHandler.obtainMessage(msg.what, msg.arg1 - 1, 0));
				else
					mHandler.sendMessage(mHandler.obtainMessage(msg.what, msg.arg1 - displacement, 0));
			}
		}
		
	};
	
	public interface OnRefreshListener {
		public void onRefresh();
	}
	
}
