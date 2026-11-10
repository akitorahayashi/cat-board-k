require "json"
require "open3"

def capture(*command)
  stdout, stderr, status = Open3.capture3(*command)
  abort(stderr.empty? ? "Command failed: #{command.join(' ')}" : stderr) unless status.success?
  stdout
end

sdk = capture("xcrun", "--sdk", "iphonesimulator", "--show-sdk-version").strip
runtimes = JSON.parse(capture("xcrun", "simctl", "list", "runtimes", "--json"))
  .fetch("runtimes")
  .select { |runtime| runtime.fetch("isAvailable") && runtime.fetch("version") == sdk }
  .map { |runtime| runtime.fetch("identifier") }
devices = JSON.parse(capture("xcrun", "simctl", "list", "devices", "available", "--json"))
  .fetch("devices")
  .select { |runtime, _| runtimes.include?(runtime) }
  .values.flatten
  .select { |device| device.fetch("isAvailable") && device.fetch("name").start_with?("iPhone ") }
device = devices.sort_by { |candidate| [candidate.fetch("name"), candidate.fetch("udid")] }.first
abort("No available iPhone simulator matches the selected iOS SDK #{sdk}") unless device
warn("Selected #{device.fetch('name')} with iOS #{sdk}")
puts device.fetch("udid")
