package com.cardboard.lynes.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.cardboard.lynes.R;
import com.cardboard.lynes.adapters.NotesAdapter;
import com.cardboard.lynes.database.NotesDB;
import com.cardboard.lynes.entities.Note;
import com.cardboard.lynes.listeners.NotesListener;
import com.cardboard.lynes.utils.FileUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListener {

    public static final int REQUEST_CODE_ADD_NOTE=1;
    public static final int REQUEST_CODE_UPDATE_NOTE=2;
    public static final int REQUEST_CODE_SHOW_NOTE=3;
    public static final int REQUEST_CODE_SELECT_IMAGE=4;
    public static final int REQUEST_CODE_STORAGE_PERMISSION=5;

    private RecyclerView recyclerView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;

    private int noteClickedPosition=-1;

    private AlertDialog dialogAddURL;

    @SuppressLint("RestrictedApi")

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageAddNoteMain=findViewById(R.id.imgAddBtnMain);
        imageAddNoteMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        new Intent(MainActivity.this,NewNoteActivity.class),
                        REQUEST_CODE_ADD_NOTE
                );
            }
        });
        recyclerView=findViewById(R.id.notesList);
        recyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );

        noteList=new ArrayList<>();
        notesAdapter=new NotesAdapter(noteList,this);
        recyclerView.setAdapter(notesAdapter);
        getNotes(REQUEST_CODE_SHOW_NOTE,false);

        EditText inputSearch=findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(noteList.size()!=0){
                    notesAdapter.searchNotes(s.toString());
                }
            }
        });

        findViewById(R.id.imgAddNote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getApplicationContext(),NewNoteActivity.class),REQUEST_CODE_ADD_NOTE);
            }
        });

        findViewById(R.id.imgAddimage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_CODE_STORAGE_PERMISSION
                    );
                } else{
                    selectImage();
                }
            }
        });

        findViewById(R.id.imgAddLinks).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pullURLDialog();
            }
        });


    }

    private void selectImage(){

        Intent intent=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if(intent.resolveActivity(getPackageManager())!=null){
            startActivityForResult(intent,REQUEST_CODE_SELECT_IMAGE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==REQUEST_CODE_STORAGE_PERMISSION && grantResults.length>0){
            selectImage();
        }
        else{
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition=position;
        Intent intent=new Intent(getApplicationContext(),NewNoteActivity.class);
        intent.putExtra("isViewOrUpdate",true);
        intent.putExtra("note",note);
        startActivityForResult(intent,REQUEST_CODE_UPDATE_NOTE);
    }

    private void getNotes(final int requestCode, final boolean isNoteDeleted){

        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>> {
            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NotesDB
                        .getDatabase(getApplicationContext())
                        .noteDao().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
//                if (noteList.size()==0){
//                    noteList.addAll(notes);
//                    notesAdapter.notifyDataSetChanged();
//                }else{
//                    noteList.add(0, notes.get(0));
//                    notesAdapter.notifyItemInserted(0);
//                }
//                recyclerView.smoothScrollToPosition(0);

                // Log.d("ALL_NOTES",notes.toString());

                if (requestCode == REQUEST_CODE_SHOW_NOTE) {
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                } else if (requestCode == REQUEST_CODE_ADD_NOTE) {
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                    recyclerView.smoothScrollToPosition(0);
                } else if (requestCode == REQUEST_CODE_UPDATE_NOTE) {
                    noteList.remove(noteClickedPosition);
                    if (isNoteDeleted) {
                        notesAdapter.notifyItemRemoved(noteClickedPosition);
                    } else {
                        noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                        notesAdapter.notifyItemChanged(noteClickedPosition);
                    }
                }


            }
        }
        //this load all the notes and displays them on main screen
        new GetNotesTask().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==REQUEST_CODE_ADD_NOTE && resultCode==RESULT_OK){
            getNotes(REQUEST_CODE_ADD_NOTE,false);
        }else if(requestCode==REQUEST_CODE_UPDATE_NOTE && resultCode==RESULT_OK){
            if (data!=null){
                getNotes(REQUEST_CODE_UPDATE_NOTE,data.getBooleanExtra("isNoteDeleted",false));
            }
        }else if(requestCode==REQUEST_CODE_SELECT_IMAGE && resultCode==RESULT_OK){

            if(data!=null){
                Uri selectedImageUri=data.getData();
                if (selectedImageUri!=null){
                    try {
                        FileUtils fileUtils=new FileUtils(MainActivity.this);

                        String selectedImagePath= fileUtils.getPath(selectedImageUri);

                        Intent intent=new Intent(getApplicationContext(),NewNoteActivity.class);
                        intent.putExtra("isFromQuickActions",true);
                        intent.putExtra("quickActionType","image");
                        intent.putExtra("imagePath",selectedImagePath);
                        startActivityForResult(intent,REQUEST_CODE_ADD_NOTE);
                    } catch (Exception e) {
                        Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

        }
    }

    private void pullURLDialog(){
        if (dialogAddURL==null){
            AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
            View view= LayoutInflater.from(this).inflate(R.layout.layout_add_url, (ViewGroup)findViewById(R.id.layoutURLDialogContainer));
            builder.setView(view);
            dialogAddURL=builder.create();

            if (dialogAddURL.getWindow()!=null){
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            final EditText inputURL=view.findViewById(R.id.inputURL);

            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(inputURL.getText().toString().trim().isEmpty()){
                        Toast.makeText(MainActivity.this, "Enter URL!", Toast.LENGTH_SHORT).show();
                    }else if(!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()){
                        Toast.makeText(MainActivity.this, "Enter Valid URL", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        dialogAddURL.dismiss();
                        Intent intent=new Intent(getApplicationContext(),NewNoteActivity.class);
                        intent.putExtra("isFromQuickActions",true);
                        intent.putExtra("quickActionType","url");
                        intent.putExtra("URL",inputURL.getText().toString().trim());
                        startActivityForResult(intent,REQUEST_CODE_ADD_NOTE);


                    }
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogAddURL.dismiss();
                }
            });
        }
        dialogAddURL.show();
    }

}