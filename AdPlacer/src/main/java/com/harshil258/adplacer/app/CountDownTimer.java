package com.harshil258.adplacer.app;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CountDownTimer {

    private final long mMillisInFuture;
    private final long mCountdownInterval;
    private long mStopTimeInFuture;
    private long mPauseTime;

    private final AtomicBoolean mCancelled = new AtomicBoolean(false);
    private final AtomicBoolean mPaused = new AtomicBoolean(false);

    public CountDownTimer(long millisInFuture, long countDownInterval) {
        mMillisInFuture = millisInFuture;
        mCountdownInterval = countDownInterval;
    }

    public final void cancel() {
        mHandler.removeMessages(MSG);
        mCancelled.set(true);
    }

    public synchronized final CountDownTimer start() {
        if (mMillisInFuture <= 0) {
            onFinish();
            return this;
        }
        mStopTimeInFuture = SystemClock.elapsedRealtime() + mMillisInFuture;
        mCancelled.set(false);
        mPaused.set(false);
        mHandler.sendMessage(mHandler.obtainMessage(MSG));
        return this;
    }

    public long pause() {
        if (!mPaused.getAndSet(true)) {
            mPauseTime = mStopTimeInFuture - SystemClock.elapsedRealtime();
        }
        return mPauseTime;
    }

    public long resume() {
        if (mPaused.getAndSet(false)) {
            mStopTimeInFuture = mPauseTime + SystemClock.elapsedRealtime();
            mHandler.sendMessage(mHandler.obtainMessage(MSG));
        }
        return mPauseTime;
    }

    public abstract void onTick(long millisUntilFinished);

    public abstract void onFinish();

    private static final int MSG = 1;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (!mPaused.get()) {
                final long millisLeft = mStopTimeInFuture - SystemClock.elapsedRealtime();

                if (millisLeft <= 0) {
                    onFinish();
                } else if (millisLeft < mCountdownInterval) {
                    sendMessageAtTime(obtainMessage(MSG), SystemClock.elapsedRealtime() + millisLeft);
                } else {
                    long lastTickStart = SystemClock.elapsedRealtime();
                    onTick(millisLeft);

                    long delay = lastTickStart + mCountdownInterval - SystemClock.elapsedRealtime();

                    if (!mCancelled.get()) {
                        sendMessageAtTime(obtainMessage(MSG), SystemClock.elapsedRealtime() + Math.max(0, delay));
                    }
                }
            }
        }
    };
}
