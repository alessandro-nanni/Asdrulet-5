package com.asdru.asdrulet5.inventory;

import com.asdru.asdrulet5.classdata.domain.ActiveEffect;
import com.asdru.asdrulet5.classdata.domain.Damage;
import com.asdru.asdrulet5.classdata.domain.EffectTarget;
import com.asdru.asdrulet5.inventory.domain.ItemDefinition;
import com.asdru.asdrulet5.inventory.domain.ItemPassive;
import com.asdru.asdrulet5.inventory.domain.ItemSlot;
import com.asdru.asdrulet5.inventory.exception.UnknownItemDefinitionException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Static catalog of equippable items — mechanics and flavor only. To add a
 * new item: add a factory method here and add it to
 * {@link #buildDefinitions()} — a flat stat boost overrides one of
 * ItemPassive's bonusX()/damagePercent() methods, a reactive item overrides
 * one of its onX() hooks, and one that depends on live combat state or
 * randomness overrides damagePercentBonus(wearer) or
 * triggersFollowUpAbility() (see {@link ItemPassive}).
 *
 * <p>Whether/how often an item drops (shop eligibility, weight, floor) is
 * not this class's concern — see {@code LootPoolRegistry} for that. Adding
 * an item here makes it equippable and describable; it still needs an entry
 * there to actually turn up anywhere in the game.
 */
@Component
public class ItemDefinitionRegistry {

    private static final Map<String, ItemDefinition> DEFINITIONS = buildDefinitions();

    private static Map<String, ItemDefinition> buildDefinitions() {
        return Stream.of(
                        scythe(),
                        torch(),
                        luckyCharm(),
                        satelliteDish(),
                        twitchingTalisman(),
                        leatherTunic(),
                        mantleOfTheUsurper(),
                        healingPotion(),
                        berserkerBreastplate()
                )
                .collect(Collectors.toMap(ItemDefinition::id, Function.identity()));
    }

    private static ItemDefinition scythe() {
        return new ItemDefinition("scythe", "Scythe", ItemSlot.WEAPON,
                "Grows deadlier with every ally who doesn't make it back — +13% damage for each dead teammate.",
                new ItemPassive() {
                    @Override
                    public int damagePercentBonus(EffectTarget wearer) {
                        return wearer.deadAllyCount() * 13;
                    }
                }, 40, 0);
    }

    private static ItemDefinition torch() {
        return new ItemDefinition("torch", "Torch", ItemSlot.WEAPON,
                "Never quite goes out — every hit carries a 10% chance to set its target on fire for 2 turns.",
                new ItemPassive() {
                    @Override
                    public void onDamageDealt(EffectTarget wearer, EffectTarget target, Damage damage) {
                        if (ThreadLocalRandom.current().nextDouble() < 0.10) {
                            target.addActiveEffect(ActiveEffect.damageOverTime(
                                    "On Fire", "Burning — takes damage each turn.", "burn", 8, 2));
                        }
                    }
                }, 35, 0);
    }

    private static ItemDefinition luckyCharm() {
        return new ItemDefinition("lucky-charm", "Lucky Charm", ItemSlot.TRINKET,
                "Warm to the touch — a rare 2% chance for any hit to land for 300% damage.",
                new ItemPassive() {
                    @Override
                    public int damagePercentBonus(EffectTarget wearer) {
                        return ThreadLocalRandom.current().nextDouble() < 0.02 ? 200 : 0;
                    }
                }, 30, 0);
    }

    private static ItemDefinition satelliteDish() {
        return new ItemDefinition("satellite-dish", "Satellite Dish", ItemSlot.TRINKET,
                "Draws power from somewhere overhead. +6 defense, but bleeds 10 stamina at the start of every turn.",
                new ItemPassive() {
                    @Override
                    public int bonusDefense() {
                        return 6;
                    }

                    @Override
                    public void onStartTurn(EffectTarget wearer) {
                        wearer.drainStamina(10);
                    }
                }, 30, 0);
    }

    private static ItemDefinition twitchingTalisman() {
        return new ItemDefinition("twitching-talisman", "Twitching Talisman", ItemSlot.TRINKET,
                "Jitters with restless energy — a 10% chance to immediately act again with the same basic ability "
                        + "(never an ultimate).",
                new ItemPassive() {
                    @Override
                    public boolean triggersFollowUpAbility() {
                        return ThreadLocalRandom.current().nextDouble() < 0.10;
                    }
                }, 35, 0);
    }

    private static ItemDefinition leatherTunic() {
        return new ItemDefinition("leather-tunic", "Leather Tunic", ItemSlot.CHESTPLATE,
                "Simple and sturdy.",
                new ItemPassive() {
                    @Override
                    public int bonusDefense() {
                        return 4;
                    }
                }, 20, 0);
    }

    private static ItemDefinition mantleOfTheUsurper() {
        return new ItemDefinition("mantle-of-the-usurper", "Mantle of the Usurper", ItemSlot.CHESTPLATE,
                "As long as you have more health than your party's leader: +5% damage and +7% max health.",
                new ItemPassive() {
                    @Override
                    public int damagePercentBonus(EffectTarget wearer) {
                        return wearer.healthierThanLeader() ? 5 : 0;
                    }

                    @Override
                    public int bonusMaxHealthPercent(EffectTarget wearer) {
                        return wearer.healthierThanLeader() ? 7 : 0;
                    }
                }, 30, 0);
    }

    private static ItemDefinition berserkerBreastplate() {
        return new ItemDefinition("berserker-breastplate", "Berserker Breastplate", ItemSlot.CHESTPLATE,
                "When below 20% health, gain 15% damage",
                new ItemPassive() {
                    @Override
                    public int damagePercentBonus(EffectTarget wearer) {
                        int amount = 0;

                        double hpPercentage = (double) wearer.currentHealth() / wearer.maxHealth();
                        if (hpPercentage < 0.20) {
                            amount = 15;
                        }
                        return amount;
                    }
                }, 40, 0);
    }

    /**
     * Consumed from the inventory screen rather than equipped — restores 40
     * health on use, plus a bonus that grows with how deep into the dungeon
     * run the party currently is. See {@code Party.consumeItem} (this 40 is
     * just the base — the depth bonus isn't part of ItemDefinition at all).
     */
    private static ItemDefinition healingPotion() {
        return new ItemDefinition("healing-potion", "Healing Potion", ItemSlot.CONSUMABLE,
                "A swirling red brew. Drink it to restore health — the deeper you've delved, the more it heals.",
                new ItemPassive() {
                }, 15, 40);
    }

    public List<ItemDefinition> all() {
        return List.copyOf(DEFINITIONS.values());
    }

    public ItemDefinition get(String id) {
        ItemDefinition definition = DEFINITIONS.get(id);
        if (definition == null) {
            throw new UnknownItemDefinitionException(id);
        }
        return definition;
    }
}
