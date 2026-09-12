# Erciyes Uçuşu

Kayseri ve Erciyes temalı, özgün bir Android arcade oyunu. Oyuncu, gönderilen karakter görselini kullanarak engeller arasından uçar.

## Teknoloji
- Native Android / Kotlin
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- compileSdk / targetSdk 35, minSdk 23
- Canvas tabanlı oyun döngüsü
- Offline çalışma
- SharedPreferences ile kalıcı yüksek skor
- ToneGenerator ile basit oyun sesleri

## Oynanış
- Ana menüden **Oyuna Başla** seçilir.
- Oyun sırasında yalnızca `ACTION_DOWN` dokunuşu karakteri yukarı zıplatır.
- Yer çekimi karakteri aşağı çeker.
- Boruların arasından geçildikçe skor 1 artar ve aynı boru tekrar skor vermez.
- Boruya, üst sınıra veya zemine çarpınca oyun biter.
- Oyun hızı skor yükseldikçe kademeli olarak artar.
- Yüksek skor cihazda saklanır.
- Ses menüden açılıp kapatılabilir.

## Görseller
- `app/src/main/res/drawable-nodpi/player_bird.png`: sağlanan karakter görseli, tam bitmap kaynağı kullanılarak aspect ratio korunur.
- Kayseri/Erciyes arka planı Canvas üzerinde çizilir; boru ve karakter görüntüsü arka plana gömülü değildir.

## Oyun mekaniği ve düzeltmeler
- Kuş hitbox'ı çizilen kuşun merkezine bağlıdır ve her kenardan yaklaşık %15 inset uygulanır.
- Üst ve alt boruların ayrı `RectF` collision alanları vardır.
- Collision koordinatları doğrudan boruların çizim koordinatlarından gelir; oyun boşluğu collision alanına dahil edilmez.
- Boru kapakları boşluğun içine taşmayacak şekilde katı boru tarafında çizilir.
- İlk boru ekranın dışında ve kuşun başlangıç yüksekliğine hizalı geniş bir boşlukla oluşturulur.
- Oyun parametreleri `onSizeChanged()` içinde ekran boyutuna göre hesaplanır: `gravity`, `flapVelocity`, `pipeSpeed`, `pipeGap`, `birdSize`.
- Dokunma olayında `setContentView()`, `requestLayout()`, `Canvas.translate()` veya `Canvas.scale()` çağrılmaz.
- Tek oyun döngüsü `postInvalidateOnAnimation()` ile çalışır; dokunma yeni bir loop başlatmaz.
- `onDraw()` Canvas durumunu `save()/restore()` ile izole eder.

## Bilinen test durumu
**GitHub Actions clean assembleDebug:** Başarılı. APK yapısal olarak doğrulandı.

**Kod seviyesinde kontrol edilen senaryolar:** collision alanlarının boşluğu kapsamaması, kuş hitbox toleransı, ilk borunun güvenli mesafesi, tekil skor artırımı, yeniden başlatmada boruların temizlenmesi, ana menü dönüşü, mute ayarının saklanması ve dokunmanın yeni oyun döngüsü başlatmaması.

**Gerçek cihaz/emülatör testi:** Bu build ortamında Android cihaz/emülatör ve `adb` bulunmadığından fiziksel kurulum ve dokunmatik ekran testi gerçekleştirilemedi. Bu nedenle gerçek cihaz testi henüz doğrulanmış kabul edilmez.

## Yerel derleme
Android Studio güncel bir sürümle projeyi açın ve Android SDK 35'in kurulu olduğundan emin olun.

```bash
./gradlew clean assembleDebug
```

APK:
`app/build/outputs/apk/debug/app-debug.apk`

Wrapper dosyaları mevcut değilse Android Studio'nun Gradle senkronizasyonunu veya sistemdeki Gradle 8.9 kurulumunu kullanarak wrapper üretilebilir:

```bash
gradle wrapper --gradle-version 8.9
./gradlew clean assembleDebug
```

## GitHub Actions
`.github/workflows/android.yml` her push'ta Ubuntu runner üzerinde Java 17, Gradle 8.9 ve Android SDK 35 ile debug APK üretir. APK, Actions artifact olarak yayınlanır.

## Release APK
Release imzalama anahtarı bu repoya eklenmemiştir. Gerçek mağaza dağıtımı için kendi keystore'unuzu güvenli şekilde CI secrets üzerinden bağlayıp `assembleRelease` çalıştırın. Keystore veya şifreleri repoya koymayın.
