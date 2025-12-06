TickScheduler API  

TickScheduler is a small and reliable utility library for Forge mods that need to:

- run code in the future  
- repeat actions every N ticks  
- execute logic when conditions become true  
- persist tasks across world reloads  
- offload heavy work to async threads 

Features:
• Run code after a delay
Execute any task after a specified number of ticks:


• Repeating tasks
Run code every N ticks, with an optional maximum number of executions:

• Task priorities
When multiple tasks trigger on the same tick, they run in priority order:

◽HIGH

◽NORMAL

◽LOW
SchedulerAPI.builder()
    .delay(40)
    .priority(Types.Priority.HIGH)
    .submit(ctx -> System.out.println("High priority task"));
•Cancel tasks or whole task groups

Useful for cooldowns or stopping chain reactions

Types.Handle h = SchedulerAPI.after(200, ctx -> {...});
h.cancel(); // cancels the task

•
