package com.example.myserver;

import static com.example.myserver.MainActivity.ips;
import static com.example.myserver.MainActivity.recyclerViewClickEnabled;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    public RecyclerViewAdapter() {
    }

    @NonNull
    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.list_item_ip_address, parent, false);
        RecyclerViewAdapter.ViewHolder viewHolder = new RecyclerViewAdapter.ViewHolder(listItem);

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recyclerViewClickEnabled) {
                    MainActivity.ipToSendFileTo = (String) viewHolder.ipAddressTextView.getText();
                }
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {
        holder.ipAddressTextView.setText(ips.get(position));
        if (recyclerViewClickEnabled){
            holder.ipAddressTextView.setTextColor(Color.parseColor("#FF6200EE"));
        }else {
            holder.ipAddressTextView.setTextColor(Color.parseColor("#888888"));
        }
    }

    @Override
    public int getItemCount() {
        return ips.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView ipAddressTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ipAddressTextView = itemView.findViewById(R.id.ipAddressTextView);
        }
    }
}
