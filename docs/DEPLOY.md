# Putting Quizzmosis online for free

This guide puts the site on the internet at no cost, so the README can link to a live demo. It
uses two free services:

- **[Aiven](https://aiven.io/free-mysql-database)** runs the MySQL database.
- **[Render](https://render.com/docs/free)** builds the Docker image from the `Dockerfile` and
  runs the website. The file [`render.yaml`](../render.yaml) describes the service, so Render
  asks only for the database settings.

A GitHub Actions workflow ([`reset-demo.yml`](../.github/workflows/reset-demo.yml)) can also
reload the demo data every night (step 5).

You need a GitHub account with the project pushed to it, and the `mysql` command-line client on
your computer (it comes with MySQL; on a Mac, `brew install mysql` installs it).

## What to expect from free plans

- Render's free server goes to sleep after 15 minutes without visitors. The next visit wakes it
  up, which takes a minute or so. Logged-in visitors are logged out when it sleeps.
- The free server gets only a small share of a processor. Each page is a little slow the first
  time it's opened after waking up (Tomcat compiles it), and logging in takes a few seconds,
  because passwords are hashed with 600,000 PBKDF2 rounds on purpose.
- Aiven can switch off a free database that hasn't been used for a while. You can switch it back
  on in the Aiven console.
- Everyone can log in with the demo accounts, including `alice`, the administrator. That's what a
  demo is for, and the nightly reset from step 5 undoes whatever visitors change or delete.

Free plans change from time to time, so check the current limits on
[Render's free plan page](https://render.com/docs/free) and [Aiven's free tier
page](https://aiven.io/docs/products/mysql/concepts/mysql-free-tier).

## 1. Create the database on Aiven

1. Sign up at [aiven.io](https://aiven.io/free-mysql-database). The free plan needs no credit card.
2. Create a new service: choose **MySQL**, then the **Free** plan, and a region. Pick a region
   near the one you'll use on Render (`render.yaml` says `frankfurt`): every page runs several
   queries, so a database on another continent slows every page down.
3. Wait until the service says **Running** (a few minutes).
4. Keep its **Overview** page open. Under **Connection information** you'll find the host, the
   port, the user (`avnadmin`), the password and the database name (`defaultdb`).

## 2. Load the tables and the demo data

In a terminal, go to the project folder and run these two commands, with the host and port from
Aiven in place of `HOST` and `PORT`. Each one asks for the Aiven password.

```bash
mysql --host=HOST --port=PORT --user=avnadmin --password --ssl-mode=REQUIRED defaultdb < db/schema.sql
mysql --host=HOST --port=PORT --user=avnadmin --password --ssl-mode=REQUIRED defaultdb < db/seed.sql
```

To check that it worked, list the demo users (you should see seven, from alice to grace):

```bash
mysql --host=HOST --port=PORT --user=avnadmin --password --ssl-mode=REQUIRED \
      --execute="SELECT username FROM users" defaultdb
```

## 3. Start the website on Render

1. Sign up at [render.com](https://render.com) with your GitHub account.
2. In the dashboard, choose **New → Blueprint** and pick the Quizzmosis repository. Render reads
   `render.yaml`: one free web service called `quizzmosis`, built from the `Dockerfile`.
3. Render asks for three values:

   | Variable | Value |
   |---|---|
   | `DB_URL` | `jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED` (host and port from Aiven) |
   | `DB_USER` | `avnadmin` |
   | `DB_PASSWORD` | the Aiven password |

   `sslMode=REQUIRED` encrypts the connection between the two services; Aiven accepts only
   encrypted connections.
4. Confirm. The first build takes a few minutes, because Maven downloads the libraries. When the
   log shows `Quizzmosis started, connected to MySQL`, the site is running and has reached the
   database.
5. Open the address at the top of the service's page (something like
   `https://quizzmosis.onrender.com`) and log in as `alice` with the password `password123`.

From now on, every push to GitHub updates the site automatically.

## 4. Link the demo from the README

Add a line like this under the title of `README.md`, with your own address:

```markdown
**[Try the live demo](https://quizzmosis.onrender.com)**: log in as `alice` / `password123`.
The free server sleeps when nobody uses it, so the first page can take a minute to load.
```

## 5. Reset the demo every night (recommended)

The workflow in [`.github/workflows/reset-demo.yml`](../.github/workflows/reset-demo.yml)
reloads `db/schema.sql` and `db/seed.sql` into the demo database every night at 03:17 UTC. So
the demo always looks the way the README describes, and because the demo data's dates are
relative to the moment it's loaded, the "today" leaderboard never runs empty. Anything visitors
created is gone the next morning, which is fine for a demo.

The workflow does nothing until it knows the database. On GitHub, open the repository's
**Settings → Secrets and variables → Actions** and add:

| Name | Where | Value |
|---|---|---|
| `DEMO_DB_HOST` | **Variables** tab | the host from Aiven |
| `DEMO_DB_PORT` | **Variables** tab | the port from Aiven |
| `DEMO_DB_USER` | **Variables** tab | `avnadmin` |
| `DEMO_DB_NAME` | **Variables** tab | `defaultdb` |
| `DEMO_DB_PASSWORD` | **Secrets** tab | the Aiven password |

To try it right away, open the **Actions** tab, choose **Reset demo** and click
**Run workflow**. Its log ends with the number of demo users, 7.

GitHub pauses scheduled workflows in a public repository after 60 days without any activity in
it. If that happens, GitHub sends you an email, and one click on the Actions tab turns the
workflow back on.

## If something goes wrong

- **Render's log says `Quizzmosis started but cannot reach the database`**, and the site shows
  an error page. Check `DB_URL`, `DB_USER` and `DB_PASSWORD` under the service's
  **Environment** settings on Render (host, port, `defaultdb` and `?sslMode=REQUIRED` all
  matter), and check that the Aiven service is running. Saving changed settings redeploys the
  site.
- **The log mentions `Table 'defaultdb.users' doesn't exist`.** Step 2 hasn't been run against
  this database yet.
- **The first page takes a long time.** The free server was asleep and is starting up; the
  following pages are quicker.

Any other host that runs a `Dockerfile` works the same way: the container listens on port 8080
and needs the three `DB_` settings.
