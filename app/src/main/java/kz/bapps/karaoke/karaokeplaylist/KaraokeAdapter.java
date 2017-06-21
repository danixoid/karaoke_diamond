package kz.bapps.karaoke.karaokeplaylist;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by danixoid on 16.06.17.
 */

class KaraokeAdapter  extends RecyclerView.Adapter<KaraokeAdapter.ViewHolder> {

    private final List<Karaoke> Karaokees;
    private final OnListFragmentInteractionListener mListener;

    public KaraokeAdapter(Context applicationContext, List<Karaoke> items, OnListFragmentInteractionListener listener) {
        Karaokees = items;
        mListener = listener;
    }

    public Karaoke getItem(int position) {
        return Karaokees.get(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.karaoke_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = Karaokees.get(position);
        holder.mId.setText(Karaokees.get(position).getId());
        holder.mArtist.setText(Karaokees.get(position).getArtist());
        holder.mSong.setText(Karaokees.get(position).getSong());
        holder.mQuality.setText(Karaokees.get(position).getQuality());

//        if (null != mListener && holder.mItem.isOwner()) {
//            holder.mImgBtnEdit.setImageResource(android.R.drawable.ic_menu_edit);
//            holder.mImgBtnEdit.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    mListener.onEditKaraoke(holder.mItem.getId());
//                }
//            });
//        }

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.getWindow(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return Karaokees.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView mId;
        final TextView mArtist;
        final TextView mSong;
        final TextView mQuality;
        Karaoke mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            mId = (TextView) view.findViewById(R.id.tvID);
            mArtist = (TextView) view.findViewById(R.id.tvArtist);
            mSong = (TextView) view.findViewById(R.id.tvSong);
            mQuality = (TextView) view.findViewById(R.id.tvQuality);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mArtist.getText() + " " + mSong.getText() +"'";
        }
    }
}
