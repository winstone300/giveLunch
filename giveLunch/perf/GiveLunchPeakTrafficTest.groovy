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
class GiveLunchPeakTrafficTest {

    public static GTest testMain
    public static HTTPRequest request

    static String targetHost
    static String userName
    static String password

    static final String[] HOT_FOODS = ["김치찌개", "제육볶음", "돈까스", "비빔밥", "된장찌개"]
    static final String[] SEARCH_QUERIES = ["김", "제", "돈", "라", "파", "샐", "찌개", "밥"]

    String csrfToken

    @BeforeProcess
    static void beforeProcess() {
        HTTPPluginControl.getConnectionDefaults().timeout = 6000
        HTTPPluginControl.getConnectionDefaults().useCookies = true
        CookieModule.setCookiePolicyHandler(null)

        targetHost = Grinder.grinder.getProperties().getProperty("targetHost", "http://localhost:8080")
        userName = Grinder.grinder.getProperties().getProperty("userName", "loadtest1")
        password = Grinder.grinder.getProperties().getProperty("password", "loadtest-pass!")

        request = new HTTPRequest()
        testMain = new GTest(1, "giveLunch peak traffic")
        testMain.record(request)
    }

    @BeforeThread
    void beforeThread() {
        if (Grinder.grinder.threadNumber == 0) {
            Grinder.grinder.logger.info("targetHost={}, userName={}", targetHost, userName)
        }
        login()
        Grinder.grinder.statistics.delayReports = true
    }

    @Before
    void before() {
        request.setHeaders([
                new NVPair("Content-Type", "application/json"),
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

    private void login() {
        HTTPResponse loginPage = request.GET("${targetHost}/login")
        assertOk(loginPage, "/login")

        String loginHtml = loginPage.getText()
        String loginCsrf = extractCsrfValue(loginHtml)

        HTTPResponse loginResp = request.POST(
                "${targetHost}/login",
                [
                        new NVPair("userName", userName),
                        new NVPair("password", password),
                        new NVPair("_csrf", loginCsrf)
                ] as NVPair[]
        )

        Assert.assertTrue("Login failed: status=${loginResp.statusCode}",
                loginResp.statusCode == 200 || loginResp.statusCode == 302)

        HTTPResponse roulettePage = request.GET("${targetHost}/roulette")
        assertOk(roulettePage, "/roulette")
        csrfToken = extractCsrfValue(roulettePage.getText())
        Assert.assertNotNull("CSRF token not found from /roulette", csrfToken)
    }

    private void suggest() {
        String query = pick(SEARCH_QUERIES)
        HTTPResponse response = request.GET("${targetHost}/api/menus/suggest?query=${url(query)}")
        assertOk(response, "suggest")
    }

    private void searchFoodId() {
        String query = pick(SEARCH_QUERIES)
        HTTPResponse response = request.GET("${targetHost}/api/foods/search?name=${url(query)}")
        assertOk(response, "food search")
    }

    private void nutrition() {
        long foodId = ThreadLocalRandom.current().nextLong(1, 1000000)
        HTTPResponse response = request.GET("${targetHost}/api/foods/${foodId}/nutrition")
        Assert.assertTrue("nutrition failed: ${response.statusCode}",
                response.statusCode == 200 || response.statusCode == 404)
    }

    private void topRanks() {
        HTTPResponse response = request.GET("${targetHost}/api/ranks/top?limit=5")
        assertOk(response, "top ranks")
    }

    private void postRank() {
        String name = pick(HOT_FOODS)
        HTTPResponse response = request.POST(
                "${targetHost}/api/ranks",
                "{\"name\":\"${name}\"}".bytes
        )
        assertCreatedOrOk(response, "post rank")
    }

    private void addMenu() {
        String menuName = "menu_${ThreadLocalRandom.current().nextInt(100000)}"
        long foodId = ThreadLocalRandom.current().nextLong(1, 1000000)
        HTTPResponse response = request.POST(
                "${targetHost}/api/menus",
                "{\"menuName\":\"${menuName}\",\"foodId\":${foodId}}".bytes
        )
        Assert.assertTrue("add menu failed: ${response.statusCode}",
                response.statusCode == 201 || response.statusCode == 200)
    }

    private void deleteMenu() {
        String menuName = "menu_${ThreadLocalRandom.current().nextInt(100000)}"
        HTTPResponse response = request.DELETE(
                "${targetHost}/api/menus",
                "{\"menuName\":\"${menuName}\"}".bytes
        )
        Assert.assertTrue("delete menu failed: ${response.statusCode}",
                response.statusCode == 204 || response.statusCode == 200 || response.statusCode == 404)
    }

    private static void assertOk(HTTPResponse response, String name) {
        Assert.assertEquals("${name} failed", 200, response.statusCode)
    }

    private static void assertCreatedOrOk(HTTPResponse response, String name) {
        Assert.assertTrue("${name} failed: ${response.statusCode}",
                response.statusCode == 201 || response.statusCode == 200)
    }

    private static String pick(String[] arr) {
        return arr[ThreadLocalRandom.current().nextInt(arr.length)]
    }

    private static String extractCsrfValue(String html) {
        def matcher = (html =~ /name="_csrf"\s+value="([^"]+)"/)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    private static String url(String raw) {
        return java.net.URLEncoder.encode(raw, "UTF-8")
    }
}