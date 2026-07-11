package com.asdru.asdrulet5.combat.web;

import com.asdru.asdrulet5.combat.CombatService;
import com.asdru.asdrulet5.combat.web.dto.CombatStateDto;
import com.asdru.asdrulet5.combat.web.dto.UseAbilityRequest;
import com.asdru.asdrulet5.party.exception.DevToolsDisabledException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only tooling mirroring PartyDevController: lets a locally-simulated bot
 * act on its own turn, so multi-member combat flows can be tested from a
 * single logged-in session. Gated behind app.dev-tools.enabled.
 */
@RestController
@RequestMapping("/api/parties/{code}/combat/dev/{memberId}")
@RequiredArgsConstructor
public class CombatDevController {

    private final CombatService combatService;

    @Value("${app.dev-tools.enabled:false}")
    private boolean devToolsEnabled;

    @PostMapping("/actions")
    public CombatStateDto useAbilityAsFakeMember(@PathVariable String code,
                                                  @PathVariable String memberId,
                                                  @Valid @RequestBody UseAbilityRequest request) {
        requireDevToolsEnabled();
        return combatService.useAbility(code.toUpperCase(), memberId, request.abilityId(), request.targetId());
    }

    @PostMapping("/end-turn")
    public CombatStateDto endTurnAsFakeMember(@PathVariable String code, @PathVariable String memberId) {
        requireDevToolsEnabled();
        return combatService.endTurn(code.toUpperCase(), memberId);
    }

    private void requireDevToolsEnabled() {
        if (!devToolsEnabled) {
            throw new DevToolsDisabledException();
        }
    }
}
