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
class GiveLunchDeleteMenuOnlyTest {
    static GTest testDeleteMenu
    static String targetHost
    static String userName
    static String password
    static String runId
    static final int TIMEOUT_MS = 6000
    static final String RUN_ID_HEADER = "X-LoadTest-Run-Id"
    static final String SCENARIO_HEADER = "X-LoadTest-Scenario"

    HTTPRequest authRequest
    HTTPRequest deleteMenuRequest
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
        runId = props.getProperty("runId", "delete-menu-${System.currentTimeMillis()}")

        testDeleteMenu = new GTest(1, "DELETE /api/menus")
    }

    @BeforeThread
    void beforeThread() {
        authRequest = new HTTPRequest()
        deleteMenuRequest = new HTTPRequest()
        testDeleteMenu.record(deleteMenuRequest)
        login()
        Grinder.grinder.statistics.delayReports = true
    }

    @Before
    void before() {
        setHeaders(deleteMenuRequest, "delete-menu")
    }

    @Test
    @RunRate(100)
    void deleteMenuScenario() {
        String menuName = "menu_${ThreadLocalRandom.current().nextInt(100000)}"
        HTTPResponse resp = deleteMenuRequest.DELETE(
                urlFor("/api/menus"),
                jsonBytes([menuName: menuName])
        )
        assertStatusIn(resp, "DELETE menu", 204)
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
        assertStatusIn(loginResp, "POST /login", 200, 302)

        HTTPResponse roulettePage = get("/roulette", "GET /roulette", 200)
        csrfToken = extractCsrfValue(roulettePage.getText())
        Assert.assertNotNull("CSRF token not found from /roulette", csrfToken)
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

    private static void assertStatusIn(HTTPResponse response, String name, int... allowed) {
        boolean ok = allowed.any { it == response.statusCode }
        Assert.assertTrue("${name} failed: status=${response.statusCode}", ok)
    }

    private static String extractCsrfValue(String html) {
        if (!html) return null
        def matcher = (html =~ /name="_csrf"[^>]*value="([^"]+)"/)
        return matcher.find() ? matcher.group(1) : null
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
