package com.quizzmosis.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The enums that turn request parameters and database values into Java values. */
class RequestParameterParsingTest {

    @Test
    void questionTypesRoundTripThroughTheirDatabaseValues() {
        for (QuestionType type : QuestionType.values()) {
            assertEquals(type, QuestionType.fromDbValue(type.getDbValue()));
        }
        assertEquals(QuestionType.FILL_BLANK, QuestionType.fromParam("fill_blank"));
    }

    @Test
    void unknownQuestionTypeFromAFormIsNull() {
        assertNull(QuestionType.fromParam("essay"));
        assertNull(QuestionType.fromParam(null));
    }

    @Test
    void unknownQuestionTypeInTheDatabaseIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> QuestionType.fromDbValue("essay"));
    }

    @Test
    void quizVisibilityRoundTripsAndRejectsUnknownValues() {
        for (QuizVisibility visibility : QuizVisibility.values()) {
            assertEquals(visibility, QuizVisibility.fromDbValue(visibility.getDbValue()));
        }
        assertNull(QuizVisibility.fromParam("secret"));
        assertThrows(IllegalArgumentException.class, () -> QuizVisibility.fromDbValue("secret"));
    }

    @Test
    void leaderboardPeriodDefaultsToAllTime() {
        assertEquals(Timeframe.WEEK, Timeframe.fromParam("week"));
        assertEquals(Timeframe.TODAY, Timeframe.fromParam("today"));
        assertEquals(Timeframe.ALL, Timeframe.fromParam(null));
        assertEquals(Timeframe.ALL, Timeframe.fromParam("'; DROP TABLE users; --"));
    }
}
