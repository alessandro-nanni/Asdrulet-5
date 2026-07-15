package com.asdru.asdrulet5.party.domain;

import com.asdru.asdrulet5.classdata.domain.ActiveEffect;

/**
 * What a single spin of a MYSTERY room's wheel can land on. Each constant
 * implements {@link #applyTo}, so PartyService's spinWheel just calls
 * {@code effect.applyTo(spinningMember, context)} polymorphically instead of
 * switching on the rolled effect.
 */
public enum WheelEffect {

    FULL_HEAL {
        @Override
        public void applyTo(PartyMember member, WheelContext context) {
            context.party().setMemberHealth(member.userId(), null);
        }
    },
    HALVE_HEALTH {
        @Override
        public void applyTo(PartyMember member, WheelContext context) {
            int max = context.effectiveMaxHealth(member);
            int current = member.currentHealth() != null ? member.currentHealth() : max;
            context.party().setMemberHealth(member.userId(), Math.max(1, current / 2));
        }
    },
    GIVE_ITEM {
        @Override
        public void applyTo(PartyMember member, WheelContext context) {
            context.giveRandomItemTo(member);
        }
    },
    GIVE_COINS {
        /** Fixed payout so every "Coins" spin feels equally worth landing on. */
        private static final int COIN_AMOUNT = 30;

        @Override
        public void applyTo(PartyMember member, WheelContext context) {
            context.party().addCoins(COIN_AMOUNT);
        }
    },
    CLEAR_EFFECTS {
        @Override
        public void applyTo(PartyMember member, WheelContext context) {
            context.party().clearPendingEffects(member.userId());
        }
    },
    POISON {
        /** Flat damage per turn, for this many turns, once the member's next fight starts. */
        private static final int POWER_PER_TURN = 6;
        private static final int DURATION_TURNS = 4;

        @Override
        public void applyTo(PartyMember member, WheelContext context) {
            context.party().addPendingEffect(member.userId(), ActiveEffect.damageOverTime(
                    "Poison", "A lingering venom saps your health each turn.", "poison",
                    POWER_PER_TURN, DURATION_TURNS));
        }
    };

    public abstract void applyTo(PartyMember member, WheelContext context);
}
