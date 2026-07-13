package com.asdru.asdrulet5.inventory.web;

import com.asdru.asdrulet5.inventory.ItemDefinitionRegistry;
import com.asdru.asdrulet5.inventory.web.dto.ItemDefinitionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemDefinitionController {

    private final ItemDefinitionRegistry registry;

    @GetMapping
    public List<ItemDefinitionDto> getAllItems() {
        return registry.all().stream().map(ItemDefinitionMapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public ItemDefinitionDto getItem(@PathVariable String id) {
        return ItemDefinitionMapper.toDto(registry.get(id));
    }
}
