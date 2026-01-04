package com.avacuoss.TickScheduler.Ticks;

import com.avacuoss.TickScheduler.Types.Types;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BooleanSupplier;

public class ConditionScheduler {

    //entry point
    public static Builder when(BooleanSupplier condition) {
        return new Builder(condition);
    }

    public static class Builder {
        private final BooleanSupplier condition;

        private long checkEvery = 1;
        private long startDelay = 0;
        private long timeoutTicks = -1;

        private Types.TaskFn onSuccess;
        private Types.TaskFn onTimeout;

        private Types.TaskGroup cancelGroupOnFinish;

        //optional context
        private ServerLevel level;
        private ServerPlayer player;
        private BlockPos pos;

        Builder(BooleanSupplier condition) {
            this.condition = condition;
        }

        //check interval
        public Builder checkEvery(long ticks) {
            this.checkEvery = Math.max(1, ticks);
            return this;
        }

        //delay before first check
        public Builder startDelay(long ticks) {
            this.startDelay = Math.max(0, ticks);
            return this;
        }

        //timeout limit
        public Builder timeout(long ticks) {
            this.timeoutTicks = Math.max(1, ticks);
            return this;
        }

        //callback on success
        public Builder onSuccess(Types.TaskFn fn) {
            this.onSuccess = fn;
            return this;
        }

        //callback on timeout
        public Builder onTimeout(Types.TaskFn fn) {
            this.onTimeout = fn;
            return this;
        }

        //cancel group when finished
        public Builder cancelGroupOnFinish(Types.TaskGroup g) {
            this.cancelGroupOnFinish = g;
            return this;
        }

        //context
        public Builder level(ServerLevel lvl) { this.level = lvl; return this; }
        public Builder player(ServerPlayer p) { this.player = p; return this; }
        public Builder pos(BlockPos p) { this.pos = p; return this; }

        //start watcher
        public void start() {
            long now = TickScheduler.getTickStatic();
            long firstCheck = now + startDelay;
            long timeoutTick = (timeoutTicks > 0) ? (now + timeoutTicks) : -1;

            String groupId = cancelGroupOnFinish == null ? null : cancelGroupOnFinish.id;

            TickScheduler.ConditionWatcher w = new TickScheduler.ConditionWatcher(
                    condition,
                    checkEvery,
                    firstCheck,
                    timeoutTick,
                    onSuccess,
                    onTimeout,
                    groupId,
                    level,
                    player,
                    pos
            );

            TickScheduler.get().addWatcher(w);
        }
    }
}
