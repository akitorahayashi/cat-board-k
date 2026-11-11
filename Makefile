.DEFAULT_GOAL := help
.NOTPARALLEL:
-include .env

LOCAL_SIMULATOR_UDID ?= $(shell ruby ios/scripts/simulator.rb)
IOS_DERIVED_DATA := ../.tmp/agent-device/build/ios-simulator-debug

.PHONY: help setup setup-tools gen-proj framework build-ios build-android test test-kotlin test-android test-ios unit-test ui-test fix check check-workflows build-debug build-release sign-debug-development sign-release-development sign-release-app-store sign-release-ad-hoc sign-release-enterprise

help:
	@echo "make setup       Install iOS tools and generate the Xcode project"
	@echo "make framework   Build the Kotlin simulator framework"
	@echo "make build-ios   Build the iOS simulator app"
	@echo "make build-android  Build the shared Android library"
	@echo "make test        Run Kotlin, Swift boundary, and UI tests"
	@echo "make test-android  Run shared Kotlin tests on the Android host"
	@echo "make fix         Format and autocorrect Swift sources"
	@echo "make check       Apply fixes and validate Swift sources"
	@echo "make check-workflows  Validate GitHub Actions workflows"
	@echo "make build-release  Build an unsigned iOS release archive"

setup:
	cd ios && bundle install
	$(MAKE) setup-tools gen-proj

setup-tools:
	cd ios && mint bootstrap

gen-proj:
	cd ios && TEAM_ID="$(TEAM_ID)" mint run xcodegen generate

framework:
	./gradlew :modules:app:linkDebugFrameworkIosSimulatorArm64

build-ios: gen-proj
	cd ios && xcodebuild -project CatBoardApp.xcodeproj -scheme CatBoardApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath $(IOS_DERIVED_DATA) CODE_SIGN_IDENTITY=- build

build-android:
	./gradlew :modules:app:assembleAndroidMain

test: test-kotlin test-ios

test-kotlin:
	./gradlew iosSimulatorArm64Test

test-android:
	./gradlew testAndroidHostTest

test-ios: gen-proj
	cd ios && xcodebuild -project CatBoardApp.xcodeproj -scheme CatBoardApp -configuration Debug -sdk iphonesimulator -destination 'platform=iOS Simulator,id=$(LOCAL_SIMULATOR_UDID)' -derivedDataPath $(IOS_DERIVED_DATA) -parallel-testing-enabled NO $(IOS_TEST_FILTER) CODE_SIGN_IDENTITY=- test

unit-test: IOS_TEST_FILTER = -only-testing:CatBoardTests
unit-test: test-ios

ui-test: IOS_TEST_FILTER = -only-testing:CatBoardUITests
ui-test: test-ios

fix:
	cd ios && mint run swiftformat CatBoardApp CatBoardTests CatBoardUITests
	cd ios && mint run swiftlint --fix

check: fix
	cd ios && mint run swiftformat CatBoardApp CatBoardTests CatBoardUITests --lint
	cd ios && mint run swiftlint --strict

check-workflows:
	actionlint

build-debug: gen-proj
	cd ios && bundle exec fastlane build_debug

build-release: gen-proj
	cd ios && bundle exec fastlane build_release

sign-debug-development:
	cd ios && bundle exec fastlane sign_debug_development

sign-release-development:
	cd ios && bundle exec fastlane sign_release export_method:debugging

sign-release-app-store:
	cd ios && bundle exec fastlane sign_release export_method:app-store-connect

sign-release-ad-hoc:
	cd ios && bundle exec fastlane sign_release export_method:release-testing

sign-release-enterprise:
	cd ios && bundle exec fastlane sign_release export_method:enterprise
