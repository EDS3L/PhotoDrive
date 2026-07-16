package pl.photodrive.core.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pl.photodrive.core.application.port.repository.AlbumRepository;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.support.IntegrationTest;
import pl.photodrive.core.support.TestFixtures;

import java.net.URI;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Macierz autoryzacji (4.5): dla KAŻDEGO chronionego endpointu i KAŻDEJ roli sprawdzamy,
 * czy wejście jest otwarte, czy zatrzaśnięte — na żywym torze HTTP (filtry + Spring Security
 * + reguły domeny), a nie na mockach.
 *
 * <p>Macierz jest <b>efektywna</b>: opisuje, kto realnie przejdzie przez web I domenę, a nie tylko
 * co deklaruje jedna warstwa. Dla {@code /api/user/getAssignedUsers} obie warstwy są zgodne — przez
 * bramkę przechodzi tylko fotograf (dedykowany test niżej pilnuje, że admin jest odrzucany już na
 * filtrze web, zanim żądanie dotknie domeny — A13). Test patrzy na to, co widzi klient API.
 *
 * <p>Druga część klasy pilnuje rozróżnienia z A10: <b>odmowa autoryzacji to 403, nie 400</b>.
 * Wcześniej „nie wolno ci" było nieodróżnialne od „popraw dane" — obie ścieżki wracały jako 400.
 */
class SecurityAuthorizationIT extends IntegrationTest {

    /** Identyfikator, którego nie ma w bazie — kontrola dostępu musi zadziałać, zanim ktokolwiek go poszuka. */
    private static final UUID UNKNOWN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final Set<Role> ALL_ROLES = EnumSet.of(Role.ADMIN, Role.PHOTOGRAPHER, Role.CLIENT);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlbumRepository albumRepository;

    private User admin;
    private User photographer;
    private User client;

    @BeforeEach
    void seedUsers() {
        admin = fixtures.admin("admin@photodrive.dev");
        photographer = fixtures.photographer("foto@photodrive.dev");
        client = fixtures.client("klient@photodrive.dev");
    }

    /** Jeden wiersz macierzy: endpoint + role, które mają się przez niego przedostać. */
    private record Endpoint(HttpMethod method, String path, String body, Set<Role> allowedRoles) {
        @Override
        public String toString() {
            return method + " " + path + " → " + allowedRoles;
        }
    }

    private static Endpoint endpoint(HttpMethod method, String path, Role... allowedRoles) {
        return new Endpoint(method, path, null, Set.of(allowedRoles));
    }

    private static Endpoint endpoint(HttpMethod method, String path, String body, Role... allowedRoles) {
        return new Endpoint(method, path, body, Set.of(allowedRoles));
    }

    static Stream<Endpoint> protectedEndpoints() {
        return Stream.of(
                // --- użytkownicy ---
                endpoint(HttpMethod.GET, "/api/user/me", Role.ADMIN, Role.PHOTOGRAPHER, Role.CLIENT),
                endpoint(HttpMethod.GET, "/api/user/all", Role.ADMIN),
                endpoint(HttpMethod.GET, "/api/user/activeUsers", Role.ADMIN),
                // Lista klientów fotografa należy do fotografa — admin ma do tego osobny endpoint niżej.
                endpoint(HttpMethod.GET, "/api/user/getAssignedUsers", Role.PHOTOGRAPHER),
                endpoint(HttpMethod.GET, "/api/user/" + UNKNOWN_ID + "/assignedUsers", Role.ADMIN),
                endpoint(HttpMethod.POST, "/api/user/add",
                        """
                        {"name":"Matrix","email":"matrix@photodrive.dev","role":"CLIENT"}""",
                        Role.ADMIN, Role.PHOTOGRAPHER),
                endpoint(HttpMethod.PATCH, "/api/user/" + UNKNOWN_ID + "/addRole",
                        """
                        {"role":"PHOTOGRAPHER"}""", Role.ADMIN),
                endpoint(HttpMethod.PATCH, "/api/user/" + UNKNOWN_ID + "/removeRole",
                        """
                        {"role":"PHOTOGRAPHER"}""", Role.ADMIN),
                endpoint(HttpMethod.PATCH, "/api/user/" + UNKNOWN_ID + "/activateUser", "true", Role.ADMIN),
                endpoint(HttpMethod.PATCH, "/api/user/" + UNKNOWN_ID + "/deactivateUser", "false", Role.ADMIN),
                endpoint(HttpMethod.PATCH, "/api/user/" + UNKNOWN_ID + "/assignUsers",
                        """
                        {"userIdList":[]}""", Role.ADMIN),
                endpoint(HttpMethod.PATCH, "/api/user/" + UNKNOWN_ID + "/removeUsers",
                        """
                        {"userIdList":[]}""", Role.ADMIN),
                // Zmiana hasła i maila cudzego konta: tylko admin (albo właściciel — tu ID jest cudze).
                endpoint(HttpMethod.PATCH, "/api/user/" + UNKNOWN_ID + "/changePassword",
                        """
                        {"currentPassword":"Test1234!","newPassword":"Test5678!"}""", Role.ADMIN),
                endpoint(HttpMethod.PATCH, "/api/user/" + UNKNOWN_ID + "/changeEmail",
                        """
                        {"newEmail":"inny@photodrive.dev"}""", Role.ADMIN),

                // --- albumy ---
                endpoint(HttpMethod.GET, "/api/album/all", Role.ADMIN),
                endpoint(HttpMethod.GET, "/api/album/all/withoutTdd", Role.ADMIN),
                endpoint(HttpMethod.GET, "/api/album/getAllAssignedAlbums", Role.PHOTOGRAPHER, Role.CLIENT),
                endpoint(HttpMethod.POST, "/api/album/admin/create",
                        """
                        {"name":"MatrixAlbum"}""", Role.ADMIN),
                endpoint(HttpMethod.POST, "/api/album/client/" + UNKNOWN_ID + "/create",
                        """
                        {"name":"MatrixAlbum"}""", Role.PHOTOGRAPHER),
                endpoint(HttpMethod.PATCH, "/api/album/" + UNKNOWN_ID + "/setPublic?isPublic=true", Role.ADMIN),

                // --- znak wodny ---
                // Status czyta też fotograf (steruje widocznością akcji w UI); logiem zarządza tylko admin.
                endpoint(HttpMethod.GET, "/api/watermark/status", Role.ADMIN, Role.PHOTOGRAPHER),
                endpoint(HttpMethod.GET, "/api/watermark", Role.ADMIN),
                endpoint(HttpMethod.DELETE, "/api/watermark", Role.ADMIN),

                // --- sloty strony wizytówki ---
                // Zarządza tylko admin (upload multipart pokrywa ta sama reguła /api/site/** — jak przy watermarku).
                endpoint(HttpMethod.GET, "/api/site/slots", Role.ADMIN),
                endpoint(HttpMethod.DELETE, "/api/site/slots/HOME_HERO", Role.ADMIN)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("protectedEndpoints")
    @DisplayName("A request without a session never reaches a protected endpoint")
    void shouldRejectAnonymousRequestOnEveryProtectedEndpoint(Endpoint endpoint) throws Exception {
        // When / Then
        mockMvc.perform(build(endpoint, null))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("protectedEndpoints")
    @DisplayName("Every role outside the matrix is refused, and every role inside it gets through")
    void shouldEnforceTheAuthorizationMatrixForEveryRole(Endpoint endpoint) throws Exception {
        for (Role role : ALL_ROLES) {
            // When
            int status = mockMvc.perform(build(endpoint, cookieFor(role)))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            // Then - a role outside the matrix must be stopped by authorization, not by anything else
            if (endpoint.allowedRoles().contains(role)) {
                assertThat(status)
                        .as("%s should reach %s", role, endpoint)
                        .isNotIn(401, 403);
            } else {
                assertThat(status)
                        .as("%s must be refused on %s", role, endpoint)
                        .isEqualTo(403);
            }
        }
    }

    // =======================================================================
    // A13 — odmowa pada na warstwie web, nie dopiero w domenie
    // =======================================================================

    @Test
    @DisplayName("An admin is turned away from the photographer's client list at the web filter, before the domain is consulted")
    void shouldDenyAdminAssignedUsersAtTheWebLayer() throws Exception {
        // Given - admin ma własny endpoint na listy klientów (/{id}/assignedUsers); ten należy do fotografa.
        // When
        MvcResult result = mockMvc.perform(get("/api/user/getAssignedUsers")
                        .cookie(fixtures.authCookie(admin)))
                .andExpect(status().isForbidden())
                .andReturn();

        // Then - to odmowa z filtra Spring Security (accessDeniedHandler → {"error":"Forbidden"}),
        // a NIE z domeny (DomainSecurityException → ApiException z kodem SECURITY_EXCEPTION). Gdyby
        // WebConfig znów wpuścił admina na tę ścieżkę, 403 przyszłoby z domeny i to rozróżnienie by padło.
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("Forbidden");
        assertThat(body).doesNotContain("SECURITY_EXCEPTION");
    }

    // =======================================================================
    // A10 — odmowa autoryzacji to 403, nie 400
    // =======================================================================

    @Test
    @DisplayName("Client is refused write access to his own album as a denial (403), not as a bad request (400)")
    void shouldRefuseClientWritingToHisOwnAlbumWith403() throws Exception {
        // Given - album, do którego klient MA dostęp do odczytu: jedyne, co go zatrzymuje, to brak prawa zapisu
        UUID albumId = createClientAlbum("DenialTest");
        Cookie clientCookie = fixtures.authCookie(client);

        // When / Then - usunięcie albumu
        mockMvc.perform(delete("/api/album/{albumId}/delete", albumId).cookie(clientCookie))
                .andExpect(status().isForbidden());

        // When / Then - odsłonięcie zdjęć
        mockMvc.perform(patch("/api/album/{albumId}/files/setVisible", albumId)
                        .param("visible", "true")
                        .cookie(clientCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idList":["%s"]}""".formatted(UNKNOWN_ID)))
                .andExpect(status().isForbidden());

        // When / Then - włączenie znaku wodnego
        mockMvc.perform(post("/api/album/{albumId}/files/addWatermark", albumId)
                        .param("hasWatermark", "true")
                        .cookie(clientCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filesUUIDList":["%s"]}""".formatted(UNKNOWN_ID)))
                .andExpect(status().isForbidden());

        // When / Then - ustawienie daty usunięcia albumu
        mockMvc.perform(patch("/api/album/{albumId}/setTtd", albumId)
                        .cookie(clientCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ttd":"%s"}""".formatted(LocalDate.now().plusDays(30))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Client cannot rename a photo in his own album, even one that really exists there")
    void shouldRefuseClientRenamingAnExistingPhotoWith403() throws Exception {
        // Given - prawdziwy plik w albumie, żeby odmowa nie mogła się schować za „pliku nie ma"
        UUID albumId = createClientAlbum("RenameDenialTest");
        MockMultipartFile photo = new MockMultipartFile("files", "foto.jpg", "image/jpeg", TestFixtures.jpeg(40, 40));
        mockMvc.perform(multipart("/api/album/upload/{albumId}/files", albumId)
                        .file(photo)
                        .cookie(fixtures.authCookie(photographer)))
                .andExpect(status().isAccepted());
        UUID fileId = uploadedFileId(albumId);

        // When / Then
        mockMvc.perform(put("/api/album/{albumId}/rename/{fileId}", albumId, fileId)
                        .cookie(fixtures.authCookie(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newFileName":"przejete.jpg"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Photographer is refused a TTD on an album he does not own, so ownership is checked, not just the role")
    void shouldRefuseForeignPhotographerSettingTtdWith403() throws Exception {
        // Given - album należy do `photographer`, próbuje inny fotograf (ta sama rola!)
        UUID albumId = createClientAlbum("ForeignTtdTest");
        User otherPhotographer = fixtures.photographer("obcy@photodrive.dev");

        // When / Then
        mockMvc.perform(patch("/api/album/{albumId}/setTtd", albumId)
                        .cookie(fixtures.authCookie(otherPhotographer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ttd":"%s"}""".formatted(LocalDate.now().plusDays(30))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A broken business rule is still a 400, so not every refusal collapsed into 403")
    void shouldStillReturn400WhenTheOwnerBreaksABusinessRule() throws Exception {
        // Given - właściciel albumu, więc autoryzacja przechodzi; łamie za to regułę „TTD w przyszłości"
        UUID albumId = createClientAlbum("BusinessRuleTest");

        // When / Then
        mockMvc.perform(patch("/api/album/{albumId}/setTtd", albumId)
                        .cookie(fixtures.authCookie(photographer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ttd":"%s"}""".formatted(LocalDate.now().minusDays(1))))
                .andExpect(status().isBadRequest());
    }

    // --- helpers -----------------------------------------------------------

    private MockHttpServletRequestBuilder build(Endpoint endpoint, Cookie cookie) {
        MockHttpServletRequestBuilder builder = request(endpoint.method(), URI.create(endpoint.path()));
        if (endpoint.body() != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(endpoint.body());
        }
        if (cookie != null) {
            builder.cookie(cookie);
        }
        return builder;
    }

    private Cookie cookieFor(Role role) {
        return switch (role) {
            case ADMIN -> fixtures.authCookie(admin);
            case PHOTOGRAPHER -> fixtures.authCookie(photographer);
            case CLIENT -> fixtures.authCookie(client);
        };
    }

    private UUID createClientAlbum(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/album/client/{clientId}/create", client.getId().value())
                        .cookie(fixtures.authCookie(photographer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}""".formatted(name)))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(json(result).get("albumId").asText());
    }

    /** Odczyt agregatu w transakcji — mapowanie encja→domena jest leniwe (patrz {@code IntegrationTest}). */
    private UUID uploadedFileId(UUID albumId) {
        return inTransaction(() -> albumRepository.findByAlbumId(new AlbumId(albumId))
                .orElseThrow(() -> new AssertionError("Album " + albumId + " is not in the database"))
                .getPhotos().values().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Album " + albumId + " has no photos"))
                .getFileId().value());
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
