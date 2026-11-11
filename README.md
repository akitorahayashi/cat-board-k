# Cat Board

Cat Boardは、SwiftUIの画面とKotlin Multiplatformのアプリケーションロジックで構成する、iOS向けの猫画像ギャラリーです。MVVM構造で、プラットフォーム固有のViewと共有のViewModel・データ層を分離しています。共有ライブラリはAndroidにも対応しており、Androidのアプリケーションと画像分類器は利用側で用意する構成です。

## アーキテクチャ

| モジュール | 責務 |
|---|---|
| [cat-api](modules/cat-api) | The Cat APIへのページ単位のリクエスト、レスポンス検証、取得件数の制限 |
| [image-store](modules/image-store) | Roomで永続化する画像URLのFIFOキューとバックグラウンド補充 |
| [image-loader](modules/image-loader) | Coilによる画像読み込み、容量を制限したキャッシュ、エンコード済み画像データの提供 |
| [image-screener](modules/image-screener) | スクリーニングのインターフェースと不変の設定値 |
| [image-prefetcher](modules/image-prefetcher) | スクリーニング設定ごとに分けて永続化する、採用済み画像のキュー |
| [app](modules/app) | 共有ViewModelの状態・操作、プラットフォーム別の依存構築、iOS向けフレームワークの公開 |

[プラットフォーム用の規約プラグイン](build-logic/src/main/kotlin/catboard.platforms.gradle.kts)が、iOSとAndroidのライブラリターゲットを定義しています。アプリケーションロジックは`commonMain`、通信エンジン・データベース構築・設定保存・画像エンコードは`iosMain`と`androidMain`に配置しています。iOSアプリは1つの静的フレームワークを利用し、Androidアプリは`:modules:app`に依存します。

[Swiftアプリ](ios/CatBoardApp)は、画面を`View`、状態のバインディングを`ViewModel`、依存構築とシーンの管理を`Composition`、Core MLとの連携を`Screening`に分けています。`GalleryBinding`は共有の`GallerySession`を監視して操作を伝え、読み込みロジックを重複して持ちません。アプリケーションは、共有キャッシュとリポジトリを持つ`GalleryRuntime`を所有します。各シーンは独立したセッションとViewModelを持ち、バインディングの解放時にはそのセッションだけをキャンセルします。ランタイムの終了時には、残っている処理をすべてキャンセルしてから共有リソースを閉じます。画像データもセッションを通じて共有キャッシュから取得します。

SwiftUIのギャラリーが、段組みレイアウト、ネイティブのナビゲーションバー、アイコンのアニメーション、設定シートを担当します。Androidでは、同じ`GalleryViewModel`の状態から利用側のViewを描画します。画像分類器は共有の`Screener`インターフェースを実装するプラットフォーム固有の処理で、Androidの依存構築時には指定が必須です。

ギャラリーは画像を段階的に読み込み、一覧の古い画像側に到達すると追加取得します。表示上限に達すると新しいギャラリーに切り替わります。段組みレイアウトは、正方形の画像をグループ単位で遅延描画します。設定変更時には進行中の処理をキャンセルし、変更前の設定で先読みした画像を破棄します。

スクリーニングの初期値は、シミュレーターでは無効、実機では有効です。通常モードでは安全と判定された画像、怖いモードでは安全でないと判定された画像を採用します。分類に失敗した場合はエラーを返し、その画像を採用しません。実装は[CoreMLClassifier.swift](ios/CatBoardApp/Screening/CoreMLClassifier.swift)にあり、[project.yml](ios/project.yml)で定義したパッケージを利用しています。

## 開発環境とビルド

必要な環境は、Apple Silicon搭載のMac、Xcode、JDK 17以上、Homebrewで導入したMint、[ios/.ruby-version](ios/.ruby-version)で指定したRubyです。Kotlinの依存ライブラリとプラグインは[Gradleのバージョンカタログ](gradle/libs.versions.toml)、Swift用ツールは[ios/Mintfile](ios/Mintfile)で管理しています。Swift用ツールは、確認済みのコミットに固定しています。

ツールチェーンの対応関係は[Kotlinの互換性ガイド](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)に記載されています。CIでは、[iOSの準備用Action](.github/actions/setup-ios/action.yml)がApple Siliconランナー上のXcodeを選択し、[Gradleの準備用Action](.github/actions/setup-gradle/action.yml)がJDKの選択とGradleのキャッシュを管理します。

リポジトリのルートで使う、環境準備・フレームワークのビルド・シミュレーター用アプリのビルドのコマンドは次のとおりです。

```sh
make setup
make framework
make build-ios
```

`make setup`はRubyの依存ライブラリとSwift用ツールを導入し、`ios/CatBoardApp.xcodeproj`を生成します。Xcodeでのビルド時にはGradleが呼び出され、Kotlinのフレームワークをビルドしてアプリに組み込みます。生成したプロジェクトでXcode上の開発ができます。

Android向け共有ライブラリは`make build-android`でビルドできます。Android SDKの場所は`ANDROID_HOME`または`local.properties`で指定し、SDKのレベルはバージョンカタログで管理しています。

署名なしアーカイブは`make build-release`で生成でき、出力先は`ios/fastlane/build/release/archive/CatBoardApp.xcarchive`です。署名用のFastlaneレーンは、環境変数の`APPLE_ID`と`TEAM_ID`を利用します。

## 検証

```sh
make test
```

`make test`は、Kotlinのシミュレーターテスト、Swiftのスクリーニング境界のテスト、XCTestのUIテストを実行します。シミュレーターの選択処理は、インストール済みの一覧から、使用中のXcode SDKとメジャー・マイナーバージョンが一致する最新パッチのランタイムを選び、そのランタイムのiPhoneを使用します。`LOCAL_SIMULATOR_UDID`で特定のシミュレーターを指定することもできます。

対象を絞ったiOSの検証には、`make test-kotlin`、`make unit-test`、`make ui-test`があります。`make test-android`では、Androidのホスト環境で共有Kotlinのテストを実行します。`make fix`はSwiftの整形と自動修正を行い、`make check`はその修正を適用してから整形・Lintを検証します。

UIテストは、起動引数で固定のローカル画像や模擬エラーを指定し、初期表示・更新・再試行・初回取得分を超える追加読み込み・設定変更を検証します。SwiftUIのViewにはネイティブのアクセシビリティ識別子を設定しています。画面の確認用に、ギャラリーと設定画面のスクリーンショットも保存します。

自動検証にはシミュレーターを使用します。実機でのスクリーニングとスクロール性能の測定は、今回の移行における検証範囲に含まれません。

## CI

[CI](.github/workflows/ci.yml)は、`main`向けのPull Request、`main`へのpush、手動実行で起動します。コード品質、共有Kotlinの検証、iOSアプリのテスト、署名なしReleaseアーカイブを、それぞれ独立したworkflowとして並列実行します。AndroidのホストテストとライブラリビルドはLinux、Kotlin Native・Swift・UIのテストとReleaseアーカイブの生成はApple SiliconのmacOSで実行します。

共通の準備用Actionがツールチェーンの選択とキャッシュを管理し、Makeターゲットがビルドとテストのコマンドを定義しています。コード品質の検証では、整形によってSwiftソースに差分が生じた場合は失敗とし、`actionlint`でworkflowの構文も確認します。`actionlint`を導入した環境では、`make check-workflows`でローカルでもworkflowを検証できます。

KotlinとXCTestの結果は別々の成果物として保存し、テスト失敗時にも取得できた結果を保存します。署名なしアーカイブは、権限とシンボリックリンクを保持するため、圧縮したtarファイルで保存します。成果物の保存期間は3日間です。アーカイブの生成には署名用のSecretsを使用しません。
