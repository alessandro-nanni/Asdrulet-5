package com.asdru.asdrulet5.inventory;

import com.asdru.asdrulet5.classdata.domain.EffectTarget;
import com.asdru.asdrulet5.inventory.domain.ItemDefinition;
import com.asdru.asdrulet5.inventory.domain.ItemPassive;
import com.asdru.asdrulet5.inventory.domain.ItemSlot;
import com.asdru.asdrulet5.inventory.exception.UnknownItemDefinitionException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Static catalog of equippable items. To add a new item: add a factory
 * method here and add it to {@link #buildDefinitions()} — a flat stat boost
 * overrides one of ItemPassive's bonusX() methods, a reactive item overrides
 * one of its onX() hooks (see {@link ItemPassive}).
 */
@Component
public class ItemDefinitionRegistry {

    private static final Map<String, ItemDefinition> DEFINITIONS = buildDefinitions();

    private static Map<String, ItemDefinition> buildDefinitions() {
        return Stream.of(
                        rustedSword(), flameEdge(), vampiricFang(),
                        leatherVest(), plateArmor(), thornedPlate(),
                        luckyCharm(), berserkersRing(), executionersBadge())
                .collect(Collectors.toMap(ItemDefinition::id, Function.identity()));
    }

    private static ItemDefinition rustedSword() {
        return new ItemDefinition("rusted-sword", "Rusted Sword", ItemSlot.WEAPON,
                "A pitted old blade — better than fists.",
                new ItemPassive() {
                    @Override
                    public int bonusDamage() {
                        return 3;
                    }
                });
    }

    private static ItemDefinition flameEdge() {
        return new ItemDefinition("flame-edge", "Flame Edge", ItemSlot.WEAPON,
                "Still warm from the forge.",
                new ItemPassive() {
                    @Override
                    public int bonusDamage() {
                        return 7;
                    }
                });
    }

    private static ItemDefinition vampiricFang() {
        return new ItemDefinition("vampiric-fang", "Vampiric Fang", ItemSlot.WEAPON,
                "Drinks deep with every cut, feeding a quarter of the wound back to its wielder.",
                new ItemPassive() {
                    @Override
                    public void onDamageDealt(EffectTarget wearer, EffectTarget target, int amount) {
                        wearer.applyHeal(Math.max(1, amount / 4));
                    }
                });
    }

    private static ItemDefinition leatherVest() {
        return new ItemDefinition("leather-vest", "Leather Vest", ItemSlot.CHESTPLATE,
                "Light and flexible.",
                new ItemPassive() {
                    @Override
                    public int bonusMaxHealth() {
                        return 10;
                    }

                    @Override
                    public int bonusDefense() {
                        return 2;
                    }
                });
    }

    private static ItemDefinition plateArmor() {
        return new ItemDefinition("plate-armor", "Plate Armor", ItemSlot.CHESTPLATE,
                "Heavy, but it'll stop most things.",
                new ItemPassive() {
                    @Override
                    public int bonusMaxHealth() {
                        return 20;
                    }

                    @Override
                    public int bonusDefense() {
                        return 6;
                    }
                });
    }

    private static ItemDefinition thornedPlate() {
        return new ItemDefinition("thorned-plate", "Thorned Plate", ItemSlot.CHESTPLATE,
                "Studded with barbs that bite back — a fifth of any hit lands right back on the attacker.",
                new ItemPassive() {
                    @Override
                    public void onDamageTaken(EffectTarget wearer, EffectTarget attacker, int amount) {
                        attacker.applyDamage(Math.max(1, amount / 5));
                    }
                });
    }

    private static ItemDefinition luckyCharm() {
        return new ItemDefinition("lucky-charm", "Lucky Charm", ItemSlot.TRINKET,
                "Warm to the touch.",
                new ItemPassive() {
                    @Override
                    public int bonusMaxStamina() {
                        return 15;
                    }
                });
    }

    private static ItemDefinition berserkersRing() {
        return new ItemDefinition("berserkers-ring", "Berserker's Ring", ItemSlot.TRINKET,
                "Hums with restrained aggression — a trade of safety for power.",
                new ItemPassive() {
                    @Override
                    public int bonusMaxHealth() {
                        return -10;
                    }

                    @Override
                    public int bonusDefense() {
                        return -2;
                    }

                    @Override
                    public int bonusDamage() {
                        return 5;
                    }
                });
    }

    private static ItemDefinition executionersBadge() {
        return new ItemDefinition("executioners-badge", "Executioner's Badge", ItemSlot.TRINKET,
                "A grim trophy that rewards finishing the job — heals 10 health on a kill.",
                new ItemPassive() {
                    @Override
                    public void onKill(EffectTarget wearer, EffectTarget victim) {
                        wearer.applyHeal(10);
                    }
                });
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
