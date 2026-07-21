package com.migration.marketplace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.migration.connectors.SchemaInfo;
import com.migration.jobs.LabSchemas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabControllerTest {

    @Mock
    private LabIntrospectionService labIntrospection;

    @Mock
    private LabAdminService labAdmin;

    @Mock
    private LabDevtoolsInstaller labDevtoolsInstaller;

    @Mock
    private com.migration.connectors.PluginDirectoryService pluginDirectory;

    @Mock
    private com.migration.auth.UserService userService;

    @InjectMocks
    private LabController controller;

    @Test
    void listsLabSchemas() throws Exception {
        when(labIntrospection.listSchemas()).thenReturn(
            new SchemaInfo(java.util.List.of(LabSchemas.SOURCE, LabSchemas.DESTINATION)));
        assertEquals(2, controller.schemas().schemas().size());
    }
}
