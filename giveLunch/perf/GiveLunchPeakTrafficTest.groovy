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
    static GTest testMain

    static String targetHost
    static String userName
    static String password

    static final int TIMEOUT_MS = 6000

    static final String[] HOT_FOODS = ["김치찌개", "제육볶음", "돈까스", "비빔밥", "된장찌개"]
    static final String[] SEARCH_QUERIES = ["김", "제", "돈", "라", "파", "샐", "찌개", "밥"]

    // -----------------------------
    // Thread-level (스레드별 상태)
    // -----------------------------
    HTTPRequest request
    String csrfToken

    @BeforeProcess
    static void beforeProcess() {
        HTTPPluginControl.getConnectionDefaults().timeout = TIMEOUT_MS
        HTTPPluginControl.getConnectionDefaults().useCookies = true
        CookieModule.setCookiePolicyHandler(null)

        def props = Grinder.grinder.getProperties()
        targetHost = props.getProperty("targetHost", "http://localhost:8080")
        userName = props.getProperty("userName", "loadtest1")
        password = props.getProperty("password", "loadtest-pass!")

        testMain = new GTest(1, "giveLunch peak traffic")
    }

    @BeforeThread
    void beforeThread() {
        if (Grinder.grinder.threadNumber == 0) {
            Grinder.grinder.logger.info("targetHost={}, userName={}", targetHost, userName)
        }

        // 스레드마다 request 인스턴스를 분리(헤더/쿠키 등 공유로 인한 섞임 방지)
        request = new HTTPRequest()
        testMain.record(request)

        login()
        Grinder.grinder.statistics.delayReports = true
    }

    @Before
    void before() {
        request.setHeaders([
                new NVPair("Content-Type", "application/json"),
                new NVPair("Accept", "application/json"),
                new NVPair("X-CSRF-TOKEN", csrfToken ?: "")
        ] as NVPair[])
    }

    @Test
    @RunRate(100)
    void trafficMix() {
        int r = ThreadLocalRandom.current().nextInt(100)

        if (r < 30) {
            suggest()
        } else if (r < 50) {
            searchFoodId()
        } else if (r < 65) {
            nutrition()
        } else if (r < 80) {
            topRanks()
        } else if (r < 90) {
            addMenu()
        } else if (r < 95) {
            deleteMenu()
        } else {
            postRank()
        }
    }

    // -----------------------------
    // Flows
    // -----------------------------
    private void login() {
        HTTPResponse loginPage = get("/login", "GET /login", 200)
        String loginCsrf = extractCsrfValue(loginPage.getText())
        Assert.assertNotNull("CSRF token not found from /login", loginCsrf)

        HTTPResponse loginResp = request.POST(
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
        get("/api/menus/suggest?query=${urlEncode(query)}", "GET suggest", 200)
    }

    private void searchFoodId() {
        String query = pick(SEARCH_QUERIES)
        get("/api/foods/search?name=${urlEncode(query)}", "GET food search", 200)
    }

    private void nutrition() {
        long foodId = ThreadLocalRandom.current().nextLong(1, 1_000_000)
        HTTPResponse resp = request.GET(urlFor("/api/foods/${foodId}/nutrition"))
        assertStatusIn(resp, "GET nutrition", 200, 404)
    }

    private void topRanks() {
        get("/api/ranks/top?limit=5", "GET top ranks", 200)
    }

    private void postRank() {
        String name = pick(HOT_FOODS)
        HTTPResponse resp = request.POST(
                urlFor("/api/ranks"),
                jsonBytes([name: name])
        )
        assertStatusIn(resp, "POST rank", 200, 201)
    }

    private void addMenu() {
        String menuName = "menu_${ThreadLocalRandom.current().nextInt(100000)}"
        long foodId = ThreadLocalRandom.current().nextLong(1, 1_000_000)

        HTTPResponse resp = request.POST(
                urlFor("/api/menus"),
                jsonBytes([menuName: menuName, foodId: foodId])
        )
        assertStatusIn(resp, "POST add menu", 200, 201)
    }

    private void deleteMenu() {
        String menuName = "menu_${ThreadLocalRandom.current().nextInt(100000)}"

        HTTPResponse resp = request.DELETE(
                urlFor("/api/menus"),
                jsonBytes([menuName: menuName])
        )
        assertStatusIn(resp, "DELETE menu", 200, 204, 404)
    }

    // -----------------------------
    // Helpers
    // -----------------------------
    private HTTPResponse get(String pathAndQuery, String name, int... allowedStatus) {
        HTTPResponse resp = request.GET(urlFor(pathAndQuery))
        assertStatusIn(resp, name, allowedStatus)
        return resp
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
        def matcher = (html =~ /name="_csrf"\s+value="([^"]+)"/)
        return matcher.find() ? matcher.group(1) : null
    }

    private static String urlEncode(String raw) {
        return java.net.URLEncoder.encode(raw ?: "", "UTF-8")
    }

    private static byte[] jsonBytes(Map body) {
        return JsonOutput.toJson(body).getBytes("UTF-8")
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
