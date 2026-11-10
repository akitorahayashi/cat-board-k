import Foundation

protocol Classifier: Sendable {
    func indexes(in images: [Data], scaryMode: Bool) async throws -> [Int]
}
