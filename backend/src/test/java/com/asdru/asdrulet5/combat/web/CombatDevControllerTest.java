package com.asdru.asdrulet5.combat.web;

import com.asdru.asdrulet5.combat.CombatService;
import com.asdru.asdrulet5.combat.web.dto.UseAbilityRequest;
import com.asdru.asdrulet5.party.exception.DevToolsDisabledException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class CombatDevControllerTest {

    @Test
    void devEndpointsAreDisabledByDefault() {
        CombatService combatService = mock(CombatService.class);
        CombatDevController controller = new CombatDevController(combatService);

        assertThatThrownBy(() -> controller.useAbilityAsFakeMember("ABC123", "bot-1", new UseAbilityRequest("ability", "target")))
                .isInstanceOf(DevToolsDisabledException.class);
        assertThatThrownBy(() -> controller.endTurnAsFakeMember("ABC123", "bot-1"))
                .isInstanceOf(DevToolsDisabledException.class);
        verifyNoInteractions(combatService);
    }
}
