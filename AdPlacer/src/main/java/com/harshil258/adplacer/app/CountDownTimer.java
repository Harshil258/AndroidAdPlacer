package com.harshil258.adplacer.app;
import android.os.Handler;
import android.os.Looper;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CountDownTimer {

    private final long mMillisInFuture;
    private final long mCountdownInterval;
    private long mStopTimeInFuture;
    private long mPauseTime;

    private final AtomicBoolean mCancelled = new AtomicBoolean(false);
    private final AtomicBoolean mPaused = new AtomicBoolean(false);

    private Timer mTimer;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public CountDownTimer(long millisInFuture, long countDownInterval) {
        mMillisInFuture = millisInFuture;
        mCountdownInterval = countDownInterval;
    }

    public final void cancel() {
        if (mTimer != null) {
            mTimer.cancel();
        }
        mCancelled.set(true);
    }

    public synchronized final CountDownTimer start() {
        if (mMillisInFuture <= 0) {
            onFinish();
            return this;
        }
        mStopTimeInFuture = System.currentTimeMillis() + mMillisInFuture;
        mCancelled.set(false);
        mPaused.set(false);

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!mPaused.get()) {
                    final long millisLeft = mStopTimeInFuture - System.currentTimeMillis();

                    if (millisLeft <= 0) {
                        mHandler.post(() -> {
                            cancel();
                            onFinish();
                        });
                    } else {
                        mHandler.post(() -> onTick(millisLeft));
                    }
                }
            }
        }, 0, mCountdownInterval);

        return this;
    }

    public long pause() {
        if (!mPaused.getAndSet(true)) {
            mPauseTime = mStopTimeInFuture - System.currentTimeMillis();
            if (mTimer != null) {
                mTimer.cancel();
            }
        }
        return mPauseTime;
    }

    public long resume() {
        if (mPaused.getAndSet(false)) {
            mStopTimeInFuture = mPauseTime + System.currentTimeMillis();

            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!mPaused.get()) {
                        final long millisLeft = mStopTimeInFuture - System.currentTimeMillis();

                        if (millisLeft <= 0) {
                            mHandler.post(() -> {
                                cancel();
                                onFinish();
                            });
                        } else {
                            mHandler.post(() -> onTick(millisLeft));
                        }
                    }
                }
            }, 0, mCountdownInterval);
        }
        return mPauseTime;
    }

    public abstract void onTick(long millisUntilFinished);

    public abstract void onFinish();
}