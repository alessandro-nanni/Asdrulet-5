package com.asdru.asdrulet5.party.domain;

import com.asdru.asdrulet5.classdata.domain.ActiveEffect;
import com.asdru.asdrulet5.inventory.domain.ItemSlot;
import com.asdru.asdrulet5.inventory.domain.Loadout;
import com.asdru.asdrulet5.party.exception.*;
import lombok.Getter;
import lombok.Synchronized;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class Party {

    /**
     * Fixed lobby size cap, independent of how many CharacterClass values
     * exist — the two happen to match today but must not be coupled.
     */
    public static final int MAX_MEMBERS = 4;

    /** Shared party storage grid: 3 columns x 4 rows, rendered by the frontend. */
    public static final int STORAGE_SIZE = 12;

    @Getter
    @Accessors(fluent = true)
    private final String code;

    @Getter
    @Accessors(fluent = true)
    private final String leaderId;

    private final Map<String, PartyMember> members = new LinkedHashMap<>();
    private final AtomicInteger fakeMemberSequence = new AtomicInteger();
    private final List<String> storage = new ArrayList<>(Collections.nCopies(STORAGE_SIZE, null));
    private List<String> turnOrder = List.of();

    /**
     * Bookkeeping for the MYSTERY room currently entered (if any): who has
     * spun already, what they landed on (drawn from one shared pool of
     * WheelEffects — see {@link #claimedWheelEffects} — so nobody can land
     * on an effect someone else already claimed), and who has acknowledged
     * having seen their own result (see {@link #recordWheelAcknowledgement}).
     * Reset every time a new room is entered — see
     * {@link #resetWheelSpins()} — so it's always scoped to whichever room
     * is currently entered, never a stale earlier visit.
     */
    private final Set<String> wheelSpunMemberIds = new LinkedHashSet<>();
    private final Map<String, WheelEffect> wheelResults = new LinkedHashMap<>();
    private final Set<String> wheelAcknowledgedMemberIds = new LinkedHashSet<>();
    private final Set<WheelEffect> claimedWheelEffects = new LinkedHashSet<>();

    @Getter
    @Accessors(fluent = true)
    private PartyStatus status = PartyStatus.LOBBY;

    public Party(String code, String leaderId, String leaderDisplayName, String leaderAvatarUrl) {
        this.code = code;
        this.leaderId = leaderId;
        members.put(leaderId,
                new PartyMember(leaderId, leaderDisplayName, leaderAvatarUrl, null, true, false, Loadout.empty(), null, List.of()));
    }

    @Synchronized
    public PartyMember addMember(String userId, String displayName, String avatarUrl) {
        PartyMember existing = members.get(userId);
        if (existing != null) {
            return existing;
        }
        requireRoom();
        PartyMember member = new PartyMember(
                userId, displayName, avatarUrl, null, userId.equals(leaderId), false, Loadout.empty(), null, List.of());
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
        PartyMember member = new PartyMember(fakeId, displayName, null, null, false, true, Loadout.empty(), null, List.of());
        members.put(fakeId, member);
        return member;
    }

    private void requireRoom() {
        if (members.size() >= MAX_MEMBERS) {
            throw new PartyFullException(code, MAX_MEMBERS);
        }
    }

    private PartyMember requireMember(String userId) {
        PartyMember member = members.get(userId);
        if (member == null) {
            throw new NotPartyMemberException(code, userId);
        }
        return member;
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
        this.status = PartyStatus.DUNGEON;
    }

    @Synchronized
    public void equipItem(String userId, ItemSlot slot, String itemId) {
        PartyMember member = requireMember(userId);
        members.put(userId, member.withLoadout(member.loadout().withItem(slot, itemId)));
    }

    /**
     * Fills the shared storage grid — called once, right after creation, with
     * whatever the catalog currently holds. Any cells beyond the given list
     * (or beyond STORAGE_SIZE) are simply left empty.
     */
    @Synchronized
    public void seedStorage(List<String> itemIds) {
        for (int i = 0; i < itemIds.size() && i < STORAGE_SIZE; i++) {
            storage.set(i, itemIds.get(i));
        }
    }

    @Synchronized
    public List<String> storage() {
        // Not List.copyOf: cells are legitimately null (empty), and
        // List.copyOf/List.of both reject null elements.
        return Collections.unmodifiableList(new ArrayList<>(storage));
    }

    /**
     * Equips whatever item sits in the given shared-storage cell, swapping
     * whatever the member already had in that item's slot back into the same
     * cell — so the grid never loses or duplicates an item. slotResolver
     * looks up the item's slot (via ItemDefinitionRegistry, kept out of this
     * class the same way PartyService already resolves it for equipItem)
     * inside this synchronized method, so the read of the storage cell and
     * the swap happen atomically.
     */
    @Synchronized
    public void equipFromStorage(String userId, int storageIndex, Function<String, ItemSlot> slotResolver) {
        PartyMember member = requireMember(userId);
        if (storageIndex < 0 || storageIndex >= STORAGE_SIZE) {
            throw new InvalidStorageIndexException(code, storageIndex);
        }
        String itemId = storage.get(storageIndex);
        if (itemId == null) {
            throw new EmptyStorageSlotException(code, storageIndex);
        }
        ItemSlot slot = slotResolver.apply(itemId);
        String previousItemId = member.loadout().itemIdFor(slot);
        storage.set(storageIndex, previousItemId);
        members.put(userId, member.withLoadout(member.loadout().withItem(slot, itemId)));
    }

    @Synchronized
    public void enterCombat(String requesterId) {
        if (!leaderId.equals(requesterId)) {
            throw new NotPartyLeaderException(code, requesterId);
        }
        this.status = PartyStatus.IN_PROGRESS;
    }

    /**
     * Called once combat resolves in the party's favor — system-triggered
     * (off the back of a CombatVictoryEvent), not a direct user action, so
     * unlike enterCombat there's no leader check here.
     */
    @Synchronized
    public void returnToDungeon() {
        this.status = PartyStatus.DUNGEON;
    }

    @Synchronized
    public List<PartyMember> members() {
        return List.copyOf(members.values());
    }

    @Synchronized
    public List<String> turnOrder() {
        return turnOrder;
    }

    /** Called whenever a room is entered, so wheel bookkeeping never leaks from one room visit into the next. */
    @Synchronized
    public void resetWheelSpins() {
        wheelSpunMemberIds.clear();
        wheelResults.clear();
        wheelAcknowledgedMemberIds.clear();
        claimedWheelEffects.clear();
    }

    @Synchronized
    public boolean hasSpunWheel(String userId) {
        return wheelSpunMemberIds.contains(userId);
    }

    @Synchronized
    public void recordWheelSpin(String userId, WheelEffect effect) {
        wheelSpunMemberIds.add(userId);
        wheelResults.put(userId, effect);
        claimedWheelEffects.add(effect);
    }

    @Synchronized
    public boolean allMembersHaveSpunWheel() {
        return wheelSpunMemberIds.containsAll(members.keySet());
    }

    @Synchronized
    public Map<String, WheelEffect> wheelResults() {
        return Map.copyOf(wheelResults);
    }

    /** The WheelEffects nobody in the party has landed on yet this room visit — what a spin draws from. */
    @Synchronized
    public List<WheelEffect> remainingWheelEffects() {
        return Arrays.stream(WheelEffect.values())
                .filter(effect -> !claimedWheelEffects.contains(effect))
                .toList();
    }

    /**
     * Whether userId is next in line to spin, per the same turnOrder set at
     * game start (see {@link #start}) — the first member in that sequence
     * who hasn't spun yet gets the wheel next, everyone else has to wait.
     */
    @Synchronized
    public boolean isMembersWheelTurn(String userId) {
        for (String candidate : turnOrder) {
            if (!wheelSpunMemberIds.contains(candidate)) {
                return candidate.equals(userId);
            }
        }
        return false;
    }

    /**
     * Called once a member's own client has finished locally announcing
     * their spin result — see PartyService.acknowledgeWheelResult for why
     * the room's actual clear waits on this instead of a fixed delay.
     */
    @Synchronized
    public void recordWheelAcknowledgement(String userId) {
        wheelAcknowledgedMemberIds.add(userId);
    }

    @Synchronized
    public boolean allMembersHaveAcknowledgedWheel() {
        return wheelAcknowledgedMemberIds.containsAll(members.keySet());
    }

    @Synchronized
    public void setMemberHealth(String userId, Integer currentHealth) {
        PartyMember member = requireMember(userId);
        members.put(userId, member.withCurrentHealth(currentHealth));
    }

    /**
     * Attaches a pending ActiveEffect (e.g. a MYSTERY wheel's "poison") that
     * CombatService seeds into this member's next fresh Combatant. Reapplying
     * an effect with the same name replaces it in place, mirroring how
     * Combatant.addActiveEffect handles the same case mid-combat.
     */
    @Synchronized
    public void addPendingEffect(String userId, ActiveEffect effect) {
        PartyMember member = requireMember(userId);
        List<ActiveEffect> updated = new ArrayList<>(member.pendingEffects());
        updated.removeIf(existing -> existing.name().equals(effect.name()));
        updated.add(effect);
        members.put(userId, member.withPendingEffects(updated));
    }

    @Synchronized
    public void clearPendingEffects(String userId) {
        PartyMember member = requireMember(userId);
        members.put(userId, member.withPendingEffects(List.of()));
    }

    /** Drops itemId into the first empty storage cell; a no-op if the grid is already full. */
    @Synchronized
    public void addItemToStorage(String itemId) {
        int emptyIndex = storage.indexOf(null);
        if (emptyIndex >= 0) {
            storage.set(emptyIndex, itemId);
        }
    }

    /**
     * Equips itemId directly onto userId's own slot — used by the MYSTERY
     * wheel's GIVE_ITEM result so the reward benefits only the spinner, not
     * the whole party (unlike a normal loot pickup, which goes through the
     * shared storage grid where anyone could take it). Whatever the member
     * already had in that slot, if anything, is swapped back into shared
     * storage rather than lost — same swap-preserving idea as
     * equipFromStorage, just triggered from the other direction.
     */
    @Synchronized
    public void giveAndEquipItem(String userId, ItemSlot slot, String itemId) {
        PartyMember member = requireMember(userId);
        String previousItemId = member.loadout().itemIdFor(slot);
        if (previousItemId != null) {
            addItemToStorage(previousItemId);
        }
        members.put(userId, member.withLoadout(member.loadout().withItem(slot, itemId)));
    }
}
