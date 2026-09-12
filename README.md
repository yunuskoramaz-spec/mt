# Erciyes Uçuşu

Kayseri ve Erciyes temalı native Android arcade oyunu.

## v4 kalite yenilemesi
- 1080x1920 sanal oyun alanı ve tek uniform ölçek kullanılır.
- Tek `Choreographer.FrameCallback` vardır; `frameCallbackPosted` + `isRunning` ile aynı callback iki kez planlanamaz.
- İlk frame delta'sı `0f`; diğer delta değerleri `0..0.033s` aralığına clamp edilir.
- `onPause()` oyun döngüsünü durdurur; `onResume()` zaman tabanını sıfırlayarak fizik sıçramasını önler.
- Dokunma yalnızca `ACTION_DOWN` üzerinde flap üretir. Touch hiçbir zaman yeni loop, Runnable, layout veya Activity oluşturmaz.
- Frame içi pahalı `LinearGradient` ve `Typeface.create` üretimleri kaldırıldı; shader ve fontlar bir kez hazırlanır.
- Bitmap bir kez yüklenir ve tam kaynak alanıyla aspect ratio korunarak çizilir.
- Boru yaşam döngüsü `PipeSpawner` içine ayrıldı. Render katmanı boru listesine dokunmaz.
- Sabit `MAX_PIPES = 8` sınırı kaldırıldı. Borular ekran dışına çıktıkça silinir ve sağ tarafta yeniden üretilir.
- Borular benzersiz ID taşır; skor ve spawn birbirinden bağımsızdır.
- Her fizik güncellemesinde en fazla bir yeni boru oluşturulur.
- Aktif boru listesi sürekli korunur; NaN/infinite koordinatlar ve spacing ihlalleri invariant ile yakalanır.
- İlk üç engel daha öngörülebilir boşlukla başlar; sonraki boşluklar kontrollü rastgelelik kullanır.
- Oyun sonu state'inde fizik güncellemesi durur; tekrar oynama ve ana menü geçişleri boru state'ini temiz başlatır.
- Oyun sonu kartı mevcut premium tasarım korunarak merkez hizası ve sabit tasarım koordinatlarıyla çizilir.

## Oynanış
- Ana menüden Oyuna Başla seçilir.
- Oyun sırasında `ACTION_DOWN` kuşu bir kez zıplatır.
- Boru boşluğundan tamamen geçildiğinde skor bir kez artar.
- Boruya, üst sınıra veya zemine çarpınca oyun biter.
- Boru üretimi skora veya dokunmaya bağlı değildir; oyun sürdüğü sürece devam eder.
- Tekrar Oyna temiz oyun state'i ile başlar.
- Ana Menü eski boruları temizler.
- Ses açılıp kapatılabilir.

## Otomatik doğrulama
`PipeSpawnerTest` aşağıdaki davranışları doğrular:
- 8. engelden sonra üretimin devam etmesi.
- 30'dan fazla engelin simülasyonda üretilmesi.
- Aktif listenin sınırlı kalması.
- Ardışık boru spacing değerinin sabit kalması.
- NaN/infinite x değerlerinin oluşmaması.
- Reset sonrasında tek ve temiz ilk borunun kurulması.
- Tek fizik güncellemesinde burst spawn yapılmaması.

GitHub Actions, APK'dan önce `testDebugUnitTest` çalıştırır; ardından `clean assembleDebug` ve APK doğrulaması yapılır.

## Bilinen test durumu
**Kod ve otomatik test:** PipeSpawner simülasyonu 5000 frame boyunca çalıştırıldığında 58'den fazla engel üretir ve aktif liste 3 civarında tutulur. Repository testinde 30+ üretim ve reset/spacing invariantları doğrulanır.

**Frame/touch kod incelemesi:** tek callback guard, ilk-frame sıfır delta, 33 ms delta clamp, pause/resume sıfırlaması, yalnızca ACTION_DOWN flap, touch sırasında layout/state yeniden kurulmaması ve frame içi shader/font allocationlarının kaldırılması kontrol edildi.

**Gerçek cihaz/emülatör:** Bu geliştirme ortamında Android cihaz/emülatör ve `adb` bulunmadığı için fiziksel kurulum ve 5 dakikalık gerçek oynanış testi yapılamadı. Bu nedenle gerçek cihaz FPS ölçümü veya dokunmatik spike ölçümü doğrulanmış kabul edilmez.

**Performans hedefi:** 60 FPS. Render döngüsü tek callback üzerinden çalışır; bitmap decode ve ağır kaynak hazırlama frame dışındadır. Gerçek cihaz frame-time ölçümü yapılamamıştır.

## Teknoloji
- Native Android / Kotlin
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- compileSdk / targetSdk 35, minSdk 23
- Canvas + Choreographer
- SharedPreferences
- ToneGenerator
