package com.cardboard.lynes.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cardboard.lynes.R;
import com.cardboard.lynes.database.NotesDB;
import com.cardboard.lynes.entities.Note;
import com.cardboard.lynes.utils.FileUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NewNoteActivity extends AppCompatActivity {

    private EditText inputNoteTitle, inputNoteSub, inputNoteText;
    private TextView txtDateTime;
    private String selectedNoteBgColor;
    private View subIndicator;
    private ImageView imageNote;
    TextView textWebURL;
    LinearLayout layoutWebURL;
    private String selectedImagePath;

    private static final int REQUEST_CODE_STORAGE_PERMISSION=1;
    private static final int REQUEST_CODE_SELECT_IMAGE=2;

    private AlertDialog urlDialog;
    private AlertDialog deleteDialog;
    private Note alreadyAvailableNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_note);

        ImageView imageBackBtn=findViewById(R.id.imgBack);
        imageBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        inputNoteTitle=findViewById(R.id.inputNoteTitle);
        inputNoteSub=findViewById(R.id.inputSubtitle);
        inputNoteText=findViewById(R.id.inputNoteText);
        txtDateTime=findViewById(R.id.txtdateTime);
        subIndicator=findViewById(R.id.vSubIndicator);
        imageNote=findViewById(R.id.inputNoteImage);
        textWebURL=findViewById(R.id.textWebURL);
        layoutWebURL=findViewById(R.id.layoutWebURL);

        txtDateTime.setText(
                new SimpleDateFormat("EEEE,dd MMMM yyyy HH:mm a", Locale.getDefault())
                .format(new Date())
        );

        ImageView imgsavebtn=findViewById(R.id.imgSaveBtn);

        imgsavebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        selectedNoteBgColor="#333333";
        selectedImagePath="";

        if(getIntent().getBooleanExtra("isViewOrUpdate",false)){
            alreadyAvailableNote=(Note)getIntent().getSerializableExtra("note");
            setViewOrUpdate();
        }

        findViewById(R.id.imageRemoveWebUrl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textWebURL.setText(null);
                layoutWebURL.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.imageRemoveImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageNote.setImageBitmap(null);
                imageNote.setVisibility(View.GONE);
                findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
                selectedImagePath="";
            }
        });


        if(getIntent().getBooleanExtra("isFromQuickActions",false)){
            String type=getIntent().getStringExtra("quickActionType");
            if(type!=null){
                if(type.equals("image")){
                    selectedImagePath=getIntent().getStringExtra("imagePath");
                    imageNote.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath));
                    imageNote.setVisibility(View.VISIBLE);
                    findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                }else if(type.equals("url")){
                    textWebURL.setText(getIntent().getStringExtra("URL"));
                    layoutWebURL.setVisibility(View.VISIBLE);
                    findViewById(R.id.imageRemoveWebUrl).setVisibility(View.VISIBLE);
                }
            }
        }



        triggerMiscBgSheet();
        setSubIndicatorColor();
    }


    private void setViewOrUpdate(){

        inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        inputNoteSub.setText(alreadyAvailableNote.getSubtitle());
        inputNoteText.setText(alreadyAvailableNote.getNote_text());
        txtDateTime.setText(alreadyAvailableNote.getDate_time());

        if(alreadyAvailableNote.getImage_path()!=null && !alreadyAvailableNote.getImage_path().trim().isEmpty()){
            imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImage_path()));
            imageNote.setVisibility(View.VISIBLE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
        }

        if(alreadyAvailableNote.getWeb_link()!=null && !alreadyAvailableNote.getWeb_link().trim().isEmpty()){
            textWebURL.setText(alreadyAvailableNote.getWeb_link());
            layoutWebURL.setVisibility(View.VISIBLE);
            findViewById(R.id.imageRemoveWebUrl).setVisibility(View.VISIBLE);
        }

    }


    private void saveNote(){
        if(inputNoteTitle.getText().toString().trim().isEmpty()){
            Toast.makeText(this, "Note Title can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }else if (inputNoteSub.getText().toString().trim().isEmpty() && inputNoteText.getText().toString().trim().isEmpty()){
            Toast.makeText(this, "Note can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        final Note note=new Note();
        note.setTitle(inputNoteTitle.getText().toString());
        note.setSubtitle(inputNoteSub.getText().toString());
        note.setNote_text(inputNoteText.getText().toString());
        note.setDate_time(txtDateTime.getText().toString());
        note.setColor(selectedNoteBgColor);
        note.setImage_path(selectedImagePath);

        if(layoutWebURL.getVisibility()==View.VISIBLE){
            note.setWeb_link(textWebURL.getText().toString());
        }

        if(alreadyAvailableNote!=null){
            note.setId(alreadyAvailableNote.getId());
        }

        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void,Void,Void>{

            @Override
            protected Void doInBackground(Void... voids) {
                NotesDB.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Intent intent=new Intent();
                setResult(RESULT_OK,intent);
                finish();
            }
        }

        new SaveNoteTask().execute();
    }

    private void triggerMiscBgSheet(){
        final LinearLayout layout_misc=findViewById(R.id.layoutMiscellaneous);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior=BottomSheetBehavior.from(layout_misc);
        layout_misc.findViewById(R.id.txtMisc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bottomSheetBehavior.getState()!=BottomSheetBehavior.STATE_EXPANDED){
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }else{
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        final ImageView imageColor1=layout_misc.findViewById(R.id.imageColor1);
        final ImageView imageColor2=layout_misc.findViewById(R.id.imageColor2);
        final ImageView imageColor3=layout_misc.findViewById(R.id.imageColor3);
        final ImageView imageColor4=layout_misc.findViewById(R.id.imageColor4);
        final ImageView imageColor5=layout_misc.findViewById(R.id.imageColor5);

        layout_misc.findViewById(R.id.viewColor1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteBgColor="#333333";
                imageColor1.setImageResource(R.drawable.ic_done);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                setSubIndicatorColor();
            }
        });


        layout_misc.findViewById(R.id.viewColor2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteBgColor= "#FDBE3B";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(R.drawable.ic_done);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                setSubIndicatorColor();
            }
        });


        layout_misc.findViewById(R.id.viewColor3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteBgColor= "#FF4842";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(R.drawable.ic_done);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                setSubIndicatorColor();
            }
        });


        layout_misc.findViewById(R.id.viewColor4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteBgColor= "#3A52FC";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(R.drawable.ic_done);
                imageColor5.setImageResource(0);
                setSubIndicatorColor();
            }
        });


        layout_misc.findViewById(R.id.viewColor5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteBgColor= "#000000";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(R.drawable.ic_done);
                setSubIndicatorColor();
            }
        });

        if(alreadyAvailableNote!=null && alreadyAvailableNote.getColor()!=null && !alreadyAvailableNote.getColor().trim().isEmpty()){

            switch (alreadyAvailableNote.getColor()){
                case "#FDBE3B":
                    layout_misc.findViewById(R.id.viewColor2).performClick();
                    break;
                case "#FF4842":
                    layout_misc.findViewById(R.id.viewColor3).performClick();
                    break;
                case "#3A52FC":
                    layout_misc.findViewById(R.id.viewColor4).performClick();
                    break;
                case "#000000":
                    layout_misc.findViewById(R.id.viewColor5).performClick();
                    break;
            }

        }

        if (alreadyAvailableNote!=null){
            layout_misc.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            layout_misc.findViewById(R.id.layoutDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    pullDeleteDialog();
                }
            });
        }


        layout_misc.findViewById(R.id.layoutAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                if(ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(
                            NewNoteActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_CODE_STORAGE_PERMISSION
                    );
                } else{
                    selectImage();
                }
            }
        });

        layout_misc.findViewById(R.id.layoutAddUrl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                pullURLDialog();
            }
        });
    }



    private void setSubIndicatorColor(){
        GradientDrawable gradientDrawable=(GradientDrawable)subIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteBgColor));
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==REQUEST_CODE_SELECT_IMAGE && resultCode==RESULT_OK){
            if(data!=null){
                Uri selectedImageUri=data.getData();
                if (selectedImageUri!=null){
                    try {
                        InputStream inputStream=getContentResolver().openInputStream(selectedImageUri);
                        Bitmap bitmap= BitmapFactory.decodeStream(inputStream);
                        imageNote.setImageBitmap(bitmap);
                        imageNote.setVisibility(View.VISIBLE);
                        findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);

                        FileUtils fileUtils=new FileUtils(NewNoteActivity.this);

                        selectedImagePath= fileUtils.getPath(selectedImageUri);
                    } catch (Exception e) {
                        Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

        }
    }

    private void pullURLDialog(){
            if (urlDialog==null){
                AlertDialog.Builder builder=new AlertDialog.Builder(NewNoteActivity.this);
                View view= LayoutInflater.from(this).inflate(R.layout.layout_add_url, (ViewGroup)findViewById(R.id.layoutURLDialogContainer));
                builder.setView(view);
                urlDialog=builder.create();

                if (urlDialog.getWindow()!=null){
                    urlDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                }
                final EditText inputURL=view.findViewById(R.id.inputURL);

                view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(inputURL.getText().toString().trim().isEmpty()){
                            Toast.makeText(NewNoteActivity.this, "Enter URL!", Toast.LENGTH_SHORT).show();
                        }else if(!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()){
                            Toast.makeText(NewNoteActivity.this, "Enter Valid URL", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            textWebURL.setText(inputURL.getText().toString());
                            layoutWebURL.setVisibility(View.VISIBLE);
                            findViewById(R.id.imageRemoveWebUrl).setVisibility(View.VISIBLE);
                            urlDialog.dismiss();
                        }
                    }
                });

                view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        urlDialog.dismiss();
                    }
                });
            }
            urlDialog.show();
    }


    private void pullDeleteDialog(){
        if(deleteDialog==null){
            AlertDialog.Builder delBuilder=new AlertDialog.Builder(NewNoteActivity.this);
            View view=LayoutInflater.from(this)
                    .inflate(R.layout.layout_delete_note,
                            (ViewGroup)findViewById(R.id.layoutDeleteContainer));
            delBuilder.setView(view);
            deleteDialog=delBuilder.create();

            if(deleteDialog.getWindow()!=null){
                deleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.txtDeleteNoteConfirm).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    @SuppressLint("StaticFieldLeak")
                    class DeleteNoteTask extends AsyncTask<Void,Void,Void>{

                        @Override
                        protected Void doInBackground(Void... voids) {
                            NotesDB.getDatabase(getApplicationContext()).noteDao().deleteNote(alreadyAvailableNote);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            Intent intent=new Intent();
                            intent.putExtra("isNoteDeleted",true);
                            setResult(RESULT_OK,intent);
                            finish();
                        }
                    }

                    new DeleteNoteTask().execute();
                }
            });

            view.findViewById(R.id.txtDeleteNoteCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        deleteDialog.dismiss();
                }
            });

        }

        deleteDialog.show();
    }
}