package com.asdru.asdrulet5.party.web;

import com.asdru.asdrulet5.inventory.domain.Loadout;
import com.asdru.asdrulet5.party.domain.Party;
import com.asdru.asdrulet5.party.domain.PartyMember;
import com.asdru.asdrulet5.party.web.dto.LoadoutDto;
import com.asdru.asdrulet5.party.web.dto.PartyMemberDto;
import com.asdru.asdrulet5.party.web.dto.PartyStateDto;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PartyMapper {

    public PartyStateDto toDto(Party party) {
        return new PartyStateDto(
                party.code(),
                party.leaderId(),
                party.members().stream().map(PartyMapper::toDto).toList(),
                party.turnOrder(),
                party.status()
        );
    }

    private PartyMemberDto toDto(PartyMember member) {
        return new PartyMemberDto(
                member.userId(),
                member.displayName(),
                member.avatarUrl(),
                member.characterClass(),
                member.leader(),
                member.bot(),
                toDto(member.loadout())
        );
    }

    private LoadoutDto toDto(Loadout loadout) {
        return new LoadoutDto(loadout.weaponItemId(), loadout.chestplateItemId(), loadout.trinketItemId());
    }
}
