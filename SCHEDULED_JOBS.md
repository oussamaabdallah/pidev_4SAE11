# Scheduled jobs (Planning & Task)

This document describes the **automatic background tasks** that run inside the **Planning** and **Task** microservices. They use Spring’s `@Scheduled` feature and the **cron** expressions configured in each service’s `application.properties` (and optionally the Config Server).

**Important:** Times follow the **JVM’s default timezone** (usually the server’s system timezone), unless you configure Spring to use a specific zone.

Cron format used here (6 fields):  
`second minute hour day-of-month month day-of-week`

---

## Planning microservice (`planningdb`)

| What it does (plain English) | When it runs (default) | What changes in the database | Notifications |
|------------------------------|------------------------|------------------------------|---------------|
| **Overdue “next progress update” reminder** | **Every day at 08:00:00** (`0 0 8 * * ?`) | For each progress row that has a **next update due date in the past** and has **not** been flagged yet: sets `nextDueOverdueNotified` to **true** (so we don’t spam the same overdue row every day). | **Yes** — sends a notification to the **freelancer** (type `PROGRESS_NEXT_DUE_OVERDUE`). The app’s notification list and header badge pick this up like any other notification. |
| **Orphan Google Calendar ID cleanup** | **Every Sunday at 03:00:00** (`0 0 3 ? * SUN`) | Clears `nextDueCalendarEventId` when **`nextUpdateDue` is empty** but an old calendar event id was still stored (housekeeping after the due date was removed). | **No** |

**Config properties (Planning):**

- `planning.scheduler.overdue-cron` — overdue reminder job  
- `planning.scheduler.cleanup-cron` — orphan calendar id cleanup  

If the freelancer **changes** the “next update due” date on a progress update, the service **resets** `nextDueOverdueNotified` to **false**, so a **new** overdue cycle can notify once again later.

---

## Task microservice (`taskdb`)

| What it does (plain English) | When it runs (default) | What changes in the database | Notifications |
|------------------------------|------------------------|------------------------------|---------------|
| **Escalate overdue task priority** | **Every day at 09:00:00** (`0 0 9 * * ?`) | Finds tasks that are **overdue** (due date before today, not **DONE** or **CANCELLED**) and still **LOW** or **MEDIUM** priority → sets priority to **HIGH**. | **Yes** — if the task has an **assignee**, sends them a notification (type `TASK_PRIORITY_ESCALATED`). |
| **Purge old cancelled tasks** | **Every Monday at 04:00:00** (`0 0 4 ? * MON`) | **Deletes** tasks whose status is **CANCELLED** and whose `updatedAt` is **older than** the configured number of days (default **90**). Comments and subtasks are removed using the same rules as a normal manual delete. | **No** |

**Config properties (Task):**

- `task.scheduler.escalate-cron` — priority escalation job  
- `task.scheduler.purge-cron` — purge job  
- `task.scheduler.purge-cancelled-days` — how many days a **CANCELLED** task must sit before purge (default `90`)

---

## Frontend (how users see the result)

- **Notifications** from Planning and Task schedulers are stored by the **Notification** microservice like other events. The Angular **header** refreshes the unread count on a short interval, so new items can appear without reloading the whole app.
- **Notification types** `PROGRESS_NEXT_DUE_OVERDUE` and `TASK_PRIORITY_ESCALATED` have routes in `notification.service.ts` (e.g. progress pages / my tasks) and readable labels on the notifications page.
- **Priority changes** and **deleted tasks** show up when the user opens or refreshes task boards and lists (normal API data).

---

## Changing the schedule

1. Edit the properties above in  
   `backEnd/Microservices/planning/src/main/resources/application.properties`  
   and/or  
   `backEnd/Microservices/task/src/main/resources/application.properties`  
2. If you use the **Config Server**, mirror the same keys in  
   `backEnd/ConfigServer/src/main/resources/config/planning.properties`  
   and  
   `backEnd/ConfigServer/src/main/resources/config/task.properties`  
3. **Restart** the microservice (or refresh config if you rely on `/actuator/refresh` and compatible beans).

Use a cron cheat sheet or an online cron validator if you change expressions; Spring expects the **6-field** format with seconds.
