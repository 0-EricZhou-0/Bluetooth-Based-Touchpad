package com.example.websocketTest;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

abstract class Connection {
    private static final String separator = Character.toString((char) 9);
    private Context context;
    private Activity activity;
    private Button startConnection;
    private ViewGroup rootView;


    private void completeProgressLoading(final ProgressBar progressBar) {
        final int progressNow = progressBar.getProgress();
        final int progressMax = progressBar.getMax();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    try {
                        Thread.sleep(2);
                        progressBar.setProgress((progressMax - progressNow) / 100 * i + progressNow);
                    } catch (InterruptedException e) {
                        progressBar.setProgress(progressMax);
                    }
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });
                progressBar.setProgress(progressBar.getMin());
            }
        }).start();
    }

    abstract private class TryConnect extends Thread {
        public void run() {

        }
    }


}
