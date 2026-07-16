package com.asdru.asdrulet5.classdata.web;

import com.asdru.asdrulet5.classdata.SkillTreeRegistry;
import com.asdru.asdrulet5.classdata.web.dto.SkillTreeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/skill-trees")
@RequiredArgsConstructor
public class SkillTreeController {

    private final SkillTreeRegistry registry;

    @GetMapping
    public List<SkillTreeDto> getAllSkillTrees() {
        return registry.all().stream().map(SkillTreeMapper::toDto).toList();
    }
}
