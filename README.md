# MelWeather

Galaksi temalı, cam (glassmorphism) tasarımlı hava durumu uygulaması. Kullanıcılar o anki havayla birlikte fotoğraf paylaşır, paylaşımlar herkese canlı görünür. Ana ekran widget'ı son paylaşılan fotoğrafı, şehri, sıcaklığı, saati ve tarihi gösterir.

## Kullanılan teknolojiler

- **Kotlin + Jetpack Compose** (Android, minSdk 26, compileSdk 36)
- **Open-Meteo**: anlık, saatlik ve 7 günlük tahmin, şehir arama (API anahtarı gerekmez)
- **OpenStreetMap Nominatim**: konumdan şehir adı
- **Firebase Authentication**: kullanıcı adı + şifre ile giriş (`ad@havadurumu.app` biçiminde)
- **Cloud Firestore**: `posts` koleksiyonu, fotoğraflar Base64 olarak saklanır (ücretsiz Spark planında çalışır)
- **Maestro**: `.maestro/` klasöründe UI test akışları

## Bilgisayar formatlandıktan sonra kurulum

1. **Android Studio**'yu kur. Kurulum sırasında Android SDK da gelir.
2. **Git**'i kur ve projeyi indir:
   ```
   git clone https://github.com/ece00nur/melWeather.git
   ```
3. Android Studio → **File → Open** → indirilen `melWeather` klasörünü seç → **Trust Project**.
4. Gradle senkronizasyonu bitince eksik SDK (Platform 36) varsa Android Studio kendisi indirir.
5. Emülatör veya telefon seç → **▶ Run**.

`app/google-services.json` depoda olduğu için Firebase bağlantısı ek bir ayar gerektirmeden çalışır.

## İmzalı APK (yayın sürümü)

**Build → Generate Signed App Bundle or APK → APK** yolunu izle ve **aynı keystore dosyasını** seç. Keystore depoda **yoktur**; ayrı bir yerde (Drive/USB) saklanır. Farklı bir keystore ile imzalanan APK, telefondaki eski sürümün üzerine kurulamaz; önce eskisini kaldırmak gerekir.

## Firebase

- Proje: `havadurumu-b9b79` (Spark planı)
- Firestore güvenlik kuralları: `firestore.rules` (Firebase Console → Firestore → Rules)
- Authentication'da **E-posta/Şifre** giriş yöntemi açık olmalı.

## Klasörler

| Klasör | İçerik |
|---|---|
| `app/` | Uygulama kaynak kodu |
| `belgeler/` | Tasarım taslağı, widget tasarımı ve ekran görüntüleri |
| `.maestro/` | Maestro test akışları |

Eski ilk sürüm (XML tabanlı HavaDurumu projesi) bu deponun **`eski-havadurumu`** dalındadır.
