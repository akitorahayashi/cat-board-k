import SwiftUI

struct SettingsView: View {
    @ObservedObject var viewModel: GalleryBinding

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    SettingsToggleCard(
                        title: "画像スクリーニング",
                        caption: "不適切な画像をフィルタリングします。オフにすると全ての画像が表示されます",
                        isOn: Binding(
                            get: { viewModel.settings.isScreeningEnabled },
                            set: { viewModel.setScreeningEnabled($0) }
                        ),
                        identifier: CBAccessibilityID.Settings.screeningToggle
                    )

                    Text("※シミュレータでは十分なパフォーマンスを出せないため、iPhone16等の実機でスクリーニングを実行することを推奨します")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.leading)
                        .padding(.horizontal)

                    Spacer().frame(height: 4)

                    SettingsToggleCard(
                        title: "スケアリーモード",
                        caption: "危険と判定された画像のみを表示します。通常は安全な画像のみが表示されます",
                        isOn: Binding(
                            get: { viewModel.settings.scaryMode },
                            set: { viewModel.setScaryMode($0) }
                        ),
                        identifier: CBAccessibilityID.Settings.scaryModeToggle
                    )

                    Text("※このモードを使用するには画像スクリーニングをオンにする必要があります")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.leading)
                        .padding(.horizontal)
                }
                .padding()
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
        }
        .accessibilityIdentifier("settingsSheet")
    }
}
