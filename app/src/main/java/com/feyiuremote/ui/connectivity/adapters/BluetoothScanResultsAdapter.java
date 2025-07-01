package com.feyiuremote.ui.connectivity.adapters;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.feyiuremote.R;

import java.util.ArrayList;

public class BluetoothScanResultsAdapter extends BaseAdapter {

    Context c;
    ArrayList<ScanResult> results;

    public BluetoothScanResultsAdapter(Context c) {
        this.c = c;
        this.results = new ArrayList<>();
    }

    public void setResults(ArrayList<ScanResult> results) {
        this.results = results;
    }

    @Override
    public int getCount() {
        return results.size();
    }

    @Override
    public ScanResult getItem(int i) {
        return this.results.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(c).inflate(R.layout.fragment_bt_scan_item, viewGroup, false);
        }

        TextView mTextDeviceTitle = view.findViewById(R.id.textDeviceTitle);
        TextView mTextDeviceAddress = view.findViewById(R.id.textDeviceAddress);

        mTextDeviceTitle.setText(this.results.get(i).getScanRecord().getDeviceName() != null
                ? this.results.get(i).getScanRecord().getDeviceName() :
                "Unknown");

        mTextDeviceAddress.setText(this.results.get(i).getDevice().getAddress());

        return view;
    }

}