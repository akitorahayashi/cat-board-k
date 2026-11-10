import CatBoard
import SwiftUI
import UIKit

@MainActor
final class Dependencies: ObservableObject {
    private let runtime: GalleryRuntime

    init() {
        runtime = Self.makeRuntime()
    }

    deinit { runtime.close() }

    func makeGallery() -> GallerySession {
        runtime.createSession()
    }

    private static func makeRuntime() -> GalleryRuntime {
        let arguments = ProcessInfo.processInfo.arguments
        if arguments.contains("--uitesting") {
            guard let data = UIImage(named: "SampleUITestImage")?.pngData() else {
                fatalError("The UI test image is missing")
            }
            return GalleryFactoryKt.createTestGalleryRuntime(
                image: data,
                simulateError: arguments.contains("--simulate-error")
            )
        }
        #if targetEnvironment(simulator)
            let screeningEnabled = false
        #else
            let screeningEnabled = true
        #endif
        return GalleryFactoryKt.createGalleryRuntime(
            screener: Bridge(classifier: CoreMLClassifier()),
            defaultScreeningEnabled: screeningEnabled
        )
    }
}
