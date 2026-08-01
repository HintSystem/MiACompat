package dev.hintsystem.miacompat.server.config.geary.item;

import dev.hintsystem.miacompat.server.MiniMessageParser;
import dev.hintsystem.miacompat.server.config.StringParsers;
import dev.hintsystem.miacompat.server.config.geary.ItemYamlSchema;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public class ActionCooldown {
    private static final int COOLDOWN_SYNC_TOLERANCE_MS = 200;
    private static final int MAX_COOLDOWN_TIME_DIFF_MS = 700;

    public final long durationMs;
    @Nullable public final String cooldownDisplay;
    @Nullable public final String failMessage;

    private long triggeredTime = Long.MIN_VALUE;
    private long endTime = Long.MIN_VALUE;

    private ActionCooldown(long durationMs, @Nullable String cooldownDisplay, @Nullable String failMessage) {
        this.durationMs = durationMs;
        this.cooldownDisplay = cooldownDisplay;
        this.failMessage = failMessage;
    }

    /**
     * Measured in milliseconds
     */
    public long getTriggeredTime() {
        return triggeredTime;
    }

    /**
     * Measured in milliseconds
     */
    public long getTriggeredEndTime() {
        return getTriggeredTime() + durationMs;
    }

    /**
     * Measured in milliseconds
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Measured in milliseconds
     */
    public long getRemainingTime() {
        return Math.max(0, getEndTime() - System.currentTimeMillis());
    }

    public float getPercent() {
        if (!isActive()) return 0f;

        return Math.clamp(
            getRemainingTime() / (float) durationMs,
            0f, 1f
        );
    }

    public boolean isActive() {
        return System.currentTimeMillis() < endTime;
    }


    /**
     * Equivalent to {@link #trigger(int) trigger(0)}.
     *
     * @see #trigger(int)
     */
    public void trigger() { trigger(0); }

    /**
     * Called when the player triggers a cooldown via input
     * @param offsetMs applies this offset to triggered time, can be used to adjust for latency
     */
    public void trigger(int offsetMs) {
        triggeredTime = System.currentTimeMillis() + offsetMs;
    }

    /**
     * Starts the cooldown using the previous trigger time
     *
     * @return {@code true} if the cooldown started
     */
    public boolean begin() {
        long expectedEnd = triggeredTime + durationMs;

        // begin() has already been called for the current trigger
        if (Math.abs(endTime - expectedEnd) < MAX_COOLDOWN_TIME_DIFF_MS)
            return false;

        this.endTime = expectedEnd;
        return true;
    }

    /**
     * Synchronizes the cooldown with the authoritative end time received from the server.
     *
     * @param observedEndTime the cooldown end time reported by the server
     * @return {@code true} if the cooldown started
     */
    public boolean beginFromServer(long observedEndTime) {
        if (Math.abs(endTime - observedEndTime) < COOLDOWN_SYNC_TOLERANCE_MS)
            return false;

        this.endTime = observedEndTime;
        return true;
    }

    public void cancel() {
        endTime = System.currentTimeMillis();
    }

    public static List<ActionCooldown> filterCooldownCandidates(Iterable<ActionCooldown> cooldowns, long observedRemainingMs) {
        List<ActionCooldown> candidates = new ArrayList<>();

        // exclude cooldowns that are too short, remaining time can't be higher than total duration
        for (ActionCooldown cooldown : cooldowns) {
            if (cooldown.durationMs + COOLDOWN_SYNC_TOLERANCE_MS >= observedRemainingMs) {
                candidates.add(cooldown);
            }
        }

        return candidates;
    }

    @Nullable
    public static ActionCooldown findTriggeredCooldown(
        List<ActionCooldown> cooldownCandidates,
        long observedEndTime
    ) {
        if (cooldownCandidates.isEmpty()) return null;
        if (cooldownCandidates.size() == 1) return cooldownCandidates.getFirst();

        long closestDiff = Long.MAX_VALUE;
        ActionCooldown closest = null;

        // find cooldown whose triggered end time is closest to the observed end time
        for (ActionCooldown cooldown : cooldownCandidates) {
            long diff = Math.abs(cooldown.getTriggeredEndTime() - observedEndTime);

            if (diff < closestDiff) {
                closestDiff = diff;
                closest = cooldown;
            }
        }

        return closest;
    }

    @Nullable
    public static ActionCooldown fromActionsConfig(@Nullable List<ItemYamlSchema.Action> actions) {
        if (actions == null) return null;

        String length = null;
        String cooldownDisplay = null;
        String failMessage = null;

        for (ItemYamlSchema.Action action : actions) {
            if (action.cooldown != null) {
                length = action.cooldown.length;
                cooldownDisplay = MiniMessageParser.stripTags(action.cooldown.display);
            } else if (action.ensure != null && action.ensure.onFail != null && !action.ensure.onFail.isEmpty()) {
                ItemYamlSchema.FailAction fail = action.ensure.onFail.getFirst();
                if (fail.sendActionBar != null) {
                    failMessage = MiniMessageParser.stripTags(fail.sendActionBar.text);
                }
            }
        }

        if (length == null) return null;

        return new ActionCooldown(
            StringParsers.parseDuration(length).toMillis(),
            cooldownDisplay, failMessage
        );
    }
}
