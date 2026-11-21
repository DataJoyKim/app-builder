package com.prometis.appbuilder.app.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FILE 유형 화면의 HTML 소스가 {root-path}/{applicationId}/views/{objectCode}.html 에 저장되고,
 * 그 밖의 경로나 UTF-8 이 아닌 파일은 거부되는지 확인한다.
 */
class ViewSourceStorageTest {
    @TempDir
    Path root;

    private ViewSourceStorage storage;

    @BeforeEach
    void setUp() {
        storage = new ViewSourceStorage(new ViewProperties(root.toString(), DataSize.ofKilobytes(1)));
    }

    @Test
    void 소스는_루트_아래_애플리케이션의_views_폴더에_저장된다() throws Exception {
        storage.write("ehr", "EMP_LIST", "<div>사원</div>");

        Path file = root.resolve("ehr").resolve("views").resolve("EMP_LIST.html");
        assertTrue(Files.isRegularFile(file));
        assertEquals("<div>사원</div>", Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(Optional.of("<div>사원</div>"), storage.read("ehr", "EMP_LIST"));
        // 임시 파일이 남지 않는다
        try(var files = Files.list(file.getParent())) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void 다시_저장하면_덮어쓴다() {
        storage.write("ehr", "V1", "first");
        storage.write("ehr", "V1", "second");

        assertEquals(Optional.of("second"), storage.read("ehr", "V1"));
    }

    @Test
    void 없는_소스는_empty_이고_지우면_없어진다() {
        assertEquals(Optional.empty(), storage.read("ehr", "NONE"));

        storage.write("ehr", "V1", "x");
        storage.delete("ehr", "V1");

        assertEquals(Optional.empty(), storage.read("ehr", "V1"));
    }

    @Test
    void 경로를_벗어나는_코드나_애플리케이션ID는_거부한다() {
        assertThrows(ViewSourceException.class, () -> storage.write("ehr", "../evil", "x"));
        assertThrows(ViewSourceException.class, () -> storage.write("ehr", "a/b", "x"));
        assertThrows(ViewSourceException.class, () -> storage.write("ehr", "..", "x"));
        assertThrows(ViewSourceException.class, () -> storage.write("../other", "V1", "x"));
        assertThrows(ViewSourceException.class, () -> storage.read("ehr", "a\\b"));
        assertFalse(Files.exists(root.resolve("evil.html")));
    }

    @Test
    void 크기_한도를_넘으면_거부한다() {
        String big = "a".repeat(1025);

        assertThrows(ViewSourceException.class, () -> storage.write("ehr", "V1", big));
        assertEquals(Optional.empty(), storage.read("ehr", "V1"));
    }

    @Test
    void 올린_파일은_UTF8_만_받고_BOM_은_뗀다() {
        byte[] withBom = ("﻿<p>한글</p>").getBytes(StandardCharsets.UTF_8);
        assertEquals("<p>한글</p>", storage.writeBytes("ehr", "V1", withBom));
        assertEquals(Optional.of("<p>한글</p>"), storage.read("ehr", "V1"));

        byte[] eucKr = "<p>한글</p>".getBytes(Charset.forName("EUC-KR"));
        assertThrows(ViewSourceException.class, () -> storage.writeBytes("ehr", "V2", eucKr));
        assertEquals(Optional.empty(), storage.read("ehr", "V2"));
    }
}
