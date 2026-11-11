require "json"
require "open3"

def capture(*command)
  stdout, stderr, status = Open3.capture3(*command)
  abort(stderr.empty? ? "Command failed: #{command.join(' ')}" : stderr) unless status.success?
  stdout
end

sdk = capture("xcrun", "--sdk", "iphonesimulator", "--show-sdk-version").strip
runtime = JSON.parse(capture("xcrun", "simctl", "list", "runtimes", "--json"))
  .fetch("runtimes")
  .select { |candidate| candidate.fetch("isAvailable") && candidate.fetch("version").split(".").first(2) == sdk.split(".").first(2) }
  .max_by { |candidate| candidate.fetch("version").split(".").map(&:to_i) }
abort("No available iOS simulator runtime matches the selected iOS SDK #{sdk}") unless runtime
devices = JSON.parse(capture("xcrun", "simctl", "list", "devices", "available", "--json"))
  .fetch("devices")
  .select { |identifier, _| identifier == runtime.fetch("identifier") }
  .values.flatten
  .select { |device| device.fetch("isAvailable") && device.fetch("name").start_with?("iPhone ") }
device = devices.sort_by { |candidate| [candidate.fetch("name"), candidate.fetch("udid")] }.first
abort("No available iPhone simulator matches the selected iOS SDK #{sdk}") unless device
warn("Selected #{device.fetch('name')} with iOS #{runtime.fetch('version')} (SDK #{sdk})")
puts device.fetch("udid")
