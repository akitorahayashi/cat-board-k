import SwiftUI

struct SettingsToggleCard: View {
    let title: String
    let caption: String
    @Binding var isOn: Bool
    let identifier: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.headline)
                        .foregroundColor(.primary)

                    Text(caption)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.leading)
                }

                Spacer()

                Toggle("", isOn: $isOn)
                    .labelsHidden()
                    .accessibilityIdentifier(identifier)
            }
        }
        .padding()
        .background(Color(.systemBackground).opacity(0.6))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
