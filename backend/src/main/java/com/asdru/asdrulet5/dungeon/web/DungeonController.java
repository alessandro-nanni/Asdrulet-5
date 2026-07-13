package com.asdru.asdrulet5.dungeon.web;

import com.asdru.asdrulet5.auth.web.AuthenticatedUserMapper;
import com.asdru.asdrulet5.dungeon.DungeonService;
import com.asdru.asdrulet5.dungeon.web.dto.DungeonStateDto;
import com.asdru.asdrulet5.dungeon.web.dto.MoveToNodeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parties/{code}/dungeon")
@RequiredArgsConstructor
public class DungeonController {

    private final DungeonService dungeonService;

    @GetMapping
    public DungeonStateDto getDungeon(@PathVariable String code) {
        return dungeonService.getState(code.toUpperCase());
    }

    @PostMapping("/move")
    public DungeonStateDto moveTo(@PathVariable String code,
                                  @AuthenticationPrincipal OidcUser principal,
                                  @Valid @RequestBody MoveToNodeRequest request) {
        String actorId = AuthenticatedUserMapper.from(principal).id();
        return dungeonService.moveTo(code.toUpperCase(), actorId, request.nodeId());
    }
}
