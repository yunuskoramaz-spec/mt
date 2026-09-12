# Erciyes Uçuşu

Kayseri ve Erciyes temalı native Android arcade oyunu.

## Görsel kalite geri dönüşü
- Aydınlık mavi gökyüzü, sade Erciyes silüeti, düşük kontrastlı Kayseri şehir çizgisi ve yeşil zemin kullanılır.
- Oyun karakteri ayrı `birdPaint` ile çizilir; her frame öncesi `alpha = 255` ve `colorFilter = null` zorlanır.
- Oyun HUD'unda yalnızca güncel skor bulunur. `YENİ REKOR` yalnızca oyun bittikten sonra ve final skor önceki rekoru geçtiyse görünür.
- Boru genişliği 160/1080, boşluk 600/1920 sanal tasarım alanındadır. Kapaklar gövdeye göre sınırlı genişletilir.
- Cam/blur ağırlıklı efektler ve oyun sırasında karartma overlay'i kaldırılmıştır. Overlay yalnızca `GAMEOVER` durumunda çizilir.
- Status/navigation bar renkleri gökyüzü ve zeminle uyumludur; Android 15 edge-to-edge opt-out ve tek inset hesap noktası kullanılır.
- `CleanGameView` menü, ayarlar, oyun, sade skor HUD'u ve oyun sonu kartını tek kontrollü görsel hiyerarşide render eder.

## Oyun döngüsü ve performans
- Tek `Choreographer.FrameCallback` kullanılır.
- `framePosted` + `running` ile aynı callback'in birden fazla planlanması engellenir.
- Frame delta `0..0.033s` aralığına clamp edilir.
- `onPause()` callback'i durdurur; `onResume()` zaman tabanını sıfırlar.
- Touch yalnızca `ACTION_DOWN` üzerinde flap üretir.
- Render ve fizik ayrıdır.
- Bitmap decode, kuş frame hazırlığı, shader ve font hazırlığı başlangıçta yapılır.
- Frame içinde yeni `RectF`, `Path`, `Bitmap` veya `Typeface` oluşturulmaz.
- Debug build'de her 600 frame'de FPS, maksimum frame aralığı, callback ve aktif pipe sayısı Logcat'e yazılır.

## Sonsuz engel sistemi
`PipeSpawner` boruların tek sahibidir.
- Sabit `MAX_PIPES = 8` sınırı yoktur.
- Ekran dışı borular temizlenir.
- Aynı fizik güncellemesinde en fazla bir yeni boru eklenir.
- Ardışık borular sabit spacing ile korunur.
- NaN/infinite X ve spacing ihlalleri invariant ile yakalanır.
- `reset()` temiz oyun state'i kurar.
- Per-frame `sortedBy`, `maxByOrNull`, `removeAll` ve lambda tabanlı invariant taramaları kaldırılmıştır.

## Ses ve haptic
`ToneGenerator` kaldırıldı. `PremiumSoundEngine` başlangıçta PCM ses bankasını hazırlar ve oyun sırasında yeniden kullanır.

## Otomatik doğrulama
`PipeSpawnerTest` şunları doğrular:
- 20'den fazla engel üretimi.
- Uzun simülasyonda 35+ engel üretimi.
- Aktif listenin bounded kalması.
- Sabit spacing ve gelecekte boru bulunması.
- NaN/infinite X oluşmaması.
- Reset sonrası temiz ilk boru ve ID.
- Tek update'te burst spawn olmaması.

GitHub Actions önce `testDebugUnitTest`, ardından `clean assembleDebug` çalıştırır ve APK bütünlüğünü doğrular.

## Görsel doğrulama
`Final Visual Validation` workflow'u Android 14 emülatöründe menü, oyun başlangıcı, kuş merkez, erken skor, geç skor, oyun sonu, tekrar oynama ve ayarlar durumlarını ekran görüntüsü olarak toplamayı ve tam 8 PNG oluştuğunu doğrulamayı hedefler.

## Test durumu
- Unit test + debug APK build: başarılı.
- Android 14 emülatöründe runtime tanılama: başarılı.
- APK: doğrulanmış ve artifact olarak yüklenmiştir.
- Görsel ekran görüntüsü artifact'ı için final workflow ayrıca tetiklenmektedir.

## Teknoloji
- Native Android / Kotlin
- compileSdk / targetSdk 35, minSdk 23
- Canvas + Choreographer
- SharedPreferences
- AudioTrack / PCM
