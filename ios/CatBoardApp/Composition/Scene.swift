import SwiftUI

struct GalleryScene: View {
    @StateObject private var gallery: GalleryBinding

    init(dependencies: Dependencies) {
        _gallery = StateObject(wrappedValue: GalleryBinding(session: dependencies.makeGallery()))
    }

    var body: some View {
        CatImageGallery(viewModel: gallery)
    }
}
