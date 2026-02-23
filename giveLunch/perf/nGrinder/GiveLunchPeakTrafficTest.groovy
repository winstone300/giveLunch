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
    static String runId
    static boolean enableAdminFoodSearchScenario

    static final int TIMEOUT_MS = 6000
    static final String RUN_ID_HEADER = "X-LoadTest-Run-Id"
    static final String SCENARIO_HEADER = "X-LoadTest-Scenario"

    static final String[] HOT_FOODS = ["김치찌개", "제육볶음", "돈까스", "비빔밥", "된장찌개"]
    static final String[] SEARCH_QUERIES = ["김", "제", "돈", "라", "파", "샐", "찌개", "밥"]

    // -----------------------------
    // Thread-level (스레드별 상태)
    // -----------------------------
    HTTPRequest authRequest
    HTTPRequest suggestRequest
    HTTPRequest searchFoodIdRequest
    HTTPRequest nutritionRequest
    HTTPRequest topRanksRequest
    HTTPRequest addMenuRequest
    HTTPRequest deleteMenuRequest
    HTTPRequest postRankRequest
    HTTPRequest adminFoodSearchRequest
    String csrfToken
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
        authRequest = new HTTPRequest()
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

        login()
        seedKnownFoodIds()
        Grinder.grinder.statistics.delayReports = true
    }

    @Before
    void before() {
        setHeaders(suggestRequest, "suggest")
        setHeaders(searchFoodIdRequest, "food-search")
        setHeaders(nutritionRequest, "nutrition")
        setHeaders(topRanksRequest, "top-ranks")
        setHeaders(addMenuRequest, "add-menu")
        setHeaders(deleteMenuRequest, "delete-menu")
        setHeaders(postRankRequest, "post-rank")
        setHeaders(adminFoodSearchRequest, "admin-food-search")
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
    private void login() {
        HTTPResponse loginPage = get("/login", "GET /login", 200)
        String loginCsrf = extractCsrfValue(loginPage.getText())
        Assert.assertNotNull("CSRF token not found from /login", loginCsrf)

        HTTPResponse loginResp = authRequest.POST(
                urlFor("/login"),
                [
                        new NVPair("userName", userName),
                        new NVPair("password", password),
                        new NVPair("_csrf", loginCsrf)
                ] as NVPair[]
        )
        assertStatusIn(loginResp, "POST /login", 200, 302)

        HTTPResponse roulettePage = get("/roulette", "GET /roulette", 200)
        csrfToken = extractCsrfValue(roulettePage.getText())
        Assert.assertNotNull("CSRF token not found from /roulette", csrfToken)
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
        String name = pick(HOT_FOODS)
        HTTPResponse resp = postRankRequest.POST(
                urlFor("/api/ranks"),
                jsonBytes([name: name])
        )
        assertStatusIn(resp, "POST rank", 200, 201)
    }

    private void addMenu() {
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
        String query = pick(SEARCH_QUERIES)
        HTTPResponse resp = adminFoodSearchRequest.GET(
                urlFor("/api/admin/foods?page=0&size=10&keyword=${urlEncode(query)}"))
        assertStatusIn(resp, "GET admin food search", 200)
    }

    private void seedKnownFoodIds() {
        LinkedHashSet<Long> seeded = new LinkedHashSet<>()

        (HOT_FOODS + SEARCH_QUERIES).each { String keyword ->
            HTTPResponse response = authRequest.GET(urlFor("/api/foods/search?name=${urlEncode(keyword)}"))
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
    private HTTPResponse get(String pathAndQuery, String name, int... allowedStatus) {
        HTTPResponse resp = authRequest.GET(urlFor(pathAndQuery))
        assertStatusIn(resp, name, allowedStatus)
        return resp
    }

    private void setHeaders(HTTPRequest httpRequest, String scenario) {
        httpRequest.setHeaders([
                new NVPair("Content-Type", "application/json"),
                new NVPair("Accept", "application/json"),
                new NVPair("X-CSRF-TOKEN", csrfToken ?: ""),
                new NVPair(RUN_ID_HEADER, runId),
                new NVPair(SCENARIO_HEADER, scenario)
        ] as NVPair[])
    }

    private static void assertStatusIn(HTTPResponse response, String name, int... allowed) {
        boolean ok = allowed.any { it == response.statusCode }
        Assert.assertTrue("${name} failed: status=${response.statusCode}", ok)
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
