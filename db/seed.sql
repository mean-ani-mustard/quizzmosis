-- =====================================================================
--  Demo data for Quizzmosis. Load it after schema.sql:
--      mysql -u root -p quiz_db < db/seed.sql
--
--  Every demo account uses the password:  password123
--      alice (administrator), bob, carol, dave, erin, frank, grace
--
--  Dates are relative to NOW(), so the "today" / "this week" filters
--  always have something to show.
-- =====================================================================

-- This file is UTF-8. Without this line the mysql client guesses the encoding from the
-- computer's language settings (inside Docker it guesses Latin-1), and accented letters
-- such as the "í" in "Brasília" would be stored garbled.
SET NAMES utf8mb4;

INSERT INTO users (user_id, username, password_hash, display_name, mantra, show_challenge_history, is_admin, created_at, last_login) VALUES
    (1, 'alice', 'pbkdf2_sha256$600000$MyV/1UqIREbzNN4f2vDCHg==$ovftHuXjs0WYtPDcloXwIIu2QfSGSXuwP5ME4vU/Gl0=', 'Alice Johnson', 'Curiosity first, answers second.', TRUE, TRUE,  NOW() - INTERVAL 60 DAY, NOW() - INTERVAL 1 DAY),
    (2, 'bob',   'pbkdf2_sha256$600000$iNmK3m6Nv+djwc83mXi/Lg==$+qpgG7qGaZX5ejCwFdTZCmJtHY7omM6Oj0/3hcQEdko=', 'Bob Smith',     'Coffee, code, quizzes.',          TRUE, FALSE, NOW() - INTERVAL 55 DAY, NOW() - INTERVAL 3 HOUR),
    (3, 'carol', 'pbkdf2_sha256$600000$Juz0Eo47PaeQx/8UVFLkow==$egI/VULDp/oEYeyKXwi0QHWAOgsI996l2BZRWxmnudM=', 'Carol Lee',     'Never stop learning.',            FALSE, FALSE, NOW() - INTERVAL 50 DAY, NOW() - INTERVAL 2 DAY),
    (4, 'dave',  'pbkdf2_sha256$600000$LDr5zjqBAlgoIN61dWEGmA==$DTSsx0k68j76kqi14xfBXQauo9L+5zmauwODR2BHL1Q=', 'Dave Brown',    NULL,                              TRUE, FALSE, NOW() - INTERVAL 45 DAY, NOW() - INTERVAL 5 HOUR),
    (5, 'erin',  'pbkdf2_sha256$600000$3nGUNXALpwLTrjwA0Cqjig==$rCh3h/h21OT0fQ/YWzHeoPctK6yvIqxVEaHN8H3L3Gc=', 'Erin Davis',    'New here - say hi!',              TRUE, FALSE, NOW() - INTERVAL 10 DAY, NULL),
    (6, 'frank', 'pbkdf2_sha256$600000$1rpL/nOJnqF6xR+ruXM9AA==$eEOz0D5I0TZL21tj+1peWB8K8QZdYKrMn56O+iD6K0I=', 'Frank Miller',  'Trivia night champion (self-declared).', TRUE, FALSE, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 1 DAY),
    (7, 'grace', 'pbkdf2_sha256$600000$uR/yfF+BwOgZU1w2siC87g==$eLOVPWKVvCeg2WWm6VuLcuHOoVYQbGC2lcVm5DCqacU=', 'Grace Kim',     NULL,                              TRUE, FALSE, NOW() - INTERVAL 8 DAY,  NOW() - INTERVAL 2 DAY);

-- ---------------------------------------------------------------------
-- Quizzes
-- ---------------------------------------------------------------------
-- Quiz 4 has a time limit of 2 minutes. Quiz 5 is only for bob's friends (alice and dave);
-- quiz 6 is alice's private practice quiz.
INSERT INTO quizzes (quiz_id, owner_id, title, description, random_order, multiple_pages, immediate_correction, visibility, time_limit_seconds, creation_date) VALUES
    (1, 1, 'World Capitals',        'How well do you know the capital cities of the world? Five quick questions, all on one page.', TRUE,  FALSE, FALSE, 'public',  NULL, NOW() - INTERVAL 30 DAY),
    (2, 2, 'Java Basics',           'Check your core Java knowledge. One question per page with instant feedback.',               FALSE, TRUE,  TRUE,  'public',  NULL, NOW() - INTERVAL 25 DAY),
    (3, 3, 'Famous Sights',         'Name the landmark, painting or animal in each picture.',                                       FALSE, TRUE,  FALSE, 'public',  NULL, NOW() - INTERVAL 20 DAY),
    (4, 4, 'Solar System',          'Planets, orbits and other space trivia. You have two minutes!',                                  TRUE,  FALSE, FALSE, 'public',  120,  NOW() - INTERVAL 15 DAY),
    (5, 2, 'How well do you know Bob?', 'A quiz for my friends only. No cheating!',                                                  FALSE, FALSE, FALSE, 'friends', NULL, NOW() - INTERVAL 10 DAY),
    (6, 1, 'Spanish Vocabulary',    'My own practice quiz for Spanish class.',                                                        TRUE,  FALSE, FALSE, 'private', NULL, NOW() - INTERVAL 5 DAY);

INSERT INTO questions (question_id, quiz_id, question_type, question_text, image_url, question_order) VALUES
    -- World Capitals
    (1,  1, 'multiple_choice',   'What is the capital of Australia?', NULL, 1),
    (2,  1, 'question_response', 'What is the capital of Japan?', NULL, 2),
    (3,  1, 'multiple_choice',   'Which city is the capital of Canada?', NULL, 3),
    (4,  1, 'fill_blank',        '____ is the capital of Georgia (the country).', NULL, 4),
    (5,  1, 'question_response', 'What is the capital of Brazil?', NULL, 5),
    -- Java Basics
    (6,  2, 'multiple_choice',   'Which keyword makes one class a subclass of another?', NULL, 1),
    (7,  2, 'fill_blank',        'A Java program starts running in the ____ method.', NULL, 2),
    (8,  2, 'multiple_choice',   'Which of these is NOT a primitive type?', NULL, 3),
    (9,  2, 'question_response', 'What does JVM stand for?', NULL, 4),
    (10, 2, 'multiple_choice',   'What is the default value of an int field?', NULL, 5),
    -- Famous Sights (images from Wikimedia Commons)
    (11, 3, 'picture_response',  'Which famous tower is this?', 'https://upload.wikimedia.org/wikipedia/commons/thumb/a/a8/Tour_Eiffel_Wikimedia_Commons.jpg/500px-Tour_Eiffel_Wikimedia_Commons.jpg', 1),
    (12, 3, 'picture_response',  'Name this painting.', 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ec/Mona_Lisa,_by_Leonardo_da_Vinci,_from_C2RMF_retouched.jpg/500px-Mona_Lisa,_by_Leonardo_da_Vinci,_from_C2RMF_retouched.jpg', 2),
    (13, 3, 'picture_response',  'Which monument is this?', 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/bd/Taj_Mahal,_Agra,_India_edit3.jpg/500px-Taj_Mahal,_Agra,_India_edit3.jpg', 3),
    (14, 3, 'picture_response',  'What kind of bird is this?', 'https://upload.wikimedia.org/wikipedia/commons/thumb/4/45/Eopsaltria_australis_-_Mogo_Campground.jpg/500px-Eopsaltria_australis_-_Mogo_Campground.jpg', 4),
    -- Solar System
    (15, 4, 'multiple_choice',   'Which planet is known as the Red Planet?', NULL, 1),
    (16, 4, 'multiple_choice',   'What is the largest planet in our solar system?', NULL, 2),
    (17, 4, 'question_response', 'How many planets are in our solar system?', NULL, 3),
    (18, 4, 'fill_blank',        'The planet closest to the Sun is ____.', NULL, 4),
    -- How well do you know Bob? (friends only)
    (19, 5, 'multiple_choice',   'In which month is my birthday?', NULL, 1),
    (20, 5, 'question_response', 'What is my favourite programming language?', NULL, 2),
    -- Spanish Vocabulary (private)
    (21, 6, 'question_response', 'How do you say "thank you" in Spanish?', NULL, 1),
    (22, 6, 'question_response', 'How do you say "cat" in Spanish?', NULL, 2),
    (23, 6, 'multiple_choice',   'Which word means "house"?', NULL, 3);

INSERT INTO question_choices (question_id, choice_text, is_correct) VALUES
    (1, 'Sydney', FALSE), (1, 'Canberra', TRUE), (1, 'Melbourne', FALSE), (1, 'Perth', FALSE),
    (3, 'Toronto', FALSE), (3, 'Vancouver', FALSE), (3, 'Ottawa', TRUE), (3, 'Montreal', FALSE),
    (6, 'extends', TRUE), (6, 'implements', FALSE), (6, 'inherits', FALSE), (6, 'super', FALSE),
    (8, 'int', FALSE), (8, 'boolean', FALSE), (8, 'String', TRUE), (8, 'double', FALSE),
    (10, '0', TRUE), (10, 'null', FALSE), (10, '1', FALSE), (10, 'It has no default value', FALSE),
    (15, 'Venus', FALSE), (15, 'Mars', TRUE), (15, 'Jupiter', FALSE), (15, 'Mercury', FALSE),
    (16, 'Saturn', FALSE), (16, 'Neptune', FALSE), (16, 'Jupiter', TRUE), (16, 'Earth', FALSE),
    (19, 'March', FALSE), (19, 'July', TRUE), (19, 'October', FALSE), (19, 'December', FALSE),
    (23, 'casa', TRUE), (23, 'perro', FALSE), (23, 'libro', FALSE);

INSERT INTO question_answers (question_id, answer_text) VALUES
    (2, 'Tokyo'),
    (4, 'Tbilisi'),
    (5, 'Brasilia'), (5, 'Brasília'),
    (7, 'main'),
    (9, 'Java Virtual Machine'),
    (11, 'Eiffel Tower'), (11, 'The Eiffel Tower'), (11, 'Eiffel'),
    (12, 'Mona Lisa'), (12, 'The Mona Lisa'), (12, 'La Gioconda'),
    (13, 'Taj Mahal'), (13, 'The Taj Mahal'),
    (14, 'Eastern Yellow Robin'), (14, 'Yellow Robin'), (14, 'Robin'),
    (17, '8'), (17, 'eight'),
    (18, 'Mercury'),
    (20, 'Java'),
    (21, 'gracias'),
    (22, 'gato');

-- ---------------------------------------------------------------------
-- Finished attempts (alice's include their answers, so her results pages have details)
-- ---------------------------------------------------------------------
INSERT INTO quiz_attempts (attempt_id, user_id, quiz_id, score, total_questions, duration_seconds, attempt_date) VALUES
    (1,  2, 1, 4, 5,  95, NOW() - INTERVAL 3 DAY),
    (2,  3, 1, 5, 5, 120, NOW() - INTERVAL 2 DAY),
    (3,  4, 1, 3, 5,  80, NOW() - INTERVAL 5 HOUR),
    (4,  1, 2, 4, 5, 140, NOW() - INTERVAL 1 DAY),
    (5,  3, 2, 5, 5, 150, NOW() - INTERVAL 20 DAY),
    (6,  5, 2, 2, 5, 200, NOW() - INTERVAL 3 HOUR),
    (7,  1, 3, 4, 4,  75, NOW() - INTERVAL 10 DAY),
    (8,  4, 3, 2, 4,  90, NOW() - INTERVAL 6 DAY),
    (9,  2, 4, 3, 4,  60, NOW() - INTERVAL 2 HOUR),
    (10, 1, 4, 4, 4,  70, NOW() - INTERVAL 40 DAY),
    (11, 4, 5, 2, 2,  25, NOW() - INTERVAL 4 DAY),
    (12, 1, 6, 2, 3,  40, NOW() - INTERVAL 1 DAY);

INSERT INTO attempt_answers (attempt_id, question_id, user_answer_text, correct_answer, is_correct) VALUES
    (4, 6,  'extends', 'extends', TRUE),
    (4, 7,  'main', 'main', TRUE),
    (4, 8,  'boolean', 'String', FALSE),
    (4, 9,  'Java Virtual Machine', 'Java Virtual Machine', TRUE),
    (4, 10, '0', '0', TRUE),
    (7, 11, 'Eiffel Tower', 'Eiffel Tower / The Eiffel Tower / Eiffel', TRUE),
    (7, 12, 'Mona Lisa', 'Mona Lisa / The Mona Lisa / La Gioconda', TRUE),
    (7, 13, 'taj mahal', 'Taj Mahal / The Taj Mahal', TRUE),
    (7, 14, 'Robin', 'Eastern Yellow Robin / Yellow Robin / Robin', TRUE),
    (10, 15, 'Mars', 'Mars', TRUE),
    (10, 16, 'Jupiter', 'Jupiter', TRUE),
    (10, 17, '8', '8 / eight', TRUE),
    (10, 18, 'Mercury', 'Mercury', TRUE),
    (12, 21, 'gracias', 'gracias', TRUE),
    (12, 22, 'gata', 'gato', FALSE),
    (12, 23, 'casa', 'casa', TRUE);

-- ---------------------------------------------------------------------
-- Friends: alice-bob, alice-carol, bob-dave, frank-bob, frank-carol and grace-carol are friends,
-- erin sent alice a request, alice sent dave a request. So frank (2 friends in common with
-- alice) and grace (1) are "suggested for you" on alice's friends page.
-- ---------------------------------------------------------------------
INSERT INTO friends (user_id, friend_id, status, request_date) VALUES
    (1, 2, 'accepted', NOW() - INTERVAL 40 DAY), (2, 1, 'accepted', NOW() - INTERVAL 40 DAY),
    (1, 3, 'accepted', NOW() - INTERVAL 35 DAY), (3, 1, 'accepted', NOW() - INTERVAL 35 DAY),
    (2, 4, 'accepted', NOW() - INTERVAL 30 DAY), (4, 2, 'accepted', NOW() - INTERVAL 30 DAY),
    (6, 2, 'accepted', NOW() - INTERVAL 15 DAY), (2, 6, 'accepted', NOW() - INTERVAL 15 DAY),
    (6, 3, 'accepted', NOW() - INTERVAL 12 DAY), (3, 6, 'accepted', NOW() - INTERVAL 12 DAY),
    (7, 3, 'accepted', NOW() - INTERVAL 6 DAY),  (3, 7, 'accepted', NOW() - INTERVAL 6 DAY),
    (5, 1, 'pending',  NOW() - INTERVAL 1 DAY),
    (1, 4, 'pending',  NOW() - INTERVAL 2 DAY);

-- ---------------------------------------------------------------------
-- Challenges: carol -> alice and alice -> bob are waiting for an answer, carol declined one from
-- alice, and bob's challenges to alice and dave were accepted and completed. carol hides her
-- challenge history from her profile (see users.show_challenge_history).
-- ---------------------------------------------------------------------
INSERT INTO challenges (challenge_id, challenger_id, challenged_id, quiz_id, message, status, response_comment, attempt_id, created_at, responded_at, completed_at) VALUES
    (1, 3, 1, 1, 'I got a perfect score on World Capitals. Your turn!', 'pending',  NULL, NULL, NOW() - INTERVAL 1 DAY, NULL, NULL),
    (2, 1, 2, 4, 'Think you know the planets better than me?',          'pending',  NULL, NULL, NOW() - INTERVAL 3 DAY, NULL, NULL),
    (3, 2, 1, 2, 'My quiz, my rules: beat my friends!',                 'completed', 'You''re on!', 4, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 25 HOUR, NOW() - INTERVAL 1 DAY),
    (4, 1, 3, 4, 'Planets time!',                                       'declined', 'Space is not my thing, sorry!', NULL, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 5 DAY, NULL),
    (5, 2, 4, 5, 'How well do you REALLY know me?',                     'completed', 'Easy!', 11, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 97 HOUR, NOW() - INTERVAL 4 DAY);

-- Notes (bob and alice have a conversation of three messages) and what challengers were told.
INSERT INTO messages (message_id, sender_id, receiver_id, message_type, content, thread_id, challenge_id, is_read, sent_at) VALUES
    (1, 2, 1, 'note', 'Hey Alice! I just published a Java quiz - let me know what you think.', NULL, NULL, TRUE,  NOW() - INTERVAL 5 HOUR),
    (2, 1, 2, 'note', 'Just played it. The primitive types question got me!',                  1,    NULL, TRUE,  NOW() - INTERVAL 4 HOUR),
    (3, 2, 1, 'note', 'Ha, that one catches everybody.',                                      1,    NULL, FALSE, NOW() - INTERVAL 2 HOUR),
    (4, 4, 2, 'note', 'Nice quiz, the default value question got me!',                        NULL, NULL, TRUE,  NOW() - INTERVAL 1 DAY),
    (5, 1, 2, 'challenge_accepted', 'You''re on!',                   NULL, 3, TRUE,  NOW() - INTERVAL 25 HOUR),
    (6, 1, 2, 'challenge_finished', NULL,                            NULL, 3, FALSE, NOW() - INTERVAL 1 DAY),
    (7, 3, 1, 'challenge_declined', 'Space is not my thing, sorry!', NULL, 4, FALSE, NOW() - INTERVAL 5 DAY),
    (8, 4, 2, 'challenge_accepted', 'Easy!',                         NULL, 5, TRUE,  NOW() - INTERVAL 97 HOUR),
    (9, 4, 2, 'challenge_finished', NULL,                            NULL, 5, TRUE,  NOW() - INTERVAL 4 DAY);

-- Comments players left after taking a quiz (alice's also reaches bob with her challenge result).
INSERT INTO quiz_comments (quiz_id, user_id, attempt_id, content, created_at) VALUES
    (1, 2, 1,  'Tbilisi?! I had to look that one up.',          NOW() - INTERVAL 3 DAY),
    (1, 3, 2,  'Loved it. Canberra catches everyone out.',       NOW() - INTERVAL 2 DAY),
    (2, 1, 4,  'Great quiz, Bob. The primitive types question got me!', NOW() - INTERVAL 1 DAY),
    (2, 5, 6,  'Harder than I thought!',                         NOW() - INTERVAL 3 HOUR),
    (5, 4, 11, 'Easy, I have known Bob for years.',              NOW() - INTERVAL 4 DAY);

-- Star ratings from players who took the quiz (authors don't rate their own quizzes).
INSERT INTO quiz_ratings (quiz_id, user_id, rating) VALUES
    (1, 2, 4), (1, 3, 5), (1, 4, 4),
    (2, 1, 5), (2, 3, 4), (2, 5, 3),
    (3, 1, 5), (3, 4, 4),
    (4, 2, 3), (4, 1, 4),
    (5, 4, 5);

INSERT INTO user_achievements (user_id, achievement_id, earned_at) VALUES
    (1, 1, NOW() - INTERVAL 30 DAY),
    (1, 5, NOW() - INTERVAL 40 DAY),
    (2, 1, NOW() - INTERVAL 25 DAY),
    (3, 1, NOW() - INTERVAL 20 DAY),
    (3, 5, NOW() - INTERVAL 2 DAY),
    (4, 1, NOW() - INTERVAL 15 DAY);

INSERT INTO announcements (title, content, created_by, created_at, is_active) VALUES
    ('Welcome to Quizzmosis!', 'Create your own quizzes, challenge your friends and climb the leaderboard. Have fun!', 1, NOW() - INTERVAL 7 DAY, TRUE),
    ('New: picture questions', 'Quiz authors can now add picture-response questions: paste an image URL and players name what they see.', 1, NOW() - INTERVAL 1 DAY, TRUE),
    ('Scheduled maintenance', 'The site may be unavailable for a few minutes on Sunday night.', 1, NOW() - INTERVAL 30 DAY, FALSE);
