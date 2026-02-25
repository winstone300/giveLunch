import HTTPClient.CookieModule
import HTTPClient.HTTPResponse
import HTTPClient.NVPair
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
class GiveLunchNutritionOnlyTest {
    static GTest testNutrition
    static String targetHost
    static String userName
    static String password
    static String runId
    static final int TIMEOUT_MS = 6000
    static final String RUN_ID_HEADER = "X-LoadTest-Run-Id"
    static final String SCENARIO_HEADER = "X-LoadTest-Scenario"
    static final String[] HOT_FOODS = ["김치찌개", "제육볶음", "돈까스", "비빔밥", "된장찌개"]
    static final String[] SEARCH_QUERIES = ["김", "제", "돈", "라", "파", "샐", "찌개", "밥"]

    HTTPRequest authRequest
    HTTPRequest nutritionRequest
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
        runId = props.getProperty("runId", "nutrition-${System.currentTimeMillis()}")

        testNutrition = new GTest(1, "GET /api/foods/{id}/nutrition")
    }

    @BeforeThread
    void beforeThread() {
        authRequest = new HTTPRequest()
        nutritionRequest = new HTTPRequest()
        testNutrition.record(nutritionRequest)
        login()
        seedKnownFoodIds()
        Grinder.grinder.statistics.delayReports = true
    }

    @Before
    void before() {
        setHeaders(nutritionRequest, "nutrition")
    }

    @Test
    @RunRate(100)
    void nutritionScenario() {
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
    }

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
        assertStatusIn(loginResp, "POST /login", 302)
        String location = headerValue(loginResp, "Location")
        Assert.assertNotNull("POST /login missing Location header", location)
        boolean successRedirect = location.contains("/roulette") || location.contains("/admin")
        boolean failureRedirect = location.contains("/login?error") || location.contains("/login?locked")
        Assert.assertTrue("Login failed or unknown redirect. Location=${location}", successRedirect && !failureRedirect)
        HTTPResponse roulettePage = get("/roulette", "GET /roulette", 200)
        csrfToken = extractCsrfValue(roulettePage.getText())
        Assert.assertNotNull("CSRF token not found from /roulette", csrfToken)
        HTTPResponse authProbe = authRequest.GET(urlFor("/api/menus/suggest?query=%EA%B9%80"))
        assertStatusIn(authProbe, "GET /api/menus/suggest preflight", 200)
    }

    private HTTPResponse get(String pathAndQuery, String name, int... allowedStatus) {
        HTTPResponse resp = authRequest.GET(urlFor(pathAndQuery))
        assertStatusIn(resp, name, allowedStatus)
        return resp
    }

    private void setHeaders(HTTPRequest request, String scenario) {
        request.setHeaders([
                new NVPair("Content-Type", "application/json"),
                new NVPair("Accept", "application/json"),
                new NVPair("X-CSRF-TOKEN", csrfToken ?: ""),
                new NVPair(RUN_ID_HEADER, runId),
                new NVPair(SCENARIO_HEADER, scenario)
        ] as NVPair[])
    }
    private static String headerValue(HTTPResponse response, String name) {
        String direct = response.getHeader(name)
        if (direct != null) return direct
        NVPair[] headers = response.getHeaders()
        if (headers == null) return null
        for (NVPair p : headers) {
            if (p != null && p.getName() != null && p.getName().equalsIgnoreCase(name)) {
                return p.getValue()
            }
        }
        return null
    }


    private static void assertStatusIn(HTTPResponse response, String name, int... allowed) {
        boolean ok = allowed.any { it == response.statusCode }
        Assert.assertTrue("${name} failed: status=${response.statusCode}", ok)
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

    private static String extractCsrfValue(String html) {
        if (!html) return null
        def matcher = (html =~ /name="_csrf"[^>]*value="([^"]+)"/)
        return matcher.find() ? matcher.group(1) : null
    }

    private static String urlEncode(String raw) {
        return java.net.URLEncoder.encode(raw ?: "", "UTF-8")
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
