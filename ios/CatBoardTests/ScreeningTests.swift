import CatBoard
@testable import CatBoardApp
import XCTest

@MainActor
final class ScreeningTests: XCTestCase {
    func testDisabledScreeningReturnsAllImagesWithoutClassification() async {
        let classifier = StubClassifier(result: .failure(NSError(domain: "Unexpected call", code: 1)))
        for scaryMode in [false, true] {
            let response = await screen(
                images: images(),
                settings: ScreeningSettings(isScreeningEnabled: false, scaryMode: scaryMode),
                classifier: classifier
            )
            XCTAssertEqual(response.urls, ["https://cats.example/a", "https://cats.example/b"])
            XCTAssertNil(response.error)
        }
        let calls = await classifier.calls
        XCTAssertEqual(calls, 0)
    }

    func testEmptyInputDoesNotInitializeTheClassifier() async {
        let classifier = StubClassifier(result: .failure(NSError(domain: "Unexpected call", code: 1)))
        let response = await screen(
            images: [],
            settings: ScreeningSettings(isScreeningEnabled: true, scaryMode: false),
            classifier: classifier
        )
        XCTAssertEqual(response.urls, [])
        XCTAssertNil(response.error)
        let calls = await classifier.calls
        XCTAssertEqual(calls, 0)
    }

    func testClassificationReceivesDataAndModeAndSelectsTheReturnedIndex() async {
        let classifier = StubClassifier(result: .success([1]))
        let response = await screen(
            images: images(),
            settings: ScreeningSettings(isScreeningEnabled: true, scaryMode: true),
            classifier: classifier
        )
        XCTAssertEqual(response.urls, ["https://cats.example/b"])
        XCTAssertNil(response.error)
        let payload = await classifier.payload
        let scaryMode = await classifier.scaryMode
        XCTAssertEqual(payload, [Data([1, 2, 3]), Data([1, 2, 3])])
        XCTAssertEqual(scaryMode, true)
    }

    func testClassificationErrorsAreForwardedWithoutAcceptingAnyImage() async {
        let failure = NSError(domain: "Classification unavailable", code: 42)
        let classifier = StubClassifier(result: .failure(failure))
        let response = await screen(
            images: images(),
            settings: ScreeningSettings(isScreeningEnabled: true, scaryMode: false),
            classifier: classifier
        )
        XCTAssertNil(response.urls)
        XCTAssertEqual((response.error as NSError?)?.domain, failure.domain)
        XCTAssertEqual((response.error as NSError?)?.code, failure.code)
    }

    func testIndexesOutsideTheSubmittedBatchFailClosed() async {
        for index in [-1, 2] {
            let response = await screen(
                images: images(),
                settings: ScreeningSettings(isScreeningEnabled: true, scaryMode: false),
                classifier: StubClassifier(result: .success([index]))
            )
            XCTAssertNil(response.urls)
            XCTAssertNotNil(response.error)
        }
    }

    private func images() -> [ImageData] {
        ["https://cats.example/a", "https://cats.example/b"].map { url in
            let bytes = KotlinByteArray(size: 3)
            bytes.set(index: 0, value: 1)
            bytes.set(index: 1, value: 2)
            bytes.set(index: 2, value: 3)
            return ImageData(imageURL: url, bytes: bytes)
        }
    }

    private func screen(
        images: [ImageData],
        settings: ScreeningSettings,
        classifier: any Classifier
    ) async -> (urls: [String]?, error: Error?) {
        let bridge = ScreeningBridge(classifier: classifier)
        return await withCheckedContinuation { continuation in
            bridge.screenImages(images: images, settings: settings) { urls, error in
                continuation.resume(returning: (urls, error))
            }
        }
    }
}

private actor StubClassifier: Classifier {
    private let result: Result<[Int], Error>
    private(set) var calls = 0
    private(set) var payload: [Data]?
    private(set) var scaryMode: Bool?

    init(result: Result<[Int], Error>) {
        self.result = result
    }

    func indexes(in images: [Data], scaryMode: Bool) async throws -> [Int] {
        calls += 1
        payload = images
        self.scaryMode = scaryMode
        return try result.get()
    }
}
