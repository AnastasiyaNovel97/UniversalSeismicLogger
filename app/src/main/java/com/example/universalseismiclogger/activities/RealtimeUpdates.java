package com.example.universalseismiclogger.activities;

import android.app.Activity;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.universalseismiclogger.R;
import com.example.universalseismiclogger.recorder.RecorderValue;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Random;
import java.util.Vector;

public class RealtimeUpdates extends Fragment {
    private final Handler mHandler = new Handler();
    private LineGraphSeries<DataPoint> mSeries1;

    private RecorderValue recorderValue = RecorderValue.GetInstance();
    private int dataPointNumber = 50;
    private Vector<DataPoint> pointVector = new Vector<DataPoint>();

    private Runnable mTimer1 = new Runnable() {
        @Override
        public void run() {

            mSeries1.resetData(updateData());
            mHandler.postDelayed(this, 100);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_realtime_updates, container, false);

        GraphView graph = (GraphView) rootView.findViewById(R.id.graph);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(7);
        graph.getViewport().setMaxY(13);

        generateData();
        mSeries1 = new LineGraphSeries<>(updateData());
        graph.addSeries(mSeries1);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        ((RecordingActivity) activity).onSectionAttached(
//                getArguments().getInt(MainActivity.ARG_SECTION_NUMBER));
    }

    @Override
    public void onResume() {
        super.onResume();
        generateData();


    }



    @Override
    public void onPause() {
        mHandler.removeCallbacks(mTimer1);
        super.onPause();
    }

    private DataPoint[] updateData() {
        pointVector.remove(0);
        pointVector.add(new DataPoint(pointVector.lastElement().getX()+1,recorderValue.GetValue()));
        DataPoint[] arr = new DataPoint[dataPointNumber];
        if(pointVector.lastElement().getX() >= 10000) generateData();
        return pointVector.toArray(arr);
    }

    public void generateData() {
        pointVector = new Vector<DataPoint>();
        for (int i=0; i<dataPointNumber; i++) {
            pointVector.add(new DataPoint(i,0));
        }
    }

    public void StartShow(){
        mHandler.postDelayed(mTimer1, 300);
    }

    public void StopShow(){
        mHandler.removeCallbacks(mTimer1);
    }




}