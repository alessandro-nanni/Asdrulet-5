package com.asdru.asdrulet5.combat.web;

import com.asdru.asdrulet5.auth.web.AuthenticatedUserMapper;
import com.asdru.asdrulet5.combat.CombatService;
import com.asdru.asdrulet5.combat.web.dto.CombatStateDto;
import com.asdru.asdrulet5.combat.web.dto.UseAbilityRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parties/{code}/combat")
@RequiredArgsConstructor
public class CombatController {

    private final CombatService combatService;

    @GetMapping
    public CombatStateDto getCombat(@PathVariable String code) {
        return combatService.getState(code.toUpperCase());
    }

    @PostMapping("/actions")
    public CombatStateDto useAbility(@PathVariable String code,
                                     @AuthenticationPrincipal OidcUser principal,
                                     @Valid @RequestBody UseAbilityRequest request) {
        String actorId = AuthenticatedUserMapper.from(principal).id();
        return combatService.useAbility(code.toUpperCase(), actorId, request.abilityId(), request.targetId());
    }

    @PostMapping("/end-turn")
    public CombatStateDto endTurn(@PathVariable String code, @AuthenticationPrincipal OidcUser principal) {
        String actorId = AuthenticatedUserMapper.from(principal).id();
        return combatService.endTurn(code.toUpperCase(), actorId);
    }
}
