package com.asdru.asdrulet5.party.domain;

import com.asdru.asdrulet5.classdata.domain.ActiveEffect;
import com.asdru.asdrulet5.inventory.domain.ItemSlot;
import com.asdru.asdrulet5.party.exception.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartyTest {

    @Test
    void creatingPartyRegistersLeaderAsFirstMemberWithEmptyLoadout() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        assertThat(party.members()).hasSize(1);
        PartyMember leader = party.members().getFirst();
        assertThat(leader.userId()).isEqualTo("leader-1");
        assertThat(leader.leader()).isTrue();
        assertThat(leader.characterClass()).isNull();
        assertThat(leader.loadout().equippedItemIds()).isEmpty();
    }

    @Test
    void addMemberJoinsAsNonLeader() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        party.addMember("player-2", "Player Two", "avatar2.png");

        assertThat(party.members()).hasSize(2);
        PartyMember joined = party.members().get(1);
        assertThat(joined.leader()).isFalse();
    }

    @Test
    void addMemberIsIdempotentForSameUser() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        party.addMember("player-2", "Player Two", "avatar2.png");
        party.addMember("player-2", "Player Two", "avatar2.png");

        assertThat(party.members()).hasSize(2);
    }

    @Test
    void selectClassUpdatesMember() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        party.selectClass("leader-1", CharacterClass.MAGE);

        assertThat(party.members().getFirst().characterClass()).isEqualTo(CharacterClass.MAGE);
    }

    @Test
    void selectClassForUnknownUserThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        assertThatThrownBy(() -> party.selectClass("nobody", CharacterClass.TANK))
                .isInstanceOf(NotPartyMemberException.class);
    }

    @Test
    void selectClassAlreadyTakenByAnotherMemberThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");
        party.selectClass("leader-1", CharacterClass.WARRIOR);

        assertThatThrownBy(() -> party.selectClass("player-2", CharacterClass.WARRIOR))
                .isInstanceOf(ClassAlreadyTakenException.class);
    }

    @Test
    void reselectingOwnCurrentClassIsAllowed() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.selectClass("leader-1", CharacterClass.WARRIOR);

        party.selectClass("leader-1", CharacterClass.WARRIOR);

        assertThat(party.members().getFirst().characterClass()).isEqualTo(CharacterClass.WARRIOR);
    }

    @Test
    void startByNonLeaderThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");

        assertThatThrownBy(() -> party.start("player-2", List.of("leader-1", "player-2")))
                .isInstanceOf(NotPartyLeaderException.class);
        assertThat(party.status()).isEqualTo(PartyStatus.LOBBY);
    }

    @Test
    void startWithMissingMemberThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");

        assertThatThrownBy(() -> party.start("leader-1", List.of("leader-1")))
                .isInstanceOf(InvalidTurnOrderException.class);
    }

    @Test
    void startWithDuplicateThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");

        assertThatThrownBy(() -> party.start("leader-1", List.of("leader-1", "leader-1")))
                .isInstanceOf(InvalidTurnOrderException.class);
    }

    @Test
    void startWithMemberMissingClassSelectionThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");
        party.selectClass("leader-1", CharacterClass.WARRIOR);

        assertThatThrownBy(() -> party.start("leader-1", List.of("player-2", "leader-1")))
                .isInstanceOf(MissingClassSelectionException.class);
        assertThat(party.status()).isEqualTo(PartyStatus.LOBBY);
    }

    @Test
    void startByLeaderWithValidPermutationSetsOrderAndStatus() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");
        party.selectClass("leader-1", CharacterClass.WARRIOR);
        party.selectClass("player-2", CharacterClass.HEALER);
        assertThat(party.status()).isEqualTo(PartyStatus.LOBBY);

        party.start("leader-1", List.of("player-2", "leader-1"));

        assertThat(party.turnOrder()).containsExactly("player-2", "leader-1");
        assertThat(party.status()).isEqualTo(PartyStatus.DUNGEON);
    }

    @Test
    void addFakeMemberCreatesNonLeaderBotWithUniqueId() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        PartyMember bot1 = party.addFakeMember("Grog");
        PartyMember bot2 = party.addFakeMember("Brynn");

        assertThat(bot1.bot()).isTrue();
        assertThat(bot1.leader()).isFalse();
        assertThat(bot1.userId()).isNotEqualTo(bot2.userId());
        assertThat(party.members()).hasSize(3);
    }

    @Test
    void selectClassWorksForFakeMembersLikeAnyOtherMember() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        PartyMember bot = party.addFakeMember("Grog");

        party.selectClass(bot.userId(), CharacterClass.TANK);

        PartyMember updated = party.members().stream()
                .filter(member -> member.userId().equals(bot.userId()))
                .findFirst()
                .orElseThrow();
        assertThat(updated.characterClass()).isEqualTo(CharacterClass.TANK);
    }

    @Test
    void addMemberRejectsJoiningBeyondMaxMembers() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");
        party.addMember("player-3", "Player Three", "avatar3.png");
        party.addMember("player-4", "Player Four", "avatar4.png");

        assertThatThrownBy(() -> party.addMember("player-5", "Player Five", "avatar5.png"))
                .isInstanceOf(PartyFullException.class);
        assertThat(party.members()).hasSize(Party.MAX_MEMBERS);
    }

    @Test
    void addMemberStaysIdempotentEvenWhenPartyIsFull() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");
        party.addMember("player-3", "Player Three", "avatar3.png");
        party.addMember("player-4", "Player Four", "avatar4.png");

        PartyMember rejoined = party.addMember("player-2", "Player Two", "avatar2.png");

        assertThat(rejoined.userId()).isEqualTo("player-2");
        assertThat(party.members()).hasSize(Party.MAX_MEMBERS);
    }

    @Test
    void addFakeMemberRejectsBeyondMaxMembers() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addFakeMember("Grog");
        party.addFakeMember("Brynn");
        party.addFakeMember("Thistle");

        assertThatThrownBy(() -> party.addFakeMember("Kael"))
                .isInstanceOf(PartyFullException.class);
        assertThat(party.members()).hasSize(Party.MAX_MEMBERS);
    }

    @Test
    void equipItemFillsTheGivenSlotForThatMember() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        party.equipItem("leader-1", ItemSlot.WEAPON, "rusted-sword");

        PartyMember leader = party.members().getFirst();
        assertThat(leader.loadout().weaponItemId()).isEqualTo("rusted-sword");
        assertThat(leader.loadout().chestplateItemId()).isNull();
    }

    @Test
    void equipItemDoesNotDisturbOtherSlots() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        party.equipItem("leader-1", ItemSlot.WEAPON, "rusted-sword");
        party.equipItem("leader-1", ItemSlot.TRINKET, "lucky-charm");

        PartyMember leader = party.members().getFirst();
        assertThat(leader.loadout().weaponItemId()).isEqualTo("rusted-sword");
        assertThat(leader.loadout().trinketItemId()).isEqualTo("lucky-charm");
    }

    @Test
    void equipItemForUnknownMemberThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        assertThatThrownBy(() -> party.equipItem("nobody", ItemSlot.WEAPON, "rusted-sword"))
                .isInstanceOf(NotPartyMemberException.class);
    }

    @Test
    void freshMemberStartsAtFullHealthWithNoPendingEffects() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        PartyMember leader = party.members().getFirst();
        assertThat(leader.currentHealth()).isNull();
        assertThat(leader.pendingEffects()).isEmpty();
    }

    @Test
    void setMemberHealthUpdatesJustThatMember() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        party.setMemberHealth("leader-1", 42);

        assertThat(party.members().getFirst().currentHealth()).isEqualTo(42);
    }

    @Test
    void setMemberHealthBackToNullMeansFull() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.setMemberHealth("leader-1", 10);

        party.setMemberHealth("leader-1", null);

        assertThat(party.members().getFirst().currentHealth()).isNull();
    }

    @Test
    void setMemberHealthForUnknownMemberThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        assertThatThrownBy(() -> party.setMemberHealth("nobody", 10))
                .isInstanceOf(NotPartyMemberException.class);
    }

    @Test
    void addPendingEffectThenClearRemovesIt() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        party.addPendingEffect("leader-1",
                ActiveEffect.damageOverTime("Poison", "Venom.", "poison", 6, 4));
        PartyMember poisoned = party.members().getFirst();
        assertThat(poisoned.pendingEffects()).hasSize(1);
        assertThat(poisoned.pendingEffects().getFirst().name()).isEqualTo("Poison");
        assertThat(poisoned.pendingEffects().getFirst().remainingTurns()).isEqualTo(4);

        party.clearPendingEffects("leader-1");
        PartyMember cleared = party.members().getFirst();
        assertThat(cleared.pendingEffects()).isEmpty();
    }

    @Test
    void addPendingEffectWithSameNameReplacesInsteadOfStacking() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addPendingEffect("leader-1",
                ActiveEffect.damageOverTime("Poison", "Venom.", "poison", 6, 4));

        party.addPendingEffect("leader-1",
                ActiveEffect.damageOverTime("Poison", "Venom.", "poison", 6, 2));

        PartyMember member = party.members().getFirst();
        assertThat(member.pendingEffects()).hasSize(1);
        assertThat(member.pendingEffects().getFirst().remainingTurns()).isEqualTo(2);
    }

    @Test
    void addItemToStorageFillsFirstEmptyCell() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        party.addItemToStorage("rusted-sword");

        assertThat(party.storage().get(0)).isEqualTo("rusted-sword");
    }

    @Test
    void addItemToStorageIsANoOpWhenFull() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        List<String> full = new ArrayList<>();
        for (int i = 0; i < Party.STORAGE_SIZE; i++) {
            full.add("rusted-sword");
        }
        party.seedStorage(full);

        party.addItemToStorage("flame-edge");

        assertThat(party.storage()).doesNotContain("flame-edge");
    }

    @Test
    void wheelSpinTrackingRecordsAndResets() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");

        assertThat(party.hasSpunWheel("leader-1")).isFalse();
        assertThat(party.allMembersHaveSpunWheel()).isFalse();

        party.recordWheelSpin("leader-1", WheelEffect.FULL_HEAL);
        assertThat(party.hasSpunWheel("leader-1")).isTrue();
        assertThat(party.wheelResults()).containsEntry("leader-1", WheelEffect.FULL_HEAL);
        assertThat(party.allMembersHaveSpunWheel()).isFalse();

        party.recordWheelSpin("player-2", WheelEffect.POISON);
        assertThat(party.allMembersHaveSpunWheel()).isTrue();

        party.resetWheelSpins();
        assertThat(party.hasSpunWheel("leader-1")).isFalse();
        assertThat(party.wheelResults()).isEmpty();
        assertThat(party.allMembersHaveSpunWheel()).isFalse();
    }

    @Test
    void wheelAcknowledgementTrackingRecordsAndResets() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");

        assertThat(party.allMembersHaveAcknowledgedWheel()).isFalse();

        party.recordWheelAcknowledgement("leader-1");
        assertThat(party.allMembersHaveAcknowledgedWheel()).isFalse();

        party.recordWheelAcknowledgement("player-2");
        assertThat(party.allMembersHaveAcknowledgedWheel()).isTrue();

        party.resetWheelSpins();
        assertThat(party.allMembersHaveAcknowledgedWheel()).isFalse();
    }

    @Test
    void giveAndEquipItemEquipsDirectlyOntoTheMember() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        party.giveAndEquipItem("leader-1", ItemSlot.WEAPON, "rusted-sword");

        assertThat(party.members().getFirst().loadout().weaponItemId()).isEqualTo("rusted-sword");
        // A personal reward, not a shared drop — the item never touches the party's storage grid.
        assertThat(party.storage()).allMatch(java.util.Objects::isNull);
    }

    @Test
    void giveAndEquipItemSwapsWhateverWasEquippedBackIntoStorage() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.equipItem("leader-1", ItemSlot.WEAPON, "rusted-sword");

        party.giveAndEquipItem("leader-1", ItemSlot.WEAPON, "flame-edge");

        assertThat(party.members().getFirst().loadout().weaponItemId()).isEqualTo("flame-edge");
        assertThat(party.storage()).contains("rusted-sword");
    }

    @Test
    void giveAndEquipItemForUnknownMemberThrows() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");

        assertThatThrownBy(() -> party.giveAndEquipItem("nobody", ItemSlot.WEAPON, "rusted-sword"))
                .isInstanceOf(NotPartyMemberException.class);
    }

    @Test
    void remainingWheelEffectsStartsWithTheFullSetAndShrinksAsTheyreClaimed() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        assertThat(party.remainingWheelEffects()).containsExactlyInAnyOrder(WheelEffect.values());

        party.recordWheelSpin("leader-1", WheelEffect.POISON);

        assertThat(party.remainingWheelEffects()).containsExactlyInAnyOrder(
                WheelEffect.FULL_HEAL, WheelEffect.HALVE_HEALTH, WheelEffect.GIVE_ITEM, WheelEffect.CLEAR_EFFECTS);
    }

    @Test
    void resetWheelSpinsRestoresTheFullEffectPool() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.recordWheelSpin("leader-1", WheelEffect.POISON);

        party.resetWheelSpins();

        assertThat(party.remainingWheelEffects()).containsExactlyInAnyOrder(WheelEffect.values());
    }

    @Test
    void isMembersWheelTurnFollowsTurnOrderSequentially() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.addMember("player-2", "Player Two", "avatar2.png");
        party.selectClass("leader-1", CharacterClass.WARRIOR);
        party.selectClass("player-2", CharacterClass.HEALER);
        party.start("leader-1", List.of("player-2", "leader-1"));

        assertThat(party.isMembersWheelTurn("player-2")).isTrue();
        assertThat(party.isMembersWheelTurn("leader-1")).isFalse();

        party.recordWheelSpin("player-2", WheelEffect.FULL_HEAL);

        assertThat(party.isMembersWheelTurn("leader-1")).isTrue();
        assertThat(party.isMembersWheelTurn("player-2")).isFalse();
    }

    @Test
    void isMembersWheelTurnIsFalseForEveryoneOnceAllHaveSpun() {
        Party party = new Party("ABC123", "leader-1", "Leader", "avatar.png");
        party.selectClass("leader-1", CharacterClass.WARRIOR);
        party.start("leader-1", List.of("leader-1"));

        party.recordWheelSpin("leader-1", WheelEffect.FULL_HEAL);

        assertThat(party.isMembersWheelTurn("leader-1")).isFalse();
    }
}
