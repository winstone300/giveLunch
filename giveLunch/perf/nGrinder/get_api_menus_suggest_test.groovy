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
class GiveLunchSuggestOnlyTest {
    static GTest testSuggest
    static String targetHost
    static String userName
    static String password
    static String runId
    static final int TIMEOUT_MS = 6000
    static final String RUN_ID_HEADER = "X-LoadTest-Run-Id"
    static final String SCENARIO_HEADER = "X-LoadTest-Scenario"
    static final String[] SEARCH_QUERIES = ["김", "제", "돈", "라", "파", "샐", "김치", "볶"]

    HTTPRequest authRequest
    HTTPRequest suggestRequest
    String csrfToken

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
        runId = props.getProperty("runId", "suggest-${System.currentTimeMillis()}")

        testSuggest = new GTest(1, "GET /api/menus/suggest")
    }

    @BeforeThread
    void beforeThread() {
        authRequest = new HTTPRequest()
        suggestRequest = new HTTPRequest()
        testSuggest.record(suggestRequest)
        login()
        Grinder.grinder.statistics.delayReports = true
    }

    @Before
    void before() {
        setHeaders(suggestRequest, "suggest")
    }

    @Test
    @RunRate(100)
    void suggestScenario() {
        String query = pick(SEARCH_QUERIES)
        HTTPResponse resp = suggestRequest.GET(urlFor("/api/menus/suggest?query=${urlEncode(query)}"))
        assertStatusIn(resp, "GET suggest", 200)
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

    private static String extractCsrfValue(String html) {
        if (!html) return null
        def matcher = (html =~ /name="_csrf"[^>]*value="([^"]+)"/)
        return matcher.find() ? matcher.group(1) : null
    }

    private static String pick(String[] arr) {
        return arr[ThreadLocalRandom.current().nextInt(arr.length)]
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
