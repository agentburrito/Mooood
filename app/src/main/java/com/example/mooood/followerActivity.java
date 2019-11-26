package com.example.mooood;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Show profile of searched user and can send request
 */
public class followerActivity extends AppCompatActivity {
    Button followButton;
    Button backButton;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference;
    TextView setDate;
    TextView setTime;
    TextView setAuthor;

    /**
     * This implements all methods below accordingly
     * Will also check to see if user has already sent a request to change text of button
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follower_profile);

        setDate=findViewById(R.id.recentMoodDate);
        setTime = findViewById(R.id.recentMoodTime);
        setAuthor= findViewById(R.id.author);
        collectionReference=db.collection("MoodEvents");
        Intent intent = getIntent();
        final String toFollow = intent.getStringExtra("accountMood");
        final String loginName= intent.getStringExtra("loginName");
        final String date = intent.getStringExtra("moodDate");
        final String time = intent.getStringExtra("moodTime");
        final String author = intent.getStringExtra("moodAuthor");

        Log.d("follower", "date "+ date);
        Log.d("follower", "time "+ time);
        Log.d("follower", "Author "+ author);
        setDate.setText(date);
        setTime.setText(time);
        setAuthor.setText(author);

        collectionReference.document(toFollow).collection("Request").document(loginName)
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        followButton.setText("REQUEST SENT");
                    } else {
                        Log.d("checking", "Document does not exist!");
                    }
                } else {
                    Log.d("checking", "Failed with: ", task.getException());
                }
            }
        });

        followUser(toFollow, loginName);
        backToFeed();
    }//End of onCreate

    /**
     * After clicking follow button, will send a request to the account while adding request collection to database
     * @param toFollow
     *  This is the account the user searhed up in feed Acitvity and wants to follow
     * @param loginName
     *  This is the account name used to sign in
     */
    private void followUser(final String toFollow, final String loginName) {
        followButton = findViewById(R.id.follow_button);
        Date currentTime = Calendar.getInstance().getTime();
        //LocalDateTime now = LocalDateTime.now();
        SimpleDateFormat requestDateFormat = new SimpleDateFormat("MMM dd yyyy h:mm a");
        String date = requestDateFormat.format(currentTime);

        final Map<String, Object> request = new HashMap<>();
        request.put("Username",loginName);
        request.put("Request", "Sent");
        request.put("Request Time", date);
        followButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("login name after", loginName);
                Log.d("trying to follow after", toFollow);
                collectionReference.document(toFollow).collection("Request").document(loginName).set(request);
                followButton.setText("REQUEST SENT");
                Log.d("SENT", "request sent");

            }
        });
    }

    /**
     * Will take user back to Feed Activity by clicking on the back button
     */
    private void backToFeed(){
        backButton= findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
             finish();
            }
        });
    }



}
