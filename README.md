# Quizzmosis

A quiz website where people create quizzes, take them, challenge their friends and climb the
leaderboard. Built with Java 21, Jakarta Servlets and JSP, and MySQL.

![Home page](docs/screenshots/home.webp)

## Features

- **Four question types**: question-response, fill in the blank, multiple choice and
  picture-response. Typed answers can have several accepted spellings and are checked ignoring
  upper/lower case and extra spaces.
- **Quiz settings**: random question order, all questions on one page or one per page,
  immediate feedback after every answer, and an optional **time limit** with a countdown (the
  server checks the time too). Authors can change the title, the settings and every question
  later.
- **Private quizzes**: every quiz is *Public*, *Friends only* or *Only me*, and the author can
  change it at any time. A quiz someone isn't allowed to see never shows up for them: not in
  lists, search, leaderboards, profiles, friends' activity or challenges.
- **Results and statistics**: a review of every answer after a quiz; per-quiz statistics, your own
  attempts (sortable by date, score or time), top players of all time and of the last 24 hours.
- **Leaderboard** across all quizzes or one quiz, for all time, today, the last 7 or 30 days.
  Everyone appears once per quiz, with their best result.
- **Challenges**: challenge a friend to a quiz. They accept it (and play right away) or decline it,
  with an optional comment, and the challenger gets a message either way, then the score once
  the quiz is finished. Challenges are never deleted: they form a history that people can hide
  from their profile.
- **Friends**: friend requests, "suggested for you" (friends of friends, the most mutual friends
  first), a list of everyone else to browse, and a quick profile preview, with the friends you
  have in common, before you add someone. The home page shows what your friends played recently.
- **Notes**: conversations between friends, with replies underneath.
- **Profiles** with an avatar, a personal mantra, achievements, quizzes, recent results, mutual
  friends and the challenge history.
- **Ratings and comments**: after finishing a quiz, players rate it from 1 to 5 stars and can
  leave a comment, which appears on the quiz page next to their score (and reaches a friend who
  challenged them). The quiz list can be sorted by best rated, most played or newest.
- **Account settings**: change your display name or password, or delete your account.
- **Achievements** for writing quizzes and for playing them.
- **Administration**: announcements, granting and removing admin rights, deleting users and
  quizzes, clearing a quiz's results.
- Works on phones as well as on large screens. Long lists come in pages of 20, and the number
  of new messages updates by itself.

## Screenshots

| | |
|---|---|
| ![Quizzes sorted by rating](docs/screenshots/quiz-list.webp) | ![Quiz page with statistics](docs/screenshots/quiz-details.webp) |
| ![Taking a quiz against the clock](docs/screenshots/take-quiz.webp) | ![Immediate feedback](docs/screenshots/immediate-feedback.webp) |
| ![Results](docs/screenshots/result.webp) | ![Creating a quiz](docs/screenshots/create-quiz.webp) |
| ![Editing a question](docs/screenshots/manage-questions.webp) | ![Answering challenges](docs/screenshots/messages.webp) |
| ![Friend suggestions and a profile preview](docs/screenshots/friends.webp) | ![Profile with mantra and challenge history](docs/screenshots/profile.webp) |
| ![A conversation with replies](docs/screenshots/notes.webp) | ![Comments on a quiz](docs/screenshots/quiz-comments.webp) |
| ![Leaderboard](docs/screenshots/leaderboard.webp) | ![Achievements](docs/screenshots/achievements.webp) |
| ![Account settings](docs/screenshots/settings.webp) | ![Administration](docs/screenshots/admin.webp) |

<p>
  <img src="docs/screenshots/mobile-home.webp" alt="Home page on a phone" width="260">
  &nbsp;
  <img src="docs/screenshots/mobile-quiz.webp" alt="Quiz page on a phone" width="260">
</p>

## Run it

### With Docker (easiest)

You only need [Docker](https://docs.docker.com/get-docker/).

```bash
docker compose up --build
```

Then open <http://localhost:8080>. The database starts with demo data; every demo account uses the
password `password123`:

| Username | Role |
|---|---|
| `alice` | administrator |
| `bob`, `carol`, `dave`, `erin`, `frank`, `grace` | regular users |

Things to try as `alice`: carol's challenge is waiting in **Messages**, bob replied to her note,
frank and grace are suggested on the **Friends** page, and "Solar System" has a two-minute time
limit. Log in as `carol` or `bob` in another
browser to see the other side of a challenge.

To see private quizzes at work: bob's "How well do you know Bob?" is for his friends only (alice
and dave) and alice's "Spanish Vocabulary" is hers alone, so neither exists for `carol`.

`docker compose down -v` stops everything and deletes the database, so the next start is fresh.
To change the passwords or ports, copy `.env.example` to `.env` and edit it.

### Without Docker

You need JDK 21, Maven 3.9+, MySQL 8 and Tomcat 10.1 (or 11).

1. Create the database and load the tables and demo data:
   ```bash
   mysql -u root -p -e "CREATE DATABASE quiz_db"
   mysql -u root -p quiz_db < db/schema.sql
   mysql -u root -p quiz_db < db/seed.sql
   ```
2. Tell the app how to reach the database, with environment variables (or Java system
   properties) in the environment Tomcat runs in:

   | Environment variable | System property | Default |
   |---|---|---|
   | `DB_URL` | `db.url` | `jdbc:mysql://localhost:3306/quiz_db` |
   | `DB_USER` | `db.user` | `root` |
   | `DB_PASSWORD` | `db.password` | *(empty)* |

3. Build the WAR with `mvn package` and deploy `target/quizzmosis.war` to Tomcat, for example by
   copying it into Tomcat's `webapps` folder. The site is then at
   <http://localhost:8080/quizzmosis/>. (In IntelliJ IDEA, a Tomcat run configuration that
   deploys the `quizzmosis:war exploded` artifact does the same.)

### Online, for free

[docs/DEPLOY.md](docs/DEPLOY.md) puts the site on the internet at no cost: the database on
Aiven's free MySQL plan and the website on Render's free plan, set up by
[`render.yaml`](render.yaml). A scheduled GitHub Actions workflow
([`reset-demo.yml`](.github/workflows/reset-demo.yml)) reloads the demo data every night.

## Tests

```bash
mvn test      # unit tests: no database needed
mvn verify    # unit tests + integration tests against MySQL
```

- **Unit tests** (`*Test`) cover the logic that needs no database: password hashing, grading,
  the quiz-taking state, registration and login rules (with an in-memory stand-in for the
  database), and formatting.
- **Integration tests** (`*IT`) run every DAO and service against a real MySQL database: SQL
  queries, cascading deletes, leaderboard ranking, time filters, friendships, challenges,
  conversations, comments and achievements.
  They use a database called `quiz_db_test`, which is created if it doesn't exist. Because they
  wipe it, they refuse to run against any database whose name doesn't end in `_test`.

The quickest way to get a MySQL server for the integration tests is the one from Docker Compose:

```bash
docker compose up -d --wait db     # --wait: until MySQL is ready
mvn verify -Dtest.db.password=root
```

Use `-Dtest.db.url`, `-Dtest.db.user` and `-Dtest.db.password` to point the tests at another
server. On every push, GitHub Actions ([ci.yml](.github/workflows/ci.yml)) runs all tests against
a fresh MySQL database, and a second job builds the Docker image, starts it and logs in to the
running site as a smoke test.

## How it's built

| Layer | Technology |
|---|---|
| Web | Jakarta Servlet 6 and JSP 3.1 with JSTL, on Tomcat 10.1 |
| Front end | Bootstrap 5 and Bootstrap Icons; a little plain JavaScript |
| Data | MySQL 8 through plain JDBC, with the HikariCP connection pool |
| Build and tests | Maven, JUnit 5 |
| Delivery | Docker, Docker Compose, GitHub Actions, a Render Blueprint for hosting |

```mermaid
flowchart LR
    Browser -- request --> Filters["Filters<br>login → CSRF → admin"]
    Filters --> Servlets["Servlets<br>one per page"]
    Servlets --> Services["Services<br>login, grading, challenges, achievements"]
    Servlets --> DAOs
    Services --> DAOs["DAOs<br>all the SQL"]
    DAOs -- HikariCP --> MySQL[("MySQL")]
    Servlets -- forward --> JSP["JSP views"]
    JSP -- HTML --> Browser
```

```
src/main/java/com/quizzmosis/
├── model/      plain data classes: User, Quiz, Question, QuizAttempt, QuizSession, ...
├── dao/        every SQL query, one class per topic (UserDAO, QuizDAO, FriendsDAO, ...)
├── service/    rules that are more than a query: AuthService, QuizGrader, QuizService,
│               ChallengeService, AchievementService, QuizAccess
├── security/   PasswordHasher
├── util/       small formatting helpers
└── web/        servlets (one per page), filters, and helpers shared by the servlets
src/main/webapp/
├── WEB-INF/views/       the JSP pages (only reachable through a servlet)
├── WEB-INF/fragments/   pieces the pages share: <head>, navigation bar, footer, quiz card, ...
├── WEB-INF/tags/        a custom tag: <q:avatar>
├── WEB-INF/quizzmosis.tld   an EL function for the avatars' initials
└── static/              CSS, JavaScript and the icon
db/
├── schema.sql   creates the tables
└── seed.sql     demo users, quizzes, results, challenges and messages
```

A few design decisions:

- **A quiz is saved only when it's finished.** While someone plays, their answers live in a
  `QuizSession` object in the HTTP session. Finishing saves the attempt and all answers in one
  database transaction, so abandoned quizzes never leave half-finished results behind, and
  every question can be answered only once.
- **Post/Redirect/Get.** Every form submission ends with a redirect, so refreshing a page never
  submits a form twice. Success and error messages survive the redirect as "flash" messages.
- **One visibility rule, written once.** Every list gets the same SQL condition
  (`QuizDAO.VISIBLE_TO`), and single quiz pages ask `QuizAccess`, which applies the same rule.
  Admins may open any quiz, so they can check it before deleting it, but their lists stay normal.
- **The server keeps the time.** The countdown on the page only helps the player; the quiz
  session knows when it started, so answers that arrive after the time limit (plus a few seconds
  for the page's own last-second submit) don't count.
- **Long lists are read a page at a time** with `LIMIT` and `OFFSET`, plus a `COUNT(*)` for the
  number of pages (`util.Page`).
- **Challenges are never deleted.** A challenge only changes its status (waiting, accepted,
  completed or declined), so the table doubles as the challenge history. Hiding the history
  sets a flag on the user; nothing is removed.
- **A conversation is a note plus its replies.** Each reply points to the first note of its
  conversation (`thread_id`), so one query loads all of a user's notes and Java groups them.
- **The database does the heavy lifting.** Deleting a user or a quiz removes everything that
  belongs to it through `ON DELETE CASCADE`, and the leaderboards pick each player's best
  attempt with the `ROW_NUMBER()` window function instead of loading everything into Java.
- **No passwords in the code.** Database settings come from environment variables, and user
  passwords are only stored as salted hashes.

## Security

- Passwords are hashed with PBKDF2-HMAC-SHA256 (600,000 iterations, random salt per user) and
  compared in constant time. Failed logins take the same time whether or not the username exists.
- Every query uses `PreparedStatement` parameters. Sort orders come from a fixed list, and `%`
  and `_` in search text are matched literally.
- Everything users type is HTML-escaped on output (`<c:out>`, `fn:escapeXml`), and picture links
  must be `http(s)` URLs.
- Every form that changes something carries a CSRF token, which `CsrfFilter` checks. Logging out
  is a form too.
- Changing the password needs the current one, and deleting an account needs the password; the
  only administrator can't delete their account.
- Logging in and changing the password issue a new session id. The session cookie is `HttpOnly`
  and `SameSite=Lax`, and it is never put in URLs.
- `AuthFilter` lets only logged-in users in and reloads them from the database on every request,
  so removed admin rights take effect immediately. `AdminFilter` guards `/admin`, and servlets
  check ownership: only a quiz's author can edit it, only the player (or an admin) can open
  a detailed result or comment on it, only the challenged friend can answer a challenge (once),
  and only the two people in a conversation can reply to it, while they are friends.
- A quiz that someone may not see answers with "not found", exactly like a quiz that doesn't
  exist, so its title doesn't leak.
- Error pages never show stack traces.

## Credits

Quizzmosis was created by team **OverflowQuartet** as the final project of a university
object-oriented programming course. This version was later reworked: security fixes, a clearer
structure, tests, Docker and CI.

## License

Released under the [MIT License](LICENSE).
