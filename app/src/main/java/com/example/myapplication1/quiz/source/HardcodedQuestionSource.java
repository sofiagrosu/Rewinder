package com.example.myapplication1.quiz.source;

import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.QuestionSource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Test questions written by hand, until the database exists. */
public class HardcodedQuestionSource implements QuestionSource {

    private final List<Question> allQuestions = new ArrayList<>();

    public HardcodedQuestionSource() {
        // ---- Subject 1: Anatomy · Brachial plexus ----
        allQuestions.add(new Question(1, 1,
                "Which nerve innervates the deltoid muscle?",
                "Axillary", "Radial", "Ulnar", 1,
                "The axillary nerve (C5–C6) supplies the deltoid and teres minor."));

        allQuestions.add(new Question(2, 1,
                "The brachial plexus is formed by the ventral rami of which spinal nerves?",
                "C5–T1", "C1–C4", "T1–T6", 1,
                "Roots C5, C6, C7, C8 and T1 form the brachial plexus."));

        allQuestions.add(new Question(3, 1,
                "\"Wrist drop\" is caused by injury to which nerve?",
                "Median", "Radial", "Ulnar", 2,
                "The radial nerve supplies the wrist and finger extensors."));

        allQuestions.add(new Question(4, 1,
                "The musculocutaneous nerve innervates the biceps brachii.",
                "True", "False", null, 1,
                null));   // no explanation: tests the null case

        allQuestions.add(new Question(5, 1,
                "A \"claw hand\" is mainly caused by injury to which nerve?",
                "Ulnar", "Axillary", "Musculocutaneous", 1,
                "The ulnar nerve supplies most of the small muscles of the hand."));

        // ---- Subject 2: Physiology · Cardiac cycle ----
        allQuestions.add(new Question(6, 2,
                "The first heart sound (S1) is caused by the closure of the:",
                "AV valves", "Semilunar valves", "Foramen ovale", 1,
                "S1 = closure of the mitral and tricuspid valves at the start of systole."));

        allQuestions.add(new Question(7, 2,
                "During isovolumetric contraction, all four heart valves are closed.",
                "True", "False", null, 1,
                "Pressure rises, but volume stays constant until the aortic valve opens."));

        allQuestions.add(new Question(8, 2,
                "What is the normal resting stroke volume of an adult?",
                "About 70 mL", "About 20 mL", "About 200 mL", 1,
                "Stroke volume = end-diastolic volume (~120 mL) − end-systolic volume (~50 mL)."));

        allQuestions.add(new Question(9, 2,
                "Ventricular ejection begins when ventricular pressure exceeds:",
                "Aortic pressure", "Atrial pressure", "Venous pressure", 1,
                "The aortic valve opens only when the left ventricle overcomes aortic pressure."));

        allQuestions.add(new Question(10, 2,
                "At rest, atrial contraction fills most of the ventricle.",
                "True", "False", null, 2,
                "Most filling (~70–80%) is passive; the atrial \"kick\" adds only ~20–30%."));
    }

    @Override
    public List<Question> getQuestionsForSubject(long subjectId) {
        return allQuestions.stream()
                .filter(question -> question.getSubjectId() == subjectId)
                .collect(Collectors.toList());
    }

    @Override
    public Question getQuestionById(long questionId) {
        for (Question q : allQuestions) {
            if (q.getId() == questionId) return q;
        }
        return null;
    }

}