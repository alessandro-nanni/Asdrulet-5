package com.asdru.asdrulet5.party.web;

import com.asdru.asdrulet5.party.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class PartyExceptionHandler {

    @ExceptionHandler(PartyNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(PartyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotPartyLeaderException.class)
    public ResponseEntity<Map<String, String>> handleNotLeader(NotPartyLeaderException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotPartyMemberException.class)
    public ResponseEntity<Map<String, String>> handleNotMember(NotPartyMemberException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTurnOrderException.class)
    public ResponseEntity<Map<String, String>> handleInvalidTurnOrder(InvalidTurnOrderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MissingClassSelectionException.class)
    public ResponseEntity<Map<String, String>> handleMissingClassSelection(MissingClassSelectionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ClassAlreadyTakenException.class)
    public ResponseEntity<Map<String, String>> handleClassAlreadyTaken(ClassAlreadyTakenException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DevToolsDisabledException.class)
    public ResponseEntity<Void> handleDevToolsDisabled() {
        // 404 rather than 403, so the feature's existence isn't revealed when disabled (e.g. in prod).
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(NotAFakeMemberException.class)
    public ResponseEntity<Map<String, String>> handleNotAFakeMember(NotAFakeMemberException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(PartyFullException.class)
    public ResponseEntity<Map<String, String>> handlePartyFull(PartyFullException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidStorageIndexException.class)
    public ResponseEntity<Map<String, String>> handleInvalidStorageIndex(InvalidStorageIndexException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(EmptyStorageSlotException.class)
    public ResponseEntity<Map<String, String>> handleEmptyStorageSlot(EmptyStorageSlotException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ItemNotConsumableException.class)
    public ResponseEntity<Map<String, String>> handleItemNotConsumable(ItemNotConsumableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotInMysteryRoomException.class)
    public ResponseEntity<Map<String, String>> handleNotInMysteryRoom(NotInMysteryRoomException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AlreadySpunWheelException.class)
    public ResponseEntity<Map<String, String>> handleAlreadySpunWheel(AlreadySpunWheelException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotYetSpunWheelException.class)
    public ResponseEntity<Map<String, String>> handleNotYetSpunWheel(NotYetSpunWheelException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotYourWheelTurnException.class)
    public ResponseEntity<Map<String, String>> handleNotYourWheelTurn(NotYourWheelTurnException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotInMerchantRoomException.class)
    public ResponseEntity<Map<String, String>> handleNotInMerchantRoom(NotInMerchantRoomException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ItemNotInShopException.class)
    public ResponseEntity<Map<String, String>> handleItemNotInShop(ItemNotInShopException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientCoinsException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientCoins(InsufficientCoinsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotInLootRoomException.class)
    public ResponseEntity<Map<String, String>> handleNotInLootRoom(NotInLootRoomException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AlreadyLootedException.class)
    public ResponseEntity<Map<String, String>> handleAlreadyLooted(AlreadyLootedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotYetLootedException.class)
    public ResponseEntity<Map<String, String>> handleNotYetLooted(NotYetLootedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotYourLootTurnException.class)
    public ResponseEntity<Map<String, String>> handleNotYourLootTurn(NotYourLootTurnException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientManaException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientMana(InsufficientManaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SkillAlreadyUnlockedException.class)
    public ResponseEntity<Map<String, String>> handleSkillAlreadyUnlocked(SkillAlreadyUnlockedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SkillPrerequisiteNotMetException.class)
    public ResponseEntity<Map<String, String>> handleSkillPrerequisiteNotMet(SkillPrerequisiteNotMetException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
