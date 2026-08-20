package com.g42.platform.gms.common.service;

import com.cloudinary.Api;
import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import com.g42.platform.gms.common.service.MediaLibraryService.MediaInUseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.multipart.MultipartFile;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MediaLibraryServiceTest {

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Api api;
    @Mock
    private ImageUploadService imageUploadService;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private MediaLibraryService service;

    @BeforeEach
    void setUp() {
        lenient().when(cloudinary.api()).thenReturn(api);
        service = new MediaLibraryService(cloudinary, imageUploadService, jdbcTemplate);
    }

    @Test
    void listImagesMapsCloudinaryMetadataAndCursor() throws Exception {
        ApiResponse response = apiResponse(Map.of(
                "resources", List.of(Map.of(
                        "public_id", "garage/posts/post-cover",
                        "secure_url", "https://res.cloudinary.com/demo/image/upload/v1/garage/posts/post-cover.webp",
                        "format", "webp",
                        "width", 1600,
                        "height", 900,
                        "bytes", 125_000,
                        "created_at", "2026-08-20T08:00:00Z")),
                "next_cursor", "next-page"));
        when(api.resources(anyMap())).thenReturn(response);

        var page = service.listImages(null, 60, null);

        assertThat(page.resources()).hasSize(1);
        assertThat(page.resources().get(0).publicId()).isEqualTo("garage/posts/post-cover");
        assertThat(page.resources().get(0).folder()).isEqualTo("garage/posts");
        assertThat(page.resources().get(0).referenceStatus()).isEqualTo("UNKNOWN");
        assertThat(page.nextCursor()).isEqualTo("next-page");
    }

    @Test
    void deleteImageDestroysOnlyAfterReferenceScanIsSafe() throws Exception {
        stubResource();
        stubSearchableColumn("posts", "content_html");
        when(jdbcTemplate.queryForObject(contains("SELECT COUNT(*)"), eq(Long.class), anyString(), anyString()))
                .thenReturn(0L);
        when(imageUploadService.deleteImageWithResult("garage/posts/post-cover")).thenReturn("ok");

        var result = service.deleteImage("garage/posts/post-cover");

        assertThat(result.result()).isEqualTo("ok");
        assertThat(result.invalidated()).isTrue();
        verify(imageUploadService).deleteImageWithResult("garage/posts/post-cover");
    }

    @Test
    void deleteImageIsBlockedWhenDatabaseContainsReference() throws Exception {
        stubResource();
        stubSearchableColumn("posts", "content_html");
        when(jdbcTemplate.queryForObject(contains("SELECT COUNT(*)"), eq(Long.class), anyString(), anyString()))
                .thenReturn(2L);

        assertThatThrownBy(() -> service.deleteImage("garage/posts/post-cover"))
                .isInstanceOf(MediaInUseException.class)
                .hasMessageContaining("2 vị trí");
        verify(imageUploadService, never()).deleteImageWithResult(anyString());
    }

    @Test
    void referenceScanFailureReturnsUnknownAndBlocksDeletion() throws Exception {
        stubResource();
        stubSearchableColumn("posts", "content_html");
        when(jdbcTemplate.queryForObject(contains("SELECT COUNT(*)"), eq(Long.class), anyString(), anyString()))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        var usage = service.inspectUsage("garage/posts/post-cover");

        assertThat(usage.referenceStatus()).isEqualTo("UNKNOWN");
        assertThat(usage.referenceCount()).isNull();
        assertThatThrownBy(() -> service.deleteImage("garage/posts/post-cover"))
                .isInstanceOf(MediaInUseException.class)
                .hasMessageContaining("Chưa thể xác minh");
        verify(imageUploadService, never()).deleteImageWithResult(anyString());
    }

    @Test
    void invalidUploadFolderIsRejectedBeforeCloudinaryCall() {
        MultipartFile file = mock(MultipartFile.class);
        assertThatThrownBy(() -> service.uploadImages(List.of(file), "../outside"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không hợp lệ");
    }

    private void stubResource() throws Exception {
        ApiResponse response = apiResponse(Map.of(
                "public_id", "garage/posts/post-cover",
                "secure_url", "https://res.cloudinary.com/demo/image/upload/v1/garage/posts/post-cover.webp",
                "format", "webp"));
        when(api.resource(eq("garage/posts/post-cover"), anyMap())).thenReturn(response);
    }

    private ApiResponse apiResponse(Map<String, Object> values) {
        ApiResponse response = mock(ApiResponse.class);
        when(response.get(any())).thenAnswer(invocation -> values.get(String.valueOf((Object) invocation.getArgument(0))));
        return response;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubSearchableColumn(String table, String column) {
        when(jdbcTemplate.query(contains("INFORMATION_SCHEMA.COLUMNS"), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper mapper = invocation.getArgument(1);
                    ResultSet resultSet = mock(ResultSet.class);
                    when(resultSet.getString("TABLE_NAME")).thenReturn(table);
                    when(resultSet.getString("COLUMN_NAME")).thenReturn(column);
                    return List.of(mapper.mapRow(resultSet, 0));
                });
    }
}
