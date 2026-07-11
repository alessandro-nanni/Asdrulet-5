package com.asdru.asdrulet5.party.web;

import com.asdru.asdrulet5.auth.web.AuthenticatedUserMapper;
import com.asdru.asdrulet5.party.PartyService;
import com.asdru.asdrulet5.party.web.dto.PartyStateDto;
import com.asdru.asdrulet5.party.web.dto.SelectClassRequest;
import com.asdru.asdrulet5.party.web.dto.SetTurnOrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parties")
public class PartyController {

    private final PartyService partyService;

    public PartyController(PartyService partyService) {
        this.partyService = partyService;
    }

    @PostMapping
    public ResponseEntity<PartyStateDto> createParty(@AuthenticationPrincipal OidcUser principal) {
        PartyStateDto dto = partyService.createParty(AuthenticatedUserMapper.from(principal));
        return ResponseEntity.status(201).body(dto);
    }

    @GetMapping("/{code}")
    public PartyStateDto getParty(@PathVariable String code) {
        return partyService.getState(code.toUpperCase());
    }

    @PostMapping("/{code}/join")
    public PartyStateDto joinParty(@PathVariable String code, @AuthenticationPrincipal OidcUser principal) {
        return partyService.joinParty(code.toUpperCase(), AuthenticatedUserMapper.from(principal));
    }

    @PostMapping("/{code}/class")
    public PartyStateDto selectClass(@PathVariable String code,
                                      @AuthenticationPrincipal OidcUser principal,
                                      @Valid @RequestBody SelectClassRequest request) {
        return partyService.selectClass(code.toUpperCase(), AuthenticatedUserMapper.from(principal), request.characterClass());
    }

    @PostMapping("/{code}/turn-order")
    public PartyStateDto setTurnOrder(@PathVariable String code,
                                       @AuthenticationPrincipal OidcUser principal,
                                       @Valid @RequestBody SetTurnOrderRequest request) {
        return partyService.setTurnOrder(code.toUpperCase(), AuthenticatedUserMapper.from(principal), request.memberIds());
    }
}
