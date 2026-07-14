package com.asdru.asdrulet5.dungeon.web;

import com.asdru.asdrulet5.dungeon.DungeonService;
import com.asdru.asdrulet5.dungeon.web.dto.DungeonStateDto;
import com.asdru.asdrulet5.dungeon.web.dto.SelectNodeRequest;
import com.asdru.asdrulet5.party.exception.DevToolsDisabledException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * Dev-only tooling mirroring CombatDevController: lets a locally-simulated
 * leader identity (dev "quick game", session-less) move through the dungeon
 * without a Google session. Gated behind app.dev-tools.enabled.
 */
@RestController
@RequestMapping("/api/parties/{code}/dungeon/dev/{memberId}")
@RequiredArgsConstructor
public class DungeonDevController {

    private final DungeonService dungeonService;

    @Value("${app.dev-tools.enabled:false}")
    private boolean devToolsEnabled;

    @PostMapping("/select")
    public DungeonStateDto selectAsMember(@PathVariable String code,
                                          @PathVariable String memberId,
                                          @Valid @RequestBody SelectNodeRequest request) {
        requireDevToolsEnabled();
        return dungeonService.select(code.toUpperCase(), memberId, request.nodeId());
    }

    private void requireDevToolsEnabled() {
        if (!devToolsEnabled) {
            throw new DevToolsDisabledException();
        }
    }
}
