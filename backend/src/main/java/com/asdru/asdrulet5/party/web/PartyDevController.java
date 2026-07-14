package com.asdru.asdrulet5.party.web;

import com.asdru.asdrulet5.party.PartyService;
import com.asdru.asdrulet5.party.exception.DevToolsDisabledException;
import com.asdru.asdrulet5.party.web.dto.AddFakeMembersRequest;
import com.asdru.asdrulet5.party.web.dto.PartyStateDto;
import com.asdru.asdrulet5.party.web.dto.SelectClassRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * Dev-only tooling to test multi-member party flows from a single browser,
 * without needing separate devices for every player. Gated behind
 * app.dev-tools.enabled (default false); never enable this in a public
 * deployment.
 */
@RestController
@RequestMapping("/api/parties/{code}/dev")
@RequiredArgsConstructor
public class PartyDevController {

    private final PartyService partyService;

    @Value("${app.dev-tools.enabled:false}")
    private boolean devToolsEnabled;

    @PostMapping("/fake-members")
    public PartyStateDto addFakeMembers(@PathVariable String code, @Valid @RequestBody AddFakeMembersRequest request) {
        requireDevToolsEnabled();
        return partyService.addFakeMembers(code.toUpperCase(), request.count());
    }

    @PostMapping("/{memberId}/class")
    public PartyStateDto selectClassAsFakeMember(@PathVariable String code,
                                                 @PathVariable String memberId,
                                                 @Valid @RequestBody SelectClassRequest request) {
        requireDevToolsEnabled();
        return partyService.selectClassAsFakeMember(code.toUpperCase(), memberId, request.characterClass());
    }

    private void requireDevToolsEnabled() {
        if (!devToolsEnabled) {
            throw new DevToolsDisabledException();
        }
    }
}
