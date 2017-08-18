package ru.neosvet.wallpaper.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.neosvet.wallpaper.R;

/**
 * Created by NeoSvet on 14.07.2017.
 */

public class PagesAdapter extends RecyclerView.Adapter<PagesAdapter.ViewHolder> {
    public interface OnPageClickListener {
        void onPageClick(int page);
    }

    private Context context;
    private PagesAdapter.OnPageClickListener mListener;
    private int start, count, select;

    public PagesAdapter(PagesAdapter.OnPageClickListener mListener, int count, int select) {
        this.mListener = mListener;
        this.count = count;
        if (count > 20)
            start = count - 21;
        else
            start = 0;
        this.select = select;
    }

    @Override
    public PagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.little_text_item, parent, false);
        return new PagesAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PagesAdapter.ViewHolder holder, int position) {
        boolean empty = true;
        if (position == 0 && start > 0) {
            if (start > 11)
                position = -10;
            else
                position = 0;
            holder.page.setText("<");
            empty = false;
        } else if (position == 21) {
            if (count >= start + 30)
                position = 30;
            else
                position = count - start;
            empty = false;
            holder.page.setText(">");
        } else if (position + start == 0)
            start = 1;
        final int page = position + start;
        if (empty)
            holder.page.setText(String.valueOf(page));
        if (page == select)
            holder.page.setBackgroundResource(R.drawable.cell_select);
        else
            holder.page.setBackgroundResource(R.drawable.cell_none);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onPageClick(page);
            }
        });
    }

    @Override
    public int getItemCount() {
        return (count > 19 ? 22 : count);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView page;

        ViewHolder(View itemView) {
            super(itemView);
            page = (TextView) itemView.findViewById(R.id.text);
        }
    }
}
