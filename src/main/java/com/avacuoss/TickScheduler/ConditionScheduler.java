package com.avacuoss.TickScheduler;

import java.util.UUID;
import java.util.function.BooleanSupplier;

public class ConditionScheduler {

    public static Builder when(BooleanSupplier condition) {
        return new Builder(condition);
    }

    public static class Builder {

        private final BooleanSupplier condition;
        private long checkInterval = 20L;
        private long timeoutTicks = -1L;
        private Types.TaskFn onSuccess;
        private Types.TaskFn onTimeout;

        public Builder(BooleanSupplier condition) {
            this.condition = condition;
        }

        public Builder checkEvery(long ticks) {
            this.checkInterval = Math.max(1L, ticks);
            return this;
        }

        public Builder timeout(long ticks) {
            this.timeoutTicks = ticks;
            return this;
        }

        public Builder onSuccess(Types.TaskFn fn) {
            this.onSuccess = fn;
            return this;
        }

        public Builder onTimeout(Types.TaskFn fn) {
            this.onTimeout = fn;
            return this;
        }

        public void start() {
            final long startTick = TickScheduler.getTickStatic();
            final Types.TaskGroup group = new Types.TaskGroup("cond-" + UUID.randomUUID());

            SchedulerAPI.builder()
                    .delay(checkInterval)
                    .repeat(checkInterval)
                    .group(group)
                    .submit(ctx -> {
                        long now = TickScheduler.getTickStatic();

                        if (condition.getAsBoolean()) {
                            if (onSuccess != null) onSuccess.run(ctx);
                            group.cancelAll();
                            return;
                        }

                        if (timeoutTicks > 0 && now - startTick >= timeoutTicks) {
                            if (onTimeout != null) onTimeout.run(ctx);
                            group.cancelAll();
                        }
                    });
        }
    }
}
