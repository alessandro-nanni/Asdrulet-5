package com.asdru.asdrulet5.party.web;

import com.asdru.asdrulet5.auth.web.AuthenticatedUserMapper;
import com.asdru.asdrulet5.party.PartyService;
import com.asdru.asdrulet5.party.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parties")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService partyService;

    @PostMapping
    public ResponseEntity<PartyStateDto> createParty(@AuthenticationPrincipal OidcUser principal,
                                                     @Valid @RequestBody CreatePartyRequest request) {
        PartyStateDto dto = partyService.createParty(AuthenticatedUserMapper.from(principal), request.displayName());
        return ResponseEntity.status(201).body(dto);
    }

    @GetMapping("/{code}")
    public PartyStateDto getParty(@PathVariable String code) {
        return partyService.getState(code.toUpperCase());
    }

    @PostMapping("/{code}/join")
    public PartyStateDto joinParty(@PathVariable String code,
                                   @AuthenticationPrincipal OidcUser principal,
                                   @Valid @RequestBody JoinPartyRequest request) {
        return partyService.joinParty(code.toUpperCase(), AuthenticatedUserMapper.from(principal), request.displayName());
    }

    @PostMapping("/{code}/class")
    public PartyStateDto selectClass(@PathVariable String code,
                                     @AuthenticationPrincipal OidcUser principal,
                                     @Valid @RequestBody SelectClassRequest request) {
        return partyService.selectClass(code.toUpperCase(), AuthenticatedUserMapper.from(principal), request.characterClass());
    }

    @PostMapping("/{code}/start")
    public PartyStateDto startGame(@PathVariable String code,
                                   @AuthenticationPrincipal OidcUser principal,
                                   @Valid @RequestBody StartGameRequest request) {
        return partyService.startGame(code.toUpperCase(), AuthenticatedUserMapper.from(principal), request.memberIds());
    }
}
