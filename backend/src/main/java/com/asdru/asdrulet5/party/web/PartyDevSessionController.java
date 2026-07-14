package com.asdru.asdrulet5.party.web;

import com.asdru.asdrulet5.auth.AuthenticatedUser;
import com.asdru.asdrulet5.party.PartyService;
import com.asdru.asdrulet5.party.exception.DevToolsDisabledException;
import com.asdru.asdrulet5.party.web.dto.CreatePartyRequest;
import com.asdru.asdrulet5.party.web.dto.EquipItemRequest;
import com.asdru.asdrulet5.party.web.dto.PartyStateDto;
import com.asdru.asdrulet5.party.web.dto.SelectClassRequest;
import com.asdru.asdrulet5.party.web.dto.StartGameRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Dev-only tooling that lets the "Quick game" tool run entirely without a
 * Google session: it creates a party led by a synthetic, session-less
 * identity and lets that same identity drive class selection and game start,
 * mirroring what {@link PartyController} does for a real {@code OidcUser}.
 * Gated behind app.dev-tools.enabled (default false); never enable this in a
 * public deployment.
 */
@RestController
@RequestMapping("/api/parties/dev")
@RequiredArgsConstructor
public class PartyDevSessionController {

    private final PartyService partyService;

    @Value("${app.dev-tools.enabled:false}")
    private boolean devToolsEnabled;

    @PostMapping
    public ResponseEntity<PartyStateDto> createParty(@Valid @RequestBody CreatePartyRequest request) {
        requireDevToolsEnabled();
        String syntheticId = "dev-" + UUID.randomUUID();
        AuthenticatedUser leader = new AuthenticatedUser(syntheticId, request.displayName(), null);
        PartyStateDto dto = partyService.createParty(leader, request.displayName());
        return ResponseEntity.status(201).body(dto);
    }

    @PostMapping("/{code}/{memberId}/class")
    public PartyStateDto selectClass(@PathVariable String code,
                                     @PathVariable String memberId,
                                     @Valid @RequestBody SelectClassRequest request) {
        requireDevToolsEnabled();
        return partyService.selectClassAsMember(code.toUpperCase(), memberId, request.characterClass());
    }

    @PostMapping("/{code}/{memberId}/start")
    public PartyStateDto startGame(@PathVariable String code,
                                   @PathVariable String memberId,
                                   @Valid @RequestBody StartGameRequest request) {
        requireDevToolsEnabled();
        return partyService.startGame(code.toUpperCase(), memberId, request.memberIds());
    }

    @PostMapping("/{code}/{memberId}/enter-room")
    public PartyStateDto enterRoom(@PathVariable String code, @PathVariable String memberId) {
        requireDevToolsEnabled();
        return partyService.enterRoom(code.toUpperCase(), memberId);
    }

    @PostMapping("/{code}/{memberId}/inventory/equip")
    public PartyStateDto equipItem(@PathVariable String code,
                                   @PathVariable String memberId,
                                   @Valid @RequestBody EquipItemRequest request) {
        requireDevToolsEnabled();
        return partyService.equipItem(code.toUpperCase(), memberId, request.itemId());
    }

    private void requireDevToolsEnabled() {
        if (!devToolsEnabled) {
            throw new DevToolsDisabledException();
        }
    }
}
