import Foundation
import ScaryCatScreeningKit

actor CoreMLClassifier: Classifier {
    private var initialization: Task<ScaryCatScreener, Error>?
    private var model: ScaryCatScreener?

    func indexes(in images: [Data], scaryMode: Bool) async throws -> [Int] {
        let screener = try await loadModel()
        let result = try await screener.screen(imageDataList: images, probabilityThreshold: 0.85, enableLogging: false)
        return (scaryMode ? result.unsafeResults : result.safeResults).map(\.originalIndex)
    }

    private func loadModel() async throws -> ScaryCatScreener {
        if let model {
            return model
        }
        if let initialization {
            return try await initialization.value
        }
        let task = Task { try await ScaryCatScreener(enableLogging: false) }
        initialization = task
        do {
            let screener = try await task.value
            model = screener
            initialization = nil
            return screener
        } catch {
            initialization = nil
            throw error
        }
    }
}
