package com.ftechz.DebatingTimer;

import android.util.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;

import static java.util.Collections.sort;

/**
 * AlarmChain class
 * Keeps the time and calls alerts at associated alert times
 * When last alert has been reached, it continues to check the last alert for match
 */
public abstract class AlarmChain extends TimerTask {
    //
    // Classes
    public static abstract class AlarmChainAlert {
        public long time;
        protected AlertManager mAlertManager;

        public AlarmChainAlert(long seconds) {
            time = seconds;
        }

        public AlarmChainAlert(long seconds, AlertManager alertManager) {
            time = seconds;
            mAlertManager = alertManager;
        }

        public void setAlertManager(AlertManager alertManager) {
            mAlertManager = alertManager;
        }

        public abstract void alert();

        public void reset() {

        }
    }

    public static class IntermediateAlert extends AlarmChain.AlarmChainAlert {
        public IntermediateAlert(long seconds) {
            super(seconds);
        }

        @Override
        public void alert() {
            Log.i(this.getClass().getSimpleName(), "Intermediate Alert.");
        }
    }

    public static class WarningAlert extends AlarmChain.AlarmChainAlert {
        public WarningAlert(long seconds, AlertManager alertManager) {
            super(seconds, alertManager);
        }

        public WarningAlert(long seconds) {
            super(seconds);
        }

        @Override
        public void alert() {
            Log.i(this.getClass().getSimpleName(), "Warning Alert.");
            mAlertManager.triggerAlert(R.raw.beep1);
        }
    }

    public static class FinishAlert extends AlarmChain.AlarmChainAlert {
        public FinishAlert(long seconds, AlertManager alertManager) {
            super(seconds, alertManager);
        }

        public FinishAlert(long seconds) {
            super(seconds);
        }

        @Override
        public void alert() {
            Log.i(this.getClass().getSimpleName(), "Finish.");
            // Do an do-do alert
            mAlertManager.triggerAlert(R.raw.beep2);

        }
    }

    public static class OvertimeAlert extends AlarmChain.AlarmChainAlert {
        private long mRepeatPeriod = 0;
        private long initTime;

        public OvertimeAlert(long seconds, long repeatPeriod, AlertManager alertManager) {
            super(seconds, alertManager);
            initTime = seconds;
            mRepeatPeriod = repeatPeriod;
        }

        public OvertimeAlert(long seconds, long repeatPeriod) {
            super(seconds);
            initTime = seconds;
            mRepeatPeriod = repeatPeriod;
        }

        @Override
        public void alert() {
            time += mRepeatPeriod;
            Log.i(this.getClass().getSimpleName(), "OVERTIME!");
            // Do an do-do-do alert
            mAlertManager.triggerAlert(R.raw.beep3);

        }

        @Override
        public void reset() {
            time = initTime;
        }
    }

    public class AlarmChainAlertCompare implements Comparator<AlarmChainAlert> {
        @Override
        public int compare(AlarmChainAlert alert1, AlarmChainAlert alert2) {
            return (int) (alert1.time - alert2.time);
        }
    }

    public enum RunningState {
        Stopped,
        Running,
        Paused,
        Finished
    }

    //
    // Members
    private long mSecondCounter;
    protected ArrayList<AlarmChainAlert> mAlerts;

    protected String mName;

    private int mAlertNumber;
    private AlarmChainAlertCompare mAlertComparator = new AlarmChainAlertCompare();
    protected boolean mCountdown = false;
    protected long mFinishTime = 0;

    protected RunningState mRunningState = RunningState.Stopped;

    //
    // Methods
    public AlarmChain() {
        super();
        init();
    }

    public AlarmChain(AlarmChainAlert[] alerts) {
        super();
        init(alerts);
    }

    public AlarmChain(AlarmChainAlert[] alerts, boolean countdown) {
        super();
        init(alerts);
        mCountdown = countdown;
    }

    private void init() {
        mAlerts = new ArrayList<AlarmChainAlert>();
        mAlertNumber = 0;
        mSecondCounter = 0;
    }

    private void init(AlarmChainAlert[] alerts) {
        init();
        addTimes(alerts);
    }

    // Assumed to execute every second
    // Increments counter and checks for alert times
    @Override
    public void run() {
        if (mRunningState != RunningState.Running) {
            return;
        }

        mSecondCounter++;

        if (mAlertNumber < mAlerts.size()) {
            if (mSecondCounter == mAlerts.get(mAlertNumber).time) {
                do {
                    handleAlert(mAlerts.get(mAlertNumber));
                    if (mAlertNumber < mAlerts.size() - 1) {
                        mAlertNumber++;
                    } else {
                        break;
                    }
                } while (mSecondCounter == mAlerts.get(mAlertNumber).time); // Handle multiple with the same time
            } else if (mSecondCounter > mAlerts.get(mAlertNumber).time) {
                if (mAlertNumber < mAlerts.size() - 1) {
                    mAlertNumber++;
                }
            }
        }
    }

    public void addTime(AlarmChainAlert alert) {
        mAlerts.add(alert);
        if (alert.getClass() == FinishAlert.class) {
            mFinishTime = alert.time;
        }

        sort(mAlerts, mAlertComparator);
    }

    public void addTimes(AlarmChainAlert[] alerts) {
        if (alerts != null) {
            for (AlarmChainAlert alert : alerts) {
                if (alert.getClass() == FinishAlert.class) {
                    mFinishTime = alert.time;
                }
                mAlerts.add(alert);
            }
        }

        sort(mAlerts, mAlertComparator);
    }

    public long getSeconds() {
        if (mCountdown) {
            long time = getFinishTime() - mSecondCounter;
            if (time > 0) {
                return time;
            } else {
                return 0;
            }
        } else {
            return mSecondCounter;
        }
    }

    public long getNextTime() {
        if (mAlertNumber < mAlerts.size()) {
            if (mCountdown) {
                return getFinishTime() - mAlerts.get(mAlertNumber).time;
            } else {
                return mAlerts.get(mAlertNumber).time;
            }
        } else {
            return 0;
        }
    }

    public long getFinalTime() {
        if (mAlerts.size() > 0) {
            if (mCountdown) {
                return 0;
            } else {
                return getFinishTime();
            }
        } else {
            return 0;
        }
    }

    protected abstract void handleAlert(AlarmChainAlert alert);

    public abstract String getStateText();

    public void resetState() {
        mSecondCounter = 0;
        mAlertNumber = 0;
        for (AlarmChainAlert alert : mAlerts) {
            alert.reset();
        }
    }

    public abstract String getNotificationText();

    public abstract String getNotificationTickerText();

    // Required for rescheduling...
    public abstract AlarmChain newCopy();

    public void start(Timer timer) {
        mRunningState = RunningState.Running;
        resetState();
        onStart();
        timer.schedule(this, 1000, 1000);
    }

    public void pause() {
        mRunningState = RunningState.Paused;
    }

    public void resume() {
        mRunningState = RunningState.Running;
    }

    protected abstract void onStart();

    public abstract String getTitleText();

    private long getFinishTime() {
        if (mFinishTime > 0) {
            return mFinishTime;
        } else {
            return mAlerts.get(mAlerts.size() - 1).time;
        }
    }

    @Override
    public boolean cancel() {
        mRunningState = RunningState.Finished;
        return super.cancel();
    }

    public RunningState getRunningState() {
        return mRunningState;
    }

    public String getName() {
        return mName;
    }
}