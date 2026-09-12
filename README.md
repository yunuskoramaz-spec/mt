# Erciyes Uçuşu

Kayseri ve Erciyes temalı native Android arcade oyunu.

## v3 görsel ve performans yenilemesi
- Oyun mantığı 1080x1920 sanal koordinat sisteminde çalışır.
- Tek `Choreographer.FrameCallback` oyun döngüsü kullanılır; dokunma yeni loop/Runnable oluşturmaz.
- Delta time 0..33 ms aralığına sınırlandırılır ve fizik FPS'ten bağımsızdır.
- Canvas transformu her frame `save()/restore()` ile izole edilir.
- Dokunma koordinatları tek uniform ölçeğin inverse transformu ile oyun koordinatına çevrilir.
- Bitmap oyun başlangıcında bir kez yüklenir ve kuş tam source rectangle ile çizilir.
- Kuş boruların ve tüm dekoratif arka plan katmanlarının üstünde çizilir.
- Collision kuşun gerçek çizim merkezinden türetilir ve yaklaşık %18 inset uygulanır.
- Üst ve alt boru hitboxları ayrı hesaplanır; boru boşluğu collision dışındadır.
- İlk boru ekran dışında başlar ve başlangıç boşluğu kuşun başlangıç yüksekliğine hizalanır.
- Boru, HUD, menü ve oyun sonu kartları yeniden tasarlandı.
- Alt zemin oyun alanının yalnızca son bölümünü kaplar.
- Erciyes, Kayseri şehir silüeti, kubbe/minare ve bulutlar sade katmanlar halinde çizilir.

## Oynanış
- Ana menüden Oyuna Başla seçilir.
- Oyun sırasında `ACTION_DOWN` kuşu bir kez zıplatır.
- Boru boşluğundan tamamen geçildiğinde skor bir kez artar.
- Boruya, üst sınıra veya zemine çarpınca oyun biter.
- Tekrar Oyna temiz oyun state'i ile başlar.
- Ana Menü eski boruları temizler.
- Ses açılıp kapatılabilir.

## Bilinen test durumu
**GitHub Actions:** `clean assembleDebug` başarılı ve debug APK artifact olarak üretildi.

**Kod seviyesinde doğrulama:** sanal koordinat sistemi, inverse touch transformu, tek Choreographer loop, collision boşluğu, skorun tekilleştirilmesi, ilk boru güvenliği, Canvas save/restore ve tam bitmap çizimi kontrol edildi.

**Gerçek cihaz/emülatör:** Bu geliştirme ortamında Android cihaz/emülatör ve `adb` bulunmadığı için v3 APK'nın fiziksel kurulum ve 5 dakikalık gerçek oynanış testi yapılamadı. Bu nedenle gerçek cihaz testi tamamlandı olarak işaretlenmez.

**Performans:** Frame update çizimden ayrıldı, bitmap decode frame dışına alındı, arka plan path'leri önceden oluşturuldu ve tek frame callback kullanıldı. Hedef 60 FPS'tir; gerçek cihaz FPS ölçümü yapılmadı.

## Teknoloji
- Native Android / Kotlin
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- compileSdk / targetSdk 35, minSdk 23
- Canvas + Choreographer
- SharedPreferences
- ToneGenerator
