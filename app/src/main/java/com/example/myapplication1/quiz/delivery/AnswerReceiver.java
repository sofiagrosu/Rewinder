package com.example.myapplication1.quiz.delivery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.myapplication1.quiz.QuizCoordinator;

/**
 * Receives, even when the app is closed, and hands to the coordinator:
 *   - the tap on an answer button (phone or watch)
 *   - the swipe that removes the question notification
 * Registered in AndroidManifest.xml.
 */
public class AnswerReceiver extends BroadcastReceiver {

    public static final String ACTION_ANSWER = "com.example.myapplication1.quiz.ANSWER";
    public static final String ACTION_DISMISSED = "com.example.myapplication1.quiz.DISMISSED";
    public static final String EXTRA_QUESTION_ID = "questionId";
    public static final String EXTRA_OPTION = "option";

    @Override
    public void onReceive(Context context, Intent intent) {
        long questionId = intent.getLongExtra(EXTRA_QUESTION_ID, -1);
        if (questionId < 0) return;

        if (ACTION_ANSWER.equals(intent.getAction())) {
            int option = intent.getIntExtra(EXTRA_OPTION, -1);
            Log.d("QUIZ", "Answered question " + questionId + " with option " + option);
            if (option < 1) return;
            QuizCoordinator.getInstance(context).onAnswer(questionId, option);

        } else if (ACTION_DISMISSED.equals(intent.getAction())) {
            Log.d("QUIZ", "Question " + questionId + " was swiped away");
            QuizCoordinator.getInstance(context).onDismissed(questionId);
        }
    }
}
