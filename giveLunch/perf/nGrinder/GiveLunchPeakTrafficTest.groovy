import HTTPClient.CookieModule
import HTTPClient.HTTPResponse
import HTTPClient.NVPair
import groovy.json.JsonOutput
import net.grinder.plugin.http.HTTPPluginControl
import net.grinder.plugin.http.HTTPRequest
import net.grinder.script.GTest
import net.grinder.script.Grinder
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import net.grinder.scriptengine.groovy.junit.annotation.RunRate
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ThreadLocalRandom

@RunWith(GrinderRunner)
class GiveLunchPeakTrafficTest {

    // -----------------------------
    // Process-level (공유 설정/상수)
    // -----------------------------
    static GTest testSuggest
    static GTest testSearchFoodId
    static GTest testNutrition
    static GTest testTopRanks
    static GTest testAddMenu
    static GTest testDeleteMenu
    static GTest testPostRank
    static GTest testAdminFoodSearch

    static String targetHost
    static String userName
    static String password
    static String adminUserName
    static String adminPassword
    static String runId
    static boolean enableAdminFoodSearchScenario
    static final AtomicInteger authRedirectFailureCount = new AtomicInteger(0)
    static final AtomicInteger functionalFailureCount = new AtomicInteger(0)

    static final int TIMEOUT_MS = 6000
    static final String RUN_ID_HEADER = "X-LoadTest-Run-Id"
    static final String SCENARIO_HEADER = "X-LoadTest-Scenario"

    static final String[] HOT_FOODS = ["김치찌개", "제육볶음", "돈까스", "비빔밥", "된장찌개"]
    static final String[] SEARCH_QUERIES = ["김", "제", "돈", "라", "파", "샐", "찌개", "밥"]

    // -----------------------------
    // Thread-level (스레드별 상태)
    // -----------------------------
    HTTPRequest userAuthRequest
    HTTPRequest adminAuthRequest
    HTTPRequest suggestRequest
    HTTPRequest searchFoodIdRequest
    HTTPRequest nutritionRequest
    HTTPRequest topRanksRequest
    HTTPRequest addMenuRequest
    HTTPRequest deleteMenuRequest
    HTTPRequest postRankRequest
    HTTPRequest adminFoodSearchRequest
    String userCsrfToken
    String adminCsrfToken
    boolean userSessionReady
    boolean adminSessionReady
    List<Long> knownFoodIds = []

    @BeforeProcess
    static void beforeProcess() {
        HTTPPluginControl.getConnectionDefaults().timeout = TIMEOUT_MS
        HTTPPluginControl.getConnectionDefaults().useCookies = true
        CookieModule.setCookiePolicyHandler(null)

        new HTTPRequest()

        def props = Grinder.grinder.getProperties()
        targetHost = props.getProperty("targetHost", "http://localhost:8080")
        userName = props.getProperty("userName", "loadtest1")
        password = props.getProperty("password", "loadtest-pass!")
        adminUserName = props.getProperty("adminUserName", "admin")
        adminPassword = props.getProperty("adminPassword", "admin-pass!")
        runId = props.getProperty("runId", "peak-${System.currentTimeMillis()}")
        enableAdminFoodSearchScenario = Boolean.parseBoolean(
                props.getProperty("enableAdminFoodSearchScenario", "false"))

        testSuggest = new GTest(1, "GET /api/menus/suggest")
        testSearchFoodId = new GTest(2, "GET /api/foods/search")
        testNutrition = new GTest(3, "GET /api/foods/{id}/nutrition")
        testTopRanks = new GTest(4, "GET /api/ranks/top")
        testAddMenu = new GTest(5, "POST /api/menus")
        testDeleteMenu = new GTest(6, "DELETE /api/menus")
        testPostRank = new GTest(7, "POST /api/ranks")
        testAdminFoodSearch = new GTest(8, "GET /api/admin/foods?keyword=")
    }

    @BeforeThread
    void beforeThread() {
        if (Grinder.grinder.threadNumber == 0) {
            Grinder.grinder.logger.info("targetHost={}, userName={}, runId={}", targetHost, userName, runId)
        }

        // 스레드마다 request 인스턴스를 분리(헤더/쿠키 등 공유로 인한 섞임 방지)
        userAuthRequest = new HTTPRequest()
        adminAuthRequest = new HTTPRequest()
        suggestRequest = new HTTPRequest()
        searchFoodIdRequest = new HTTPRequest()
        nutritionRequest = new HTTPRequest()
        topRanksRequest = new HTTPRequest()
        addMenuRequest = new HTTPRequest()
        deleteMenuRequest = new HTTPRequest()
        postRankRequest = new HTTPRequest()
        adminFoodSearchRequest = new HTTPRequest()

        testSuggest.record(suggestRequest)
        testSearchFoodId.record(searchFoodIdRequest)
        testNutrition.record(nutritionRequest)
        testTopRanks.record(topRanksRequest)
        testAddMenu.record(addMenuRequest)
        testDeleteMenu.record(deleteMenuRequest)
        testPostRank.record(postRankRequest)
        testAdminFoodSearch.record(adminFoodSearchRequest)

        userSessionReady = false
        adminSessionReady = false
        loginAsUser()
        seedKnownFoodIds()
        Grinder.grinder.statistics.delayReports = true
    }

    @Before
    void before() {
        setHeaders(suggestRequest, "suggest")
        setHeaders(searchFoodIdRequest, "food-search")
        setHeaders(nutritionRequest, "nutrition")
        setHeaders(topRanksRequest, "top-ranks")
        setHeaders(addMenuRequest, "add-menu", userCsrfToken)
        setHeaders(deleteMenuRequest, "delete-menu")
        setHeaders(postRankRequest, "post-rank", userCsrfToken)
        setHeaders(adminFoodSearchRequest, "admin-food-search", adminCsrfToken)
    }

    @Test
    @RunRate(30)
    void suggestScenario() {
        suggest()
    }

    @Test
    @RunRate(20)
    void searchFoodIdScenario() {
        searchFoodId()
    }

    @Test
    @RunRate(15)
    void nutritionScenario() {
        nutrition()
    }

    @Test
    @RunRate(15)
    void topRanksScenario() {
        topRanks()
    }

    @Test
    @RunRate(10)
    void addMenuScenario() {
        addMenu()
    }

    @Test
    @RunRate(5)
    void deleteMenuScenario() {
        deleteMenu()
    }

    @Test
    @RunRate(5)
    void postRankScenario() {
        postRank()
    }

    @Test
    @RunRate(5)
    void adminFoodSearchScenario() {
        if (!enableAdminFoodSearchScenario) {
            return
        }
        adminFoodSearch()
    }

    // -----------------------------
    // Flows
    // -----------------------------
    private void loginAsUser() {
        userCsrfToken = loginAndCreateSession(userAuthRequest, userName, password, "/roulette", "user")
        userSessionReady = true
    }

    private void loginAsAdmin() {
        adminCsrfToken = loginAndCreateSession(adminAuthRequest, adminUserName, adminPassword, "/admin", "admin")
        adminSessionReady = true
    }

    private String loginAndCreateSession(HTTPRequest request, String loginId, String loginPw, String csrfPagePath, String roleLabel) {
        HTTPResponse loginPage = get(request, "/login", "GET /login (${roleLabel})", 200)
        String loginCsrf = extractCsrfValue(loginPage.getText())
        Assert.assertNotNull("CSRF token not found from /login", loginCsrf)

        HTTPResponse loginResp = request.POST(
                urlFor("/login"),
                [
                        new NVPair("userName", loginId),
                        new NVPair("password", loginPw),
                        new NVPair("_csrf", loginCsrf)
                ] as NVPair[]
        )
        assertStatusIn(loginResp, "POST /login", 302)
        if (!hasJSessionIdCookie(loginResp)) {
            Grinder.grinder.logger.warn("{} login response has no JSESSIONID Set-Cookie. Will verify session with authenticated probe.", roleLabel)
        }
        String location = headerValue(loginResp, "Location")
        Assert.assertNotNull("POST /login missing Location header", location)
        boolean successRedirect = location.contains("/roulette") || location.contains("/admin")
        boolean failureRedirect = location.contains("/login?error") || location.contains("/login?locked")
        Assert.assertTrue("Login failed or unknown redirect. Location=${location}", successRedirect && !failureRedirect)

        HTTPResponse csrfPage = get(request, csrfPagePath, "GET ${csrfPagePath} (${roleLabel})", 200)
        String token = extractCsrfValue(csrfPage.getText())
        if (token == null) {
            Grinder.grinder.logger.info("No CSRF token found from {} for {} session; proceeding without CSRF header.", csrfPagePath, roleLabel)
        }

        HTTPResponse authProbe = request.GET(urlFor("/api/menus/suggest?query=%EA%B9%80"))
        assertStatusIn(authProbe, "GET /api/menus/suggest preflight (${roleLabel})", 200)
        return token
    }

    private void suggest() {
        String query = pick(SEARCH_QUERIES)
        HTTPResponse resp = suggestRequest.GET(urlFor("/api/menus/suggest?query=${urlEncode(query)}"))
        assertStatusIn(resp, "GET suggest", 200)
    }

    private void searchFoodId() {
        String query = pick(SEARCH_QUERIES)
        HTTPResponse resp = searchFoodIdRequest.GET(urlFor("/api/foods/search?name=${urlEncode(query)}"))
        assertStatusIn(resp, "GET food search", 200)
    }

    private void nutrition() {
        if (!knownFoodIds.isEmpty()) {
            long foodId = knownFoodIds[ThreadLocalRandom.current().nextInt(knownFoodIds.size())]
            HTTPResponse resp = nutritionRequest.GET(urlFor("/api/foods/${foodId}/nutrition"))
            assertStatusIn(resp, "GET nutrition", 200)
            return
        }

        long randomFoodId = ThreadLocalRandom.current().nextLong(1, 1_000_000)
        HTTPResponse fallbackResp = nutritionRequest.GET(urlFor("/api/foods/${randomFoodId}/nutrition"))
        assertStatusIn(fallbackResp, "GET nutrition (fallback)", 200, 404)
    }

    private void topRanks() {
        HTTPResponse resp = topRanksRequest.GET(urlFor("/api/ranks/top?limit=5"))
        assertStatusIn(resp, "GET top ranks", 200)
    }

    private void postRank() {
        ensureUserSession()
        setHeaders(postRankRequest, "post-rank", userCsrfToken)
        String name = pick(HOT_FOODS)
        HTTPResponse resp = postRankRequest.POST(
                urlFor("/api/ranks"),
                jsonBytes([name: name])
        )
        assertStatusIn(resp, "POST rank", 200, 201)
    }

    private void addMenu() {
        ensureUserSession()
        setHeaders(addMenuRequest, "add-menu", userCsrfToken)
        String menuName = "menu_${ThreadLocalRandom.current().nextInt(100000)}"
        long foodId = ThreadLocalRandom.current().nextLong(1, 1_000_000)

        HTTPResponse resp = addMenuRequest.POST(
                urlFor("/api/menus"),
                jsonBytes([menuName: menuName, foodId: foodId])
        )
        assertStatusIn(resp, "POST add menu", 200, 201)
    }

    private void deleteMenu() {
        String menuName = "menu_${ThreadLocalRandom.current().nextInt(100000)}"

        HTTPResponse resp = deleteMenuRequest.DELETE(
                urlFor("/api/menus"),
                jsonBytes([menuName: menuName])
        )
        assertStatusIn(resp, "DELETE menu", 204)
    }

    private void adminFoodSearch() {
        ensureAdminSession()
        setHeaders(adminFoodSearchRequest, "admin-food-search", adminCsrfToken)
        String query = pick(SEARCH_QUERIES)
        HTTPResponse resp = adminFoodSearchRequest.GET(
                urlFor("/api/admin/foods?page=0&size=10&keyword=${urlEncode(query)}"))
        assertStatusIn(resp, "GET admin food search", 200)
    }

    private void seedKnownFoodIds() {
        LinkedHashSet<Long> seeded = new LinkedHashSet<>()

        (HOT_FOODS + SEARCH_QUERIES).each { String keyword ->
            HTTPResponse response = userAuthRequest.GET(urlFor("/api/foods/search?name=${urlEncode(keyword)}"))
            if (response.statusCode != 200) {
                return
            }

            Long foodId = parseLongSafely(response.getText())
            if (foodId != null) {
                seeded.add(foodId)
            }
        }

        knownFoodIds = seeded.toList()
        if (knownFoodIds.isEmpty()) {
            Grinder.grinder.logger.warn("No valid foodId seeded. nutrition scenario will allow 404 fallback.")
        }
    }

    // -----------------------------
    // Helpers
    // -----------------------------
    private HTTPResponse get(HTTPRequest request, String pathAndQuery, String name, int... allowedStatus) {
        HTTPResponse resp = request.GET(urlFor(pathAndQuery))
        assertStatusIn(resp, name, allowedStatus)
        return resp
    }

    private void setHeaders(HTTPRequest httpRequest, String scenario) {
        setHeaders(httpRequest, scenario, null)
    }

    private void setHeaders(HTTPRequest httpRequest, String scenario, String csrfToken) {
        httpRequest.setHeaders([
                new NVPair("Content-Type", "application/json"),
                new NVPair("Accept", "application/json"),
                new NVPair("X-CSRF-TOKEN", csrfToken ?: ""),
                new NVPair(RUN_ID_HEADER, runId),
                new NVPair(SCENARIO_HEADER, scenario)
        ] as NVPair[])
    }
    private static String headerValue(HTTPResponse response, String name) {
        if (response == null || name == null) {
            return null
        }

        String direct = response.getHeader(name)
        if (direct != null) return direct

        for (NVPair p : responseHeaders(response)) {
            if (p != null && p.getName() != null && p.getName().equalsIgnoreCase(name)) {
                return p.getValue()
            }
        }
        return null
    }


    private static void assertStatusIn(HTTPResponse response, String name, int... allowed) {
        if (isAuthRedirect(response)) {
            int count = authRedirectFailureCount.incrementAndGet()
            Grinder.grinder.logger.warn("[AUTH_REDIRECT][{}] status={}, location={}, count={}",
                    name, response.statusCode, headerValue(response, "Location"), count)
            Assert.fail("${name} authentication redirect: status=${response.statusCode}, location=${headerValue(response, "Location")}")
        }

        boolean ok = allowed.any { it == response.statusCode }
        if (!ok) {
            int count = functionalFailureCount.incrementAndGet()
            Grinder.grinder.logger.warn("[FUNCTIONAL_FAILURE][{}] status={}, count={}", name, response.statusCode, count)
        }
        Assert.assertTrue("${name} failed: status=${response.statusCode}", ok)
    }

    private static boolean isAuthRedirect(HTTPResponse response) {
        if (response == null || response.statusCode != 302) {
            return false
        }
        String location = headerValue(response, "Location") ?: ""
        return location.contains("/login")
    }

    private static boolean hasJSessionIdCookie(HTTPResponse response) {
        for (NVPair p : responseHeaders(response)) {
            if (p != null && p.getName() != null && p.getName().equalsIgnoreCase("Set-Cookie") && p.getValue() != null && p.getValue().contains("JSESSIONID=")) {
                return true
            }
        }
        return false
    }

    private static NVPair[] responseHeaders(HTTPResponse response) {
        if (response == null) {
            return [] as NVPair[]
        }

        try {
            if (response.metaClass.respondsTo(response, "listHeaders")) {
                Object listed = response.listHeaders()
                if (listed instanceof NVPair[]) {
                    return listed as NVPair[]
                }
            }
        } catch (Exception ignored) {
            // fall through
        }

        try {
            if (response.metaClass.respondsTo(response, "getHeaders")) {
                Object headers = response.getHeaders()
                if (headers instanceof NVPair[]) {
                    return headers as NVPair[]
                }
            }
        } catch (Exception ignored) {
            // fall through
        }

        return [] as NVPair[]
    }

    private void ensureUserSession() {
        if (!userSessionReady) {
            loginAsUser()
        }
    }

    private void ensureAdminSession() {
        if (!adminSessionReady) {
            loginAsAdmin()
        }
    }

    private static String pick(String[] arr) {
        return arr[ThreadLocalRandom.current().nextInt(arr.length)]
    }

    private static String extractCsrfValue(String html) {
        if (!html) return null
        def matcher = (html =~ /name="_csrf"[^>]*value="([^"]+)"/)
        return matcher.find() ? matcher.group(1) : null
    }

    private static String urlEncode(String raw) {
        return java.net.URLEncoder.encode(raw ?: "", "UTF-8")
    }

    private static byte[] jsonBytes(Map body) {
        return JsonOutput.toJson(body).getBytes("UTF-8")
    }

    private static Long parseLongSafely(String value) {
        if (!value) {
            return null
        }
        try {
            return Long.parseLong(value.trim())
        } catch (Exception ignored) {
            return null
        }
    }

    private static String urlFor(String pathAndQuery) {
        if (!pathAndQuery) return targetHost

        if (targetHost.endsWith("/") && pathAndQuery.startsWith("/")) {
            return targetHost.substring(0, targetHost.length() - 1) + pathAndQuery
        }
        if (!targetHost.endsWith("/") && !pathAndQuery.startsWith("/")) {
            return targetHost + "/" + pathAndQuery
        }
        return targetHost + pathAndQuery
    }
}
