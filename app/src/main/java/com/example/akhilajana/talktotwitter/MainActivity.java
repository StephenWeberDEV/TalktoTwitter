package com.example.akhilajana.talktotwitter;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import twitter4j.Status;

public class MainActivity extends AppCompatActivity implements TwitterService.IData{

    private TextView txtSpeechInput;
    private ImageButton btnSpeak;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private List<Status> tweets;
    private String result;

    FirebaseDatabase database;
    DatabaseReference dbRef;
    FirebaseAuth mAuth;

    ArrayList<String> inputList;

    private static HashSet<String> thingKeywords = new HashSet<>();
    private static HashSet<String> personKeywords = new HashSet<>();

    static {
        thingKeywords.add(" about ");
        thingKeywords.add(" on ");
        thingKeywords.add(" for ");
        personKeywords.add(" from ");
        personKeywords.add(" by ");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);



        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        dbRef=database.getReference();

        inputList = new ArrayList<>();

        // hide the action bar
        //getActionBar().hide();

//        btnSpeak.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                promptSpeechInput();
//            }
//        });
        manipulateInput(" by Donald Trump");
    }

    /** Showing google speech input dialog * */
    private void promptSpeechInput()
    {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try
        {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        }
        catch (ActivityNotFoundException a)
        {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** Receiving speech input * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case REQ_CODE_SPEECH_INPUT:
                {
                if (resultCode == RESULT_OK && null != data)
                {

                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Log.d("Yo","Result is: "+result);

                    txtSpeechInput.setText(result.get(0));


                    if("FirebaseResult" != null){
                        manipulateInput(result.get(0));
                    } else {
                        buildIntent(new ArrayList<Tweet>());
                    }

                }
                break;
            }

        }
    }

    private void manipulateInput(String userInput) {
        boolean keywordExists = false;
        for(String keyword : thingKeywords) {
            if(userInput.contains(keyword)) {
                int keywordIndex = userInput.indexOf(keyword);
                result = userInput.substring(keywordIndex + keyword.length());
                keywordExists = true;

                makeTwitterCall(result, Constants.THING_KEYWORD);

                break;
            }
        }
        if(!keywordExists) {
            for (String keyword : personKeywords) {
                if (userInput.contains(keyword)) {
                    int keywordIndex = userInput.indexOf(keyword);
                    result = userInput.substring(keywordIndex + keyword.length());
                    keywordExists = true;

                    makeTwitterCall(result, Constants.PERSON_KEYWORD);

                    break;
                }
            }
        }

        if (!keywordExists) {
            Toast.makeText(this, "Please Try Again", Toast.LENGTH_LONG).show();
        }


    }

    private void getInputData()
    {
        Query query=dbRef.child("User_Input");
        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                String inputObj=dataSnapshot.getValue(String.class);
                inputList.add(inputObj);
                Log.d("listSize",inputList.size()+"");

                //Replace 1st element if list size exceeds 3
                if(inputList.size()>3)
                {
                    inputList.remove(0);
                }

                //adapter.notifyDataSetChanged();

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s)
            {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s)
            {

            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
    }



    private void makeTwitterCall(String input, String queryType){
        new TwitterService(MainActivity.this, MainActivity.this, queryType).execute(input);
    }

    @Override
    public void setupData(TweetsList tweetsList) {
        ArrayList<Tweet> tweets = tweetsList.getTweets();
        addTweetsToFirebase(tweets);
        buildIntent(tweets);
    }

    public void addTweetsToFirebase(ArrayList<Tweet> tweets){
        int i = 1;
        for(Tweet tweet : tweets) {
            dbRef.child(result).child(i+"").child("tweet").setValue(tweet.getTweetContent());
            dbRef.child(result).child(i+"").child("timestamp").setValue(tweet.getCreatedAt());
            dbRef.child(result).child(i+"").child("userName").setValue(tweet.getUser().getName());
            dbRef.child(result).child(i+"").child("userPic").setValue(tweet.getUser().getImageUrl());
            i++;
        }
    }

    private void buildIntent(ArrayList<Tweet> tweets) {
        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("TweetKey", tweets);
        intent.putExtra("BundleKey", bundle);
        startActivity(intent);
    }
}
