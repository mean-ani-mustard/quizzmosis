-- =====================================================================
--  Quizzmosis database schema (MySQL 8)
--
--  Running this file (re)creates every table, so it WIPES existing data.
--  Usage:   mysql -u root -p quiz_db < db/schema.sql
--  Then optionally load demo data:  mysql -u root -p quiz_db < db/seed.sql
--
--  Deleting a user or a quiz cleans up everything that belongs to it
--  automatically thanks to ON DELETE CASCADE, so the Java code never has
--  to delete child rows by hand.
--
--  Text columns use utf8mb4 (any language, emoji included) with the
--  utf8mb4_0900_ai_ci collation, so comparisons ignore upper/lower case:
--  "Alice" and "alice" are the same username, and searches match either.
-- =====================================================================

-- Drop in reverse dependency order (children before parents).
-- "answers" is a legacy table from the first version of the project.
DROP TABLE IF EXISTS answers;
DROP TABLE IF EXISTS quiz_ratings;
DROP TABLE IF EXISTS quiz_comments;
DROP TABLE IF EXISTS user_achievements;
DROP TABLE IF EXISTS achievements;
DROP TABLE IF EXISTS announcements;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS challenges;
DROP TABLE IF EXISTS friends;
DROP TABLE IF EXISTS attempt_answers;
DROP TABLE IF EXISTS quiz_attempts;
DROP TABLE IF EXISTS question_answers;
DROP TABLE IF EXISTS question_choices;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS quizzes;
DROP TABLE IF EXISTS users;

-- ---------------------------------------------------------------------
-- Users. Passwords are stored as salted PBKDF2 hashes (see PasswordHasher).
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id       INT          NOT NULL AUTO_INCREMENT,
    username      VARCHAR(30)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    -- A short motto shown on the user's profile.
    mantra        VARCHAR(150) NULL,
    -- Whether friends can see the user's challenge history on their profile. The history itself
    -- is never deleted; this only hides it.
    show_challenge_history BOOLEAN NOT NULL DEFAULT TRUE,
    is_admin      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login    TIMESTAMP    NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- Quizzes and their questions.
-- ---------------------------------------------------------------------
CREATE TABLE quizzes (
    quiz_id              INT          NOT NULL AUTO_INCREMENT,
    owner_id             INT          NOT NULL,
    title                VARCHAR(100) NOT NULL,
    description          VARCHAR(500) NULL,
    random_order         BOOLEAN      NOT NULL DEFAULT FALSE,
    multiple_pages       BOOLEAN      NOT NULL DEFAULT FALSE,
    immediate_correction BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Who can see the quiz: everyone, the author's friends, or only the author.
    visibility           ENUM ('public', 'friends', 'private') NOT NULL DEFAULT 'public',
    -- How long players have to finish the quiz; NULL means there is no time limit.
    time_limit_seconds   INT          NULL,
    creation_date        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (quiz_id),
    CONSTRAINT fk_quizzes_owner FOREIGN KEY (owner_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE questions (
    question_id    INT           NOT NULL AUTO_INCREMENT,
    quiz_id        INT           NOT NULL,
    question_type  ENUM ('question_response', 'fill_blank', 'multiple_choice', 'picture_response') NOT NULL,
    question_text  VARCHAR(500)  NOT NULL,
    image_url      VARCHAR(1000) NULL,
    question_order INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (question_id),
    KEY idx_questions_quiz_order (quiz_id, question_order),
    CONSTRAINT fk_questions_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (quiz_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Options of a multiple-choice question (one or more can be correct).
CREATE TABLE question_choices (
    choice_id   INT          NOT NULL AUTO_INCREMENT,
    question_id INT          NOT NULL,
    choice_text VARCHAR(255) NOT NULL,
    is_correct  BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (choice_id),
    CONSTRAINT fk_choices_question FOREIGN KEY (question_id) REFERENCES questions (question_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Accepted answers for the typed-answer question types.
CREATE TABLE question_answers (
    answer_id   INT          NOT NULL AUTO_INCREMENT,
    question_id INT          NOT NULL,
    answer_text VARCHAR(255) NOT NULL,
    PRIMARY KEY (answer_id),
    CONSTRAINT fk_answers_question FOREIGN KEY (question_id) REFERENCES questions (question_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- Finished quiz attempts. total_questions is stored so that old results
-- keep their "x out of y" even if the quiz is edited later.
-- ---------------------------------------------------------------------
CREATE TABLE quiz_attempts (
    attempt_id       INT       NOT NULL AUTO_INCREMENT,
    user_id          INT       NOT NULL,
    quiz_id          INT       NOT NULL,
    score            INT       NOT NULL DEFAULT 0,
    total_questions  INT       NOT NULL DEFAULT 0,
    duration_seconds INT       NOT NULL DEFAULT 0,
    attempt_date     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attempt_id),
    KEY idx_attempts_user_date (user_id, attempt_date),
    KEY idx_attempts_quiz_score (quiz_id, score),
    CONSTRAINT fk_attempts_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_attempts_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (quiz_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- One row per answered question of an attempt (at most one per question).
CREATE TABLE attempt_answers (
    attempt_answer_id INT          NOT NULL AUTO_INCREMENT,
    attempt_id        INT          NOT NULL,
    question_id       INT          NOT NULL,
    user_answer_text  VARCHAR(500) NULL,
    correct_answer    VARCHAR(500) NULL,
    is_correct        BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (attempt_answer_id),
    UNIQUE KEY uq_attempt_question (attempt_id, question_id),
    CONSTRAINT fk_attempt_answers_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts (attempt_id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_answers_question FOREIGN KEY (question_id) REFERENCES questions (question_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- Friendships.
--   pending row  (A, B)          -> A sent B a friend request
--   accepted rows (A, B) + (B, A) -> A and B are friends
-- ---------------------------------------------------------------------
CREATE TABLE friends (
    user_id      INT       NOT NULL,
    friend_id    INT       NOT NULL,
    status       ENUM ('pending', 'accepted') NOT NULL,
    request_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, friend_id),
    KEY idx_friends_friend (friend_id, status),
    CONSTRAINT fk_friends_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_friends_friend FOREIGN KEY (friend_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- Quiz challenges between friends, and messages.
-- ---------------------------------------------------------------------
CREATE TABLE challenges (
    challenge_id     INT          NOT NULL AUTO_INCREMENT,
    challenger_id    INT          NOT NULL,
    challenged_id    INT          NOT NULL,
    quiz_id          INT          NOT NULL,
    message          VARCHAR(500) NOT NULL,
    -- pending: waiting for an answer; accepted: said yes, completed once they finish the quiz;
    -- declined: said no. Answered challenges are kept as history and never deleted by users.
    status           ENUM ('pending', 'accepted', 'declined', 'completed') NOT NULL DEFAULT 'pending',
    -- What the challenged user wrote when accepting or declining (optional).
    response_comment VARCHAR(500) NULL,
    -- The finished attempt that completed the challenge.
    attempt_id       INT          NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at     TIMESTAMP    NULL,
    completed_at     TIMESTAMP    NULL,
    PRIMARY KEY (challenge_id),
    KEY idx_challenges_challenged (challenged_id, status),
    KEY idx_challenges_challenger (challenger_id),
    CONSTRAINT fk_challenges_challenger FOREIGN KEY (challenger_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_challenges_challenged FOREIGN KEY (challenged_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_challenges_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (quiz_id) ON DELETE CASCADE,
    CONSTRAINT fk_challenges_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts (attempt_id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Messages between users:
--   note: a note, or a reply to one (thread_id = the note that started the conversation)
--   challenge_accepted, challenge_declined, challenge_finished: tell a challenger what
--   happened to their challenge (challenge_id)
CREATE TABLE messages (
    message_id   INT           NOT NULL AUTO_INCREMENT,
    sender_id    INT           NOT NULL,
    receiver_id  INT           NOT NULL,
    message_type ENUM ('note', 'challenge_accepted', 'challenge_declined', 'challenge_finished') NOT NULL,
    -- Optional for challenge messages (accepting or declining without a comment).
    content      VARCHAR(1000) NULL,
    thread_id    INT           NULL,
    challenge_id INT           NULL,
    is_read      BOOLEAN       NOT NULL DEFAULT FALSE,
    sent_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id),
    KEY idx_messages_receiver (receiver_id, sent_at),
    KEY idx_messages_thread (thread_id),
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_receiver FOREIGN KEY (receiver_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_thread FOREIGN KEY (thread_id) REFERENCES messages (message_id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_challenge FOREIGN KEY (challenge_id) REFERENCES challenges (challenge_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Comments that players leave on a quiz after taking it (at most one per attempt).
CREATE TABLE quiz_comments (
    comment_id INT           NOT NULL AUTO_INCREMENT,
    quiz_id    INT           NOT NULL,
    user_id    INT           NOT NULL,
    attempt_id INT           NULL,
    content    VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id),
    UNIQUE KEY uq_comments_attempt (attempt_id),
    KEY idx_comments_quiz (quiz_id, created_at),
    CONSTRAINT fk_comments_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (quiz_id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts (attempt_id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Star ratings (1 to 5) that players give a quiz after taking it; one per player, which they can change.
CREATE TABLE quiz_ratings (
    quiz_id INT     NOT NULL,
    user_id INT     NOT NULL,
    rating  TINYINT NOT NULL,
    PRIMARY KEY (quiz_id, user_id),
    CONSTRAINT chk_ratings_range CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_ratings_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (quiz_id) ON DELETE CASCADE,
    CONSTRAINT fk_ratings_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- Achievements. The rows below are reference data: AchievementService
-- refers to them by id, so they are part of the schema, not the demo data.
-- "icon" is a Bootstrap Icons class name.
-- ---------------------------------------------------------------------
CREATE TABLE achievements (
    achievement_id INT          NOT NULL,
    name           VARCHAR(100) NOT NULL,
    description    VARCHAR(255) NOT NULL,
    icon           VARCHAR(50)  NOT NULL,
    PRIMARY KEY (achievement_id),
    UNIQUE KEY uq_achievements_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_achievements (
    user_id        INT       NOT NULL,
    achievement_id INT       NOT NULL,
    earned_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, achievement_id),
    CONSTRAINT fk_user_achievements_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_achievements_achievement FOREIGN KEY (achievement_id) REFERENCES achievements (achievement_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO achievements (achievement_id, name, description, icon) VALUES
    (1, 'Amateur Author',    'Created your first quiz.',              'bi-pencil-fill'),
    (2, 'Prolific Author',   'Created 5 quizzes.',                    'bi-journal-bookmark-fill'),
    (3, 'Prodigious Author', 'Created 10 quizzes.',                   'bi-journals'),
    (4, 'Quiz Machine',      'Finished 10 quizzes.',                  'bi-lightning-charge-fill'),
    (5, 'I am the Greatest', 'Got the highest score on a quiz.',      'bi-trophy-fill');

-- ---------------------------------------------------------------------
-- Site-wide announcements written by administrators.
-- ---------------------------------------------------------------------
CREATE TABLE announcements (
    announcement_id INT           NOT NULL AUTO_INCREMENT,
    title           VARCHAR(150)  NOT NULL,
    content         VARCHAR(2000) NOT NULL,
    created_by      INT           NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    PRIMARY KEY (announcement_id),
    CONSTRAINT fk_announcements_user FOREIGN KEY (created_by) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
