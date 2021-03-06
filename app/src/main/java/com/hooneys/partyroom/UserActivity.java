package com.hooneys.partyroom;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.hooneys.partyroom.Application.MyApp;
import com.hooneys.partyroom.DO.User;

import java.util.HashMap;

public class UserActivity extends AppCompatActivity {
    private final String TAG = UserActivity.class.getSimpleName();

    private EditText nickName, pwd;
    private Spinner markerColor, loginType;
    private Button enterBtn;
    private int typeLogin;
    private float markerColorFloat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        init();
    }

    @Override
    protected void onStart() {
        super.onStart();

        boolean flag = isSavingRoom(MyApp.roomChannel);
        Log.i(TAG, "flag : " + flag);
        if(flag){;
            final String nickname = getRoomNickName(MyApp.roomChannel);
            MainActivity.rootRef
                    .child("User").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for(DataSnapshot node : dataSnapshot.getChildren()){
                                String key = node.getKey();
                                if(key.equals(nickname)){
                                    MyApp.myUser = (User) node.getValue(User.class);
                                    MyApp.roomNickName = getRoomNickName(MyApp.roomChannel);
                                    intentMainActivity();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }
    }

    private void init() {
        nickName = findViewById(R.id.user_nickname);
        pwd = findViewById(R.id.user_pwd);
        markerColor = findViewById(R.id.user_marker_spinner);
        markerColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] markerFloats = getResources().getStringArray(R.array.map_markers_float);
                markerColorFloat = Float.parseFloat(markerFloats[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                markerColorFloat = 0.0f;
            }
        });
        loginType = findViewById(R.id.user_login_type);
        loginType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                typeLogin = position;
                if(position == 0){
                    markerColor.setVisibility(View.VISIBLE);
                }else if(position == 1){
                    markerColor.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                typeLogin = 0;
            }
        });
        enterBtn = findViewById(R.id.user_enter_btn);
        enterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(typeLogin == 0){
                    //Add
                    addUser();
                }else if(typeLogin == 1){
                    //Enter
                    enterUser();
                }
            }
        });

    }

    private void enterUser() {
        final String s_nickname = nickName.getText().toString();
        final String s_pwd = pwd.getText().toString();

        if(s_nickname.trim().isEmpty() || s_pwd.trim().isEmpty()){
            Toast.makeText(this, "정보를 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        MainActivity.rootRef.child("User").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot == null || (dataSnapshot.getChildrenCount() < 1) ){
                    Toast.makeText(UserActivity.this,
                            "생성된 아이디가 아닙니다.", Toast.LENGTH_SHORT).show();
                    return;
                }


                for (DataSnapshot node : dataSnapshot.getChildren()){
                    if(node.getKey().equals(s_nickname)){
                        if(node.child("pwd").getValue().toString().equals(s_pwd)){
                            //성공
                            MyApp.myUser = node.getValue(User.class);
                            MyApp.roomNickName = s_nickname;
                            saveRoomInformation(MyApp.myUser);
                            intentMainActivity();
                            break;
                        }
                        //비번 다름
                        Toast.makeText(UserActivity.this, "비번을 확인해주세요.",
                                Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void addUser() {
        final String s_nickname = nickName.getText().toString();
        final String s_pwd = pwd.getText().toString();

        if(s_nickname.trim().isEmpty() || s_pwd.trim().isEmpty()){
            Toast.makeText(this, "정보를 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        MainActivity.rootRef.child("User").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot == null || (dataSnapshot.getChildrenCount() < 1) ){
                    makeUser(s_nickname, s_pwd);
                    return;
                }

                for(DataSnapshot item : dataSnapshot.getChildren()){
                    if(item.getKey().equals(s_nickname)){
                        Toast.makeText(UserActivity.this, "중복된 닉네임입니다.", Toast.LENGTH_SHORT).show();
                        break;
                    }else{
                        makeUser(s_nickname, s_pwd);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void makeUser(final String s_nickname, String s_pwd) {
        final User user = new User();
        user.setNickName(s_nickname);
        user.setMarkerColor(markerColorFloat);
        user.setPwd(s_pwd);
        MainActivity.rootRef
                .child("User")
                .child(s_nickname)
                .setValue(user)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(UserActivity.this, "생성에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        saveRoomInformation(user);
                        MyApp.myUser = user;
                        MyApp.roomNickName = s_nickname;
                        intentMainActivity();
                    }
                });
    }

    private void intentMainActivity() {
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }

    private void saveRoomInformation(User user) {
        String key = MyApp.roomChannel + "_";
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key+"name", user.getNickName());
        Log.d(TAG, "save name : " + user.getNickName());
        editor.commit();
    }

    private boolean isSavingRoom(String roomChanel){
        String key = roomChanel + "_";
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        String nickName = pref.getString(key+"name", null);
        if(nickName == null){
            return false;
        }
        return true;
    }

    private String getRoomNickName(String roomChanel){
        String key = roomChanel + "_";
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        String nickName = pref.getString(key+"name", null);
        return nickName;
    }

}
