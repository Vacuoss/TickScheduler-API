## TickScheduler API  
## Works on Forge 1.20.1
## TickScheduler is a lightweight scheduling library for Minecraft mods that need to run logic over time, repeat actions, wait for conditions, or persist timers across world reloads

## Features:
Run code after a delay

Repeat code every N ticks

Run logic when a condition becomes true

Run tasks in priority order

Group and cancel tasks

Run async work safely

Persist tasks in world data

Attach NBT data to tasks

Profile execution cost

## Basic usage
• Run code after a delay

Execute any task after a specified number of ticks:
```java
SchedulerAPI.after(40, ctx -> {
    System.out.println("Executed at tick " + ctx.tick);
});
```

• Repeating tasks

Run code every N ticks, with an optional maximum number of executions:
```java
SchedulerAPI.repeat(20, 20, 5, ctx -> {
    System.out.println("Repeating at tick: " + ctx.tick);
});
```
• Priority
```java
SchedulerAPI.builder()
    .delay(40)
    .priority(Types.Priority.HIGH)
    .submit(ctx -> System.out.println("High priority task"));
```
## Canceling tasks
• Cancel a single task
```java
Types.Handle h = SchedulerAPI.after(200, ctx -> { });
h.cancel();
```
• Cancel a group
```java
var group = SchedulerAPI.group("cooldowns");
SchedulerAPI.builder().group(group).submit(...);
// cancel everything in the group
group.cancelAll();
```
• Async execution
```java
SchedulerAPI.runAsync(() -> {
});
```
• Condition-based scheduling
Run code only when a condition becomes true:
```java
SchedulerAPI.when(() -> someValue > 10)
    .checkEvery(5)
    .onSuccess(ctx -> System.out.println("Condition met"))
    .timeout(200)
    .onTimeout(ctx -> System.out.println("Timeout"))
    .start();
```
• Persistent tasks
Tasks can be saved in world data and restored on load
```java
CompoundTag data = new CompoundTag();
data.putUUID("player", player.getUUID());

SchedulerAPI.builder()
    .delay(200)
    .persistent()
    .type("cooldown")
    .data(data)
    .submit(ctx -> {
        var p = ctx.server.getPlayerList().getPlayer(ctx.data().getUUID("player"));
        if (p != null) p.sendSystemMessage(Component.literal("Cooldown finished"));
    });
```
## Commands:
```java
CompoundTag data = new CompoundTag();
data.putUUID("player", player.getUUID());

SchedulerAPI.builder()
    .delay(200)
    .persistent()
    .type("cooldown")
    .data(data)
    .submit(ctx -> {
        var p = ctx.server.getPlayerList().getPlayer(ctx.data().getUUID("player"));
        if (p != null) p.sendSystemMessage(Component.literal("Cooldown finished"));
    });
```
## API overview
```java
SchedulerAPI     - Main user API
TickScheduler    - Internal engine
Types            - Task, Context, Builder, Priority
ConditionScheduler - Condition-based tasks
PersistentStorage - JSON snapshots
TaskRegistry     - Persistent task handlers
```
• Example:
```java
SchedulerAPI.builder()
    .delay(60)
    .repeat(10)
    .maxRuns(5)
    .priority(Types.Priority.HIGH)
    .persistent()
    .type("regenerate")
    .submit(ctx -> {
        System.out.println("Regenerating at tick " + ctx.tick);
    });
```



