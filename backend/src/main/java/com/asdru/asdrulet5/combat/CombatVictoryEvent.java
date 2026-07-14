package com.asdru.asdrulet5.combat;

/**
 * Published exactly once, by whichever CombatService call causes a Combat's
 * status to transition into PARTY_WON — Combat itself has no way to notify
 * anyone, and CombatService can't depend on PartyService directly without a
 * circular bean dependency (PartyService already depends on CombatService to
 * start combat), so this event is the decoupling point: PartyService listens
 * for it to flip the party back to DUNGEON and clear the dungeon node.
 */
public record CombatVictoryEvent(String partyCode) {
}
