package com.example.asynctask;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AsyncTaskActivity extends Activity {

    private static final String TAG = "AsyncTaskActivity";
    private static final int P_BAR_MAX = 100;
    Button bStart;
    Button bStop;
    TextView textViewMessage;
    ProgressBar pBar;
    private UpdateTask myUpdateTask=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_async_task);

        bStart = (Button) findViewById(R.id.buttonStart);
        bStop = (Button) findViewById(R.id.buttonStop);
        textViewMessage = (TextView) findViewById(R.id.textView2);
        pBar = (ProgressBar) findViewById(R.id.progressBar1);

        //what is the max value
        pBar.setMax(P_BAR_MAX);
    }


    //see https://developer.android.com/guide/components/activities/activity-lifecycle
    //must detach and attach thread in onStop and onStart
    @Override
    protected void onStart() {
        super.onStart();
        //lets see if the device rotated and we need to regrab thread
        //create or get ref to existing singleton, and then get the thread it's holding
        myUpdateTask = (UpdateTask)Singleton.getInstance().get_thread();

        //if a thread was retained then grab it
        if (myUpdateTask != null) {
            myUpdateTask.attach(this);
            pBar.setProgress(myUpdateTask.progress);
        }

        //set the buttonstate accordingly
        bStart.setEnabled(Singleton.getInstance().get_startState());
        bStop.setEnabled(!Singleton.getInstance().get_startState());
    }

    @Override
    protected void onStop() {
        super.onStop();
        //oh no, we rotated, save the thread
        if (myUpdateTask != null) {
            Log.d(TAG, "onStop");
            myUpdateTask.detach();
            Singleton.getInstance().set_thread(myUpdateTask);
            Singleton.getInstance().set_startState(bStart.isEnabled());
        }
    }

    // make sure only one is enabled at a time===========
    private void setButtonState(boolean state) {
        bStart.setEnabled(!state);
        bStop.setEnabled(state);
    }

    // start thread
    public void doStart(View v) {
        setButtonState(true);
        textViewMessage.setText("Working...");

        //create and start thread
        myUpdateTask = new UpdateTask(this);
        myUpdateTask.execute();
    }

    //try to cancel thread
    public void doStop(View v) {
        setButtonState(false);
        textViewMessage.setText("Stopping...");
        pBar.setProgress(0);

        myUpdateTask.cancel(true);
    }

    public void doButton(View view) {
        try {
            Thread.sleep(25000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class UpdateTask extends AsyncTask<Void,Integer,String>{
        int progress = 1;
        private AsyncTaskActivity activity = null;

        public UpdateTask(AsyncTaskActivity activity) {
            attach(activity);
        }

        public void attach(AsyncTaskActivity activity){
            this.activity = activity;
        }

        public void detach(){
            this.activity = null;
        }

        @Override
        protected String doInBackground(Void... voids) {
            for (int i = 1; i <= 10; i++) {
                //simulate some work sleep for .5 seconds
                SystemClock.sleep(500);

                //let main thread know we are busy
                //notice that we are autoboxing int to Integer
                publishProgress(i);

                //periodically check if the user canceled
                if (isCancelled())
                    return ("Canceled");
            }
            return "Finished";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            doMessage(s);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            progress = values[0] * 10;
            if(activity!= null)
                activity.pBar.setProgress(progress);
        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
            doMessage(s);
        }
        void doMessage(String s){
            if(activity!= null) {
                activity.textViewMessage.setText(s);
                activity.setButtonState(false);
            }
        }
    }
}
