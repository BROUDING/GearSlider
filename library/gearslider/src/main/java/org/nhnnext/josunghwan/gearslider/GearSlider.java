package org.nhnnext.josunghwan.gearslider;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.daimajia.easing.Glider;
import com.daimajia.easing.Skill;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;

/**
 * Created by josunghwan on 15. 6. 30..
 */
public class GearSlider extends FrameLayout {
    private static final String DEBUG_TAG = GearSlider.class.getSimpleName();

    protected int mNumberOfBar;
    protected int mIntervalOfBar;
    protected int mIntervalOfLongBar;

    protected float mHeightOfBar;
    protected float mHeightOfLongBar;

    private int mBackgroundColor;
    private int mCenterBarColor;
    private int mBarColor;

    private boolean isFling;

    public static float DPSIZE;

    public interface OnValueChangeListener {
        void onValueChange(int value);
    }

    private Context mContext;
    private RulerView mRulerView;
    private CenterBar mCenterBar;

    private OnValueChangeListener mListener;
    private int mCurrentValue = 0;

    private GestureDetectorCompat mDetector;

    public GearSlider(Context context) {
        super(context);
        initGearSliderView(context);
    }

    public GearSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.GearSlider,
                0, 0);

        try {
            //TODO : DEFAULT VALUE에 대한 고민 및 코드 작성
            mCurrentValue = a.getInteger(R.styleable.GearSlider_init_value, 0);
            mNumberOfBar = a.getInteger(R.styleable.GearSlider_number_of_bar, 50);
            mIntervalOfLongBar = a.getInteger(R.styleable.GearSlider_interval_of_longbar, 5);

            mIntervalOfBar = a.getDimensionPixelSize(R.styleable.GearSlider_interval_of_bar, -1);
            mHeightOfBar = a.getDimensionPixelSize(R.styleable.GearSlider_height_of_bar, -1);
            mHeightOfLongBar = a.getDimensionPixelSize(R.styleable.GearSlider_height_of_longbar, -1);

            mCenterBarColor = a.getColor(R.styleable.GearSlider_centerbar_color, -1);
            mBackgroundColor = a.getColor(R.styleable.GearSlider_background_color, -1);
            mBarColor = a.getColor(R.styleable.GearSlider_bar_color, -1);

            isFling = a.getBoolean(R.styleable.GearSlider_on_fling, false);
        } finally {
            a.recycle();
        }
        initGearSliderView(context);
    }

    public GearSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initGearSliderView(context);
    }

    public void setChangeValueListener(OnValueChangeListener listener) {
        mListener = listener;
    }

    public void initGearSliderView(Context context) {
        mContext = context;
        DPSIZE = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                1, getResources().getDisplayMetrics());
        setClickable(true);
        setBackgroundColor(mBackgroundColor);
        mDetector = new GestureDetectorCompat(getContext(), new MyGestureListener());
    }

    public void setNumberOfBar(int newValue) {
        mNumberOfBar = newValue;
        removeView(mRulerView);
        removeView(mCenterBar);

        RulerView.Attrs attrs = new RulerView.Attrs(mIntervalOfBar, mIntervalOfLongBar, mNumberOfBar, mHeightOfBar, mHeightOfLongBar, mBarColor);
        mRulerView = new RulerView(mContext, attrs);
        addView(mRulerView);

        mCenterBar = new CenterBar(mContext, mBackgroundColor, mCenterBarColor, mHeightOfLongBar);
        addView(mCenterBar);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        RulerView.Attrs attrs = new RulerView.Attrs(mIntervalOfBar, mIntervalOfLongBar, mNumberOfBar, mHeightOfBar, mHeightOfLongBar, mBarColor);
        mRulerView = new RulerView(mContext, attrs);
        addView(mRulerView);

        mCenterBar = new CenterBar(mContext, mBackgroundColor, mCenterBarColor, mHeightOfLongBar);
        addView(mCenterBar);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mRulerView.setX(w / 2 - (mIntervalOfBar * mCurrentValue));
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                                float distanceY) {
            if (getWidth() / 2 < (mRulerView.getX() - distanceX)) {
                Log.i(DEBUG_TAG, "Too Low Value");
            } else if ((mRulerView.getX() + mRulerView.getWidth()) - distanceX < getWidth() / 2) {
                Log.i(DEBUG_TAG, "Too High Value");
            } else {
                mCurrentValue = (int) (mNumberOfBar * ((mRulerView.getX() * -1) + (getWidth() / 2)) / mRulerView.getWidth());
                mRulerView.setX(mRulerView.getX() - distanceX);
                if (mListener != null) {
                    mListener.onValueChange(mCurrentValue);
                }
                Log.i(DEBUG_TAG, String.format("mCurrentValue=%d", mCurrentValue));
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            if (!isFling)
                return true;
            Log.d(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString());
            Log.d(DEBUG_TAG, "VelocityX: " + velocityX);
            int tempValue;
            int moveValue;
            if (Math.abs(velocityX) > 4000) {
                moveValue = getMoveValue(Math.abs(velocityX));
                if (velocityX < 0) {
                    if (mCurrentValue + 6 * moveValue > getMaximumValue() + 1)
                        tempValue = getMaximumValue() + 1;
                    else
                        tempValue = mCurrentValue + 6 * moveValue;
                } else {
                    if (mCurrentValue - 6 * moveValue < getMinimumValue())
                        tempValue = getMinimumValue();
                    else
                        tempValue = mCurrentValue - 6 * moveValue;
                }
                setValueWithAnimation(tempValue);
            }
            return true;
        }

        private int getMoveValue(float velocityX) {
            velocityX /= 1000;
            if (velocityX > 12)
                return 24;
            else {
                velocityX -= 4;
                return (int) (velocityX * 2 + 4);
            }
        }
    }

    public void setValueWithAnimation(int value) {
        mCurrentValue = value;
        Log.i(DEBUG_TAG, String.format("mCurrentValue=%d", mCurrentValue));
        final ObjectAnimator oa = ObjectAnimator.ofFloat(mRulerView, "x", (getWidth() / 2) - (mIntervalOfBar * value));
        AnimatorSet set = new AnimatorSet();
        set.playTogether(Glider.glide(Skill.ExpoEaseOut, 1200, oa));
        set.setDuration(1200);
        set.start();
        if (mListener != null)
            mListener.onValueChange(value);
    }

    public void setValue(int value) {
        mCurrentValue = value;
        mRulerView.setX((getWidth() / 2) - (mIntervalOfBar * value));
        if (mListener != null)
            mListener.onValueChange(value);
    }

    public int getValue() {
        return mCurrentValue;
    }

    public int getMinimumValue() {
        return 0;
    }

    public int getMaximumValue() {
        return mNumberOfBar - 1;
    }

}