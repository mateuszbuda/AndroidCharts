package com.dacer.androidchartsexample;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.dacer.androidcharts.LineView;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Created by mateusz on 09/07/2014.
 */
public class InterpolatedLineFragment extends Fragment {
    int randomint = 36;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_line, container, false);
        final LineView lineView = (LineView) rootView.findViewById(R.id.line_view);

        ArrayList<String> test = new ArrayList<String>();
        for (int i = 0; i < randomint; i++) {
            test.add(String.valueOf(i));
        }
        lineView.setBottomTextList(test);
        lineView.setDrawDotLine(false);
        lineView.setShowPopup(LineView.SHOW_POPUPS_NONE);

        // ! New functionalities:
        lineView.enableHorizontalAnimation();
        lineView.enableInterpolation();
        lineView.setHorizontalLinesCount(4);

        Button lineButton = (Button) rootView.findViewById(R.id.line_button);
        lineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                randomSet(lineView);
            }
        });

        randomSet(lineView);
        return rootView;
    }

    private void randomSet(LineView lineView) {
        ArrayList<Float> dataList = new ArrayList<Float>();
        for (int i = 0; i < randomint; i++) {
            dataList.add(round(10 + (float) (Math.random() * 4), 2));
        }

        ArrayList<Float> dataList2 = new ArrayList<Float>();
        for (int i = 0; i < randomint; i++) {
            dataList2.add(round(10 + (float) (Math.random() * 4), 2));
        }

        ArrayList<ArrayList<Float>> dataLists = new ArrayList<ArrayList<Float>>();
        dataLists.add(dataList);
        dataLists.add(dataList2);

        lineView.setDataList(dataLists);
    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }
}
