package com.prometis.appbuilder.app.image;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 앱에서 쓰는 이미지(로고, 프로필사진, 아이콘 등)를 저장소 유형(StorageType)별로 DB에 보관한다.
 * 콘솔(Image Manage)에서 업로드/삭제하고, 앱은 /{applicationId}/image/{storageType}/{filename} 링크로 조회한다.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(ImageProperties.class)
public class ImageService {
    // storageType 과 filename 은 ImageStorage 컬럼 길이(100)를 넘을 수 없다
    private static final int MAX_NAME_LENGTH = 100;
    // storageType 은 이미지 링크의 경로 한 칸이 된다
    private static final Pattern STORAGE_TYPE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1," + MAX_NAME_LENGTH + "}$");
    // 링크 경로에서 문제가 되는 문자 (경로 구분자, ; 매트릭스 변수, % 인코딩, # ? 등)와 제어문자
    private static final Pattern UNSAFE_FILENAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|;#%\\p{Cntrl}]");
    // 사용자별 이미지 링크(Layout 프로필 이미지)의 filename 자리. 로그인한 사용자ID(User.id)로 바뀐다
    public static final String USER_ID_EXPRESSION = "#{userId}";

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "gif", "image/gif",
            "webp", "image/webp",
            "bmp", "image/bmp",
            "ico", "image/x-icon",
            "svg", "image/svg+xml",
            "avif", "image/avif",
            "tif", "image/tiff"
    );

    private final ImageStorageRepository imageStorageRepository;
    private final StorageTypeRepository storageTypeRepository;
    private final ImageProperties properties;

    public ImageProperties policy() {
        return properties;
    }

    public String contentTypeOf(String fileExtension) {
        return CONTENT_TYPES.getOrDefault(fileExtension == null ? "" : fileExtension.toLowerCase(Locale.ROOT), "application/octet-stream");
    }

    /* ---------- 저장소 유형 ---------- */

    public List<StorageType> getStorageTypes(String applicationId) {
        return storageTypeRepository.findByApplicationIdOrderByStorageTypeAsc(applicationId);
    }

    @Transactional
    public StorageType createStorageType(String applicationId, String storageType, String displayName) {
        storageType = text(storageType);
        displayName = text(displayName);

        if(storageType == null || displayName == null) {
            throw new ImageException("저장소 유형과 표시명을 입력해주세요.");
        }
        if(!STORAGE_TYPE_PATTERN.matcher(storageType).matches()) {
            throw new ImageException("저장소 유형은 영문, 숫자, _, - 로 " + MAX_NAME_LENGTH + "자 이내로 입력해주세요.");
        }
        if(storageTypeRepository.findByApplicationIdAndStorageType(applicationId, storageType).isPresent()) {
            throw new ImageException("이미 존재하는 저장소 유형입니다. [" + storageType + "]");
        }

        return storageTypeRepository.save(StorageType.builder()
                .applicationId(applicationId)
                .storageType(storageType)
                .displayName(displayName)
                .build());
    }

    @Transactional
    public StorageType updateStorageType(String applicationId, Long id, String displayName) {
        StorageType savedData = ownedStorageType(applicationId, id);

        displayName = text(displayName);
        if(displayName == null) {
            throw new ImageException("표시명을 입력해주세요.");
        }

        savedData.updateDisplayName(displayName);

        return storageTypeRepository.save(savedData);
    }

    // 이미지가 남아 있으면 링크가 끊기므로 지우지 않는다
    @Transactional
    public void deleteStorageType(String applicationId, Long id) {
        StorageType savedData = ownedStorageType(applicationId, id);

        if(imageStorageRepository.existsByApplicationIdAndStorageType(applicationId, savedData.getStorageType())) {
            throw new ImageException("이미지가 남아 있는 저장소 유형은 삭제할 수 없습니다. 이미지를 먼저 삭제해주세요.");
        }

        storageTypeRepository.delete(savedData);
    }

    /* ---------- 이미지 ---------- */

    public List<ImageInfo> getImages(String applicationId, String storageType) {
        return imageStorageRepository.findInfos(applicationId, storageType);
    }

    /**
     * 여러 이미지를 한 번에 올린다. 한 개라도 검사에 걸리면 아무것도 저장하지 않는다.
     * filename 은 확장자 없는 링크 이름(/{applicationId}/image/{storageType}/{filename})이다.
     * filenames 는 files 와 같은 순서로 파일마다 지정하는 이름이고, 비어 있으면 원본 파일명에서 확장자를 뺀 이름을 쓴다.
     * 같은 저장소 유형에 이미 있는 filename 은 overwrite 일 때만 새 이미지로 바꾼다 (프로필처럼 같은 링크로 다시 올리는 경우).
     */
    @Transactional
    public List<ImageInfo> upload(String applicationId, String storageType, List<MultipartFile> files, List<String> filenames, boolean overwrite) {
        storageType = text(storageType);
        if(storageType == null || storageTypeRepository.findByApplicationIdAndStorageType(applicationId, storageType).isEmpty()) {
            throw new ImageException("저장소 유형을 찾을 수 없습니다. [" + storageType + "]");
        }

        List<MultipartFile> uploadFiles = files == null ? List.of() : files.stream().filter(Objects::nonNull).toList();
        if(uploadFiles.isEmpty()) {
            throw new ImageException("업로드할 이미지를 선택해주세요.");
        }

        List<String> requestedFilenames = filenames == null ? List.of() : filenames;
        if(!requestedFilenames.isEmpty() && requestedFilenames.size() != uploadFiles.size()) {
            throw new ImageException("파일명 개수가 파일 개수와 다릅니다.");
        }

        List<String> uploadFilenames = new ArrayList<>();
        for(int i = 0; i < uploadFiles.size(); i++) {
            String requested = requestedFilenames.isEmpty() ? null : text(requestedFilenames.get(i));
            uploadFilenames.add(requested != null ? requested : defaultFilenameOf(baseNameOf(uploadFiles.get(i).getOriginalFilename())));
        }

        Set<String> existingFilenames = new HashSet<>(imageStorageRepository.findFilenames(applicationId, storageType));
        validate(uploadFiles, uploadFilenames, existingFilenames, overwrite);

        List<ImageInfo> results = new ArrayList<>();

        for(int i = 0; i < uploadFiles.size(); i++) {
            MultipartFile file = uploadFiles.get(i);
            String filename = uploadFilenames.get(i);
            String originalFilename = baseNameOf(file.getOriginalFilename());
            String extension = extensionOf(originalFilename);

            byte[] content;
            try {
                content = file.getBytes();
            }
            catch (IOException e) {
                throw new ImageException("이미지를 읽지 못했습니다. [" + originalFilename + "]");
            }

            // 덮어쓸 때는 지우고 새로 넣는다. id 가 바뀌어야 이미지 링크의 ETag 가 바뀐다
            if(existingFilenames.contains(filename)) {
                imageStorageRepository.deleteByFilename(applicationId, storageType, filename);
            }

            ImageStorage savedData = imageStorageRepository.save(ImageStorage.builder()
                    .applicationId(applicationId)
                    .storageType(storageType)
                    .filename(filename)
                    .originalFilename(truncate(originalFilename, MAX_NAME_LENGTH))
                    .fileExtension(extension)
                    .fileSize((long) content.length)
                    .content(content)
                    .build());

            results.add(new ImageInfo(savedData.getId(), applicationId, storageType, filename,
                    savedData.getOriginalFilename(), extension, savedData.getFileSize()));
        }

        return results;
    }

    @Transactional
    public void deleteImage(String applicationId, Long id) {
        ImageInfo image = imageStorageRepository.findInfo(id)
                .filter(owned -> applicationId.equals(owned.applicationId()))
                .orElseThrow(() -> new ImageException("이미지를 찾을 수 없습니다."));

        imageStorageRepository.deleteById(image.id());
    }

    public Optional<ImageInfo> findImage(String applicationId, String storageType, String filename) {
        return imageStorageRepository.findInfo(applicationId, storageType, filename);
    }

    public Optional<byte[]> findContent(Long id) {
        return imageStorageRepository.findContent(id);
    }

    /* ---------- 사용자별 이미지 링크 (Layout 프로필 이미지) ---------- */

    /**
     * 사용자별 이미지 링크 설정이 맞는 형식인지 본다: /{applicationId}/image/{storageType}/#{userId}
     */
    public static boolean isUserImageLink(String applicationId, String link) {
        return userImageStorageTypeOf(applicationId, link) != null;
    }

    /**
     * 사용자별 이미지 링크 설정의 #{userId} 를 사용자ID로 바꾼 링크. 링크 형식이 틀렸거나 그 사용자의 이미지가 없으면 null
     */
    public String resolveUserImageLink(String applicationId, String link, Long userId) {
        String storageType = userImageStorageTypeOf(applicationId, link);
        if(storageType == null || userId == null) {
            return null;
        }

        String filename = String.valueOf(userId);

        return imageStorageRepository.findInfo(applicationId, storageType, filename)
                .map(ImageInfo::url)
                .orElse(null);
    }

    private static String userImageStorageTypeOf(String applicationId, String link) {
        if(applicationId == null || link == null) {
            return null;
        }

        String prefix = ImageInfo.urlOf(applicationId, "x", "x");
        prefix = prefix.substring(0, prefix.length() - "x/x".length());
        String suffix = "/" + USER_ID_EXPRESSION;

        String trimmed = link.trim();
        if(!trimmed.startsWith(prefix) || !trimmed.endsWith(suffix)) {
            return null;
        }

        String storageType = trimmed.substring(prefix.length(), trimmed.length() - suffix.length());

        return STORAGE_TYPE_PATTERN.matcher(storageType).matches() ? storageType : null;
    }

    private void validate(List<MultipartFile> files, List<String> filenames, Set<String> existingFilenames, boolean overwrite) {
        long maxFileSize = properties.maxFileSize().toBytes();
        List<String> errors = new ArrayList<>();
        Set<String> batchFilenames = new HashSet<>();

        for(int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String originalFilename = baseNameOf(file.getOriginalFilename());
            String filename = filenames.get(i);

            if(originalFilename.isEmpty()) {
                errors.add("파일명이 없는 파일이 있습니다.");
                continue;
            }

            String extension = extensionOf(originalFilename);
            if(extension.isEmpty() || !properties.allowedExtensions().contains(extension)) {
                errors.add("[" + originalFilename + "] 허용되지 않는 확장자입니다. (허용: " + String.join(", ", properties.allowedExtensions()) + ")");
            }
            else if(file.isEmpty()) {
                errors.add("[" + originalFilename + "] 빈 파일입니다.");
            }
            else if(file.getSize() > maxFileSize) {
                errors.add("[" + originalFilename + "] 파일 크기가 제한(" + properties.maxFileSize() + ")을 넘습니다.");
            }

            String filenameError = filenameErrorOf(filename);
            if(filenameError != null) {
                errors.add("[" + originalFilename + "] " + filenameError);
            }
            else if(!batchFilenames.add(filename)) {
                errors.add("[" + originalFilename + "] 파일명 '" + filename + "' 이(가) 함께 올리는 다른 파일과 겹칩니다.");
            }
            else if(!overwrite && existingFilenames.contains(filename)) {
                errors.add("[" + originalFilename + "] 파일명 '" + filename + "' 은(는) 이미 있습니다. 다른 파일명을 쓰거나 덮어쓰기를 선택해주세요.");
            }
        }

        if(!errors.isEmpty()) {
            throw new ImageException(String.join("\n", errors));
        }
    }

    // 지정한 파일명은 고치지 않고 거절한다 (링크가 예상과 달라지지 않게)
    private static String filenameErrorOf(String filename) {
        if(filename.length() > MAX_NAME_LENGTH) {
            return "파일명은 " + MAX_NAME_LENGTH + "자 이내로 입력해주세요.";
        }
        if(UNSAFE_FILENAME_CHARS.matcher(filename).find()) {
            return "파일명 '" + filename + "' 에 쓸 수 없는 문자가 있습니다. (\\ / : * ? \" < > | ; # % 제외)";
        }
        if(filename.chars().allMatch(c -> c == '.')) {
            return "파일명을 . 으로만 지을 수 없습니다.";
        }
        return null;
    }

    // 브라우저에 따라 원본 파일명에 경로가 붙어 오는 경우가 있다
    private static String baseNameOf(String originalFilename) {
        if(originalFilename == null) {
            return "";
        }

        String name = originalFilename.replace('\\', '/');
        return name.substring(name.lastIndexOf('/') + 1).trim();
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    // 원본 파일명에서 확장자를 빼고, 링크에 쓸 수 없는 문자를 _ 로 바꾼다
    private static String defaultFilenameOf(String originalFilename) {
        int dot = originalFilename.lastIndexOf('.');
        String name = UNSAFE_FILENAME_CHARS.matcher(dot < 0 ? originalFilename : originalFilename.substring(0, dot)).replaceAll("_").trim();

        // . 이나 .. 만 남으면 경로로 해석될 수 있다
        name = name.isEmpty() || name.chars().allMatch(c -> c == '.') ? "image" : name;

        return truncate(name, MAX_NAME_LENGTH);
    }

    private static String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private StorageType ownedStorageType(String applicationId, Long id) {
        return storageTypeRepository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(() -> new ImageException("저장소 유형을 찾을 수 없습니다."));
    }

    private static String text(Object value) {
        if(value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();

        return text.isEmpty() ? null : text;
    }
}
