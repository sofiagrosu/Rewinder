package com.example.myapplication1.quiz.delivery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.myapplication1.quiz.QuizCoordinator;

/**
 * Receives, even when the app is closed, and hands to the coordinator:
 *   - the tap on an answer button (phone or watch), including "Nu știu" (option 0)
 *   - the swipe that removes the question notification
 *   - the Da / Nu answer to "Este un moment nepotrivit pentru întrebări?"
 * Registered in AndroidManifest.xml.
 */
public class AnswerReceiver extends BroadcastReceiver {

    public static final String ACTION_ANSWER = "com.example.myapplication1.quiz.ANSWER";
    public static final String ACTION_DISMISSED = "com.example.myapplication1.quiz.DISMISSED";
    public static final String ACTION_BAD_MOMENT = "com.example.myapplication1.quiz.BAD_MOMENT";
    public static final String EXTRA_QUESTION_ID = "questionId";
    public static final String EXTRA_OPTION = "option";
    public static final String EXTRA_BAD_MOMENT = "badMoment";

    /** The "I don't know" button: not option 1..3, so it can never match the correct option. */
    public static final int OPTION_DONT_KNOW = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (ACTION_BAD_MOMENT.equals(action)) {   // no question id in this one
            boolean bad = intent.getBooleanExtra(EXTRA_BAD_MOMENT, false);
            Log.d("QUIZ", "\"Is this a good time?\" answered: " + (bad ? "No (pause)" : "Yes"));
            QuizCoordinator.getInstance(context).onBadMoment(bad);
            return;
        }

        long questionId = intent.getLongExtra(EXTRA_QUESTION_ID, -1);
        if (questionId < 0) return;

        if (ACTION_ANSWER.equals(action)) {
            int option = intent.getIntExtra(EXTRA_OPTION, -1);
            Log.d("QUIZ", "Answered question " + questionId + " with option " + option);
            if (option < OPTION_DONT_KNOW) return;
            QuizCoordinator.getInstance(context).onAnswer(questionId, option);

        } else if (ACTION_DISMISSED.equals(action)) {
            Log.d("QUIZ", "Question " + questionId + " was swiped away");
            QuizCoordinator.getInstance(context).onDismissed(questionId);
        }
    }
}
