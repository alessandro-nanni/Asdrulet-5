package com.asdru.asdrulet5.party.web;

import com.asdru.asdrulet5.party.PartyService;
import com.asdru.asdrulet5.party.exception.DevToolsDisabledException;
import com.asdru.asdrulet5.party.web.dto.AddFakeMembersRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class PartyDevControllerTest {

    @Test
    void devEndpointsAreDisabledByDefault() {
        PartyService partyService = mock(PartyService.class);
        PartyDevController controller = new PartyDevController(partyService);

        assertThatThrownBy(() -> controller.addFakeMembers("ABC123", new AddFakeMembersRequest(3)))
                .isInstanceOf(DevToolsDisabledException.class);
        verifyNoInteractions(partyService);
    }
}
