package com.sustbus.driver;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sustbus.driver.util.CallBack;
import com.sustbus.driver.util.RotateBitmap;
import com.sustbus.driver.util.UserInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import de.hdodenhof.circleimageview.CircleImageView;
import studio.carbonylgroup.textfieldboxes.SimpleTextChangedWatcher;
import studio.carbonylgroup.textfieldboxes.TextFieldBoxes;

public class ProfileActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ProfileActivity";
    private static final int REQUESTING_DP = 1001;
    private static final int REQUESTING_ID = 1002;

    UserInfo userInfo;
    private Button updateProfileBtn;
    private Button idChooserBtn;
    private Button dpChooserBtn;
    private TextFieldBoxes userNameTf;
    private TextFieldBoxes regiNoTf;
    private TextView profileHelperTv;
    private TextView idAvailibilityTv;
    private TextView changePasswordTv;
    private ImageButton backBtn;
    private EditText userNameEt;
    private EditText regiNoEt;
    private StorageReference storageReference;
    private CircleImageView dpEv;
    private String userName = null, regiNo = null;
    private boolean userNameOk;
    private boolean regiNoOk;
    private DocumentReference db;
    private ListenerRegistration listener;
    private Uri dpFilePath = null, idFilePath = null;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_v2);

        updateProfileBtn = findViewById(R.id.update_profile_btn);
        userNameTf = findViewById(R.id.profile_username_tf);
        regiNoTf = findViewById(R.id.profile_regino_tf);
        userNameEt = findViewById(R.id.profile_username_et);
        regiNoEt = findViewById(R.id.profile_regino_et);
        dpEv = findViewById(R.id.profile_user_image_ev);
        idChooserBtn = findViewById(R.id.profile_id_chooser_btn);
        idAvailibilityTv = findViewById(R.id.profile_id_availability);
        profileHelperTv = findViewById(R.id.profile_helper_tv);
        changePasswordTv = findViewById(R.id.profile_change_password_tv);
        backBtn = findViewById(R.id.profile_back_btn);
        dpChooserBtn = findViewById(R.id.dp_chooser_button);
        if(getIntent().getBooleanExtra("newId",false)){
            Log.d(TAG, "subscription: " );
            if(FirebaseAuth.getInstance().getCurrentUser()!=null)
            FirebaseMessaging.getInstance().subscribeToTopic(FirebaseAuth.getInstance().getUid());
        }
        userInfo = UserInfo.getInstance();
        initUi();


        userNameTf.setSimpleTextChangeWatcher(new SimpleTextChangedWatcher() {
            @Override
            public void onTextChanged(String theNewText, boolean isError) {
                userNameValidator(theNewText);
            }
        });
        regiNoTf.setSimpleTextChangeWatcher(new SimpleTextChangedWatcher() {
            @Override
            public void onTextChanged(String theNewText, boolean isError) {
                regiNoValidator(theNewText);
            }
        });
        updateProfileBtn.setOnClickListener(this);
        changePasswordTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initChangePasswordAD();
            }
        });

        progressDialog = new ProgressDialog(this);
    }

    private void initChangePasswordAD() {
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.forgot_password_alertdialog, null);
        new AlertDialog.Builder(ProfileActivity.this)
                .setTitle("Change Password")
                .setView(view)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText currentPasswordEt, newPasswordEt, confirmPasswordEt;
                        currentPasswordEt = view.findViewById(R.id.change_pass_ad_current_password);
                        newPasswordEt = view.findViewById(R.id.change_pass_ad_new_password);
                        confirmPasswordEt = view.findViewById(R.id.change_pass_ad_confirm_password);
                        String currentPass = currentPasswordEt.getText().toString().trim();
                        FirebaseAuth.getInstance().getCurrentUser()
                                .reauthenticate(EmailAuthProvider.getCredential(userInfo.getEmail(), currentPass))
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        Log.d(TAG, "onComplete: asche");
                                        if (task.isSuccessful()) {
                                            if (newPasswordEt.getText().toString().equals(confirmPasswordEt.getText().toString())) {
                                                FirebaseAuth.getInstance().getCurrentUser().updatePassword(newPasswordEt.getText().toString())
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    Toast.makeText(ProfileActivity.this,
                                                                            "Password Changed\nPlease Log in again",
                                                                            Toast.LENGTH_SHORT).show();
                                                                    userInfo.reset();
                                                                    FirebaseAuth.getInstance().signOut();
                                                                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                                    startActivity(intent);
                                                                    finish();
                                                                } else {
                                                                    Toast.makeText(ProfileActivity.this,
                                                                            task.getException().getMessage(),
                                                                            Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });
                                            } else {
                                                Toast.makeText(ProfileActivity.this, "passwords do not match", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(ProfileActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                })
                .setNegativeButton("cancel", null)
                .show();
    }

    private void loadImage() {
        assert userInfo.getUrl() != null;
        try {
            String img = userInfo.getUrl();
            byte[] imageAsBytes = Base64.decode(img.getBytes(), Base64.DEFAULT);
            dpEv.setImageBitmap(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length));
        } catch (NullPointerException e) {
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "onSuccess: " + e.getMessage());
        }
    }

    private void userNameValidator(String theNewText) {
        if(theNewText == null){
            userNameOk = false;
            return;
        }
        userName = theNewText;
        if (theNewText.length() < 4) {
            userNameTf.setHelperText("must contain 6 letters at least");
            userNameTf.setHelperTextColor(ContextCompat.getColor(com.sustbus.driver.ProfileActivity.this, R.color.A400red));
            userNameTf.setPrimaryColor(ContextCompat.getColor(ProfileActivity.this, R.color.A400red));
            userNameOk = false;
        } else {
            userNameTf.setHelperText(" ");
            userNameTf.setPrimaryColor(ContextCompat.getColor(ProfileActivity.this, R.color.sust));
            userNameOk = true;
        }
    }

    private void regiNoValidator(String theNewText) {
        if(theNewText == null){
            regiNoOk = false;
            return;
        }
        regiNo = theNewText;
        if (theNewText.length() != 10) {
            regiNoTf.setHelperText("registration number must contain 10 numbers");
            regiNoTf.setHelperTextColor(ContextCompat.getColor(ProfileActivity.this, R.color.A400red));
            regiNoTf.setPrimaryColor(ContextCompat.getColor(ProfileActivity.this, R.color.A400red));
            regiNoOk = false;
        } else {
            regiNoTf.setHelperText("");
            regiNoTf.setPrimaryColor(ContextCompat.getColor(ProfileActivity.this, R.color.sust));
            regiNoOk = true;
        }
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.update_profile_btn) {
            Log.d(TAG, "onClick: " + "update button clicked ");
            progressDialog.setMessage("Updating Profile");
            progressDialog.setCancelable(false);
            progressDialog.show();
            if (userNameOk && regiNoOk) {

                if (dpFilePath != null) {
                    Log.d(TAG, "onClick: dp upload init");
                    BackgroundImageUpload backgroundImageUpload = new BackgroundImageUpload("dp");
                    backgroundImageUpload.execute(dpFilePath);
                    dpFilePath = null;
                }
                if (idFilePath != null) {
                    BackgroundImageUpload backgroundImageUpload = new BackgroundImageUpload("id");
                    backgroundImageUpload.execute(idFilePath);
                    idFilePath = null;
                }
                updateRestOfTheData();
            } else {
                progressDialog.hide();
                Toast.makeText(ProfileActivity.this, "enter all fields correctly", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateRestOfTheData() {

        userInfo.setUserName(userName);
        userInfo.setRegiNo(regiNo);
        if (!userInfo.isProfileCompleted()) {
            permissionPending();

            userInfo.setProfileCompleted(true);
            Toast.makeText(ProfileActivity.this, "Requesting permission", Toast.LENGTH_SHORT).show();
        } else if (userInfo.isPermitted()) {
            permitted();
            Toast.makeText(ProfileActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
        }
        userInfo.updateToDbase(new CallBack() {
            @Override
            public void ok() {
                Log.d(TAG, "onSuccess: " + "profile updated");
                if(progressDialog.isShowing())progressDialog.hide();
            }

            @Override
            public void notOk() {
                Toast.makeText(ProfileActivity.this, "something went wrong", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: " + userName + " " + regiNo);

        if (requestCode == REQUESTING_DP && resultCode == RESULT_OK &&
                data != null && data.getData() != null) {
            Log.d(TAG, "onActivityResult: " + data + "dp fetched");
            dpChooserBtn.setText("change");
            dpFilePath = data.getData();
//            Glide.with(this)
//                    .load(dpFilePath)
//                    .into(dpEv);
            try {
                dpEv.setImageBitmap(MediaStore.Images.Media.getBitmap(ProfileActivity.this.getContentResolver(), dpFilePath));
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (requestCode == REQUESTING_ID && resultCode == RESULT_OK &&
                data != null && data.getData() != null) {
            Log.d(TAG, "onActivityResult: " + data + "id fetched");
            idFilePath = data.getData();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        userNameEt.setText(userName);
        regiNoEt.setText(regiNo);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void initUi() {
        backBtn.setVisibility(View.GONE);
        userNameValidator(userInfo.getUserName());
        regiNoValidator(userInfo.getRegiNo());
        if (!userInfo.isPermitted() && !userInfo.isProfileCompleted()) {
            updateProfileBtn.setEnabled(true);
            profileHelperTv.setText("Complete all the fields and request for permission");
            updateProfileBtn.setText("Request Permission");
            idChooserBtn.setVisibility(View.VISIBLE);
            idAvailibilityTv.setText("ID Not Detected");
            updateProfileBtn.setBackground(ContextCompat.getDrawable(ProfileActivity.this, R.drawable.custom_button));

        } else if (!userInfo.isPermitted() && userInfo.isProfileCompleted()) {
            permissionPending();
        } else if (userInfo.isPermitted()) {
            permitted();
        }
        userName = userInfo.getUserName();
        regiNo = userInfo.getRegiNo();
        userNameEt.setText(userName);
        regiNoEt.setText(regiNo);
        loadImage();
    }

    private void permitted() {
        updateProfileBtn.setEnabled(true);
        updateProfileBtn.setText("Update Profile");
        idAvailibilityTv.setText("ID Confirmed");
        idChooserBtn.setVisibility(View.GONE);
        profileHelperTv.setVisibility(View.GONE);
        backBtn.setVisibility(View.VISIBLE);
        updateProfileBtn.setBackground(ContextCompat.getDrawable(ProfileActivity.this, R.drawable.custom_button));
    }

    private void permissionPending() {
        updateProfileBtn.setEnabled(false);
        idChooserBtn.setVisibility(View.GONE);
        profileHelperTv.setText("Please wait, this may take upto 48 hours");
        idAvailibilityTv.setText(" ID will be checked manually");
        updateProfileBtn.setText("your request is being proceed...");
        updateProfileBtn.setBackground(ContextCompat.getDrawable(ProfileActivity.this, R.drawable.custom_button));
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        progressDialog.dismiss();
        progressDialog = null;
        listener = null;
        db = null;
        storageReference = null;
        System.gc();
    }
    

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        backButtonPressed(null);
    }

    public void backButtonPressed(View view) {
        if(!userInfo.isProfileCompleted() || !userInfo.isPermitted()){
            finishAndRemoveTask();
            return;
        }
        if (!userInfo.getUserName().equals(userName) || !userInfo.getRegiNo().equals(regiNo) ||
                dpFilePath != null || idFilePath != null) {
            new AlertDialog.Builder(ProfileActivity.this)
                    .setTitle("Save Profile")
                    .setMessage("or your changes will be lost")
                    .setPositiveButton("yes", null)
                    .setNegativeButton("no", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).show();
        } else finish();
    }

    public void idChooserButtonPressed(View view) {
        Log.d(TAG, "onClick: " + "id chooser button clicked " + userName + " " + regiNo);
        idChooserBtn.setText("change");
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select ID"), REQUESTING_ID);
    }

    public void dpChooserButtonPressed(View view) {
        Log.d(TAG, "dpChooserButtonPressed: ");
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Photo"), REQUESTING_DP);
    }

    public class BackgroundImageUpload extends AsyncTask<Uri, Integer, byte[]> {
        Bitmap bitmap;
        String message;

        BackgroundImageUpload(String message) {
            this.message = message;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "onPreExecute: ");
        }

        @Override
        protected byte[] doInBackground(Uri... uris) {
            Log.d(TAG, "doInBackground: started");

            if (bitmap == null) {
                try {
                    RotateBitmap rotateBitmap = new RotateBitmap();
                    bitmap = rotateBitmap.HandleSamplingAndRotationBitmap(ProfileActivity.this, uris[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            byte[] bytes;
            bytes = getBytesFromBitmap(bitmap, 20);
            String encodedImage = Base64.encodeToString(bytes, Base64.NO_WRAP);

            if (message.equals("dp")) userInfo.setUrl(encodedImage);
            else if (message.equals("id")) userInfo.setIdUrl(encodedImage);
            userInfo.updateToDbase(new CallBack() {
                @Override
                public void ok() {
                    Log.d(TAG, "ok: uploaded via singleton");
                }

                @Override
                public void notOk() {
                    Log.d(TAG, "notOk: error");
                }
            });
            return bytes;
        }

        @Override
        protected void onPostExecute(byte[] bytes) {
            super.onPostExecute(bytes);
            Log.d(TAG, "onPostExecute: done " + bytes.length);
            
            Log.d(TAG, "onPostExecute: " + userInfo.toString());

        }
    }
    public static byte[] getBytesFromBitmap(Bitmap bitmap, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }
  
}
