# 🧠 The Memory Game Project

This is a full-stack Android application built for the Android CA project, consisting of:

- **TheMemoryGame_Android** – the memory card-matching game for Android.
- **TheMemoryGame_Backend** – a C# .NET 8 REST API backend with leaderboard support and MySQL database integration.


## 📁 Project Structure
```
📂 TheMemoryGame\_Android    → Android game frontend (Kotlin)
📂 TheMemoryGame\_Backend    → ASP.NET Core Web API backend
📄 DummyData.sql            → Sample MySQL data
📄 .gitignore
📄 README.md
```

## ⚙️ Prerequisites

### Android App:
- Android Studio
- Minimum SDK: 24 (Android 7.0)
- Internet connection (for image & advertisment calls)

### Backend:
- [.NET 8 SDK](https://dotnet.microsoft.com/en-us/download)
- [MySQL 8+](https://dev.mysql.com/downloads/)
- Visual Studio 2022+ or VS Code (optional)
- Postman or any API testing tool (optional for testing endpoints)

## 🛠️ Setup Instructions

### 1. 🧩 Set up the Backend

1. Open the `TheMemoryGame_Backend` folder.
2. Open `appsettings.json` and **edit the database connection string** under `ConnectionStrings`:

   ```json
   {
     "Logging": {
       "LogLevel": {
         "Default": "Information",
         "Microsoft.AspNetCore": "Warning"
       }
     },
     "AllowedHosts": "*",
     "ConnectionStrings": {
       "DefaultConnection": "server=localhost;database=TheMemoryGame;user=root;password=root;"
     }
   }

🔧 Replace `server`, `user`, and `password` with your actual MySQL database configuration.

3. Import `DummyData.sql` into your MySQL database:

   ```bash
   mysql -u root -p < DummyData.sql
   ```

4. Run the backend API:

   ```bash
   cd TheMemoryGame_Backend
   dotnet run
   ```

   The API should be available at `http://localhost:5178`.


### 2. 📱 Set up the Android App

1. Open `TheMemoryGame_Android` in Android Studio.

2. Make sure the device or emulator has internet access.

3. In `ApiConstants.kt`, the base URL is hardcoded as:

   ```kotlin
   const val BASE_URL = "http://10.0.2.2:5178"
   ```

   🔧 **What is `10.0.2.2`?**
   This IP points to your host machine when using the Android emulator. It works **only on the emulator**.

   ⚠️ If you are testing on a **physical Android device**, replace it with your **computer's local IP address** (e.g., `http://192.168.x.x:5178`), and make sure both the device and your machine are on the same Wi-Fi network.

4. Build and run the app.

## 🎮 Game Features

* 🃏 Match card pairs by flipping them
* ⏱️ Countdown timer and match counter
* 🔐 Login system for user sessions
* 🖼️ Fetches card images from [StockSnap.io](https://stocksnap.io)
* 🏆 Leaderboard with top 5 scores and personal rank
* 🔊 Audio feedback and animations
* 📢 Ad display based on user type (free or paid)



## 🧪 Sample Test Credentials


```
Username: aung
Password: aung123
```



## 📝 Notes

* Ensure both the **backend API** and **MySQL** are running before launching the app.
* `DummyData.sql` contains sample leaderboard records you can replace or expand.


## 👨‍🏫 Checklist Before Running

✅ Please ensure you:

* Have MySQL installed and the connection string updated in `appsettings.json`
* Import `DummyData.sql` to create and populate the database
* Start the backend before testing the Android app
* Use `dotnet run` in the `TheMemoryGame_Backend` folder to launch the API

That's all. Thank you for checking our project!


---

© 2025 The Memory Game CA Project