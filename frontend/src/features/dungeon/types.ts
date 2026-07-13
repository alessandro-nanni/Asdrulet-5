export type RoomType = 'START' | 'FIGHT' | 'LOOT' | 'MERCHANT' | 'BOSS'

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
  currentNodeId: string
  availableNodeIds: string[]
  visitedNodeIds: string[]
}
