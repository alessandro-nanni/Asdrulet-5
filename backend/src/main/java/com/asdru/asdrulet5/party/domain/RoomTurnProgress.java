package com.asdru.asdrulet5.party.domain;

import java.util.*;

/**
 * Turn-ordered "each member acts once, then everyone acknowledges" bookkeeping
 * shared by the MYSTERY wheel and LOOT room state machines: whoever's next in
 * turnOrder among those who haven't acted yet goes next, their own result is
 * recorded exactly once, and the surrounding room only clears once every
 * member has both acted and acknowledged having seen their own result. Reset
 * at the start of every room visit (see {@link #reset()}) so bookkeeping
 * never leaks from one visit into the next.
 *
 * <p>No locking of its own — instances are only ever touched from within
 * Party's own {@code @Synchronized} methods, which is the sole synchronization
 * boundary for party state.
 */
class RoomTurnProgress<T> {

    private final Set<String> actedMemberIds = new LinkedHashSet<>();
    private final Map<String, T> results = new LinkedHashMap<>();
    private final Set<String> acknowledgedMemberIds = new LinkedHashSet<>();

    void reset() {
        actedMemberIds.clear();
        results.clear();
        acknowledgedMemberIds.clear();
    }

    boolean hasActed(String userId) {
        return actedMemberIds.contains(userId);
    }

    void recordResult(String userId, T result) {
        actedMemberIds.add(userId);
        results.put(userId, result);
    }

    Map<String, T> results() {
        return Map.copyOf(results);
    }

    boolean allMembersHaveActed(Set<String> memberIds) {
        return actedMemberIds.containsAll(memberIds);
    }

    /**
     * Whether userId is next in line to act, per the given turnOrder — the
     * first member in that sequence who hasn't acted yet goes next, everyone
     * else has to wait.
     */
    boolean isMembersTurn(String userId, List<String> turnOrder) {
        for (String candidate : turnOrder) {
            if (!actedMemberIds.contains(candidate)) {
                return candidate.equals(userId);
            }
        }
        return false;
    }

    void recordAcknowledgement(String userId) {
        acknowledgedMemberIds.add(userId);
    }

    boolean allMembersHaveAcknowledged(Set<String> memberIds) {
        return acknowledgedMemberIds.containsAll(memberIds);
    }
}
