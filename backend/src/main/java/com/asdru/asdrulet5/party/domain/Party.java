package com.asdru.asdrulet5.party.domain;

import com.asdru.asdrulet5.party.exception.*;
import lombok.Getter;
import lombok.Synchronized;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Party {

    /**
     * Fixed lobby size cap, independent of how many CharacterClass values
     * exist — the two happen to match today but must not be coupled.
     */
    public static final int MAX_MEMBERS = 4;

    @Getter
    @Accessors(fluent = true)
    private final String code;

    @Getter
    @Accessors(fluent = true)
    private final String leaderId;

    private final Map<String, PartyMember> members = new LinkedHashMap<>();
    private final AtomicInteger fakeMemberSequence = new AtomicInteger();
    private List<String> turnOrder = List.of();

    @Getter
    @Accessors(fluent = true)
    private PartyStatus status = PartyStatus.LOBBY;

    public Party(String code, String leaderId, String leaderDisplayName, String leaderAvatarUrl) {
        this.code = code;
        this.leaderId = leaderId;
        members.put(leaderId, new PartyMember(leaderId, leaderDisplayName, leaderAvatarUrl, null, true, false));
    }

    @Synchronized
    public PartyMember addMember(String userId, String displayName, String avatarUrl) {
        PartyMember existing = members.get(userId);
        if (existing != null) {
            return existing;
        }
        requireRoom();
        PartyMember member = new PartyMember(userId, displayName, avatarUrl, null, userId.equals(leaderId), false);
        members.put(userId, member);
        return member;
    }

    /**
     * Adds a locally-simulated party member with no real identity behind it,
     * used only by the dev-only "quick game" tooling to test multi-member
     * flows without needing separate logged-in devices.
     */
    @Synchronized
    public PartyMember addFakeMember(String displayName) {
        requireRoom();
        String fakeId = "bot-" + code + "-" + fakeMemberSequence.incrementAndGet();
        PartyMember member = new PartyMember(fakeId, displayName, null, null, false, true);
        members.put(fakeId, member);
        return member;
    }

    private void requireRoom() {
        if (members.size() >= MAX_MEMBERS) {
            throw new PartyFullException(code, MAX_MEMBERS);
        }
    }

    @Synchronized
    public void selectClass(String userId, CharacterClass characterClass) {
        PartyMember member = members.get(userId);
        if (member == null) {
            throw new NotPartyMemberException(code, userId);
        }
        boolean takenByAnotherMember = members.values().stream()
                .anyMatch(other -> !other.userId().equals(userId) && other.characterClass() == characterClass);
        if (characterClass != null && takenByAnotherMember) {
            throw new ClassAlreadyTakenException(code, characterClass);
        }
        members.put(userId, member.withCharacterClass(characterClass));
    }

    @Synchronized
    public void start(String requesterId, List<String> order) {
        if (!leaderId.equals(requesterId)) {
            throw new NotPartyLeaderException(code, requesterId);
        }
        boolean sameMembers = order.size() == members.size() && Set.copyOf(order).equals(members.keySet());
        if (!sameMembers) {
            throw new InvalidTurnOrderException(code);
        }
        boolean everyoneHasAClass = members.values().stream().allMatch(member -> member.characterClass() != null);
        if (!everyoneHasAClass) {
            throw new MissingClassSelectionException(code);
        }
        this.turnOrder = List.copyOf(order);
        this.status = PartyStatus.IN_PROGRESS;
    }

    @Synchronized
    public List<PartyMember> members() {
        return List.copyOf(members.values());
    }

    @Synchronized
    public List<String> turnOrder() {
        return turnOrder;
    }
}
