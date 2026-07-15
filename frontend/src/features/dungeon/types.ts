export type RoomType = 'START' | 'FIGHT' | 'LOOT' | 'MERCHANT' | 'MYSTERY' | 'BOSS'

export interface DungeonNode {
    id: string
    roomType: RoomType
    layer: number
    indexInLayer: number
    nextNodeIds: string[]
}

export interface DungeonState {
    code: string
    nodes: DungeonNode[]
    homeNodeId: string
    currentNodeId: string
    enteredNodeId: string | null
    availableNodeIds: string[]
    clearedNodeIds: string[]
}
