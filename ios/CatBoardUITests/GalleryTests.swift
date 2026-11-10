import XCTest

@MainActor
final class GalleryTests: XCTestCase {
    private var app: XCUIApplication!
    private let timeout: TimeInterval = 20

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
    }

    override func tearDown() {
        app.terminate()
        app = nil
        super.tearDown()
    }

    func testInitialDisplay() {
        app.launch()
        XCTAssertTrue(app.descendants(matching: .any)["gallery"].waitForExistence(timeout: timeout))
        XCTAssertTrue(app.images["galleryImage_0"].waitForExistence(timeout: timeout))
    }

    func testRefresh() {
        app.launch()
        XCTAssertTrue(app.images["galleryImage_0"].waitForExistence(timeout: timeout))
        let refresh = app.buttons["refreshButton"]
        XCTAssertTrue(refresh.isEnabled)
        refresh.tap()
        XCTAssertTrue(app.images["galleryImage_0"].waitForExistence(timeout: timeout))
        XCTAssertFalse(app.staticTexts["errorTitle"].exists)
    }

    func testErrorAndRetry() {
        app.launchArguments.append("--simulate-error")
        app.launch()
        XCTAssertTrue(app.staticTexts["errorTitle"].waitForExistence(timeout: timeout))
        app.buttons["retryButton"].tap()
        XCTAssertTrue(app.images["galleryImage_0"].waitForExistence(timeout: timeout))
        XCTAssertFalse(app.staticTexts["errorTitle"].exists)
    }

    func testAdditionalFetch() {
        app.launch()
        XCTAssertTrue(app.images["galleryImage_0"].waitForExistence(timeout: timeout))
        let gallery = app.descendants(matching: .any)["gallery"]
        let target = app.images["galleryImage_39"]
        for _ in 0 ..< 15 {
            if target.exists, target.isHittable {
                break
            }
            gallery.swipeDown(velocity: .fast)
        }
        XCTAssertTrue(target.waitForExistence(timeout: timeout))
        XCTAssertTrue(target.isHittable)
    }

    func testSettingsChangeAndReturnToGallery() {
        app.launch()
        XCTAssertTrue(app.images["galleryImage_0"].waitForExistence(timeout: timeout))
        app.buttons["settingsButton"].tap()
        let screening = app.switches["screeningToggle"]
        let scaryMode = app.switches["scaryModeToggle"]
        XCTAssertTrue(screening.waitForExistence(timeout: timeout))
        XCTAssertFalse(scaryMode.isEnabled)
        screening.tap()
        expectation(for: NSPredicate(format: "enabled == true"), evaluatedWith: scaryMode)
        waitForExpectations(timeout: timeout)
        scaryMode.tap()
        app.buttons["doneButton"].tap()
        XCTAssertTrue(app.images["galleryImage_0"].waitForExistence(timeout: timeout))
    }
}
