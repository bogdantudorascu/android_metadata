package com.example.uni.photoristic;

import android.support.v7.widget.RecyclerView;

/**
 * Class used to pass the view holder and calling position to async methods
 */
public class HolderPositionAndPath {
    int position;
    RecyclerView.ViewHolder holder;

    public HolderPositionAndPath(int position, RecyclerView.ViewHolder holder) {
        this.position = position;
        this.holder = holder;
    }

    public RecyclerView.ViewHolder getHolder() {
        return holder;
    }

    public int getPosition() {
        return position;
    }
}