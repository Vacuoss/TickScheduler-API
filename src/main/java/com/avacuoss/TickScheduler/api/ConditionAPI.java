package com.avacuoss.TickScheduler.api;

import com.avacuoss.TickScheduler.Ticks.TickScheduler;
import com.avacuoss.TickScheduler.Ticks.ConditionScheduler;
import com.avacuoss.TickScheduler.Types.Types;
import java.util.function.BooleanSupplier;

//high level wrapper
public class ConditionAPI {

    public static Wait waitUntil(BooleanSupplier condition) {
        return new Wait(condition);
    }

    public static class Wait {
        private final BooleanSupplier condition;
        private long checkEvery = 5;
        private long timeout = -1;

        Wait(BooleanSupplier cond) {
            this.condition = cond;
        }

        public Wait checkEvery(long ticks) {
            this.checkEvery = Math.max(1, ticks);
            return this;
        }

        public Wait timeout(long ticks) {
            this.timeout = ticks;
            return this;
        }

        public void then(Runnable r) {
            ConditionScheduler.when(condition)
                    .checkEvery(checkEvery)
                    .timeout(timeout)
                    .onSuccess(ctx -> r.run())
                    .start();
        }
    }
}
