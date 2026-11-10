import CatBoard
import SwiftUI

@main
struct CatBoardApp: App {
    var body: some Scene {
        WindowGroup {
            GalleryHost().ignoresSafeArea()
        }
    }
}

private struct GalleryHost: UIViewControllerRepresentable {
    func makeUIViewController(context _: Context) -> UIViewController {
        #if targetEnvironment(simulator)
            let screeningEnabled = false
        #else
            let screeningEnabled = true
        #endif
        return EntryPointKt.galleryViewController(
            screener: ScreeningBridge(classifier: CoreMLClassifier()),
            defaultScreeningEnabled: screeningEnabled
        )
    }

    func updateUIViewController(_: UIViewController, context _: Context) {}
}
