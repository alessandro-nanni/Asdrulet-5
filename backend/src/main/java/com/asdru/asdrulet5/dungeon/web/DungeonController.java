package com.asdru.asdrulet5.dungeon.web;

import com.asdru.asdrulet5.dungeon.DungeonService;
import com.asdru.asdrulet5.dungeon.web.dto.DungeonStateDto;
import com.asdru.asdrulet5.dungeon.web.dto.SelectNodeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/{memberId}/select")
    public DungeonStateDto select(@PathVariable String code,
                                  @PathVariable String memberId,
                                  @Valid @RequestBody SelectNodeRequest request) {
        return dungeonService.select(code.toUpperCase(), memberId, request.nodeId());
    }
}
