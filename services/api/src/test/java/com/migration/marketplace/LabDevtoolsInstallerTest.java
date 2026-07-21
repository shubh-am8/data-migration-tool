package com.migration.marketplace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LabDevtoolsInstallerTest {

    @Test
    void listSqlFilesSortsByFilename(@TempDir Path tmp) throws IOException {
        Path sqlDir = Files.createDirectories(tmp.resolve("sql"));
        Files.writeString(sqlDir.resolve("002_second.sql"), "SELECT 2;");
        Files.writeString(sqlDir.resolve("001_first.sql"), "SELECT 1;");
        Files.writeString(sqlDir.resolve("readme.txt"), "not sql");

        List<Path> files = LabDevtoolsInstaller.listSqlFiles(tmp);

        assertEquals(2, files.size());
        assertEquals("001_first.sql", files.get(0).getFileName().toString());
        assertEquals("002_second.sql", files.get(1).getFileName().toString());
    }

    @Test
    void listSqlFilesThrowsWhenSqlDirMissing(@TempDir Path tmp) {
        assertThrows(IOException.class, () -> LabDevtoolsInstaller.listSqlFiles(tmp));
    }

    @Test
    void listSqlFilesThrowsWhenSqlDirEmpty(@TempDir Path tmp) throws IOException {
        Files.createDirectories(tmp.resolve("sql"));

        assertThrows(IOException.class, () -> LabDevtoolsInstaller.listSqlFiles(tmp));
    }
}
