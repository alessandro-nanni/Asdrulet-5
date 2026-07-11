package com.asdru.asdrulet5.party.domain;

import com.asdru.asdrulet5.party.exception.ClassAlreadyTakenException;
import com.asdru.asdrulet5.party.exception.InvalidTurnOrderException;
import com.asdru.asdrulet5.party.exception.NotPartyLeaderException;
import com.asdru.asdrulet5.party.exception.NotPartyMemberException;
import lombok.Getter;
import lombok.Synchronized;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Party {

    @Getter
    @Accessors(fluent = true)
    private final String code;

    @Getter
    @Accessors(fluent = true)
    private final String leaderId;

    private final Map<String, PartyMember> members = new LinkedHashMap<>();
    private List<String> turnOrder = List.of();

    public Party(String code, String leaderId, String leaderDisplayName, String leaderAvatarUrl) {
        this.code = code;
        this.leaderId = leaderId;
        members.put(leaderId, new PartyMember(leaderId, leaderDisplayName, leaderAvatarUrl, null, true));
    }

    @Synchronized
    public PartyMember addMember(String userId, String displayName, String avatarUrl) {
        return members.computeIfAbsent(userId,
                id -> new PartyMember(id, displayName, avatarUrl, null, id.equals(leaderId)));
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
    public void setTurnOrder(String requesterId, List<String> order) {
        if (!leaderId.equals(requesterId)) {
            throw new NotPartyLeaderException(code, requesterId);
        }
        boolean sameMembers = order.size() == members.size() && Set.copyOf(order).equals(members.keySet());
        if (!sameMembers) {
            throw new InvalidTurnOrderException(code);
        }
        this.turnOrder = List.copyOf(order);
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
