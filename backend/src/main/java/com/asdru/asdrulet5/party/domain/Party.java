package com.asdru.asdrulet5.party.domain;

import com.asdru.asdrulet5.classdata.domain.ActiveEffect;
import com.asdru.asdrulet5.inventory.domain.ItemDefinition;
import com.asdru.asdrulet5.inventory.domain.ItemSlot;
import com.asdru.asdrulet5.inventory.domain.Loadout;
import com.asdru.asdrulet5.party.exception.*;
import lombok.Getter;
import lombok.Synchronized;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Aggregate root for one party's game state. Owns the member roster,
 * turn order/status, shared loadout mutations, and coins directly; the
 * MYSTERY wheel and LOOT room state machines (identical in shape — see
 * {@link RoomTurnProgress}) and the shared storage grid / shop stock (see
 * {@link SharedStorage} / {@link ShopStock}) are delegated to small
 * single-purpose collaborators so this class stays about coordinating them,
 * not reimplementing each one inline. Every public method is
 * {@code @Synchronized} — that's the one lock those collaborators rely on,
 * since none of them lock on their own.
 */
public class Party {

    /**
     * Fixed lobby size cap, independent of how many CharacterClass values
     * exist — the two happen to match today but must not be coupled.
     */
    public static final int MAX_MEMBERS = 4;

    /**
     * Shared party storage grid: 3 columns x 10 rows, rendered by the frontend.
     */
    public static final int STORAGE_SIZE = 30;

    @Getter
    @Accessors(fluent = true)
    private final String code;

    @Getter
    @Accessors(fluent = true)
    private final String leaderId;

    private final Map<String, PartyMember> members = new LinkedHashMap<>();
    private final AtomicInteger fakeMemberSequence = new AtomicInteger();
    private final SharedStorage storage = new SharedStorage(STORAGE_SIZE);
    private final ShopStock shopStock = new ShopStock();
    /**
     * Bookkeeping for the MYSTERY room currently entered (if any) — see {@link RoomTurnProgress}.
     */
    private final RoomTurnProgress<WheelEffect> wheelProgress = new RoomTurnProgress<>();
    /**
     * Bookkeeping for the LOOT room currently entered (if any) — see {@link RoomTurnProgress}.
     */
    private final RoomTurnProgress<LootResult> lootProgress = new RoomTurnProgress<>();
    private List<String> turnOrder = List.of();
    /**
     * Shared coin pool the whole party spends from — see the shop below.
     */
    @Getter
    @Accessors(fluent = true)
    private int coins = 0;
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
        storage.seed(itemIds);
    }

    @Synchronized
    public List<String> storage() {
        return storage.list();
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
        if (storageIndex < 0 || storageIndex >= storage.size()) {
            throw new InvalidStorageIndexException(code, storageIndex);
        }
        String itemId = storage.at(storageIndex);
        if (itemId == null) {
            throw new EmptyStorageSlotException(code, storageIndex);
        }
        ItemSlot slot = slotResolver.apply(itemId);
        String previousItemId = member.loadout().itemIdFor(slot);
        storage.set(storageIndex, previousItemId);
        members.put(userId, member.withLoadout(member.loadout().withItem(slot, itemId)));
    }

    /**
     * Extra healing a consumed potion grants per layer of dungeon depth the
     * party has reached (see {@link #consumeItem}'s dungeonLayer parameter)
     * — a potion found right at the start is a modest top-up, one found deep
     * into a run heals for meaningfully more, mirroring how the dungeon
     * itself gets tougher with depth.
     */
    private static final int HEAL_BONUS_PER_DUNGEON_LAYER = 8;

    /**
     * Consumes whatever item sits in the given shared-storage cell — same
     * bounds/empty-cell validation as {@link #equipFromStorage}, plus a check
     * that the item is actually {@link ItemSlot#CONSUMABLE} (equippable gear
     * has no "use" of its own). The item is removed from storage rather than
     * moved anywhere, and its {@link ItemDefinition#healAmount()} — bumped by
     * {@link #HEAL_BONUS_PER_DUNGEON_LAYER} for every layer of dungeonLayer —
     * is applied to the consuming member, clamped to their own
     * effectiveMaxHealth (same "null means full health" convention as
     * {@link #setMemberHealth}). definitionLookup/effectiveMaxHealth are
     * injected the same way equipFromStorage injects its own slotResolver,
     * keeping ItemDefinitionRegistry/ClassDefinitionRegistry out of this
     * package; dungeonLayer is likewise resolved by the caller (see
     * {@code DungeonService.currentLayer}), since this package has no notion
     * of the dungeon either.
     */
    @Synchronized
    public void consumeItem(String userId, int storageIndex,
                             Function<String, ItemDefinition> definitionLookup,
                             ToIntFunction<PartyMember> effectiveMaxHealth,
                             int dungeonLayer) {
        PartyMember member = requireMember(userId);
        if (storageIndex < 0 || storageIndex >= storage.size()) {
            throw new InvalidStorageIndexException(code, storageIndex);
        }
        String itemId = storage.at(storageIndex);
        if (itemId == null) {
            throw new EmptyStorageSlotException(code, storageIndex);
        }
        ItemDefinition definition = definitionLookup.apply(itemId);
        if (definition.slot() != ItemSlot.CONSUMABLE) {
            throw new ItemNotConsumableException(code, itemId);
        }
        storage.set(storageIndex, null);
        int maxHealth = effectiveMaxHealth.applyAsInt(member);
        int currentHealth = member.currentHealth() == null ? maxHealth : member.currentHealth();
        int healAmount = definition.healAmount() + Math.max(0, dungeonLayer) * HEAL_BONUS_PER_DUNGEON_LAYER;
        int healedHealth = Math.min(maxHealth, currentHealth + healAmount);
        members.put(userId, member.withCurrentHealth(healedHealth >= maxHealth ? null : healedHealth));
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

    /**
     * Called whenever a room is entered, so wheel bookkeeping never leaks from one room visit into the next.
     */
    @Synchronized
    public void resetWheelSpins() {
        wheelProgress.reset();
    }

    @Synchronized
    public boolean hasSpunWheel(String userId) {
        return wheelProgress.hasActed(userId);
    }

    @Synchronized
    public void recordWheelSpin(String userId, WheelEffect effect) {
        wheelProgress.recordResult(userId, effect);
    }

    @Synchronized
    public boolean allMembersHaveSpunWheel() {
        return wheelProgress.allMembersHaveActed(members.keySet());
    }

    @Synchronized
    public Map<String, WheelEffect> wheelResults() {
        return wheelProgress.results();
    }

    /**
     * The WheelEffects nobody in the party has landed on yet this room visit — what a spin draws from.
     */
    @Synchronized
    public List<WheelEffect> remainingWheelEffects() {
        Set<WheelEffect> claimed = Set.copyOf(wheelProgress.results().values());
        return Arrays.stream(WheelEffect.values())
                .filter(effect -> !claimed.contains(effect))
                .toList();
    }

    /**
     * Whether userId is next in line to spin, per the same turnOrder set at
     * game start (see {@link #start}) — the first member in that sequence
     * who hasn't spun yet gets the wheel next, everyone else has to wait.
     */
    @Synchronized
    public boolean isMembersWheelTurn(String userId) {
        return wheelProgress.isMembersTurn(userId, turnOrder);
    }

    /**
     * Called once a member's own client has finished locally announcing
     * their spin result — see PartyService.acknowledgeWheelResult for why
     * the room's actual clear waits on this instead of a fixed delay.
     */
    @Synchronized
    public void recordWheelAcknowledgement(String userId) {
        wheelProgress.recordAcknowledgement(userId);
    }

    @Synchronized
    public boolean allMembersHaveAcknowledgedWheel() {
        return wheelProgress.allMembersHaveAcknowledged(members.keySet());
    }

    /**
     * Called whenever a room is entered, so loot bookkeeping never leaks from one room visit into the next.
     */
    @Synchronized
    public void resetLootClaims() {
        lootProgress.reset();
    }

    @Synchronized
    public boolean hasLooted(String userId) {
        return lootProgress.hasActed(userId);
    }

    @Synchronized
    public void recordLoot(String userId, LootResult result) {
        lootProgress.recordResult(userId, result);
    }

    @Synchronized
    public boolean allMembersHaveLooted() {
        return lootProgress.allMembersHaveActed(members.keySet());
    }

    @Synchronized
    public Map<String, LootResult> lootResults() {
        return lootProgress.results();
    }

    /**
     * Whether userId is next in line to loot, per the same turnOrder set at
     * game start (see {@link #start}) — the first member in that sequence
     * who hasn't looted yet gets the chest next, everyone else has to wait.
     */
    @Synchronized
    public boolean isMembersLootTurn(String userId) {
        return lootProgress.isMembersTurn(userId, turnOrder);
    }

    /**
     * Called once a member's own client has finished locally announcing
     * their loot result — see PartyService.acknowledgeLootResult for why
     * the room's actual clear waits on this instead of a fixed delay.
     */
    @Synchronized
    public void recordLootAcknowledgement(String userId) {
        lootProgress.recordAcknowledgement(userId);
    }

    @Synchronized
    public boolean allMembersHaveAcknowledgedLoot() {
        return lootProgress.allMembersHaveAcknowledged(members.keySet());
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

    /**
     * Drops itemId into the first empty storage cell; a no-op if the grid is already full.
     */
    @Synchronized
    public void addItemToStorage(String itemId) {
        storage.addFirstEmpty(itemId);
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

    @Synchronized
    public void addCoins(int amount) {
        coins += amount;
    }

    /**
     * Called whenever a MERCHANT room is entered, replacing whatever was left over from a prior visit.
     */
    @Synchronized
    public void rollShopStock(List<String> itemIds) {
        shopStock.roll(itemIds);
    }

    @Synchronized
    public List<String> shopStock() {
        return shopStock.list();
    }

    /**
     * Spends coins on an item currently for sale, atomically: validates the
     * item is actually in stock and the party can afford it, then removes it
     * from the shop's stock and drops it into shared storage — same
     * destination a LOOT pickup would use, so a bought item is equippable by
     * anyone, not just whoever paid for it.
     */
    @Synchronized
    public void buyFromShop(String itemId, int price) {
        if (!shopStock.contains(itemId)) {
            throw new ItemNotInShopException(code, itemId);
        }
        if (coins < price) {
            throw new InsufficientCoinsException(code, price, coins);
        }
        shopStock.remove(itemId);
        coins -= price;
        addItemToStorage(itemId);
    }
}
