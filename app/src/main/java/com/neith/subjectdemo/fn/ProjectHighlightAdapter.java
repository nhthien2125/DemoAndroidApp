package com.neith.subjectdemo.fn;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.neith.subjectdemo.R;
import com.neith.subjectdemo.fn.model.ProjectHighlight;
import java.util.List;

public class ProjectHighlightAdapter extends RecyclerView.Adapter<ProjectHighlightAdapter.ViewHolder> {

    private List<ProjectHighlight> projectList;

    public ProjectHighlightAdapter(List<ProjectHighlight> projectList) {
        this.projectList = projectList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project_highlight, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProjectHighlight project = projectList.get(position);
        holder.tvProjectName.setText(project.getName());
        holder.tvProjectCode.setText("Code: " + project.getCode());
        holder.tvRevenue.setText(project.getRevenue());
        holder.tvCost.setText("Cost: " + project.getCost());
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProjectName, tvProjectCode, tvRevenue, tvCost;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
            tvProjectCode = itemView.findViewById(R.id.tvProjectCode);
            tvRevenue = itemView.findViewById(R.id.tvRevenue);
            tvCost = itemView.findViewById(R.id.tvCost);
        }
    }
}
