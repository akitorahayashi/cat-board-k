import CatBoard
import Foundation

final class ScreeningBridge: NSObject, Screener {
    private let classifier: any Classifier

    init(classifier: any Classifier) {
        self.classifier = classifier
        super.init()
    }

    func screenImages(
        images: [ImageData],
        settings: CatBoard.ScreeningSettings,
        completionHandler: @escaping ([String]?, Error?) -> Void
    ) {
        guard settings.isScreeningEnabled, !images.isEmpty else {
            completionHandler(images.map(\.imageURL), nil)
            return
        }
        let payload = images.map { $0.asData() }
        let urls = images.map(\.imageURL)
        let scaryMode = settings.scaryMode
        Task {
            do {
                let indexes = try await classifier.indexes(in: payload, scaryMode: scaryMode)
                let selected = try indexes.map { index in
                    guard urls.indices.contains(index) else {
                        throw NSError(
                            domain: "ScreeningBridge",
                            code: 1,
                            userInfo: [NSLocalizedDescriptionKey: "Core ML returned an invalid image index"]
                        )
                    }
                    return urls[index]
                }
                completionHandler(selected, nil)
            } catch {
                completionHandler(nil, error)
            }
        }
    }
}
