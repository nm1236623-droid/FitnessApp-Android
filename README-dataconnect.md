# Firebase Data Connect (Postgres) migration – ActivityLogRepository (A)

This repository currently stores activity logs in a local JSON file (`activity_logs.json`).
The goal is to migrate ActivityLogRepository to Firebase Data Connect backed by Postgres.

## Status

- **Scaffolding only**: Data Connect requires Firebase CLI + a configured Data Connect instance (connector) in your Firebase project.
- This repo does not yet contain Data Connect config (`firebase.json`, `dataconnect.yaml`, schema, operations) because they must match your Firebase project setup.

## What you need to do in Firebase Console

1. Enable **Firebase Data Connect** for your project.
2. Choose **Cloud SQL for PostgreSQL**.
3. Create a connector (you will be asked for a connector name).

## Local setup (Windows PowerShell)

1. Install Firebase CLI

```powershell
npm i -g firebase-tools
firebase --version
```

2. Login + select project

```powershell
firebase login
firebase use --add
```

3. Initialize Data Connect

```powershell
firebase init dataconnect
```

Choose:
- Product: **Data Connect**
- Database: **PostgreSQL**

This generates configuration files under `dataconnect/` plus updates `firebase.json`.

## Next

After the config exists in-repo, we can:
- add Gradle dependencies/plugin required by the generated Kotlin client
- implement `DataConnectActivityLogDataSource`
- refactor `ActivityLogRepository` to use it **without changing its public API**.

