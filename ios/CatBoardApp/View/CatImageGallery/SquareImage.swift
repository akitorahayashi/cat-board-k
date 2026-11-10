import CatBoard
import ImageIO
import OSLog
import SwiftUI

struct SquareImage: View {
    let url: String
    let viewModel: GalleryBinding

    @State private var image: UIImage?
    @State private var request: Observation?
    private let cornerRadius: CGFloat = 8

    var body: some View {
        GeometryReader { geometry in
            let size = geometry.size.width
            Group {
                if let image {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .transition(.opacity)
                } else {
                    Color(.secondarySystemBackground)
                }
            }
            .frame(width: size, height: size)
            .clipped()
            .cornerRadius(cornerRadius)
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .stroke(Color(.systemBackground).opacity(0.3), lineWidth: 2)
            )
        }
        .onAppear(perform: loadImage)
        .onDisappear {
            request?.cancel()
            request = nil
        }
    }

    private func loadImage() {
        guard image == nil, request == nil else { return }
        request = viewModel.loadImage(url) { data in
            let options: [CFString: Any] = [
                kCGImageSourceCreateThumbnailFromImageAlways: true,
                kCGImageSourceCreateThumbnailWithTransform: true,
                kCGImageSourceThumbnailMaxPixelSize: 200,
                kCGImageSourceShouldCacheImmediately: true,
            ]
            guard let source = CGImageSourceCreateWithData(data as CFData, nil),
                  let thumbnail = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary)
            else {
                Logger(subsystem: "com.akitorahayashi.CatBoardApp", category: "Gallery")
                    .error("Image thumbnail decoding failed")
                return
            }
            withAnimation(.easeOut(duration: 0.3)) {
                image = UIImage(cgImage: thumbnail)
            }
        }
    }
}
