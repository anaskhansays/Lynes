package com.cardboard.lynes.adapters;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cardboard.lynes.R;
import com.cardboard.lynes.entities.Note;
import com.cardboard.lynes.listeners.NotesListener;
import com.makeramen.roundedimageview.RoundedImageView;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<Note> notes;
    private NotesListener notesListener;
    private Timer timer;
    private List<Note> notesrc;

    public NotesAdapter(List<Note> notes, NotesListener notesListener){
        this.notes=notes;
        this.notesListener=notesListener;
        notesrc=notes;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.note_container,parent,false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.setNote(notes.get(position));
        holder.noteContainerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notesListener.onNoteClicked(notes.get(position),position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder{
        TextView textTitle, textSubT, textDateNTime;
        LinearLayout noteContainerLayout;
        RoundedImageView roundedImageView;
        NoteViewHolder(@NonNull View itemView){
            super(itemView);

            textTitle=itemView.findViewById(R.id.txtNTitle);
            textSubT=itemView.findViewById(R.id.txtNSub);
            textDateNTime=itemView.findViewById(R.id.txtDnT);
            noteContainerLayout=itemView.findViewById(R.id.layoutNote);
            roundedImageView=itemView.findViewById(R.id.imageNote);

        }

        void setNote(Note note){
            textTitle.setText(note.getTitle());
            if(note.getSubtitle().trim().isEmpty()){
                textSubT.setVisibility(View.GONE);
            }else{
                textSubT.setText(note.getSubtitle());
            }
            textDateNTime.setText(note.getDate_time());
            GradientDrawable gradientDrawable=(GradientDrawable)noteContainerLayout.getBackground();
            if (note.getColor()!=null){
                gradientDrawable.setColor(Color.parseColor(note.getColor()));
            }else{
                gradientDrawable.setColor(Color.parseColor("#333333"));
            }

            if(note.getImage_path()!=null){
                roundedImageView.setImageBitmap(BitmapFactory.decodeFile(note.getImage_path()));
                roundedImageView.setVisibility(View.VISIBLE);
            }
            else{
                roundedImageView.setVisibility(View.GONE);
            }

        }
    }


    public void searchNotes(final String searchstr){
        timer=new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(searchstr.trim().isEmpty()){
                    notes=notesrc;
                }else{
                    ArrayList<Note> temp=new ArrayList<>();
                    for(Note n:notesrc){
                        if(n.getTitle().toLowerCase().contains(searchstr.toLowerCase())
                                || n.getSubtitle().contains(searchstr.toLowerCase())
                                || n.getNote_text().contains(searchstr.toLowerCase())){
                            temp.add(n);

                        }
                    }
                    notes=temp;
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });

            }
        },500);
    }

    public void cancelTimer(){
        if(timer!=null){
            timer.cancel();
        }
    }
}
