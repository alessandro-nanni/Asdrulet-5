package com.asdru.asdrulet5.combat;

import com.asdru.asdrulet5.classdata.ClassDefinitionRegistry;
import com.asdru.asdrulet5.classdata.SkillTreeRegistry;
import com.asdru.asdrulet5.classdata.domain.Ability;
import com.asdru.asdrulet5.classdata.domain.ClassDefinition;
import com.asdru.asdrulet5.classdata.domain.SkillTreeResolver;
import com.asdru.asdrulet5.classdata.domain.Stats;
import com.asdru.asdrulet5.classdata.domain.UltimateAbility;
import com.asdru.asdrulet5.combat.domain.*;
import com.asdru.asdrulet5.combat.exception.CombatNotFoundException;
import com.asdru.asdrulet5.combat.web.CombatMapper;
import com.asdru.asdrulet5.combat.web.dto.CombatStateDto;
import com.asdru.asdrulet5.enemydata.EnemyDefinitionRegistry;
import com.asdru.asdrulet5.enemydata.EnemyEncounterRegistry;
import com.asdru.asdrulet5.enemydata.domain.EncounterSize;
import com.asdru.asdrulet5.enemydata.domain.EnemyDefinition;
import com.asdru.asdrulet5.inventory.ItemDefinitionRegistry;
import com.asdru.asdrulet5.inventory.domain.ItemDefinition;
import com.asdru.asdrulet5.inventory.domain.ItemPassive;
import com.asdru.asdrulet5.inventory.domain.Loadout;
import com.asdru.asdrulet5.party.domain.Party;
import com.asdru.asdrulet5.party.domain.PartyMember;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CombatService {

    /**
     * The dungeon doesn't have distinct floors yet — every encounter table
     * currently sits on {@code EnemyEncounterRegistry}'s floor 1, so this is
     * the only floor ever drawn from. Kept as its own constant (rather than
     * a bare literal at each call site) so swapping this for the party's
     * actual depth, once floors exist, is a one-line change here.
     */
    private static final int CURRENT_FLOOR = 1;

    /**
     * Percentage bump to a regular enemy's maxHealth/defense for every
     * player beyond {@link EncounterSize#BASELINE_PARTY_SIZE} (solo) — see
     * {@link #scaledStats}. A bigger party doesn't just face more enemies
     * (see {@link EncounterSize#scaledForPartySize}), each one is tougher
     * too, so a lone straggler among them doesn't trivialize the fight.
     */
    private static final double REGULAR_ENEMY_HEALTH_PERCENT_PER_EXTRA_PLAYER = 20;
    private static final double REGULAR_ENEMY_DEFENSE_PERCENT_PER_EXTRA_PLAYER = 10;
    /**
     * Same idea as the regular-enemy constants above, but steeper — a boss
     * always stays exactly 1 (see {@link #toEnemyCombatants}), so it alone
     * has to keep pace with the whole party's added output instead of
     * splitting that extra difficulty across a bigger enemy roster.
     */
    private static final double BOSS_HEALTH_PERCENT_PER_EXTRA_PLAYER = 35;
    private static final double BOSS_DEFENSE_PERCENT_PER_EXTRA_PLAYER = 15;

    private final CombatRepository combatRepository;
    private final ClassDefinitionRegistry classDefinitionRegistry;
    private final SkillTreeRegistry skillTreeRegistry;
    private final EnemyDefinitionRegistry enemyDefinitionRegistry;
    private final EnemyEncounterRegistry enemyEncounterRegistry;
    private final ItemDefinitionRegistry itemDefinitionRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final EnemyActionDelay enemyActionDelay;
    private final SecureRandom random = new SecureRandom();

    /**
     * isBossFight spawns exactly one {@code EnemyDefinitionRegistry.DEFAULT_ENEMY_ID},
     * regardless of party size; otherwise enemies are drawn from
     * {@link EnemyEncounterRegistry}'s current-floor table (2-3 at
     * {@link EncounterSize#BASELINE_PARTY_SIZE}, more for a bigger party —
     * see {@link EncounterSize#scaledForPartySize}), which may repeat the
     * same enemy more than once. Either way, every spawned enemy's own
     * maxHealth/defense is also bumped for a bigger party (see
     * {@link #scaledStats}) — a boss steeper than a regular enemy, since it
     * alone has to keep pace with the whole party instead of splitting that
     * extra difficulty across a bigger roster.
     */
    public CombatStateDto startCombat(Party party, List<PartyMember> members, List<String> turnOrder, boolean isBossFight) {
        List<Combatant> combatants = new ArrayList<>();
        List<PreparedPlayerCombatant> playerCombatants = new ArrayList<>();
        for (String userId : turnOrder) {
            PartyMember member = members.stream()
                    .filter(candidate -> candidate.userId().equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Turn order references unknown member " + userId));
            PreparedPlayerCombatant prepared = toCombatant(member, party);
            playerCombatants.add(prepared);
            combatants.add(prepared.combatant());
        }
        resolveLeaderRelativeMaxHealthBonuses(playerCombatants);

        List<Combatant> enemyCombatants = toEnemyCombatants(isBossFight, members.size());
        combatants.addAll(enemyCombatants);

        List<String> turnSequence = new ArrayList<>(turnOrder);
        enemyCombatants.forEach(enemy -> turnSequence.add(enemy.combatantId()));

        Combat combat = new Combat(party.code(), combatants, turnSequence);
        combatRepository.save(combat);
        return broadcast(combat);
    }

    /**
     * Wires every player combatant's roster in early — before the enemies
     * even exist, let alone the {@link Combat} they'll all eventually share
     * ({@link Combat}'s own constructor will attach the complete roster
     * again once it's built, superseding this one) — so each one's own
     * {@code wearer.healthierThanLeader()} can already see every other
     * member's starting health, then applies whatever leader-relative max
     * health bonus that unlocks (see Mantle of the Usurper). A damage-percent
     * equivalent needs no such step: it's resolved live, per hit, straight
     * off {@link ItemPassive#damagePercentBonus} once the fight is already
     * running — only max health is stuck being a fight-start-only
     * resolution, since it's baked into Stats when the Combatant is built.
     */
    private void resolveLeaderRelativeMaxHealthBonuses(List<PreparedPlayerCombatant> playerCombatants) {
        List<Combatant> roster = playerCombatants.stream().<Combatant>map(PreparedPlayerCombatant::combatant).toList();
        playerCombatants.forEach(prepared -> prepared.combatant().attachRoster(roster));
        for (PreparedPlayerCombatant prepared : playerCombatants) {
            PlayerCombatant combatant = prepared.combatant();
            int percent = prepared.passives().stream().mapToInt(passive -> passive.bonusMaxHealthPercent(combatant)).sum();
            combatant.increaseMaxHealth(combatant.maxHealth() * percent / 100);
        }
    }

    /**
     * A player combatant alongside the resolved {@link ItemPassive}s that
     * built it — kept together only long enough for
     * {@link #resolveLeaderRelativeMaxHealthBonuses} to re-consult those same
     * passives without having to re-derive them from the Combatant's own,
     * type-erased {@code CombatantPassive} list.
     */
    private record PreparedPlayerCombatant(PlayerCombatant combatant, List<ItemPassive> passives) {
    }

    /**
     * A single player action, followed by an auto-end-turn check: once the
     * actor can no longer afford any basic ability and their ultimate isn't
     * charged either, there's nothing left for them to legally do, so their
     * turn ends right here instead of leaving them stuck with a live turn
     * and no legal action to take. That check runs strictly after the
     * ability above has already resolved — including whatever ultimate
     * charge it just granted — so a hit that both drains the last of their
     * stamina and finishes charging their ultimate in the very same call is
     * judged correctly: {@code Combat.hasViableAction} sees the
     * now-ready ultimate and the auto-end is skipped.
     */
    public CombatStateDto useAbility(String code, String actorId, String abilityId, String targetId) {
        Combat combat = getOrThrow(code);
        combat.useAbility(actorId, abilityId, targetId);
        CombatStateDto dto = broadcast(combat);
        if (combat.status() == CombatStatus.IN_PROGRESS && !combat.hasViableAction(actorId)) {
            combat.beginEndTurn(actorId);
            dto = stepUntilAllyTurnOrCombatEnd(combat);
        }
        publishVictoryIfWon(combat);
        return dto;
    }

    /**
     * Unlike a single player action above (one broadcast), ending a turn can
     * cascade through several enemies before control returns to the next
     * ally — see {@link #stepUntilAllyTurnOrCombatEnd}.
     */
    public CombatStateDto endTurn(String code, String actorId) {
        Combat combat = getOrThrow(code);
        combat.beginEndTurn(actorId);
        CombatStateDto dto = stepUntilAllyTurnOrCombatEnd(combat);
        publishVictoryIfWon(combat);
        return dto;
    }

    /**
     * Drives {@code Combat.advanceOneStep} — and so {@code Combat.resolveEnemyTurn}
     * — one enemy at a time, whether the turn ended because a player
     * explicitly clicked "End Turn" or because {@link #useAbility} auto-ended
     * it above. Broadcasting (and pausing) once per enemy, rather than only
     * after a multi-enemy cascade finishes entirely, is what stops every
     * enemy's attack from landing in the very same instant on the frontend:
     * each broadcast represents exactly one actor's action, the same
     * granularity a player's own turn already has.
     */
    private CombatStateDto stepUntilAllyTurnOrCombatEnd(Combat combat) {
        CombatStateDto dto;
        Combat.StepOutcome outcome;
        do {
            outcome = combat.advanceOneStep();
            dto = broadcast(combat);
            if (outcome == Combat.StepOutcome.ENEMY_ACTED) {
                enemyActionDelay.sleep();
            }
        } while (outcome == Combat.StepOutcome.ENEMY_ACTED);
        return dto;
    }

    /**
     * Combat.requireInProgress() rejects any call once status is no longer
     * IN_PROGRESS, so the one call whose own action caused the PARTY_WON
     * transition is the only call that will ever observe it here — safe to
     * publish unconditionally on that observation without double-firing.
     */
    private void publishVictoryIfWon(Combat combat) {
        if (combat.status() == CombatStatus.PARTY_WON) {
            eventPublisher.publishEvent(new CombatVictoryEvent(combat.code()));
        }
    }

    public CombatStateDto getState(String code) {
        return CombatMapper.toDto(getOrThrow(code));
    }

    /**
     * The non-enemy combatants from a resolved fight, in their exact ending
     * state (health, remaining active effects) — used by PartyService right
     * after a victory to carry that state back onto each PartyMember, so it
     * persists into whatever room comes next instead of being lost the
     * moment combat ends.
     */
    public List<Combatant> partyCombatantsFor(String code) {
        return getOrThrow(code).combatants().stream().filter(combatant -> !combatant.enemy()).toList();
    }

    /**
     * Builds a fresh PlayerCombatant with its flat item bonuses only — any
     * leader-relative bonus (see Mantle of the Usurper) is deliberately left
     * unresolved here and applied afterward, once every player combatant in
     * this fight exists and has its starting health set, by
     * {@link #resolveLeaderRelativeMaxHealthBonuses}.
     */
    private PreparedPlayerCombatant toCombatant(PartyMember member, Party party) {
        ClassDefinition definition = classDefinitionRegistry.get(member.characterClass());
        // Reflects whatever the member has actually unlocked in their own
        // class's skill tree (upgraded numbers, evolved/added abilities) —
        // not the raw, unmodified catalog every member of the class starts
        // from. See SkillTreeResolver's own doc.
        List<Ability> effectiveAbilities = SkillTreeResolver.effectiveAbilities(
                definition, skillTreeRegistry.get(member.characterClass()), member.unlockedSkillIds());
        int ultimateChargeThreshold = effectiveAbilities.stream()
                .filter(UltimateAbility.class::isInstance)
                .map(UltimateAbility.class::cast)
                .findFirst()
                .orElseThrow()
                .chargeThreshold();
        List<ItemPassive> passives = resolvePassives(member.loadout());
        int baseMaxHealth = definition.stats().maxHealth();
        Stats stats = new Stats(
                Math.max(1, baseMaxHealth + sumBonus(passives, ItemPassive::bonusMaxHealth)),
                Math.max(0, definition.stats().defense() + sumBonus(passives, ItemPassive::bonusDefense)),
                Math.max(0, definition.stats().maxStamina() + sumBonus(passives, ItemPassive::bonusMaxStamina)));
        PlayerCombatant combatant = new PlayerCombatant(
                member.userId(), member.displayName(), member.characterClass(), stats,
                ultimateChargeThreshold, effectiveAbilities, passives, party);
        // Both carried over from whatever the member's last room left them
        // with — a wheel/loot roll, or the ending state of their last fight
        // (see PartyService.syncMembersAfterCombat) — so a fresh Combatant
        // always starts from where the party actually left off. Set before
        // any leader-relative max health bonus, deliberately: that bonus
        // still needs to compare against this member's own pre-bonus
        // starting health, exactly as if it were never carried over.
        if (member.currentHealth() != null) {
            combatant.setStartingHealth(member.currentHealth());
        }
        member.pendingEffects().forEach(combatant::addActiveEffect);
        return new PreparedPlayerCombatant(combatant, passives);
    }

    private List<ItemPassive> resolvePassives(Loadout loadout) {
        return loadout.equippedItemIds().stream()
                .map(itemDefinitionRegistry::get)
                .map(ItemDefinition::passive)
                .toList();
    }

    /**
     * Summed here rather than clamped per-item, so the floor (applied by the
     * caller) only kicks in once against the combined total — a trade-off
     * item's negative bonus could otherwise combine with a low base stat to
     * go to zero or below.
     */
    private int sumBonus(List<ItemPassive> passives, ToIntFunction<ItemPassive> bonus) {
        return passives.stream().mapToInt(bonus).sum();
    }

    /**
     * A boss fight is exactly one {@code DEFAULT_ENEMY_ID} — always exactly
     * 1, regardless of partySize (see {@link #scaledStats} for how a boss
     * keeps pace with a bigger party instead). A regular fight rolls
     * {@link EnemyEncounterRegistry}'s current-floor table (itself scaled
     * for partySize), which can repeat the same enemy — when it does, each
     * repeat gets a " 1"/" 2"/... suffix on its display name so e.g. two
     * Cave Rats are distinguishable in the UI (their combatant ids,
     * "enemy-1"/"enemy-2"/..., already keep them distinct to the engine
     * either way).
     */
    private List<Combatant> toEnemyCombatants(boolean isBossFight, int partySize) {
        List<String> enemyDefinitionIds = isBossFight
                ? List.of(EnemyDefinitionRegistry.DEFAULT_ENEMY_ID)
                : enemyEncounterRegistry.forFloor(CURRENT_FLOOR).roll(random, partySize);
        Map<String, Long> occurrences = enemyDefinitionIds.stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        Map<String, Integer> seenSoFar = new HashMap<>();
        List<Combatant> enemies = new ArrayList<>();
        for (int i = 0; i < enemyDefinitionIds.size(); i++) {
            String definitionId = enemyDefinitionIds.get(i);
            EnemyDefinition definition = enemyDefinitionRegistry.get(definitionId);
            String displayName = occurrences.get(definitionId) > 1
                    ? definition.displayName() + " " + seenSoFar.merge(definitionId, 1, Integer::sum)
                    : definition.displayName();
            enemies.add(new EnemyCombatant(
                    "enemy-" + (i + 1), displayName, definitionId, scaledStats(definition.stats(), partySize, isBossFight),
                    0, definition.abilities(), definition.passives()));
        }
        return enemies;
    }

    /**
     * Bumps maxHealth/defense by a percentage per player beyond
     * {@link EncounterSize#BASELINE_PARTY_SIZE} — a different (steeper)
     * pair of percentages for a boss than a regular enemy, see the constants
     * above. maxStamina is left untouched: it's already each enemy's own
     * fixed "how many big hits can I afford before falling back to my basic
     * attack" budget (see EnemyDefinitionRegistry), unrelated to party size.
     */
    private Stats scaledStats(Stats base, int partySize, boolean isBossFight) {
        int extraPlayers = Math.max(0, partySize - EncounterSize.BASELINE_PARTY_SIZE);
        double healthPercent = isBossFight ? BOSS_HEALTH_PERCENT_PER_EXTRA_PLAYER : REGULAR_ENEMY_HEALTH_PERCENT_PER_EXTRA_PLAYER;
        double defensePercent = isBossFight ? BOSS_DEFENSE_PERCENT_PER_EXTRA_PLAYER : REGULAR_ENEMY_DEFENSE_PERCENT_PER_EXTRA_PLAYER;
        int scaledHealth = (int) Math.round(base.maxHealth() * (1 + extraPlayers * healthPercent / 100.0));
        int scaledDefense = (int) Math.round(base.defense() * (1 + extraPlayers * defensePercent / 100.0));
        return new Stats(scaledHealth, scaledDefense, base.maxStamina());
    }

    private Combat getOrThrow(String code) {
        return combatRepository.findByCode(code).orElseThrow(() -> new CombatNotFoundException(code));
    }

    private CombatStateDto broadcast(Combat combat) {
        CombatStateDto dto = CombatMapper.toDto(combat);
        messagingTemplate.convertAndSend("/topic/party/" + combat.code() + "/combat", dto);
        return dto;
    }
}
