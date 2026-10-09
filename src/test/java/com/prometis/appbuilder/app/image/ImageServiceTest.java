package com.prometis.appbuilder.app.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 이미지 업로드가 확장자/크기 제한(platform.image)을 지키고, 링크에 쓸 파일명을 겹치지 않게 만드는지 확인한다.
 */
class ImageServiceTest {
    private static final String APP = "ehr";
    private static final String LOGO = "LOGO";

    private ImageStorageRepository imageStorageRepository;
    private StorageTypeRepository storageTypeRepository;
    private ImageService imageService;

    @BeforeEach
    void setUp() {
        imageStorageRepository = mock(ImageStorageRepository.class);
        storageTypeRepository = mock(StorageTypeRepository.class);

        when(storageTypeRepository.findByApplicationIdAndStorageType(APP, LOGO))
                .thenReturn(Optional.of(StorageType.builder().id(1L).applicationId(APP).storageType(LOGO).displayName("로고").build()));
        when(imageStorageRepository.findFilenames(APP, LOGO)).thenReturn(List.of("logo"));
        when(imageStorageRepository.save(any(ImageStorage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        imageService = new ImageService(imageStorageRepository, storageTypeRepository,
                new ImageProperties(List.of("PNG", ".jpg"), DataSize.ofBytes(10)));
    }

    @Test
    void 확장자_설정은_소문자로_점없이_정리된다() {
        assertEquals(List.of("png", "jpg"), imageService.policy().allowedExtensions());
    }

    @Test
    void 파일명을_지정하지_않으면_원본_파일명에서_확장자를_뺀다() {
        List<ImageInfo> results = imageService.upload(APP, LOGO, List.of(
                file("main.png", 3),
                file("C:\\fakepath\\ba;d#name.v2.JPG", 3)
        ), List.of(), false);

        assertEquals(List.of("main", "ba_d_name.v2"), results.stream().map(ImageInfo::filename).toList());
        assertEquals("ba;d#name.v2.JPG", results.get(1).originalFilename());
        assertEquals("jpg", results.get(1).fileExtension());
        assertEquals("/ehr/image/LOGO/main", results.get(0).url());
        assertEquals("/ehr/image/LOGO/%EB%A1%9C%EA%B3%A0%20v2", ImageInfo.urlOf(APP, LOGO, "로고 v2"));
    }

    @Test
    void 파일마다_지정한_파일명을_쓰고_빈_값은_원본_파일명을_쓴다() {
        List<ImageInfo> results = imageService.upload(APP, LOGO, List.of(
                file("a.png", 3),
                file("b.png", 3)
        ), Arrays.asList(" 1001 ", ""), false);

        assertEquals(List.of("1001", "b"), results.stream().map(ImageInfo::filename).toList());
        assertEquals("a.png", results.get(0).originalFilename());
    }

    @Test
    void 지정한_파일명에_링크에_쓸_수_없는_문자가_있으면_거절한다() {
        assertThrows(ImageException.class, () -> imageService.upload(APP, LOGO, List.of(file("a.png", 3)), List.of("a/b"), false));
        assertThrows(ImageException.class, () -> imageService.upload(APP, LOGO, List.of(file("a.png", 3)), List.of(".."), false));
        verify(imageStorageRepository, never()).save(any());
    }

    @Test
    void 함께_올리는_파일끼리_파일명이_겹치면_거절한다() {
        assertThrows(ImageException.class, () -> imageService.upload(APP, LOGO, List.of(file("a.png", 3), file("a.jpg", 3)), List.of(), false));
        verify(imageStorageRepository, never()).save(any());
    }

    @Test
    void 이미_있는_파일명은_덮어쓰기일_때만_지우고_새로_넣는다() {
        ImageException e = assertThrows(ImageException.class, () -> imageService.upload(APP, LOGO, List.of(file("logo.png", 3)), List.of(), false));
        assertTrue(e.getMessage().contains("logo"));
        verify(imageStorageRepository, never()).save(any());

        List<ImageInfo> results = imageService.upload(APP, LOGO, List.of(file("logo.png", 3)), List.of(), true);

        assertEquals("logo", results.get(0).filename());
        verify(imageStorageRepository).deleteByFilename(APP, LOGO, "logo");
        verify(imageStorageRepository).save(any(ImageStorage.class));
    }

    @Test
    void 파일명_개수가_파일_개수와_다르면_거절한다() {
        assertThrows(ImageException.class, () -> imageService.upload(APP, LOGO, List.of(file("a.png", 3), file("b.png", 3)), List.of("a"), false));
    }

    @Test
    void 프로필_링크의_userId_를_사용자ID로_바꾸고_이미지가_없으면_null() {
        String link = "/ehr/image/PROFILE/#{userId}";
        when(imageStorageRepository.findInfo(APP, "PROFILE", "7"))
                .thenReturn(Optional.of(new ImageInfo(1L, APP, "PROFILE", "7", "me.png", "png", 3L)));

        assertTrue(ImageService.isUserImageLink(APP, link));
        assertFalse(ImageService.isUserImageLink(APP, "/ehr/image/PROFILE/7"));
        assertFalse(ImageService.isUserImageLink(APP, "/other/image/PROFILE/#{userId}"));
        assertFalse(ImageService.isUserImageLink(APP, "/ehr/image/a/b/#{userId}"));

        assertEquals("/ehr/image/PROFILE/7", imageService.resolveUserImageLink(APP, link, 7L));
        assertNull(imageService.resolveUserImageLink(APP, link, 8L));
        assertNull(imageService.resolveUserImageLink(APP, null, 7L));
    }

    private static List<ImageInfo> upload(ImageService imageService, List<MultipartFile> files) {
        return imageService.upload(APP, LOGO, files, List.of(), false);
    }


    @Test
    void 허용되지_않는_확장자가_하나라도_있으면_아무것도_저장하지_않는다() {
        ImageException e = assertThrows(ImageException.class, () -> upload(imageService, List.of(
                file("ok.png", 3),
                file("evil.svg", 3)
        )));

        assertTrue(e.getMessage().contains("evil.svg"));
        verify(imageStorageRepository, never()).save(any());
    }

    @Test
    void 크기_제한을_넘으면_거절한다() {
        ImageException e = assertThrows(ImageException.class, () -> upload(imageService, List.of(file("big.png", 11))));

        assertTrue(e.getMessage().contains("big.png"));
        verify(imageStorageRepository, never()).save(any());
    }

    @Test
    void 없는_저장소_유형에는_올릴_수_없다() {
        assertThrows(ImageException.class, () -> imageService.upload(APP, "NONE", List.of(file("a.png", 3)), List.of(), false));
    }

    @Test
    void 이미지가_남은_저장소_유형은_삭제하지_않는다() {
        StorageType storageType = StorageType.builder().id(1L).applicationId(APP).storageType(LOGO).displayName("로고").build();
        when(storageTypeRepository.findById(1L)).thenReturn(Optional.of(storageType));
        when(imageStorageRepository.existsByApplicationIdAndStorageType(APP, LOGO)).thenReturn(true);

        assertThrows(ImageException.class, () -> imageService.deleteStorageType(APP, 1L));
        verify(storageTypeRepository, never()).delete(any());
    }

    @Test
    void 저장소_유형_코드는_링크에_쓸_수_있는_문자만_허용한다() {
        assertThrows(ImageException.class, () -> imageService.createStorageType(APP, "로고 이미지", "로고"));

        when(storageTypeRepository.save(any(StorageType.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<StorageType> captor = ArgumentCaptor.forClass(StorageType.class);

        imageService.createStorageType(APP, " PROFILE ", "프로필");

        verify(storageTypeRepository).save(captor.capture());
        assertEquals("PROFILE", captor.getValue().getStorageType());
    }

    private static MultipartFile file(String originalFilename, int size) {
        return new MockMultipartFile("files", originalFilename, "application/octet-stream", new byte[size]);
    }
}
