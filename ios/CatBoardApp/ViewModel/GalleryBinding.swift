import CatBoard
import OSLog
import SwiftUI

@MainActor
final class GalleryBinding: ObservableObject {
    @Published private(set) var state: GalleryState
    @Published private(set) var settings: ScreeningSettings

    private let session: GallerySession
    private var observations: [Observation] = []
    private let logger = Logger(subsystem: "com.akitorahayashi.CatBoardApp", category: "Gallery")

    var imageURLsToShow: [GalleryImage] {
        state.images
    }

    var errorMessage: String? {
        state.errorMessage
    }

    var isInitializing: Bool {
        state.isInitializing
    }

    var isAdditionalFetching: Bool {
        state.isAdditionalFetching
    }

    init(session: GallerySession) {
        self.session = session
        state = session.state
        settings = session.settings
        observations = [
            session.observeState { [weak self] state in
                guard let self else { return }
                self.state = state
                if let warning = state.warning {
                    logger.error("Background image processing failed: \(warning, privacy: .public)")
                }
            },
            session.observeSettings { [weak self] settings in
                self?.settings = settings
            },
        ]
    }

    deinit {
        observations.forEach { $0.cancel() }
        session.close()
    }

    func loadInitialImages() {
        session.loadInitialImages()
    }

    func refresh() {
        session.refresh()
    }

    func fetchAdditionalImages() {
        session.fetchAdditionalImages()
    }

    func setScreeningEnabled(_ enabled: Bool) {
        session.changeSettings(isScreeningEnabled: enabled, scaryMode: settings.scaryMode)
    }

    func setScaryMode(_ enabled: Bool) {
        session.changeSettings(isScreeningEnabled: settings.isScreeningEnabled, scaryMode: enabled)
    }

    func loadImage(_ url: String, onData: @escaping (Data) -> Void) -> Observation {
        session.loadImage(imageURL: url) { image in
            onData(image.asData())
        } onError: { [logger] error in
            logger.error("Image loading failed: \(error, privacy: .public)")
        }
    }
}
