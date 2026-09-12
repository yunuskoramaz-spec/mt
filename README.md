# Erciyes Uçuşu

Kayseri ve Erciyes temalı, premium görsel dil hedefiyle yeniden tasarlanmış native Android arcade oyunu.

## Premium 2026 yeniden tasarımı
- Erciyes laciverti, buz mavisi, sıcak altın, petrol yeşili ve kırık beyazdan oluşan tutarlı renk tokenları.
- Özel oyun logosu: Erciyes dağı + kanat/uçuş izi motifi.
- Katmanlı gökyüzü, bulut, Erciyes silüeti, Kayseri şehir silüeti ve ön plan zemininden oluşan 2.5D dünya.
- Kayseri taş mimarisinden esinlenen, gradient/highlight/gölge kullanan yeni kule engelleri.
- Cam görünümlü skor HUD'u, merkezlenmiş oyun sonu kartı ve birincil/ikincil buton hiyerarşisi.
- Ana menüde idle kuş animasyonu, logo giriş animasyonu ve buton basma mikro animasyonu.
- Oyun içinde kuş dönüşü + 3 karelik önceden hazırlanmış idle/flap ölçek animasyonu.
- Çarpışmada kısa parçacık ve flash efekti; oyun sonu ekranında fizik tamamen durur.
- Ayarlar ekranında ses ve haptic kontrolü; tercihler SharedPreferences ile korunur.
- Farklı ekran oranlarında uniform tasarım ölçeği ve Android system-bar inset güvenli alanı.

## Oyun döngüsü ve performans
- Tek `Choreographer.FrameCallback` kullanılır.
- `frameCallbackPosted` + `isRunning` ile aynı callback'in birden fazla planlanması engellenir.
- İlk frame delta'sı `0f`; sonraki delta değerleri `0..0.033s` aralığına clamp edilir.
- `onPause()` callback'i durdurur; `onResume()` zaman tabanını sıfırlar.
- Touch yalnızca `ACTION_DOWN` üzerinde flap üretir. `ACTION_MOVE`, `ACTION_UP` ve `ACTION_CANCEL` fiziği değiştirmez.
- `onDraw()` yalnızca render eder; fizik, skor ve spawn `updateGame()` içindedir.
- Bitmap decode, bird frame hazırlığı, shader, font ve ses buffer hazırlığı başlangıçta bir kez yapılır.
- Frame içinde yeni `RectF`, `Path`, `Bitmap`, `Typeface` veya dosya/SharedPreferences erişimi yapılmaz.
- Debug build'de her 600 frame'de ortalama FPS, maksimum frame aralığı, callback sayısı, aktif pipe sayısı, son spawn zamanı ve son spawn X değeri Logcat'e yazılır. Kullanıcıya görünür debug HUD yoktur.

## Sonsuz engel sistemi
`PipeSpawner` boruların tek sahibidir. Render ve touch listede değişiklik yapmaz.

- Sabit `MAX_PIPES = 8` sınırı yoktur.
- Borular fizik güncellemesinde hareket eder.
- Ekran dışına çıkan borular temizlenir.
- Sağdaki borunun konumuna göre en fazla bir yeni boru aynı fizik güncellemesinde eklenir.
- Yeni boru sabit `PIPE_SPACING` mesafesiyle sağ tarafa planlanır.
- Spawn skordan, touch'tan ve render'dan bağımsızdır.
- Her boru benzersiz ID ve `passed` state'i taşır.
- Oyun aktifken liste boş bırakılamaz; NaN/infinite X ve spacing ihlalleri invariant ile yakalanır.
- Yeniden oynama `reset()` ile tamamen temiz state kurar; ana menü `clear()` ile obstacle state'ini kapatır.
- İlk engeller kontrollü boşluklarla başlar, ilerleyen engellerde rastgelelik kademeli olarak devreye girer.

## Ses ve haptic
`ToneGenerator` kaldırıldı. `PremiumSoundEngine`, başlangıçta küçük PCM ses bankasını hazırlar ve oyun sırasında önceden oluşturulmuş `AudioTrack` nesnelerini yeniden kullanır.

Ses bankası:
- Menü butonu
- Flap
- Skor
- Yeni rekor
- Çarpışma
- Oyun başlangıcı
- Oyun sonu
- Kısa UI tick

Haptic cihaz destekliyorsa buton, skor geçişi ve çarpışmada kullanılır. Ses/haptic tercihleri korunur.

## Otomatik doğrulama
`PipeSpawnerTest` şunları doğrular:
- 20'den fazla engelin üretilmesi.
- Uzun simülasyonda 35+ engelin üretilmesi.
- Aktif obstacle listesinin sınırlı kalması.
- Ardışık boru spacing değerinin sabit olması.
- NaN/infinite X oluşmaması.
- Gelecekte bir borunun her zaman planlı olması.
- Reset sonrasında tek ve temiz ilk borunun kurulması.
- Tek fizik güncellemesinde birden fazla burst spawn yapılmaması.

GitHub Actions önce `testDebugUnitTest`, ardından `clean assembleDebug` çalıştırır ve APK ZIP bütünlüğünü doğrular.

## Test durumu
- **Otomatik spawn testi:** 3600 fizik frame'i ve 60 FPS sabit zaman adımıyla 35'ten fazla engel üretimi doğrulanır; liste bounded kalır.
- **Uzun simülasyon:** Aynı mimari üzerinde 5000 frame'lik önceki v4 simülasyonunda 58+ engel gözlenmiştir. V5 testleri aynı invariantları daha sıkı şekilde doğrular.
- **Frame/touch statik inceleme:** tek callback guard, delta clamp, pause/resume reset, yalnızca ACTION_DOWN flap, render/fizik ayrımı ve frame allocation azaltımı kontrol edilmiştir.
- **Gerçek cihaz/emülatör:** Bu geliştirme ortamında Android cihaz/emülatör ve `adb` bulunmadığından fiziksel kurulum, gerçek 60 FPS ölçümü ve dokunma spike ölçümü yapılamamıştır.

## Teknoloji
- Native Android / Kotlin
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- compileSdk / targetSdk 35, minSdk 23
- Canvas + Choreographer
- SharedPreferences
- AudioTrack / PCM
