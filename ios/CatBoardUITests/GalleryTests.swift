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
        XCTAssertTrue(app.navigationBars["Cat Board"].exists)
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "Gallery"
        attachment.lifetime = .keepAlways
        add(attachment)
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
        XCTAssertTrue(app.navigationBars["Settings"].exists)
        XCTAssertEqual(screening.value as? String, "0")
        screening.tap()
        expectation(for: NSPredicate(format: "value == '1'"), evaluatedWith: screening)
        waitForExpectations(timeout: timeout)
        scaryMode.tap()
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "Settings"
        attachment.lifetime = .keepAlways
        add(attachment)
        let header = app.navigationBars["Settings"].coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0))
        let bottom = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.98))
        header.press(forDuration: 0.1, thenDragTo: bottom)
        expectation(for: NSPredicate(format: "exists == false"), evaluatedWith: screening)
        waitForExpectations(timeout: timeout)
        XCTAssertTrue(app.images["galleryImage_0"].waitForExistence(timeout: timeout))
    }
}
