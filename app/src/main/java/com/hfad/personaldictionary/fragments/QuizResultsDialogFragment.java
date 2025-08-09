package com.hfad.personaldictionary.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.hfad.personaldictionary.R;
import com.hfad.personaldictionary.activities.FavoriteWordsQuizActivity;
import com.hfad.personaldictionary.activities.SpellingQuizActivity;
import com.hfad.personaldictionary.activities.SynonymsAntonymsQuizActivity;
import com.hfad.personaldictionary.activities.TimedQuizActivity; // Adjust if your quiz activity has a different name
import com.hfad.personaldictionary.activities.MainActivity; // Assuming this is your main/home activity
import com.hfad.personaldictionary.activities.WordCompletionQuizActivity;
import com.hfad.personaldictionary.activities.WordMeaningQuizActivity;

public class QuizResultsDialogFragment extends DialogFragment {

    // Keys for arguments Bundle
    public static final String ARG_SCORE = "score";
    public static final String ARG_TOTAL_QUESTIONS = "total_questions";

    // Static factory method to create a new instance with arguments
    public static QuizResultsDialogFragment newInstance(int score, int totalQuestions) {
        QuizResultsDialogFragment fragment = new QuizResultsDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SCORE, score);
        args.putInt(ARG_TOTAL_QUESTIONS, totalQuestions);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Retrieve arguments
        Bundle args = getArguments();
        int score = 0;
        int totalQuestions = 0;
        if (args != null) {
            score = args.getInt(ARG_SCORE, 0);
            totalQuestions = args.getInt(ARG_TOTAL_QUESTIONS, 0);
        }

        // Inflate the custom layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_quiz_results, null);

        // Get references to views
        TextView tvScore = view.findViewById(R.id.tvScore);
        Button btnPlayAgain = view.findViewById(R.id.btnPlayAgain);
        Button btnGoHome = view.findViewById(R.id.btnGoHome);

        // Set the score text
        tvScore.setText("Your Score: " + score + "/" + totalQuestions);

        // Set up button click listeners
        btnPlayAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Option 1: Restart the current quiz activity
                // You will need to implement a 'restartQuiz()' method in your quiz activity
                if (getActivity() instanceof TimedQuizActivity) {
                    ((TimedQuizActivity) getActivity()).restartQuiz();
                }else if(getActivity() instanceof FavoriteWordsQuizActivity) {
                    ((FavoriteWordsQuizActivity) getActivity()).restartQuiz();
                }else if(getActivity() instanceof WordCompletionQuizActivity) {
                    ((WordCompletionQuizActivity) getActivity()).restartQuiz();
                }else if(getActivity() instanceof SpellingQuizActivity) {
                    ((SpellingQuizActivity) getActivity()).restartQuiz();
                }else if(getActivity() instanceof SynonymsAntonymsQuizActivity) {
                    ((SynonymsAntonymsQuizActivity) getActivity()).restartQuiz();
                }else if(getActivity() instanceof WordMeaningQuizActivity) {
                    ((WordMeaningQuizActivity) getActivity()).restartQuiz();
                }
                dismiss(); // Dismiss the dialog
            }
        });

        btnGoHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Option 2: Navigate to your main/home activity
                Intent intent = new Intent(getActivity(), MainActivity.class); // Adjust MainActivity if different
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear back stack
                startActivity(intent);
                dismiss(); // Dismiss the dialog
                requireActivity().finish(); // Finish the quiz activity
            }
        });

        // Build the AlertDialog and set the custom view
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(view);

        // Create the dialog
        return builder.create();
    }
}