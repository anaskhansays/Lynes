package com.cardboard.lynes.listeners;

import com.cardboard.lynes.entities.Note;

public interface NotesListener {

    void onNoteClicked(Note note, int position);
}
