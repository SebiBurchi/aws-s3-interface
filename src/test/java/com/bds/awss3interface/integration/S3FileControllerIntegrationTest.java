package com.bds.awss3interface.integration;

import com.bds.awss3interface.model.ListResult;
import com.bds.awss3interface.model.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for S3FileController.
 * Verifies upload, list, and pagination functionalities with appropriate role-based access controls.
 */
@AutoConfigureMockMvc
public class S3FileControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${s3.bucket}")
    private static String bucketName;

    private static final String UPLOADS_PREFIX = "uploads/";
    private static final String TEST_FOLDER = bucketName + "/";
    private static final int TOTAL_TEST_FILES = 25; // Ensures pagination is tested

    /**
     * Verifies that an ADMIN user can successfully upload a file.
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void adminCanUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "testfile_upload.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This is a test upload file".getBytes()
        );

        mockMvc.perform(multipart("/api/s3/files/upload")
                        .file(file)
                        .param("key", UPLOADS_PREFIX + "testfile_upload.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("File uploaded successfully")));
    }

    /**
     * Ensures that a USER role cannot upload a file (403 Forbidden).
     */
    @Test
    @WithMockUser(username = "user")
    public void userCannotUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "testfile_user_upload.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This is a test upload file by user".getBytes()
        );

        mockMvc.perform(multipart("/api/s3/files/upload")
                        .file(file)
                        .param("key", UPLOADS_PREFIX + "testfile_user_upload.txt"))
                .andExpect(status().isForbidden());
    }

    /**
     * Verifies that an ADMIN user can retrieve a paginated list of files using continuation tokens.
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void adminCanListFilesWithPagination() throws Exception {
        // Upload multiple files to the test folder
        for (int i = 1; i <= TOTAL_TEST_FILES; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    String.format("test-file-%d.txt", i),
                    MediaType.TEXT_PLAIN_VALUE,
                    String.format("Test content %d", i).getBytes(StandardCharsets.UTF_8)
            );

            mockMvc.perform(multipart("/api/s3/files/upload")
                            .file(file)
                            .param("key", TEST_FOLDER + file.getOriginalFilename()))
                    .andExpect(status().isOk());
        }

        // Fetch the first page
        String firstResponse = mockMvc.perform(get("/api/s3/files/list/folder")
                        .param("folderId", TEST_FOLDER)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(lessThanOrEqualTo(20)))) // Assume backend uses a default page size of 20
                .andExpect(jsonPath("$.cursor", not(emptyOrNullString()))) // Verify cursor is returned
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Parse the first page response
        ListResult<Resource> firstPage = objectMapper.readValue(firstResponse,
                objectMapper.getTypeFactory().constructParametricType(ListResult.class, Resource.class));
        String cursor = firstPage.getCursor();

        // Fetch the second page using the cursor
        mockMvc.perform(get("/api/s3/files/list/folder")
                        .param("folderId", TEST_FOLDER)
                        .param("cursor", cursor)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(greaterThanOrEqualTo(1)))) // Ensure at least one file is returned
                .andExpect(jsonPath("$.cursor", anyOf(nullValue(), emptyOrNullString()))); // Verify last page has null/empty cursor
    }
}
