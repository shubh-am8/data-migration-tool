package com.migration.marketplace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.migration.connectors.SchemaInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabControllerTest {

    @Mock
    private LabIntrospectionService labIntrospection;

    @InjectMocks
    private LabController controller;

    @Test
    void listsLabSchemas() throws Exception {
        when(labIntrospection.listSchemas()).thenReturn(new SchemaInfo(java.util.List.of("app", "test")));
        assertEquals(2, controller.schemas().schemas().size());
    }
}
