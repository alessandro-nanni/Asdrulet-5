package com.asdru.asdrulet5.combat.web;

import com.asdru.asdrulet5.combat.CombatService;
import com.asdru.asdrulet5.combat.web.dto.CombatStateDto;
import com.asdru.asdrulet5.combat.web.dto.UseAbilityRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/{memberId}/actions")
    public CombatStateDto useAbility(@PathVariable String code,
                                     @PathVariable String memberId,
                                     @Valid @RequestBody UseAbilityRequest request) {
        return combatService.useAbility(code.toUpperCase(), memberId, request.abilityId(), request.targetId());
    }

    @PostMapping("/{memberId}/end-turn")
    public CombatStateDto endTurn(@PathVariable String code, @PathVariable String memberId) {
        return combatService.endTurn(code.toUpperCase(), memberId);
    }
}
