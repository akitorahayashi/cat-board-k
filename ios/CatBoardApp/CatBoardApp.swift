import SwiftUI

@main
struct CatBoardApp: App {
    @StateObject private var dependencies = Dependencies()

    var body: some Scene {
        WindowGroup {
            GalleryScene(dependencies: dependencies)
        }
    }
}
