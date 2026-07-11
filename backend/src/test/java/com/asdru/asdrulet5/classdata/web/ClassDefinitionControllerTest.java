package com.asdru.asdrulet5.classdata.web;

import com.asdru.asdrulet5.classdata.ClassDefinitionRegistry;
import com.asdru.asdrulet5.classdata.web.dto.ClassDefinitionDto;
import com.asdru.asdrulet5.party.domain.CharacterClass;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClassDefinitionControllerTest {

    private final ClassDefinitionController controller = new ClassDefinitionController(new ClassDefinitionRegistry());

    @Test
    void getAllClassesReturnsFourDefinitions() {
        List<ClassDefinitionDto> classes = controller.getAllClasses();

        assertThat(classes).hasSize(4);
    }

    @Test
    void getClassReturnsMatchingDefinition() {
        ClassDefinitionDto dto = controller.getClass(CharacterClass.MAGE);

        assertThat(dto.characterClass()).isEqualTo(CharacterClass.MAGE);
        assertThat(dto.abilities()).hasSize(3);
    }
}
