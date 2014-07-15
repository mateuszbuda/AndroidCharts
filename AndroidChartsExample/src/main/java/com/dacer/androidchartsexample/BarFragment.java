package com.dacer.androidchartsexample;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.dacer.androidcharts.BarView;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Created by Dacer on 11/15/13.
 * Edited by mateuszbuda on 15 July 2014.
 */
public class BarFragment extends Fragment {
    int SIZE = 7;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bar, container, false);
        final BarView barView = (BarView) rootView.findViewById(R.id.bar_view);
        barView.setFgColor("#CAE8A2");
        Button button = (Button) rootView.findViewById(R.id.bar_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //randomSet(barView);
                weekDaysRandomSet(barView);
            }
        });
        //randomSet(barView);
        weekDaysRandomSet(barView);
        return rootView;
    }

    private void weekDaysRandomSet(BarView barView) {
        ArrayList<String> labels = new ArrayList<String>(SIZE);
        labels.add("Mo");
        labels.add("Tu");
        labels.add("We");
        labels.add("Th");
        labels.add("Fr");
        labels.add("Sa");
        labels.add("Su");

        barView.setBottomTextList(labels);

        int max = 100;
        ArrayList<Float> data = new ArrayList<Float>(SIZE);
        for (int i = 0; i < SIZE; i++)
            data.add(round((float) (Math.random() * max), 2));

        barView.setDataList(data, max);
    }

    private void randomSet(BarView barView) {
        int random = (int) (Math.random() * 20) + 6;
        ArrayList<String> test = new ArrayList<String>();
        for (int i = 0; i < random; i++) {
            test.add("test");
            test.add("pqg");
        }
        barView.setBottomTextList(test);

        ArrayList<Float> barDataList = new ArrayList<Float>();
        for (int i = 0; i < random * 2; i++) {
            barDataList.add((float) (Math.random() * 100));
        }
        barView.setDataList(barDataList, 100);
    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }
}