## TickScheduler API  
## Works on Forge 1.20.1
## TickScheduler is an utility library for mods that need to:

- run code in the future  
- repeat actions every N ticks  
- execute logic when conditions become true  
- persist tasks across world reloads  
- offload heavy work to async threads 

## Features:

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

• Task priorities

When multiple tasks trigger on the same tick, they run in priority order:

◽HIGH

◽NORMAL

◽LOW
```java
SchedulerAPI.builder()
    .delay(40)
    .priority(Types.Priority.HIGH)
    .submit(ctx -> System.out.println("High priority task"));
```
•Cancel tasks or whole task groups

Useful for cooldowns or stopping chain reactions
```java
Types.Handle h = SchedulerAPI.after(200, ctx -> {...});
h.cancel(); // cancels the task
```
```java
var group = SchedulerAPI.group("cooldowns");
SchedulerAPI.builder().group(group).submit(...);

// Cancel all tasks in the group:
group.cancelAll();
```
•Async task execution
```java
SchedulerAPI.runAsync(() -> {
    // heavy calculations
});
```
•Condition-based scheduler:

Run a task only when a condition becomes true, with optional timeout and repeated checks:
```java
SchedulerAPI.runAsync(() -> {
    // heavy calculations
});
```
•Built-in profiler

Measure execution cost of scheduled tasks:
```java|
SchedulerAPI.enableProfiler(true);
var stats = SchedulerAPI.getProfilerStats();
System.out.println("Avg: " + stats.getAvgMillisPerTask() + " ms");
```
Profiler tracks:

total executed tasks

average time

max time

tick range
•/commands:
- /scheduler debug
- /scheduler profiler on
- /scheduler profiler off
- /scheduler profiler stats
- /scheduler save
- /scheduler load
- /scheduler tasks
- /scheduler test

## TickScheduler is ideal for:
- temporary effects
- cooldown timers
- delayed events
- AI / structure updates
- repeating logic loops
- async computations
- persistent timers
- condition-based triggers
- modular systems that need precise timing
  
## • API classes:

```java
SchedulerAPI - High-level scheduling interface
TickScheduler - Internal engine
Types - Task types, context, builder, priorities
PersistentStorage - Save & load persistent tasks ConditionScheduler
ConditionScheduler - Condition-based logic
TaskRegistry - Restore persistent task types
```
•Example: Building a custom scheduler task:
```java
SchedulerAPI.builder()
    .delay(60)
    .repeat(10)
    .maxRuns(5)
    .priority(Types.Priority.HIGH)
    .persistent()
    .type("regenerate")
    .submit(ctx -> {
        System.out.println("Regenerating at " + ctx.tick);
    });
```



