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
- SoundPool ile yerel oyun sesleri

## Oynanış
- Ana menüden **Oyuna Başla** seçilir.
- Oyun sırasında her dokunuş karakteri yukarı zıplatır.
- Yer çekimi karakteri aşağı çeker.
- Boruların arasından geçildikçe skor 1 artar.
- Boruya, üst sınıra veya zemine çarpınca oyun biter.
- Oyun hızı skor yükseldikçe kademeli olarak artar.
- Yüksek skor cihazda saklanır.
- Ses menüden açılıp kapatılabilir.

## Görseller
- `app/src/main/res/drawable-nodpi/player_bird.png`: sağlanan karakter görseli, şeffaf arka planlı.
- `app/src/main/res/drawable-nodpi/background.png`: Kayseri/Erciyes temalı özgün arka plan.

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
