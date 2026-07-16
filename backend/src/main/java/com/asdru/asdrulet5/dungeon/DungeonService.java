package com.asdru.asdrulet5.dungeon;

import com.asdru.asdrulet5.dungeon.domain.Dungeon;
import com.asdru.asdrulet5.dungeon.domain.DungeonNode;
import com.asdru.asdrulet5.dungeon.domain.RoomType;
import com.asdru.asdrulet5.dungeon.exception.DungeonNotFoundException;
import com.asdru.asdrulet5.dungeon.web.DungeonMapper;
import com.asdru.asdrulet5.dungeon.web.dto.DungeonStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DungeonService {

    private final DungeonRepository dungeonRepository;
    private final DungeonGenerator dungeonGenerator;
    private final SimpMessagingTemplate messagingTemplate;

    public DungeonStateDto startDungeon(String code, String leaderId) {
        List<DungeonNode> nodes = dungeonGenerator.generate();
        String startNodeId = nodes.stream()
                .filter(node -> node.roomType() == RoomType.START)
                .findFirst()
                .orElseThrow()
                .id();
        Dungeon dungeon = new Dungeon(code, leaderId, nodes, startNodeId);
        dungeonRepository.save(dungeon);
        return broadcast(dungeon);
    }

    public DungeonStateDto select(String code, String requesterId, String targetNodeId) {
        Dungeon dungeon = getOrThrow(code);
        dungeon.select(requesterId, targetNodeId);
        return broadcast(dungeon);
    }

    /**
     * Commits to whatever's currently selected and returns its room type, so
     * the caller (PartyService) can decide whether to start combat or
     * auto-clear immediately.
     */
    public RoomType enterNode(String code, String requesterId) {
        Dungeon dungeon = getOrThrow(code);
        dungeon.enter(requesterId);
        broadcast(dungeon);
        return dungeon.currentRoomType();
    }

    public DungeonStateDto clearEnteredNode(String code) {
        Dungeon dungeon = getOrThrow(code);
        dungeon.clearEnteredNode();
        return broadcast(dungeon);
    }

    /**
     * Null if nothing is currently entered — callers decide what that means for them.
     */
    public RoomType enteredRoomType(String code) {
        Dungeon dungeon = getOrThrow(code);
        return dungeon.enteredNodeId() != null ? dungeon.currentRoomType() : null;
    }

    public DungeonStateDto getState(String code) {
        return DungeonMapper.toDto(getOrThrow(code));
    }

    /**
     * See {@link Dungeon#currentLayer()} — used by PartyService to scale a
     * consumed healing potion's effect with dungeon depth.
     */
    public int currentLayer(String code) {
        return getOrThrow(code).currentLayer();
    }

    private Dungeon getOrThrow(String code) {
        return dungeonRepository.findByCode(code).orElseThrow(() -> new DungeonNotFoundException(code));
    }

    private DungeonStateDto broadcast(Dungeon dungeon) {
        DungeonStateDto dto = DungeonMapper.toDto(dungeon);
        messagingTemplate.convertAndSend("/topic/party/" + dungeon.code() + "/dungeon", dto);
        return dto;
    }
}
