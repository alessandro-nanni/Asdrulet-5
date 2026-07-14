package com.asdru.asdrulet5.party.web;

import com.asdru.asdrulet5.auth.AuthenticatedUser;
import com.asdru.asdrulet5.party.PartyService;
import com.asdru.asdrulet5.party.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * There's no login: {@code id} in every request is a string the client
 * generated once (see the frontend's local identity store) and just keeps
 * presenting — same trust model the old dev-only "quick game" tooling used,
 * now the only one. Party/Dungeon/Combat enforce who's allowed to do what
 * (e.g. only the leader can start the game) purely by comparing these ids.
 */
@RestController
@RequestMapping("/api/parties")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService partyService;

    @PostMapping
    public ResponseEntity<PartyStateDto> createParty(@Valid @RequestBody CreatePartyRequest request) {
        AuthenticatedUser leader = new AuthenticatedUser(request.id(), request.displayName(), request.avatarUrl());
        PartyStateDto dto = partyService.createParty(leader);
        return ResponseEntity.status(201).body(dto);
    }

    @GetMapping("/{code}")
    public PartyStateDto getParty(@PathVariable String code) {
        return partyService.getState(code.toUpperCase());
    }

    @PostMapping("/{code}/{memberId}/join")
    public PartyStateDto joinParty(@PathVariable String code,
                                   @PathVariable String memberId,
                                   @Valid @RequestBody JoinPartyRequest request) {
        AuthenticatedUser user = new AuthenticatedUser(memberId, request.displayName(), request.avatarUrl());
        return partyService.joinParty(code.toUpperCase(), user);
    }

    @PostMapping("/{code}/{memberId}/class")
    public PartyStateDto selectClass(@PathVariable String code,
                                     @PathVariable String memberId,
                                     @Valid @RequestBody SelectClassRequest request) {
        return partyService.selectClass(code.toUpperCase(), memberId, request.characterClass());
    }

    @PostMapping("/{code}/{memberId}/start")
    public PartyStateDto startGame(@PathVariable String code,
                                   @PathVariable String memberId,
                                   @Valid @RequestBody StartGameRequest request) {
        return partyService.startGame(code.toUpperCase(), memberId, request.memberIds());
    }

    @PostMapping("/{code}/{memberId}/enter-room")
    public PartyStateDto enterRoom(@PathVariable String code, @PathVariable String memberId) {
        return partyService.enterRoom(code.toUpperCase(), memberId);
    }

    @PostMapping("/{code}/{memberId}/inventory/equip")
    public PartyStateDto equipItem(@PathVariable String code,
                                   @PathVariable String memberId,
                                   @Valid @RequestBody EquipItemRequest request) {
        return partyService.equipItem(code.toUpperCase(), memberId, request.itemId());
    }

    @PostMapping("/{code}/{memberId}/inventory/equip-from-storage")
    public PartyStateDto equipFromStorage(@PathVariable String code,
                                          @PathVariable String memberId,
                                          @Valid @RequestBody EquipFromStorageRequest request) {
        return partyService.equipFromStorage(code.toUpperCase(), memberId, request.storageIndex());
    }
}
