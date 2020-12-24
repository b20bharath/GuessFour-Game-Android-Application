package com.example.guessfour;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class MainActivity extends Activity {
    public Boolean Lock = false;
    public Handler mainHandler = new Handler(Looper.getMainLooper()){          // Attaching handler to the mainactivity looper
        @Override
        public void handleMessage(@NonNull Message msg) { // handling messages from for the main thread
            super.handleMessage(msg);
            int what =msg.what;
            synchronized (Lock) { // synchronized lock for modifying UI elements
                switch (what) {
                    case SET_LIST_1:

                        Bundle f1 = msg.getData();
                        //update the List 1
                        List1.add("Player1 Guess: " + String.valueOf(msg.obj) + "\nDigits in Correct Position: " + f1.getString("NumOfCorrect") + "\nDigits in Wrong Position: " + f1.getString("NumOfWrong") + "\nMissed Digit: " + f1.getString("MissedValue"));
                        adapter1.notifyDataSetChanged();
                        //checking whether we have a winner
                        if(msg.arg2 != 1){
                        Message msg1 = handler1.obtainMessage(FEEDBACK_PROCESS);
                        msg1.setData(msg.getData());
                        handler1.sendMessage(msg1);
                        }
                        else{ // Player 1 is the winner
                            //signaling the threads that we have winner
                            handler2.post(new Runnable() {
                                @Override
                                public void run() {
                                    handler2.getLooper().quit();
                                }
                            });
                            handler1.getLooper().quit();
                            Toast.makeText(MainActivity.this, "Player " + String.valueOf(msg.arg1) + "won the game :)", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case SET_LIST_2:
                        Bundle f2 = msg.getData();
                        //Update the List 2
                        List2.add("Player2 Guess: " + String.valueOf(msg.obj) + "\nDigits in Correct Position: " + f2.getString("NumOfCorrect") + "\nDigits in Wrong Position: " + f2.getString("NumOfWrong") + "\nMissed Digit: " + f2.getString("MissedValue"));
                        adapter2.notifyDataSetChanged();
                        if(msg.arg2 != 1) { // checking whether we have a winner
                            Message msg2 = handler2.obtainMessage(FEEDBACK_PROCESS);
                            msg2.setData(msg.getData());
                            handler2.sendMessage(msg2);
                        }
                        else{// Player 2 is the winner
                            // signaling the threads that we have winner
                            handler1.post(new Runnable() {
                                @Override
                                public void run() {
                                    handler1.getLooper().quit();
                                }
                            });
                            handler2.getLooper().quit();
                            Toast.makeText(MainActivity.this, "Player " + String.valueOf(msg.arg1) + "won the game :)", Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case LIMIT_REACHED: // 20 guessed are made by each thread so the limit has reached
                        handler1.getLooper().quitSafely();
                        handler2.getLooper().quitSafely();
                        Toast.makeText(MainActivity.this, "Guess Limit is Reached!!! Try Again :)", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    };

    private Button btn;
    private Button restart_btn;
    private ListView gue1;
    private ListView gue2;
    private ArrayAdapter<String> adapter1;
    private ArrayAdapter<String> adapter2;
    private ArrayList<String> List1;
    private  ArrayList<String> List2;
    private TextView display1;
    private TextView display2;

    public Handler handler1;
    public Handler handler2;

    public static final int SET_LIST_1 = 0 ;
    public static final int SET_LIST_2 = 1 ;

    public static final int SET_WINNER = 2 ;
    public static final int PROCESSING_GUESS = 3;
    public static final int FEEDBACK_PROCESS = 4;
    public static final int LIMIT_REACHED = 5;
    public int winner_set = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        restart_btn = (Button) findViewById(R.id.button_restart); // Intializing a button
        btn = (Button) findViewById(R.id.button);
        gue1 = (ListView) findViewById(R.id.guess1); // Intializing a ListView
        gue2 = (ListView) findViewById(R.id.guess2);
        display1 = (TextView) findViewById(R.id.display1);
        display2 = (TextView) findViewById(R.id.display2);
        List1 = new ArrayList<String>(); // empty List
        List2 = new ArrayList<String>();

        restart_btn.setEnabled(false);
        // Defining an adapter for the Lists
        adapter1 = new ArrayAdapter<String>(MainActivity.this,R.layout.list_item_1,List1);
        adapter2 = new ArrayAdapter<String>(MainActivity.this,R.layout.list_item_2,List2);

        gue1.setAdapter(adapter1);
        gue2.setAdapter(adapter2);
        // when you click start button
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn.setEnabled(false);
                restart_btn.setEnabled(true);
                Thread t1 = new Thread(new Thread1());
                t1.start();

                Thread t2 = new Thread(new Thread2());
                t2.start();

            }
        });
        // When you click restart button
        restart_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("restart button","entered");
                // removing all the messages
                handler2.removeMessages(SET_LIST_1);
                handler1.removeMessages(SET_LIST_2);
                // Quiting the loopers of the handlers
                handler2.getLooper().quit();
                handler1.getLooper().quit();
                try {  Thread.sleep(4000); }
                catch (InterruptedException e) { System.out.println("Thread interrupted!") ; } ;


                List1.removeAll(List1);
                adapter1.notifyDataSetChanged();
                List2.removeAll(List2);
                adapter2.notifyDataSetChanged();
                winner_set = 0;
                Thread1 t11 = null;
                Thread2 t22 = null;
                t11 = new Thread1();
                t22 = new Thread2();
                Thread t1 = new Thread(t11);
                t1.start();
                Thread t2 = new Thread(t22);
                t2.start();
            }
        });
    }


    public class Thread1 implements Runnable { // Worker Thread 1
        public int flag1 = 0;
        public Boolean feedbackGiven = false;
        private Boolean completed1 = false;
        public ArrayList<Integer> missedNumbers1 = new ArrayList<Integer>();
        public String target1=randomNumber(new ArrayList<Integer>(),false); // Secret Number Generation
        public ArrayList<String> ListOfGuesses1 = new ArrayList<>();

        public  String[] processing(String guess){ // evaluating the guess from the opponent
            // Returning a String array whose values are digits in correct position, digits in wrong position and missed values
            Random rand = new Random();
            if(guess.equals(target1)){
                return new String[]{"4","",""};
            }
            int numOfCorrect = 0,numOfWrong = 0;
            String mNumber = "";
            String nCorrect = "";
            String nWrong = "";
            ArrayList<Character> misNumber = new ArrayList<>();
            ArrayList<Character> target = new ArrayList<>();
            for (Character c:target1.toCharArray()){
                target.add(c);
            }
            ArrayList<Character> guessNumber = new ArrayList<>();
            for (Character c:guess.toCharArray()){
                guessNumber.add(c);
            }
            for(Character c: guessNumber){
                if(target.contains(c)){
                    if (target.indexOf(c) == guessNumber.indexOf(c)){
                        numOfCorrect++;
                    }
                    else{
                        numOfWrong++;
                    }
                }
                else{
                    misNumber.add(c);
                }
            }
            if (!(misNumber.size() == 0)){
                mNumber = String.valueOf(misNumber.get(rand.nextInt(misNumber.size())));
            }
            if(numOfCorrect != 0){
                nCorrect = String.valueOf(numOfCorrect);
            }
            if(numOfWrong != 0){
                nWrong = String.valueOf(numOfWrong);
            }
            return new String[]{nCorrect,nWrong,mNumber};
        }

        public String randomNumber(ArrayList<Integer> missedNum, Boolean noMissedValues){
            //creating an array list of integer objects with values from 0-9
            ArrayList<Integer> allNumbers = new ArrayList<>(Arrays.asList(Integer.valueOf(0),Integer.valueOf(1),new Integer(2),new Integer(3),new Integer(4),new Integer(5),new Integer(6),new Integer(7),new Integer(8),new Integer(9)));
            //removing the missed values from the above arrayList
            if (missedNum.size()>0) {
                for (Integer i : missedNum
                ) {
                    allNumbers.remove(i);
                }
            }
            String number = "";
            Random rand = new Random();
            if(noMissedValues == false) {
                ArrayList<Integer> numb = new ArrayList<>();

                //Selecting number randomly from the new arrayList after removing the missed numbers

                int i = 0;
                int temp;
                while (i < 4) {
                    temp = allNumbers.get(rand.nextInt(allNumbers.size()));
                    if (numb.size() == 0) {
                        numb.add(temp);
                        i++;
                    } else {
                        if (!numb.contains(temp)) {
                            numb.add(temp);
                            i++;
                        }
                    }
                }


                for (int k : numb) {
                    number = number + String.valueOf(k);
                }
            }
            else{ // If we have no missed values
                String lastNumber = ListOfGuesses1.get(ListOfGuesses1.size() - 1);
                ArrayList<Character> lNumber = new ArrayList<>();
                for (Character c:lastNumber.toCharArray()){
                    lNumber.add(c);
                }

                ArrayList<Integer> numb = new ArrayList<>();

                //Selecting number randomly from the new arrayList after removing the missed numbers

                int i = 0;
                int temp;
                while (i < 4) {
                    temp = lNumber.get(rand.nextInt(allNumbers.size()));
                    if (numb.size() == 0) {
                        numb.add(temp);
                        i++;
                    } else {
                        if (!numb.contains(temp)) {
                            numb.add(temp);
                            i++;
                        }
                    }
                }


                for (int k : numb) {
                    number = number + String.valueOf(k);
                }

            }
            return number;
        }

        @Override
        public void run() {

            Looper.prepare();
            handler1 = new Handler(Looper.myLooper()){ // creating a handler and attaching to the thread 1 looper
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    int what = msg.what;
                    switch(what){
                        case PROCESSING_GUESS:
                            try {  Thread.sleep(2000); }
                            catch (InterruptedException e) { System.out.println("Thread interrupted!") ; } ;
                            String[] result = processing(String.valueOf(msg.obj));
                            Bundle b = new Bundle(3);
                            b.putString("NumOfCorrect",result[0]);
                            b.putString("NumOfWrong",result[1]);
                            b.putString("MissedValue",result[2]);
                            if (result[0] != "") {
                                if (Integer.parseInt(result[0]) == 4) {
                                    //completed1 = true;
                                    winner_set = 1;
                                }
                            }

                            Message mainMsg = mainHandler.obtainMessage(SET_LIST_2);
                            mainMsg.setData(b);
                            mainMsg.obj = msg.obj;
                            mainMsg.arg1 = 2;
                            if(winner_set == 1) {
                                mainMsg.arg2 = 1;
                            }
                            feedbackGiven = true;
                            mainHandler.sendMessage(mainMsg);

                            break;
                        case FEEDBACK_PROCESS:
                            while(true) {
                                try {  Thread.sleep(2000); }
                                catch (InterruptedException e) { System.out.println("Thread interrupted!") ; } ;
                                if(feedbackGiven == true) {
                                    try {  Thread.sleep(2000); }
                                    catch (InterruptedException e) { System.out.println("Thread interrupted!") ; } ;
                                    Bundle feedback = msg.getData();
                                    String guessNumber1 = "";
                                    Log.i("Thread1", "Processing feed back");


                                    if (!(feedback.getString("MissedValue") == "")) {
                                        missedNumbers1.add(Integer.valueOf(feedback.getString("MissedValue")));
                                    }
                                    flag1++;
                                    if (!(flag1 < 20)) {
                                        completed1 = true;
                                    }
                                    if (completed1 == true) {

                                        if (flag1 < 20) {

                                        } else {
                                            Message Limit = mainHandler.obtainMessage(LIMIT_REACHED);
                                            mainHandler.sendMessage(Limit);
                                        }
                                    } else {
                                        feedbackGiven = false;
                                        Message message = handler2.obtainMessage(PROCESSING_GUESS);
                                        String guess1 = "";
                                        while (true) {
                                            guess1 = randomNumber(missedNumbers1, false);
                                            if (ListOfGuesses1.contains(guess1)) {
                                                continue;
                                            } else {
                                                ListOfGuesses1.add(guess1);
                                                break;
                                            }
                                        }
                                        message.obj = guess1;

                                        handler2.sendMessage(message);
                                    }
                                    break;
                                }
                            }
                            break;
                    }
                }
            };
            mainHandler.post(new Runnable() { // Posting the runnable which contains code for displaying a secret number
                @Override
                public void run() {
                    display1.setText("Player 1 Secret Number:"+target1);
                }
            });

            try {  Thread.sleep(1000); }
            catch (InterruptedException e) { System.out.println("Thread interrupted!") ; } ;

            if(flag1 == 0) {
                // Sending the First Guess from thread 1
                Message player1msg = handler2.obtainMessage(PROCESSING_GUESS);
                String p = randomNumber(missedNumbers1, false);
                ListOfGuesses1.add(p);
                player1msg.obj = p;
                Log.i("Thread1","First Guess created");
                handler2.sendMessage(player1msg);
            }
            Looper.loop();
        }
    }
    public class Thread2 implements Runnable { // Worker Thread 2
        public int flag2 = 0;
        private Boolean completed2 = false;
        public ArrayList<String> ListOfGuesses2 = new ArrayList<>();
        public ArrayList<Integer> missedNumbers2 = new ArrayList<Integer>();
        public String target2=randomNumber(); // Secret Number Generation
        public Boolean feedbackGiven = false;
        // Evaluating the opponent's guess
        public  String[] processing(String guess){
            Random rand = new Random();
            if(guess.equals(target2)){
                return new String[]{"4","",""};
            }
            int numOfCorrect = 0,numOfWrong = 0;
            String mNumber = "";
            String nCorrect = "";
            String nWrong = "";
            ArrayList<Character> misNumber = new ArrayList<>();
            ArrayList<Character> target = new ArrayList<>();
            for (Character c:target2.toCharArray()){
                target.add(c);
            }
            ArrayList<Character> guessNumber = new ArrayList<>();
            for (Character c:guess.toCharArray()){
                guessNumber.add(c);
            }
            for(Character c: guessNumber){
                if(target.contains(c)){
                    if (target.indexOf(c) == guessNumber.indexOf(c)){
                        numOfCorrect++;
                    }
                    else{
                        numOfWrong++;
                    }
                }
                else{
                    misNumber.add(c);
                }
            }
            if (!(misNumber.size() == 0)){
                mNumber = String.valueOf(misNumber.get(rand.nextInt(misNumber.size())));
            }
            if(numOfCorrect != 0){
                nCorrect = String.valueOf(numOfCorrect);
            }
            if(numOfWrong != 0){
                nWrong = String.valueOf(numOfWrong);
            }
            return new String[]{nCorrect,nWrong,mNumber};
        }

        public String randomNumber(){ // Generating a random number without removing missed values.... It is different from the thread 1 random number generation logic
            //creating an array list of integer objects with values from 0-9
            ArrayList<Integer> allNumbers = new ArrayList<>(Arrays.asList(Integer.valueOf(0),Integer.valueOf(1),new Integer(2),new Integer(3),new Integer(4),new Integer(5),new Integer(6),new Integer(7),new Integer(8),new Integer(9)));

            Random rand = new Random();
            String number = "";

            ArrayList<Integer> numb = new ArrayList<>();

            //Selecting number randomly from the new arrayList after removing the missed numbers

            int i = 0;
            int temp;
            while (i < 4) {
                temp = allNumbers.get(rand.nextInt(allNumbers.size()));
                if (numb.size() == 0) {
                    numb.add(temp);
                    i++;
                } else {
                    if (!numb.contains(temp)) {
                        numb.add(temp);
                        i++;
                    }
                }
            }
            for (int k : numb) {
                number = number + String.valueOf(k);
            }
            return number;
        }
        @Override
        public void run() {
            Looper.prepare();
            handler2 = new Handler(Looper.myLooper()){ // Attaching
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    int what = msg.what;
                    switch (what){
                        case PROCESSING_GUESS:

                            String[] result = processing(String.valueOf(msg.obj));

                            Bundle b = new Bundle(3);
                            b.putString("NumOfCorrect",result[0]);
                            b.putString("NumOfWrong",result[1]);
                            b.putString("MissedValue",result[2]);

                            if (result[0] != "") {
                                if (Integer.parseInt(result[0]) == 4) {
                                    //completed1 = true;
                                    winner_set = 1;
                                }
                            }

                            Message mainMsg = mainHandler.obtainMessage(SET_LIST_1);
                            mainMsg.setData(b);
                            mainMsg.obj = msg.obj;
                            mainMsg.arg1 = 1;
                            if(winner_set == 1) {
                                mainMsg.arg2 = 1;
                            }
                            feedbackGiven = true;
                            mainHandler.sendMessage(mainMsg);

                            break;
                        case FEEDBACK_PROCESS:
                            Bundle feedback = msg.getData();
                            String guessNumber2 = "";
                            while(true) {
                                try {  Thread.sleep(2000); }
                                catch (InterruptedException e) { System.out.println("Thread interrupted!") ; } ;
                                if(feedbackGiven == true) {
                                    Log.i("Thread2", "processing feedback");
                                    /*if (feedback.getString("NumOfCorrect") != "") {
                                        if (Integer.parseInt(feedback.getString("NumOfCorrect")) == 4) {
                                            completed2 = true;
                                        }
                                    }*/
                                    if (!(feedback.getString("MissedValue") == "")) {
                                        missedNumbers2.add(Integer.valueOf(feedback.getString("MissedValue")));
                                    }
                                    flag2++;
                                    if (!(flag2 < 20)) {
                                        completed2 = true;
                                    }
                                    if (completed2 == true) {
                                        if (flag2 < 20) {

                                        } else {
                                            Message Limit1 = mainHandler.obtainMessage(LIMIT_REACHED);
                                            mainHandler.sendMessage(Limit1);
                                        }
                                    } else {
                                        feedbackGiven = false;
                                        Log.i("Thread2", "Guess2 generated");
                                        Message message = handler1.obtainMessage(PROCESSING_GUESS);
                                        String guess2 = "";
                                        while (true) {
                                            guess2 = randomNumber();
                                            if (ListOfGuesses2.contains(guess2)) {
                                                continue;
                                            } else {
                                                ListOfGuesses2.add(guess2);
                                                break;
                                            }
                                        }
                                        message.obj = guess2;

                                        handler1.sendMessage(message);
                                    }
                                    break;
                                }
                            }
                            break;

                    }
                }
            };
            mainHandler.post(new Runnable() {
                @Override
                public void run() {  // posting a Runnable which contains code of displaying the Secret number of Player 2
                    display2.setText("Player 2 Secret Number:"+target2);
                }
            });
            try {  Thread.sleep(1000); }
            catch (InterruptedException e) { System.out.println("Thread interrupted!") ; } ;
            if(flag2 == 0) {
                Message player2msg = handler1.obtainMessage(PROCESSING_GUESS);
                String guess2 = "";
                while(true) {
                    guess2 = randomNumber();
                    if (ListOfGuesses2.contains(guess2)) {
                        continue;
                    }
                    else{
                        ListOfGuesses2.add(guess2);
                        break;
                    }
                }
                player2msg.obj = guess2;

                handler1.sendMessage(player2msg);
            }

            Looper.loop();
        }
    }
}