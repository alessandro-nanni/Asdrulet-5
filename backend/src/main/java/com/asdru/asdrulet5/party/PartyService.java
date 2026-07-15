package com.asdru.asdrulet5.party;

import com.asdru.asdrulet5.auth.AuthenticatedUser;
import com.asdru.asdrulet5.classdata.ClassDefinitionRegistry;
import com.asdru.asdrulet5.combat.CombatService;
import com.asdru.asdrulet5.combat.CombatVictoryEvent;
import com.asdru.asdrulet5.combat.domain.Combatant;
import com.asdru.asdrulet5.dungeon.DungeonService;
import com.asdru.asdrulet5.dungeon.domain.RoomType;
import com.asdru.asdrulet5.inventory.ItemDefinitionRegistry;
import com.asdru.asdrulet5.inventory.domain.ItemDefinition;
import com.asdru.asdrulet5.inventory.domain.ItemSlot;
import com.asdru.asdrulet5.party.dev.FakeNameGenerator;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import com.asdru.asdrulet5.party.domain.LootResult;
import com.asdru.asdrulet5.party.domain.Party;
import com.asdru.asdrulet5.party.domain.PartyMember;
import com.asdru.asdrulet5.party.domain.WheelContext;
import com.asdru.asdrulet5.party.domain.WheelEffect;
import com.asdru.asdrulet5.party.exception.AlreadyLootedException;
import com.asdru.asdrulet5.party.exception.AlreadySpunWheelException;
import com.asdru.asdrulet5.party.exception.NotAFakeMemberException;
import com.asdru.asdrulet5.party.exception.NotInLootRoomException;
import com.asdru.asdrulet5.party.exception.NotInMerchantRoomException;
import com.asdru.asdrulet5.party.exception.NotInMysteryRoomException;
import com.asdru.asdrulet5.party.exception.NotPartyLeaderException;
import com.asdru.asdrulet5.party.exception.NotPartyMemberException;
import com.asdru.asdrulet5.party.exception.NotYetLootedException;
import com.asdru.asdrulet5.party.exception.NotYetSpunWheelException;
import com.asdru.asdrulet5.party.exception.NotYourLootTurnException;
import com.asdru.asdrulet5.party.exception.NotYourWheelTurnException;
import com.asdru.asdrulet5.party.exception.PartyNotFoundException;
import com.asdru.asdrulet5.party.web.PartyMapper;
import com.asdru.asdrulet5.party.web.dto.PartyStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartyService {

    /**
     * How long a won fight stays on screen before the party is pulled back
     * to the dungeon map. Without this gap, the party-status broadcast
     * (DUNGEON) can reach the frontend so close behind the combat action's
     * own HTTP response (PARTY_WON) that the dungeon screen swaps back in
     * before the player ever sees the victory banner.
     */
    private static final long VICTORY_DISPLAY_DELAY_SECONDS = 3;

    /** How many distinct items a freshly entered MERCHANT room offers for sale. */
    private static final int SHOP_STOCK_SIZE = 3;

    private final PartyRepository partyRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DungeonService dungeonService;
    private final CombatService combatService;
    private final ItemDefinitionRegistry itemDefinitionRegistry;
    private final ClassDefinitionRegistry classDefinitionRegistry;
    private final ScheduledExecutorService victoryReturnScheduler;
    private final RoomEntryDelay roomEntryDelay;
    private final SecureRandom random = new SecureRandom();

    public PartyStateDto createParty(AuthenticatedUser leader) {
        String code = partyRepository.generateUniqueCode();
        Party party = new Party(code, leader.id(), leader.displayName(), leader.avatarUrl());
        partyRepository.save(party);
        return PartyMapper.toDto(party);
    }

    public PartyStateDto joinParty(String code, AuthenticatedUser user) {
        Party party = getOrThrow(code);
        party.addMember(user.id(), user.displayName(), user.avatarUrl());
        return broadcast(party);
    }

    public PartyStateDto startGame(String code, String requesterId, List<String> order) {
        Party party = getOrThrow(code);
        party.start(requesterId, order);
        PartyStateDto dto = broadcast(party);
        dungeonService.startDungeon(party.code(), party.leaderId());
        return dto;
    }

    /**
     * Commits to whatever next room is currently selected on the dungeon
     * map. Fight/boss rooms drop the party into combat (status flips to
     * IN_PROGRESS, mirroring the old enterCombat flow); anything else has no
     * gameplay of its own yet, so it's cleared immediately, unlocking the
     * next set of choices right away.
     *
     * <p>roomEntryDelay.sleep() holds the transition (and therefore the
     * broadcast announcing it) back briefly so the frontend's room-entry
     * swirl animation has time to actually play before the screen changes —
     * see {@link RoomEntryDelay}'s own comment for why this can't just be a
     * client-side delay.
     */
    public PartyStateDto enterRoom(String code, String requesterId) {
        Party party = getOrThrow(code);
        RoomType enteredRoomType = dungeonService.enterNode(code, requesterId);
        roomEntryDelay.sleep();
        if (enteredRoomType == RoomType.FIGHT || enteredRoomType == RoomType.BOSS) {
            // Snapshot before entering combat: this is what carries each
            // member's pending effects (e.g. a MYSTERY wheel's poison) into
            // the fresh Combatant CombatService is about to build. The
            // party's own copy is cleared right after taking the snapshot,
            // so a pending effect a wheel handed out is consumed exactly
            // once even though the fight itself doesn't write anything back
            // onto PartyMember when it ends.
            List<PartyMember> membersForCombat = party.members();
            party.enterCombat(requesterId);
            membersForCombat.stream()
                    .filter(member -> !member.pendingEffects().isEmpty())
                    .forEach(member -> party.clearPendingEffects(member.userId()));
            PartyStateDto dto = broadcast(party);
            combatService.startCombat(party.code(), membersForCombat, party.turnOrder());
            return dto;
        }
        if (enteredRoomType == RoomType.MYSTERY) {
            // Stays entered — unlike LOOT there's real gameplay here, so the
            // room only clears once every member has spun (see spinWheel).
            // resetWheelSpins() means a MYSTERY room visited a second time (a
            // fresh dungeon run, or in principle a re-roll) starts with a
            // clean slate rather than reusing stale spins.
            party.resetWheelSpins();
            return broadcast(party);
        }
        if (enteredRoomType == RoomType.MERCHANT) {
            // Also stays entered — a real shop to browse rather than an
            // instant-clear flavor room, cleared explicitly via leaveShop.
            // Rolled fresh every visit so re-entering doesn't just replay
            // whatever was left over from a prior stop.
            party.rollShopStock(pickRandomUnplacedItemIds(party, SHOP_STOCK_SIZE, true));
            return broadcast(party);
        }
        if (enteredRoomType == RoomType.LOOT) {
            // Also stays entered — same turn-ordered, one-chest-per-member
            // shape as MYSTERY (see lootChest), so the room only clears once
            // every member has both looted and acknowledged.
            // resetLootClaims() means a LOOT room visited a second time (a
            // fresh dungeon run) starts with a clean slate rather than
            // reusing stale claims.
            party.resetLootClaims();
            return broadcast(party);
        }
        dungeonService.clearEnteredNode(code);
        return PartyMapper.toDto(party);
    }

    /**
     * One party member spinning the wheel in the MYSTERY room the party
     * currently has entered. There's one shared wheel for the whole party:
     * the effect is drawn from whichever {@link WheelEffect}s nobody has
     * landed on yet this room visit (see Party.remainingWheelEffects), so
     * results never repeat across members, and only whoever is next in the
     * party's own turnOrder (set at game start) is allowed to spin — see
     * Party.isMembersWheelTurn. The rolled effect applies to the spinning
     * member alone, polymorphically (each effect knows its own behavior —
     * see {@link WheelEffect#applyTo}, nothing here switches on which one
     * was rolled). Never touches the room's clear state directly — see
     * {@link #acknowledgeWheelResult} for that, so a member's screen is
     * never swapped out from under them mid-spin.
     */
    public PartyStateDto spinWheel(String code, String userId) {
        Party party = getOrThrow(code);
        PartyMember member = party.members().stream()
                .filter(candidate -> candidate.userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new NotPartyMemberException(code, userId));
        if (dungeonService.enteredRoomType(code) != RoomType.MYSTERY) {
            throw new NotInMysteryRoomException(code);
        }
        if (party.hasSpunWheel(userId)) {
            throw new AlreadySpunWheelException(code, userId);
        }
        if (!party.isMembersWheelTurn(userId)) {
            throw new NotYourWheelTurnException(code, userId);
        }

        List<WheelEffect> remaining = party.remainingWheelEffects();
        WheelEffect effect = remaining.get(random.nextInt(remaining.size()));
        effect.applyTo(member, wheelContextFor(party));
        party.recordWheelSpin(userId, effect);
        return broadcast(party);
    }

    /**
     * Called once a member's own client has finished locally announcing
     * their spin result (see MysteryWheelScreen's spin-then-announce
     * timing on the frontend). The room only actually clears once every
     * member has both spun and acknowledged — driven by the clients
     * themselves rather than a fixed backend delay, so there's no timer to
     * tune against however long the frontend's own animation happens to
     * take.
     */
    public PartyStateDto acknowledgeWheelResult(String code, String userId) {
        Party party = getOrThrow(code);
        boolean isMember = party.members().stream().anyMatch(candidate -> candidate.userId().equals(userId));
        if (!isMember) {
            throw new NotPartyMemberException(code, userId);
        }
        if (!party.hasSpunWheel(userId)) {
            throw new NotYetSpunWheelException(code, userId);
        }
        party.recordWheelAcknowledgement(userId);
        if (party.allMembersHaveAcknowledgedWheel()) {
            dungeonService.clearEnteredNode(code);
        }
        return broadcast(party);
    }

    /**
     * One party member opening the chest in the LOOT room the party
     * currently has entered. Same turn-ordered shape as {@link #spinWheel},
     * minus a shared pool to draw from — unlike a WheelEffect, each member's
     * find is rolled fresh (see {@link #rollLoot}) rather than drawn from a
     * fixed exhaustible set, so results can repeat in kind (e.g. two members
     * can both find coins) even though each member only ever opens the
     * chest once. Never touches the room's clear state directly — see
     * {@link #acknowledgeLootResult} for that.
     */
    public PartyStateDto lootChest(String code, String userId) {
        Party party = getOrThrow(code);
        boolean isMember = party.members().stream().anyMatch(candidate -> candidate.userId().equals(userId));
        if (!isMember) {
            throw new NotPartyMemberException(code, userId);
        }
        if (dungeonService.enteredRoomType(code) != RoomType.LOOT) {
            throw new NotInLootRoomException(code);
        }
        if (party.hasLooted(userId)) {
            throw new AlreadyLootedException(code, userId);
        }
        if (!party.isMembersLootTurn(userId)) {
            throw new NotYourLootTurnException(code, userId);
        }

        LootResult result = rollLoot(party);
        if (result.coins() > 0) {
            party.addCoins(result.coins());
        }
        if (result.itemId() != null) {
            ItemSlot slot = itemDefinitionRegistry.get(result.itemId()).slot();
            party.giveAndEquipItem(userId, slot, result.itemId());
        }
        party.recordLoot(userId, result);
        return broadcast(party);
    }

    /**
     * Called once a member's own client has finished locally announcing
     * their loot result (see LootRoomScreen's open-then-announce timing on
     * the frontend). The room only actually clears once every member has
     * both looted and acknowledged — driven by the clients themselves
     * rather than a fixed backend delay, same as the wheel's own
     * acknowledgement flow.
     */
    public PartyStateDto acknowledgeLootResult(String code, String userId) {
        Party party = getOrThrow(code);
        boolean isMember = party.members().stream().anyMatch(candidate -> candidate.userId().equals(userId));
        if (!isMember) {
            throw new NotPartyMemberException(code, userId);
        }
        if (!party.hasLooted(userId)) {
            throw new NotYetLootedException(code, userId);
        }
        party.recordLootAcknowledgement(userId);
        if (party.allMembersHaveAcknowledgedLoot()) {
            dungeonService.clearEnteredNode(code);
        }
        return broadcast(party);
    }

    /**
     * A chest always yields something — coins, an item, or both, picked with
     * equal odds across those three shapes. Item drops are unrestricted (not
     * filtered to {@link ItemDefinition#purchasable()} items, unlike the
     * shop) — this is exactly where the non-purchasable, reactive items are
     * meant to turn up.
     */
    private LootResult rollLoot(Party party) {
        int roll = random.nextInt(3);
        int coins = roll != 1 ? rollLootCoins() : 0;
        String itemId = roll != 0 ? pickRandomUnplacedItemId(party) : null;
        return new LootResult(coins, itemId);
    }

    private int rollLootCoins() {
        return 10 + random.nextInt(16);
    }

    private WheelContext wheelContextFor(Party party) {
        return new WheelContext() {
            @Override
            public Party party() {
                return party;
            }

            @Override
            public int effectiveMaxHealth(PartyMember member) {
                int base = classDefinitionRegistry.get(member.characterClass()).stats().maxHealth();
                int bonus = member.loadout().equippedItemIds().stream()
                        .map(itemDefinitionRegistry::get)
                        .mapToInt(definition -> definition.passive().bonusMaxHealth())
                        .sum();
                return Math.max(1, base + bonus);
            }

            @Override
            public void giveRandomItemTo(PartyMember member) {
                String itemId = pickRandomUnplacedItemId(party);
                ItemSlot slot = itemDefinitionRegistry.get(itemId).slot();
                party.giveAndEquipItem(member.userId(), slot, itemId);
            }
        };
    }

    /**
     * Favors items that aren't already in play (in the shared storage or
     * equipped by anyone) so a spin/shop visit feels like discovering
     * something new rather than handing back a duplicate — falling back to
     * the full catalog only once every item is already somewhere in the
     * party. count is clamped to however many distinct candidates exist.
     * purchasableOnly restricts the pool to {@link ItemDefinition#purchasable()}
     * items — used by the shop, so items meant to only ever drop as loot
     * (e.g. a wheel's GIVE_ITEM, which calls this with purchasableOnly=false)
     * never show up for sale.
     */
    private List<String> pickRandomUnplacedItemIds(Party party, int count, boolean purchasableOnly) {
        Set<String> inPlay = new HashSet<>(party.storage());
        party.members().forEach(member -> inPlay.addAll(member.loadout().equippedItemIds()));
        List<String> candidates = itemDefinitionRegistry.all().stream()
                .filter(definition -> !purchasableOnly || definition.purchasable())
                .map(ItemDefinition::id)
                .filter(id -> !inPlay.contains(id))
                .collect(Collectors.toCollection(ArrayList::new));
        if (candidates.isEmpty()) {
            candidates = itemDefinitionRegistry.all().stream()
                    .filter(definition -> !purchasableOnly || definition.purchasable())
                    .map(ItemDefinition::id)
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        Collections.shuffle(candidates, random);
        return candidates.subList(0, Math.min(count, candidates.size()));
    }

    private String pickRandomUnplacedItemId(Party party) {
        return pickRandomUnplacedItemIds(party, 1, false).get(0);
    }

    /**
     * Fired by CombatService the instant a Combat's status flips to
     * PARTY_WON. Each member's ending health and any still-active effects
     * are carried back onto their PartyMember right away (see
     * {@link #syncMembersAfterCombat}) — otherwise, per PartyMember's own
     * prior doc, a fight's outcome was never written back onto it and every
     * room after a battle would silently reset the party to how it looked
     * going in. The coin reward is granted right away too — broadcast
     * immediately so it (and its toast) show up while the "Victory!" screen
     * is still up — but the actual return-to-dungeon transition is scheduled
     * a few seconds out, so the victory banner itself has time to actually
     * be seen — see VICTORY_DISPLAY_DELAY_SECONDS.
     */
    @EventListener
    public void onCombatVictory(CombatVictoryEvent event) {
        Party party = getOrThrow(event.partyCode());
        syncMembersAfterCombat(party, event.partyCode());
        party.addCoins(rollVictoryCoins());
        broadcast(party);
        victoryReturnScheduler.schedule(
                () -> returnToDungeonAfterVictory(event.partyCode()),
                VICTORY_DISPLAY_DELAY_SECONDS,
                TimeUnit.SECONDS);
    }

    /**
     * Copies each party combatant's final health and remaining active
     * effects (buffs, DoTs, ...) from the just-resolved Combat back onto its
     * matching PartyMember — reusing the exact same currentHealth/
     * pendingEffects fields a MYSTERY wheel roll writes to, so a fight's
     * outcome flows into the next room the same way a wheel result would.
     * Health at (or above) max is normalized back to null, matching the
     * "null means full health" convention everywhere else this field is set.
     */
    private void syncMembersAfterCombat(Party party, String combatCode) {
        for (Combatant combatant : combatService.partyCombatantsFor(combatCode)) {
            Integer health = combatant.currentHealth() >= combatant.maxHealth() ? null : combatant.currentHealth();
            party.setMemberHealth(combatant.id(), health);
            party.clearPendingEffects(combatant.id());
            combatant.activeEffects().forEach(effect -> party.addPendingEffect(combatant.id(), effect));
        }
    }

    private int rollVictoryCoins() {
        return 15 + random.nextInt(16);
    }

    private void returnToDungeonAfterVictory(String code) {
        Party party = getOrThrow(code);
        dungeonService.clearEnteredNode(code);
        party.returnToDungeon();
        broadcast(party);
    }

    /**
     * Leader-only, like {@link #leaveShop} — everyone can browse the shop's
     * stock (see the read-only PartyStateDto.shopStock broadcast to all
     * members), but only the leader spends the party's shared coins.
     */
    public PartyStateDto buyItem(String code, String userId, String itemId) {
        Party party = getOrThrow(code);
        if (!party.leaderId().equals(userId)) {
            throw new NotPartyLeaderException(code, userId);
        }
        if (dungeonService.enteredRoomType(code) != RoomType.MERCHANT) {
            throw new NotInMerchantRoomException(code);
        }
        int price = itemDefinitionRegistry.get(itemId).price();
        party.buyFromShop(itemId, price);
        return broadcast(party);
    }

    /**
     * Closes the currently entered MERCHANT room, leader-only like every
     * other room-navigation action — there's no per-member turn-taking here
     * (unlike the wheel), so whoever's driving the map just calls this once
     * everyone's done shopping.
     */
    public PartyStateDto leaveShop(String code, String requesterId) {
        Party party = getOrThrow(code);
        if (!party.leaderId().equals(requesterId)) {
            throw new NotPartyLeaderException(code, requesterId);
        }
        dungeonService.clearEnteredNode(code);
        return broadcast(party);
    }

    public PartyStateDto equipItem(String code, String userId, String itemId) {
        Party party = getOrThrow(code);
        ItemDefinition definition = itemDefinitionRegistry.get(itemId);
        party.equipItem(userId, definition.slot(), itemId);
        return broadcast(party);
    }

    public PartyStateDto equipFromStorage(String code, String userId, int storageIndex) {
        Party party = getOrThrow(code);
        party.equipFromStorage(userId, storageIndex, itemId -> itemDefinitionRegistry.get(itemId).slot());
        return broadcast(party);
    }

    public PartyStateDto getState(String code) {
        return PartyMapper.toDto(getOrThrow(code));
    }

    public PartyStateDto addFakeMembers(String code, int count) {
        Party party = getOrThrow(code);
        for (int i = 0; i < count; i++) {
            party.addFakeMember(FakeNameGenerator.next());
        }
        return broadcast(party);
    }

    public PartyStateDto selectClassAsFakeMember(String code, String fakeMemberId, CharacterClass characterClass) {
        Party party = getOrThrow(code);
        PartyMember target = party.members().stream()
                .filter(member -> member.userId().equals(fakeMemberId))
                .findFirst()
                .orElseThrow(() -> new NotPartyMemberException(code, fakeMemberId));
        if (!target.bot()) {
            throw new NotAFakeMemberException(code, fakeMemberId);
        }
        party.selectClass(fakeMemberId, characterClass);
        return broadcast(party);
    }

    /**
     * Like {@link #selectClassAsFakeMember}, but for any real (non-bot)
     * member — no bot-ownership guard, since a real member can only ever act
     * as themselves (the id they present is their own).
     */
    public PartyStateDto selectClass(String code, String memberId, CharacterClass characterClass) {
        Party party = getOrThrow(code);
        party.selectClass(memberId, characterClass);
        return broadcast(party);
    }

    private Party getOrThrow(String code) {
        return partyRepository.findByCode(code).orElseThrow(() -> new PartyNotFoundException(code));
    }

    private PartyStateDto broadcast(Party party) {
        PartyStateDto dto = PartyMapper.toDto(party);
        messagingTemplate.convertAndSend("/topic/party/" + party.code(), dto);
        return dto;
    }
}
