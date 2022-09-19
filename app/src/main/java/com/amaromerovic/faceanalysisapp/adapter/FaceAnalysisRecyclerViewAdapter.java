package com.amaromerovic.faceanalysisapp.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amaromerovic.faceanalysisapp.databinding.ItemBinding;
import com.amaromerovic.faceanalysisapp.model.FaceModel;

import java.util.List;

public class FaceAnalysisRecyclerViewAdapter extends RecyclerView.Adapter<FaceAnalysisRecyclerViewAdapter.ViewHolder> {

    private final List<FaceModel> faceModelList;

    public FaceAnalysisRecyclerViewAdapter(List<FaceModel> faceModelList) {
        this.faceModelList = faceModelList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.binding.faceID.setText(String.format("Face: %s", faceModelList.get(position).getId()));
        holder.binding.faceAnalysisResult.setText(faceModelList.get(position).getText());
    }

    @Override
    public int getItemCount() {
        return faceModelList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemBinding binding;

        public ViewHolder(@NonNull ItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
