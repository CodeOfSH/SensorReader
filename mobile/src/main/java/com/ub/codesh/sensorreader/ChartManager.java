package com.ub.codesh.sensorreader;

import com.github.mikephil.charting.charts.*;
import com.github.mikephil.charting.components.*;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ChartManager {
    private LineChart lineChart;
    private  YAxis leftAxis;
    private YAxis rightAxis;
    private XAxis xAxis;
    private LineData lineData;
    private LineDataSet lineDataSet;
    private List<ILineDataSet> lineDataSets=new ArrayList<>();
    private SimpleDateFormat df=new SimpleDateFormat("HH:mm:ss");
    private  List<String> timeList=new ArrayList<>();

    // constructor for one line
    public ChartManager(LineChart newLineChart, String name, int color)
    {
        this.lineChart=newLineChart;
        leftAxis=newLineChart.getAxisLeft();
        rightAxis=newLineChart.getAxisRight();
        xAxis=newLineChart.getXAxis();
        initLineChart();
        initLineDataSet(name,color);
    }

    public ChartManager(LineChart newLineChart, List<String> names, List<Integer> colors){
        this.lineChart=newLineChart;
        leftAxis= lineChart.getAxisLeft();
        rightAxis=lineChart.getAxisRight();
        xAxis=lineChart.getXAxis();
        initLineChart();
        initLineDataSet(names,colors);
    }

    //initializer for line chart
    private void initLineChart(){
        lineChart.setDrawGridBackground(false);
        lineChart.setDrawBorders(true);
        //set the chart
        Legend legend = lineChart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextSize(11f);
        //set position
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment((Legend.LegendHorizontalAlignment.LEFT));
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(10);

        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return timeList.get((int)value%timeList.size());
            }
        });


        leftAxis.setAxisMinimum(0f);
        rightAxis.setAxisMinimum(0f);
    }

    //initialize one line
    private void initLineDataSet(String name,int color){
        lineDataSet = new LineDataSet(null,name);
        lineDataSet.setLineWidth(1.5f);
        lineDataSet.setCircleRadius(1.5f);
        lineDataSet.setColor(color);
        lineDataSet.setCircleColor(color);

        lineDataSet.setDrawFilled(true);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setValueTextSize(10f);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        lineData=new LineData();
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    //initialize multi lines
    private void initLineDataSet(List<String> names, List<Integer> colors){
        for(int i=0;i<names.size();i++){
            lineDataSet = new LineDataSet(null,names.get(i));
            lineDataSet.setColor(colors.get(i));
            lineDataSet.setLineWidth(1.5f);
            lineDataSet.setCircleRadius(1.5f);
            lineDataSet.setCircleColor(colors.get(i));

            lineDataSet.setDrawFilled(true);
            lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            lineDataSet.setValueTextSize(10f);
            lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

            lineDataSets.add(lineDataSet);
        }
        lineData=new LineData();
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    // dynamic add data for one line
    public void addEntry(float number){

        // when first data
        if(lineDataSet.getEntryCount()==0){
            lineData.addDataSet(lineDataSet);
        }
        lineChart.setData(lineData);

        // if too much data, clear time list
        if(timeList.size()>101) {
            timeList.clear();
        }

        timeList.add(df.format(System.currentTimeMillis()));
        Entry entry=new Entry(lineDataSet.getEntryCount(),number);
        lineData.addEntry(entry,0);
        //notify data change
        lineData.notifyDataChanged();
        lineChart.notifyDataSetChanged();

        lineChart.setVisibleXRangeMaximum(100);
        lineChart.moveViewToX(lineData.getEntryCount()-90);
    }

    //dynamic add data for multi lines
    public void addEntry(List<Float> numbers){
        if(lineDataSets.get(0).getEntryCount()==0){
            lineData = new LineData(lineDataSets);
            lineChart.setData(lineData);
        }
        if(timeList.size()>101)
        {
            timeList.clear();
        }
        timeList.add(df.format(System.currentTimeMillis()));
        for(int i=0;i<numbers.size();i++){
            Entry entry = new Entry(lineDataSet.getEntryCount(), numbers.get(i));
            lineData.addEntry(entry,i);
            lineData.notifyDataChanged();
            lineChart.notifyDataSetChanged();
            lineChart.setVisibleXRangeMaximum(100);
            lineChart.moveViewToX(lineData.getEntryCount()-90);
        }
    }

    public void setYAxis(float max, float min, int labelCount){
        if(max<min){
            return;
        }
        leftAxis.setAxisMinimum(min);
        leftAxis.setAxisMaximum(max);
        leftAxis.setLabelCount(labelCount);

        rightAxis.setAxisMinimum(min);
        rightAxis.setAxisMaximum(max);
        rightAxis.setLabelCount(labelCount);
        lineChart.invalidate();
    }

    public void setHighLimit(float high, String name, int color){
        if(name == null){
            name = "HighLimit";
        }
        LimitLine heightLimit = new LimitLine(high,name);
        heightLimit.setLineWidth(4f);
        heightLimit.setTextSize(10f);
        heightLimit.setLineColor(color);
        heightLimit.setTextColor(color);
        leftAxis.addLimitLine(heightLimit);
        lineChart.invalidate();
    }

    public void setLowLimit(float low, String name, int color){
        if(name == null){
            name = "LowLimit";
        }
        LimitLine heightLimit = new LimitLine(low,name);
        heightLimit.setLineWidth(4f);
        heightLimit.setTextSize(10f);
        heightLimit.setLineColor(color);
        heightLimit.setTextColor(color);
        leftAxis.addLimitLine(heightLimit);
        lineChart.invalidate();
    }

    public void setDescription(String str){
        Description description = new Description();
        description.setText(str);
        lineChart.setDescription(description);
        lineChart.invalidate();
    }
}
