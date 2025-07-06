# 🍉 The Memory Game — Setup & Task Guide

Hi Team, here’s everything you need to test run the project, understand your assigned tasks, and follow proper GitHub workflow.

---

## ✅ How to Test Run the Application

1. Go to the `TheMemoryGame_Backend/Models` folder.

2. Open `MyDbContext.cs` and find this line:

   ```csharp
   "server=localhost;user=root;password=password;database=TheMemoryGame;"
   ```

3. Change the `password` to match the password you've set in your MySQL Workbench.

4. Connect to your MySQL Workbench to ensure your MySQL server is running.

5. Run the backend application. This will automatically create the `TheMemoryGame` database in your MySQL Workbench.

6. Populate the database tables by running the SQL commands inside `DummyData.sql`.

7. Open the Android Studio frontend project and run the app. You should now be able to test the application end-to-end.

---

## 🚀 Tasks To Be Completed

### 1. Overall UI Design — **Paul**

* Design all screens and activities throughout the app.

### 2. Login Activity — **Simba**

* Create a login activity in Android Studio.
* Use hashed passwords to store in the database (if possible).
* After login, store the user object or at least the `UserId` in session for tracking logged-in users.

### 3. Sound Effects and Animations — **Jingjia**

* Add sound effects and animations for the following:

  * Fetching and displaying images.
  * When images are matched.
  * When images are not matched.
  * When the game is won and the app redirects to the leaderboard.

### 4. Ads Display — **Zhangrui**

* Display ads inside the Play Activity **only** if the logged-in user’s `UserType` in the database is `"free"`.
* No ads should appear for `"paid"` users.

### 5. Leaderboard Activity — **Haziq**

* Complete the existing leaderboard functionality.
* Extract the **Top 5** fastest completion times and display them in the leaderboard UI.
* Extract and display the leaderboard rank of the current completion time after a game is completed.
* Change the current hardcoded method of saving completion times — collaborate with Simba to:

* Ensure the logged-in user’s `UserId` is used to store completion time in the `Record` table.

**Note:**  
Use `formatElapsedTime()` from the `TimeUtils` class to convert raw milliseconds into the readable time format used in the app.

---

## 🛠 GitHub Workflow (Best Practices)

Here’s the proper way to collaborate using Git:

### 1. Clone the Repository

```bash
git clone repository_url
```

### 2. Set Upstream (Only needed if not automatically configured)

```bash
git remote add upstream repository_url
```

### 3. Create a New Feature Branch

```bash
git checkout -b branch_name
```

*Example:* `animation_and_audio`

### 4. Develop Your Functions and Features

### 5. Commit Changes Frequently

```bash
git add .
git commit -m "Short, clear message about what you changed"
```

### 6. Check Repository Status

```bash
git status
```

### 7. Stay Up-to-Date with the Remote Repository

If your repo is behind:

```bash
git fetch origin
git rebase origin/main
```

*This places your local commits on top of the latest version from `main`.*

### 8. Push Your Feature Branch to Remote

```bash
git push origin your_feature_branch_name
```

### 9. Create a Pull Request (PR)

* Go to GitHub, create a PR from your feature branch to `main`.
* A teammate will review your PR.

### 10. Resolve Comments if Requested

* If changes are requested, update your branch, push again, and resolve the review comments.

### 11. Use Small, Incremental Updates

* Avoid large, bundled commits. Smaller updates make reviews faster and reduce merge conflicts.

---

## 💡 Final Notes

* Keep your `.gitignore` clean to avoid committing unnecessary build files.
* Communicate regularly on progress.
* Test your features thoroughly before pushing.

Good luck, team! Let’s make this project great. 🎉
