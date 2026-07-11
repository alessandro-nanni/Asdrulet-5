package com.asdru.asdrulet5.classdata.web;

import com.asdru.asdrulet5.classdata.ClassDefinitionRegistry;
import com.asdru.asdrulet5.classdata.web.dto.ClassDefinitionDto;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassDefinitionController {

    private final ClassDefinitionRegistry registry;

    @GetMapping
    public List<ClassDefinitionDto> getAllClasses() {
        return registry.all().stream().map(ClassDefinitionMapper::toDto).toList();
    }

    @GetMapping("/{characterClass}")
    public ClassDefinitionDto getClass(@PathVariable CharacterClass characterClass) {
        return ClassDefinitionMapper.toDto(registry.get(characterClass));
    }
}
